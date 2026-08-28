#!/usr/bin/env python3
"""Validate the non-public Player website authority contract."""

from __future__ import annotations

import json
import re
import subprocess
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


def require_git_commit(value: object, label: str) -> None:
    """Require that an authority pin is resolvable in this checkout.

    A syntactically valid SHA is not authority evidence when its object and
    cited source file are absent.  This intentionally fails closed in release
    validation until the exact source reference has been fetched.
    """

    require_sha(value, label)
    result = subprocess.run(
        ["git", "cat-file", "-e", f"{value}^{{commit}}"],
        cwd=ROOT,
        capture_output=True,
        text=True,
        check=False,
    )
    require(result.returncode == 0, f"{label} is not available as a commit in this checkout")


def require_git_file(commit: object, path: object, label: str) -> None:
    require(isinstance(path, str) and path and not Path(path).is_absolute(), f"{label} must be a repository-relative file path")
    result = subprocess.run(
        ["git", "cat-file", "-e", f"{commit}:{path}"],
        cwd=ROOT,
        capture_output=True,
        text=True,
        check=False,
    )
    require(result.returncode == 0, f"{label} is not present at its pinned commit")


def main() -> int:
    try:
        contract = json.loads(CONTRACT_PATH.read_text(encoding="utf-8"))
        require(contract.get("schema_version") == 1, "schema_version must be 1")
        require(contract.get("repository") == "James-Jennison/24Seven.FM-Player", "repository must name the Player repository")

        app = contract["app_behavior"]
        require(app.get("authority_branch") == "main", "app behavior must be pinned from main")
        require_git_commit(app.get("pinned_commit"), "app_behavior.pinned_commit")

        privacy = contract["privacy_claims"]
        require(privacy.get("canonical_file") == "PRIVACY.md", "privacy canonical_file must be PRIVACY.md")
        require_git_commit(privacy.get("pinned_commit"), "privacy_claims.pinned_commit")
        require_git_file(privacy.get("pinned_commit"), privacy.get("canonical_file"), "privacy canonical_file")
        require(privacy.get("content_review_status") in {"pending", "reviewed"}, "privacy content_review_status must be pending or reviewed")
        reconciliation_record = privacy.get("reconciliation_record")
        require(isinstance(reconciliation_record, str) and reconciliation_record.startswith("docs/") and (ROOT / reconciliation_record).is_file(), "privacy reconciliation_record is required")
        portal_addendum = privacy.get("portal_program_addendum")
        require(isinstance(portal_addendum, dict) and portal_addendum.get("extraction_status") in {"pending_extraction_and_review", "reviewed"}, "portal privacy addendum extraction status is invalid")
        require(portal_addendum.get("content_review_status") in {"pending", "reviewed"}, "portal privacy addendum review status is invalid")
        if privacy.get("content_review_status") == "reviewed":
            digest = privacy.get("content_sha256")
            require(isinstance(digest, str) and re.fullmatch(r"[0-9a-f]{64}", digest) is not None, "reviewed privacy text requires content_sha256")

        release = contract["release_claims"]
        candidate = release["candidate_record"]
        require(candidate.get("canonical_file", "").startswith("docs/releases/"), "release candidate must use a release record")
        require_git_commit(candidate.get("pinned_commit"), "release_claims.candidate_record.pinned_commit")
        require_git_file(candidate.get("pinned_commit"), candidate.get("canonical_file"), "release candidate canonical_file")
        require(isinstance(candidate.get("version_code"), int) and candidate["version_code"] > 0, "release candidate version_code must be positive")
        require(isinstance(candidate.get("version_name"), str) and candidate["version_name"], "release candidate version_name is required")
        status_records = release.get("non_authoritative_status_records")
        require(isinstance(status_records, list) and status_records, "release non_authoritative_status_records must document unversioned status claims")
        for record in status_records:
            require(isinstance(record, dict) and all(isinstance(record.get(key), str) and record[key] for key in ("file", "claim", "reason")), "each non-authoritative release status record needs file, claim, and reason")
        require(release.get("availability_claim_status") == "ready_from_console_observation", "availability claims must be governed by Console observations")
        require(release.get("artifact_provenance_status") in {"incomplete", "complete"}, "artifact provenance status is invalid")
        required_provenance_fields = release.get("required_artifact_provenance_fields")
        require(isinstance(required_provenance_fields, list) and {"artifact_sha256", "artifact_source_commit", "evidence_reference"}.issubset(required_provenance_fields), "artifact provenance field requirements are incomplete")
        availability_records = release.get("availability_records")
        require(isinstance(availability_records, list) and availability_records, "availability_records are required")
        for record in availability_records:
            require(isinstance(record, dict), "each availability record must be an object")
            require(isinstance(record.get("version_name"), str) and record["version_name"], "availability record version_name is required")
            require(isinstance(record.get("version_code"), int) and record["version_code"] > 0, "availability record version_code must be positive")
            require(isinstance(record.get("release_record"), str) and record["release_record"].startswith("docs/releases/"), "availability record release_record is invalid")
            require_git_commit(record.get("release_record_commit"), "availability record release_record_commit")
            require_git_file(record.get("release_record_commit"), record.get("release_record"), "availability record release_record")
            manifest_path = record.get("manifest")
            require(isinstance(manifest_path, str) and manifest_path.startswith("docs/release-manifests/") and not Path(manifest_path).is_absolute(), "availability record manifest path is invalid")
            manifest = json.loads((ROOT / manifest_path).read_text(encoding="utf-8"))
            require(manifest.get("schema_version") == 1 and manifest.get("manifest_status") in {"availability_observed", "complete"}, "release manifest status is invalid")
            manifest_candidate = manifest.get("candidate")
            require(isinstance(manifest_candidate, dict) and manifest_candidate.get("version_name") == record["version_name"] and manifest_candidate.get("version_code") == record["version_code"], "release manifest candidate must match its availability record")
            require(manifest_candidate.get("release_record") == record["release_record"] and manifest_candidate.get("release_record_commit") == record["release_record_commit"], "release manifest release record must match its availability record")
            observation = manifest.get("console_observation")
            require(isinstance(observation, dict) and observation.get("status") in {"not_submitted", "in_review", "available_to_testers", "rolled_back", "removed"}, "release manifest Console status is invalid")
            require(all(isinstance(observation.get(field), str) and observation[field] for field in ("track", "recorded_at")), "release manifest Console observation is incomplete")
            require(observation.get("source") == "read_only_play_console", "release manifest Console source must be read_only_play_console")
            provenance = manifest.get("artifact_provenance")
            require(isinstance(provenance, dict) and isinstance(provenance.get("verified"), bool), "release manifest artifact provenance is invalid")
            if provenance["verified"]:
                require(isinstance(provenance.get("artifact_sha256"), str) and re.fullmatch(r"[0-9a-f]{64}", provenance["artifact_sha256"]) is not None, "verified artifact provenance requires artifact_sha256")
                require_sha(provenance.get("artifact_source_commit"), "verified artifact provenance source commit")
                require(isinstance(provenance.get("evidence_reference"), str) and provenance["evidence_reference"], "verified artifact provenance requires evidence_reference")

        portal = contract["portal_surface"]
        require(portal.get("authority_branch") == "codex/onboarding-portal-production", "portal authority branch is invalid")
        require_git_commit(portal.get("pinned_commit"), "portal_surface.pinned_commit")
        require_git_file(portal.get("pinned_commit"), "PRIVACY.md", "portal legacy privacy notice")
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
