"""Stage an isolated Forge/Rosetta test using mandatory dependencies from the live client."""
from pathlib import Path
import hashlib
import io
import json
import shutil
import socket
import zipfile
try:
    import tomllib
except ImportError:
    import tomli as tomllib

PACK = Path('G:/MinecraftGames/Sunlit Valley(BaopuEdition)/.minecraft/versions/Society Sunlit Valley')
RUN = Path('G:/emoney-void-suit-test')
HERE = Path(__file__).resolve().parents[2]
SEEDS = {'gtceu', 'mekanism', 'mekanismgenerators', 'zmod', 'thaumcraft',
         'thaumictinkerer', 'tainted_magic', 'forbidden_magic', 'kubejs', 'ldlib', 'ae2'}
BUILTINS = {'forge', 'minecraft', 'java'}


def metadata(path):
    with zipfile.ZipFile(path) as archive:
        ids, needs = archive_metadata(archive)
        roots = set()
        if 'META-INF/mods.toml' in archive.namelist():
            roots.update(entry['modId'] for entry in tomllib.loads(archive.read('META-INF/mods.toml').decode('utf-8-sig')).get('mods', []))
        return ids, needs, roots


def archive_metadata(archive):
    ids, needs = set(), set()
    if 'META-INF/mods.toml' in archive.namelist():
        data = tomllib.loads(archive.read('META-INF/mods.toml').decode('utf-8-sig'))
        ids.update(entry['modId'] for entry in data.get('mods', []))
        needs.update(dep['modId'] for entries in data.get('dependencies', {}).values() for dep in entries
                     if dep.get('mandatory', False) and dep.get('side', 'BOTH') != 'CLIENT')
    if 'META-INF/jarjar/metadata.json' in archive.namelist():
        for entry in json.loads(archive.read('META-INF/jarjar/metadata.json')).get('jars', []):
            with zipfile.ZipFile(io.BytesIO(archive.read(entry['path']))) as nested:
                child_ids, child_needs = archive_metadata(nested)
                ids.update(child_ids)
                needs.update(child_needs)
    return ids, needs


