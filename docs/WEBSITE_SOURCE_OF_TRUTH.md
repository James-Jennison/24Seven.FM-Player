# Website Source of Truth

`WEBSITE_FACTS_CONTRACT.json` is the machine-readable authority record for
Player website, privacy, release, and protected portal work. It is not a
public-site input and is deliberately separate from generated website data.

## Authority boundaries

| Surface | Authority | Constraint |
| --- | --- | --- |
| Native app behavior | The exact `main` commit in the facts contract | A branch name is a moving pointer and is not a release claim. |
| App privacy claims | `PRIVACY.md` at the exact pinned native commit | The content must have a recorded content review and digest before public publication. |
| Release version and availability | The pinned release record plus a complete matching release manifest | `build.gradle.kts` identifies a build candidate only. It cannot by itself establish tester availability, a released version, or public release status. |
| Protected portal implementation | The exact portal commit in the facts contract | Portal code owns portal behavior only; it must not independently define app/privacy/release facts. |
| Live website artifact | A deployment manifest | Similar markup and a branch name are insufficient provenance. The manifest must name the source commit, artifact digest, verification record, and rollback reference. |

Every exact source pin is also a resolvability requirement. Before a reviewed
artifact can pass its release gate, `validate-website-facts-contract.py` uses
`git cat-file` to verify both the cited commit and the cited file at that
commit. A SHA-shaped string, a moving branch name, or a file only present in
the portal checkout is not source evidence.

## Current state

The contract records production availability from the September 27, 2026
observation of the public Google Play listing. That record supports only the
versionless statement that the Player is available on Google Play in the United
States. The Alpha 08 release document remains the authority for its exact
closed-test version and availability only.

The privacy publication gate is ready. The native notice is pinned with its
review digest, the owner's operational attestation is complete, the legacy
surfaces have a recorded disposition, and the reconciled `PRIVACY.md` on the
site lineage is the approved replacement source.

## Required order for a user-facing change

1. Update the relevant pinned source only after its review is complete.
2. Record the reviewed privacy digest or complete release manifest in the
   contract.
3. Synchronize the portal/website source from the exact recorded commit.
4. Validate the generated artifact and record its deployment manifest.
5. Deploy only through the approved atomic promotion and public-verification
   process.

This order prevents a portal branch, a development `versionName`, or an
unverified live page from becoming an accidental authority.

The legacy portal privacy notice is superseded. Its disposition at each known
public location is recorded in `PRIVACY_LEGACY_SURFACE_INVENTORY.md`.

Each release manifest has two independent gates. A recorded read-only Play
Console observation supports a matching availability statement only. A
cross-checked artifact hash and source commit support a matching code-content
statement only. A statement that combines availability with code contents
requires both gates; neither gate silently substitutes for the other.

## Conflict rule

When a named release record and an unversioned project-status statement
conflict, the pinned release record governs public wording. The unversioned
statement remains historical context, but cannot establish an available build
or override the recorded release state. The contract lists the currently known
non-authoritative status records so that this is an explicit evidence ranking,
not a silent edit of historical documentation.
