# Player Cloudflare and TLS read-only audit

Audited July 22, 2026. This record contains no Cloudflare account identifier,
zone identifier, origin address, token value, mail value, certificate private
material, or validation token. No Cloudflare, DNS, Webuzo, SSL, site-content,
GitHub, or production setting was changed.

## Verified Cloudflare state

| Area | Verified state |
| --- | --- |
| Zone | `jamesjennison.net` is active, unpaused, full-setup, and on the Free Website plan |
| Authoritative DNS | Cloudflare nameservers are authoritative |
| Apex | Proxied A record points to the Webuzo origin; TTL is Auto |
| `www` | Proxied CNAME points to the apex; TTL is Auto |
| Status | Proxied Cloudflare record remains present |
| Mail | DNS-only mail record plus MX, SRV, and TXT dependencies remain present and out of scope |
| Player | No A, AAAA, or CNAME record exists |
| Player aliases | No `www.player` or `mail.player` record exists |
| Wildcard DNS | No wildcard record exists |
| CAA | No CAA record exists |
| Legacy Page Rules | No active or disabled Page Rules exist |
| DNS settings | CNAME flatten-all disabled; multi-provider DNS disabled; secondary overrides disabled |
| Origin encryption | Full; not Full (Strict) |
| HTTPS settings | Always Use HTTPS on; Automatic HTTPS Rewrites on |
| Edge protocols | Minimum TLS 1.0; TLS 1.3 API state `zrt`; 0-RTT on; HTTP/3 on; Brotli on |
| HSTS | Disabled; no zone-level HSTS policy is active |
| Default cache | Aggressive cache level; browser cache TTL 14,400 seconds; Development Mode off |
| Modern Rulesets | No deployed redirect, transform, origin, cache, configuration, compression, or response-header entry point |

The dedicated DNS token is active and can read the zone and DNS configuration.
The existing Wrangler OAuth session can identify the zone. Neither credential
has permission to read Zone Settings, SSL and Certificates, or modern Rulesets.
The API returned authorization failures without exposing either credential.

The owner then supplied a separate user API token for the expanded audit. Its
secret is stored outside the repository with mode `0600`. The first reads were
rejected while its policy had not taken effect; after the owner reviewed and
saved the exact account and zone scope, Cloudflare authorized the zone,
settings, SSL/certificate, and Rulesets reads. No secret or identifier was
written to the repository or included in this record.

## Verified SSL and certificate state

- Universal SSL is enabled.
- The active Universal certificate pack covers `jamesjennison.net` and
  `*.jamesjennison.net`, uses Google Trust Services ECDSA, and expires October
  17, 2026. This includes `player.jamesjennison.net` at the Cloudflare edge.
- A Let's Encrypt Universal backup certificate for the same apex and wildcard
  coverage is in `backup_issued` state and expires October 17, 2026.
- A separate active Advanced certificate pack covers the apex,
  `status.jamesjennison.net`, and `*.status.jamesjennison.net` with Google Trust
  Services ECDSA and RSA certificates expiring October 20, 2026.
- Cloudflare reports certificate verification active. The Free plan does not
  permit uploaded custom edge certificates; none is needed for Player.
- At audit time, the zone-to-origin mode was Full. It encrypts the connection
  but does not validate the origin certificate, so the then-current Player
  self-signed placeholder would not have failed solely because it was
  untrusted. That was not the approved production posture; the target remains
  Full (Strict) with a trusted Player origin certificate.

## Verified Ruleset state

Cloudflare returned four zone-visible Rulesets: its managed URL normalization,
Free Managed WAF, and HTTP DDoS Rulesets plus one zone rate-limit entry point.
The account inventory contains only the managed Free WAF Ruleset. There is no
zone or account entry point in the Single Redirect, Bulk Redirect, URL Rewrite,
request-header transform, response-header transform, Origin Rules, Cache Rules,
Configuration Rules, or Compression Rules phase. The direct URL Rewrite and
header-transform phase reads independently returned not found. Legacy Page
Rules are also empty.