def main():
    if (RUN / 'server.pid').exists():
        raise SystemExit('PID marker exists; verify the isolated server has exited before restaging')
    try:
        connection = socket.create_connection(('127.0.0.1', 48793), timeout=1)
    except OSError:
        pass
    else:
        connection.close()
        raise SystemExit('Stop the isolated test server before restaging it')
    marker = RUN / 'VOID_SUIT_TEST.json'
    if RUN.exists() and not marker.exists():
        raise SystemExit('Refusing to stage over an unrelated directory')
    providers = {}
    primary = set()
    records = {}
    for path in sorted((PACK / 'mods').glob('*.jar')):
        record = metadata(path)
        if not record:
            continue
        records[path] = record
        for mod in record[0]:
            if mod not in providers or (mod in record[2] and mod not in primary):
                providers[mod] = path
            if mod in record[2]:
                primary.add(mod)
    chosen = {providers[mod] for mod in SEEDS}
    covered = set(BUILTINS)
    pending = set()
    for path in chosen:
        ids, needs = records[path][:2]
        covered.update(ids)
        pending.update(needs)
    while pending:
        mod = min(pending)
        pending.remove(mod)
        if mod in covered:
            continue
        if mod not in providers:
            raise SystemExit('Missing required mod: ' + mod)
        path = providers[mod]
        ids, needs = records[path][:2]
        chosen.add(path)
        covered.update(ids)
        pending.update(needs - covered)
    runmods = RUN / 'mods'
    runmods.mkdir(parents=True, exist_ok=True)
    chosen.add(Path('G:/emoney-full-phase4-20261104/mods/rosetta_remote_debug_bridge-1.0.0.jar'))
    chosen.add(HERE / 'build/libs/sunlit-industry-compat-1.20.1-0.6.0.jar')
    expected = {path.name for path in chosen} | {'test-mixinextras-core-0.5.3.jar'}
    extras = [path.name for path in runmods.glob('*.jar')
              if path.name not in expected and not path.name.startswith('sunlit-industry-compat-')]
    if extras:
        raise SystemExit('Unexpected mod jars in isolated server: ' + ', '.join(sorted(extras)))
    for old in runmods.glob('sunlit-industry-compat-*.jar'):
        old.unlink()
    manifest = []
    for path in sorted(chosen):
        shutil.copy2(path, runmods / path.name)
        manifest.append({'file': path.name, 'sha256': hashlib.sha256(path.read_bytes()).hexdigest()})
    # This pack's nested MixinExtras core lacks FMLModType. The standalone test launcher needs it
    # exposed as a game library; HMCL/Rosetta supply the core through their existing launch path.
    with zipfile.ZipFile(providers['gtceu']) as gt:
        with zipfile.ZipFile(io.BytesIO(gt.read('META-INF/jarjar/mixinextras-forge-0.5.3.jar'))) as wrapper:
            with zipfile.ZipFile(io.BytesIO(wrapper.read('META-INF/jars/MixinExtras-0.5.3.jar'))) as core:
                with zipfile.ZipFile(runmods / 'test-mixinextras-core-0.5.3.jar', 'w', zipfile.ZIP_DEFLATED) as library:
                    for info in core.infolist():
                        payload = core.read(info.filename)
                        if info.filename == 'META-INF/MANIFEST.MF':
                            payload = b'Manifest-Version: 1.0\r\nFMLModType: GAMELIBRARY\r\nAutomatic-Module-Name: MixinExtras\r\n\r\n'
                        library.writestr(info, payload)
    forge = Path('H:/MinecraftMods/_prodtest_hxtg/libraries')
    arguments = (forge / 'net/minecraftforge/forge/1.20.1-47.4.23/win_args.txt').read_text()
    arguments = '\n'.join('-DlibraryDirectory=' + forge.as_posix() if line.startswith('-DlibraryDirectory=')
                          else line.replace('libraries/', forge.as_posix() + '/') for line in arguments.splitlines()) + '\n'
    (RUN / 'forge-test-args.txt').write_text(arguments, encoding='utf-8')
    (RUN / 'eula.txt').write_text('eula=true\n', encoding='ascii')
    scripts = RUN / 'kubejs/server_scripts/industry'
    scripts.mkdir(parents=True, exist_ok=True)
    shutil.copy2(PACK / 'kubejs/server_scripts/industry/voidMekaSuitAssembly.js', scripts)
    shutil.copy2(HERE / 'tools/server_perf/perf_reload_probe.js', RUN / 'kubejs/server_scripts/00_perf_reload_probe.js')
    probe_config = RUN / 'kubejs/config'
    probe_config.mkdir(exist_ok=True)
    shutil.copy2(HERE / 'tools/server_perf/perf_reload_probe.json', probe_config / 'sunlit_perf_probe.json')
    config = RUN / 'config'
    config.mkdir(exist_ok=True)
    shutil.copy2(PACK / 'config/gtceu.yaml', config / 'gtceu.yaml')
    shutil.copy2(PACK / 'config/sunlitindustry-common.toml', config)
    shutil.copy2(HERE / 'pack/config/sunlitindustry-server-performance.toml', config)
    (RUN / 'server.properties').write_text(
        'server-ip=127.0.0.1\nserver-port=25584\nonline-mode=false\nenable-rcon=false\n'
        'level-name=sunlit_perf_clean\nlevel-type=minecraft:flat\ngenerate-structures=false\n'
        'view-distance=3\nsimulation-distance=3\nspawn-protection=0\nmax-tick-time=-1\n'
        'allow-flight=true\nmax-players=2\n', encoding='ascii')
    (RUN / 'evidence').mkdir(exist_ok=True)
    marker.write_text(json.dumps({'purpose': 'isolated GT server optimization and KubeJS reload benchmark', 'mods': manifest}, indent=2), encoding='utf-8')
    print('Staged', len(chosen), 'mods in', RUN)


if __name__ == '__main__':
    main()
