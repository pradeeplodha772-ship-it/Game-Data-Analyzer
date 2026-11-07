# AMURIA X — Android & App Network Testing

AMURIA X is an authorized app/game testing workspace. The Android app currently
supports local JSON, Base64, Protobuf and classic PCAP inspection. The
`tools/` folder adds an optional read-only mitmproxy inspector and local
read-only dashboard for traffic from apps and test systems you own or are
authorized to assess.

## Safe traffic inspection (desktop / Termux Python environment)

1. Install dependencies: `pip install mitmproxy flask`
2. Start the proxy: `mitmdump -p 8080 -s tools/capture_addon.py`
3. In your own debug app/test device, explicitly configure its debug network
   security settings to trust the test CA, then set its proxy to the host
   running mitmproxy. Proxy capture is not device-wide VPN capture.
4. Start the dashboard: `python tools/dashboard.py`
5. Open `http://127.0.0.1:5000` on the same host.

The inspector never modifies traffic and redacts common credential fields
before writing records. Treat captured traffic as sensitive even after
redaction. Use synthetic accounts and test data.

## Included Android tools

- Import classic Ethernet PCAP and inspect IPv4 TCP/UDP metadata.
- Format JSON, decode Base64, inspect hex and schema-less Protobuf wire fields.
- GitHub Actions builds a debug APK on push to `main` or manual workflow run.

## Roadmap

- PCAPNG, IPv6, TCP stream reassembly and richer DNS/TLS metadata.
- Android local-VPN capture with explicit user consent and per-app selection.
- Session search, comparison, export and test reports.
- API test collections targeting staging/local endpoints.

## Scope

No certificate-pinning, root/integrity, or anti-debugging bypass; no credential
harvesting; no stealth interception; no remote game-state manipulation; no
arbitrary replay or response tampering. For security-negative tests, implement
controlled fault cases in a local mock server or an owned debug build.
