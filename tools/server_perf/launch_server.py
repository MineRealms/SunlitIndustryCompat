"""Launch one A/B phase of the isolated GT/KubeJS benchmark server."""
from pathlib import Path
import json
import subprocess
import sys
import time

RUN = Path('G:/emoney-void-suit-test')
JAVA = Path('C:/Program Files/Eclipse Adoptium/jdk-17.0.18.8-hotspot/bin/java.exe')
mode = sys.argv[1] if len(sys.argv) == 2 else ''
if mode not in {'baseline', 'optimized', 'ae2-baseline', 'ae2-optimized',
                 'ae2-final-baseline', 'ae2-final-optimized',
                 'ae2-fluid-baseline', 'ae2-fluid-optimized', 'clean-baseline', 'clean-optimized',
                 'clean-warm-baseline'}:
    raise SystemExit('Usage: launch_server.py <baseline|optimized|clean-baseline|clean-optimized|ae2[-final|-fluid]-baseline|ae2[-final|-fluid]-optimized>')
if not (RUN / 'VOID_SUIT_TEST.json').is_file():
    raise SystemExit('Missing isolated test marker')
if (RUN / 'server.pid').exists():
    raise SystemExit('PID marker exists; inspect the server before launching another')

import socket
try:
    with socket.create_connection(('127.0.0.1', 48793), timeout=1):
        raise SystemExit('Rosetta port 48793 is already active')
except OSError:
    pass

evidence = RUN / 'evidence'
evidence.mkdir(exist_ok=True)
args = [str(JAVA), '-Xms2G', '-Xmx6G', '-Dfile.encoding=UTF-8',
        '-Dminecraft.gameDir=' + RUN.as_posix(),
        '-Drosetta.remote.bind=127.0.0.1', '-Drosetta.remote.port=48793',
        '-Xlog:gc*,safepoint:file=evidence/gc-' + mode + '.log:time,uptime,level,tags:filecount=2,filesize=16m',
        '-XX:StartFlightRecording=filename=evidence/' + mode + '.jfr,settings=profile,duration=900s,dumponexit=true']
if mode.endswith('baseline'):
    args.append('-Dsunlitindustry.disableServerOptimizations=true')
args += ['@forge-test-args.txt', 'nogui']
stdout_path = evidence / ('server-' + mode + '-stdout.log')
with stdout_path.open('wb') as output:
    process = subprocess.Popen(args, cwd=RUN, stdin=subprocess.DEVNULL, stdout=output, stderr=subprocess.STDOUT,
                               creationflags=subprocess.DETACHED_PROCESS | subprocess.CREATE_NEW_PROCESS_GROUP)
(RUN / 'server.pid').write_text(str(process.pid), encoding='ascii')
(evidence / ('launch-' + mode + '.json')).write_text(
    json.dumps({'pid': process.pid, 'mode': mode, 'java': str(JAVA)}, indent=2), encoding='utf-8')
start = time.monotonic()
while time.monotonic() - start < 1500:
    if process.poll() is not None:
        raise SystemExit('Server exited with code ' + str(process.returncode))
    with stdout_path.open('rb') as output:
        output.seek(0, 2)
        output.seek(max(0, output.tell() - 65536))
        tail = output.read()
    if b'Failed to load datapacks' in tail or b'Encountered an unexpected exception' in tail:
        process.terminate()
        process.wait(timeout=30)
        raise SystemExit('Isolated server startup failed; see ' + str(stdout_path))
    try:
        with socket.create_connection(('127.0.0.1', 48793), timeout=1):
            print(json.dumps({'pid': process.pid, 'mode': mode, 'readyAfterSeconds': round(time.monotonic()-start, 1)}))
            raise SystemExit(0)
    except OSError:
        time.sleep(5)
raise SystemExit('Startup timed out; server remains running with PID ' + str(process.pid))
