#!/usr/bin/env python3
"""AMURIA X local API test server.

Run only on your own machine/device test network:
    python tools/network_lab.py --host 127.0.0.1 --port 8765

Endpoints:
  GET  /health
  GET  /echo
  POST /echo
  GET  /delay/<milliseconds>   (0..10000)
  GET  /status/<code>           (400..599, or 200)
  POST /validate-json           (checks JSON syntax and reports structure)

This is a local test fixture. It does not proxy, intercept, or alter third-party traffic.
"""
from __future__ import annotations

import argparse
import json
import time
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from urllib.parse import urlparse


class Handler(BaseHTTPRequestHandler):
    server_version = "AMURIA-X-Lab/1.0"

    def _send(self, code: int, payload: dict) -> None:
        body = json.dumps(payload, ensure_ascii=False, indent=2).encode("utf-8")
        self.send_response(code)
        self.send_header("Content-Type", "application/json; charset=utf-8")
        self.send_header("Content-Length", str(len(body)))
        self.send_header("Cache-Control", "no-store")
        self.end_headers()
        self.wfile.write(body)

    def _read_body(self) -> bytes:
        try:
            length = min(int(self.headers.get("Content-Length", "0")), 1_048_576)
        except ValueError:
            length = 0
        return self.rfile.read(max(0, length))

    def do_GET(self) -> None:
        path = urlparse(self.path).path
        if path == "/health":
            self._send(200, {"ok": True, "service": "AMURIA X local test lab"})
        elif path == "/echo":
            self._send(200, {
                "method": self.command,
                "path": self.path,
                "headers": {k: v for k, v in self.headers.items()
                            if k.lower() not in {"authorization", "cookie", "set-cookie"}},
            })
        elif path.startswith("/delay/"):
            try:
                delay_ms = int(path.rsplit("/", 1)[1])
            except ValueError:
                self._send(400, {"error": "delay must be an integer"})
                return
            if not 0 <= delay_ms <= 10000:
                self._send(400, {"error": "delay must be between 0 and 10000 ms"})
                return
            time.sleep(delay_ms / 1000)
            self._send(200, {"ok": True, "simulated_delay_ms": delay_ms})
        elif path.startswith("/status/"):
            try:
                code = int(path.rsplit("/", 1)[1])
            except ValueError:
                self._send(400, {"error": "status must be an integer"})
                return
            if code not in {200, *range(400, 600)}:
                self._send(400, {"error": "supported status codes: 200 or 400..599"})
                return
            self._send(code, {"ok": code == 200, "simulated_status": code})
        else:
            self._send(404, {"error": "not found", "endpoints": [
                "/health", "/echo", "/delay/250", "/status/503", "/validate-json"
            ]})

    def do_POST(self) -> None:
        path = urlparse(self.path).path
        raw = self._read_body()
        if path == "/echo":
            self._send(200, {
                "method": "POST",
                "content_type": self.headers.get("Content-Type", ""),
                "body_bytes": len(raw),
                "body_utf8": raw[:65536].decode("utf-8", errors="replace"),
            })
        elif path == "/validate-json":
            try:
                value = json.loads(raw.decode("utf-8"))
            except (UnicodeDecodeError, json.JSONDecodeError) as exc:
                self._send(400, {"valid": False, "error": str(exc)})
                return
            self._send(200, {
                "valid": True,
                "root_type": type(value).__name__,
                "keys": list(value.keys())[:100] if isinstance(value, dict) else [],
                "array_length": len(value) if isinstance(value, list) else None,
            })
        else:
            self._send(404, {"error": "not found"})

    def log_message(self, fmt: str, *args) -> None:
        print("[AMURIA-LAB] " + (fmt % args))


def main() -> None:
    parser = argparse.ArgumentParser(description="AMURIA X local API test lab")
    parser.add_argument("--host", default="127.0.0.1")
    parser.add_argument("--port", type=int, default=8765)
    args = parser.parse_args()
    server = ThreadingHTTPServer((args.host, args.port), Handler)
    print(f"AMURIA X Lab listening on http://{args.host}:{args.port}")
    print("Use only with your own test clients and test data. Ctrl+C to stop.")
    try:
        server.serve_forever()
    except KeyboardInterrupt:
        print("\\nStopping AMURIA X Lab...")
    finally:
        server.server_close()


if __name__ == "__main__":
    main()
