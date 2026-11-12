"""Deterministic FTBQ support shared by the two Sunlit industry chapters."""

import argparse
import hashlib
import json
import math
import re
from pathlib import Path

import nbtlib
from build_astral_sorcery_chapter import SnbtParser

TOOLS = Path(__file__).resolve().parent
ROOT = TOOLS.parents[2]
LIBRARY = ROOT / "config/ftbquests/quests"
GROUP = "4A46A5E1358A80A6"


def quest(key, title, icon, xy, desc, deps=(), items=(), check=None, ext=()):
    return dict(key=key, title=title, icon=icon, xy=xy, desc=desc,
                deps=list(deps), items=list(items), check=check, ext=list(ext))


def dump(value, depth=0):
    tab = "\t" * depth
    if isinstance(value, dict):
        return "{\n" + ",\n".join(
            tab + "\t" + key + ": " + dump(child, depth + 1)
            for key, child in value.items()) + "\n" + tab + "}"
    if isinstance(value, list):
        return ("[\n" + ",\n".join(tab + "\t" + dump(child, depth + 1)
                for child in value) + "\n" + tab + "]") if value else "[ ]"
    if isinstance(value, str):
        return json.dumps(value, ensure_ascii=False)
    if isinstance(value, bool):
        return str(value).lower()
    if isinstance(value, float):
        return f"{value:.1f}d"
    return str(value)


def walk(value):
    if isinstance(value, dict):
        yield value
        for child in value.values():
            yield from walk(child)
    elif isinstance(value, list):
        for child in value:
            yield from walk(child)


def nbt_normalize(value):
    if isinstance(value, bool):
        return int(value)
    if isinstance(value, dict):
        return {key: nbt_normalize(child) for key, child in value.items()}
    if isinstance(value, list):
        return [nbt_normalize(child) for child in value]
    return value


def first_diff(left, right, path="root"):
    if type(left) is not type(right):
        return path + " types=" + type(left).__name__ + "/" + type(right).__name__
    if isinstance(left, dict):
        if left.keys() != right.keys():
            return path + " keys differ"
        for key in left:
            result = first_diff(left[key], right[key], path + "." + str(key))
            if result:
                return result
    elif isinstance(left, list):
        if len(left) != len(right):
            return path + " length differs"
        for index, (a, b) in enumerate(zip(left, right)):
            result = first_diff(a, b, path + "[" + str(index) + "]")
            if result:
                return result
    elif left != right:
        return path + " values differ: " + repr(left) + "/" + repr(right)
    return ""


def library(output, library_root=LIBRARY):
    definitions, quests = {}, set()
    for path in sorted(library_root.rglob("*.snbt")):
        if path.name == output.name:
            continue
        doc = SnbtParser(path.read_text(encoding="utf-8-sig")).parse()
        for obj in walk(doc):
            if "id" in obj and isinstance(obj["id"], str):
                definitions.setdefault(obj["id"], []).append(str(path.relative_to(library_root)))
        quests.update(obj["id"] for obj in doc.get("quests", ()) if "id" in obj)
    return definitions, quests


def valid_id(value):
    return isinstance(value, str) and re.fullmatch(r"[0-7][0-9A-F]{15}", value) is not None and int(value, 16) > 0


def allocate(spec, external):
    path = TOOLS / (spec["filename"] + "_ids.json")
    old = json.loads(path.read_text(encoding="utf-8")) if path.exists() else {}
    if old and (old.get("schema") != 1 or old.get("salt") != spec["salt"] or old.get("group") != GROUP):
        raise ValueError("ID registry owner/schema changed")
    keys = {"chapter"}
    for q in spec["quests"]:
        keys.add("quest/" + q["key"])
        keys.update("task/" + q["key"] + "/item/" + item for item in q["items"])
        if q["check"]:
            keys.add("task/" + q["key"] + "/check")
    ids = {key: value for key, value in old.get("ids", {}).items() if key in keys}
    retired = dict(old.get("retired_ids", {}))
    retired.update({key: value for key, value in old.get("ids", {}).items()
                    if key not in keys and key not in spec.get("moved_keys", ())})
    for key, value in spec.get("preserved", {}).items():
        if key not in keys or key in ids and ids[key] != value:
            raise ValueError("Preserved objective changed: " + key)
        ids[key] = value
    reserved = set(external) | set(ids.values()) | set(retired.values())
    for key in sorted(keys - ids.keys()):
        attempt = 0
        while True:
            suffix = f":{attempt}" if attempt else ""
            value = int.from_bytes(hashlib.sha256(f"{spec['salt']}:{key}{suffix}".encode()).digest()[:8], "big") & 0x7FFFFFFFFFFFFFFF
            candidate = f"{value:016X}"
            if value and candidate not in reserved:
                ids[key] = candidate
                reserved.add(candidate)
                break
            attempt += 1
    if not all(valid_id(value) for value in [*ids.values(), *retired.values()]):
        raise ValueError("Invalid registry ID")
    if len(ids) != len(set(ids.values())):
        raise ValueError("Repeated active registry ID")
    return ids, retired


