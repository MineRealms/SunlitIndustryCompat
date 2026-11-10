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

Implementation checkpoint. Runtime comparison, reload regression results, migration matrix and benchmark
charts will be recorded in the subsequent verification commit. Unit tests compare native group maps and
iteration order across 300 deterministic random topologies and changes to color/distinct/bypass/order.
