# Privacy-reconciliation deployment

- **Deployed artifact source:** `a6be2b9c091011d7881191a720177318412fe8fa`
- **Source branch:** `codex/site-production-release`
- **Deployed:** September 27, 2026 (UTC timestamp `20260928T041950Z`)
- **Artifact inventory:** 64 files; relative-path SHA-256 manifest
  `3751d0d7261289a139d91580dcd2e4a0eb50d287f71cc66fa278e65512792bbf`.
- **Scope:** Public website content only. No Cloudflare, Webuzo configuration,
  certificate, Google Play, database, or protected-portal data change.

## Content changes

Only `/privacy/` changed, apart from cache-busting stamps. It now carries the
application-behavior statements recorded in
`docs/privacy-reviews/unified-notice-proposal-2026-09-27.md`, including the
optional foreground Chat monitor. The statement that the application does not
add background polling is removed. Retention, contact, and tester-program
wording is unchanged.

No files were added or removed.

## Promotion and rollback

Both the HTTP and HTTPS `24sevenfmplayer.com` virtual hosts mapped to the same
document root, and that root still matched the previous deployment's manifest.
The artifact was staged in `site-rollbacks/24sevenfmplayer.com/`, outside the
web root and on the same filesystem, so that neither the staged copy nor the
rollback was publicly served at any point. Its relative-path file manifest was
compared exactly, and the five PHP entry points passed syntax checks under the
server runtime before promotion. The former live artifact is retained as:

`site-rollbacks/24sevenfmplayer.com/24sevenfmplayer.com.rollback-privacy-reconciliation-a6be2b9-20260928T041950Z`

## Verification

After promotion, public HTTPS returned the same status for every route recorded
before the September 27 availability deployment, with `/cast/` at `200`. The
served privacy notice contained the foreground-monitor, app-feedback, and
foreground special-use statements and the Cast SDK disclosure. The three legacy
domains each retained their expected `308` redirect to the primary hostname.

## Not verified

Casting from a device to the receiver was not tested in this deployment.
