// priority: -1100
(function () {
  var UHV = 1966080;
  var recipes = [
    {
      part: 'helmet', sample: 'thaumictinkerer:awakened_ichorcloth_cowl', duration: 2400, computation: 128000,
      inputs: [
        'mekanism:mekasuit_helmet', 'tainted_magic:voidmetal_goggles', 'tainted_magic:void_fortress_helmet',
        '8x #gtceu:circuits/uhv', '4x gtceu:neutronium_plate', '8x gtceu:naquadah_alloy_plate',
        '4x gtceu:uv_sensor', '4x gtceu:uv_emitter', '2x gtceu:uv_field_generator',
        '4x zmod:xuanxiang_circuit', '4x tainted_magic:shadow_cloth', '4x #forge:ingots/void',
        'tainted_magic:primordial_mote', 'tainted_magic:primordial_nodule'
      ], fluids: [4608, 4608, 4000]
    },
    {
      part: 'bodyarmor', sample: 'thaumictinkerer:awakened_ichorcloth_robe', duration: 3600, computation: 192000,
      inputs: [
        'mekanism:mekasuit_bodyarmor', 'tainted_magic:void_fortress_chestplate', 'thaumcraft:hover_harness',
        '16x #gtceu:circuits/uhv', '8x gtceu:neutronium_plate', '16x gtceu:naquadah_alloy_plate',
        '4x gtceu:uv_field_generator', 'mekanism:ultimate_energy_cube', '4x gtceu:uv_electric_motor',
        '8x zmod:xuanxiang_circuit', '8x tainted_magic:shadow_cloth', '8x #forge:ingots/void',
        '2x tainted_magic:primordial_mote', 'tainted_magic:primordial_nodule', '4x mekanism:pellet_antimatter'
      ], fluids: [9216, 9216, 8000]
    },
    {
      part: 'pants', sample: 'thaumictinkerer:awakened_ichorcloth_leggings', duration: 3000, computation: 160000,
      inputs: [
        'mekanism:mekasuit_pants', 'tainted_magic:void_fortress_leggings',
        '12x #gtceu:circuits/uhv', '6x gtceu:neutronium_plate', '12x gtceu:naquadah_alloy_plate',
        '8x gtceu:uv_electric_motor', '4x gtceu:uv_electric_piston', '2x gtceu:uv_field_generator',
        '6x zmod:xuanxiang_circuit', '6x tainted_magic:shadow_cloth', '6x #forge:ingots/void',
        'tainted_magic:primordial_mote', 'tainted_magic:primordial_nodule', '4x zmod:phase_coupler'
      ], fluids: [6912, 6912, 12000]
    },
    {
      part: 'boots', sample: 'thaumictinkerer:awakened_ichorcloth_boots', duration: 2400, computation: 128000,
      inputs: [
        'mekanism:mekasuit_boots', 'tainted_magic:voidwalker_boots',
        '8x #gtceu:circuits/uhv', '4x gtceu:neutronium_plate', '8x gtceu:naquadah_alloy_plate',
        '4x gtceu:uv_electric_motor', '4x gtceu:uv_electric_piston', '2x gtceu:uv_sensor',
        '4x zmod:xuanxiang_circuit', '4x tainted_magic:shadow_cloth', '4x #forge:ingots/void',
        'tainted_magic:primordial_mote', 'tainted_magic:primordial_nodule', '2x mekanism:teleportation_core',
        'thaumcraft:eldritch_object'
      ], fluids: [4608, 4608, 8000]
    }
  ];

  if (typeof module !== 'undefined' && module.exports) module.exports = {eut: UHV, recipes: recipes};
  if (typeof ServerEvents === 'undefined') return;

  ServerEvents.recipes(function (event) {
    if (!Platform.isLoaded('gtceu') || !Platform.isLoaded('mekanism') || !Platform.isLoaded('zmod')
        || !Platform.isLoaded('thaumictinkerer') || !Platform.isLoaded('sunlitindustry')) return;
    try {
      Java.loadClass('com.society.sunlitindustry.VoidMekaSuitAssembly');
    } catch (error) {
      console.warn('[SUNLIT-VOID-SUIT] Requires Sunlit Industry Compat 0.3.0 or newer; retaining original armor recipes');
      return;
    }

    recipes.forEach(function (row) {
      var output = 'zmod:void_mekasuit_' + row.part;
      // Transformation-only infusion recipes have no ordinary result, so remove their IDs explicitly.
      event.remove({id: output});
      event.remove({output: output});
      event.remove({id: 'pollution:industrial_infusion_recipes/industrial_infusion/thaumic/zmod/recipes/void_mekasuit_' + row.part});
      var recipe = event.recipes.gtceu.assembly_line('society:industry/void_mekasuit_' + row.part);
      row.inputs.forEach(function (input) { recipe.itemInputs(input); });
      recipe.inputFluids(Fluid.of('gtceu:soldering_alloy', row.fluids[0]),
          Fluid.of('gtceu:polybenzimidazole', row.fluids[1]), Fluid.of('gtceu:lubricant', row.fluids[2]))
        .itemOutputs(output).duration(row.duration).EUt(UHV)
        .addDataString('sunlitindustry.armor_source', 'mekanism:mekasuit_' + row.part)
        .stationResearch(function (builder) {
          return builder.researchStack(Item.of(row.sample)).dataStack(Item.of('gtceu:data_orb'))
            .researchId('sunlit_void_mekasuit_' + row.part + '_v1')
            .EUt(491520).CWUt(32, row.computation);
        });
    });
    console.info('[SUNLIT-VOID-SUIT] Registered 4 UHV assembly recipes with separate KAMI data-orb research');
  });
})();
