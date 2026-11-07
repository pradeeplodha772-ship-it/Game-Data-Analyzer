"""AMURIA safe mitmproxy inspector.

Use only with applications and test environments you own or are authorized to assess.
This addon observes traffic; it never changes requests or responses.
Sensitive headers and common secret fields are redacted before storage.
"""
import json, os, re, sqlite3, time
from mitmproxy import ctx

DB = os.environ.get("AMURIA_DB", "capture.db")
MAX_BODY = 32_000
SENSITIVE = re.compile(r"(authorization|cookie|token|secret|password|api[-_]?key|session)", re.I)

def clean_headers(headers):
    return {str(k): ("[REDACTED]" if SENSITIVE.search(str(k)) else str(v))
            for k, v in headers.items()}

def clean_text(raw, content_type=""):
    if not raw:
        return ""
    text = raw[:MAX_BODY].decode("utf-8", "replace")
    if "json" in content_type.lower() or text.lstrip().startswith(("{", "[")):
        try:
            def scrub(x):
                if isinstance(x, dict):
                    return {k: ("[REDACTED]" if SENSITIVE.search(k) else scrub(v))
                            for k, v in x.items()}
                if isinstance(x, list):
                    return [scrub(v) for v in x]
                if isinstance(x, str):
                    return re.sub(r"(?i)(bearer\\s+)[A-Za-z0-9._~+/-]+=*", r"\\1[REDACTED]", x)
                return x
            return json.dumps(scrub(json.loads(text)), ensure_ascii=False)
        except Exception:
            pass
    text = re.sub(r"(?i)(bearer\\s+)[A-Za-z0-9._~+/-]+=*", r"\\1[REDACTED]", text)
    return text

class AmuriaInspector:
    def load(self, loader):
        self.db = sqlite3.connect(DB, check_same_thread=False)
        self.db.execute("""CREATE TABLE IF NOT EXISTS traffic(
            id INTEGER PRIMARY KEY, ts REAL, method TEXT, url TEXT, status INTEGER,
            request_headers TEXT, response_headers TEXT, request_body TEXT, response_body TEXT)""")
        self.db.commit()

    def response(self, flow):
        req = flow.request
        res = flow.response
        row = (time.time(), req.method, req.pretty_url, res.status_code,
               json.dumps(clean_headers(req.headers)), json.dumps(clean_headers(res.headers)),
               clean_text(req.raw_content, req.headers.get("content-type", "")),
               clean_text(res.raw_content, res.headers.get("content-type", "")))
        self.db.execute("""INSERT INTO traffic(ts,method,url,status,request_headers,
            response_headers,request_body,response_body) VALUES(?,?,?,?,?,?,?,?)""", row)
        self.db.commit()
        ctx.log.info(f"[AMURIA] {req.method} {req.pretty_url} -> {res.status_code}")

    def done(self):
        if hasattr(self, "db"):
            self.db.close()

addons = [AmuriaInspector()]