def build(spec, ids):
    doc = dict(default_hide_dependency_lines=False, default_quest_shape="square",
               filename=spec["filename"], group=GROUP, icon=spec["icon"],
               id=ids["chapter"], order_index=spec["order"], quest_links=[],
               quests=[], title=spec["title"])
    keys = {q["key"] for q in spec["quests"]}
    if len(keys) != len(spec["quests"]):
        raise ValueError("Repeated quest key")
    for q in spec["quests"]:
        if set(q["deps"]) - keys:
            raise ValueError("Unknown prerequisite for " + q["key"])
        tasks = [dict(id=ids["task/" + q["key"] + "/item/" + item], type="item", item=item,
                      count=1, consume_items=False, match_nbt=False, only_from_crafting=False)
                 for item in q["items"]]
        if q["check"]:
            tasks.append(dict(id=ids["task/" + q["key"] + "/check"], title=q["check"], type="checkmark"))
        if not tasks:
            raise ValueError("Missing objective for " + q["key"])
        doc["quests"].append(dict(description=q["desc"],
            dependencies=[ids["quest/" + key] for key in q["deps"]] + q["ext"],
            icon=q["icon"], id=ids["quest/" + q["key"]], tasks=tasks,
            title=q["title"], x=float(q["xy"][0]), y=float(q["xy"][1])))
    return doc


def validate(doc, external, external_quests, minimum_spacing=2.5, library_root=LIBRARY):
    definitions = [obj["id"] for obj in walk(doc) if "id" in obj]
    if len(definitions) != len(set(definitions)) or not all(valid_id(value) for value in definitions):
        raise ValueError("Invalid or repeated chapter/quest/objective ID")
    collision = set(definitions) & external.keys()
    if collision:
        raise ValueError("Global ID collision: " + repr({key: external[key] for key in collision}))
    graph = {q["id"]: q for q in doc["quests"]}
    missing = {dep for q in graph.values() for dep in q["dependencies"]} - graph.keys() - external_quests
    if missing:
        raise ValueError("Missing quest prerequisite: " + repr(sorted(missing)))
    groups = SnbtParser((library_root / "chapter_groups.snbt").read_text(encoding="utf-8-sig")).parse()
    if not any(g["id"] == GROUP for g in groups.get("chapter_groups", ())):
        raise ValueError("GT chapter group missing")
    positions = list(graph.values())
    minimum = min(math.dist((a["x"], a["y"]), (b["x"], b["y"]))
                  for i, a in enumerate(positions) for b in positions[i + 1:])
    if minimum < minimum_spacing:
        raise ValueError("Quest positions overlap")
    for q in positions:
        if not all(math.isfinite(q[key]) for key in ("x", "y")):
            raise ValueError("Nonfinite quest position")
        for dep in q["dependencies"]:
            if dep not in graph:
                continue
            start = graph[dep]
            if start["x"] >= q["x"]:
                raise ValueError("Local prerequisite must point right: " + q["title"])
            dx, dy = q["x"] - start["x"], q["y"] - start["y"]
            for other in positions:
                if other["id"] in (dep, q["id"]):
                    continue
                t = ((other["x"] - start["x"]) * dx + (other["y"] - start["y"]) * dy) / (dx * dx + dy * dy)
                if 0 < t < 1 and math.dist((other["x"], other["y"]), (start["x"] + t * dx, start["y"] + t * dy)) < 0.65:
                    raise ValueError("Prerequisite crosses node: " + q["title"] + " from " +
                                     start["title"] + " via " + other["title"])
    return minimum, len(definitions)


def render(spec, external_library=LIBRARY):
    output = LIBRARY / "chapters" / (spec["filename"] + ".snbt")
    external, external_quests = library(output, external_library)
    ids, retired = allocate(spec, external)
    doc = build(spec, ids)
    minimum, count = validate(doc, external, external_quests,
                              spec.get("minimum_spacing", 2.5), external_library)
    payload = (dump(doc) + "\n").encode("utf-8")
    text = payload.decode("utf-8")
    parsed = SnbtParser(text).parse()
    independent = nbtlib.parse_nbt(text).unpack()
    if parsed != doc or independent != nbt_normalize(doc):
        raise ValueError("Independent SNBT parse mismatch: local=" + str(parsed == doc) +
                         " nbtlib=" + str(independent == nbt_normalize(doc)) +
                         " local_diff=" + first_diff(doc, parsed) +
                         " nbt_diff=" + first_diff(nbt_normalize(doc), independent))
    registry = dict(schema=1, salt=spec["salt"], group=GROUP, order_index=spec["order"],
                    ids=dict(sorted(ids.items())), output_sha256=hashlib.sha256(payload).hexdigest())
    if retired:
        registry["retired_ids"] = dict(sorted(retired.items()))
    map_bytes = (json.dumps(registry, ensure_ascii=False, indent=2, sort_keys=True) + "\n").encode("utf-8")
    return payload, map_bytes, minimum, count


def run(spec):
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--check", action="store_true")
    parser.add_argument("--verify-repeat", action="store_true")
    parser.add_argument("--external-library", type=Path, default=LIBRARY,
                        help="Quest tree used to validate external prerequisites and chapter groups")
    args = parser.parse_args()
    external_library = args.external_library.resolve()
    output = LIBRARY / "chapters" / (spec["filename"] + ".snbt")
    registry = TOOLS / (spec["filename"] + "_ids.json")
    result = render(spec, external_library)
    if args.verify_repeat:
        if result != render(spec, external_library):
            raise ValueError("Nondeterministic render")
        print("Independent repeat render: identical")
    payload, map_bytes, minimum, count = result
    if args.check:
        for path, expected in ((output, payload), (registry, map_bytes)):
            if not path.exists() or path.read_bytes() != expected:
                raise SystemExit(path.name + " differs from deterministic render")
        print("Read-only check passed")
    else:
        output.write_bytes(payload)
        registry.write_bytes(map_bytes)
    print(f"{spec['filename']}: {len(spec['quests'])} quests, {count} IDs; min spacing {minimum:.1f}; all local edges point right; no node crossings")
