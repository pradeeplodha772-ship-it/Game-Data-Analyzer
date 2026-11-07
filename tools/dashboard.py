"""Read-only local dashboard for AMURIA capture.db."""
import os, sqlite3
from flask import Flask, jsonify, request, render_template_string

DB = os.environ.get("AMURIA_DB", "capture.db")
app = Flask(__name__)
PAGE = """<!doctype html><meta name=viewport content='width=device-width,initial-scale=1'>
<title>AMURIA X</title><style>
body{background:#0b1018;color:#e6edf3;font:14px monospace;margin:20px}
input,button{padding:10px;background:#161f2b;color:#fff;border:1px solid #354255}
article{border:1px solid #354255;padding:12px;margin:10px 0;overflow-wrap:anywhere}
small{color:#8b9bb0}pre{white-space:pre-wrap;overflow-wrap:anywhere;background:#111923;padding:10px}
</style><h2>AMURIA X · Traffic Inspector</h2>
<p>Read-only local view · sensitive values are redacted</p>
<input id=q placeholder='Filter URL or method'><button onclick='loadRows()'>Filter</button>
<div id=rows></div><script>
async function loadRows(){let q=encodeURIComponent(document.querySelector('#q').value);
let d=await (await fetch('/api/traffic?q='+q)).json();
document.querySelector('#rows').innerHTML=d.rows.map(x=>
'<article><b>'+x.method+' → '+x.status+'</b><br><small>'+x.ts+' · '+x.url+'</small>'+
'<details><summary>Request / response</summary><pre>'+x.request_headers+
'\\n'+x.request_body+'\\n--- RESPONSE ---\\n'+x.response_headers+'\\n'+x.response_body+'</pre></details></article>'
).join('')}
loadRows();setInterval(loadRows,3000);
</script>"""
@app.get("/")
def index():
    return render_template_string(PAGE)
@app.get("/api/traffic")
def traffic():
    q = request.args.get("q", "")[:200]
    con = sqlite3.connect(DB); con.row_factory = sqlite3.Row
    rows = con.execute("""SELECT id,ts,method,url,status,request_headers,response_headers,
        request_body,response_body FROM traffic WHERE url LIKE ? OR method LIKE ?
        ORDER BY id DESC LIMIT 150""", (f"%{q}%", f"%{q}%")).fetchall()
    con.close()
    return jsonify(rows=[dict(r) for r in rows])
if __name__ == "__main__":
    app.run("127.0.0.1", 5000, debug=False)
