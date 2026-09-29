# Privacy-restoration deployment

- **Deployed artifact source:** `2c8fe7a0e03e6e798c66498e96753bfffef1353f`
- **Source branch:** `codex/site-status-privacy-merge`
- **Deployed:** September 29, 2026 (UTC timestamp `20260929T223619Z`)
- **Built on:** `gthost-build01`; `scripts/validate-project-site.sh` exited 0
  there, including the PHP contract tests.
- **Artifact inventory:** 63 files; relative-path SHA-256 manifest
  `68ef6bddceff6b2c3214720e03462f551abb7263a8c1d3b5ac51310a82fb770d`.
- **Scope:** Public website content only. No Cloudflare, Webuzo configuration,
  certificate, Google Play, Cast console, database, or protected-portal data
  change.

## Why

The status-refresh release deployed earlier the same day was built from a
lineage that forked on September 10 and lacked the September 27 privacy
commits. It reverted `/privacy/` to the August 15 notice. This release merges
the two lineages.

## Content changes

- `/privacy/` returns to the September 27 notice, byte-identical in its body
  to the notice served before the status-refresh release.
- `/roadmap/`, `/development/`, `/resources/`, and `/testing/` are removed.
  Nothing linked to them and their buttons led to addresses that return 404.
- One unused style rule is removed from `assets/project.css`.

Every other page and asset is unchanged apart from cache-busting stamps. The
five PHP files, both `.htaccess` files, and the Cast receiver are
byte-identical to the previous release.

## Promotion and rollback

Both virtual hosts mapped to the same document root. The artifact was staged
as a sibling directory, compared exactly by relative-path manifest, and its
PHP files were syntax-checked under the server's PHP 8.4 runtime. The staged
directory and the live directory were exchanged in one atomic rename. The
former live artifact is retained as:

`site-rollbacks/24sevenfmplayer.com/24sevenfmplayer.com.rollback-status-refresh-d4ea6e5-20260929T223619Z`

The rollback directory left beside the document root by the status-refresh
release was moved to the same store. Both former sibling addresses returned
200 through the parent site before the move and 404 after it.

## Verification

Origin and public HTTPS returned 200 for every retained route and 404 for the
four removed routes. Every public file matched the artifact byte for byte.
The served notice is dated September 27, 2026 and contains the Cast
disclosure and the station request contact. The three legacy domains retained
their `308` redirects.

## Not verified

The Chromium and Firefox browser suites were not run against this artifact.
Casting from a device to the receiver was not tested. The protected portals
were checked only for a 200 response on their sign-in pages.
