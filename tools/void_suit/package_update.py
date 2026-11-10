"""Package only the EU item / Void MekaSuit changes, including the required mod replacement."""
from pathlib import Path
import hashlib
import json
import shutil
import zipfile

ROOT = Path(__file__).resolve().parents[2]
PACK = Path('G:/MinecraftGames/Sunlit Valley(BaopuEdition)/.minecraft/versions/Society Sunlit Valley')
OUT = ROOT / 'build/delivery/Sunlit-EU-VoidSuit-0.3.0'
FILES = [
    'kubejs/server_scripts/industry/voidMekaSuitAssembly.js',
    'config/ftbquests/tools/staged_tiangong_progression.py',
    'config/ftbquests/tools/build_huixuan_tiangong_chapter.py',
    'config/ftbquests/tools/huixuan_tiangong_ids.json',
    'config/ftbquests/quests/chapters/huixuan_tiangong.snbt',
]
REMOVE = ['mods/sunlit-industry-compat-1.20.1-' + version + '.jar' for version in ('0.1.0', '0.2.0', '0.2.1')]


def main():
    sources = {name: PACK / name for name in FILES}
    sources['config/sunlitindustry-item-energy.toml'] = ROOT / 'pack/config/sunlitindustry-item-energy.toml'
    sources['mods/sunlit-industry-compat-1.20.1-0.3.0.jar'] = ROOT / 'build/libs/sunlit-industry-compat-1.20.1-0.3.0.jar'
    for relative, source in sources.items():
        destination = OUT / relative
        destination.parent.mkdir(parents=True, exist_ok=True)
        shutil.copy2(source, destination)
    manifest = {'version': '0.3.0', 'remove_before_start': REMOVE, 'files': [
        {'path': name, 'sha256': hashlib.sha256((OUT / name).read_bytes()).hexdigest()}
        for name in sorted(sources)]}
    (OUT / 'UPDATE-MANIFEST.json').write_text(json.dumps(manifest, indent=2) + '\n', encoding='utf-8')
    (OUT / 'INSTALL.txt').write_text(
        'Update both client and server during their normal restart window.\n'
        'Back up the replaced files, remove old sunlit-industry-compat jars listed in UPDATE-MANIFEST.json,\n'
        'then overlay this directory. Keep exactly one sunlitindustry jar in mods.\n'
        'A full restart is required for item capabilities and the assembly inheritance mixin.\n'
        'Do not deploy the KubeJS recipe alone to an older compat mod. The script checks for 0.3.0 support.\n'
        'The quest generator also uses the existing industrial_quest_support.py,\n'
        'industry-progression-manifest.json and build_astral_sorcery_chapter.py from the previous industry update.\n'
        'This package does not contain the local test bootstrap library, test worlds or credentials.\n', encoding='ascii')
    archive = OUT.parent / (OUT.name + '.zip')
    with zipfile.ZipFile(archive, 'w', zipfile.ZIP_DEFLATED) as output:
        for path in sorted(OUT.rglob('*')):
            if path.is_file():
                output.write(path, path.relative_to(OUT).as_posix())
    print(archive)
    print('Payload files:', len(sources), 'ZIP bytes:', archive.stat().st_size)


if __name__ == '__main__':
    main()
