from pathlib import Path
from http.server import ThreadingHTTPServer,BaseHTTPRequestHandler
from urllib.parse import urlparse,parse_qs
import json,hmac,os
root=Path('work/wifi-player');key=json.loads((root/'control.json').read_text())['key']
class H(BaseHTTPRequestHandler):
 def log_message(self,*args):pass
 def headers_ok(self):
  token=parse_qs(urlparse(self.path).query).get('key',[''])[0]
  return hmac.compare_digest(token,key)
 def respond(self,body=b'{}',code=200,ctype='application/json'):
  self.send_response(code);self.send_header('Content-Type',ctype);self.send_header('Content-Length',str(len(body)));self.send_header('Access-Control-Allow-Origin','*');self.send_header('Cache-Control','no-store');self.end_headers();self.wfile.write(body)
 def do_OPTIONS(self):
  self.send_response(204);self.send_header('Access-Control-Allow-Origin','*');self.send_header('Access-Control-Allow-Headers','Content-Type,X-Capture-Status');self.send_header('Access-Control-Allow-Methods','GET,POST,OPTIONS');self.end_headers()
 def do_GET(self):
  if not self.headers_ok():self.respond(code=403);return
  path=urlparse(self.path).path
  if path=='/state':self.respond((root/'command.json').read_bytes())
  elif path=='/image':self.respond((root/'image.png').read_bytes(),ctype='image/png')
  else:self.respond(code=404)
 def do_POST(self):
  if not self.headers_ok():self.respond(code=403);return
  size=int(self.headers.get('Content-Length','0'))
  if size<0 or size>2000000:self.respond(code=413);return
  data=self.rfile.read(size);path=urlparse(self.path).path
  if path=='/picture':
   (root/'wifi-camera.yuyv').write_bytes(data);(root/'capture-status.json').write_text(json.dumps({'status':self.headers.get('X-Capture-Status'),'bytes':len(data),'revision':parse_qs(urlparse(self.path).query).get('revision')}));self.respond()
  elif path=='/status':(root/'projector-status.json').write_bytes(data);self.respond()
  else:self.respond(code=404)
ThreadingHTTPServer((os.environ.get('PROJECTION_BIND','127.0.0.1'),8769),H).serve_forever()
