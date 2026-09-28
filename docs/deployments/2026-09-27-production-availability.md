# Production-availability deployment

- **Deployed artifact source:** `369c21c4f47652a92ce93fecff22a67dd6b4bcb5`
- **Source branch:** `codex/site-production-release`, based on `codex/primary-domain-cutover`
- **Deployed:** September 27, 2026 (UTC timestamp `20260928T040234Z`)
- **Artifact inventory:** 64 files; relative-path SHA-256 manifest
  `2303cc322a7bd2b89c94aaa2d7738a721449de46bbd4a094a065ebd799ff157d`.
- **Scope:** Public website content only. No Cloudflare, Webuzo configuration,
  certificate, Google Play, database, or protected-portal data change.

## Content changes

- Replaces the in-review statement with the recorded availability statement and
  links the Google Play listing from the home and features pages.
- Restores the `/cast/` receiver: seven files, byte-identical to the receiver
  deployed on September 2, 2026.
- Points the four moved notices at the `/dev/` workspace pages served by this
  site.
- Publishes the owner-directed privacy correction recorded in
  `docs/privacy-reviews/owner-directed-correction-2026-09-27.md`.

Compared with the previous live artifact, 7 files were added, 18 changed, 39
were identical, and none were removed.

## Promotion and rollback

Both the HTTP and HTTPS `24sevenfmplayer.com` virtual hosts mapped to the same
document root. The artifact was staged beside it, its relative-path file
manifest was compared exactly, and the five PHP entry points passed syntax
checks under the server runtime before promotion. The former live artifact is
retained as:

`.24sevenfmplayer.com.rollback-production-availability-369c21c-20260928T040234Z`

This is the content rollback point for the primary hostname. It does not alter
the separate Cloudflare redirect rules for the legacy domains.

## Verification

After promotion, public HTTPS returned `200` for `/`, `/features/`,
`/product-testing/`, `/privacy/`, `/dev/roadmap/`, `/tester-portal.php`,
`/private-tester-queue.php`, and `/turnstile-test/`, and `303` for
`/alpha-tester-interest.php`, matching the responses recorded before
promotion. `/cast/` changed from `404` to `200`, and each receiver file
served over public HTTPS matched its manifest hash.

No served page retained the in-review statement. The privacy notice showed the
September 27, 2026 date and the Cast SDK disclosure.

The three legacy domains (`24sevenfmplayer.net`, `24sevenfmplayer.app`, and
`player.jamesjennison.net`) each retained their expected `308` redirect to the
primary hostname.

## Not verified

Casting from a device to the restored receiver was not tested in this
deployment.
