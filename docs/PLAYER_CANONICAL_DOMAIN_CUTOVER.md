# Player canonical-domain cutover runbook

## Active routing contract

`https://24sevenfmplayer.com` is the primary public Player hostname. The
following exact hostnames use active Cloudflare Single Redirect rules that
return HTTP `308` and preserve the requested path and query string:

- `24sevenfmplayer.net`
- `24sevenfmplayer.app`
- `player.jamesjennison.net`

The rules are deliberately exact-host matches. They do not redirect a parent
domain or any other subdomain.

## Normal verification

Use public HTTPS header checks, without sending form data, for all three
legacy hostnames. Each must return `308` with a `Location` under
`https://24sevenfmplayer.com` that retains both the path and query string.
For each host, the expected transformations include:

- `/` → `https://24sevenfmplayer.com/` (exactly one slash after `.com`)
- `/privacy/?x=1&y=a%20b` →
  `https://24sevenfmplayer.com/privacy/?x=1&y=a%20b`

Then check the primary public routes:

- `/` and `/privacy/` return `200`.
- `/alpha-tester-interest.php` returns its expected `303` transition.
- `/tester-portal.php` and `/private-tester-queue.php` return `200` for their
  public entry responses.

The primary-host Turnstile widgets are configured to allow
`24sevenfmplayer.com`. A human browser challenge and, if separately authorized,
a real form submission remain the acceptance test for the full challenge and
submission path. Do not create a tester application just to exercise this
check.

## Recovery boundaries

The retained prior primary document-root artifact is a **content rollback** for
`24sevenfmplayer.com`; it is not a domain-routing rollback.

### Primary-content rollback

1. Verify the primary hostname is reachable and identify the retained prior
   artifact as the desired known-good release.
2. Use the established atomic artifact-promotion procedure to restore that
   artifact into the primary document root, retaining the then-current artifact
   as the next rollback point.
3. Re-run the primary public route checks above. This changes primary content
   only; it leaves every Cloudflare redirect rule in place.

Domain routing has a separate Cloudflare control plane. Before changing a
redirect rule, capture the three exact-host rules' current state and test the
primary route. To reverse a redirect, restore the recorded prior state for
that exact rule only, verify the intended destination can actually serve the
old hostname, and then retest all three hostnames plus the primary route.

### Host-routing reversal

1. Establish and verify a viable HTTPS origin for the exact old hostname; a
   redirect-only hostname is not a valid destination.
2. Capture the current state of all three exact-host rules and run the normal
   primary route checks.
3. With separate approval, change only the exact-host Cloudflare rule that is
   being reversed to its recorded prior state or to the verified replacement
   destination. Do not modify parent-domain or unrelated hostname rules.
4. Publicly verify the changed old host, the two unchanged old hosts, and the
   primary route. Record that a previously cached `308` may continue directing
   some clients to the primary during cache expiry.

Do not simply disable a rule as an emergency shortcut: the alternate new apex
domains are redirect-only and may not have a viable independent origin. Also,
an HTTP `308` can remain cached by browsers and intermediaries after a rule is
changed. Treat any host-routing reversal as a separate, explicitly approved
deployment with an origin-readiness check; use the retained artifact only for
a content rollback on the primary hostname.

## Operational constraints

- Manage domain routing through Cloudflare and Webuzo-supported interfaces;
  never hand-edit Webuzo-generated virtual-host configuration.
- Keep the existing prior primary artifact until a later, separately approved
  cleanup.
- Host-scoped tester and Coordinator sessions do not transfer to the new
  primary hostname. Existing users sign in again at `24sevenfmplayer.com`.
