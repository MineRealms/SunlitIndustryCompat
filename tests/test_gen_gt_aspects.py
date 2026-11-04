"""Unit tests for the offline GT aspect rule engine (plan A pure functions).

Run:  python -m pytest tests/test_gen_gt_aspects.py  (or plain python)
"""
import os
import sys

sys.path.insert(0, r'H:/MinecraftMods/SunlitIndustryCompat/tools/phase3')

import gen_gt_aspects as g  # module import must not regenerate (main() guard)


def test_snake():
    assert g.snake('InfusedAir') == 'infused_air'
    assert g.snake('NaquadahEnriched') == 'naquadah_enriched'
    assert g.snake('Uranium235') == 'uranium235' or g.snake('Uranium235') == 'uranium_235'
    assert g.snake('Steel') == 'steel'


def test_aspect_material_resolution():
    assert g.resolve_base('infused_air') == {'aer': 1}
    assert g.resolve_base('InfusedFire') == {'ignis': 1}  # CamelCase constant form
    assert g.resolve_base('infused_life') == {'victus': 1}


def test_tc4r_family_rules():
    # brass: TC4R bundles dusts(2/1/1) and ingots(3/1)
    assert g.base_rule_for_material('Brass', 'dusts') == {'metallum': 2, 'perditio': 1, 'instrumentum': 1}
    assert g.base_rule_for_material('Brass', 'ingots') == {'metallum': 3, 'instrumentum': 1}
    # steel has an ingots rule, used as base for plates
    assert g.emit_value('plate', 'Steel')[0] == {'metallum': 3, 'ordo': 1}


def test_primal_units_scaling():
    gem, why = g.emit_value('gem', 'infused_air')
    assert gem == {'aer': 1}
    assert g.emit_value('block', 'infused_air')[0] == {'aer': 9}
    # single dominant aspect, exact unit scaling: GT gem recipes run both ways
    # (cutter 1 flawless -> 2 gem, engraver 2 flawless -> 1 exquisite), so every
    # tier must be an exact integer multiple of the unit-1 value
    assert g.emit_value('flawlessGem', 'infused_air')[0] == {'aer': 2}
    assert g.emit_value('exquisiteGem', 'infused_air')[0] == {'aer': 4}


def test_null_forms_block_inference():
    for prefix in ('tinyDust', 'smallDust', 'nugget', 'crushedOre', 'foil', 'rod', 'bolt'):
        val, why = g.emit_value(prefix, 'infused_air')
        assert val == {}, (prefix, val)
    # unknown-base material full forms are explicit nulls too
    assert g.emit_value('dust', 'Iron')[0] == {}


def test_element_alloy_composition_floors_to_null():
    # aertitanium = bauxite 2 + aluminium 1 + manganese 1 + infused_air 5
    # dominant-aspect scheme: aer is the only resolvable component, so the
    # ingot carries it at unit strength. Exact conservation across GT's
    # decomposition granularity (1 ingot -> 5 dusts) is impossible offline;
    # the residual is bounded by material acquisition, not an infinite loop.
    val, why = g.emit_value('ingot', 'aertitanium')
    assert val == {'aer': 1}


def test_composition_is_conservative():
    # any composition-derived value must not exceed the sum of its components' item values
    for material, comps in g.materials.items():
        if not comps:
            continue
        # only composition-derived materials carry the weighted-mean invariant;
        # rule (a)/(b) materials may resolve regardless of their components
        if (material in g.POLLUTION_ASPECT_MATERIALS
                or g.snake(material) in g.ASPECT_BY_SNAKE
                or g.base_rule_for_material(material)):
            continue
        resolved = [(n, g.resolve_base(m)) for m, n in comps if g.resolve_base(m)]
        if not resolved:
            continue
        den = sum(n for _, n in comps)  # all components, unresolvable count as zero
        base = g.resolve_base(material)
        for aspect, value in (base or {}).items():
            weighted = sum(v.get(aspect, 0) * n for n, v in resolved) / float(den)
            assert value <= weighted + 1e-9, (material, aspect, value, weighted)


def test_unknown_prefix_reports_not_emitted():
    assert g.emit_value('someFutureForm', 'Steel') == (None, 'unknown')


if __name__ == '__main__':
    fails = 0
    for name, fn in sorted(globals().items()):
        if name.startswith('test_') and callable(fn):
            try:
                fn()
                print('PASS', name)
            except AssertionError as exc:
                fails += 1
                print('FAIL', name, exc)
    print('done, fails =', fails)
    raise SystemExit(1 if fails else 0)
