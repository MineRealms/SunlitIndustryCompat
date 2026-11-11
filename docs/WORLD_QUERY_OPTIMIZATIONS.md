# World Query Adaptation

## GT Sunlight Query

Target: the exact `GTUtil.canSeeSunClearly(Level, BlockPos)` body in GTCEu 7.5.3.
The source was checked against `javap -c -p` on the mapped 7.5.3 artifact, not
inferred from GTO's modified GTCEu jar.

Native 7.5.3 repeats `pos.above()` up to five times, reads the same biome twice
on the ordinary clear-day path, and constructs `javd:void` even without JAVD.
`GtSunlightQuery` reuses one position and biome holder and a constant resource
ID. Non-JAVD night checks return after the native sky-access check because all
remaining native branches can only return false. No time, weather, biome/tag or
sky result survives a call. JAVD's inverted day rule is retained; other void
dimensions do not acquire special behavior.

`WorldPerformanceConfig.register()` registers
`sunlitindustry-world-performance.toml`; `gtSunlightQuery` defaults true.
The root integration must register `performance.world.GtSunlightMixin` behind
the dedicated-server, exact-GTCEu-7.5.3 and GTO/WarpLoad conflict gates.

The correctness probe switches the config between native and patched paths
over all loaded dimensions, day/night/rain/thunder states and covered/exposed
heights. It restores the config, time and exact weather interpolation fields.
The benchmark interleaves native/patched rounds on one loaded overworld chunk,
after warmup, with the same positions and state. No block edits are performed.
JAVD compatibility still needs a JAVD-enabled runtime; the minimal server alone
cannot exercise its registered biome branch.

## Excluded GTO Void Ticking

GTO's `ServerChunkCacheMixin` has several gameplay assumptions beyond caching:

- Its explicit `gtolib$isVoid()` flag returns no spawn state and forces game
  rule reads false. A void terrain generator or dimension name does not prove
  that this pack forbids natural spawning there.
- It replaces random-tick speed with 600 or 30 and skips `tickChunks` except
  every 200 or 10 ticks, respectively. Stock random ticks, crop growth, chunk
  work and weather progression would no longer follow the configured rules.
- Vanilla spawn state is externally observable through the chunk cache. Even
  when `doMobSpawning=false`, omitting state construction can change counters
  read by mods or debug tools. This module does not guess that nobody uses it.

No spawn-rule overrides, tick-rate changes, named-dimension shortcuts or whole
`ServerChunkCache` replacements are introduced. These paths need a pack-level
gameplay contract before they can be optimized by omitting work.

## Verification Status

Implementation and Rosetta probes are supplied for root integration. This
module has not launched Gradle or a server independently. Record correctness
and per-scenario latency/allocation results before claiming a measured win.
