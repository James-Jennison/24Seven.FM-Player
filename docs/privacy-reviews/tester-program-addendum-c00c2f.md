# Tester-program privacy addendum extraction — c00c2f

This non-public record isolates the tester-program section from the legacy
combined portal notice. It is an exact-source review artifact, not a public
privacy replacement and not an operational retention attestation.

## Exact source

- **Commit:** `c00c2f4982232e7c197aa0e35d174d7c3478436e`.
- **File and range:** `PRIVACY.md`, lines 52–62, headed `Closed-test
  tester-interest form`.
- **SHA-256 of the exact source-line bytes:**
  `24cfa41df0504ff070efa94e46cacf95b21781668ea4e25186b1ddb47ee3d14c`.
- **Verification command:**
  `git show c00c2f4982232e7c197aa0e35d174d7c3478436e:PRIVACY.md | sed -n '52,62p' | sha256sum`.

## Reviewed addendum boundaries

The extracted source says the program collects application identity and device
coverage details, creates a private deduplicated coordinator record, sends a
copy to the coordinator mailbox, supports recruitment/assignment/feedback,
and records transport acceptance rather than inbox delivery. It also covers
optional opt-in and smoke-test self-confirmations, per-tester support chat,
the Community Pack roster boundary, withdrawal/deletion requests, and a
90-day deletion-or-anonymization promise for tester intake, assignments,
feedback, and invitation correspondence.

The portal's code-backed 90-day chat purge applies only to portal chat records
during portal handling. It does not prove the broader tester-program promise,
especially for intake, assignments, feedback, invitation correspondence, or
mailbox copies. The entire 90-day tester-program retention statement therefore
remains an operational claim that **requires owner attestation**. Its current
status is pending; this content review does not attest it. It is deliberately
not merged into the native Player privacy notice by this review.

The validator re-extracts these source lines from the exact commit and checks
their digest before it accepts this reviewed state.
