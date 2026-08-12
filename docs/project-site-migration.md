# Project-site migration and production record

Owner-approved production activation completed July 22, 2026. This document
does not authorize any later GitHub, master-site, status-monitoring, Play
Console, Cloudflare, DNS, SSL, or Webuzo change.

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
| Source branch | `codex/player-site-migration`; production artifact source checkpoint `2cb59c8`, local-only pending push approval |
| Approved hostname | `player.jamesjennison.net` |
| Canonical hostname | `player.jamesjennison.net`; no `www` variant |
| Framework | Jekyll through the GitHub Pages build environment |
| Build runtime | Digest-pinned `ghcr.io/actions/jekyll-build-pages` container |
| Build method | `./scripts/build-project-site.sh` |
| Validation method | `./scripts/validate-project-site.sh` |
| Production output | Static `_site/` artifact; live production digest `9ba4d8e103f23201d5705ce7bc300d0c3247f585cc2be9674cd068f3d8863396` |
| Server runtime | None |
| Database | None |
| Persistent process or port | None |
| Webuzo | Version 4.7.4, revision 3723; Apache 2.4.68 serves the public virtual hosts directly |
| Webuzo user | `jamesjen` |
| Webuzo document root | `/home/jamesjen/player.jamesjennison.net` |
| Deployment connection | Use the managed `website-vm-admin` SSH alias only. `webuzo-production-admin` is not a Player deployment target. |
| Origin SSL owner and method | Webuzo Automatic SSL; dedicated Let's Encrypt certificate for only `player.jamesjennison.net`, valid through October 21, 2026, with next renewal registered for September 20 |
| Cloudflare record and proxy state | Exactly one proxied A record for `player.jamesjennison.net`, TTL Auto; Full (Strict); no public `www.player`, `mail.player`, AAAA, CNAME, or wildcard record |
| Cache behavior | HTML returns `public, max-age=0, must-revalidate, no-transform`; other cache behavior remains unchanged |
| Logging | Existing Webuzo domain access/error logs only; no browser analytics |
| Health check | Static route, asset, TLS, metadata, header, and cross-site navigation checks |
| Backup | Restic pre-activation `85b7382c`, post-Automatic-SSL `11ad18e9`, and production `1f28541a`; each passed repository and streamed-restore checks |
| Rollback | Remove only the Player Cloudflare record, atomically restore `/home/jamesjen/.player-previous-m16b-20260723T040350Z` or snapshot `1f28541a`, restore certificate state through Webuzo only if required, and leave Pages available |

The source, build container, Docker socket, repository metadata, documentation,
dependencies, and credentials must remain outside the public document root.

## August 11, 2026 privacy-notice deployment

The Player privacy-notice update was built from source commit `59a5a70` and
passed the complete local project-site validator. The public artifact contains
25 files. It was staged and compared file-by-file using relative artifact
paths, then promoted through the active Player virtual-host mapping with the
prior live artifact retained for rollback. Public HTTPS verification confirmed
the updated August 11 notice, the station-side deletion request process, the
canonical James-Jennison repository link, and HTTP 200 responses for
Google-like user agents. This record does not authorize a later content,
Cloudflare, or server change.

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

### Current Player deployment procedure

For Player content deployment, connect only through `website-vm-admin`. Before
uploading, discover the active HTTP and HTTPS Player virtual-host mappings and
confirm that they serve the same static directory. Build the reviewed `_site/`
artifact locally, stage it as a sibling directory, compare hashes with relative
artifact paths, then atomically swap only the verified staging directory into
place. Retain the former live directory as the rollback point and verify the
public HTTPS routes after promotion. Do not reuse a document root discovered
for another site or treat `webuzo-production-admin` as a fallback.

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

## Production-activation record

Owner-approved production activation completed July 22, 2026. Exactly one
proxied A record was created for `player.jamesjennison.net`, pointing to the
same Webuzo origin target as the apex. Cloudflare remained in Full (Strict).
No `www.player`, `mail.player`, AAAA, CNAME, or wildcard record was created, and
the complete mail-related DNS boundary remained byte-for-byte unchanged.

A randomized short-lived HTTP-01 probe passed through Cloudflare and was
removed before Webuzo's supported domain-scoped Automatic SSL operation ran.
Webuzo issued and installed a dedicated Let's Encrypt certificate whose only
subject alternative name is `player.jamesjennison.net`. The certificate is
valid through October 21, 2026; Webuzo registered September 20 as the next
renewal time, retained the existing daily `renew_all` job, and left no
challenge file behind.

