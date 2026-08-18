#!/usr/bin/env python3
"""Validate the canonical Tester Task registry against the manual PT catalog."""

from __future__ import annotations

import json
import re
import sys
from collections import Counter
from pathlib import Path


ROOT = Path(__file__).resolve().parent.parent
REGISTRY = ROOT / "privacy-site/_data/tester_tasks.json"
CATALOG = ROOT / "privacy-site/dev/tester-workspace/index.html"
PT_PATTERN = re.compile(r'<article(?:\s+[^>]*?)?\sid="(pt-\d{2})"', re.IGNORECASE)
EXPECTED_PT_IDS = {f"PT-{number:02d}" for number in range(1, 29)} | {f"PT-{number:02d}" for number in range(33, 39)}
EXPECTED_FUTURE_PT_IDS = {"PT-22", "PT-26", "PT-27", "PT-28"}
EXPECTED_RETIRED_PT_IDS = {"PT-33", "PT-34", "PT-36", "PT-37", "PT-38"}


def fail(message: str) -> None:
    print(f"Tester Task registry validation failed: {message}", file=sys.stderr)
    raise SystemExit(1)


def main() -> None:
    try:
        registry = json.loads(REGISTRY.read_text(encoding="utf-8"))
    except (OSError, json.JSONDecodeError) as error:
        fail(f"cannot load {REGISTRY}: {error}")

    tasks = registry.get("tasks")
    summary = registry.get("summary")
    if not isinstance(tasks, list) or not isinstance(summary, dict):
        fail("registry requires tasks[] and summary{}")

    catalog_ids = {case.upper() for case in PT_PATTERN.findall(CATALOG.read_text(encoding="utf-8"))}
    if catalog_ids != EXPECTED_PT_IDS:
        fail(f"catalog IDs differ from the 34-case canonical set: {sorted(catalog_ids)}")
    if len(catalog_ids) != 34:
        fail(f"expected 34 catalog PT cases, found {len(catalog_ids)}")

    task_ids = [task.get("id") for task in tasks if isinstance(task, dict)]
    if len(tasks) != 23 or len(set(task_ids)) != 23:
        fail("expected exactly 23 uniquely identified Tester Tasks")
    if task_ids != [f"TT-{number:02d}" for number in range(1, 24)]:
        fail("Tester Task IDs must be TT-01 through TT-23 in order")

    all_pt_ids: list[str] = []
    future_task_ids: list[str] = []
    retired_task_ids: list[str] = []
    for task in tasks:
        if not isinstance(task, dict):
            fail("every task must be an object")
        for key in ("id", "title", "purpose", "state", "tags", "stationScope", "accountRequirements", "deviceRequirements", "prerequisites", "mutation", "safetyWarning", "ptIds"):
            if key not in task:
                fail(f"{task.get('id', 'unknown')} is missing {key}")
        if "testerSteps" in task and (not isinstance(task["testerSteps"], list) or not task["testerSteps"] or not all(isinstance(step, str) and step for step in task["testerSteps"])):
            fail(f"{task['id']} testerSteps must be a non-empty list of text steps when provided")
        if "expectedResult" in task and (not isinstance(task["expectedResult"], str) or not task["expectedResult"]):
            fail(f"{task['id']} expectedResult must be non-empty text when provided")
        pt_ids = task["ptIds"]
        if not isinstance(pt_ids, list) or not pt_ids:
            fail(f"{task['id']} must contain at least one PT ID")
        all_pt_ids.extend(pt_ids)
        if task["state"] not in {"current", "future", "retired"}:
            fail(f"{task['id']} has invalid state {task['state']!r}")
        if task["state"] == "future":
            future_task_ids.append(task["id"])
            if not task.get("blockReason"):
                fail(f"{task['id']} is future but has no block reason")
        if task["state"] == "retired":
            retired_task_ids.append(task["id"])
            if not task.get("blockReason"):
                fail(f"{task['id']} is retired but has no block reason")

    occurrences = Counter(all_pt_ids)
    mapped = set(all_pt_ids)
    if mapped != EXPECTED_PT_IDS:
        fail(f"task PT union differs from canonical catalog; missing={sorted(EXPECTED_PT_IDS - mapped)}, unknown={sorted(mapped - EXPECTED_PT_IDS)}")
    duplicates = sorted(case for case, count in occurrences.items() if count != 1)
    if duplicates:
        fail(f"each PT case must occur exactly once; bad mappings={duplicates}")
    if any(case in mapped for case in {"PT-29", "PT-30", "PT-31"}):
        fail("phantom PT-29/PT-30/PT-31 was synthesized")

    current_pt_ids = {
        pt_id for task in tasks if task["state"] == "current" for pt_id in task["ptIds"]
    }
    future_pt_ids = {
        pt_id for task in tasks if task["state"] == "future" for pt_id in task["ptIds"]
    }
    retired_pt_ids = {
        pt_id for task in tasks if task["state"] == "retired" for pt_id in task["ptIds"]
    }
    if future_pt_ids != EXPECTED_FUTURE_PT_IDS:
        fail(f"future PT cases differ: {sorted(future_pt_ids)}")
    if retired_pt_ids != EXPECTED_RETIRED_PT_IDS:
        fail(f"retired PT cases differ: {sorted(retired_pt_ids)}")
    if len(current_pt_ids) != 25 or len(future_pt_ids) != 4 or len(retired_pt_ids) != 5:
        fail("expected 25 current, 4 future, and 5 retired PT cases")
    if future_task_ids != ["TT-17", "TT-21"]:
        fail(f"future task set differs: {future_task_ids}")
    if retired_task_ids != ["TT-22", "TT-23"]:
        fail(f"retired task set differs: {retired_task_ids}")
    if len(tasks) - len(future_task_ids) - len(retired_task_ids) != 19:
        fail("expected exactly 19 currently assignable tasks")

    expected_summary = {
        "ptCaseCount": 34,
        "taskCount": 23,
        "assignableTaskCount": 19,
        "futureTaskCount": 2,
        "retiredTaskCount": 2,
        "currentPtCaseCount": 25,
        "futurePtCaseCount": 4,
        "retiredPtCaseCount": 5,
    }
    if summary != expected_summary:
        fail(f"summary must be exactly {expected_summary}")

    artifact_registry = ROOT / "_site/assets/tester-tasks.json"
    if artifact_registry.exists() and artifact_registry.read_bytes() != REGISTRY.read_bytes():
        fail("built public task metadata differs from the canonical registry")

    print("Tester Task registry: 34 PT cases, 23 tasks, 19 current, 2 future, 2 retired — valid.")


if __name__ == "__main__":
    main()
