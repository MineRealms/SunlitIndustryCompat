"""Validate the addon chapters and export their item IDs for a minimal server."""

import argparse
import json
import math
import sys
from collections import Counter
from pathlib import Path


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--pack", type=Path, required=True)
    parser.add_argument("--targets", type=Path)
    parser.add_argument("--snapshot", type=Path)
    args = parser.parse_args()
    tools = args.pack / "config/ftbquests/tools"
    sys.path.insert(0, str(tools))
    from industrial_quest_support import SnbtParser, valid_id, walk

    chapters = args.pack / "config/ftbquests/quests/chapters"
    names = ("gtnn_industry", "gtca_industry", "gtca_space")
    docs = {path.stem: SnbtParser(path.read_text(encoding="utf-8-sig")).parse()
            for path in chapters.glob("*.snbt")}
    quests = {q["id"]: q for doc in docs.values() for q in doc.get("quests", [])}
    definitions = Counter(obj["id"] for doc in docs.values() for obj in walk(doc)
                          if valid_id(obj.get("id")))
    targets, rows = set(), []
    visiting, visited = set(), set()

    def visit(key):
        if key in visited:
            return
        assert key not in visiting, "Dependency cycle: " + key
        assert key in quests, "Unknown prerequisite: " + key
        visiting.add(key)
        for dep in quests[key].get("dependencies", []):
            visit(dep)
        visiting.remove(key)
        visited.add(key)

    for name in names:
        doc = docs[name]
        own = doc["quests"]
        for obj in walk(doc):
            if "id" in obj and valid_id(obj["id"]):
                assert definitions[obj["id"]] == 1, "Duplicate ID: " + obj["id"]
        for q in own:
            visit(q["id"])
            if isinstance(q.get("icon"), str):
                targets.add(q["icon"])
            for task in q.get("tasks", []):
                if task["type"] == "item":
                    assert task.get("consume_items") is False, "Consuming task: " + task["id"]
                    assert isinstance(task["item"], str)
                    targets.add(task["item"])
        if isinstance(doc.get("icon"), str):
            targets.add(doc["icon"])
        gap = min(math.dist((a["x"], a["y"]), (b["x"], b["y"]))
                  for i, a in enumerate(own) for b in own[i + 1:])
        assert gap >= 2.5, name + ": insufficient spacing"
        rows.append(dict(chapter=name, quests=len(own), minSpacing=gap))

    if args.targets:
        args.targets.write_text(json.dumps(dict(items=sorted(targets)), indent=2) + "\n", encoding="utf-8")
    if args.snapshot:
        snapshot = json.loads(args.snapshot.read_text(encoding="utf-8"))["_validation"]
        assert snapshot["invalidQuestItems"] == [], snapshot["invalidQuestItems"]
        assert snapshot["requestedQuestItems"] == len(targets), "Stale target manifest"
    print(json.dumps(dict(result="PASS", chapters=rows, uniqueItemIds=len(targets),
                          reachablePrerequisites=len(visited), runtimeChecked=bool(args.snapshot)),
                     ensure_ascii=False, indent=2))


if __name__ == "__main__":
    main()
