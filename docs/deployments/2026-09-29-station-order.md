# Station-order deployment

- **Deployed artifact source:** `03257c30dfeb963a7428e2e84ff192dcca2322d8`
- **Source branch:** `codex/site-c2-redesign`
- **Deployed:** September 29, 2026 (UTC timestamp `20260930T003024Z`)
- **Built on:** `gthost-build01`; `scripts/validate-project-site.sh` exited 0
  there, including the PHP contract tests.
- **Artifact inventory:** 58 files; relative-path SHA-256 manifest
  `09815fda2455ddeca36fe769032fffa183c567b0ecdf6216c6b0c921200e1614`.
- **Scope:** Public website content only. No Cloudflare, Webuzo configuration,
  certificate, Google Play, Cast console, database, or protected-portal data
  change.

## Content changes

The stations are listed in the order the Player shows them, alphabetical by
name: 1980s.FM, Adagio.FM, Death.FM, Entranced.FM, StreamingSoundtracks.com.
The order applies to the Home doors, the station rows on narrower screens, and
the Stations page. 1980s.FM is the door open on arrival.

Only Home and the Stations page changed, apart from cache-busting stamps. No
files were added or removed.

## Promotion and rollback

Both virtual hosts mapped to the same document root, which still matched the
previous deployment's manifest. The artifact was staged as a sibling
directory, compared exactly by relative-path manifest, and its PHP files were
syntax-checked under the server's PHP 8.4 runtime. The staged directory and
the live directory were exchanged in one atomic rename. The former live
artifact is retained as:

`site-rollbacks/24sevenfmplayer.com/24sevenfmplayer.com.rollback-c2-redesign-b3a6a72-20260930T003024Z`

Its former sibling address returned 200 through the parent site before the
move and 404 after it.

## Verification

Origin and public HTTPS returned 200 for every page and portal sign-in route
and 404 for `/dev/`, `/dev/tester-workspace/`, and `/turnstile-test/`. All 51
public files matched the artifact byte for byte. Home and the Stations page
served the five names in the new order. `scripts/test-project-site-browser.mjs`
passed against the live site in Chrome.

## Not verified

Safari and physical phones were not tested. No tester or administrator signed
in. The app screenshots on the site predate the app's alphabetical order and
still show StreamingSoundtracks.com first in the app's own station strip.
