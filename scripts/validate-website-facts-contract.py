#!/usr/bin/env python3
"""Validate the non-public Player website authority contract."""

from __future__ import annotations

import json
import re
import sys
from pathlib import Path


ROOT = Path(__file__).resolve().parent.parent
CONTRACT_PATH = ROOT / "docs" / "WEBSITE_FACTS_CONTRACT.json"
SHA = re.compile(r"^[0-9a-f]{40}$")


def require(condition: bool, message: str) -> None:
    if not condition:
        raise ValueError(message)


def require_sha(value: object, label: str) -> None:
    require(isinstance(value, str) and SHA.fullmatch(value) is not None, f"{label} must be a 40-character lowercase commit SHA")


def main() -> int:
    try:
        contract = json.loads(CONTRACT_PATH.read_text(encoding="utf-8"))
        require(contract.get("schema_version") == 1, "schema_version must be 1")
        require(contract.get("repository") == "James-Jennison/24Seven.FM-Player", "repository must name the Player repository")

        app = contract["app_behavior"]
        require(app.get("authority_branch") == "main", "app behavior must be pinned from main")
        require_sha(app.get("pinned_commit"), "app_behavior.pinned_commit")

        privacy = contract["privacy_claims"]
        require(privacy.get("canonical_file") == "PRIVACY.md", "privacy canonical_file must be PRIVACY.md")
        require_sha(privacy.get("pinned_commit"), "privacy_claims.pinned_commit")
        require(privacy.get("content_review_status") in {"pending", "reviewed"}, "privacy content_review_status must be pending or reviewed")
        if privacy.get("content_review_status") == "reviewed":
            digest = privacy.get("content_sha256")
            require(isinstance(digest, str) and re.fullmatch(r"[0-9a-f]{64}", digest) is not None, "reviewed privacy text requires content_sha256")

        release = contract["release_claims"]
        candidate = release["candidate_record"]
        require(candidate.get("canonical_file", "").startswith("docs/releases/"), "release candidate must use a release record")
        require_sha(candidate.get("pinned_commit"), "release_claims.candidate_record.pinned_commit")
        require(isinstance(candidate.get("version_code"), int) and candidate["version_code"] > 0, "release candidate version_code must be positive")
        require(isinstance(candidate.get("version_name"), str) and candidate["version_name"], "release candidate version_name is required")
        require(release.get("public_claim_status") in {"blocked_pending_release_manifest", "ready_for_public_claim"}, "release public_claim_status is invalid")
        required_manifest_fields = release.get("required_manifest_fields")
        require(isinstance(required_manifest_fields, list) and {"artifact_sha256", "artifact_source_commit", "play_track", "availability_verified_at", "evidence_reference"}.issubset(required_manifest_fields), "release manifest field requirements are incomplete")

        portal = contract["portal_surface"]
        require(portal.get("authority_branch") == "codex/onboarding-portal-production", "portal authority branch is invalid")
        require_sha(portal.get("pinned_commit"), "portal_surface.pinned_commit")
        require(portal.get("last_synced_app_facts_commit") is None or SHA.fullmatch(portal["last_synced_app_facts_commit"]) is not None, "portal sync pin must be null or a commit SHA")

        deployment = contract["deployment"]
        require(isinstance(deployment.get("live_source_confirmed"), bool), "deployment.live_source_confirmed must be boolean")
        require(isinstance(deployment.get("manifest_exists"), bool), "deployment.manifest_exists must be boolean")
    except (KeyError, OSError, ValueError, json.JSONDecodeError) as error:
        print(f"Website facts contract failed: {error}", file=sys.stderr)
        return 1

    print(f"Website facts contract passed: {CONTRACT_PATH}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
