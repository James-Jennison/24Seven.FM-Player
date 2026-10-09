# Native privacy notice review — 2a16681

This non-public review records the exact privacy notice designated as the
authority for the public privacy page for Player 1.0.2. It does not alter
station policy or the tester-program addendum.

## Exact source

- **Commit:** `2a16681033b1634d7996e75aafd5493e51c59891` on `codex/site-c2-redesign`.
- **File:** `PRIVACY.md`.
- **SHA-256 of exact Git file bytes:**
  `a69b01016c50ffde686427089e7e9e668cead616c280601dc55a82f0d10d5ce2`.
- **Verification command:**
  `git show 2a16681033b1634d7996e75aafd5493e51c59891:PRIVACY.md | sha256sum`.
- **App text it carries:** `PRIVACY.md` at `main` commit
  `ea69c9dff41f7005c064da367762db108016d773` (tag `v1.0.2`, "Last updated:
  October 3, 2026"), plus the site-only "Closed-test tester-interest form"
  section that the tester-program addendum already pins.

## Reviewed boundaries

1. New in this notice against the September 27, 2026 publication: private
   messages (folders, opened messages, the unread count read at most once every
   two minutes, one-time send after review), album ratings and the member's own
   favourites changes (add, move in the ranked list, remove), read-only album
   reviews, the public community pages (Recently Added, Online Now and the
   members list, events and birthdays), Edit profile (the member's own form,
   password fields always sent empty), the profile card and its Email and
   website links, and the played-history archive. Each is described as user
   initiated, kept in memory only, and never written to app storage, logs, or
   diagnostics.
2. The station-side statements are unchanged in substance: the shared network
   policy, the 30-day and 90-day retention periods, the 90-day encrypted-backup
   period, PayPal as the only other provider, and the morg@24seven.fm request
   path. The notice now says the station has not stated a separate retention
   period for private messages or album ratings and that the request path
   covers them.
3. The site's public-contact rules keep the app text's mailto links and the
   Player mailbox off the page: Player privacy questions go through the
   project's GitHub issue channel, and station-side requests go to
   morg@24seven.fm, written as plain text. This is the only wording that
   differs from the app notice besides the tester-program section.
4. The tester-program section is carried unchanged from the previous
   publication; its own pin and attestation in the facts contract still apply.
5. This review establishes the source and its app-behavior boundaries only. The
   owner attestation of September 27, 2026 continues to cover the retention
   claims, which this notice repeats verbatim.

The digest is validated against the exact pinned Git object by
`scripts/validate-website-facts-contract.py`.
