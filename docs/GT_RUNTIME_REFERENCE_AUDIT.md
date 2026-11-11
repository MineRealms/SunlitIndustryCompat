# GTCEu 7.5.3 Runtime References And Muffler Recovery

## Exact Stock Paths

The Gradle 7.5.3 jar was inspected with `javap -p -c`. The source of the GTO
comparison is `GTOCore/src/main/java/com/gtocore/mixin/gtm/api/machine` and
`mixin/gtm/machine/MufflerPartMachineMixin.java` under `GTOCoreGithub`.

- `WorkableMultiblockMachine.getCapabilitiesProxy()` and the no-argument
  `getCapabilitiesFlat()` are `aload_0; getfield; areturn`. They allocate nothing
  and already expose per-controller caches. Formation populates them;
  invalidation and part unload clear them. A second lookup cache would add work.
- `IRecipeCapabilityHolder.getCapabilitiesFlat(io, cap)` does two native map
  `getOrDefault` calls and returns the authoritative list. A second reference map
  would have to track publicly mutable native maps and lists for no allocation win.
- `FluidDrillLogic` already stores `veinFluid`. GTO's cached
  `DrillingControlCenterMachine` is an added GTO machine and multiplier feature;
  it is absent from stock GTCEu. Stock drill methods obtain live saved vein data,
  including remaining operations, before computing each yield.
- GTO's muffler drone-center and air-scrubber references implement GTO gameplay.
  Stock `MufflerPartMachine` has no such repeated lookups. Its server snow check
  is already throttled to every ten ticks; caching world/block state would make
  snow or controller changes stale.
- `GTHandlerGroups` already caches ordered capability-group topology while
  validating identity, color, distinctness and capability membership. This work
  does not duplicate or change that cache.

## Candidate: Muffler Recovery Loop

Stock `recoverItemsTable(ItemStack[])` creates an `IntStream.range`, captures the
muffler and input array in an `IntConsumer`, then rolls once per visited entry.
The loop bound is `min(array.length, inventory.getSlots())`.

`GtMufflerRecoveryMixin` replaces only that stream traversal with an indexed
loop. It calls the native private `calculateChance()` in the same order and
still copies each winning input before `ItemHandlerHelper.insertItemStacked`.
No random result, amount, item, controller, world or inventory reference is
cached. Native chance behavior, excess-entry truncation, leftovers, stack copies
and inventory callbacks remain authoritative. The source template array is not
modified. Disabling the option runs the unmodified native method.

This is a candidate until the runtime probes establish its cost on the target
JVM; no TPS or allocation improvement is claimed merely from removing a stream.

## Root Integration

- Register `GtMufflerRecoveryConfig.register()` during common initialization.
- Register `performance.gt.GtMufflerRecoveryMixin` behind the existing dedicated
  server, exact GTCEu 7.5.3 and GTO/WarpLoad conflict gates.
- The separate common file is `sunlitindustry-gt-muffler.toml`, with
  `mufflerRecoveryLoop=true`. The global server performance switch also applies.
- The mixin uses a private shadow of `calculateChance()Z` and a HEAD injection
  on `recoverItemsTable([Lnet/minecraft/world/item/ItemStack;)V`, `remap=false`.

## Runtime Verification

`gt_muffler_recovery_correctness.snippet` compares the actual native and injected
methods, not a copied reference implementation. It discovers the native muffler
definitions actually registered by the pack and requires at least two tiers.
It tests empty/small/oversized input arrays,
empty/full/partially filled inventories, empty stacks, template counts and RNG
tail state over 384 fixture/seed combinations per registered tier. Output reports
actual IDs, tiers, chance thresholds, slot counts and comparison count; disabled
high tiers are not counted as coverage. The probe restores the config and fixture
block, clears fixture inventory and refuses to replace a preexisting block entity.

`gt_muffler_recovery_benchmark.snippet` measures the same actual methods with
ThreadMXBean allocation counters, five warmups and four retained measured
rounds (`round > 0`). It uses the registered native muffler tiers with
full-inventory fixtures and 0/1/4 entries to
separate stream overhead from native RNG and insertion cost. Output carries all
rounds so the root report can include regressions. Both snippets require the
loopback-only isolated Rosetta test server and execute on its server thread.

No full build or server was run by this subtask; integration and runtime
execution remain with the root agent.
