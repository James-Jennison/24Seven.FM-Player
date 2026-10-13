# Native privacy notice review — 24bf777

This non-public review records the exact privacy notice designated as the
authority for the public privacy page after 24seven.FM made the Player its
official Android app on October 13, 2026. It does not alter station policy.

## Exact source

- **Commit:** `24bf777da4d1e3ac200e3abfab7d82c5ae02d8e0` on `codex/site-c2-redesign` (the squash merge of pull request #127; the
  notice bytes are identical to the branch commit `f207a53`).
- **File:** `PRIVACY.md`.
- **SHA-256 of exact Git file bytes:**
  `ffa0746094fe5cf89b31a144123a73b7c2a83ba7d8bd1cfb5c7a3d25b5b16c67`.
- **Verification command:**
  `git show 24bf777da4d1e3ac200e3abfab7d82c5ae02d8e0:PRIVACY.md | sha256sum`.
- **App text it carries:** `PRIVACY.md` from the app repository's pull
  request James-Jennison/24Seven.FM-Player#126, commit
  `5713bca` ("Last updated: October 13, 2026"), plus the site-only
  "Closed-test tester-interest form" section pinned by the tester-program
  addendum record `tester-program-addendum-24bf777.md`.

## Reviewed boundaries

1. The only change against the October 9, 2026 publication (review
   `native-privacy-2a16681.md`) is the relationship statement. The opening
   paragraph now reads: "24Seven.FM Player is the official native Android app
   of the 24seven.FM internet radio network. It is developed and published by
   James Jennison, an early network member, with the network's approval. It is
   non-commercial: no ads and no paid features." The tester-program section's
   description of the app changes the same way. The "Last updated" date moves
   to October 13, 2026.
2. Authority for the statement: the owner's instruction in the working session
   on October 13, 2026 ("We're official"), following the network
   administrator's offer to make the Player the official network app. The
   owner chose this wording from three options on the same day.
3. No data-handling statement changes. What the app collects, keeps in memory,
   never writes to storage, and sends to the chosen station is unchanged, as
   are the station-side statements, the retention periods, PayPal as the only
   other provider, and the morg@24seven.fm request path.
4. The site's public-contact rules are unchanged: no mailto links and no
   Player mailbox on the page.
5. The owner attestation of September 27, 2026 continues to cover the retention
   claims, which this notice repeats verbatim.

The digest is validated against the exact pinned Git object by
`scripts/validate-website-facts-contract.py`.
