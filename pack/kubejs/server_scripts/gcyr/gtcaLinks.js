// Optional GTCA x GCYR x GTNN space progression bridge.
// GTCA 2.3 owns the PIKO wafer and nanotube spool producers. This script only
// replaces the final elevator controller with a standard GTCEu recipe, so it
// does not duplicate or delete GTCA custom recipe types.
const ENABLE_GTCA_GCYR_LINKS = true;

console.info("[GTCA-LINKS] gtcaLinks.js loaded");

ServerEvents.recipes((e) => {
  if (!ENABLE_GTCA_GCYR_LINKS) return;
  if (!Platform.isLoaded("gtca") || !Platform.isLoaded("gtceu")) return;
  if (!Platform.isLoaded("gcyr") || !Platform.isLoaded("gtnn")) return;

  // Remove GTCA's original UHV recipe and its generated duplicate research
  // entry before adding the cross-mod version.
  e.remove({ id: "gtceu:assembly_line/space_elevator_controller" });
  e.remove({ id: "gtceu:research_station/1x_gtca_nanotube_spool" });

  e.recipes.gtceu.assembly_line("society:gtca/space_elevator_controller")
    .itemInputs(
      "64x #forge:frames/neutronium",
      "16x gtceu:uv_field_generator",
      "16x #gtceu:circuits/uhv",
      "64x gtca:piko_wafer_ic",
      "64x gtceu:neutronium_screw",
      "16x gtceu:uv_electric_motor",
      "4x gcyr:space_fabric",
      "4x gtnn:heavy_plate_t4",
      "gcyr:space_station_package"
    )
    .inputFluids(
      Fluid.of("gtceu:soldering_alloy", 5760),
      Fluid.of("gtceu:uu_matter", 16000),
      Fluid.of("gtceu:lubricant", 32000),
      Fluid.of("gtceu:polybenzimidazole", 1440)
    )
    .itemOutputs("gtca:space_elevator")
    .duration(7200)
    .EUt(7864320)
    .stationResearch((builder) => builder
      .researchStack(Item.of("gcyr:space_station_package"))
      .researchId("society_gtca_space_elevator_controller_v2")
      .EUt(1966080)
      .CWUt(128, 256000));

  console.info("[GTCA-LINKS] Registered UHV GTCA elevator bridge with GCYR station and GTNN heavy alloy inputs");
});
