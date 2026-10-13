# Tester-program privacy addendum extraction — 24bf777

This non-public record re-pins the tester-program section after the
relationship statement changed on October 13, 2026. It is an exact-source
review artifact, not a public privacy replacement and not an operational
retention attestation.

## Exact source

- **Commit:** `24bf777da4d1e3ac200e3abfab7d82c5ae02d8e0`.
- **File and range:** `PRIVACY.md`, lines 61–71, headed `Closed-test
  tester-interest form`.
- **SHA-256 of the exact source-line bytes:**
  `162c971df4d94dba30a475c51649a0db25bfd77a99120cf76144a5463ad81b2c`.
- **Verification command:**
  `git show 24bf777da4d1e3ac200e3abfab7d82c5ae02d8e0:PRIVACY.md | sed -n '61,71p' | sha256sum`.

## Reviewed addendum boundaries

The section is the one extracted in `tester-program-addendum-c00c2f.md` with
one change: the app is described as "the official Android app of the
24seven.FM network of internet radio stations" instead of "an independently
developed, unofficial player". Every data-handling statement is unchanged:
what the form collects, the private deduplicated coordinator record, the
coordinator mailbox copy, recruitment/assignment/feedback use, transport
acceptance rather than inbox delivery, the optional self-confirmations, the
per-tester support chat, the Community Pack roster boundary, withdrawal and
deletion requests, and the 90-day deletion-or-anonymization promise.

The operational retention attestation recorded in the facts contract
continues to apply unchanged.

The validator re-extracts these source lines from the exact commit and checks
their digest before it accepts this reviewed state.
