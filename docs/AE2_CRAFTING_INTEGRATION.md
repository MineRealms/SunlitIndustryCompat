# AE2 crafting integration

Owned implementation: `performance/ae2/crafting` and the listed mixins.
AE2 source target: installed Forge 15.4.10 (mapped jar decompiled locally, not GTO's custom AE2).

## Required central integration

Call `Ae2CraftingConfig.register()` from the mod constructor. This registers
`sunlitindustry-ae2-crafting.toml` (separate config to avoid concurrent central-file edits).
Also call `PatternDecodeCache.registerEvents()` only when AE2 is installed. No automatic
EventBusSubscriber annotation is used, so absent optional AE2 classes are never reflected at boot.

Register the following entries relative to the existing mixin package and gate all of them to
dedicated server + exact AE2 15.4.10 + existing server optimization compatibility checks:

- `performance.ae2.AE2PatternCacheLevelMixin`
- `performance.ae2.AE2CraftingPatternDecodeMixin`
- `performance.ae2.AE2ProcessingPatternDecodeMixin`
- `performance.ae2.AE2CraftingPatternStateMixin`
- `performance.ae2.AE2PatternRecipeEpochMixin`
- `performance.ae2.AE2ProviderSortCacheMixin`
- `performance.ae2.AE2CraftingRootTemplatesMixin`

`PatternDecodeCache` has Forge subscribers for tags and level unload. Recipe epoch invalidation is
in the RecipeManager mixin, including both resource apply and direct replaceRecipes.

## Implemented design

- Positive decoded native crafting/processing pattern cache, owned by ServerLevel, bounded LRU
  (default 2048). Recipe-manager identity and monotonic recipe/tag epoch are checked on every lookup.
  Null decode and exceptions are not retained; native decode/recovery exception handling remains.
  Crafting caching is restricted to exact vanilla shaped/shapeless recipe classes using ordinary
  Ingredient instances: custom recipes/ingredients may depend on state outside a reload epoch.
- Native AECraftingPattern scratch-grid methods are synchronized because globally sharing a decoded
  pattern without protecting testFrame/specialRecipeTestFrame introduces cross-planner races.
  Remainder/substitution scratch state is restored in finally even if custom recipes throw.
- Crafting provider sorting fixes an AE2 15.4.10 hot-path bug: sortPatterns never reset needsSorting,
  so every getCraftingFor call rebuilt the sorted/deduplicated list. The mixin resets it only after
  successful native sorting. Native provider mount/unmount already set it true on every change.
- Root crafting-tree requests reuse their immutable InputTemplate/list across attempts. Substitution
  candidate searches for non-root nodes remain live against each current simulation inventory.

## Verification status

Epoch LRU tests cover positive/negative loads, recipe changes/deletion/restoration, source replacement,
eviction, capacity shrink, exceptions and concurrent loads. Live A/B measurements cover processing
pattern decoding and 64-provider sorting against the same recipe graph and output checksum.
The BlockApiCache override was removed after it measured slower than AE2's native path (about
133 ns / 24 B per lookup versus 46 ns / near-zero B); the native adjacency benchmark remains as a
baseline for later AE2 API changes.
This is not a replacement for GTO's complete custom OptimizedCalculation planner.
