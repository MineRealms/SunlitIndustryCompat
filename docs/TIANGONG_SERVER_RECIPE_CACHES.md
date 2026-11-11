# TianGong Server Recipe Caches

## Scope

These two opt-in caches target the installed pack artifact:

`G:/MinecraftGames/Sunlit Valley(BaopuEdition)/.minecraft/versions/Society Sunlit Valley/mods/zmod-forge-1.20.1-1.40.03-all.jar`

`javap -p -s -c` confirmed both target call sites in that jar. The dirty TianGong source tree and its build output are not the deployment or compilation reference.

1. `zmod.compat.Recipes.all(RecipeManager, RecipeType)` creates a new holder per recipe and an immutable list on every call. `recipeHolderLists` reuses this immutable result for one server recipe epoch.
2. `IndustrialAutomation.acceptsItem` scans every material-processing recipe, while `MaterialProcessingWork.recipes` filters the same machine categories before sorting. `materialMachineGroups` preselects only `recipe.machine()` and shares that ordered subset between both callers.

The second cache does **not** cache ingredient acceptance, `isIncomplete()`, research, NBT, recipe output, quantities, or inventory reservations. Their native code still executes. The original work-list sorting remains in place. No tick interval, machine throughput, resource transaction, structure validation, network synchronization, or save format changes.

## Configuration

Register `TianGongServerConfig.register()` during the compatibility mod's normal configuration bootstrap, only when `zmod` is loaded. File: `config/sunlitindustry-tiangong-server.toml`.

```toml
enabled = false
recipeHolderLists = false
materialMachineGroups = false
maxCachedRecipeTypes = 64
maxCachedRecipeHolders = 32768
maxMaterialMachineGroups = 16
```

Each feature also respects `ServerPerformanceConfig.enabled()` and its existing JVM disable property. Disabled options continue into TianGong's original methods. Lists above the retained-holder budget use the original path. Both caches have independent holder budgets; grouping and wrapper entries retain references to the existing recipe objects, not copies of the recipes.

## Ownership And Invalidation

- Cache storage belongs to a single `RecipeManager`; there is no static map of managers, levels, players, or recipe objects.
- Calls are eligible only on the current server thread and only for `server.getRecipeManager()`. Startup workers, client recipe managers, replacement managers not yet installed on the server, and foreign managers use native code.
- `RecipeManager.apply` and `replaceRecipes` invalidate at entry and return. This covers datapack/KubeJS recipe installation, rather than keying on a filesystem fingerprint.
- Tag updates, TianGong configuration changes, other SunlitIndustry configuration changes, and server shutdown invalidate the generation. Clearing the current manager is scheduled on its server thread.
- A newly installed manager has its own empty cache. Old managers are not retained by these modules.
- No disk cache and no recipe registration or merge: a cache miss calls native `Recipes.all` exactly once, and no generated recipes are added back to RecipeManager.

## Integration

The owner agent must add the following compile-only dependency using the actual installed jar, preferably staged under the build's existing local dependency convention:

```groovy
compileOnly(fg.deobf(files('G:/MinecraftGames/Sunlit Valley(BaopuEdition)/.minecraft/versions/Society Sunlit Valley/mods/zmod-forge-1.20.1-1.40.03-all.jar')))
```

Add these entries to the common mixin list:

```text
performance.tiangong.server.TianGongRecipeManagerMixin
performance.tiangong.server.TianGongRecipeListsMixin
performance.tiangong.server.TianGongIndustrialAutomationMixin
performance.tiangong.server.TianGongMaterialWorkMixin
```

All four require an optional-mod loading gate for `zmod`; use the server performance gate for the `.mixin.performance.tiangong.server.` prefix. In particular, the vanilla RecipeManager mixin must also be skipped without `zmod`, because its cache API references TianGong types. None of these four mixins should be loaded on the multiplayer client.

Known descriptors:

```text
zmod.compat.Recipes.all(RecipeManager, RecipeType): List
zmod.machine.industrial.IndustrialAutomation.acceptsItem(IndustrialController, ItemStack): boolean
zmod.machine.materialprocessing.MaterialProcessingWork.recipes(): List
MaterialProcessingWork.owner: IndustrialController
```

No bootstrap, plugin, build file, or mixin JSON was edited in this change.

## Correctness Probe

`tools/server_perf/tiangong_server_recipe_correctness.snippet` is for the isolated test server's server-thread Rosetta executor. It temporarily adds two diagnostic material recipes, then restores the original RecipeManager collection in `finally`. It exercises the native and enabled automation/work methods, wrapper identity reuse, native `replaceRecipes` invalidation, foreign-manager bypass, capacity bypass, and config rollback. It writes no config files and places no blocks.

Also execute a normal datapack `/reload` with a KubeJS recipe addition/removal and verify its count and the new lookup result, using the repository's existing reload probes. This verifies the actual pack's reload integration beyond the direct RecipeManager replacement probe.

These sources were not built or started by the subtask. Build, optional-mod gates, and isolated-server execution belong to integration validation. No benchmark result is claimed.
