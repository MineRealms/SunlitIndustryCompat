"""Generate TC4R object-aspects data JSONs for GT-namespace material items.

Inputs:
  - phase3-reports/gt-item-materials.json  (live server dump: item -> prefix/material/components)
  - .local/phase3-data/tc4r_base_aspects.json (TC4R 20759 bundled tag rules)
Outputs (into the Pollution mod resources):
  - gt_material_aspects.json (gtceu + pollution, unconditional)
  - gtnn/gcyr/gtmfo_material_aspects.json (forge:conditions mod_loaded)

Rules (plan A): per-material base A:
  a. Pollution 36 aspect materials -> {aspect: 1}
  b. TC4R bundled tag rule for the item's own family -> verbatim (already per-item)
  b'. else any TC4R family rule for that material -> base A
  c. else composition-weighted mean of resolvable components
  d. else null (explicit empty, blocks TC4R recipe inference)
item value = family-rule if present else floor(A * units(prefix)); small/derived forms -> empty.
Ore-bearing stone prefixes are left to TC4R's own data/inference.
Every emitted item id must exist in the live registry dump (unknown ids crash catalog load).
"""
import json, os, re
from collections import OrderedDict

REPORTS = r'G:/emoney-bio-test/phase3-reports'
TC_DATA = r'H:/MinecraftMods/SunlitIndustryCompat/.local/phase3-data'
RES = r'H:/MinecraftMods/Pollution-Unofficial-1.20.1/src/main/resources/data/pollution/thaumcraft/object_aspects'

dump = json.load(open(os.path.join(REPORTS, 'gt-item-materials.json'), encoding='utf-8'))
base = json.load(open(os.path.join(TC_DATA, 'tc4r_base_aspects.json'), encoding='utf-8'))
TAG_RULES = base['tags']

POLLUTION_ASPECT_MATERIALS = {
    'InfusedAir': 'aer', 'InfusedFire': 'ignis', 'InfusedWater': 'aqua',
    'InfusedEarth': 'terra', 'InfusedEntropy': 'perditio', 'InfusedOrder': 'ordo',
    'InfusedCrystal': 'vitreus', 'InfusedLife': 'victus', 'InfusedDeath': 'mortuus',
    'InfusedSoul': 'spiritus', 'InfusedWeapon': 'telum', 'InfusedMetal': 'metallum',
    'InfusedEnergy': 'potentia', 'InfusedInstrument': 'instrumentum',
    'InfusedExchange': 'permutatio', 'InfusedMagic': 'praecantatio',
    'InfusedCold': 'gelum', 'InfusedAura': 'auram', 'InfusedLight': 'lux',
    'InfusedCraft': 'fabrico', 'InfusedVoid': 'vacuos', 'InfusedMotion': 'motus',
    'InfusedTaint': 'vitium', 'InfusedDark': 'tenebrae', 'InfusedAlien': 'alienis',
    'InfusedFly': 'volatus', 'InfusedPlant': 'herba', 'InfusedMechanics': 'machina',
    'InfusedTrap': 'vinculum', 'InfusedUndead': 'exanimis', 'InfusedThought': 'cognitio',
    'InfusedSense': 'sensus', 'InfusedAnimal': 'bestia', 'InfusedHuman': 'humanus',
    'InfusedGreed': 'lucrum', 'InfusedArmor': 'tutamen',
}

FAMILY_OF_PREFIX = {
    'dust': 'dusts', 'ingot': 'ingots', 'gem': 'gems', 'plate': 'plates',
    'block': 'storage_blocks', 'raw': 'raw_materials',
}
ORE_PREFIXES = {'stone', 'granite', 'diorite', 'andesite', 'red_granite', 'marble',
                'deepslate', 'tuff', 'sand', 'redSand', 'gravel', 'basalt', 'netherrack',
                'blackstone', 'endstone', 'moon', 'mars', 'venus', 'mercury',
                'proxima_centauri_b', 'surfaceRock', 'ore'}