The token does not have each product-specific permission needed to open the
Single Redirect, Origin, Cache, Configuration, and Compression entry points
individually. Their absence is established through Cloudflare's zone/account
Ruleset list operations, which return configured entry points across phases;
it is not represented as a successful product-specific phase read.

The one enabled rate-limit rule blocks for ten seconds after five matches in
ten seconds per source IP and Cloudflare data center. Its condition is limited
to Cloudflare's leaked-password credential signal; it contains no hostname,
Player, URI, path, or source-IP filter and does not govern ordinary static-site
requests. No account Bulk Redirect phase is deployed, so an account redirect
list cannot currently alter this zone even though orphan account lists were not
enumerated.

## Verified public behavior

- Cloudflare redirects HTTP apex and HTTP `www` requests permanently to HTTPS
  on the same hostname while preserving path and query string.
- The direct Webuzo HTTP origin does not perform that redirect. The redirect is
  therefore applied by the verified Always Use HTTPS zone setting.
- HTTPS `www` does not redirect to apex. The approved master canonical redirect
  is still not configured.
- The active Cloudflare edge certificate presented for `www` contains the apex
  and `*.jamesjennison.net`, is issued by Google Trust Services, and expires
  October 17, 2026.
- A direct SNI probe for `player.jamesjennison.net` at the Cloudflare edge
  presents that valid wildcard certificate. Cloudflare then returns its
  no-DNS-record response, as expected.

This proved that edge certificate coverage was ready for a first-level Player
record. It did not make the then-current self-signed Player origin suitable for
the approved Full (Strict) production posture.

On the final audit pass, HTTPS apex and `www` both returned the same 403 Webuzo
default-site response through Cloudflare; the read-only audit did not cause that
existing response. The status hostname returned 200 from its documented
Cloudflare Worker Custom Domain with its own HSTS, content security policy, and
other hardened headers. The status Worker does not depend on the Webuzo origin
certificate path.

## Verified Webuzo origin state

| Certificate | Coverage | Issuer and trust | Validity | Status |
| --- | --- | --- | --- | --- |
| Existing master wildcard | Apex and `*.jamesjennison.net` | Let's Encrypt; publicly trusted | Through August 27, 2026 | Valid now; recent renewal health is not established |
| Player placeholder | `player.jamesjennison.net` common name only | Self-signed; not publicly trusted | Through July 22, 2027 | Staging only; incompatible with Full (Strict) |

### Post-audit origin-hardening checkpoint

The owner separately approved the first three steps of the sequence below.
Before any origin mutation, Restic snapshot `bcefb231` captured the Player
document root, Apache and Webuzo state, database dump, and certificate files; a
full repository data check and streamed restore comparisons passed. Webuzo's
supported install-certificate workflow then assigned the existing trusted
Let's Encrypt apex/wildcard certificate to Player. No generated virtual-host
file was hand-edited.

Direct-origin HTTPS now passes public chain and exact-hostname validation for
`player.jamesjennison.net`. The Player certificate identity matches the
existing wildcard, Apache syntax passes, all project routes and the custom 404
retain their expected statuses, and apex, `www`, and status retain their
pre-change responses. Public Player DNS remains absent, Cloudflare remains in
Full mode, and no Cloudflare setting changed. Hardened-state snapshot
`77f5810b` preserves the verified result.

The current organizational GitHub Pages site at
`james-jennison.github.io/24Seven.FM-Player/` remains available with HTTP 200,
and the approval-gated transition variable is absent. The historical
`codeframe78.github.io` URL returns GitHub's 404 and must not be treated as a
healthy fallback.

This wildcard is temporary Player coverage and expires August 27, 2026. The
failed earlier HTTP-01 renewal means a dedicated Webuzo-managed certificate and
controlled renewal proof remain required before relying on unattended renewal.

Webuzo runs its Let's Encrypt renewal command daily at midnight. Webuzo also
attempted a Player certificate immediately after the approved subdomain was
created, despite certificate issuance being disabled in the API request. It
pre-checked the generated `www.player` and `mail.player` aliases, skipped them
when they could not resolve, and attempted only the primary Player hostname.
Issuance failed solely because Player had no A or AAAA record, which was the
approved staging state.

