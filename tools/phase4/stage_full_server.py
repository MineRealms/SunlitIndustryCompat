"""Build an isolated full-pack server snapshot. Never touches an existing server directory."""
import hashlib
import json
import secrets
import shutil
import sys
import zipfile
from pathlib import Path

PACK = Path('G:/MinecraftGames/Sunlit Valley(BaopuEdition)/.minecraft/versions/Society Sunlit Valley')
BASE = Path('G:/emoney-bio-test')
DEST = Path('G:/emoney-full-phase4-20261104')
PROJECT = Path('H:/MinecraftMods/SunlitIndustryCompat')


def digest(p):
    return hashlib.sha256(p.read_bytes()).hexdigest()


def main():
    if DEST.exists():
        raise SystemExit('Destination exists: refusing to overwrite ' + str(DEST))
    evidence = json.loads((PROJECT / '.local/production-valid-mod-files.json').read_text(encoding='utf-8'))
    extra = {'Middle-earth-1.4.2-1.20.1-port.0.jar', 'kotlinforforge-4.12.0-all.jar'}
    selected, omitted = [], []
    for p in sorted((PACK / 'mods').glob('*.jar')):
        if p.name in evidence or p.name in extra or 'ThaumicHorizons-' in p.name:
            selected.append(p)
        else:
            omitted.append(p.name)
    overrides = {
        'AstralSorcery-1.20.1.0.jar': Path('H:/MinecraftMods/AstralSorcery-1.20.1/build/libs/AstralSorcery-1.20.1.0.jar'),
        '(59+)污染 Pollution-1.20.1-1.0.0-port.0.1.0.jar': Path('H:/MinecraftMods/Pollution-Unofficial-1.20.1/build/libs/pollution-1.20.1-1.0.0-1.20.1-port.0.1.0.jar'),
        'sunlit-industry-compat-1.20.1-0.2.0.jar': PROJECT / 'build/libs/sunlit-industry-compat-1.20.1-0.2.0.jar',
        'Connector-1.0.0-beta.50+1.20.1+dev-00000000.jar': Path('H:/MinecraftMods/MiddleEarthForge1.20.1/[修复版信雅互联]Connector-1.0.0-beta.50+1.20.1+dev-00000000.jar'),
    }
    for p in overrides.values():
        if not p.is_file():
            raise SystemExit('Missing build: ' + str(p))
    DEST.mkdir()
    (DEST / 'mods').mkdir()
    manifest = {'source': str(PACK), 'baselineRuntime': str(BASE), 'jars': [], 'omitted': omitted,
                'differences': ['New isolated flat world, no production saves or Bukkit plugins.',
                                'Current client gameplay jars; server selection based on supplied production log.',
                                'New Pollution, AS and SunlitIndustryCompat builds; Connector matches production beta.50.']}
    for p in selected:
        source = overrides.pop(p.name, p)
        shutil.copy2(source, DEST / 'mods' / p.name)
        manifest['jars'].append({'file': p.name, 'source': str(source), 'sha256': digest(source)})
    for name, source in overrides.items():
        shutil.copy2(source, DEST / 'mods' / name)
        manifest['jars'].append({'file': name, 'source': str(source), 'sha256': digest(source)})
    for name in ('libraries', 'mohist-config'):
        if (BASE / name).exists():
            shutil.copytree(BASE / name, DEST / name)
    for name in ('config', 'kubejs', 'defaultconfigs'):
        if (PACK / name).exists():
            shutil.copytree(PACK / name, DEST / name,
                            ignore=shutil.ignore_patterns('__pycache__', '*.log', '*.bak', '*.zip', '*.7z'))
    runtime = BASE / 'rosetta-1.20.1-ecf8d5ad-server.jar'
    shutil.copy2(runtime, DEST / runtime.name)
    (DEST / 'RosettaRemoteDebugBridge').mkdir()
    (DEST / 'RosettaRemoteDebugBridge/remote-token.txt').write_text(secrets.token_hex(32), encoding='utf-8')
    (DEST / 'phase4-reports').mkdir()
    (DEST / 'eula.txt').write_text('eula=true\n', encoding='utf-8')
    (DEST / 'server.properties').write_text('''server-ip=127.0.0.1
server-port=25589
online-mode=false
enable-rcon=false
enable-query=false
level-name=phase4-isolated
level-type=minecraft:flat
generator-settings={"layers":[{"block":"minecraft:bedrock","height":1},{"block":"minecraft:dirt","height":2},{"block":"minecraft:grass_block","height":1}],"biome":"minecraft:plains","structures":{}}
view-distance=3
simulation-distance=3
spawn-protection=0
max-tick-time=-1
motd=ISOLATED PHASE4 TEST - LOOPBACK ONLY
allow-flight=true
''', encoding='utf-8')
    (DEST / 'README-PHASE4.md').write_text('''# Full-pack local acceptance server

Loopback only; fresh flat world; no production credentials, saves, plugins, or FRP.
Jars match the source manifest except the explicitly listed builds. Do not use this
world for gameplay. No automatic publication or deployment to the remote server.
Keep this process alive for all recipe/tag/loot reloads and probe iterations.
Startup evidence: phase4-reports/startup.jfr, gc.log, startup-threads-*.txt.
''', encoding='utf-8')
    (DEST / 'PHASE4_MANIFEST.json').write_text(json.dumps(manifest, ensure_ascii=False, indent=2), encoding='utf-8')
    print('Staged', len(manifest['jars']), 'top-level jars into', DEST)
    print('Omitted', len(omitted), 'client/non-production jars. Nested jars raise the FML mod count.')


if __name__ == '__main__':
    main()
