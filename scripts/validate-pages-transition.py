#!/usr/bin/env python3
"""Validate the temporary GitHub Pages migration artifact."""

from __future__ import annotations

import sys
from pathlib import Path


EXPECTED_TARGETS = {
    "index.html": "https://24sevenfmplayer.com/privacy/",
    "project/index.html": "https://24sevenfmplayer.com/",
    "project/features/index.html": "https://24sevenfmplayer.com/features/",
    "project/development/index.html": "https://24sevenfmplayer.com/development/",
    "project/testing/index.html": "https://24sevenfmplayer.com/testing/",
    "project/product-testing/index.html": "https://24sevenfmplayer.com/product-testing/",
    "project/roadmap/index.html": "https://24sevenfmplayer.com/roadmap/",
    "project/resources/index.html": "https://24sevenfmplayer.com/resources/",
}


def validate() -> int:
    repository_root = Path(__file__).resolve().parent.parent
    artifact_root = Path(sys.argv[1]).resolve() if len(sys.argv) > 1 else repository_root / "_pages-transition"
    failures: list[str] = []

    expected_files = set(EXPECTED_TARGETS) | {"robots.txt", ".nojekyll"}
    actual_files: set[str] = set()
    for path in artifact_root.rglob("*"):
        if path.is_symlink():
            failures.append(f"Transition artifact contains a symlink: {path.relative_to(artifact_root)}")
        if path.is_file():
            actual_files.add(path.relative_to(artifact_root).as_posix())
    unexpected_files = sorted(actual_files - expected_files)
    missing_files = sorted(expected_files - actual_files)
    if unexpected_files:
        failures.append(f"Unexpected transition files: {', '.join(unexpected_files)}")
    if missing_files:
        failures.append(f"Missing transition files: {', '.join(missing_files)}")

    for relative_path, target in EXPECTED_TARGETS.items():
        path = artifact_root / relative_path
        if not path.is_file():
            failures.append(f"Missing transition page: {relative_path}")
            continue
        content = path.read_text(encoding="utf-8")
        for required in (
            'name="robots" content="noindex, follow"',
            f'href="{target}"',
            "target.search = window.location.search",
            "target.hash = window.location.hash",
            "window.location.replace(target.href)",
        ):
            if required not in content:
                failures.append(f"{relative_path} is missing transition contract: {required}")

    robots = artifact_root / "robots.txt"
    if not robots.is_file() or "Disallow: /" not in robots.read_text(encoding="utf-8"):
        failures.append("Transition robots.txt must disallow crawling")
    if not (artifact_root / ".nojekyll").is_file():
        failures.append("Transition artifact is missing .nojekyll")

    if failures:
        for failure in failures:
            print(f"ERROR: {failure}", file=sys.stderr)
        return 1

    print(f"Validated {len(EXPECTED_TARGETS)} noindex route-preserving GitHub Pages transition pages.")
    return 0


if __name__ == "__main__":
    raise SystemExit(validate())
