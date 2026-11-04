"""Generate pack-side loot tables for Middle-earth blocks that drop nothing.

Input : <server>/phase4-reports/me_loot_audit.json (me_loot_audit.snippet)
Output: <pack>/kubejs/data/<ns>/loot_tables/<path>.json for every me:* block whose
        default state drops nothing with a pickaxe, a silk-touch pickaxe or shears,
        written at the table id the block actually asks for (Block#getLootTable).

The upstream mod (1.21 Fabric datagen) never generated these tables; the port inherited
the gap. Rules mirror the vanilla block families the names belong to. Blocks whose
table id is not in the me namespace are reported, not written (overriding another
mod's or vanilla's table would change that block too).

Usage: python -I gen_me_loot.py <audit.json> <pack_dir> [--check]
"""
import json
import re
import sys
from pathlib import Path

NO_DROP = re.compile(r'(layered_cake|^budding_|trial_spawner$|_vault$|structure_manager$|^glowworm_main$)')
SILK_ONLY = re.compile(r'(glass|icicles$|^floating_ice$|^sticky_ice$|larva_egg$)')
SHEARS_OR_SILK = re.compile(r'(_growth$|ivy$|_vines$|webbing$|fallen_.*leaves$|^wild_grass$|^false_oatgrass$|^clovers$|^bracken$|^large_blue_fescue$|^pine_branches$)')
SOIL = re.compile(r'^(chalksoil|loam|peat|silt)_(grass_block|path|farmland)$')

SURVIVES = {"condition": "minecraft:survives_explosion"}
SILK = {"condition": "minecraft:match_tool", "predicate": {"enchantments": [{"enchantment": "minecraft:silk_touch", "levels": {"min": 1}}]}}
SHEARS = {"condition": "minecraft:match_tool", "predicate": {"items": ["minecraft:shears"]}}


def item_entry(item, functions=None):
    entry = {"type": "minecraft:item", "name": item}
    if functions:
        entry["functions"] = functions
    return entry


def table(pools):
    return {"type": "minecraft:block", "pools": pools}


def pool(entries, conditions=None):
    p = {"rolls": 1.0, "bonus_rolls": 0.0, "entries": entries}
    if conditions:
        p["conditions"] = conditions
    return p


def state_condition(block, prop, value):
    return {"condition": "minecraft:block_state_property", "block": block, "properties": {prop: value}}


def alternative(conditions):
    return {"condition": "minecraft:alternative", "terms": conditions}


