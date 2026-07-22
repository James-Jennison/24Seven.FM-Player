# Dedicated-domain project-site validation

Validated July 22, 2026. Production deployment is not authorized by this
record.

## Outcome

The existing Jekyll portal was retained, refined, and staged as a static
artifact for `https://player.jamesjennison.net`. The implementation uses the
approved clean route model, James-Jennison identity, curated public content,
GitHub-only privacy contact, an Apache-local custom 404 mapping, and six
artifact-level security headers. Webuzo origin staging and trusted wildcard
assignment are complete. Public DNS, Cloudflare settings, GitHub settings,
GitHub Pages, and production remain unchanged.

Source was isolated on local branch `codex/player-site-migration` from commit
`c54057151f34e5505f238391d08a5520d8e45c06`. The owner's separate dirty main
checkout was not used for implementation.

## Reproducible validation

Run from the repository root:

```bash
./scripts/validate-project-site.sh
python3 -m http.server 4173 --directory _site
# In a second terminal:
node ./scripts/test-project-site-browser.mjs http://127.0.0.1:4173
```

The static build uses the digest-pinned image
`ghcr.io/actions/jekyll-build-pages@sha256:6791ebfd912185ed59bfb5fb102664fa872496b79f87ff8b9cfba292a7345041`.

The final `_site/` artifact contains 25 files totaling approximately 3.42 MiB,
including nine HTML documents. Its deterministic inventory digest, calculated
by sorting each relative file path and content SHA-256 into a second SHA-256,
is:

```text
3729bc53082966c0473e8fb04da820f731c0f1c3140a5f7d821a8717b7d79bb5
```

The digest is a local validation checkpoint, not a signed release provenance
record. It must be recalculated from the approved deployment commit before
staging or production.

## Automated results

| Validation | Result |
| --- | --- |
| Jekyll production build | Pass |
| Dedicated-domain artifact validator | Pass: 25 files, 9 HTML documents |
| GitHub Pages transition validator | Pass: 8 `noindex` compatibility pages |
| Internal routes, fragments, and assets | Pass |
| Canonical, Open Graph, social, sitemap, manifest, robots, and structured data | Pass |
| JavaScript syntax | Pass |
| Shell script static analysis | Pass |
| Workflow YAML parsing | Pass |
| Sensitive-file, symlink, credential-marker, source-map, and private-content boundary | Pass |
| Artifact-local security-header contract | Pass: six exact directives; HSTS intentionally absent |
| `git diff --check` | Pass |

The browser suite exercised all nine routes at 390 by 844 pixels and key
routes at 768 by 1024, 1440 by 1000, and 1920 by 1080. It verified one primary
heading per route, canonical metadata, navigation, overflow, images, themes,
the mobile menu, site explorer, screenshot lightbox, local-only test progress,
resource and privacy search, reduced motion, forced colors, no-JavaScript
fallback, and absence of browser errors.

The same browser suite also passed against the HTTPS origin with Chrome's
operator-supplied hostname resolver mapping. This exercised the deployed CSP
and other response headers while retaining normal public certificate and
hostname verification; it did not require public DNS or a TLS trust bypass.

## Lighthouse

The final mobile homepage audit reported:

| Category or metric | Result |
| --- | ---: |
| Performance | 99 |
| Accessibility | 100 |
| Best Practices | 100 |
| SEO | 100 |
| First Contentful Paint | 1.35 s |
| Largest Contentful Paint | 1.80 s |
| Cumulative Layout Shift | 0 |
| Total Blocking Time | 0 ms |

An immediately preceding mobile run scored 100 in all four categories, and the
desktop homepage audit also scored 100 in all four categories. Privacy, Product
Testing, Roadmap, and Resources each scored 100 for accessibility, best
practices, and SEO in route-level audits.

Lighthouse results are controlled local measurements, not guarantees of
production field performance. Webuzo, Cloudflare, network, cache, and origin
behavior require separate staging and production validation.

## Public-content review

- The canonical privacy notice remains generated from `PRIVACY.md` and is
  published at `/privacy/`.
- The public privacy contact is limited to the approved GitHub issue route.
- The old `codeframe78` site identity and repository destinations are absent
  from the deployment artifact.
- Internal planning details and speculative delivery forecasts are absent
  from the rendered site.
