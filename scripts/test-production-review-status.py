#!/usr/bin/env python3
"""Exercise fail-closed boundaries for the public Google Play availability record."""

from __future__ import annotations

import importlib.util
import json
import tempfile
from pathlib import Path


SCRIPT = Path(__file__).with_name("validate-website-facts-contract.py")
SPEC = importlib.util.spec_from_file_location("website_facts_contract", SCRIPT)
assert SPEC and SPEC.loader
MODULE = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(MODULE)
ORIGINAL_CONTRACT_PATH = MODULE.CONTRACT_PATH
SOURCE = json.loads(ORIGINAL_CONTRACT_PATH.read_text(encoding="utf-8"))


def require_rejection(label: str, mutate: object) -> None:
    fixture = json.loads(json.dumps(SOURCE))
    mutate(fixture)
    with tempfile.TemporaryDirectory() as directory:
        fixture_path = Path(directory) / f"{label}.json"
        fixture_path.write_text(json.dumps(fixture), encoding="utf-8")
        MODULE.CONTRACT_PATH = fixture_path
        try:
            if MODULE.main() != 1:
                raise AssertionError(f"{label} did not fail closed")
        finally:
            MODULE.CONTRACT_PATH = ORIGINAL_CONTRACT_PATH


def main() -> int:
    require_rejection(
        "paraphrased-public-statement",
        lambda value: value["release_claims"]["production_review_status"].__setitem__(
            "public_statement", "The app is available on Google Play."
        ),
    )
    require_rejection(
        "inherited-version-in-scope-rule",
        lambda value: value["release_claims"]["production_review_status"].__setitem__(
            "scope_rule", "Public version 0.1.0-alpha08."
        ),
    )
    require_rejection(
        "unscoped-version-field",
        lambda value: value["release_claims"]["production_review_status"].__setitem__(
            "version_name", "0.1.0-alpha08"
        ),
    )
    require_rejection(
        "unsupported-country-claim",
        lambda value: value["release_claims"]["production_review_status"].__setitem__(
            "public_statement", "24Seven.FM: Internet Radio App is available worldwide from Google Play."
        ),
    )
    require_rejection(
        "unsupported-rollout-claim",
        lambda value: value["release_claims"]["production_review_status"].__setitem__(
            "public_statement", "24Seven.FM: Internet Radio App is available to install from Google Play and has completed its rollout."
        ),
    )
    print("Public Google Play availability contract: fail-closed negative fixtures passed.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
