# GT addon linkage, 2026-11-12

## Scope and Versions

This change links the existing GTNN heavy-alloy chain and GCYR orbital hardware
to GTCA's UHV space-elevator controller. It also repairs two missing GTCA
component producers and eight existing quest descriptions.

Validated versions: Minecraft 1.20.1, Forge 47.4.23, Java 17.0.18, GTCEu 7.5.3,
GTCA 2.3.0, GTNN 1.3.10, GCYR 0.2.9, KubeJS 2001.6.5-build.16.
The pack's GTNN hard platinum and naquadah options are enabled in the fixture.

The local pack previously had GTCA 2.2.0. The rebuilt 2.3.0 artifact is installed
locally; the old jar has the `.disabled` suffix. Production has not been changed.
No SunlitIndustryCompat Java code changed, so no new compat jar is required.

## Recipe Design

| Output | Process | Research sample | Cost and stage |
| --- | --- | --- | --- |
| Heavy alloy T1-T4 | Assembler | None | HV, EV, IV, LuV; existing staged chain |
| 64 PIKO wafers | Assembly line | GTCA quantum anomaly | UHV circuits, SoC, neutronex, quantum stars, UV field generators, UU matter and neutronium |
| Nanotube spool | Assembly line | GTCA carbon nanites | 16 carbon nanites, 8 UV arms, 2 quantum stars, 8 neutronium plates, 4 UHV circuits, UU matter and neutronium |
| GTCA elevator | Assembly line | Nanotube spool | Original UHV recipe plus 4 GCYR space fabric, 4 GTNN T4 heavy plates and 1 GCYR station packager |

The two component recipes use unique research IDs to avoid overwriting the
existing PCB III and nanoforge research recipes that use the same samples.
All three assembly recipes accept `#gtceu:circuits/uhv`.

Component manufacture uses 1,966,080 EU/t. Component research uses 491,520 EU/t;
elevator research uses 1,966,080 EU/t. Each research requires 128 CWU/t and
512,000 total CWU, with GTCEu's native data module medium. The elevator takes
7,200 ticks at its nominal manufacturing voltage.

The controller retains its original four fluids: 5,760 mB Incoloy MA323,
16,000 mB UU matter, 32,000 mB lubricant and 1,440 mB neutronium.
GTCA normalizes the fluid ID to `gtca:incoloy_ma_323`.

The KubeJS bridge checks the optional mods and required addon resources before
removing the original controller and research recipes. Disabling
`ENABLE_GTCA_GCYR_LINKS` restores the native controller recipe after a reload.

### Heavy-Alloy Repair

The existing T2 script referenced an empty titanium dense-plate tag. This threw
inside its recipe callback and prevented T3 and T4 from registering as well.
`heavyAlloys.js` now examines resolved item IDs and falls back to ordinary plates
at the same 9:1 material cost wherever a dense-plate form is absent. An empty
tag ingredient is not reliably detected by `Ingredient.isEmpty()` in this stack.
Recipe voltage, duration and output quantities are retained.

### Source Changes

GTCA source repository: `H:/MinecraftMods/GTCA`, branch
`sunlit/gt-addon-linkage-20261112`.

- `GTCARecipeInit.java`: register `SpaceChainRecipes`.
- `SpaceChainRecipes.java`: add the wafer and spool producers and separate research IDs.
- `MiscRecipes.java`: remove the development Space Pump recipe that converted one Incoloy DS dust to UU matter.

Commits: `07f6748` and `469b17f`. The upstream GTCA remote was not pushed.

## Quest Changes

Eight descriptions were manually edited in the active local pack:

| Chapter | Quest IDs | Correction |
| --- | --- | --- |
| EV | `3398FC108D9DCB46`, `57D4E09A9CAB150C`, `2F9D9AAB27E6534D` | Metal-powder intermediates, sludge centrifuging, platinum/palladium reduction and GTNN separation branches |
| ZPM | `74B037D7031F4A78`, `6D1F10543A391940` | Neutron activation followed by blast-furnace reduction and cooling |
| Ore generation | `143B1AE1F3CF53EF`, `4D76C334B251EB3A` | Naquadah oxide route; raw ores versus changed metal-powder byproducts |
| Space | `4E06937111357BD0` | Distinguish GCYR's Dyson elevator from GTCA's UHV module elevator |

IDs, dependencies, rewards, task targets and coordinates are preserved. The
scoped patch is `pack/quest_patches/gt_addon_descriptions.patch`; it excludes
unrelated changes already present in the pack's dirty working tree. The four
edited chapters parse successfully with the existing FTB SNBT parser.

## Runtime Validation

Fixture: `G:/emoney-gtca-link-test-20261112`, bound to loopback port 25584,
2-4 GiB heap, 10 top-level mod jars plus their bundled dependencies.
Existing heavy-alloy, rocket, fuel and GTCA casing compatibility scripts were
included. A second pass also includes the pack's existing phase-4 repair script.

The final phase-4 pass reports 30 added, 34 removed and **0 failed** KubeJS
recipes. The runtime registry contains 55,100 recipes. The focused snapshot
contains 40 GT recipes, including the three assembly recipes, their research
producers and all four heavy-alloy tiers.

`tools/gt_addons/verify_snapshot.cjs` verifies real runtime serialization:

- Each assembly recipe has the expected UHV voltage, output and circuit tag.
- Research sample, blank medium, output medium, research ID and type match.
- Each research requires the intended voltage and computation.
- The elevator retains native fluids and consumes the cross-mod inputs.
- Original elevator/research entries are removed; one elevator producer remains.
- Target item ingredients resolve to nonempty runtime item sets.
- A repeated reload preserves the canonical snapshot and total recipe count.

The supplemental compatibility document's claim that GTNN uses IDs without
machine-type prefixes does not match this runtime. GTNN recipes here also have
prefixed IDs. The phase-4 script is therefore included in the actual validation,
rather than relying on that claim to establish compatibility.

### Limits and Existing Warnings

This validates registration and research-data contracts. It does not demonstrate
complete survival progression or a physical factory processing every recipe.
GTCA's nine space module controllers, elevator structure parts, drone chain and
empty space-assembler recipe set still need a separate content-completion pass.
The controller/component repair must not be interpreted as a finished space branch.

Existing upstream warnings remain: GTCA ranged miner outputs fall back from the
KubeJS parser to native deserialization; GCYR has references to disabled high-tier
oxygen spreaders; GTCEu/LDLib expose some client classes during Rhino inspection;
GTCA has a preexisting propyne output error. None is presented as fixed here.
No production server was restarted or deployed.
