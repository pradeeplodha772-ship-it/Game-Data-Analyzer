"""Read-only local dashboard for AMURIA capture.db."""
import os
import sqlite3
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
<input id=q placeholder='Filter URL or method'><button id=filter>Filter</button>
<div id=rows></div><script>
const root=document.querySelector('#rows');
function addText(parent,tag,value){const el=document.createElement(tag);el.textContent=value??'';parent.appendChild(el);return el}
async function loadRows(){
 const q=encodeURIComponent(document.querySelector('#q').value);
 const response=await fetch('/api/traffic?q='+q);
 if(!response.ok)return;
 const data=await response.json();root.replaceChildren();
 for(const x of data.rows){
  const article=document.createElement('article');
  addText(article,'b',(x.method||'')+' → '+(x.status??''));
  addText(article,'small',(x.ts||'')+' · '+(x.url||''));
  const details=document.createElement('details');addText(details,'summary','Request / response');
  const body=[x.request_headers,x.request_body,'--- RESPONSE ---',x.response_headers,x.response_body].join('\\n');
  addText(details,'pre',body);article.appendChild(details);root.appendChild(article);
 }
}
document.querySelector('#filter').addEventListener('click',loadRows);
loadRows();setInterval(loadRows,3000);
</script>"""
@app.get("/")
def index():
    return render_template_string(PAGE)

@app.get("/api/traffic")
def traffic():
    q = request.args.get("q", "")[:200]
    con = sqlite3.connect(DB)
    con.row_factory = sqlite3.Row
    rows = con.execute(
        """SELECT id,ts,method,url,status,request_headers,response_headers,
                  request_body,response_body
           FROM traffic WHERE url LIKE ? OR method LIKE ?
           ORDER BY id DESC LIMIT 150""",
        (f"%{q}%", f"%{q}%"),
    ).fetchall()
    con.close()
    return jsonify(rows=[dict(r) for r in rows])

if __name__ == "__main__":
    app.run("127.0.0.1", 5000, debug=False)
