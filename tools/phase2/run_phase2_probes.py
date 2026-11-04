"""Run the phase-2 GT addon probes against the LOCAL test server only (G:/emoney-bio-test, loopback Rosetta).

Order matters: reload_before stashes a rocket, minecraft:reload replaces recipe objects, reload_after checks
that the rocket re-resolves the live recipe. Use `minecraft:reload`; on Mohist plain `reload` is the refused
Bukkit plugin reload and silently does nothing.
"""
from pathlib import Path
import json, subprocess, sys, time, datetime
here = Path(__file__).resolve().parent
bridge = Path('H:/MinecraftMods/Pollution-Unofficial-1.20.1/tools/bio/bio_bridge.py')
out = Path('G:/emoney-bio-test/phase2-reports'); out.mkdir(exist_ok=True)

def call(*args):
    run = subprocess.run([sys.executable, '-I', str(bridge), *args], capture_output=True, encoding='utf-8', timeout=180)
    try: return json.loads(run.stdout)
    except ValueError: return {'ok': False, 'error': run.stderr or run.stdout}

results = []
def probe(name):
    r = call('exec', str(here / (name + '.snippet')))
    text = r.get('result', {}).get('result', '') if isinstance(r.get('result'), dict) else ''
    ok = bool(r.get('ok')) and (text.startswith('PASS') or text.startswith('DUMP_OK') or text.startswith('PRE'))
    results.append({'name': name, 'passed': ok, 'response': r})
    print(name, 'PASS' if ok else 'FAIL', (text or r.get('error', ''))[:300], flush=True)

for name in ['phase2_dump', 'rocket_probe', 'plant_fuel_probe', 'equipment_probe', 'reach_probe', 'reload_before']:
    probe(name)
reload = call('console', 'minecraft:reload')
results.append({'name': 'minecraft:reload', 'passed': bool(reload.get('ok')), 'response': {'ok': reload.get('ok')}})
time.sleep(45)  # recipe reload completes asynchronously on the server thread
probe('reload_after')
(out / 'phase2-probes.json').write_text(json.dumps({'time': datetime.datetime.now().isoformat(), 'results': results}, ensure_ascii=False, indent=2), encoding='utf-8')
raise SystemExit(0 if all(r['passed'] for r in results) else 1)
