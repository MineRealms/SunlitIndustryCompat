# GTNN and GTCA Quest Chapters

Date: 2026-11-12
Pack: Society Sunlit Valley, Minecraft 1.20.1, Forge 47.4.23, GTCEu 7.5.3, GTNN 1.3.10, GCYR 0.2.9, GTCA 2.3.0.

## Chapter Map

Three chapters now occupy the GT technology group:

| Order | Chapter | Quests | Scope |
|---:|---|---:|---|
| 24 | GTNN: 工艺与量产 | 33 | Chemical plant, difficult platinum and naquadah lines, neutron activation, rocket feedstocks, heavy alloys and circuit assembly line |
| 25 | GTCA: 工业材料 | 22 | Volcanus, IsaMill and flotation; COMET particles and Tegmark metallurgy; nanites and PCB factories |
| 26 | GTCA: 轨道工业 | 24 | PIKO and nanotube research, the GTCA elevator, its motors, and miner, pump and assembler modules |

The GTCA split keeps the extensive UHV elevator chain from stretching through
unrelated particle, alloy and PCB routes. Each chapter is a set of parallel
process columns with a small number of convergence points. GTNN rocket fuel
and heavy-alloy routes remain independent until the hardware actually needs
them. The existing GCYR space chapter remains separate.

Quest item tasks are non-consuming. Checkmarks record manual commissioning;
they do not award research data, grant materials, or claim that a multiblock
has formed. Quest dependencies are navigation and progression hints, while
machine voltage, structure, recipe research, feedstock and energy remain the
actual gameplay gates.

## Dependency Graph Layout

The quest graph was generated from stable per-chapter ID registries and then
optimized with the standalone FTB Quest layout tool. The accepted coordinates
are stored in the chapter generators, so subsequent regeneration is
deterministic. The layout checks include connector crossings, edge-through-node
hits, proximity hits, backwards prerequisites and minimum node spacing.

| Chapter | Crossings | Edge-through-node | Near-node hits | Minimum spacing | Footprint |
|---|---:|---:|---:|---:|---:|
| GTNN | 0 | 0 | 0 | 2.5 | 21 × 22.5 |
| GTCA Industrial | 0 | 0 | 0 | 2.5 | 27 × 9.5 |
| GTCA Orbital | 0 | 0 | 0 | 3.0 | 36 × 15 |

These are graph geometry measurements, not pixel-perfect measurements of the
FTB Quests renderer. Before/after SVG diagrams are stored beside this document.

## Space Module Manufacturing

GTCA's space content is now reachable without requiring the orbital assembler
to manufacture its own first instance:

- All nine miner, pump and assembler controllers have ground assembly-line
  recipes. MK I, II and III run at LuV, ZPM and UV respectively; later modules
  require the previous module and have distinct research IDs.
- The five elevator motors have ground recipes from MK I through MK V. Their
  recipe and research gates are UHV from MK II upward.
- Ten mining drones have a ground production chain. LV through UV use tiered
  assembly recipes; UHV and UEV use the assembly line and research.
- Six elevator structure items have ground assembler recipes. The cable uses
  the nanotube spool, so the PIKO, carbon nanite and research paths feed into
  the structure naturally.
- The space assembler has twelve MK-gated drill tip and long rod recipes. The
  input material is conserved against ordinary GTCEu drill manufacture.
- The nonfunctional gtca:space_miner/ha test recipe that yielded 1024 gold
  nanites from one hydrogen ion has been removed.
- Mining recipe registration skips missing drill parts rather than silently
  generating a recipe with an absent ingredient.

The pack adds three optional orbital batch recipes for elevator internal
structure, base casing and cable. They consume exactly four times the
registered ground recipe's item and fluid quantities and produce four times
the output. They use LuV, ZPM and UV respectively, and require the matching
MK I, II or III orbital assembler. Ground recipes remain the initial bootstrap
route.

## Recipe Validation

The changes were tested on an isolated Forge server at
G:/emoney-gtca-link-test-20261112; no production server was touched.

Fixture contents were deliberately limited to ten mods: Forge dependencies,
GTCEu, GTCA, GTNN, GCYR and KubeJS/Rhino. The fixture used the same GTCEu,
GTNN, GCYR and GTCA versions as the pack, with the pack linkage and phase-4
repair scripts.

Results:

- GTCA source build: successful. Installed jar SHA-256:
  93bb6c549ec6ac095c6023cf3174cd81755298683de21a7c539879fe9f60cc59.
- GTNN and GTCA chapter generators: repeat render identical; SNBT accepted by
  both the project parser and nbtlib; IDs and dependencies resolve.
- All 85 referenced item IDs, including chapter icons, resolve in the fixture.
- Runtime recipe registry: 55,160 recipes after reload.
- Runtime snapshot: 840 selected recipes; canonical SHA-256
  4ef24eb91efa82e0bb50b786e10ccb33a65e745a424b8fbaea065970659d49ca,
  identical before and after /reload.
- New recipe checks: 9 module controllers, 5 motors, 10 drones, 6 structure
  recipes, 12 drill/rod batches, 3 orbital batches and 16 unique research
  contracts. The selected mining recipe set contained 625 recipes with a
  reusable drone input, plasma feed and computation requirement.
- No unresolved item ingredients in the selected GTCA, GTNN, GCYR and KubeJS
  recipes. All task item targets were present.

This test validates recipe registration, serialization, research payloads,
input tags and batch costs. It does not place and operate every multiblock in a
world; physical structure formation, power distribution and long-running
processing still require a gameplay test.

The isolated log also contains pre-existing optional-mod warnings, including
GCYR tags referring to UEV+ oxygen spreaders not present in GTCEu 7.5.3, and
Rhino attempts to inspect client-only classes on a server. They did not prevent
startup or recipe reload. Existing GTCA asteroid recipes also log ranged-output
decode warnings and fall back to vanilla recipe decoding; the selected mining
records still registered and passed basic ingredient checks.

## Reproduction

From the active pack instance, run each generator with --verify-repeat, then:

    python tools/gt_addons/verify_quests.py --pack "G:\MinecraftGames\Sunlit Valley(BaopuEdition)\.minecraft\versions\Society Sunlit Valley" --targets "G:\emoney-gtca-link-test-20261112\gtca-quest-targets.json"
    node tools/gt_addons/verify_space_snapshot.cjs "G:\emoney-gtca-link-test-20261112\gtca-recipe-validation.json"
    node tools/gt_addons/verify_snapshot.cjs "G:\emoney-gtca-link-test-20261112\gtca-recipe-validation.json" "G:\emoney-gtca-link-test-20261112\gtca-space-validation-before-reload.json"

The server-only snapshot probe is at
tools/gt_addons/gtcaValidation.js.snippet; copy it into the fixture as a
temporary .js file. It should not be included in the normal pack scripts.
