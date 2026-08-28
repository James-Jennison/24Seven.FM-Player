#!/usr/bin/env python3
"""Validate the non-public Player website authority contract."""

from __future__ import annotations

import json
import re
import subprocess
import sys
from argparse import ArgumentParser
from datetime import datetime
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


def require_iso8601_offset(value: object, label: str) -> None:
    require(isinstance(value, str), f"{label} must be an ISO 8601 timestamp")
    try:
        parsed = datetime.fromisoformat(value)
    except ValueError as error:
        raise ValueError(f"{label} must be an ISO 8601 timestamp") from error
    require(parsed.tzinfo is not None, f"{label} must include a UTC offset")


def validate_public_privacy_readiness(privacy: dict[str, object], gate: dict[str, object]) -> None:
    """Fail closed when a command is explicitly validating a public privacy release."""

    require(privacy.get("content_review_status") == "reviewed", "public privacy release requires a reviewed native privacy notice")
    portal_addendum = privacy["portal_program_addendum"]
    require(isinstance(portal_addendum, dict) and portal_addendum.get("extraction_status") == "reviewed", "public privacy release requires a reviewed tester-program addendum extraction")
    require(isinstance(portal_addendum, dict) and portal_addendum.get("content_review_status") == "reviewed", "public privacy release requires a reviewed tester-program addendum")
    require(privacy.get("legacy_surface_disposition_status") == "complete", "public privacy release requires the legacy-surface disposition to be complete")
    require(privacy.get("public_replacement_source_status") == "approved", "public privacy release requires an approved replacement source")

    require(gate.get("status") == "ready", "public privacy release requires a ready privacy publication gate")
    attestation = gate["operational_attestation"]
    require(isinstance(attestation, dict) and attestation.get("status") == "attested", "public privacy release requires a recorded owner attestation")
    record = attestation.get("attestation_record") if isinstance(attestation, dict) else None
    require(isinstance(record, dict), "recorded owner attestation requires an attestation_record")
    require_iso8601_offset(record.get("attested_at"), "operational attestation attested_at")
    require(isinstance(record.get("station_policy_evidence"), str) and record["station_policy_evidence"], "recorded owner attestation requires station_policy_evidence")
    require(isinstance(record.get("tester_program_evidence"), str) and record["tester_program_evidence"], "recorded owner attestation requires tester_program_evidence")
    require(record.get("publication_disposition") == "verified_current", "public privacy release requires an owner attestation that verifies the approved replacement wording")


def validate_interim_privacy_correction_readiness(gate: dict[str, object]) -> None:
    """Fail closed for a narrow correction after an owner disproves a live claim.

    This deliberately does not require the full replacement's unrelated source
    reviews. It does require the exact claim, correction wording, approval,
    validation, and rollback records needed for a real production release.
    """

    attestation = gate["operational_attestation"]
    require(isinstance(attestation, dict) and attestation.get("status") == "attested", "interim privacy correction requires a recorded owner attestation")
    record = attestation.get("attestation_record") if isinstance(attestation, dict) else None
    require(isinstance(record, dict), "interim privacy correction requires an attestation_record")
    require(record.get("publication_disposition") == "interim_correction_required", "interim privacy correction requires an owner finding that a served claim is inaccurate")
    attested_claims = record.get("attested_inaccurate_claims")
    require(isinstance(attested_claims, list) and attested_claims, "interim privacy correction requires each inaccurate served claim to be recorded in the owner attestation")
    attested_claim_keys = {
        (claim.get("served_route"), claim.get("served_claim"))
        for claim in attested_claims
        if isinstance(claim, dict)
    }

    correction = gate["interim_correction"]
    require(isinstance(correction, dict) and correction.get("status") == "ready", "interim privacy correction requires a ready correction record")
    claims = correction.get("claims") if isinstance(correction, dict) else None
    require(isinstance(claims, list) and claims, "interim privacy correction requires at least one exact affected claim")
    for claim in claims:
        require(isinstance(claim, dict), "each interim privacy correction claim must be an object")
        require(isinstance(claim.get("served_route"), str) and claim["served_route"].startswith("https://"), "each interim privacy correction claim requires an HTTPS served_route")
        require(isinstance(claim.get("served_claim"), str) and claim["served_claim"], "each interim privacy correction claim requires the exact served_claim")
        require((claim["served_route"], claim["served_claim"]) in attested_claim_keys, "each interim privacy correction claim must match a specific claim in the owner attestation")
        wording_source = claim.get("replacement_wording_source")
        require(isinstance(wording_source, str) and wording_source.startswith("docs/") and (ROOT / wording_source).is_file(), "each interim privacy correction claim requires a checked-in replacement_wording_source")
        require(isinstance(claim.get("production_approval_reference"), str) and claim["production_approval_reference"], "each interim privacy correction claim requires a separate production_approval_reference")
        require(isinstance(claim.get("validation_plan"), str) and claim["validation_plan"], "each interim privacy correction claim requires a validation_plan")
        require(isinstance(claim.get("rollback_plan"), str) and claim["rollback_plan"], "each interim privacy correction claim requires a rollback_plan")


