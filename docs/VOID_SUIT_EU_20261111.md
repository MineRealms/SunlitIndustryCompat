# Mekanism EU Items And Void MekaSuit, 0.3.0

## Changes

- Attach GT `IElectricItem` capabilities to Mekanism energized tools, free runners, MekaSuit,
  energy tablets, survival energy cubes, and TianGong Void MekaSuit.
- Reuse the native Mekanism energy containers. At pack defaults: 1 EU = 4 FE = 10 J.
  Only tablets/cubes discharge externally. Creative cubes are excluded. Native rate limits,
  whole-EU conversion, simulation, stack-size checks and GT voltage tiers remain enforced.
- Charger tiers are configurable in `sunlitindustry-item-energy.toml`.
- Four Void MekaSuit recipes now use UHV assembly (1,966,080 EU/t) and UHV circuit tags.
  Each has independent research of the corresponding awakened KAMI piece, carried on a data orb.
  The research station consumes its sample and blank orb; the assembly line reuses the written orb.
- A targeted mixin inherits the consumed MekaSuit's serialized data into a private per-run output.
  The native direct infusion recipe IDs are removed explicitly, including transformation-only recipes.
  The recipe script checks that the new compat support exists before replacing those recipes.

| Part | UHV circuits | Neutronium plates | Naquadah alloy plates | Assembly ticks | Research CWU |
| --- | ---: | ---: | ---: | ---: | ---: |
| Helmet | 8 | 4 | 8 | 2400 | 128000 |
| Body | 16 | 8 | 16 | 3600 | 192000 |
| Pants | 12 | 6 | 12 | 3000 | 160000 |
| Boots | 8 | 4 | 8 | 2400 | 128000 |

All four research recipes use UV voltage (491,520 EU/t), 32 CWU/t, and explicitly select data orbs.
Helmet parts emphasize sensors/emitters; body uses field generators, a storage cube and antimatter;
pants/boots emphasize motors, pistons, phase coupling or teleportation. All retain magical armor,
void metal, shadow cloth and primordial components. Full ingredients are in the versioned pack overlay.

The external pack quest generator adds five tasks to the TianGong chapter, reaching 69 quests.
IDs remain stable, with 1.8 minimum spacing and no prerequisite edges through other nodes.

## Verification

Environment: isolated `G:/emoney-void-suit-test`, Forge 47.4.23, Java 17.0.18,
GTCEu 7.5.3, Mekanism 10.4.16.80, installed TianGong/TC4R/KAMI artifacts from the client.
Rosetta binds to loopback port 48793. Production servers were not restarted or modified.

- Gradle `test build`: 21 tests passed, including six new energy conversion/transfer tests.
- `energy_items.snippet`: 17 actual registered items passed capability, simulation, energy
  conservation, charger-tier, external-discharge and item save/load checks.
- `gt_network_and_research.snippet`: native GT LV battery buffer discharged a Mekanism energy
  tablet through the GT network into a GT LV charger containing another tablet: exactly 32 EU.
  Ordinary data hatch rejected four missing carriers, four blank orbs and 12 incorrect research
  pairings; it accepted all four corresponding research orbs.
- `recipe_inventory.snippet`: each suit piece has exactly one final recipe, a real UHV circuit
  tag, valid item inputs, UHV voltage and its own KAMI sample/data-orb station recipe. Old direct
  infusion IDs are absent. Item inputs occupy 14 or 15 buses, within the native limit of 16.
- `armor_assembly.snippet`: native AssemblyLineMachine/RecipeLogic with controlled item/fluid/EU
  handlers consumed all inputs and ran the full 2400/3600/3000/2400 ticks. Output retained the
  actual energy module, energy, enchantment and custom name; the working recipe survived a
  serialization round trip. A fifth run with a plain helmet inherited no prior run's data.
  Exact EU costs were 4,718,592,000 / 7,077,888,000 / 5,898,240,000 / 4,718,592,000.
- Quest generator repeat render and `--check` passed.
- Recipe inventory, ordinary data-hatch gating and all five assembly runs also passed after `/reload`.

The controlled assembly test supplies native handlers directly. It does not validate a newly
built physical multiblock, HPCA cabling or a full research-station operation. Research gating is
tested separately through the native ordinary data hatch with the real generated data orbs.
The standalone test launcher needs an explicit MixinExtras core game library; this test-only
bootstrap artifact is excluded from the release package.

## Delivery

Build with the local library init script documented in README. `tools/void_suit/package_update.py`
creates `build/delivery/Sunlit-EU-VoidSuit-0.3.0.zip`, including mod, recipe, energy config and the
updated quest chapter/generator/IDs. Remove older sunlitindustry jars before starting either side.
The recipe and energy-config overlay are tracked in this repository under `pack/`.
The local client now contains only the 0.3.0 compat jar; its previous 0.2.1 jar is backed up under
`build/delivery/client-before-0.3.0/`. The dev/release pack overlays contain the updated recipe,
energy config and quest files. Running clients need a full restart to load the new Java classes.
The isolated test server was shut down after verification. Production deployment is pending.
