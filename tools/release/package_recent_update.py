"""Package the reviewed industry updates without collecting arbitrary instance changes."""
import argparse
from datetime import datetime, timezone
import hashlib
import json
from pathlib import Path
import subprocess
import zipfile

ROOT = Path(__file__).resolve().parents[2]
WORKSPACE = ROOT.parent
DEFAULT_PACK = Path('G:/MinecraftGames/Sunlit Valley(BaopuEdition)/.minecraft/versions/Society Sunlit Valley')
VERSION = '0.8.0'
RELEASE = '20261111-' + VERSION
JAR = 'sunlit-industry-compat-1.20.1-' + VERSION + '.jar'
EXPECTED_JAR_SHA = 'be308365feaf4c0a66d49ce0c7e4ec49b898845d8b4358919bc5af8c3a6b8ecd'


def sha(data):
    return hashlib.sha256(data).hexdigest()


def sources(pack):
    names = [
        'config/sunlitindustry-common.toml',
        'config/Mekanism/generators.toml',
        'defaultconfigs/zmod-industrial-machines.toml',
        'config/ftbquests/quests/chapters/gt_nuclear_industry.snbt',
        'config/ftbquests/quests/chapters/huixuan_tiangong.snbt',
        'config/ftbquests/quests/chapters/cataclysmancy.snbt',
        'kubejs/server_scripts/industry/gtTiangongProgression.js',
        'kubejs/server_scripts/industry/voidMekaSuitAssembly.js',
        'kubejs/server_scripts/mek/lockMekBehindGT.js',
        'kubejs/server_scripts/tc/tcBossAndIndustrial.js',
        'kubejs/server_scripts/tc/tcCataclysm.js',
        'kubejs/server_scripts/blockEvents/customBlockEntityBroken.js',
        'kubejs/startup_scripts/registration/registerIndustryCores.js',
        'kubejs/startup_scripts/registration/registerJadeData.js',
        'kubejs/config/sunlit-boss-awakening.json',
        'kubejs/config/sunlit-industrial-infusion.json',
        'kubejs/assets/society/lang/zh_cn.json',
        'kubejs/assets/society/lang/en_us.json',
        'kubejs/assets/society/textures/quests/cataclysm/cataclysm_temple.png',
        'kubejs/assets/ftbquests/textures/texture_1.png',
        'kubejs/data/society/thaumcraft/object_aspects/cataclysm.json',
        'kubejs/data/society/thaumcraft/research/cataclysm.json',
        'mods/zmod-forge-1.20.1-1.40.03-all.jar',
    ]
    for name in [
        'build_gt_nuclear_industry_chapter.py', 'build_huixuan_tiangong_chapter.py',
        'gt_nuclear_industry_ids.json', 'huixuan_tiangong_ids.json',
        'industrial_quest_support.py', 'industry-progression-manifest.json',
        'staged_tiangong_progression.py', 'build_astral_sorcery_chapter.py',
        'build_cataclysmancy_chapter.py', 'cataclysmancy_ids.json',
    ]:
        names.append('config/ftbquests/tools/' + name)
    result = {name: pack / name for name in names}
    for folder in ['kubejs/data/society/recipes/industry', 'kubejs/data/zmod/recipes']:
        for path in sorted((pack / folder).rglob('*.json')):
            result[path.relative_to(pack).as_posix()] = path
    for path in sorted((ROOT / 'pack/config').glob('sunlitindustry*.toml')):
        result['config/' + path.name] = path
    # This payload preserves the background already installed on the production server.
    mek = WORKSPACE / 'sunlit-mek-autolayout-20261110/server-deployment/payload'
    for name in [
        'config/ftbquests/quests/chapters/mekanism.snbt',
        'config/ftbquests/tools/build_mek_chapter.py',
        'config/ftbquests/tools/mek_layout.json',
        'config/ftbquests/tools/quest_layout.py',
        'config/ftbquests/tools/quest_layout.mjs',
    ]:
        result[name] = mek / name
    result['mods/' + JAR] = ROOT / 'build/libs' / JAR
    return result


