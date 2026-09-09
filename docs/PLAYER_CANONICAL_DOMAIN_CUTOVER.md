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

Domain routing has a separate Cloudflare control plane. Before changing a
redirect rule, capture the three exact-host rules' current state and test the
primary route. To reverse a redirect, restore the recorded prior state for
that exact rule only, verify the intended destination can actually serve the
old hostname, and then retest all three hostnames plus the primary route.

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
