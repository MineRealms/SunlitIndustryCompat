# GTCEu 7.5.3 Server Optimizations

## Scope

Target: the installed Sunlit Valley GTCEu 7.5.3, Forge 47.4.23 and KubeJS 2001.6.5-build.16.
Reference: local GTOCore/GTOLib optimization analysis and source. GTO's customized GTCEu 1.8.0
has a different API. The implementations here adapt the techniques to 7.5.3.

This set applies on dedicated servers only, and is skipped with GTOCore, GTOLib, WarpLoad, an
unsupported GTCEu version, or `-Dsunlitindustry.disableServerOptimizations=true`.
The Forge common config `sunlitindustry-server-performance.toml` also provides individual runtime
switches. No client rendering code is changed.

## Implemented

- Empty recipe I/O returns the same native `PASS_NO_CONTENTS` without creating a RecipeRunner
  or its temporary capability maps.
- Simulated content matching skips chance-roll preparation and tier calculation. It still requires
  all native ingredients, including non-consumed and chanced contents. Actual chance rolls remain native.
- AND-only conditions avoid building an unused OR grouping map. OR conditions retain the original path.
- RecipeRunner reuses handler grouping structures for stable machine topology. Output priority sorting
  still runs before validation; amounts, recipe matches, output room and voiding are never cached.
- Item and fluid parallel checks reuse the corresponding filtered grouping structures. Native inventory
  aggregation, NBT/tag matching and parallel calculation still run each time.

Each cache belongs to one MetaMachine, is lazy, and has at most six topology slots. Every reuse validates
ordered handler identity, group keys, bypass-distinct status and capability-filter membership. Changes
rebuild a fresh map. There is no hash-only validity test. Unknown mutable group classes and oversized
topologies use the original path. Unload, formation, invalidation and part unload drop the cache.
Retained grouping maps trade a bounded amount of per-machine memory for fewer temporary allocations;
lower post-GC heap is not guaranteed.

## WarpLoad Failure Boundaries

There is no disk recipe cache, generated JSON replay, recipe object reuse, built tag reuse, recipe packet
interception, shared worker pool or asynchronous inventory mutation. GT/KubeJS generation and data-pack
selection run normally on every reload. Deleted recipes cannot return from this topology cache.
The cache holds handlers, not recipes, tags, worlds in static maps, or previous resource-manager state.

## Verification

The final dedicated-server A/B run used an 18-mod-jar dependency closure with the installed GTCEu 7.5.3,
AE2 15.4.10, KubeJS 2001.6.5-build.16 and Forge 47.4.23. A test-only MixinExtras game library is excluded
from that jar count. Both phases used Java 17.0.18, `-Xms2G -Xmx6G`, the same flat `sunlit_perf_clean`
world and one KubeJS probe recipe. Five warm-up rounds precede four recorded rounds per scenario;
reported values are the median of rounds 1-3. The script executes on the Minecraft server thread
and compares native recipe paths directly. These are local microbenchmarks, not industrial-base TPS tests.

| Server-thread path | Baseline | Optimized | Change |
|---|---:|---:|---:|
| Empty recipe I/O latency | 305.78 ns/op | 74.20 ns/op | 75.7% lower |
| Empty recipe I/O allocation | 800 B/op | 24 B/op | 97.0% lower |
| Simulated recipe match latency | 13,252.77 ns/op | 11,200.18 ns/op | 15.5% lower |
| Simulated recipe match allocation | 10,944 B/op | 9,920 B/op | 9.4% lower |

The separate 14-handler grouping microbenchmark measured native rebuild against a warmed per-machine
topology cache. It reduced median grouping time by 75.8% in the baseline process and 71.5% in the
optimized process; warmed cache lookup allocated 0 B/op in both, versus 1,224 B/op for native rebuild.
The optimized run recorded 5,000 hits
for each exercised fast path and 5,000 handler-group cache hits, with zero fallbacks.

Both phases reported 42,493 registered recipes and exactly one KubeJS probe. The optimized phase's
three successive `/reload` cycles changed the probe input, removed the probe, then restored it;
counts were 42,493 -> 42,492 -> 42,493. Positive and negative ingredient matching also passed.
The KubeJS scripts loaded with zero script errors and zero failed recipes in the successful runs.
The raw captures and reproducible chart inputs are in `docs/benchmarks/gto-server-benchmark-20261111.*`.
The plot is `docs/benchmarks/gto-server-benchmark-20261111.png`.

GC logs and JFR recordings were collected, but their startup/reload capture windows differ, so they are archived at
`G:\\emoney-void-suit-test\\evidence` and are not used to claim a GC-rate improvement. The targeted
microbenchmark does show lower short-lived allocation for the exercised calls; a populated industrial
world is still needed to quantify whole-server TPS or heap behavior. During both phases, reload also
reproduced existing dedicated-server client-class load errors and optional TC4 smelting outputs missing
from the minimal mod set; these occurred with the optimization switch disabled as well.

Final charts use `clean-baseline-warm-benchmark.json` and `clean-optimized-warm-benchmark.json`.
Preliminary captures remain archived: their older staging script left Create and its navigation addon
in `mods` despite omitting them from its manifest. Those jars are now backed up outside `mods`, and
the stager rejects unexpected jars. A fresh flat world avoids the old world's missing-registry mappings.

The live-server probe `tools/server_perf/group_correctness.snippet` compared native group maps and
iteration order across 300 deterministic random topologies and 14,400 comparisons, including changes
to color, distinctness, bypass, handler order, capability-filter membership, handler addition/removal
and repeated cache hits. Evidence: `G:/emoney-void-suit-test/evidence/clean-group-correctness.json`.

AE2 additions and the broader migration review are documented in `AE2_SERVER_OPTIMIZATIONS.md` and
`GTO_MIGRATION_MATRIX.md`.
