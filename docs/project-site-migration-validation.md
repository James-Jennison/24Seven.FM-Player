# Dedicated-domain project-site validation

Validated July 22, 2026. Production deployment is not authorized by this
record.

## Outcome

The existing Jekyll portal was retained, refined, and staged as a static
artifact for `https://player.jamesjennison.net`. The implementation uses the
approved clean route model, James-Jennison identity, curated public content,
GitHub-only privacy contact, and an Apache-local custom 404 mapping. Webuzo
origin staging is complete; public DNS, trusted SSL, Cloudflare, GitHub
settings, GitHub Pages, and production remain unchanged.

Source was isolated on local branch `codex/player-site-migration` from commit
`c54057151f34e5505f238391d08a5520d8e45c06`. The owner's separate dirty main
checkout was not used for implementation.

## Reproducible validation

Run from the repository root:

```bash
./scripts/validate-project-site.sh
node ./scripts/test-project-site-browser.mjs
```

The static build uses the digest-pinned image
`ghcr.io/actions/jekyll-build-pages@sha256:6791ebfd912185ed59bfb5fb102664fa872496b79f87ff8b9cfba292a7345041`.

The final `_site/` artifact contains 25 files totaling approximately 3.42 MiB,
including nine HTML documents. Its deterministic inventory digest, calculated
by sorting each relative file path and content SHA-256 into a second SHA-256,
is:

```text
b6f62600b4ab04a5f124a5f2547e4795fcf8be72225fc4af597809025686a9ca
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
| `git diff --check` | Pass |

The browser suite exercised all nine routes at 390 by 844 pixels and key
routes at 768 by 1024, 1440 by 1000, and 1920 by 1080. It verified one primary
heading per route, canonical metadata, navigation, overflow, images, themes,
the mobile menu, site explorer, screenshot lightbox, local-only test progress,
resource and privacy search, reduced motion, forced colors, no-JavaScript
fallback, and absence of browser errors.

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

## Origin-staging validation

| Check | Result |
| --- | --- |
| Webuzo domain | Pass: `player.jamesjennison.net` is an isolated subdomain owned by `jamesjen` |
| Document root | Pass: exact approved path; no overlap with master, status, webmail, or another user |
| Artifact parity | Pass: all 25 files match digest `b6f62600b4ab04a5f124a5f2547e4795fcf8be72225fc4af597809025686a9ca` |
| Canonical routes | Pass: eight content routes return 200 with their approved canonical URLs |
| Custom 404 | Pass: unknown paths return status 404 with the Player error page |
| Static assets | Pass: CSS, JavaScript, PNG, manifest, robots, and sitemap are served |
| Sensitive paths | Pass: `.git`, `.env`, source documentation, and `.htaccess` are not publicly readable |
| Webuzo configuration | Pass: Apache syntax valid; Webuzo and MariaDB active; origin pages served successfully |
| Public DNS | Unchanged: Player and generated internal aliases have no A, AAAA, or CNAME records |
| Existing sites | Unchanged: apex, `www`, status, and the GitHub Pages fallback retain their prior responses |
| Backups | Pass: snapshots `40412861`, `21c5494b`, and `0f7293db`; full Restic data check and streamed restore check pass |
| HTTPS trust | Pending: Webuzo generated a self-signed placeholder; no trusted Player certificate was issued |
| Security/cache headers | Pending separate approval; no domain-specific header configuration was added |

The Webuzo template generated internal `www.player` and `mail.player` aliases.
They are intentionally non-resolving and are not approved public hostnames. No
generated virtual-host file was hand-edited, no shared service was restarted,
and no custom reverse proxy, runtime process, database, or scheduled task was
introduced.

## Remaining gates

- Review and approval of this local commit
- GitHub push or pull request
- Trusted origin certificate coverage and renewal validation
- Security and cache header configuration
- Cloudflare control-plane review and approved Player DNS record
- Production deployment and validation
- Play Console privacy URL change
- GitHub Pages transition activation and eventual retirement

None of the remaining actions occurred during this milestone.
