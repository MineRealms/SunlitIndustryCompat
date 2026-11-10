# AE2 Native Planner Improvements

Target: AE2 15.4.10. Root owns Gradle, live-server operation and benchmarks.

## Wiring

Call `performance.ae2.planner.Ae2PlannerConfig.register()` once from the performance bootstrap.
Register these dedicated-server/exact-AE2-15.4.10 mixins behind the existing incompatibility gates:

- `performance.ae2.planner.AE2PlannerCalculationMixin`
- `performance.ae2.planner.AE2PlannerGraphMemoMixin`
- `performance.ae2.planner.AE2PlannerSimulationStateMixin`

Config: `sunlitindustry-ae2-planner.toml`. Primitive craft counts and graph metadata default on and
expose individual switches.
Flags refresh on Sunlit configuration load/reload; hot paths read a volatile immutable flag snapshot.

## Adaptation

GTO `OptimizedCalculation.executeV2` assembles a global component graph, extracts strongly connected
components and topologically sorts them before inventory planning. Its metadata names recovered from
`aliases/com/gtolib/U3164U08F8U08E8U08FD.java` identify the implementation as
`gtolib/ae2/crafting2/logic/MainLogic`. The original calculation state also uses a primitive long map for
per-pattern craft counts (`U3164U08F2U08E5U08FE.java`).

This module keeps AE2's native search/branching algorithm and applies compatible state/graph techniques:

- Primitive per-pattern craft counts avoid a boxed `Long` allocation on every process visit. The map
  still implements `Map<IPatternDetails, Long>` for native plan generation, merging and UI consumers.
- Node counts and multiple-path metadata are memoized per node and per calculation graph generation.
  Every native lazy `buildChildPatterns` expansion advances the job generation, including while the
  switch is off. Repeated `CRAFT_LESS`/simulation attempts can avoid revisiting an unchanged tree.

No search decisions are cached: inventory branches, missing-item reporting, byproducts, substitution,
cycle rejection, quantity arithmetic and native pause/cancellation points remain in the native engine.
This is not a wholesale transplant of the GTO component/SCC planner.

The two-entry primary-key shortcut was measured and rejected: repeated hot-key simulation was slower
with the shortcut and allocated the same amount. It is not present in the runtime Mixin set. Primitive
craft counts trade a few nanoseconds in a tiny local merge loop for avoiding a boxed count allocation;
the independent config switch lets packs choose whether that allocation reduction is worthwhile.

## Verification

`tools/server_perf/ae2_planner_correctness.snippet` executes the native `computePlan` with isolated
storage/provider fixtures. It compares final output, bytes, simulation flag, path metadata, used,
emitted, missing and per-pattern craft counts with the module disabled/enabled. Scenarios include
chains, failed-branch rollback, missing inputs, CRAFT_LESS, byproducts, substitution, containers,
cycles, emitted items, fluids and cancellation. It additionally checks lazy graph expansion after
metadata reads, then benchmarks repeated metadata reads on a 129-node deep tree.

`tools/server_perf/ae2_planner_state_benchmark.snippet` compares native state traces for an unregistered
foreign AEKey class with shared primary/different secondary values, empty groups, abandoned/committed
branches, ignored keys and long count overflow. It benchmarks native hot inventory simulation and
craft-count accumulation with five warmup/five measured rounds and thread allocation counters.

Both probes restore the previous configuration in `finally`; fixtures never modify live inventories.
Their native private calculation entry bypasses TickHandler registration and marks only the synthetic
job runnable, so the server thread cannot wait for itself. Cancellation still uses native handlePausing.
Do not infer full-network TPS or whole-pack heap improvement from these local timings. Runtime probes
must run against the transformed jar; source/build success alone is not verification.
