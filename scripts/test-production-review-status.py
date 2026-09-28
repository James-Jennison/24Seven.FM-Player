#!/usr/bin/env python3
"""Exercise fail-closed boundaries for the recorded production status."""

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
            "public_statement", "The production release is under review."
        ),
    )
    require_rejection(
        "inherited-version-in-scope-rule",
        lambda value: value["release_claims"]["production_review_status"].__setitem__(
            "scope_rule", "Closed-test version 0.1.0-alpha08."
        ),
    )
    require_rejection(
        "unscoped-version-field",
        lambda value: value["release_claims"]["production_review_status"].__setitem__(
            "version_name", "0.1.0-alpha08"
        ),
    )
    require_rejection(
        "approval-claim-in-public-statement",
        lambda value: value["release_claims"]["production_review_status"].__setitem__(
            "public_statement", "The production release is approved and available now."
        ),
    )
    require_rejection(
        "rollout-claim-in-public-statement",
        lambda value: value["release_claims"]["production_review_status"].__setitem__(
            "public_statement", "The production release is rolling out to everyone."
        ),
    )
    require_rejection(
        "version-claim-in-public-statement",
        lambda value: value["release_claims"]["production_review_status"].__setitem__(
            "public_statement", "24Seven.FM Player 1.0.0 is available on Google Play in the United States."
        ),
    )
    require_rejection(
        "wider-geography-in-public-statement",
        lambda value: value["release_claims"]["production_review_status"].__setitem__(
            "public_statement", "24Seven.FM Player is available on Google Play."
        ),
    )
    require_rejection(
        "stale-review-statement-after-publication",
        lambda value: value["release_claims"]["production_review_status"].__setitem__(
            "public_statement",
            "The first production release is under review by Google Play and is not yet available for public installation.",
        ),
    )
    require_rejection(
        "unrecorded-owner-only-publication",
        lambda value: value["release_claims"]["production_review_status"].__setitem__(
            "source", "owner_attested_google_play_console"
        ),
    )
    print("Production status contract: fail-closed negative fixtures passed.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