def build(row, items):
    bid = row["id"]
    name = bid.split(":", 1)[1]
    item = row["item"]
    props = row["props"]
    has_item = item != "minecraft:air"

    if name.endswith("candles_layered_cake"):
        color = name.removesuffix("candles_layered_cake").rstrip("_")
        candle = "minecraft:" + (color + "_" if color else "") + "candle"
        return table([pool([item_entry(candle)], [SURVIVES])]), "candle cake: candle only"
    if name in ("small_lily_pads", "small_flowering_lily_pads", "lily_pads", "flowering_lily_pads", "large_lily_pad", "large_flowering_lily_pad"):
        return table([pool([item_entry("minecraft:lily_pad")], [SURVIVES])]), "lily without item form: vanilla lily pad"
    if name in ("duckweed", "floating_ice"):
        return table([]), "no-drop (no registered item form)"
    if NO_DROP.search(name):
        return table([]), "no-drop (vanilla-equivalent: cake/budding/spawner/vault/technical)"
    if name.startswith("potted_"):
        plant = "me:" + name[len("potted_"):]
        entries = [pool([item_entry("minecraft:flower_pot")], [SURVIVES])]
        if plant in items:
            entries.append(pool([item_entry(plant)], [SURVIVES]))
        return table(entries), "flower pot + plant"
    if not has_item:
        return None, "no item form; skipped"

    conditions = [SURVIVES]
    if "half" in props and "lower" in props["half"]:
        conditions.append(state_condition(bid, "half", "lower"))
    if "part" in props and "head" in props["part"]:
        conditions.append(state_condition(bid, "part", "head"))
    if "part" in props and "bottom" in props["part"]:
        conditions.append(state_condition(bid, "part", "bottom"))
    if name.endswith("_coin_pile"):
        children = [{"type":"minecraft:item", "name":item, "conditions":[state_condition(bid,"stage",v)],
                     "functions":[{"function":"minecraft:set_count","count":int(v)+1}]} for v in props["stage"]]
        return table([pool([{"type":"minecraft:alternatives","children":children}], [SURVIVES])]), "coin pile: one item per stage + 1"
    if name in ("strawberry_bush", "tough_berry_bush"):
        return table([pool([item_entry(item, [{"function":"minecraft:set_count","count":{"type":"minecraft:uniform","min":1,"max":2}}])],
                           [SURVIVES, {"condition":"minecraft:block_state_property","block":bid,"properties":{"age":{"min":"2"}}}])]), "berry bush: fruit only when mature"

    if name.endswith("_ore") and any(name.endswith("_" + gem + "_ore") for gem in ("ruby", "sapphire", "emerald", "adamant")):
        gem = name.rsplit("_", 2)[1]
        drop = "minecraft:emerald" if gem == "emerald" else "me:" + gem
        if drop not in items:
            return None, "gem item missing; manual review required"
        return table([pool([{"type": "minecraft:alternatives", "children": [
            {"type": "minecraft:item", "name": item, "conditions": [SILK]},
            item_entry(drop, [{"function": "minecraft:apply_bonus", "enchantment": "minecraft:fortune", "formula": "minecraft:ore_drops"},
                              {"function": "minecraft:explosion_decay"}])]}])]), "gem ore: silk self, else gem + fortune"
    if SILK_ONLY.search(name):
        return table([pool([item_entry(item)], [SILK] + conditions[1:])]), "silk touch only (glass/ice family)"
    if SHEARS_OR_SILK.search(name):
        return table([pool([item_entry(item)], [alternative([SHEARS, SILK])] + conditions[1:])]), "shears or silk touch (vine/grass family)"
    m = SOIL.match(name)
    if m:
        base = "me:" + m.group(1)
        if base not in items:
            base = "minecraft:dirt"
        if m.group(2) == "grass_block":
            return table([pool([{"type": "minecraft:alternatives", "children": [
                {"type": "minecraft:item", "name": item, "conditions": [SILK]},
                {"type": "minecraft:item", "name": base, "conditions": [SURVIVES]}]}])]), "grass block: silk self, else soil"
        return table([pool([item_entry(base)], [SURVIVES])]), "path/farmland: soil"
    if name in ("snowy_grass_block", "lorien_podzol"):
        return table([pool([{"type": "minecraft:alternatives", "children": [
            {"type": "minecraft:item", "name": item, "conditions": [SILK]},
            {"type": "minecraft:item", "name": "minecraft:dirt", "conditions": [SURVIVES]}]}])]), "grass/podzol: silk self, else dirt"
    if name.endswith("_leaves"):
        sapling = None
        for candidate in ("me:" + name.replace("_leaves", "_sapling"),
                          "me:" + re.sub(r'^(orange|red|yellow)_', '', name).replace("_leaves", "_sapling")):
            if candidate in items:
                sapling = candidate
                break
        children = [{"type": "minecraft:item", "name": item, "conditions": [alternative([SHEARS, SILK])]}]
        if sapling:
            children.append({"type": "minecraft:item", "name": sapling, "conditions": [SURVIVES, {
                "condition": "minecraft:table_bonus", "enchantment": "minecraft:fortune",
                "chances": [0.05, 0.0625, 0.083333336, 0.1]}]})
        pools = [pool([{"type": "minecraft:alternatives", "children": children}])]
        pools.append(pool([item_entry("minecraft:stick", [{"function": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": 1.0, "max": 2.0}}])],
                          [{"condition": "minecraft:inverted", "term": alternative([SHEARS, SILK])},
                           {"condition": "minecraft:table_bonus", "enchantment": "minecraft:fortune",
                            "chances": [0.02, 0.022222223, 0.025, 0.033333335, 0.1]}]))
        return table(pools), "leaves: shears/silk self, else sapling chance + sticks"
    if "layers" in props:
        entries = []
        for value in props["layers"]:
            count = int(value)
            entries.append({"type": "minecraft:item", "name": item,
                            "conditions": [state_condition(bid, "layers", value)],
                            "functions": [{"function": "minecraft:set_count", "count": float(count)}]})
        return table([pool([{"type": "minecraft:alternatives", "children": entries}], [SURVIVES])]), "layered: one item per layer"
    return table([pool([item_entry(item)], conditions)]), "self"


def main():
    audit = json.loads(Path(sys.argv[1]).read_text(encoding="utf-8"))
    pack = Path(sys.argv[2])
    check = "--check" in sys.argv
    items = set(audit.get("registeredItems", [])) | {row["item"] for row in audit["blocks"]}
    written, skipped, foreign = [], [], []
    for row in audit["blocks"]:
        if row["dropsAnything"] or row["tableLoaded"]:
            continue
        # Never infer a missing loot table from a single random empty roll, an invalid default
        # plant state or absent block entity. Existing tables are audited separately, not overwritten.
        ns, path = row["table"].split(":", 1)
        if ns != "me" or not path.startswith("blocks/"):
            foreign.append((row["id"], row["table"]))
            continue
        loot, why = build(row, items)
        if loot is None:
            skipped.append((row["id"], why))
            continue
        out = pack / "kubejs" / "data" / ns / "loot_tables" / (path + ".json")
        text = json.dumps(loot, indent=2) + "\n"
        if check:
            if not out.exists() or out.read_text(encoding="utf-8") != text:
                print("DIFF", out)
        else:
            out.parent.mkdir(parents=True, exist_ok=True)
            out.write_text(text, encoding="utf-8")
        written.append((row["id"], why))
    summary = {}
    for _, why in written:
        summary[why] = summary.get(why, 0) + 1
    print(json.dumps({"written": len(written), "by_rule": summary, "skipped": skipped, "foreign_table": foreign},
                     ensure_ascii=False, indent=1))


if __name__ == "__main__":
    main()
