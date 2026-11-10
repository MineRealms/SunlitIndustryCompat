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
| RecipeRunner allocation reduction | Implemented, benchmarked | Empty I/O and simulation preparation fast paths; actual consumption and chance rolling stay native. |
| Stable machine handler grouping | Implemented, benchmarked | Per-machine bounded topology cache; ordered identity, color/distinct/bypass/filter validation and unload invalidation. No recipe-result cache. |
| Condition short-circuit | Implemented | AND-only path avoids an unused OR map; mixed/OR expressions retain native handling. |
| AE item/fluid key canonicalization | Implemented, benchmarked | Only default ItemLike keys and null-NBT Fluid keys. Tagged/capability-bearing item conversion remains native. |
| ParallelCache / chanceCaches | Partly adapted | Group topology is shared with item/fluid parallel calculation. Caching amounts or chance outcomes would require complete inventory/configuration invalidation. |
| Recipe tier field lookup | Not directly portable | GTO's custom Recipe has a precomputed tier. Stock mutable GTRecipe content/OC state needs mutation tracking before persistent tier memoization. |
| Multiblock member tick bypass | Requires separate integration | GTO controls member ticking and unload routing together. Need prove exactly-once ticking with GT addons, hatches and chunk transitions before replacing LevelChunk dispatch. |
| Ore biome/dimension memoization | Already present in installed GT | GTCEu 7.5.3 already has WorldGeneratorUtils.WorldOreVeinCache with worldVeins/biomeVeins and an invalidate entry point. Confirmed in its jar; no duplicate cache added. |
| Disk resource/JSON/tag/recipe caching | Deliberately excluded | Avoid WarpLoad's independent fingerprints, replay accumulation and stale reload state. Prefer caching immutable bytes with a full content fingerprint in a future isolated loader project. |
| AE KeyCounter/fuzzy-map rewrite | Requires addon tests | GTO changes map identity and fuzzy indexing together; verify durability fuzzy modes, custom primary keys and mutation invalidation before replacing stock semantics. |
| AE two-type storage map | Incompatible with this pack | GTO's two slots only represent items/fluids. The pack has additional chemical/mana/astral key types. A future optimization must preserve arbitrary AEKeyType entries. |
| AE adjacent capability cache | Candidate for next profiling pass | GTO StorageBus/ExportBus/PatternProvider changes need block replacement, rotation, chunk unload and LazyOptional invalidation tests. |
| AE decoded pattern cache | Requires reload-aware design | GTO CraftingPatternItem cache stores patterns constructed with a Level. Use a world/recipe epoch and bounded ownership; invalidate on KubeJS reload and unload. |
| AE network routing rewrite | Requires storage integration benchmark | Preserve priorities, storage owners, security/access rules, nested networks and reentrancy. Local key benchmarks cannot validate this. |
| AE cell externalization | Save-format change, not enabled | GTO BasicCellInventory uses UUID-backed CellDataStorage. Requires migration, copies/creative cloning, recovery and rollback validation. |
| AE crafting planner replacement | Separate major project | GTO OptimizedCalculation depends on custom recipe/pattern/counter APIs. Need full correctness corpus, cancellation handling and concurrent job tests. |
| Client CTM/model/resource caches | Deferred to client phase | Current work is dedicated-server only; compatibility with existing client optimization mods must be tested separately. |

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

Use the existing minimal server to build a fixed ME network with cells, several priority levels,
storage buses against GT machines, export buses and repeated crafting. Collect allocation samples
before choosing the next subsystem. Add the relevant AE addons only when testing their key types.
Measure the same operation trace on/off, then test inventory replacement, chunk unload/reload,
KubeJS recipe modification/removal and nested storage. Keep client rendering out of this comparison.
