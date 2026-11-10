# AE2 15.4.10 Server Optimizations

## Scope

Adapted from the singleton-key approach in GTOCore's `ae2/stacks/AEItemKeyMixin.java` and
`AEFluidKeyMixin.java`, with narrower entry points for this pack's Forge capabilities and AE2 addons.
The implementation applies only on dedicated servers with AE2 15.4.10 and GTCEu 7.5.3. The global
server-optimization switch, GTOCore/GTOLib/WarpLoad exclusions and per-feature config switches apply.

## Implementation

- `AEItemKey.of(ItemLike)` lazily reuses one default key per registry Item.
- `AEFluidKey.of(Fluid, CompoundTag)` reuses one default key only when the tag is null. This covers
  both `of(Fluid)` and ordinary untagged `FluidStack` conversion through their native delegation.
- Tagged fluids still use AE2's normal NBT copy. All ItemStack, item-NBT, packet and tag decoding
  paths remain native, including Forge capability serialization and stack-dependent limits.
- Each registry singleton has one volatile reference. The cache is bounded by registry size, is
  populated only on demand, and holds no Level, recipe, resource manager or mutable input stack.
- The item/fluid key type registry, storage routing, fuzzy matching, crafting planner and cell NBT
  format are unchanged. Applied Mekanistics/Botanics/Astral key types keep their normal paths.

The canonical keys remain reachable with their registry singleton. This trades a bounded retained
key set for fewer temporary allocations; it is not a guarantee that post-GC heap will decrease.

The two switches are `ae2DefaultItemKeyCache` and `ae2DefaultFluidKeyCache` in
`sunlitindustry-server-performance.toml`. Diagnostics counters are disabled outside explicit probes.
`-Dsunlitindustry.disableServerOptimizations=true` skips the mixins at startup for A/B testing.

## Verification

`tools/server_perf/ae2_key_correctness.snippet` runs on the Minecraft server thread and checks:

- Distinct item/fluid NBT produces distinct keys and keys retain an independent NBT snapshot.
- Empty item/fluid stacks keep returning null.
- An ordinary FluidStack reaches the shared default key.
- Charging a Mekanism energy tablet through the GT capability changes its AE2 key and matching result.

The benchmark uses a separate minimal server, not the production server. Factory microbenchmarks
consume key identity to prevent allocation elimination, warm up first, and report the median of the
last three of four rounds. These measurements establish savings in the exercised factory calls;
they do not establish whole-network TPS, heap size or GC improvements. A populated ME network with
storage buses, patterns, cells and addon key types remains a separate integration workload.

## Results: 2026-11-11

Both phases used the same 18 mod jars, Java 17.0.18, Forge 47.4.23, AE2 15.4.10, GTCEu 7.5.3,
KubeJS build 16, test world and `-Xms2G -Xmx6G`. A test-only MixinExtras game library is separate
from that jar count. Each factory executes five warm-up rounds before four recorded rounds of
200,000 calls; round zero is discarded and the median of rounds 1-3 is reported. Diagnostic hit
counters are enabled during both warm-up and measurement, so their overhead is included.

| Default factory call | Baseline ns/op | Optimized ns/op | Latency change | Allocation B/op |
|---|---:|---:|---:|---:|
| AEItemKey.of(ItemLike) | 97.20 | 22.61 | -76.7% | 80 -> 0 |
| AEFluidKey.of(Fluid) | 53.55 | 78.33 | +46.3% | 56 -> 0 |
| AEFluidKey.of(FluidStack) | 56.72 | 19.22 | -66.1% | 56 -> 0 |

The optimized loops, including warm-up, recorded 1,800,000 item hits and 3,600,000 fluid hits. Individual rounds
are preserved because JIT and host load affect these small timings. These are paired process runs,
not a statistical whole-network benchmark. The allocation result is scoped to warmed default keys.

The direct `Fluid` overload regressed in this diagnostic capture; the result is retained in the graph.
Only the optimized path performs hit-counter increments. Production recording defaults to off, so
counter-free network profiling is needed to evaluate that CPU tradeoff. The fluid switch remains
independently configurable. Reduced allocation is not evidence of a general throughput improvement.

NBT/empty-stack/Mekanism-energy checks passed before and after reload. The reload probe changed
its input from cobblestone to dirt, deleted the recipe, then restored it; both positive and negative
ingredient matching were checked. Counts were 42,493 -> 42,492 -> 42,493, and the probe was absent
in the middle step. GT topology validation also passed 300 topologies / 14,400 comparisons.

The first control probe attempted to use `java.lang.System`, which KubeJS's class filter rejects.
Its recipe callback aborted and that capture had 44,015 recipes, so it was excluded. The probe now
uses JsonIO. The final data uses the 18-jar clean environment with 42,493 recipes; older captures also
had two leftover Create jars that the new staging checks now reject. Five warm-up rounds were added
after observing JIT transitions during preliminary measurements. Original failed and successful
captures remain in the isolated evidence directory for traceability. Final inputs are
`clean-baseline-warm-ae2-benchmark.json` and `clean-optimized-warm-ae2-benchmark.json`.

Normalized samples, validation results, jar SHA-256 and the chart are in
`docs/benchmarks/ae2-server-benchmark-20261111.json`, `.csv` and `.png`. Raw GC/JFR/stdout captures
are in `G:/emoney-void-suit-test/evidence`. Their different startup/reload windows are not used for
a claim about GC rates, pause percentiles or retained memory. Existing LDLib/Rhino client-class
errors on dedicated-server reload, optional TC4 smelting outputs and GT overlapping smelting lookup
warnings are still visible in this minimal environment.
The successful probe reloads report zero KubeJS script errors and zero failed recipes.

## GTO Changes Requiring Further Work

See `GTO_MIGRATION_MATRIX.md` for the source review. In particular, a global decoded-pattern cache
needs world/reload invalidation; cell externalization needs save-format migration; optimized network
routing needs priority, access-control and reentrancy tests. They are not enabled by this patch.
