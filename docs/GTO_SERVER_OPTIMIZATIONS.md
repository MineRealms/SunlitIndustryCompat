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

## WarpLoad Failure Boundaries

There is no disk recipe cache, generated JSON replay, recipe object reuse, built tag reuse, recipe packet
interception, shared worker pool or asynchronous inventory mutation. GT/KubeJS generation and data-pack
selection run normally on every reload. Deleted recipes cannot return from this topology cache.
The cache holds handlers, not recipes, tags, worlds in static maps, or previous resource-manager state.

## Verification

The dedicated-server A/B run used a minimal 16-mod dependency closure with the installed GTCEu 7.5.3,
KubeJS 2001.6.5-build.16 and Forge 47.4.23. Both phases used Java 17.0.18, `-Xms2G -Xmx6G`, the
same flat test world and one KubeJS probe recipe. Four measurement rounds were run per scenario after
warm-up; reported values are the median of rounds 1-3. The script executes on the Minecraft server
thread and compares the native recipe paths directly.

| Server-thread path | Baseline | Optimized | Change |
|---|---:|---:|---:|
| Empty recipe I/O latency | 478.57 ns/op | 101.16 ns/op | 78.9% lower |
| Empty recipe I/O allocation | 800 B/op | 24 B/op | 97.0% lower |
| Simulated recipe match latency | 11,483.48 ns/op | 10,550.03 ns/op | 8.1% lower |
| Simulated recipe match allocation | 10,976 B/op | 9,616 B/op | 12.4% lower |

The separate 14-handler grouping microbenchmark measured native rebuild against a warmed per-machine
topology cache. It reduced median grouping time by 87.6% in the baseline process and 81.3% in the
optimized process; warmed cache lookup allocated 0 B/op in both. The optimized run recorded 5,000 hits
for each exercised fast path and 5,000 handler-group cache hits, with zero fallbacks.

Both phases reported 43,489 registered recipes and exactly one KubeJS probe. Three successive
`/reload` cycles per phase preserved those counts; the KubeJS scripts loaded with zero script errors.
The raw captures and reproducible chart inputs are in `docs/benchmarks/gto-server-benchmark-20261111.*`.
The plot is `docs/benchmarks/gto-server-benchmark-20261111.png`.

GC logs and JFR recordings were collected, but their capture windows differ (900 s baseline JFR and
563 s optimized JFR, with different time spent in startup/reload), so they are archived at
`G:\\emoney-void-suit-test\\evidence` and are not used to claim a GC-rate improvement. The targeted
microbenchmark does show lower short-lived allocation for the exercised calls; a populated industrial
world is still needed to quantify whole-server TPS or heap behavior. During both phases, reload also
reproduced existing dedicated-server client-class load errors and a TianGong wireless-connector loot
table error; these occurred with the optimization switch disabled as well.

Unit tests compare native group maps and iteration order across 300 deterministic random topologies
and changes to color/distinct/bypass/order.
