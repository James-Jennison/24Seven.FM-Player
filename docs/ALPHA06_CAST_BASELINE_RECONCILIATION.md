# Alpha 06 + Cast baseline reconciliation

## Canonical source

The accepted Alpha 06 source baseline is commit `5cfa91d62b3a0e3b82cff4da82ce17836385ddaa`.
It contains the released Alpha 06 application behavior, including the text anti-spam sign-in flow and landscape player work.

The canonical recovery branch is based directly on that commit and carries the net Cast implementation as one reviewed reconciliation commit, followed only by focused corrective commits. It is the only branch eligible to produce the next candidate:

1. `b7368f7` — Cast sender/receiver integration and artifact-provenance gate.
2. `5069c3b` — Gradle wrapper invocation correction for the gate.
3. `34c9097` — short-wide navigation-rail correction while preserving the cover-display contract.
4. `9aa1876` — R8 optimization for release bundles.

## Superseded history

The remote `main` line that starts with `4c5c2d2` and ends at `1f6ebb9` is not a descendant of the Alpha 06 baseline. It reintroduced the legacy sign-in UI and must not be used for debug installs, release bundles, or future feature work.

The temporary Cast recovery branch remains an audit trail for the verified incremental Cast work. Its diagnostic-only commits are intentionally not replayed into the canonical branch; their net production changes are contained in `b7368f7`.

## Required promotion sequence

Before replacing `main`:

1. Complete the canonical validation suite and build a provenance-verified debug candidate from its exact `HEAD`.
2. Create a named archive ref for the current remote `main` tip.
3. Update `main` from the canonical branch with a lease-protected force update. This is necessary because the stale remote history is unrelated to the accepted Alpha 06 baseline.
4. Build the successor release from the resulting `main` tip only. The bundle must use R8, a new version code, valid Play signing, and a committed release-note file.

## Ongoing guardrails

- Start every debug candidate from the intended accepted commit in a new worktree.
- Commit each successful implementation phase before candidate building.
- Use `scripts/build-debug-candidate.sh`; do not install an APK assembled by an ad-hoc Gradle invocation.
- CI runs the same candidate-builder gate, so a debug APK without exact-HEAD provenance fails before it can be treated as a test build.
- The release gate requires explicit commit, artifact, and installed-package checks before any device test result is attributed to a candidate.
