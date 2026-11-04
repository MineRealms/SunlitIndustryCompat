# Sunlit Industry Compat (`sunlitindustry`)

Forge 1.20.1 / Java 17 compatibility mod for the Sunlit Valley pack. It adds **no items, blocks, entities or
progression**; it only fixes GCYR rocket fuel mechanics. All fuel numbers live in the pack's KubeJS
(`kubejs/server_scripts/gcyr/fixRocketFuels.js`, `gtnnLinks.js`, `mekLinks.js`).

Pinned targets: **GCYR 0.2.9**, **GTCEu 7.5.3**. The mixin is skipped entirely when GCYR is absent.

## What it fixes (`RocketFuelMixin` → `argent_matter.gcyr.common.entity.RocketEntity`)

| GCYR 0.2.9 behaviour | Fix |
|---|---|
| Tank validator caches `fluid → bool` globally, but the answer depends on the *current* rocket's motor tier | Per-rocket validation against the live recipe manager, no shared cache |
| Selected fuel recipe/duration matched by fluid only, kept across reloads and from NBT | Re-resolved on contents change, on use and after load; stale objects replaced after `/reload` |
| `consumeFuel` truncates the per-tick demand to `int`, so demand < 1 mB burns nothing | Same continuous formula, prepaid fractional credit; empty tank cannot fly on credit it never paid |

Extra NBT on rockets: `sunlitindustry.fuelCredit` (always in `[0,1)`), `sunlitindustry.creditFluid`.

Fuel tiers: GCYR compares `RecipeHelper.getRecipeEUtTier(recipe)` with the engine tier (1/2/3), so the
recipes must encode the tier as a GT voltage: 32 / 128 / 512. EUt 1/2/3 are all ULV and gate nothing.

## Build / test

```
gradle -p . -I .local/minecraft-libraries.init.gradle test build   # 6 FuelMeter unit tests
python -I tools/phase2/run_phase2_probes.py                          # needs the local G:/emoney-bio-test server running
```

The probes connect to loopback Rosetta only. Use `minecraft:reload` for recipe reloads; on Mohist plain
`reload` is the refused Bukkit plugin reload. Injection point `lambda$new$9` is a compiler-generated name —
re-verify after any GCYR upgrade.

Execution record and measured results: pack `docs/GT_ADDON_PHASE2_EXECUTION.md`.

## Phase 4 additions

The `tools/phase4/` directory contains read-only full-pack probes, deterministic loot/recipe repair generators,
and the isolated-server staging/launch helpers used for the 2026-11-05 acceptance pass. They never contain
server saves, credentials, remote bridge tokens, or player data. The phase-4 candidate package is explicitly
marked not fully validated; do not treat it as a production release without reading the pack's acceptance record.

The WorldEdit compatibility mixin is opt-in and only applies when `worldedit` is loaded. It shares an ordered
state index while WorldEdit builds its block-state neighbour lookup, preserving the original `with` results
without materializing a separate Map-of-Map neighbour table for every state.
