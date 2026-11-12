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

  // Resolve all required resources before replacing the native recipe. GTCA
  // normalizes the material name incoloy_ma323 to incoloy_ma_323.
  const incoloy = Fluid.of("gtca:incoloy_ma_323", 5760);
  const requiredItems = [
    "gtca:space_elevator", "gtca:piko_wafer_ic", "gtca:nanotube_spool",
    "gcyr:space_fabric", "gtnn:heavy_plate_t4", "gcyr:space_station_packager"
  ];
  if (incoloy.isEmpty() || requiredItems.some((id) => Item.of(id).isEmpty())) {
    console.error("[GTCA-LINKS] Missing required resources; retaining the native elevator recipe");
    return;
  }

  // Replace the native controller and its research together. The unique ID
  // keeps this research independent of other recipes using the same sample.
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
      "gcyr:space_station_packager"
    )
    .inputFluids(
      incoloy,
      Fluid.of("gtceu:uu_matter", 16000),
      Fluid.of("gtceu:lubricant", 32000),
      Fluid.of("gtceu:neutronium", 1440)
    )
    .itemOutputs("gtca:space_elevator")
    .duration(7200)
    .EUt(1966080)
    .stationResearch((builder) => builder
      .researchStack(Item.of("gtca:nanotube_spool"))
      .researchId("society_gtca_space_elevator_controller_v2")
      .EUt(1966080)
      .CWUt(128, 512000));

  console.info("[GTCA-LINKS] Registered UHV GTCA elevator bridge with GCYR orbital hardware and GTNN heavy alloy inputs");
});