Cloudflare's JavaScript Detection initially injected an inline bootstrap into
the reviewed HTML. The strict CSP correctly blocked it. After separate owner
approval, the source artifact added an HTML-only `no-transform` cache
directive. The exact rebuilt artifact was promoted atomically; the prior
release remains at
`/home/jamesjen/.player-previous-m16b-20260723T040350Z`.

The first correction promotion restored the prior release automatically when
its validation harness incorrectly treated a normal CRLF header terminator as
a failed header. The artifact and Apache syntax were valid. The corrected
assertion then promoted the same reviewed artifact successfully. No shared
service reload or generated virtual-host edit was required.

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
Cache-Control on HTML: public, max-age=0, must-revalidate, no-transform
```

Direct-origin and public HTTPS checks confirm all seven exact values on
successful and custom-404 responses. The `no-transform` directive prevents
Cloudflare from injecting scripts while retaining the strict CSP and
zone-level security features. HSTS remains intentionally absent until a
separate hostname-wide review approves it.

Recommended caching after Webuzo and Cloudflare review:

- HTML: the approved zero-age revalidation and `no-transform` policy is active;
- XML, robots, and manifest: a future short cache with revalidation remains
  unconfigured;
- versioned CSS, JavaScript, icons, and screenshots: longer cache only after an
  asset-versioning strategy exists;
- 404 responses: short or no cache;
- no cache rule may interfere with the temporary GitHub Pages transition.

## DNS, Cloudflare, and SSL state

The production design uses exactly one proxied A record for
`player.jamesjennison.net`, TTL Auto, targeting the same Webuzo origin as the
apex. Cloudflare uses Full (Strict), and Webuzo owns the dedicated
exact-hostname Let's Encrypt origin certificate and registered renewal state.
Do not expose the origin target in documentation or browser assets.

The certificate was issued only after a short-lived randomized HTTP-01 path
passed through Cloudflare. Mail-related records were compared before and after
activation and did not change. No broader cache rule, redirect rule, zone-wide
HSTS, minimum-TLS change, or additional Player hostname is part of the active
configuration.

The live read-only DNS and edge audit plus the recommended certificate sequence
are recorded in
[player-cloudflare-tls-audit.md](player-cloudflare-tls-audit.md). Cloudflare is
now in Full (Strict) mode. Always Use HTTPS and Automatic HTTPS Rewrites are
enabled, Universal SSL has active apex and wildcard coverage, and the complete
zone/account Ruleset inventories contain no custom phase that
would redirect, rewrite, change the origin, change cache behavior, or alter
response headers for Player. Trusted direct-origin coverage is now verified;
three repeated edge passes and independent origin checks confirmed the strict
transition without a 526 or service-baseline change.

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
historical `codeframe78.github.io` URL is retired and returns GitHub's 404; the
owner confirmed that it is no longer used and does not require a redirect. The
transition variable is absent, so the guarded workflow has not replaced the
current organizational Pages site. The owner will update Play Console to
`https://player.jamesjennison.net/privacy/` when the app is ready for testing
submission.

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

All listed Player production checks passed. Chromium covered 320, 390, 768,
1440, and 1920 pixel viewports; Firefox covered its practical 500-pixel
headless minimum plus 768, 1024, and 1440 pixel viewports. Keyboard, reduced
motion, forced colors, no-JavaScript, custom-404, privacy, external navigation,
and browser-console checks passed.

Production Lighthouse results were 98/100/100/92 on mobile and
100/100/96/92 on desktop for Performance, Accessibility, Best Practices, and
SEO respectively. The owner approved the SEO measurement exception: the only
failing SEO audit is Lighthouse's internal `robots.txt` fetch being denied by
the deliberately strict `connect-src 'none'` CSP. The public `robots.txt`,
sitemap, canonical metadata, and indexing directives are valid and reachable.
The CSP was not weakened to optimize a synthetic score.

The master registry and public status service do not yet include Player.
Adding the master-site project link or a status-monitoring component requires a
separate reviewed change and approval.

## Rollback

Before activation, record the prior release path or backup identifier, current
document-root inventory, ownership, permissions, DNS state, Cloudflare state,
certificate state, and expected restoration time.

If the new site fails:

1. stop the release promotion without changing unrelated Webuzo services;
2. atomically restore
   `/home/jamesjen/.player-previous-m16b-20260723T040350Z` or recover the
   required state from snapshot `1f28541a`, while preserving the failed release
   for diagnosis;
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

- Add Player to the live master-site project registry and navigation
- Add Player to public status monitoring, if desired
- Any additional hostname, redirect, general cache rule, HSTS, or zone-wide TLS change
- Owner-timed Play Console privacy URL update at app testing submission
- `PLAYER_PAGES_TRANSITION_APPROVED` repository variable
- GitHub push or pull request
- GitHub Pages transition and later retirement
