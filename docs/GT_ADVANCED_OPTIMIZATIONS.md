# GTCEu 7.5.3 Advanced Server Work

## Adaptation Boundary

The installed 7.5.3 jar was decompiled for exact method bodies; the local 7.5.2 source is only a
readable cross-reference. GTOCore's RecipeLogic and GTOLib's RecipeRunner/ParallelCache/LevelChunk
implementations target a custom API and cannot replace the installed GT classes directly.

The incompatibility is behavioral as well as structural. In stock 7.5.3, `matchRecipe`,
`handleRecipeIO` and `handleTickRecipe` return `ActionResult`; GTO's overwrites use `boolean` and
cast to its custom `Recipe` type. Stock `RecipeLogic` also owns cached-recipe/failure bookkeeping,
and Assembly Line and Research Station subclasses override several of these hooks. The stock
`GTRecipe` has no precomputed `tier` field. Therefore this mod reuses the native runner only at the
`RecipeHelper.handleRecipe` boundary and does not replace the stock recipe state machine.

Likewise, stock GT parts are driven by their tick subscriptions; there is no GTO-style controller
loop that can take over every multiblock member. The full `LevelChunk` membership bypass and chunk
unload rewrite are excluded. The optional dormant-part path below only skips a formed native
part's empty ticker after it has no active or pending subscriptions.

The GTO redirect of `Class.getName()` inside `getBlockEntityNbtForSaving` changes only the name in
the save-exception log. Stock Minecraft calls it in the catch block after `saveWithFullMetadata`
fails; it does not serialize a Java class or change the normal NBT format. It has no normal-path
performance benefit and is not included.

## Recipe Execution

`GtRecipePipelineMixin` preserves native RecipeHelper result formatting and group-color updates,
but borrows a native RecipeRunner with a bounded working set. A `finally` block clears all recipe,
holder, handler, chance-cache and ingredient references even when a mod handler throws.
Borrowed objects leave the pool; reentrant calls cannot obtain the outer invocation's runner.
Each thread retains at most four runners. Runners processing more than 256 entries in one capability
or more than 16 capabilities are discarded instead of retained.

The runner retains its native handler dispatch and uses the existing validated topology cache.
Only content-list preparation changes. Lists/maps are reused; probability input preparation uses
one scratch list; EU tier/chance-function lookup is deferred until there is a real chanced entry.
Native `ChanceLogic.roll` is invoked in the same capability/entry order, with the original cache,
total-runs value and tier. Random outcomes are never memoized. The persistent native chance cache
remains owned by RecipeLogic. Nonconsumed entries remain part of simulated matching.

## Parallel Scratch

`GtItemParallelScratchMixin` and `GtFluidParallelScratchMixin` scope native parallel calculations
with exception-safe leases. Native aggregation maps are cleared and reused, including item inventory
maps with the exact native all-but-count strategy. Custom strategy identity changes replace a map.
Ingredient equality, NBT, nonconsumed subtraction, group ordering and every quantity calculation are
still native. Inventory contents are reread every invocation; no parallel result survives the call.
Oversized scratch sets are discarded. Recursive addon calculations use separate leases.

## Experimental Dormant Part Tickers

The GTO blanket membership test would disable energy, ME, maintenance and cover subscriptions in stock
7.5.3. The adapted implementation only removes the empty native ticker of a formed, native GT part
after both its active and pending subscription lists are empty. Custom machine block classes and addon
machine classes are excluded. `subscribeServerTick` restores the vanilla chunk ticker immediately;
unformation also restores it. Native subscription execution remains exactly once per tick.
No controller manually ticks a member and there is no second scheduler for active machines.

This option defaults off until live hatch/cover/unformation/chunk lifecycle probes pass.

## Experimental ME Flush Scheduling

Native ME output item buses and fluid hatches already accept recipe output into a persisted local
KeyStorage. The optional scheduler coalesces each hatch's `autoIO` calls until END of the same level
tick. The original autoIO method then rechecks grid connectivity, sync interval and storage insertion
on the server thread. The queue contains no copied output amounts and cannot create a second item
owner. Block removal or chunk unload leaves authoritative quantities in the original GT lifecycle.
Queue overflow uses native immediate handling. There is no worker-thread mutation and no promise
that every workload benefits; this scheduling experiment defaults off.

## Experimental Preview Shapes

Each machine definition can retain a bounded snapshot of its native preview pages. Returned shapes
receive fresh arrays, BlockInfo objects, NBT and ItemStacks. Runtime `lastEntity` fields, callbacks,
custom BlockInfo subclasses and worlds are never cached. Suppliers replaced through native setters,
resource reload and server shutdown invalidate snapshots. Callback-bearing shapes use the native
path. ItemStack templates stay private and use the native copy operation rather than repeated
NBT encode/decode. Incompatible pages are bypassed until invalidation, avoiding repeated failed
capture attempts. The current bounds are 16 retained definitions, 32 pages and 65,536 cells per definition. This experiment defaults off;
dynamic suppliers require additional pack-specific validation.

## Central Wiring

Call `GtAdvancedConfig.register()` during mod construction. Register the following mixins behind
the existing dedicated-server/exact-GT-7.5.3/GTO-conflict gates:

- `performance.gt.GtRecipePipelineMixin`, `performance.gt.GtReusableRecipeRunnerMixin`.
- `performance.gt.GtItemParallelScratchMixin`, `performance.gt.GtFluidParallelScratchMixin`.
- `performance.gt.GtChunkTickerAccess`, `performance.gt.GtDormantChunkTickerMixin`,
  `performance.gt.GtDormantPartMachineMixin`, `performance.gt.GtDormantPartLifecycleMixin`.
- `performance.gt.GtMatchingShapesMixin`, `performance.gt.GtPreviewBlockInfoAccess`.
- `performance.gt.GtDeferredMeBusMixin`, `performance.gt.GtDeferredMeHatchMixin` additionally need AE2.

The parallel scope mixins require MixinExtras 0.4+ WrapMethod. Central lifecycle forwards level END
ticks, level unloads and server stop to GtAdvancedLifecycle. Resource/config reload on the server
thread calls `resourcesReloaded()` to drop scratch/shape caches and restore dormant tickers.

## Not Equivalent To GTO's Modified Jar

Stock 7.5.3 already computes voltage tier using leading-zero bit arithmetic. Adding a persistent tier
field would needlessly risk stale mutable EnergyStack/parallels/OC state, so this implementation only
avoids tier lookup when probability preparation does not need it.

Stock 7.5.3 already has world/biome ore memoization. No second ore cache is layered over it.

There is no off-thread world, inventory, AE grid or persistent chance-cache mutation.

## Verification Status

Focused tests cover scratch bounds, recursive isolation, cleanup after exceptions and map strategy
replacement. Runtime probes must additionally compare native/pooled recipe outcomes and RNG state,
real input/output parallel results, KubeJS reloads and the experimental ticker lifecycle. Compilation
and microbenchmarks alone do not establish whole-base TPS or heap improvements.
