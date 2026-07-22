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
| Source branch | Local `codex/player-site-migration`; no push authorized |
| Approved hostname | `player.jamesjennison.net` |
| Canonical hostname | `player.jamesjennison.net`; no `www` variant |
| Framework | Jekyll through the GitHub Pages build environment |
| Build runtime | Digest-pinned `ghcr.io/actions/jekyll-build-pages` container |
| Build method | `./scripts/build-project-site.sh` |
| Validation method | `./scripts/validate-project-site.sh` |
| Production output | Static `_site/` artifact; staged artifact digest `b6f62600b4ab04a5f124a5f2547e4795fcf8be72225fc4af597809025686a9ca` |
| Server runtime | None |
| Database | None |
| Persistent process or port | None |
| Webuzo | Version 4.7.4, revision 3723; Apache 2.4.68 serves the public virtual hosts directly |
| Webuzo user | `jamesjen` |
| Webuzo document root | `/home/jamesjen/player.jamesjennison.net` |
| Origin SSL owner and method | Webuzo; current auto-generated self-signed placeholder is staging-only and not production-trusted |
| Cloudflare record and proxy state | Must be read from Cloudflare and approved before DNS action |
| Cache behavior | Static asset caching proposed below; no rule configured |
| Logging | Existing Webuzo domain access/error logs only; no browser analytics |
| Health check | Static route, asset, TLS, metadata, header, and cross-site navigation checks |
| Backup | Restic pre-change snapshot `40412861`, empty-domain snapshot `21c5494b`, and staged snapshot `0f7293db` |
| Rollback | Withdraw any later Player DNS, restore the empty-domain snapshot or validated artifact, and leave Pages available |

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

Webuzo generated a self-signed placeholder certificate for the new virtual
host even though public certificate issuance was disabled. It is sufficient
only to confirm that the HTTPS virtual host responds; it is not trusted and
must be replaced or covered through an approved Webuzo-managed certificate
workflow before public DNS or Full (Strict) Cloudflare operation. Security and
cache headers also remain pending their separate approval gate.

Restic snapshot `0f7293db` contains all 25 Player files and the corresponding
Webuzo, Apache, database-dump, and certificate state. A streamed restore check
of `index.html` matched the live staged file, and a full Restic repository data
check passed.

## Proposed domain-specific headers

These are recommendations, not active configuration. Before use, identify the
active Webuzo web server, translate them into Webuzo's supported per-domain
configuration, show the exact resulting syntax, validate it, obtain approval,
and test every affected route.

```text
Content-Security-Policy: default-src 'self'; base-uri 'self'; connect-src 'none'; font-src 'self'; form-action 'none'; frame-ancestors 'none'; img-src 'self' data:; object-src 'none'; script-src 'self'; style-src 'self' 'unsafe-inline'; upgrade-insecure-requests
Permissions-Policy: camera=(), geolocation=(), microphone=(), payment=(), usb=()
Referrer-Policy: strict-origin-when-cross-origin
X-Content-Type-Options: nosniff
X-Frame-Options: DENY
Cross-Origin-Opener-Policy: same-origin
```

HSTS should be considered only after origin and edge HTTPS are stable and the
effect on every covered hostname is understood. Do not add it casually.

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

The exact record type, target, proxy state, origin certificate method, renewal
owner, HTTPS enforcement, and cache rules remain approval-gated. Do not expose
an origin IP in documentation or browser assets.

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
2. restore the prior document-root release or verified archive;
3. restore only the approved Player DNS/proxy state when DNS was part of the
   failed change;
4. leave GitHub Pages unchanged or restore its prior artifact if its transition
   was separately activated;
5. re-run master, Player, status, mail, and unrelated-host health checks;
6. document the cause and do not retry production without renewed approval.

## Remaining approval gates

- DNS and Cloudflare record
- Origin SSL issuance or coverage
- Any redirects or domain-specific server configuration
- Security and cache headers
- Production deployment
- Play Console privacy URL update
- `PLAYER_PAGES_TRANSITION_APPROVED` repository variable
- GitHub push or pull request
- GitHub Pages transition and later retirement
