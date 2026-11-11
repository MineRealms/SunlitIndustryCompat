# Reviewed cumulative release

These tools assemble the approved November 9-11 deliveries, not arbitrary dirty
instance files. They require the existing local pack, reviewed MEK deployment
payload, Boss/industrial candidate and the exact smoke-tested 0.8.0 artifact.
Connection credentials remain in the existing private Rosetta configuration.

```powershell
python -I tools/release/rosetta_release.py inspect --record .local/release-20261111
python -I tools/release/package_recent_update.py --out build/delivery/release-20261111
python -I tools/release/package_recent_update.py --out build/delivery/release-20261111 --server-before .local/release-20261111/server-before.json
python -I tools/release/rosetta_release.py stage --record .local/release-20261111 --package build/delivery/release-20261111/server-package.json
```

Use a fresh output directory for each release. Packaging verifies JSON, archive
CRC and each payload hash. Server capture retains changed text-file baselines;
staging refuses drift, backs up replaced server files and old compat artifacts,
verifies the uploaded archive and checks active files remain unchanged. It does
not install the update, reload recipes or restart the server.

The client archive includes the matching TianGong repair jar so older clients can
catch up. The server delta omits it when the inspected SHA-256 already matches.
Both sides must keep only one SunlitIndustryCompat jar after stopped installation.
