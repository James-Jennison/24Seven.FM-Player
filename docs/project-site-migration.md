# Project-site migration and deployment preparation

Production deployment is not authorized by this document.

## Approved outcome

The existing 24Seven.FM Player portal is retained and refined for
`https://player.jamesjennison.net`. The site keeps its Jekyll generator,
project-specific visual identity, real app imagery, content structure, themes,
and progressive interactions. The migration changes only the parts that
materially benefit a dedicated domain:

- project overview at `/` and privacy notice at `/privacy/`;
- clean feature, development, testing, roadmap, and resource routes;
- James-Jennison repository identity;
- master-site and public-status links;
- canonical, Open Graph, social, structured, sitemap, manifest, robots, and 404
  foundations;
- dated public evidence and a narrower public documentation index;
- explicit build, artifact, transition, backup, rollback, and health contracts.

## Site record

| Field | Value |
| --- | --- |
| Public project | 24Seven.FM Player |
| Repository | `James-Jennison/24Seven.FM-Player` |
| Source branch | `codex/player-site-migration`; remote through `b94eacd`, with the current hardening milestone local-only pending review |
| Approved hostname | `player.jamesjennison.net` |
| Canonical hostname | `player.jamesjennison.net`; no `www` variant |
| Framework | Jekyll through the GitHub Pages build environment |
| Build runtime | Digest-pinned `ghcr.io/actions/jekyll-build-pages` container |
| Build method | `./scripts/build-project-site.sh` |
| Validation method | `./scripts/validate-project-site.sh` |
| Production output | Static `_site/` artifact; current origin artifact digest `3729bc53082966c0473e8fb04da820f731c0f1c3140a5f7d821a8717b7d79bb5` |
| Server runtime | None |
| Database | None |
| Persistent process or port | None |
| Webuzo | Version 4.7.4, revision 3723; Apache 2.4.68 serves the public virtual hosts directly |
| Webuzo user | `jamesjen` |
| Webuzo document root | `/home/jamesjen/player.jamesjennison.net` |
| Origin SSL owner and method | Webuzo; existing publicly trusted Let's Encrypt apex/wildcard certificate assigned to Player through Webuzo's supported install-certificate workflow; dedicated renewal proof remains pending |
| Cloudflare record and proxy state | Player record absent; zone is proxied and uses Full rather than Full (Strict); Universal SSL is active for the apex and wildcard; no applicable redirect, transform, origin, cache, configuration, compression, or response-header Ruleset is deployed |
| Cache behavior | Static asset caching proposed below; no rule configured |
| Logging | Existing Webuzo domain access/error logs only; no browser analytics |
| Health check | Static route, asset, TLS, metadata, header, and cross-site navigation checks |
| Backup | Restic pre-change snapshots `40412861`, `21c5494b`, `0f7293db`, and `bcefb231`; hardened-state snapshot `77f5810b` |
| Rollback | Withdraw any later Player DNS, atomically restore the retained prior origin release or snapshot `bcefb231`, reinstall the backed-up prior certificate through Webuzo if needed, and leave Pages available |

The source, build container, Docker socket, repository metadata, documentation,
dependencies, and credentials must remain outside the public document root.

## Reproducible build

From the repository root:

```bash
./scripts/validate-project-site.sh
```

The preparation step generates `privacy-site/privacy/index.md` from the
canonical `PRIVACY.md` and copies approved Play Store imagery into ignored
build-input paths. The script omits only the privacy document's duplicate first
line heading because the visual privacy layout supplies the page's single
`h1`; the notice body remains canonical.

The build container is pinned by digest. It produces `_site/`, then the
validator checks:

- all nine HTML destinations;
- internal routes, fragments, images, scripts, canonical URLs, descriptions,
  robots directives, Open Graph and social metadata;
- sitemap and manifest contracts;
- absence of the legacy Pages identity and unapproved public email address;
- absence of Git metadata, environment files, caches, symlinks, and insecure
  URLs;
- JavaScript syntax;
- the temporary Pages transition mapping.

The exact deployment record must add the source commit, artifact SHA-256,
complete inventory, build-container digest, and validator output.

## Webuzo deployment model

Use Webuzo as the authority for the subdomain, user, document root, web server,
certificate, ownership, permissions, logs, and backups.

1. Read and record the Webuzo version, active web-server chain, intended user,
   exact document root, existing content, aliases, certificate method, and
   backup status.