- The resource library is narrowed to 20 durable, public-facing references.
- Current-development and roadmap statements carry dated verification context.
- No `.git`, `.env`, credential, private key, development dependency, log,
  source map, internal configuration, or repository source document is in the
  artifact.

## Deployment safety

The production artifact is `_site/`; source and dependencies must remain
outside every public document root. The Project Site workflow validates both
the dedicated-domain artifact and a temporary GitHub Pages transition, but its
deploy job cannot run unless the separately approval-gated repository variable
`PLAYER_PAGES_TRANSITION_APPROVED` equals `true`.

Webuzo remains authoritative for the exact user, document root, ownership,
permissions, active server chain, certificate, logs, and backup. Origin
staging uses Webuzo 4.7.4, Apache 2.4.68, user `jamesjen`, and document root
`/home/jamesjen/player.jamesjennison.net`. The complete deployment, health,
transition, and rollback contract is in
[project-site-migration.md](project-site-migration.md).

## Origin-hardening validation

| Check | Result |
| --- | --- |
| Webuzo domain | Pass: `player.jamesjennison.net` is an isolated subdomain owned by `jamesjen` |
| Document root | Pass: exact approved path; no overlap with master, status, webmail, or another user |
| Artifact parity | Pass: all 25 files match digest `3729bc53082966c0473e8fb04da820f731c0f1c3140a5f7d821a8717b7d79bb5` locally and at the origin |
| Canonical routes | Pass: eight content routes return 200 with their approved canonical URLs |
| Custom 404 | Pass: unknown paths return status 404 with the Player error page |
| Static assets | Pass: CSS, JavaScript, PNG, manifest, robots, and sitemap are served |
| Sensitive paths | Pass: `.git`, `.env`, source documentation, and `.htaccess` are not publicly readable |
| Webuzo configuration | Pass: Apache syntax valid; Webuzo and MariaDB active; origin pages served successfully |
| Public DNS | Unchanged: Player and generated internal aliases have no A, AAAA, or CNAME records |
| Existing sites | Apex and `www` retain their pre-existing 403 responses; status and the current organizational GitHub Pages site return 200 |
| Legacy Pages URL | `codeframe78.github.io/24Seven.FM-Player/` returns GitHub's 404; repository records identify it as a historical Play Console privacy URL, so owner verification is required before cutover |
| Backups | Pass: pre-hardening snapshot `bcefb231` passed a full Restic data check and streamed `index.html`/`.htaccess` restores; post-hardening snapshot `77f5810b` passed artifact and `.htaccess` restore verification |
| HTTPS trust | Pass: Webuzo installed the existing publicly trusted Let's Encrypt wildcard for Player; direct-origin chain and hostname validation pass without a trust bypass |
| Certificate isolation | Pass: only Player's Webuzo certificate set changed; the apex wildcard identity remained unchanged; unprivileged key access is blocked by Webuzo's restricted SSL directory chain |
| Security headers | Pass: CSP, Permissions Policy, Referrer Policy, MIME sniffing protection, frame denial, and cross-origin opener policy match exactly on HTTPS success and custom-404 responses |
| HSTS/cache headers | Intentionally unchanged pending edge-wide HTTPS and caching review |
| Cloudflare TLS | Read-only audit complete: current origin mode is Full, not Full (Strict); Universal SSL actively covers the apex and wildcard |
| Cloudflare rules | No legacy Page Rules or applicable redirect, transform, origin, cache, configuration, compression, or response-header Rulesets are deployed |

The Webuzo template generated internal `www.player` and `mail.player` aliases.
They are intentionally non-resolving and are not approved public hostnames. No
generated virtual-host file was hand-edited, no shared service was restarted,
and no custom reverse proxy, runtime process, database, or scheduled task was
introduced.

The prior origin release remains at
`/home/jamesjen/.player-previous-20260722T231413Z` for immediate atomic
rollback. The temporary wildcard expires August 27, 2026. A dedicated
Webuzo-managed certificate and controlled renewal proof remain required before
that date; this checkpoint does not claim that future renewal is automatic.

## Remaining gates

- Review and approval of this local commit
- GitHub push or pull request
- Dedicated origin certificate issuance and renewal validation
- Cache header configuration
- Approved Cloudflare Full (Strict) change and Player DNS record
- Production deployment and validation
- Play Console and other external privacy-URL verification/update
- GitHub Pages transition activation and eventual retirement

None of the remaining actions occurred during this milestone.
