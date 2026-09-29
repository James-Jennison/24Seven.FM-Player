# Privacy-decisions deployment

- **Deployed artifact source:** `e027f96ed2d22a5784d16dab8de78af0317df594`
- **Source branch:** `codex/site-production-release`
- **Deployed:** September 27, 2026 (UTC timestamp `20260928T043256Z`)
- **Artifact inventory:** 64 files; relative-path SHA-256 manifest
  `bc9fd91b593fa8379bb18bb5ff537f72a643336512254c91efcadc7548cd8eb2`.
- **Scope:** Public website content only. No Cloudflare, Webuzo configuration,
  certificate, Google Play, Cast console, database, or protected-portal data
  change.

## Content changes

Only `/privacy/` changed, apart from cache-busting stamps. Station-side
requests now name the email contact, as plain text, alongside the station's
Contact/Feedback system. The statement that investigation records are kept for
no more than one year is removed. No files were added or removed.

The facts contract passed `--require-public-privacy-ready` before promotion.

## Promotion and rollback

Both virtual hosts mapped to the same document root, which still matched the
previous deployment's manifest. The artifact was staged in
`site-rollbacks/24sevenfmplayer.com/`, outside the web root, compared exactly
by relative-path manifest, and syntax-checked under the server runtime. The
former live artifact is retained as:

`site-rollbacks/24sevenfmplayer.com/24sevenfmplayer.com.rollback-privacy-decisions-e027f96-20260928T043256Z`

## Verification

Public HTTPS returned the same status for every recorded route. The served
notice contained the email contact and no one-year statement. The receiver
loaded through `https://24sevenfmplayer.net/cast/`, the address registered in
the Cast console on this date, and the three legacy domains retained their
`308` redirects.

## Not verified

Casting from a device to the receiver was not tested in this deployment.
