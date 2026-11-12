// priority: 0
// ============================================================
// GCYR × GT--（gtnn）重型合金链补全
//
// 背景（源码核对，非推测）：
//   - GTNN 的重型合金锭配方位于 AdAstraRecipes.init()，由
//     GTNNIntegration.isModLoaded("ad_astra") 控制；本包未安装 Ad Astra，
//     因此重型锭/重型板当前不可获得 → 太空章与火箭硬化会软锁。
//   - 原配方结构（GTNN AdAstraRecipes.init，装配机）：
//       T1 = 黄铜致密板 + 铝致密板 + 钢致密板 + 72mB 不锈钢 + 电路1（HV，VA[3]=480，300t）
//       T2 = T1 + 2×Desh致密板 + 72mB 钨钢 + 电路1（EV，1920，300t）
//       T3 = T2 + 4×Ostrum致密板 + 72mB 铂 + 电路1（IV，7680，300t）
//       T4 = T3 + 4×Calorite致密板 + ...（LuV）
//   - 本脚本用包内材料替代 Ad Astra 材料：
//       Desh → 钛（T2） / Ostrum → 钨钢（T3） / Calorite → 硅岩合金（T4）
//     其余参数（机器/电压/时长/电路/流体量）与原版一致。
//   - 重型板 = 内爆压缩机（GTNN DefaultRecipes：heavy_ingot → heavy_plate + 炸药），已存在。
// ============================================================
console.info("[GCYR-GTNN] heavyAlloys.js loaded");

ServerEvents.recipes((e) => {
  if (!Platform.isLoaded("gtnn") || !Platform.isLoaded("gcyr")) return;

  // GTCEu does not generate every dense plate form. Preserve the 9:1 metal
  // cost using ordinary plates when a dense-plate tag is empty.
  function densePlates(material, count) {
    const tag = "#forge:dense_plates/" + material;
    return Ingredient.of(tag).itemIds.size() === 0
      ? (9 * count) + "x #forge:plates/" + material
      : count + "x " + tag;
  }

  // ---- T1 重型合金锭（HV 装配机，镜像 GTNN 原配方）----
  e.recipes.gtceu.assembler("kubejs:heavy_ingot_t1")
    .itemInputs(densePlates("brass", 1), densePlates("aluminium", 1), densePlates("steel", 1))
    .inputFluids(Fluid.of("gtceu:stainless_steel", 72))
    .itemOutputs("gtnn:heavy_ingot_t1")
    .circuit(1)
    .duration(300)
    .EUt(480, 1);

  // ---- T2 重型合金锭（EV；Desh → 钛）----
  e.recipes.gtceu.assembler("kubejs:heavy_ingot_t2")
    .itemInputs("gtnn:heavy_ingot_t1", densePlates("titanium", 2))
    .inputFluids(Fluid.of("gtceu:tungsten_steel", 72))
    .itemOutputs("gtnn:heavy_ingot_t2")
    .circuit(1)
    .duration(300)
    .EUt(1920, 1);

  // ---- T3 重型合金锭（IV；Ostrum → 钨钢）----
  e.recipes.gtceu.assembler("kubejs:heavy_ingot_t3")
    .itemInputs("gtnn:heavy_ingot_t2", densePlates("tungsten_steel", 4))
    .inputFluids(Fluid.of("gtceu:platinum", 72))
    .itemOutputs("gtnn:heavy_ingot_t3")
    .circuit(1)
    .duration(300)
    .EUt(7680, 1);

  // ---- T4 重型合金锭（LuV；Calorite → 硅岩合金）----
  e.recipes.gtceu.assembler("kubejs:heavy_ingot_t4")
    .itemInputs("gtnn:heavy_ingot_t3", densePlates("naquadah_alloy", 4))
    .inputFluids(Fluid.of("gcyr:trinaquadalloy", 72))
    .itemOutputs("gtnn:heavy_ingot_t4")
    .circuit(1)
    .duration(300)
    .EUt(30720, 1);
});