UNITS = {
    'dust': 1, 'ingot': 1, 'gem': 1, 'plate': 1, 'raw': 1,
    'block': 9, 'rawOreBlock': 9,
    'flawlessGem': 2, 'exquisiteGem': 4,
}
NULL_PREFIXES = {
    'tinyDust', 'smallDust', 'nugget', 'pureDust', 'purifiedOre', 'crushedOre',
    'refinedOre', 'impureDust', 'hotIngot', 'doublePlate', 'densePlate', 'rod',
    'longRod', 'bolt', 'screw', 'foil', 'gear', 'smallGear', 'frame',
    'wireGtSingle', 'wireGtDouble', 'wireGtQuadruple', 'wireGtOctal', 'wireGtHex',
    'fineWire', 'cableGtSingle', 'cableGtDouble', 'cableGtQuadruple', 'cableGtOctal',
    'cableGtHex', 'ring', 'spring', 'smallSpring', 'rotor', 'round', 'lens',
    'turbineBlade', 'drillHead', 'screwdriverTip', 'wireCutterHead', 'chainsawHead',
    'buzzSawBlade', 'wrenchTip', 'log', 'planks', 'slab', 'fence', 'stairs',
    'fenceGate', 'door', 'catalyst', 'dye',
    'pipeSmallFluid', 'pipeNormalFluid', 'pipeLargeFluid', 'pipeTinyFluid',
    'pipeHugeFluid', 'pipeQuadrupleFluid', 'pipeNonupleFluid', 'pipeSmallItem',
    'pipeNormalItem', 'pipeLargeItem', 'pipeHugeItem', 'pipeSmallRestrictive',
    'pipeNormalRestrictive', 'pipeLargeRestrictive', 'pipeHugeRestrictive',
}
SKIP_PREFIXES = ORE_PREFIXES

def snake(name):
    s = re.sub('([A-Z0-9]+)', '_\\1', name).lower()
    return s.strip('_')

# accept both CamelCase constants and lowercase registry names
ASPECT_BY_SNAKE = {snake(k): v for k, v in POLLUTION_ASPECT_MATERIALS.items()}

def tag_rule(tag_id):
    for rule in TAG_RULES:
        if rule['id'] == tag_id:
            return rule['aspects']
    return None

FAMS = ('dusts', 'ingots', 'gems', 'plates', 'ores', 'raw_materials', 'storage_blocks')

def base_rule_for_material(material, family=None):
    if family:
        rule = tag_rule('forge:%s/%s' % (family, snake(material)))
        if rule:
            return rule
    for fam in FAMS:
        rule = tag_rule('forge:%s/%s' % (fam, snake(material)))
        if rule:
            return rule
    return None

materials = {}
for row in dump['items']:
    if 'material' in row:
        materials.setdefault(row['material'], [(c['m'], c['n']) for c in row.get('comps', [])])

memo = {}

def resolve_base(material, stack=()):
    if material in memo:
        return memo[material]
    if material in stack:
        return None
    memo[material] = None
    result = None
    if material in POLLUTION_ASPECT_MATERIALS:
        result = {POLLUTION_ASPECT_MATERIALS[material]: 1}
    elif snake(material) in ASPECT_BY_SNAKE:
        result = {ASPECT_BY_SNAKE[snake(material)]: 1}
    else:
        rule = base_rule_for_material(material)
        if rule:
            result = dict(rule)
        else:
            comps = materials.get(material) or []
            num, den, any_resolved = {}, 0, False
            # denominator counts ALL components; unresolvable ones contribute zero,
            # so partial knowledge can only undershoot, never amplify.
            den = sum(n for _, n in comps)
            for comp, n in comps:
                sub = resolve_base(comp, stack + (material,))
                if sub is None:
                    continue
                any_resolved = True
                for a, v in sub.items():
                    num[a] = num.get(a, 0) + v * n
            if any_resolved and den > 0:
                result = {a: v / float(den) for a, v in num.items()}
    memo[material] = result
    return result

def scale(aspects, units):
    # Single dominant aspect, exact unit scaling. GT gem recipes run BOTH ways
    # (cutter splits 1 flawless -> 2 gem, engraver merges 2 flawless -> 1
    # exquisite), so per-aspect truncated mixes diverged between tiers and
    # amplified value. One aspect per material scaled exactly by the unit keeps
    # every split/merge recipe value-neutral in primal terms.
    if not aspects:
        return {}
    dom = max(aspects, key=lambda a: (aspects[a], a))
    return {dom: units}

