"""Loopback-only Rosetta client pinned to the isolated phase-4 server."""
from pathlib import Path
import json
import socket
import sys

RUN = Path('G:/emoney-full-phase4-20261104')
if not (RUN / 'PHASE4_MANIFEST.json').is_file():
    raise SystemExit('Missing isolation manifest')
token = (RUN / 'RosettaRemoteDebugBridge/remote-token.txt').read_text(encoding='utf-8').strip()
command = sys.argv[1] if len(sys.argv) > 1 else 'ping'
request = {'token': token, 'cmd': command}
if command == 'exec':
    request['args'] = {'code': Path(sys.argv[2]).read_text(encoding='utf-8')}
elif command == 'console':
    request['args'] = {'command': ' '.join(sys.argv[2:])}
with socket.create_connection(('127.0.0.1', 48790), timeout=60) as sock:
    sock.settimeout(240)
    stream = sock.makefile('rwb')
    stream.write((json.dumps(request) + '\n').encode())
    stream.flush()
    result = json.loads(stream.readline())
sys.stdout.reconfigure(encoding='utf-8')
print(json.dumps(result, ensure_ascii=False))
