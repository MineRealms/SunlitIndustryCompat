# GTCEu 7.5.3 Advanced Server Work

## Adaptation Boundary

The installed 7.5.3 jar was decompiled for exact method bodies; the local 7.5.2 source is only a
readable cross-reference. GTOCore's RecipeLogic and GTOLib's RecipeRunner/ParallelCache/LevelChunk
implementations target a custom API and cannot replace the installed GT classes directly.

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
