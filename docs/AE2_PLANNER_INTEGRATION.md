# AE2 Native Planner Improvements

Target: AE2 15.4.10. Root owns Gradle, live-server operation and benchmarks.

## Wiring

Call `performance.ae2.planner.Ae2PlannerConfig.register()` once from the performance bootstrap.
Register these dedicated-server/exact-AE2-15.4.10 mixins behind the existing incompatibility gates:

- `performance.ae2.planner.AE2PlannerCalculationMixin`
- `performance.ae2.planner.AE2PlannerGraphMemoMixin`
- `performance.ae2.planner.AE2PlannerSimulationStateMixin`

Config: `sunlitindustry-ae2-planner.toml`. All three features default on and expose individual switches.
Flags refresh on Sunlit configuration load/reload; hot paths read a volatile immutable flag snapshot.

## Adaptation

GTO `OptimizedCalculation.executeV2` assembles a global component graph, extracts strongly connected
components and topologically sorts them before inventory planning. Its metadata names recovered from
`aliases/com/gtolib/U3164U08F8U08E8U08FD.java` identify the implementation as
`gtolib/ae2/crafting2/logic/MainLogic`. The original calculation state also uses a primitive long map for
per-pattern craft counts (`U3164U08F2U08E5U08FE.java`).

This module keeps AE2's native search/branching algorithm and applies compatible state/graph techniques:

- Per-simulation two-entry identity cache for already initialized primary-key groups. Native KeyCounter
  itself uses primary identity. Cold groups still use native lazy parent enumeration and quantity
  extraction; only repeated initialization checks are skipped. There is no cached item quantity or
  cross-branch/global result cache.
- Primitive per-pattern craft counts avoid a boxed `Long` allocation on every process visit. The map
  still implements `Map<IPatternDetails, Long>` for native plan generation, merging and UI consumers.
- Node counts and multiple-path metadata are memoized per node and per calculation graph generation.
  Every native lazy `buildChildPatterns` expansion advances the job generation, including while the
  switch is off. Repeated `CRAFT_LESS`/simulation attempts can avoid revisiting an unchanged tree.

No search decisions are cached: inventory branches, missing-item reporting, byproducts, substitution,
cycle rejection, quantity arithmetic and native pause/cancellation points remain in the native engine.
This is not a wholesale transplant of the GTO component/SCC planner.

## Verification

Live differential and benchmark probes are added alongside this document. Compare the same trace with
feature switches off/on, and include empty groups, fuzzy variants, branch apply/rollback, overflow and
lazy graph expansion. Do not infer full-network TPS or whole-pack heap improvement from local timings.
