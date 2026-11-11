# GTO Migration Review

Reference roots:

- GTOCore: `H:/MinecraftMods/GTOCoreGithub/GTOCore/src/main/java/com/gtocore/`
- Recovered GTOLib: `H:/MinecraftMods/GTOCoreGithub/Reverse/gtolib-src-full/src/`
- Reference report: `H:/MinecraftMods/GTOCoreGithub/GTOCore/GTO_OPTIMIZATIONS.md`
- Cache failure review: `H:/MinecraftMods/WarpLoad/WARPLOAD_BUGS.md`

GTO's modified GTCEu and AE2 APIs differ from this pack. The rows below distinguish techniques
adapted here from larger architectural changes. No whole-pack performance result is implied.

| GTO technique | Current status | Adaptation or next requirement |
|---|---|---|
| RecipeRunner allocation reduction | Adapted, implemented, benchmarked | `RecipeHelper.handleRecipe` borrows a bounded pool of the stock GTCEu `RecipeRunner`; content and chance-input workspaces are reused. Result types, chance rolls and handler calls stay native. This is not GTO's full `RecipeLogic` state-machine overwrite. |
| Stable machine handler grouping | Implemented, benchmarked | Per-machine bounded topology cache; ordered identity, color/distinct/bypass/filter validation and unload invalidation. No recipe-result cache. |
| Condition short-circuit | Implemented | AND-only path avoids an unused OR map; mixed/OR expressions retain native handling. |
| AE item/fluid key canonicalization | Implemented, benchmarked | Only default ItemLike keys and null-NBT Fluid keys. Tagged/capability-bearing item conversion remains native. |
| ParallelCache / chanceCaches | Partly adapted | Group topology is shared with item/fluid parallel calculation. Caching amounts or chance outcomes would require complete inventory/configuration invalidation. |
| GTO `RecipeLogic` state-machine overwrite | Not portable to stock 7.5.3 | The GTO overwrites change return descriptors and depend on custom `Recipe` APIs. Stock has cached-recipe/failure bookkeeping and Assembly Line/Research Station subclasses that override these hooks. The safe adaptation is the measured `RecipeHelper.handleRecipe` runner pool, not a base-class overwrite. |
| Recipe tier field lookup | Direct lookup not portable or useful | Stock `GTRecipe` has no `tier` field; tier is derived from mutable EUt, parallels and OC state using a cheap native calculation. The pooled runner defers this calculation until a chanced entry needs it. |
| Async recipe output | Excluded | The GTO dual-ME path needs custom holder and task APIs. Stock output success/failure controls recipe progress; inventory, AE grid and chance state remain server-thread-owned. `GtDeferredMeFlush` only coalesces same-tick flushes on the server thread and defaults off. |
| Multiblock member tick bypass | Narrow safe case implemented and lifecycle-tested; blanket bypass excluded | `experimentalDormantPartTick` can suspend only a formed stock GT part with no active or pending tick subscriptions, restoring the vanilla ticker on subscribe/unform. A real 46-LevelTick probe verified 12 exact-once subscriber calls plus sleep/wake/unsubscribe/unform/reform/removal. It defaults off; no whole-base TPS result is implied. GTO's blanket bypass needs custom lifecycle integration. |
| Machine-name redirect while saving | No performance benefit | The `Class.getName` redirect changes an exception log after NBT saving fails, not the NBT format. No Java-class serialization is avoided. |
| Machine definition creation routing | Excluded; preview cache is implemented and benchmarked | GTO's global factory routes create custom definitions and metadata; that factory replacement has no measured speed benefit. Native preview snapshots use an identity palette and independent NBT/ItemStack/capability copies. Four registered machine previews passed correctness and allocation probes. Cache defaults off because arbitrary suppliers can change without invoking a setter. |
| Ore biome/dimension memoization | Already present in installed GT | GTCEu 7.5.3 already has WorldGeneratorUtils.WorldOreVeinCache with worldVeins/biomeVeins and an invalidate entry point. Confirmed in its jar; no duplicate cache added. |
| Sunlight/weather short-circuit | Implemented, benchmarked; 2,304 native comparisons passed | Per-call position and biome lookup reuse retains native End/JAVD/rain semantics; no world state survives the call. Six loaded dimensions tested. JAVD is absent from the minimal server, so that branch is source-reviewed only. Rainy-day allocation falls but measured latency regressed about 5%. |
| Void-world tickChunks/random ticks | Gameplay changes excluded | The recovered GTO patch also throttles tickChunks and changes random-tick speed. Applying those changes would alter crops, weather and spawning rather than preserve this pack's world rules. |
| Muffler recovery temporary objects | Implemented, benchmarked; 3,072 native comparisons passed | An ordered loop removes stream/lambda setup while retaining native chance, RNG order, copies and insertion. All eight registered tiers tested; high-load timings include small regressions while allocation consistently falls. |
| Resource archive source cache | Implemented, benchmarked | Immutable archive snapshots use full source identity/content fingerprints and bounded admission. Prebuilt suppliers open independent streams; direct tree enumeration avoids copied indexes. Dynamic/KubeJS packs are not cached. |
| Disk JSON/Tag/Recipe cache | Deliberately excluded | Avoid WarpLoad's independent fingerprints, replay accumulation and stale KubeJS reload state. Resource snapshots do not cache parsed JSON or Recipe objects. |
| AE KeyCounter/fuzzy-map rewrite | Fuzzy lookup partly adapted; map rewrite excluded | Live native range views and integer bound reuse are implemented and tested. Replacing KeyCounter/index storage is not included because it changes invalidation and custom-key contracts. |
| AE two-type storage map | Incompatible with this pack | GTO's two slots only represent items/fluids. The pack has additional chemical/mana/astral key types. A future optimization must preserve arbitrary AEKeyType entries. |
| AE adjacent capability cache | Tested and rejected | A `BlockApiCache` wrapper passed block replacement/removal correctness checks but regressed lookup from about 43 ns to 75 ns and added allocation; removed from shipped code. |
| AE decoded pattern cache | Implemented with reload invalidation | Bounded server-level pattern caches use recipe/tag epochs and invalidate on reload/unload; KubeJS modify/remove/restore probes pass. This does not replace AE2's recipe or pattern APIs. |
| AE network routing rewrite | Tested and rejected | Native priority routing, storage ownership and reentrancy remain authoritative. The snapshot override regressed insert/extract timings and has been removed; correctness probes remain. |
| AE cell externalization | External format excluded; compact in-cell format optional | GTO UUID-backed storage would change save ownership and custom key support. The local optional compression keeps data inside the cell NBT; writes default off and reads remain available for migration. |
| AE crafting planner replacement | Native planner hot paths partly optimized | Craft counts, bounded state reuse and graph metadata memoization retain native plan semantics and pass the scenario corpus. GTO's complete planner is tied to custom APIs and is not swapped in. |
| Client CTM/baked-model cache | Not implemented | No CTM metadata or baked model graph cache is shipped; it needs client-mod compatibility and reload tests. The optional client resource hook caches source bytes only, is disabled by default, and is skipped when ModernFix or Fusion is loaded. No client FPS claim is made. |

