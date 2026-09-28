# Native privacy notice review — 04d0af5

This non-public review records the exact native privacy notice designated as
the authority for app behavior and native app privacy claims. It supersedes
`native-privacy-56dc946.md`, which is retained as the earlier dated review.

## Exact source

- **Commit:** `04d0af5666d9f37f00fd8a2ceab05a973ad2d920` on `main`.
- **File:** `PRIVACY.md`.
- **SHA-256 of exact Git file bytes:**
  `20bf3457c7962099fdc8d67326635b8a0fdadea62ccaa8a421a6df279f80516f`.
- **Verification command:**
  `git show 04d0af5666d9f37f00fd8a2ceab05a973ad2d920:PRIVACY.md | sha256sum`.

## Reviewed boundaries

1. The optional one-station Chat monitor is user-started, visibly foreground,
   fetches Chat about once a minute while active, exposes a persistent Stop
   action, and is polling rather than server push.
2. The application says it has no developer-operated data server and no
   advertising, crash reporting, tracking SDKs, or developer-operated
   analytics, and it discloses the Google Cast Android Sender SDK's automatic
   anonymous diagnostics.
3. The notice no longer describes the Player as pre-release software.
4. The notice states the 30-day and 90-day station retention periods attested
   in `operational-attestation-2026-09-27.md`, and states no fixed period for
   investigation records.
5. Station-side requests go to the named email contact or the station's
   Contact/Feedback system.

## Agreement with the website notice

The website notice on the `codex/site-production-release` lineage makes the
same application-behavior, retention, and contact statements. It additionally
carries the closed-test tester-interest form section, and it shows the contact
address as plain text because the site validator forbids email links.

The digest is validated against the exact pinned Git object by
`scripts/validate-website-facts-contract.py`.