2. Propose the exact `player.jamesjennison.net` subdomain change and obtain
   approval before creating it.
3. Create an isolated document root owned by the approved Webuzo user. It must
   not be nested in or overlap the master site, status site, another project,
   mail paths, or another user's files.
4. Build and validate outside all public document roots.
5. Back up the existing destination, even when it appears empty, and verify the
   backup is readable.
6. Upload only the `_site/` files to a candidate release location.
7. Inspect ownership and permissions without recursive broad changes. Never use
   `chmod 777`.
8. Validate the candidate through a Webuzo-supported staging or origin method.
9. Activate the release with the smallest supported replacement window and
   retain the prior release.
10. Run origin and Cloudflare health checks before declaring the site live.

No custom VirtualHost, reverse proxy, runtime service, scheduled job, service
restart, or global web-server change is required for the static site.

## Origin-staging record

Owner-approved origin-only staging completed July 22, 2026. Webuzo created
`player.jamesjennison.net` as a subdomain owned by `jamesjen` with the isolated
document root `/home/jamesjen/player.jamesjennison.net`. Webuzo generated
internal `www.player` and `mail.player` virtual-host aliases as part of its
standard template; neither alias has public DNS, and no alias is approved as a
public hostname.

The staged document root contains only the 25 validated static artifact files.
The root is owned by `jamesjen:nobody` with mode `0750`; artifact directories
use `0755`, files use `0644`, and artifact entries are owned by
`jamesjen:jamesjen`. There are no symlinks, Git metadata, environment files,
private keys, logs, source maps, dependencies, or repository documentation in
the document root.

All eight public content routes and their assets return successfully through
an origin-address-bound hostname probe. Unknown routes retain HTTP status 404
and render the project-specific `404.html` through the artifact-local
`.htaccess`; no generated Webuzo virtual-host file was edited. The staged
artifact and local artifact have the same deterministic inventory digest.

Webuzo initially generated a self-signed placeholder certificate for the new
virtual host even though public certificate issuance was disabled. During the
subsequently approved origin-hardening milestone, the existing publicly trusted
Let's Encrypt wildcard was assigned to Player through Webuzo's supported
install-certificate API. Direct-origin verification now passes the certificate
chain and exact Player hostname without bypassing trust checks. This is
temporary coverage: the wildcard expires August 27, 2026, and a dedicated
Webuzo-managed certificate plus verified renewal path remain required before
that date.

Restic snapshot `0f7293db` contains all 25 Player files and the corresponding
Webuzo, Apache, database-dump, and certificate state. A streamed restore check
of `index.html` matched the live staged file, and a full Restic repository data
check passed.

Before hardening, snapshot `bcefb231` captured the exact Player document root,
Apache configuration, Webuzo state, database dump, and certificate files. A
full Restic data check and streamed `index.html` and `.htaccess` restores
passed. The hardened artifact was activated by a same-filesystem directory
swap, with the prior release retained at
`/home/jamesjen/.player-previous-20260722T231413Z`. Post-change snapshot
`77f5810b` preserves the validated working state.

## Active domain-specific headers

The approved headers are implemented in the deployment artifact's local
`.htaccess`. Apache's loaded `mod_headers` module applies them to successful and
error responses; no generated Webuzo virtual-host file or global web-server
configuration was edited.

```text
Content-Security-Policy: default-src 'self'; base-uri 'self'; connect-src 'none'; font-src 'self'; form-action 'none'; frame-ancestors 'none'; img-src 'self' data:; object-src 'none'; script-src 'self'; style-src 'self' 'unsafe-inline'; upgrade-insecure-requests
Permissions-Policy: camera=(), geolocation=(), microphone=(), payment=(), usb=()
Referrer-Policy: strict-origin-when-cross-origin
X-Content-Type-Options: nosniff
X-Frame-Options: DENY
Cross-Origin-Opener-Policy: same-origin
```

Direct-origin HTTP and HTTPS checks confirm all six exact values on successful
and custom-404 responses. HSTS remains intentionally absent until origin and
edge HTTPS are stable and the effect on every covered hostname is understood.

Recommended caching after Webuzo and Cloudflare review:

- HTML, XML, robots, and manifest: short cache with revalidation;
- versioned CSS, JavaScript, icons, and screenshots: longer cache only after an
  asset-versioning strategy exists;
- 404 responses: short or no cache;
- no cache rule may interfere with the temporary GitHub Pages transition.

## DNS, Cloudflare, and SSL proposal

