"""Execute a probe on the loopback-only void suit test server."""
from pathlib import Path
import argparse
import json
import socket

RUN = Path('G:/emoney-void-suit-test')
parser = argparse.ArgumentParser()
parser.add_argument('command', choices=('ping', 'exec', 'console'))
parser.add_argument('argument', nargs='?')
parser.add_argument('--save')
args = parser.parse_args()
if not (RUN / 'VOID_SUIT_TEST.json').is_file():
    raise SystemExit('Missing isolated test marker')
request = {'token': (RUN / 'RosettaRemoteDebugBridge/remote-token.txt').read_text().strip(), 'cmd': args.command}
if args.command == 'exec':
    request['args'] = {'code': Path(args.argument).read_text(encoding='utf-8')}
elif args.command == 'console':
    request['args'] = {'command': args.argument}
with socket.create_connection(('127.0.0.1', 48793), timeout=10) as sock:
    sock.settimeout(120)
    stream = sock.makefile('rwb')
    stream.write((json.dumps(request) + '\n').encode('utf-8'))
    stream.flush()
    response = json.loads(stream.readline())
if args.save:
    output = RUN / 'evidence' / Path(args.save).name
    output.write_text(json.dumps(response, ensure_ascii=False, indent=2), encoding='utf-8')
print(json.dumps(response, ensure_ascii=True))
raise SystemExit(0 if response.get('ok') else 1)
