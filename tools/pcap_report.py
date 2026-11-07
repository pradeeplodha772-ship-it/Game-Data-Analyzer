#!/usr/bin/env python3
"""AMURIA X read-only PCAP metadata report using tshark.

Reports packet counts, protocol distribution, endpoints, DNS query names,
TCP conversations and time span. Does not export packet payloads or secrets.
Requires Wireshark's tshark command to be installed.
"""
from __future__ import annotations

import argparse
import collections
import json
import shutil
import subprocess
import sys
from pathlib import Path


def run_tshark(path: Path, fields: list[str]) -> list[list[str]]:
    command = [
        "tshark", "-r", str(path), "-T", "fields",
        "-E", "separator=|", "-E", "occurrence=f",
    ]
    for field in fields:
        command.extend(["-e", field])
    result = subprocess.run(command, capture_output=True, text=True, check=False)
    if result.returncode:
        raise RuntimeError(result.stderr.strip() or "tshark failed to read the capture")
    return [line.split("|") for line in result.stdout.splitlines() if line]


def main() -> int:
    parser = argparse.ArgumentParser(description="Read-only PCAP metadata report")
    parser.add_argument("pcap", type=Path, help="input .pcap or .pcapng file")
    parser.add_argument("--json", dest="json_path", type=Path, help="write report JSON")
    args = parser.parse_args()

    if not args.pcap.is_file():
        parser.error(f"capture file not found: {args.pcap}")
    if not shutil.which("tshark"):
        parser.error("tshark not found; install Wireshark/tshark first")

    try:
        rows = run_tshark(args.pcap, [
            "frame.time_epoch", "frame.len", "_ws.col.Protocol",
            "ip.src", "ipv6.src", "ip.dst", "ipv6.dst",
            "tcp.srcport", "udp.srcport", "tcp.dstport", "udp.dstport",
            "dns.qry.name", "tcp.stream",
        ])
    except RuntimeError as exc:
        print(f"PCAP error: {exc}", file=sys.stderr)
        return 2

    protocols = collections.Counter()
    endpoints = collections.Counter()
    dns_names = collections.Counter()
    streams = set()
    total_bytes = 0
    timestamps = []

    for row in rows:
        row += [""] * (13 - len(row))
        epoch, length, proto, src4, src6, dst4, dst6, sport_tcp, sport_udp, dport_tcp, dport_udp, dns, stream = row[:13]
        protocols[proto or "Unknown"] += 1
        try:
            total_bytes += int(length or 0)
        except ValueError:
            pass
        if epoch:
            try:
                timestamps.append(float(epoch))
            except ValueError:
                pass
        src = src4 or src6
        dst = dst4 or dst6
        sport = sport_tcp or sport_udp
        dport = dport_tcp or dport_udp
        if src:
            endpoints[f"{src}:{sport}" if sport else src] += 1
        if dst:
            endpoints[f"{dst}:{dport}" if dport else dst] += 1
        if dns:
            dns_names[dns] += 1
        if stream:
            streams.add(stream)

    report = {
        "file": args.pcap.name,
        "packet_count": len(rows),
        "total_bytes": total_bytes,
        "duration_seconds": round(max(timestamps) - min(timestamps), 6) if len(timestamps) > 1 else 0,
        "protocols": protocols.most_common(),
        "top_endpoints": endpoints.most_common(30),
        "dns_queries": dns_names.most_common(100),
        "tcp_stream_count": len(streams),
        "note": "Metadata-only report. Packet payloads are intentionally not exported.",
    }
    print(json.dumps(report, indent=2, ensure_ascii=False))
    if args.json_path:
        args.json_path.write_text(json.dumps(report, indent=2, ensure_ascii=False), encoding="utf-8")
        print(f"Saved report: {args.json_path}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
