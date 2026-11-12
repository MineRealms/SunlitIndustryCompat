// Ground recipes remain the bootstrap route. Orbital recipes trade the
// elevator investment for larger batches with the same material quantities.
const ENABLE_GTCA_ORBITAL_BATCHES = true;

ServerEvents.recipes((event) => {
  if (!ENABLE_GTCA_ORBITAL_BATCHES || !Platform.isLoaded("gtca") || !Platform.isLoaded("gtceu")) return;

  const batches = [
    {
      id: "internal_structure", mk: 1, eu: 32768, ticks: 1600,
      inputs: [[4, "#forge:frames/naquadah_alloy"], [16, "#forge:rods/long/naquadah_alloy"], [8, "#forge:plates/neutronium"]],
      polymer: 576, output: "4x gtca:space_elevator_internal_structure"
    },
    {
      id: "base_casing", mk: 2, eu: 131072, ticks: 1600,
      inputs: [[4, "#forge:frames/naquadah_alloy"], [24, "#forge:plates/neutronium"]],
      polymer: 576, output: "4x gtca:space_elevator_base_casing"
    },
    {
      id: "cable", mk: 3, eu: 524288, ticks: 1600,
      inputs: [[4, "gtca:nanotube_spool"], [64, "#forge:rods/long/neutronium"]],
      polymer: 9216, output: "64x gtca:space_elevator_cable"
    }
  ];

  batches.forEach((batch) => {
    const polymer = Fluid.of("gtceu:polybenzimidazole", batch.polymer);
    if (Item.of(batch.output).isEmpty() || polymer.isEmpty() ||
        batch.inputs.some((input) => Ingredient.of(input[1]).itemIds.size() === 0)) {
      console.error("[GTCA-ORBITAL] Skipped " + batch.id + ": missing registered input/output");
      return;
    }

    const recipe = event.recipes.gtceu.space_assembler("society:gtca/orbital_" + batch.id)
      .inputFluids(polymer)
      .itemOutputs(batch.output)
      .addDataNumber("required_module_mk", batch.mk)
      .duration(batch.ticks)
      .EUt(batch.eu);
    batch.inputs.forEach((input) => recipe.itemInputs(input[0] + "x " + input[1]));
  });
  console.info("[GTCA-ORBITAL] Registered material-equivalent orbital batches; ground bootstrap retained");
});