Before any change, verify current records and whether mail or another service
depends on them. The expected design is one DNS record for
`player.jamesjennison.net`, proxied through Cloudflare only after origin health
is confirmed, with valid origin coverage and end-to-end Full (Strict) TLS.

The exact record type, target, proxy state, dedicated-certificate renewal path,
HTTPS enforcement, and cache rules remain approval-gated. Do not expose an
origin IP in documentation or browser assets.

The live read-only DNS and edge audit plus the recommended certificate sequence
are recorded in
[player-cloudflare-tls-audit.md](player-cloudflare-tls-audit.md). Cloudflare is
currently in Full mode, not Full (Strict). Always Use HTTPS and Automatic HTTPS
Rewrites are enabled, Universal SSL has active apex and wildcard coverage, and
the complete zone/account Ruleset inventories contain no custom phase that
would redirect, rewrite, change the origin, change cache behavior, or alter
response headers for Player. Trusted direct-origin coverage is now verified;
the zone-wide Full (Strict) transition remains required and approval-gated
before public cutover.

## GitHub Pages transition

The Project Site workflow always validates the dedicated-domain artifact and
prepares an eight-route `noindex` transition artifact. Deployment of that
artifact is additionally gated by the repository variable
`PLAYER_PAGES_TRANSITION_APPROVED=true`.

The variable must not be created or changed until:

1. the Webuzo site is production-valid;
2. the owner approves the exact GitHub change;
3. the Play Console privacy URL and other known consumers are ready to move;
4. the old and new URL matrix has passed;
5. rollback has been rehearsed.

The repository's current GitHub Pages URL is
`https://james-jennison.github.io/24Seven.FM-Player/` and returns 200. The
historical `codeframe78.github.io` URL now returns GitHub's 404. The transition
variable is absent, so the guarded workflow has not replaced the current
organizational Pages site. Before public cutover, verify which URL Play Console
and every other external consumer currently stores; repository documentation
still records the historical URL as the saved privacy destination.

Each static transition page has a visible ordinary link and a five-second HTML
fallback. Its small script preserves the incoming query string and fragment
before replacing the location. The root maps to the new privacy notice; the
seven `/project/` routes map to their clean dedicated-domain counterparts.

Keep the transition for at least 30 days and longer while known inbound links
still use Pages. Disabling Pages is a later approval gate.

## Production validation

Test direct origin behavior where safe and approved, then the Cloudflare path:

- every canonical route and the custom 404;
- HTTP to HTTPS and any same-host compatibility redirects;
- path and query preservation with repeated requests to detect loops;
- origin and edge certificate chains;
- security and cache headers;
- internal navigation, repository links, master-site return, and status link;
- privacy and Play Console destinations;
- narrow mobile, standard mobile, tablet, laptop, and large desktop layouts;
- keyboard order, focus visibility, dialog focus/escape, reduced motion, forced
  colors, high zoom, and no-JavaScript behavior;
- Lighthouse performance, accessibility, best-practices, and SEO targets;
- logs for new errors without recording or publishing sensitive topology.

The master registry must keep Player in `planned` state until these checks pass.
Changing it to `live` is a separate reviewed master-site commit.

## Rollback

Before activation, record the prior release path or backup identifier, current
document-root inventory, ownership, permissions, DNS state, Cloudflare state,
certificate state, and expected restoration time.

If the new site fails:

1. stop the release promotion without changing unrelated Webuzo services;
2. atomically restore the retained prior document-root release or snapshot
   `bcefb231` while preserving the failed release for diagnosis;
3. if certificate rollback is required, extract the prior Player materials into
   a root-only temporary directory and reinstall them through Webuzo's supported
   certificate workflow; never hand-edit the generated virtual host;
4. restore only the approved Player DNS/proxy state when DNS was part of the
   failed change;
5. leave GitHub Pages unchanged or restore its prior artifact if its transition
   was separately activated;
6. re-run master, Player, status, mail, and unrelated-host health checks;
7. document the cause and do not retry production without renewed approval.

## Remaining approval gates

- DNS and Cloudflare record
- Dedicated origin-certificate issuance and renewal proof before the temporary wildcard expires
- Any redirects or domain-specific server configuration
- Cache headers
- Production deployment
- Play Console and other external privacy-URL verification/update
- `PLAYER_PAGES_TRANSITION_APPROVED` repository variable
- GitHub push or pull request
- GitHub Pages transition and later retirement