def collect(pack):
    selected = sources(pack)
    entries = {}
    for name, path in selected.items():
        if not path.is_file():
            raise FileNotFoundError(str(path))
        data = path.read_bytes()
        if path.suffix == '.json':
            json.loads(data.decode('utf-8-sig'))
        entries[name] = data
    if sha(entries['mods/' + JAR]) != EXPECTED_JAR_SHA:
        raise ValueError('Compat artifact differs from the verified review-smoke artifact')
    for name in ['sunlit-boss-awakening.json', 'sunlit-industrial-infusion.json']:
        relative = 'kubejs/config/' + name
        approved = WORKSPACE / 'sunlit-cataclysm-infusion-20261110/candidate' / relative
        if entries[relative] != approved.read_bytes():
            raise ValueError('Boss/infusion configuration differs from reviewed delivery: ' + name)
    relative = 'kubejs/server_scripts/tc/tcBossAndIndustrial.js'
    if entries[relative] != (WORKSPACE / 'sunlit-cataclysm-infusion-20261110/candidate' / relative).read_bytes():
        raise ValueError('Boss/infusion script differs from reviewed delivery')
    return entries


def write_archive(out, side, entries, before=None):
    label = 'Sunlit-' + side.title() + '-Delta-' + RELEASE
    stage = out / label
    if stage.exists():
        raise FileExistsError('Use a fresh output directory: ' + str(stage))
    stage.mkdir(parents=True)
    revision = subprocess.check_output(['git', 'rev-parse', 'HEAD'], cwd=ROOT, text=True).strip()
    before_files = {row['path']: row for row in (before or {}).get('files', [])}
    rows = []
    for name, data in sorted(entries.items()):
        target = stage / name
        target.parent.mkdir(parents=True, exist_ok=True)
        target.write_bytes(data)
        row = {'path': name, 'bytes': len(data), 'sha256': sha(data)}
        if before is not None:
            old = before_files.get(name)
            if old is None:
                raise ValueError('Missing inspected server path: ' + name)
            row['beforeSHA256'] = old.get('sha256')
        rows.append(row)
    manifest = {
        'release': RELEASE, 'side': side, 'sourceCommit': revision,
        'createdUTC': datetime.now(timezone.utc).isoformat(),
        'requiresFullRestart': True,
        'newExperimentsEnabled': False,
        'removeOldModGlob': 'mods/sunlit-industry-compat-1.20.1-*.jar',
        'keepMod': 'mods/' + JAR,
        'files': rows,
    }
    (stage / 'UPDATE-MANIFEST.json').write_text(json.dumps(manifest, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')
    (stage / 'SHA256SUMS.txt').write_text(''.join(row['sha256'] + '  ' + row['path'] + '\n' for row in rows), encoding='utf-8')
    (stage / 'UPDATE-NOTES.zh-CN.md').write_bytes((ROOT / 'tools/release/UPDATE-NOTES.zh-CN.md').read_bytes())
    if before is not None:
        (stage / 'SERVER-BEFORE.json').write_text(json.dumps(before, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')
    archive = out / (label + '.zip')
    with zipfile.ZipFile(archive, 'w', compression=zipfile.ZIP_DEFLATED) as output:
        for path in sorted(stage.rglob('*')):
            if path.is_file():
                output.write(path, path.relative_to(stage).as_posix())
    with zipfile.ZipFile(archive) as verify:
        if verify.testzip() is not None:
            raise ValueError('ZIP CRC check failed')
        for row in rows:
            if sha(verify.read(row['path'])) != row['sha256']:
                raise ValueError('ZIP hash mismatch: ' + row['path'])
    return {'archive': str(archive), 'stage': str(stage), 'sha256': sha(archive.read_bytes()),
            'bytes': archive.stat().st_size, 'payloadFiles': len(rows)}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--pack', type=Path, default=DEFAULT_PACK)
    parser.add_argument('--out', type=Path, required=True)
    parser.add_argument('--server-before', type=Path)
    args = parser.parse_args()
    entries = collect(args.pack)
    if args.server_before:
        before = json.loads(args.server_before.read_text(encoding='utf-8-sig'))
        current = {row['path']: row for row in before['files']}
        delta = {name: data for name, data in entries.items()
                 if current.get(name, {}).get('sha256') != sha(data)}
        result = write_archive(args.out, 'server', delta, before)
    else:
        result = write_archive(args.out, 'client', entries)
    args.out.mkdir(parents=True, exist_ok=True)
    (args.out / ('server-package.json' if args.server_before else 'client-package.json')).write_text(
        json.dumps(result, indent=2) + '\n', encoding='utf-8')
    print(json.dumps(result, indent=2))


if __name__ == '__main__':
    main()
