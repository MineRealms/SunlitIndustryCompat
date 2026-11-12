const fs = require('node:fs');
const assert = require('node:assert/strict');

const recipes = JSON.parse(fs.readFileSync(process.argv[2], 'utf8'));
const itemEntries = (recipe, side) => recipe[side]?.item ?? [];
const hasItem = (recipe, side, item, count) => itemEntries(recipe, side).some(entry =>
  entry.content.ingredient?.item === item && (count === undefined || entry.content.count === count));
const voltage = recipe => recipe.tickInputs.eu[0].content;
const requireRecipe = id => {
  assert(recipes[id], `Missing ${id}`);
  return recipes[id];
};

const researchIds = new Set();
function assembly(path, output, eu, sample) {
  const id = `gtceu:assembly_line/${path}`;
  const recipe = requireRecipe(id);
  assert.equal(voltage(recipe), eu, id);
  assert(hasItem(recipe, 'outputs', output, 1), `${id}: output`);
  const research = recipe.recipeConditions.find(condition => condition.type === 'research').research;
  assert.equal(research.length, 1, id);
  const condition = research[0];
  assert(!researchIds.has(condition.researchId), `Shared research ${condition.researchId}`);
  researchIds.add(condition.researchId);
  const producer = requireRecipe(`gtceu:research_station/${condition.researchId}`);
  assert(hasItem(producer, 'inputs', sample, 1), `${id}: research sample`);
  assert(hasItem(producer, 'inputs', condition.dataItem.id, 1), `${id}: blank medium`);
  assert.equal(producer.tickInputs.cwu[0].content > 0, true, id);
  assert.equal(condition.dataItem.tag.assembly_line_research.research_id, condition.researchId);
  const data = itemEntries(producer, 'outputs')[0].content.ingredient;
  assert.equal(data.item, condition.dataItem.id, id);
  assert(data.nbt.includes(`research_id:"${condition.researchId}"`), `${id}: research payload`);
  return recipe;
}

const tiers = ['luv', 'zpm', 'uv'];
const marks = ['mki', 'mkii', 'mkiii'];
const eu = [30720, 122880, 491520];
for (const [kind, component] of [
  ['miner', 'sensor'], ['pump', 'electric_pump'], ['assembler', 'robot_arm'],
]) {
  for (let i = 0; i < 3; i++) {
    const output = `gtca:space_${kind}_${marks[i]}`;
    const previous = i ? `gtca:space_${kind}_${marks[i - 1]}` : null;
    const recipe = assembly(`space_${kind}_${marks[i]}_controller`, output, eu[i],
      previous ?? `gtceu:luv_${component}`);
    assert.equal(recipe.duration, (i + 1) * 1200);
    assert(hasItem(recipe, 'inputs', 'gtca:piko_wafer_ic', (i + 1) * 8));
    assert(itemEntries(recipe, 'inputs').some(entry =>
      entry.content.ingredient?.tag === `gtceu:circuits/${tiers[i]}`));
    if (previous) assert(hasItem(recipe, 'inputs', previous, 1));
  }
}

for (let mk = 1; mk <= 5; mk++) {
  const previous = mk === 1 ? 'gtca:space_elevator_base_casing' : `gtca:space_elevator_motor_mk_${mk - 1}`;
  const recipe = assembly(`space_elevator_motor_mk_${mk}`, `gtca:space_elevator_motor_mk_${mk}`,
    mk === 1 ? 491520 : 1966080, mk === 1 ? 'gtceu:uv_electric_motor' : previous);
  assert(hasItem(recipe, 'inputs', previous, 1));
}

const droneTiers = ['lv', 'mv', 'hv', 'ev', 'iv', 'luv', 'zpm', 'uv', 'uhv', 'uev'];
for (let i = 0; i < droneTiers.length; i++) {
  const tier = droneTiers[i];
  const previous = i ? `gtca:${droneTiers[i - 1]}_drone` : null;
  const recipe = i < 8 ? requireRecipe(`gtceu:assembler/space_mining_drone_${tier}`) :
    assembly(`space_mining_drone_${tier}`, `gtca:${tier}_drone`, 1966080, previous);
  assert(hasItem(recipe, 'outputs', `gtca:${tier}_drone`, 1));
  if (previous) assert(hasItem(recipe, 'inputs', previous, 1));
}