The earlier master-wildcard renewal attempt failed when the Cloudflare-served
HTTP-01 challenge returned 404. That separate renewal problem remains a risk to
the master domain and prevents assuming that future Player renewal will work
without a controlled test.

## Hardening findings outside the Player cutover

The minimum edge TLS version is still 1.0 and HSTS is disabled. Raising the
minimum to TLS 1.2 is recommended after a compatibility review. HSTS must wait
until every intended web hostname and redirect is HTTPS-stable; enabling
`includeSubDomains` prematurely could make an overlooked hostname inaccessible.
The zone also enables 0-RTT, which is low-risk for this static site but should be
reviewed before state-changing applications share the zone. These are separate
approval gates and are not prerequisites for installing a trusted Player origin
certificate.

## Recommended Player TLS and DNS sequence

The preferred certificate is a dedicated Webuzo-managed Let's Encrypt
certificate because it remains publicly trusted outside Cloudflare and keeps
Webuzo authoritative for issuance and renewal. Cloudflare Origin CA is the
documented fallback if Webuzo's HTTP-01 path cannot be made reliable; it should
not be the first choice while hosting independence is valuable.

1. **Complete:** take a fresh Restic snapshot of the Player virtual host, Webuzo database,
   Apache configuration, and certificate state.
2. **Complete:** through Webuzo's SSL manager, temporarily assign the existing valid wildcard
   certificate to the Player virtual host. Do not hand-edit generated Apache
   configuration.
3. **Complete:** validate direct origin HTTPS for the Player hostname and its complete chain,
   and revalidate the trusted master-origin certificate.
4. With separate approval, change the zone origin mode from Full to Full
   (Strict), then validate apex, `www`, and the status Worker. Restore Full
   immediately if an existing hostname unexpectedly fails.
5. Create exactly one Cloudflare A record: `player.jamesjennison.net`, pointing
   to the same origin target as the apex, proxied, TTL Auto. Do not create
   `www.player`, `mail.player`, AAAA, CNAME, or wildcard records.
6. Confirm edge certificate coverage, origin reachability, and Full (Strict)
   operation before treating the hostname as available.
7. Place a short-lived randomized HTTP-01 probe under the Player document root,
   retrieve it through Cloudflare without cache interference, then remove it.
8. If the probe passes, ask Webuzo to issue a dedicated Let's Encrypt
   certificate only for `player.jamesjennison.net`; generated aliases remain
   non-resolving and must not be requested as public names.
9. Validate the new certificate chain, Webuzo renewal ownership, route matrix,
   custom 404, logs, existing master/status/mail health, and the GitHub Pages
   fallback.
10. Security headers are complete. Request separate approval for cache headers
    and production cutover. Keep GitHub Pages unchanged until those gates pass.

If HTTP-01 fails, withdraw the Player DNS record or retain the valid temporary
wildcard while the hostname remains unannounced. After Full (Strict) is proven,
do not lower the zone to accommodate an untrusted origin. The alternative is a
dedicated Cloudflare Origin CA certificate generated from an origin-held key or
CSR and installed through Webuzo. Origin CA is compatible with Full (Strict),
but direct browsers do not trust it and it requires explicit expiry monitoring.

## Rollback

Before public DNS, restore the prior Player certificate and Webuzo state from
the fresh snapshot if any SSL installation fails. After DNS creation, remove
only the Player record to return the hostname to its current non-resolving
state, restore the prior Player certificate through Webuzo, and re-run apex,
`www`, status, mail, and GitHub Pages checks. No rollback step may alter the
master-site files, status service, mail DNS, unrelated domains, or shared
server configuration.

## Approval boundary

The subsequent owner approval authorized only the recorded origin-hardening
checkpoint: artifact security headers, verified backups, redeployment to the
existing isolated root, and temporary wildcard assignment through Webuzo. It
does not authorize new certificate issuance, challenge-file creation, DNS
records, Cloudflare changes, GitHub changes, public staging, or production
cutover.
