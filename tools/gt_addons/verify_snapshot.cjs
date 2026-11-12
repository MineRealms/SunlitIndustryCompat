const fs = require('node:fs');
const assert = require('node:assert/strict');
const crypto = require('node:crypto');

const [snapshotPath, previousPath] = process.argv.slice(2);
assert(snapshotPath, 'Usage: node verify_snapshot.cjs snapshot.json [previous.json]');
const recipes = JSON.parse(fs.readFileSync(snapshotPath, 'utf8'));
const elevatorId = 'society:assembly_line/gtca/space_elevator_controller';
const targets = [
  [elevatorId, 'gtca:space_elevator', 1, 'gtca:nanotube_spool', 1966080],
  ['gtceu:assembly_line/piko_wafer_ic', 'gtca:piko_wafer_ic', 64, 'gtca:quantum_anomaly', 491520],
  ['gtceu:assembly_line/nanotube_spool', 'gtca:nanotube_spool', 1, 'gtca:carbon_nanites', 491520],
];

function items(recipe, direction) {
  return (recipe[direction]?.item ?? []).map(entry => entry.content);
}

function containsItem(recipe, direction, id, count) {
  return items(recipe, direction).some(content =>
    content.ingredient?.item === id && (count === undefined || content.count === count));
}

function voltage(recipe) {
  return recipe.tickInputs.eu[0].content;
}

for (const [id, output, count, sample, researchVoltage] of targets) {
  const recipe = recipes[id];
  assert(recipe, `Missing recipe ${id}`);
  assert.equal(recipe.type, 'gtceu:assembly_line', id);
  assert.equal(voltage(recipe), 1966080, `${id}: UHV voltage`);
  assert(containsItem(recipe, 'outputs', output, count), `${id}: output`);
  assert(items(recipe, 'inputs').some(content =>
    content.ingredient?.tag === 'gtceu:circuits/uhv'), `${id}: UHV circuit tag`);
  const condition = recipe.recipeConditions.find(entry => entry.type === 'research');
  assert.equal(condition.research.length, 1, `${id}: research condition`);
  const entry = condition.research[0];
  const research = recipes[`gtceu:research_station/${entry.researchId}`];
  assert(research, `${id}: missing research producer ${entry.researchId}`);
  assert(containsItem(research, 'inputs', sample, 1), `${id}: research sample`);
  assert(containsItem(research, 'inputs', entry.dataItem.id, 1), `${id}: blank medium`);
  assert.equal(voltage(research), researchVoltage, `${id}: research voltage`);
  assert.equal(research.tickInputs.cwu[0].content, 128, `${id}: research CWU/t`);
  assert.equal(research.duration, 512000, `${id}: total research CWU`);
  const data = items(research, 'outputs')[0].ingredient;
  assert.equal(data.item, entry.dataItem.id, `${id}: research medium`);
  const fields = entry.dataItem.tag.assembly_line_research;
  assert.equal(fields.research_id, entry.researchId, `${id}: condition research ID`);
  assert.equal(fields.research_type, recipe.type, `${id}: condition recipe type`);
  for (const [name, value] of Object.entries(fields)) {
    assert(data.nbt.includes(`${name}:"${value}"`), `${id}: research NBT ${name}`);
  }
}

const elevator = recipes[elevatorId];
assert(containsItem(elevator, 'inputs', 'gcyr:space_station_packager', 1));
assert(containsItem(elevator, 'inputs', 'gcyr:space_fabric', 4));
assert(containsItem(elevator, 'inputs', 'gtnn:heavy_plate_t4', 4));
assert(elevator.inputs.fluid.some(entry =>
  entry.content.amount === 5760 && entry.content.value[0].fluid === 'gtca:incoloy_ma_323'));
assert.equal(Object.values(recipes).filter(recipe =>
  containsItem(recipe, 'outputs', 'gtca:space_elevator')).length, 1, 'Duplicate elevator producers');
assert(!recipes['gtceu:assembly_line/space_elevator_controller'], 'Native elevator was not replaced');
assert(!recipes['gtceu:research_station/1x_gtca_nanotube_spool'], 'Stale native elevator research');

for (const [index, eu] of [480, 1920, 7680, 30720].entries()) {
  const recipe = recipes[`kubejs:assembler/heavy_ingot_t${index + 1}`];
  assert(recipe, `Missing heavy alloy T${index + 1}`);
  assert.equal(voltage(recipe), eu);
  assert(containsItem(recipe, 'outputs', `gtnn:heavy_ingot_t${index + 1}`, 1));
}
assert.deepEqual(recipes._validation.invalidItems, [], 'Empty runtime item ingredients');

function canonical(value) {
  if (Array.isArray(value)) return value.map(canonical);
  if (value && typeof value === 'object') {
    return Object.fromEntries(Object.keys(value).sort().map(key => [key, canonical(value[key])]));
  }
  return value;
}
const canonicalRecipes = canonical(recipes);
if (previousPath) {
  const previous = canonical(JSON.parse(fs.readFileSync(previousPath, 'utf8')));
  assert.deepEqual(canonicalRecipes, previous, 'Recipe registry changed across reload');
}
const digest = crypto.createHash('sha256').update(JSON.stringify(canonicalRecipes)).digest('hex');
console.log(JSON.stringify({
  result: 'PASS', targets: targets.length, heavyAlloyTiers: 4,
  totalRecipes: recipes._validation.totalRecipes,
  snapshotRecipes: Object.keys(recipes).length - 1, sha256: digest,
  reloadCompared: Boolean(previousPath),
}, null, 2));
