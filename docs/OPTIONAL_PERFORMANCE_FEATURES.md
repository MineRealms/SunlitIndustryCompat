# Optional Performance Features - 0.8.0

## Policy

New TianGong optimizations and restored experiments default off. Their disabled
branches continue into native methods. Existing previously enabled GT/AE2
optimizations retain their individual settings. The shared runtime master is
`enabled` in `config/sunlitindustry-server-performance.toml`; it now also gates
resource snapshots, AE2 crafting caches, planner features and TianGong visuals.
The JVM property `-Dsunlitindustry.disableServerOptimizations=true` disables the
optional performance mixins at loading time. Restart after changing that property.

Crash fixes, balance changes and the reader for existing compact AE2 cell data
are not disabled by the performance master. The compact writer still has its own
off-by-default switch; disabling it must not make already written cells unreadable.

## New and Restored Switches

Paths below are relative to the instance's `config/`. Templates ship in `pack/config/`.

| File | Option | Default | Behavior when enabled |
|---|---|---|---|
| `sunlitindustry-tiangong-server.toml` | `enabled` | false | Enables this module's independently selected features |
| Same | `recipeHolderLists` | false | Reuses immutable holder lists on the current server RecipeManager |
| Same | `materialMachineGroups` | false | Preselects material recipes by machine for automation and work lookup |
| `sunlitindustry-tiangong-client.toml` | `dysonPrimitiveAllocation` | false | Avoids temporary arrays/vectors while emitting sky geometry |
| Same | `arcaneRingTrigReuse` | false | Reuses adjacent circle endpoints without changing segment count |
| Same | `effectBezierAllocation` | false | Computes each Bezier result with one result vector |
| Same | `distantMachineEffectCulling` | false | Omits auxiliary plasma/bubbles beyond `machineEffectDistance` (64 blocks) |
| Same | `tokamakPlasmaLod` | false | Changes only core/plasma/halo detail beyond 48/96 blocks from render bounds |
| `sunlitindustry-ae2-network-experiments.toml` | `experimentalPriorityRouting` | false | Restored priority-routing snapshot candidate |
| Same | `experimentalAdjacentCapabilityCache` | false | Restored positive LazyOptional cache candidate |
| Same | `experimentalPrimitiveCellAccounting` | false | Restored primitive cell-count candidate |
| `sunlitindustry-ae2-planner.toml` | `hotSimulationPrimaries` | false | Restored two-entry initialized-primary shortcut |
| `sunlitindustry-world-performance.toml` | `gtSunlightQuery` | false | Runs the equivalent GT query only in configured dimensions |
| `sunlitindustry-gt-muffler.toml` | `mufflerRecoveryLoop` | false | Replaces stream scaffolding with an ordered recovery loop |
| `sunlitindustry-legacy-performance.toml` | `worldEditStateIndex` | false | Shares large WorldEdit indexes; requires restart after changes |
| Same | `kubejsOriginalRecipeFastPath` | true | Existing optimization: reads the existing original-recipe cache early |

The three AE2 network candidates were restored from the changes removed in
`e2db27d` and `90577c3`. The planner shortcut was restored from the same history.
Their previous slower/no-benefit benchmark results still apply; restoring them
does not imply a performance improvement. No new benchmark was run for 0.8.0.

## Sunlight Dimension Selection

```toml
gtSunlightQuery = false
sunlightDimensions = ["minecraft:overworld"]
sunlightExcludedDimensions = []
```

An empty allowed list matches no dimensions. `["*"]` matches all, except IDs in
`sunlightExcludedDimensions`. Exclusions take precedence. IDs are validated and
compiled into immutable sets when the config loads/reloads. A disabled option,
unselected dimension, or excluded dimension executes native GT logic.

This policy selects the optimization, not new daylight or spawning rules. The
query retains native GT weather/End/JAVD behavior. JAVD's identifier is a native
compatibility rule, not the list of dimensions where the optimization is enabled.

## TianGong Scope

The server cache follows recipe-manager replacement, apply, tags, configuration
changes and shutdown. It is bounded and owned by the current manager, with no
static world references. Matching, NBT/tag predicates, research, transactions,
machine speed and structural validation remain native. See
[TIANGONG_SERVER_RECIPE_CACHES.md](TIANGONG_SERVER_RECIPE_CACHES.md).

Client hooks target the installed `zmod` 1.40.03 jar, not the dirty TianGong source
tree. They run only on the client, under exact-version gates. LOD meshes are
created lazily only when enabled and requested, with native fallback if creation
fails. Shells, ports, glass and shader render types are preserved. Optional effect
distance hiding deliberately changes distant visual effects.

Whole-machine frustum cancellation was not added: installed Industrial/FluidWorks
render bounds are not a proven animation/rotation-complete mesh envelope. Using
them to hide the entire machine could make visible parts disappear. No shader,
OpenGL backend, CTM or global baked-model cache is replaced by these hooks.

## Validation and Delivery

- `gradle --offline test build` passes: 65 tests, including restored helpers and
  the [code-quality review fixes](CODE_QUALITY_REVIEW_20261111.md).
- The isolated 18-mod dedicated server starts normally with all new options off
  and registers 42,493 recipes. Seven new/restored target classes load successfully.
- Runtime probes verify default-off flags, 18 dimension-policy checks and the
  shared master switch. TianGong checks cover 8 holder types, 4 material groups,
  16 native automation comparisons, recipe replacement, capacity and foreign
  manager fallback. Restored AE2 checks cover native/experimental/native routing,
  queued unmount/reentrancy, cell reopen/save callbacks and capability replacement.
- The final review build also passes 8 unreadable-cell quarantine scenarios and
  resource-index checks over 4 trees and 305 source-byte comparisons.
- No benchmark or client rendering session was run in this change. Client FPS,
  visual correctness and shader compatibility are not established by a server
  startup check. All client additions remain disabled.
- Test server only: `G:/emoney-void-suit-test`. The production server and live
  client have not been changed.

Use `build/libs/sunlit-industry-compat-1.20.1-0.8.0.jar` with the supplied config
templates. Existing TOML values take precedence over changed Java defaults; in
particular, an old explicitly enabled muffler or sunlight option remains enabled
until its config is changed. Review config values when updating an existing instance.
