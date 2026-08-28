# Native privacy notice review — 56dc946

This non-public review records the exact native privacy notice currently
designated as the authority for app behavior and native app privacy claims.
It does not publish, alter, or approve public wording.

## Exact source

- **Commit:** `56dc94694d158132befc22f7e97113aefc28af93` on `main`.
- **File:** `PRIVACY.md`.
- **SHA-256 of exact Git file bytes:**
  `8e147d39b46245d7de4e5354a9495a8234c064c8a0b53701364bc9610366471f`.
- **Verification command:**
  `git show 56dc94694d158132befc22f7e97113aefc28af93:PRIVACY.md | sha256sum`.

## Reviewed boundaries

1. The optional one-station Chat monitor is user-started, visibly foreground,
   fetches Chat about once a minute while active, exposes a persistent Stop
   action, and is polling rather than server push.
2. The application says it has no developer-operated data server and no
   advertising, analytics, crash-reporting, or tracking SDKs.
3. The notice describes local retention and email-app/recipient boundaries,
   then explicitly states that station-side retention periods are unknown.
4. Station-side access, correction, deletion, logs, and retention remain under
   the station or network operator; the notice names a network-authorized
   contact path but does not establish a numeric operational retention term.
5. This review establishes only the source and its app-behavior boundaries. It
   does not validate a station retention policy or supersede the separate
   tester-program addendum.

The digest is validated against the exact pinned Git object by
`scripts/validate-website-facts-contract.py`.