def emit_value(prefix, material):
    family = FAMILY_OF_PREFIX.get(prefix)
    if family:
        rule = base_rule_for_material(material, family)
        if rule:
            return dict(rule), 'family-rule'
    if prefix in UNITS:
        base_a = resolve_base(material)
        if base_a is None:
            return {}, 'null'
        return scale(base_a, UNITS[prefix]), 'scaled'
    if prefix in NULL_PREFIXES:
        return {}, 'null-form'
    return None, 'unknown'

# Explicit entries for items without a GT material association (phase-3 circuit line).
# Templates carry exactly the aspects their TC altar recipe charges, so the GT
# industrial-infusion twins are value-neutral (out == in under the audit).
# Circuits carry nothing: the assembler consumes valued inputs, output adds none.
CIRCUIT_SUPPLEMENT = {
    'pollution:blood_circuit': {},                       # template 1 (vis-only arcane recipe) == MV circuit
    'pollution:blood_circuit_advanced': {'praecantatio': 16, 'metallum': 8, 'vacuos': 8},      # template 2
    'pollution:blood_circuit_ultimate': {'praecantatio': 24, 'alienis': 16, 'metallum': 12},   # template 3
    'pollution:blood_circuit_supreme': {'praecantatio': 32, 'alienis': 24, 'auram': 24},       # template 4
    'pollution:blood_circuit.1': {}, 'pollution:blood_circuit.2': {},
    'pollution:blood_circuit.3': {}, 'pollution:blood_circuit.4': {},
    'pollution:blood_circuit.5': {}, 'pollution:blood_circuit.6': {},
}

def main():
    by_ns = {}
    stats = {'family-rule': 0, 'scaled': 0, 'null': 0, 'null-form': 0, 'skipped-ore': 0,
             'skipped-noassoc': 0, 'skipped-unknown-prefix': 0}
    unknown_prefixes = set()
    for row in dump['items']:
        iid = row['id']
        ns = iid.split(':')[0]
        if ns not in ('gtceu', 'pollution', 'gtnn', 'gcyr', 'gtmfo'):
            continue
        if 'material' not in row:
            stats['skipped-noassoc'] += 1
            continue
        prefix, material = row['prefix'], row['material']
        if prefix in SKIP_PREFIXES:
            stats['skipped-ore'] += 1
            continue
        val, why = emit_value(prefix, material)
        if val is None:
            unknown_prefixes.add(prefix)
            stats['skipped-unknown-prefix'] += 1
            continue
        stats[why] += 1
        by_ns.setdefault(ns, {})[iid] = val

    os.makedirs(RES, exist_ok=True)
    written = []
    merged_main = {'direct': {}}
    for ns in ('gtceu', 'pollution'):
        merged_main['direct'].update(by_ns.get(ns, {}))
    merged_main['direct'].update(CIRCUIT_SUPPLEMENT)
    path = os.path.join(RES, 'gt_material_aspects.json')
    with open(path, 'w', encoding='utf-8', newline='\n') as fh:
        json.dump(merged_main, fh, ensure_ascii=False, sort_keys=True, indent=1)
        fh.write('\n')
    written.append(('gt_material_aspects.json', len(merged_main['direct'])))
    for ns in ('gtnn', 'gcyr', 'gtmfo'):
        if not by_ns.get(ns):
            continue
        doc = {'forge:conditions': [{'type': 'forge:mod_loaded', 'modid': ns}],
               'direct': OrderedDict(sorted(by_ns.get(ns, {}).items()))}
        path = os.path.join(RES, '%s_material_aspects.json' % ns)
        with open(path, 'w', encoding='utf-8', newline='\n') as fh:
            json.dump(doc, fh, ensure_ascii=False, sort_keys=True, indent=1)
            fh.write('\n')
        written.append(('%s_material_aspects.json' % ns, len(doc['direct'])))

    print('stats:', json.dumps(stats))
    print('unknown prefixes:', sorted(unknown_prefixes))
    for name, n in written:
        print('wrote %-34s %d entries' % (name, n))



if __name__ == '__main__':
    main()