## AE2 Source Review Notes

- `mixin/ae2/stacks/AEItemKeyMixin.java`: the no-NBT ItemStack shortcut cannot be used here without
  preserving Forge capability state. A blank `stack.getTag()` is not proof of a stateless stack.
- `mixin/ae2/storage/CompositeStorageMixin.java` and GTOLib `AEKeyTypeMap`: retain a general key-type
  map in this pack, even when benchmarking an item/fluid-only test server.
- `mixin/ae2/stacks/KeyCounterMixin.java`: replaces map storage and mutation methods alongside fuzzy
  indexing. Porting a single method would leave the index invalidation contract incomplete.
- `mixin/ae2/FuzzyPriorityListMixin.java`: snapshots the source list at construction. Confirm callers'
  mutation expectations before introducing a snapshot into a different AE2 version.
- `mixin/ae2/pattern/CraftingPatternItemMixin.java`: a cache keyed only by item contents cannot by
  itself distinguish resource epochs or recipe managers; this matters for KubeJS reloads.
- `mixin/ae2/storage/NetworkStorageMixin.java`: sorted routing and owner deduplication must be
  validated as a unit, rather than treating the queue replacement as an isolated allocation fix.

## Next Measurement Workload

The 0.7.0 follow-up results, limitations, and reproducible probes are in
[GTO_FOLLOWUP_BENCHMARK_20261111.md](GTO_FOLLOWUP_BENCHMARK_20261111.md).

Use the existing minimal server to build a fixed ME network with cells, several priority levels,
storage buses against GT machines, export buses and repeated crafting. Collect allocation samples
before choosing the next subsystem. Add the relevant AE addons only when testing their key types.
Measure the same operation trace on/off, then test inventory replacement, chunk unload/reload,
KubeJS recipe modification/removal and nested storage. Keep client rendering out of this comparison.
