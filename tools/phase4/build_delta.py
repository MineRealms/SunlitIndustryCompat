"""Deterministic phase-4 supplement. Explicit files, never mtime/git-only discovery.

Use --candidate until the acceptance record is complete. Does not publish or deploy.
"""
from pathlib import Path
import hashlib
import json
import zipfile
import sys

PACK = Path('G:/MinecraftGames/Sunlit Valley(BaopuEdition)/.minecraft/versions/Society Sunlit Valley')
RUN = Path('G:/emoney-full-phase4-20261104')
OUT = Path('H:/MinecraftMods/dist/sunlit-phase4-20261104')
STAMP = (2026, 11, 4, 0, 0, 0)
JARS = {
    'AstralSorcery-1.20.1.0.jar': 'H:/MinecraftMods/AstralSorcery-1.20.1/build/libs/AstralSorcery-1.20.1.0.jar',
    '(59+)污染 Pollution-1.20.1-1.0.0-port.0.1.0.jar': 'H:/MinecraftMods/Pollution-Unofficial-1.20.1/build/libs/pollution-1.20.1-1.0.0-1.20.1-port.0.1.0.jar',
    'sunlit-industry-compat-1.20.1-0.2.0.jar': 'H:/MinecraftMods/SunlitIndustryCompat/build/libs/sunlit-industry-compat-1.20.1-0.2.0.jar',
    'VanillaBackport-forge-1.20.1-1.1.7.10.jar': str(RUN / 'mods/VanillaBackport-forge-1.20.1-1.1.7.10.jar'),
    'SignPicture-Rebornified-1.0.0.jar': 'H:/MinecraftMods/SignPicture-Rebornified-1.20.1/build/libs/SignPicture-Rebornified-1.0.0.jar',
}
FILES = ['config/isorropia-common.toml', 'config/ftbquests/quests/chapters/thaumic_industry.snbt',
         'kubejs/server_scripts/tags/fixVintageDelightTags.js']
DOCS = ['PHASE4_ACCEPTANCE.md', 'THAUMIC_CIRCUITS.md', 'MAGIC_GT_SCALING.md', 'AS_LUMEN_BINDING.md']


def sha(data): return hashlib.sha256(data).hexdigest()


def main():
    candidate = '--candidate' in sys.argv
    status = 'CANDIDATE - NOT FULLY VALIDATED' if candidate else 'VALIDATED'
    acceptance = (PACK / 'docs/PHASE4_ACCEPTANCE.md').read_text(encoding='utf-8')
    if not candidate and ('- [ ]' in acceptance or '进行中' in acceptance):
        raise SystemExit('Unfinished acceptance record: only --candidate packaging is allowed')
    entries = {'mods/' + name: Path(source).read_bytes() for name, source in JARS.items()}
    # Always include ore-pig policy even if untracked; no git commit is required to package it.
    for rel in FILES:
        entries[rel] = (PACK / rel).read_bytes()
    for path in sorted((PACK / 'kubejs/data/me/loot_tables/blocks').glob('*.json')):
        entries[path.relative_to(PACK).as_posix()] = path.read_bytes()
    for path in sorted((PACK / 'kubejs/server_scripts/gt').glob('phase4*.js')):
        entries[path.relative_to(PACK).as_posix()] = path.read_bytes()
    for name in DOCS: entries['docs/' + name] = (PACK / 'docs' / name).read_bytes()
    readme = f'''# Sunlit Valley 第四批补充差分包

状态：**{status}**。

这是原20261104第三批差分包的补充，不是完整整合包。精确内容见MANIFEST.json。
包含必需的SunlitIndustryCompat与矿猪白名单，不再按jar修改时间猜测部署范围。

## 安装（由服主手动执行）
1. 正常save/stop，完整备份world、mods、config和kubejs，不在运行中替换jar。
2. 客户端与服务端使用同一版AS/Pollution/兼容mod。AS新增属性同步源，旧客户端不能当作兼容。
3. 覆盖同名jar，删除SunlitIndustryCompat 0.1.0等旧版本以免重复modid。
4. 合并config/isorropia-common.toml：7矿白名单，dust_fallback=false，保留服主其它自定义配置。
5. 更新kubejs和任务章节，启动后按docs/PHASE4_ACCEPTANCE.md复核。
6. VanillaBackport/SignPicture是已有专用服兼容修复；若服务器已使用自有更新版，不要仅凭同名覆盖，先对SHA。

启动网络超时建议（JDK URLConnection使用默认值的请求）：
-Dsun.net.client.defaultConnectTimeout=10000 -Dsun.net.client.defaultReadTimeout=10000
不保证覆盖所有mod自带HTTP客户端，也不替代正常网络配置。

## 回滚
还原同一次备份的jar/config/kubejs/世界；新装备含绑定数据，不能承诺降级仍保留所有新效果。
本包不含凭据、远端桥token、FRP配置、玩家数据或生产存档。
'''
    entries['README.md'] = readme.encode('utf-8')
    manifest = {'status': status, 'files': [{'path': n, 'sha256': sha(b), 'bytes': len(b)} for n,b in sorted(entries.items())]}
    entries['MANIFEST.json'] = (json.dumps(manifest, ensure_ascii=False, indent=2)+'\n').encode('utf-8')
    entries['SHA256SUMS.txt'] = ''.join(f'{sha(b)}  {n}\n' for n,b in sorted(entries.items())).encode('utf-8')
    OUT.mkdir(parents=True, exist_ok=True)
    target = OUT / ('sunlit-phase4-candidate.zip' if candidate else 'sunlit-phase4-validated.zip')
    with zipfile.ZipFile(target, 'w') as z:
        for name, data in sorted(entries.items()):
            info=zipfile.ZipInfo(name,STAMP); info.compress_type=zipfile.ZIP_DEFLATED; info.external_attr=0o644<<16
            z.writestr(info,data)
    with zipfile.ZipFile(target) as z:
        assert all(z.read(n)==b for n,b in entries.items())
    print(target,'entries',len(entries),'sha256',sha(target.read_bytes()))


if __name__ == '__main__': main()
