# AMURIA X — Advanced Android & App Network Testing

AMURIA X is an authorized app/game network-testing workspace. It combines
local file analysis, read-only HTTP(S) inspection for owned debug clients,
PCAP metadata reporting, and a local API fault-simulation lab.

## 1. Read-only HTTP(S) session inspector

Install dependencies:

```bash
pip install mitmproxy flask
```

Start the capture addon:

```bash
mitmdump -p 8080 -s tools/capture_addon.py
```

For an app you own, configure its debug network security settings to trust
your test CA and set its proxy to the machine running mitmproxy. Then start
the dashboard in another terminal:

```bash
python tools/dashboard.py
```

Open `http://127.0.0.1:5000` on that same machine. This inspector is
read-only: it does not rewrite requests or responses. Common sensitive
headers and credential fields are redacted, but captures can still contain
private data. Use synthetic accounts and test data. Proxy capture is not
device-wide VPN capture.

## 2. Local API test lab

Run a local fixture server for your own test client:

```bash
python tools/network_lab.py --host 127.0.0.1 --port 8765
```

Available endpoints:

| Endpoint | Purpose |
|---|---|
| `GET /health` | Health check |
| `GET /echo`, `POST /echo` | Inspect test request metadata/body |
| `GET /delay/250` | Simulate latency (0–10,000 ms) |
| `GET /status/503` | Simulate an HTTP error (400–599 or 200) |
| `POST /validate-json` | Validate JSON syntax and basic structure |

The server binds to loopback by default and is intended for controlled
local testing, not public deployment.

## 3. Read-only PCAP metadata report

Install Wireshark/tshark, then run:

```bash
python tools/pcap_report.py capture.pcapng --json report.json
```

The report includes packet totals, protocol distribution, top endpoints,
DNS query names, TCP stream count and capture duration. It intentionally
does not dump packet payloads or extract secrets.

## 4. Android tools

The Android app currently supports local JSON, Base64, hex, schema-less
Protobuf wire-field and classic Ethernet PCAP inspection. The Android
local-file analyzer is separate from the optional Python live-proxy tools.

## 5. Roadmap

- Android local-VPN capture with explicit user consent and per-app selection.
- PCAPNG/IPv6 support and richer DNS/TLS metadata in the Android UI.
- Searchable sessions, request comparison, export and test reports.
- API test collections for staging/local endpoints.

## Security scope

AMURIA X is for systems and test accounts you own or are authorized to assess.
It does not bypass certificate pinning, root/integrity checks or
anti-debugging controls; harvest credentials; stealthily intercept another
app; manipulate live game state; or replay/alter arbitrary third-party traffic.
For negative tests, use controlled mock endpoints or an owned debug build.
