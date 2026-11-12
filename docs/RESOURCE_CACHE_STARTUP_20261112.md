# Resource cache startup regression (2026-11-12)

## Evidence from the supplied server logs

The archive `logs (4).7z` contains `latest.log` and `debug.log`, captured at
15:39:49. The server runs Forge 47.4.13/Mohist on Windows Server 2022, Java
21.0.11, with 444 mods reported by the loader.

- Server script loading completes at 14:20:26: 257/257 scripts, 1.355 seconds,
  zero script-load errors or warnings. This does not prove recipe processing
  completed; the recipe event was only beginning.
- Dynamic resource generation completes at 14:21:06.
- Fluid, block, biome and item tag loaders continue making progress through
  14:44:42.
- Mantle loot table injection continues between 15:00:13 and 15:34:51.
- The main thread resumes tag-result handling at 15:39:49.

This is prolonged resource-reload slowdown, not evidence of a continuous
deadlock starting at 14:20. The last logger is not necessarily the cause.
The resource Mixin is present in the debug log, but this alone does not prove
its runtime configuration was enabled. No GC log, heap flags or thread dump
was supplied. Attribution of this particular server stall therefore remains
unconfirmed.

## Confirmed cache defect

`ImmutableArchiveResourcesMixin` asks the store for a snapshot on every
resource lookup, including missing resources. The previous LRU rehydrated an
entire snapshot whenever an archive had been evicted. Disk reading, checksum
validation and object reconstruction occur under a store-wide monitor.

For a cyclic working set larger than the memory budget this can produce a
disk reload and large allocations on virtually every query, while other
resource workers wait on the same monitor. Emitted resource suppliers can
also retain their byte arrays after store eviction, so the store budget is
not a bound on total live JVM memory during a reload.

A read-only metadata scan of the local modpack's 424 top-level jars estimated
199.04 MiB of indexed snapshots, against the 32 MiB default budget. This is
not a measurement of the remote server's working set or heap. The server has
additional nested mods and may have different configuration.

## Reproduction

A standalone Java 17 probe compiled the production `ArchiveSnapshotStore`
against the installed mapped Minecraft jar. Three synthetic archives hold
4.50 MiB of snapshots in total. After initial admission, the probe performs
60 round-robin lookups of a missing resource. Native fallback uses `ZipFile`
without decompressing unrelated resources.

| Implementation | Budget | Disk snapshot reads | Temporary allocation | Elapsed |
| --- | --- | --- | --- | --- |
| Previous LRU | 4 MiB | 60 | 181.14 MiB | 998.99 ms |
| Fixed admission | 4 MiB | 0 | 0.06 MiB | 6.79 ms |
| Resident working set | 8 MiB | 0 | <0.01 MiB | 0.21 ms |

These are single-run diagnostic measurements, not a statistical benchmark or
a prediction of whole-server startup improvement. The reproducible regression
test asserts the important counters and fallback behavior without timing
thresholds.

## Fix in 0.8.1

- Archive snapshots are opt-in. Both the code default and pack config set
  `immutableArchiveCache = false`; existing explicit `true` values remain true.
- An archive can be loaded from disk or built only once per resource-reload
  epoch. After eviction it uses the original resource path until the next epoch.
- Attempts are capped at 2048 distinct digests per epoch; further misses use
  the original path. Resident snapshots remain usable.
- Forge's `AddReloadListenerEvent` resets admission at the start of a server
  resource reload. Existing immutable snapshots remain reusable, and a later
  reload can retry evicted archives. Server stop clears retained snapshots and
  admission state.
- `-Dsunlitindustry.disableArchiveCache=true` skips the resource Mixin at
  startup and also disables its runtime path. It does not disable GT/AE2
  gameplay or unrelated performance features.
- No parsed recipes, tags or KubeJS results are replayed. Dynamic packs remain
  outside the archive cache.

## Verification and operational boundary

Resource tests cover disk round trips, content replacement, corruption,
resource boundaries, immutable suppliers, oversize admission, eviction,
cyclic over-capacity access and admission reset. The over-capacity regression
requires zero snapshot disk reloads for repeated accesses in one epoch, while
a subsequent epoch admits an evicted archive again.

All 11 resource-cache/source-identity tests passed. The 0.8.1 `jarJar` artifact
and its reobfuscation completed successfully. This verification does not
replace an actual server resource-reload test of the new lifecycle hooks.

The remote server was not stopped, restarted or modified. Its Rosetta endpoint
did not return a valid response during this investigation. No full-modpack
startup speedup or resolution of the reported incident is claimed.

For a decisive comparison, disable the archive cache in the server's
`config/sunlitindustry-resources.toml`, or add the emergency JVM property above
when using 0.8.1. Capture three `jcmd <pid> Thread.print -l` samples during a
stall, plus `VM.flags`, `GC.heap_info` and GC logging. Workers blocked in
`ArchiveSnapshotStore.get` while its owner reads/builds a snapshot would
directly implicate this path. If the cache is disabled and the stall persists,
use the captures to investigate GC, paging, disk access and other reload hooks.