for (const [suffix, output] of [
  ['concrete_floor', 'ultra_high_strength_concrete_floor'],
  ['reinforced_stone', 'neutronium_plated_reinforced_stone'],
  ['base_casing', 'space_elevator_base_casing'],
  ['internal_structure', 'space_elevator_internal_structure'],
  ['support_structure', 'space_elevator_support_structure'],
  ['cable', 'space_elevator_cable'],
]) {
  const recipe = requireRecipe(`gtceu:assembler/space_elevator_${suffix}`);
  assert(hasItem(recipe, 'outputs', `gtca:${output}`));
}

const drillMaterials = ['steel', 'titanium', 'tungsten_steel', 'naquadah', 'naquadah_alloy', 'neutronium'];
for (const [i, material] of drillMaterials.entries()) {
  const mk = i < 3 ? 1 : i === 3 ? 2 : 3;
  for (const product of ['drill_heads', 'drill_rods']) {
    const recipe = requireRecipe(`gtceu:space_assembler/space_assembler_${material}_${product}`);
    assert.equal(recipe.data.required_module_mk, mk);
    assert.equal(voltage(recipe), [32768, 131072, 524288][mk - 1]);
    assert.equal(itemEntries(recipe, 'outputs')[0].content.count, product === 'drill_heads' ? 4 : 16);
  }
}

function normalizedInputs(recipe) {
  return Object.fromEntries(itemEntries(recipe, 'inputs').map(entry => {
    const ingredient = entry.content.ingredient;
    return [ingredient.item ?? `#${ingredient.tag}`, entry.content.count];
  }));
}
for (const [suffix, mk] of [['internal_structure', 1], ['base_casing', 2], ['cable', 3]]) {
  const ground = requireRecipe(`gtceu:assembler/space_elevator_${suffix}`);
  const orbital = requireRecipe(`society:space_assembler/gtca/orbital_${suffix}`);
  assert.equal(orbital.data.required_module_mk, mk);
  const inputs = normalizedInputs(ground);
  for (const key of Object.keys(inputs)) inputs[key] *= 4;
  assert.deepEqual(normalizedInputs(orbital), inputs, `${suffix}: material conservation`);
  assert.equal(orbital.inputs.fluid[0].content.amount, ground.inputs.fluid[0].content.amount * 4);
  assert.equal(itemEntries(orbital, 'outputs')[0].content.count,
    itemEntries(ground, 'outputs')[0].content.count * 4);
}

assert(!recipes['gtceu:space_miner/ha'], 'Leaked gold nanite test recipe');
const mining = Object.entries(recipes).filter(([id]) => id.startsWith('gtceu:space_miner/space_miner_'));
assert(mining.length > 0, 'No usable mining recipes');
for (const [id, recipe] of mining) {
  assert(itemEntries(recipe, 'inputs').some(entry =>
    entry.content.ingredient?.item?.startsWith('gtca:') &&
    entry.content.ingredient.item.endsWith('_drone') && entry.chance === 0), `${id}: reusable drone`);
  assert(recipe.inputs.fluid.length > 0, `${id}: missing plasma`);
  assert(recipe.tickInputs.cwu[0].content > 0, `${id}: missing computation`);
}
assert.deepEqual(recipes._validation.invalidItems, []);
assert.deepEqual(recipes._validation.invalidQuestItems, []);
assert(recipes._validation.requestedQuestItems > 0, 'Quest manifest was not read');
console.log(JSON.stringify({result: 'PASS', moduleControllers: 9, motors: 5, drones: 10,
  structuralRecipes: 6, drillBatches: 12, orbitalBatches: 3, distinctResearch: researchIds.size,
  miningRecipes: mining.length, questItemIds: recipes._validation.requestedQuestItems}, null, 2));
