# Sunlit Industry Compat (`sunlitindustry`)

Forge 1.20.1 / Java 17 compatibility mod for the Sunlit Valley pack. It adds no Java registry content.
It fixes GCYR rocket fuel mechanics, enforces configurable Mekanism nuclear fuel limits, and repairs
Forge tag creation during parallel recipe loading. Rocket fuel numbers live in the pack's KubeJS
(`kubejs/server_scripts/gcyr/fixRocketFuels.js`, `gtnnLinks.js`, `mekLinks.js`).

`FTBBackupsPreviewMixin` fixes the FTBBackups2 1.0.23 singleplayer/server startup crash when a
registered block returns a null default map colour. Only backup preview palette construction uses
`MapColor.NONE` as a fallback; the affected block ID is logged and backup settings remain effective.

`KubeJSRecipeCacheMixin` returns the existing original-recipe cache before KubeJS build 16 constructs
an unused Throwable. Initial decoding, invalidation and failed/null cached results retain their original
behaviour. This reduces recipe-filter allocation traffic; it does not remove recipes or change outputs.

Pinned targets: **Forge 47.4.23**, **GCYR 0.2.9**, **GTCEu 7.5.3**, and
**Mekanism Generators 10.4.16.80**. Mod-specific mixins are skipped when their target mod is absent.

## Nuclear Limits And Recipe Tags

`[gregtech].preventDataCarrierDowngrade` in `sunlitindustry-common.toml` defaults to true.
It prevents native GT scanner copying from a Data Bank-only research module into a data orb/stick.
Same-module copies, ordinary carrier copies and copying into a module retain their native behaviour.
This closes the cross-carrier copy route around UV research data-bank requirements.

The tiangong.requireWanxiangFormationCore setting defaults to true. A new Wanxiang Cage formed
through the native wand interaction consumes one society:wanxiang_formation_core only after
successful formation. Missing cores reject before formation; a failed structure check spends neither
the core nor wand Vis. Existing formed cages are unaffected. The core's LuV scanner and assembly-line
recipe is registered by the pack's KubeJS progression overlay.

For the UV GT research station, two 32 CWU/t HPCAs can feed the native Network Switch for 64 CWU/t.
The station's researched module must be stored in a Data Bank and delivered to an assembly-line
optical receiver. This compat prevents copying that module down to a data orb or data stick while
preserving same-module copies and upgrades.

The TianGong tokamak's final GT assembly recipe consumes exactly one gtceu:zpm_fusion_reactor
(MK II). Its research sample is the magnetic confinement unit, so the MK II is not duplicated as
research input.

`config/sunlitindustry-common.toml` controls actual fusion fuel burn, the fusion injection setting,
and actual fission fuel burn. Pack values are 2 mB/t fusion, injection 2, and 0.2 mB/t fission.
The actual burners are capped even when NBT or another caller bypasses the normal setters.
These limits constrain fuel throughput; they are not measurements of exported electricity.

`ForgeTagConcurrencyMixin` synchronizes `ForgeRegistryTagManager.getTag` and `bind`, preserving their
original bodies. Forge's copy-on-write tag map can otherwise lose tag handles created concurrently
while recipe loaders retain them. Runtime verification uses a detached manager with 8 threads,
256 keys, and 2,048 lookups, plus checks of the affected live Mekanism recipes. Recipe ingredient
membership tests are not synchronized by this patch. Recheck the exact methods on Forge upgrades.

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
