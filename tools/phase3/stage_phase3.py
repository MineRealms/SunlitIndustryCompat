#!/usr/bin/env python3
"""Phase 3 staging: deploy the rebuilt Pollution jar + missing TC addons to the bio test server.

- Backs up every touched mod jar to .local/phase3-backup-<ts>/
- Replaces the pollution jar IN PLACE (same filename, avoids duplicate-modid boot failure)
- Installs thaumadds / tainted-magic / thaumic-tinkerer (20759 pin) into server mods/
- Writes .local/phase3-backup-<ts>/MANIFEST.json with sha256 of every staged file

Run while the server is STOPPED (README-BIO-TEST: never swap jars on a running server).
"""
import hashlib
import json
import shutil
import sys
import time
from pathlib import Path

SERVER = Path("G:/emoney-bio-test")
SERVER_MODS = SERVER / "mods"
PACK_MODS = Path("G:/MinecraftGames/Sunlit Valley(BaopuEdition)/.minecraft/versions/Society Sunlit Valley/mods")
PACK_OVERRIDES = Path("G:/MinecraftGames/Sunlit Valley(BaopuEdition)/.minecraft/versions/Society Sunlit Valley/overrides/mods")
POLLUTION_BUILD = Path("H:/MinecraftMods/Pollution-Unofficial-1.20.1/build/libs")
BACKUP_ROOT = Path("H:/MinecraftMods/SunlitIndustryCompat/.local")

POLLUTION_SERVER_NAME = "(59+)污染 Pollution-1.20.1-1.0.0-port.0.1.0.jar"

ADDONS = [
    (PACK_MODS, "tainted-magic-forge-8.1.1-1.20.1-port.0.1.0-20759.jar",
     "tainted-magic-forge-8.1.1-1.20.1-port.0.1.0-20759.jar"),
    (PACK_MODS, "thaumic-tinkerer-forge-2.5-1.20.1-port.0.1.0-20759.jar",
     "thaumic-tinkerer-forge-2.5-1.20.1-port.0.1.0-20759.jar"),
    (PACK_OVERRIDES, "thaumadds-1.20.1-forge-0.1.0.jar",
     "thaumadds-1.20.1-forge-0.1.0.jar"),
]


def sha256(p):
    h = hashlib.sha256()
    with open(p, "rb") as f:
        for chunk in iter(lambda: f.read(1 << 20), b""):
            h.update(chunk)
    return h.hexdigest()


def main():
    ts = time.strftime("%Y%m%d-%H%M%S")
    backup = BACKUP_ROOT / ("phase3-backup-" + ts)
    backup.mkdir(parents=True, exist_ok=True)

    new_pollution = POLLUTION_BUILD / "pollution-1.20.1-1.0.0-1.20.1-port.0.1.0.jar"
    if not new_pollution.is_file():
        print("FATAL: build jar missing:", new_pollution)
        return 1
    target_pollution = SERVER_MODS / POLLUTION_SERVER_NAME
    if not target_pollution.is_file():
        print("FATAL: expected existing server jar not found:", target_pollution)
        return 1

    manifest = {"timestamp": ts, "backup_dir": str(backup), "changes": []}

    shutil.copy2(target_pollution, backup / POLLUTION_SERVER_NAME)
    shutil.copy2(new_pollution, target_pollution)
    manifest["changes"].append({
        "file": POLLUTION_SERVER_NAME, "action": "replaced",
        "old_sha256": sha256(backup / POLLUTION_SERVER_NAME),
        "new_sha256": sha256(target_pollution),
        "source": str(new_pollution),
    })
    print("replaced", POLLUTION_SERVER_NAME)

    for src_dir, src_name, dst_name in ADDONS:
        src = src_dir / src_name
        dst = SERVER_MODS / dst_name
        if not src.is_file():
            print("SKIP (source missing):", src)
            continue
        if dst.is_file():
            shutil.copy2(dst, backup / dst_name)
            action = "replaced"
        else:
            action = "installed"
        shutil.copy2(src, dst)
        manifest["changes"].append({
            "file": dst_name, "action": action,
            "new_sha256": sha256(dst),
            "source": str(src),
        })
        print(action + ":", dst_name)

    with open(backup / "MANIFEST.json", "w", encoding="utf-8") as f:
        json.dump(manifest, f, ensure_ascii=False, indent=2)
    print("manifest ->", backup / "MANIFEST.json")
    return 0


if __name__ == "__main__":
    sys.exit(main())
