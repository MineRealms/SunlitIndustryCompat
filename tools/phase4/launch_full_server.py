"""Start the staged loopback server once, recording startup evidence; leave it alive for probes."""
from pathlib import Path
import json
import subprocess
import sys
import time

RUN = Path('G:/emoney-full-phase4-20261104')
JAVA = Path('C:/Program Files/Java/jdk-21/bin/java.exe')
REPORT = RUN / 'phase4-reports'

if not (RUN / 'PHASE4_MANIFEST.json').is_file():
    raise SystemExit('Not the staged isolated server')
if (REPORT / 'server.pid').exists():
    raise SystemExit('PID marker already exists; inspect the process instead of starting another server')
args = [str(JAVA), '-Xms3G', '-Xmx11G', '-Dfile.encoding=UTF-8',
        '-Dminecraft.gameDir=' + RUN.as_posix(),
        '-Dsun.net.client.defaultConnectTimeout=10000', '-Dsun.net.client.defaultReadTimeout=10000',
        '-Drosetta.remote.bind=127.0.0.1', '-Drosetta.remote.port=48790',
        '-Xlog:gc*,safepoint:file=phase4-reports/gc.log:time,uptime,level,tags:filecount=2,filesize=32m',
        '-XX:StartFlightRecording=filename=phase4-reports/startup.jfr,settings=profile,duration=600s,dumponexit=true',
        '-jar', 'rosetta-1.20.1-ecf8d5ad-server.jar', 'nogui']
with (REPORT / 'console.log').open('wb') as out:
    p = subprocess.Popen(args, cwd=RUN, stdin=subprocess.DEVNULL, stdout=out, stderr=subprocess.STDOUT,
                         creationflags=subprocess.DETACHED_PROCESS | subprocess.CREATE_NEW_PROCESS_GROUP)
(REPORT / 'server.pid').write_text(str(p.pid))
start = time.monotonic()
last_size = 0
quiet_since = start
snapshots = 0
last_snapshot = 0
log = RUN / 'logs/latest.log'
while time.monotonic() - start < 1200:
    if p.poll() is not None:
        print('SERVER_EXIT', p.returncode, 'after', round(time.monotonic()-start), 'seconds')
        print((REPORT / 'console.log').read_text(encoding='utf-8', errors='replace')[-6000:])
        raise SystemExit(1)
    text = log.read_text(encoding='utf-8', errors='replace') if log.exists() else ''
    now = time.monotonic()
    if len(text) != last_size:
        quiet_since = now
        last_size = len(text)
    if now - quiet_since > 25 and now - last_snapshot > 25 and snapshots < 15:
        snapshots += 1
        last_snapshot = now
        try:
            result = subprocess.run([str(JAVA.with_name('jcmd.exe')), str(p.pid), 'Thread.print', '-l'],
                                    capture_output=True, timeout=20)
            (REPORT / f'startup-threads-{round(now-start):04d}.txt').write_bytes(result.stdout + result.stderr)
        except subprocess.TimeoutExpired:
            (REPORT / f'startup-threads-{round(now-start):04d}-timeout.txt').write_text('jcmd timed out')
    if 'Remote bridge listening on 127.0.0.1:48790' in text:
        summary = {'pid': p.pid, 'secondsToBridge': round(now-start, 1), 'threadSnapshots': snapshots,
                   'serverLeftRunning': True, 'root': str(RUN)}
        (REPORT / 'startup-summary.json').write_text(json.dumps(summary, indent=2))
        print(json.dumps(summary))
        raise SystemExit(0)
    time.sleep(5)
print('Startup observer timed out; server has NOT been stopped. PID', p.pid)
