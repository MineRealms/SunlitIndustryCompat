"""Run reproducible probes against the marked, loopback-only isolated server."""
from pathlib import Path
import argparse
import json
import socket

ROOT = Path(__file__).resolve().parent
RUN = Path('G:/emoney-void-suit-test')
parser = argparse.ArgumentParser()
parser.add_argument('phase')
parser.add_argument('--only', nargs='+')
args = parser.parse_args()
if not (RUN / 'VOID_SUIT_TEST.json').is_file():
    raise SystemExit('Isolated test marker missing')
token = (RUN / 'RosettaRemoteDebugBridge/remote-token.txt').read_text().strip()


def execute(code):
    with socket.create_connection(('127.0.0.1', 48793), timeout=10) as sock:
        sock.settimeout(180)
        stream = sock.makefile('rwb')
        request = {'token': token, 'cmd': 'exec', 'args': {'code': code}}
        stream.write((json.dumps(request) + '\n').encode('utf-8'))
        stream.flush()
        return json.loads(stream.readline())


names = args.only or ['recipe_status', 'ae2_network_correctness', 'ae2_network_benchmark',
                     'ae2_crafting_benchmark', 'ae2_adjacent_world_benchmark', 'benchmark',
                     'group_correctness', 'ae2_key_correctness']
for name in names:
    if '/' in name or '\\' in name or '..' in name:
        raise SystemExit('Probe name must be a simple stem')
    # Keep factory-key hit counters out of later timing comparisons.
    prep = execute('com.society.sunlitindustry.ServerPerformanceStats.setRecording(false); return "READY";')
    if not prep.get('ok'):
        raise SystemExit(json.dumps(prep))
    response = execute((ROOT / (name + '.snippet')).read_text(encoding='utf-8'))
    output = RUN / 'evidence' / (args.phase + '-' + name + '.json')
    output.write_text(json.dumps(response, ensure_ascii=False, indent=2), encoding='utf-8')
    result = response.get('result', {})
    summary = result.get('result', '') if isinstance(result, dict) else str(result)
    print(json.dumps({'probe': name, 'ok': response.get('ok'), 'summary': summary[:240]}, ensure_ascii=True), flush=True)
    if not response.get('ok'):
        raise SystemExit(1)
