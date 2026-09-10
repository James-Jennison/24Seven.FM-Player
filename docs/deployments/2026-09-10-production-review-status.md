# Production-review status deployment

- **Deployed artifact source:** `53bb9062e7c3e0bb4e858de99e730b90f6221fc0`
- **Source branch:** `codex/primary-domain-cutover`
- **Artifact inventory:** 57 files; relative-path SHA-256 manifest
  `110283e9a430a5db7809d82d970d774cf2f8f9fe9aa7913d1e2ed187a359630b`.
- **Scope:** Public website content only. No Cloudflare, Webuzo configuration,
  certificate, Google Play, database, or protected-portal data change.

## Promotion and rollback

The artifact was staged beside the verified primary document root, then its
relative-path file manifest was compared exactly before promotion. The former
live artifact is retained as:

`24sevenfmplayer.com.rollback-production-review-53bb906-20260910T124256Z`

This is the content rollback point for the primary hostname. It does not alter
the separate Cloudflare redirect rules for the legacy domains.

## Verification

After promotion, direct-origin HTTPS and public HTTPS each returned `200` for
`/`, `/features/`, `/product-testing/`, `/dev/roadmap/`, and `/privacy/`. The
public home page contained the exact owner-attested statement that the first
production release is under Google Play review and is not yet available for
public installation.

The three legacy domains (`24sevenfmplayer.net`, `24sevenfmplayer.app`, and
`player.jamesjennison.net`) each retained their expected `308` redirect to the
primary hostname.