def main(require_public_privacy_ready: bool = False, require_interim_privacy_correction_ready: bool = False) -> int:
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
        legacy_surface_inventory = privacy.get("legacy_surface_inventory")
        require(isinstance(legacy_surface_inventory, str) and legacy_surface_inventory.startswith("docs/") and (ROOT / legacy_surface_inventory).is_file(), "privacy legacy_surface_inventory is required")
        portal_addendum = privacy.get("portal_program_addendum")
        require(isinstance(portal_addendum, dict) and portal_addendum.get("extraction_status") in {"pending_extraction_and_review", "reviewed"}, "portal privacy addendum extraction status is invalid")
        require(portal_addendum.get("content_review_status") in {"pending", "reviewed"}, "portal privacy addendum review status is invalid")
        require(privacy.get("legacy_surface_disposition_status") in {"pending", "complete"}, "privacy legacy_surface_disposition_status is invalid")
        require(privacy.get("public_replacement_source_status") in {"pending", "approved"}, "privacy public_replacement_source_status is invalid")
        if privacy.get("content_review_status") == "reviewed":
            digest = privacy.get("content_sha256")
            require(isinstance(digest, str) and re.fullmatch(r"[0-9a-f]{64}", digest) is not None, "reviewed privacy text requires content_sha256")

        privacy_gate = contract.get("privacy_publication_gate")
        require(isinstance(privacy_gate, dict) and privacy_gate.get("status") in {"blocked_pending_owner_attestation", "ready"}, "privacy publication gate status is invalid")
        attestation = privacy_gate.get("operational_attestation") if isinstance(privacy_gate, dict) else None
        require(isinstance(attestation, dict) and attestation.get("status") in {"deadline_set_pending_attestation", "attested"}, "operational attestation status is invalid")
        require(isinstance(attestation.get("designated_owner"), str) and attestation["designated_owner"], "operational attestation designated_owner is required")
        require_iso8601_offset(attestation.get("deadline"), "operational attestation deadline")
        require(isinstance(attestation.get("deadline_display"), str) and attestation["deadline_display"], "operational attestation deadline_display is required")
        scopes = attestation.get("required_scopes")
        require(isinstance(scopes, list) and len(scopes) >= 2 and all(isinstance(scope, str) and scope for scope in scopes), "operational attestation required_scopes are incomplete")
        require(isinstance(attestation.get("rule"), str) and attestation["rule"], "operational attestation rule is required")
        if attestation.get("status") == "deadline_set_pending_attestation":
            require(attestation.get("attestation_record") is None, "pending owner attestation must not contain an attestation_record")
        if attestation.get("status") == "attested":
            record = attestation.get("attestation_record")
            require(isinstance(record, dict), "attested owner status requires an attestation_record")
            require_iso8601_offset(record.get("attested_at"), "operational attestation attested_at")
            require(isinstance(record.get("station_policy_evidence"), str) and record["station_policy_evidence"], "attested owner status requires station_policy_evidence")
            require(isinstance(record.get("tester_program_evidence"), str) and record["tester_program_evidence"], "attested owner status requires tester_program_evidence")
            require(record.get("publication_disposition") in {"verified_current", "interim_correction_required"}, "attested owner status has an invalid publication_disposition")
            attested_claims = record.get("attested_inaccurate_claims")
            require(isinstance(attested_claims, list), "attested owner status requires attested_inaccurate_claims to be a list")
            for claim in attested_claims:
                require(isinstance(claim, dict), "each attested inaccurate claim must be an object")
                require(isinstance(claim.get("served_route"), str) and claim["served_route"].startswith("https://"), "each attested inaccurate claim requires an HTTPS served_route")
                require(isinstance(claim.get("served_claim"), str) and claim["served_claim"], "each attested inaccurate claim requires the exact served_claim")
            if record.get("publication_disposition") == "verified_current":
                require(not attested_claims, "verified-current owner attestation cannot contain inaccurate claims")
            if record.get("publication_disposition") == "interim_correction_required":
                require(bool(attested_claims), "interim-correction owner attestation requires at least one inaccurate claim")
        interim_correction = privacy_gate.get("interim_correction") if isinstance(privacy_gate, dict) else None
        require(isinstance(interim_correction, dict) and interim_correction.get("status") in {"not_required", "pending", "ready"}, "interim privacy correction status is invalid")
        claims = interim_correction.get("claims") if isinstance(interim_correction, dict) else None
        require(isinstance(claims, list), "interim privacy correction claims must be a list")
        require(isinstance(interim_correction.get("rule"), str) and interim_correction["rule"], "interim privacy correction rule is required")
        if attestation.get("status") == "deadline_set_pending_attestation":
            require(interim_correction.get("status") == "not_required" and not claims, "pending owner attestation cannot open an interim correction")
        if require_public_privacy_ready:
            validate_public_privacy_readiness(privacy, privacy_gate)
        if require_interim_privacy_correction_ready:
            validate_interim_privacy_correction_readiness(privacy_gate)

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
    parser = ArgumentParser(description=__doc__)
    parser.add_argument(
        "--require-public-privacy-ready",
        action="store_true",
        help="fail unless all owner-attestation and source-review gates for a public privacy release are complete",
    )
    parser.add_argument(
        "--require-interim-privacy-correction-ready",
        action="store_true",
        help="fail unless a specifically disproven served claim has its own correction wording, approval, validation, and rollback records",
    )
    args = parser.parse_args()
    raise SystemExit(main(
        require_public_privacy_ready=args.require_public_privacy_ready,
        require_interim_privacy_correction_ready=args.require_interim_privacy_correction_ready,
    ))
