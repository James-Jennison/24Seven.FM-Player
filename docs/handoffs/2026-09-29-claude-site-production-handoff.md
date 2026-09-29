# 24Seven.FM Player public-site handoff

Prepared for Claude's site-production and potential redesign work after the
2026-09-29 public-status correction.

## Current production baseline

- Canonical public site: `https://24sevenfmplayer.com/`.
- Deployed source commit: `d4ea6e52841225b06fce47dc8bfa70c5ddd83fa4`
  (`codex/site-release-status-refresh`).
- Built on `gthost-build01`; the promoted static artifact was verified before
  cutover and again at the live document root. The server-side release keeps a
  recoverable prior-directory rollback point.
- Public HTTPS and origin checks returned 200 for `/`, `/platforms/`,
  `/features/`, `/product-testing/`, and `/tester-portal.php`. The production
  edge is served through Cloudflare.
- The current change corrects public release status. It is not a redesign and
  should be treated as a stable factual baseline rather than a visual target.

## Claims that must remain accurate

Use these boundaries unless a newer authoritative public source is verified
immediately before a change:

1. Android mobile: **“24Seven.FM: Internet Radio App is available to install
   from Google Play.”** Link only to the verified package listing:
   `com.codeframe78.twentyfourseven.player`.
2. Do not infer country coverage, installed version, release-artifact identity,
   source commit, rollout completion, or Google Play review state from that
   statement.
3. Android TV remains early development/testing. Do not show a public install
   CTA until an authoritative public listing exists.
4. The invitation-led tester program is separate from the public Android
   release. Do not portray it as the public installation path.
5. Relationship wording: the Player is independently developed by an **early
   24Seven.FM member** and is not published or sponsored by 24Seven.FM or its
   individual stations. Do not revert to “unofficial,” “not affiliated,” or
   “pre-release” language for the public Player.

The machine-readable fact contract is
[`docs/WEBSITE_FACTS_CONTRACT.json`](../WEBSITE_FACTS_CONTRACT.json). The
listing-backed observation and its explicit scope limits are in
[`docs/releases/public-google-play-availability-2026-09-29.md`](../releases/public-google-play-availability-2026-09-29.md).

## Product and design direction from the owner

The Player is a focused native-app home for five official station choices; it
must not duplicate the stations' web listening experiences with an embedded
web player.

- Prefer a compact utility-style landing page with an immediately useful,
  screen-fitting decision surface on a wide desktop display.
- Do not make the site a long, scrolling campaign narrative or a stack of
  oversized cards. Use secondary pages and clear navigation for depth.
- Keep the five stations as distinct destinations that link to their official
  sites; use verified official station/app imagery rather than generic visual
  placeholders.
- Preserve separate, clear routes for Stations, Features, Platforms, Privacy,
  and the protected tester program.
- A future visual language should be deliberately chosen for this product; do
  not mechanically reuse the prior dark-gradient/card treatment.

## Scope boundary: protected portals

`tester-portal.php`, `private-tester-queue.php`, associated protected assets,
and tester records are not part of the public-site redesign. Their claims,
access controls, and implementation belong to the separate protected portal
scope. Do not use a public-site redesign to change tester/coordinator behavior
or data handling.

## Production procedure and evidence gates

1. Inspect the current public source and the live public page before changing
   claims; do not rely on this dated handoff for volatile release facts.
2. Build the website on `gthost-build01`, never the workstation. Record the
   source commit and artifact hash.
3. Use only the `website-vm-admin` SSH alias for this Player website. Discover
   the active HTTP and HTTPS document-root mappings; do not infer a path from
   another site or use the legacy mail-VM alias.
4. Stage the generated artifact as a sibling release directory, compare its
   relative-path hashes, preserve non-site-owned content such as the current
   Cast receiver directory, lint the staged PHP files with the Webuzo PHP
   runtime, then atomically swap it into the active root.
5. Retain the previous directory as rollback. Verify both origin and public
   HTTPS routes after promotion; never claim live success from a local build
   alone.
6. Treat Cloudflare DNS, proxy, TLS, cache, and WAF changes as separate actions
   from a routine content release. Do not change them unless explicitly scoped.

## Current validation and review evidence

- The source facts contract, fail-closed negative fixtures, static site
  validation, tester-task registry validation, JavaScript syntax checks, and
  pages-transition validation passed on the remote build.
- The exact staged PHP files passed Webuzo PHP 8.4 syntax lint before the
  atomic cutover.
- Chromium coverage against the copied remote artifact exercised responsive
  layouts, keyboard/pointer behavior, local-only state, reduced motion, forced
  colors, and no-JavaScript fallback. Firefox responsive coverage existed for
  the unchanged UI before this status-only correction. WebKit has not been run
  and must not be reported as validated.
- Grok, Gemini, and the UX reviewer found no remaining evidence-backed defects
  in the remediation package. A DeepSeek claim about stale wording in a
  protected-portal template was not applicable to the public artifact; the
  scoped public confirmation-email contract passed. This is a recorded scope
  distinction, not evidence that protected portal wording was redesigned.

## Useful implementation locations

- Public pages: `privacy-site/index.html`, `privacy-site/stations/`,
  `privacy-site/features/`, `privacy-site/platforms/`, and
  `privacy-site/product-testing/`.
- Shared layout/navigation: `privacy-site/_layouts/` and
  `privacy-site/_includes/`.
- Visual system: `privacy-site/assets/project.css` and
  `privacy-site/assets/project.js`.
- Build/validation: `scripts/build-project-site.sh`,
  `scripts/prepare-project-site.sh`, `scripts/validate-project-site.sh`, and
  `scripts/validate-website-facts-contract.py`.

Before a redesign, make a fresh visual audit at the intended desktop, tablet,
and mobile sizes and re-run the facts and deployment gates after any material
source change.
