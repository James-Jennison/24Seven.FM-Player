# Milestone Real-World Time Ledger

Last updated: `July 22, 2026 at 4:24:19 PM PDT (UTC−07:00)`

This is the permanent time-accounting record for the canonical M01–M60 milestones in [ROADMAP.md](ROADMAP.md). It
preserves historical uncertainty instead of treating commit spans as labor time. Future milestone work must be
measured prospectively from the actual authorized execution start.

## Cumulative project totals

| Measure | Cumulative value | Qualification |
| --- | ---: | --- |
| Total canonical milestones planned | 60 | M01–M60 |
| Total canonical milestones completed | 33 | M01–M28 and M31–M35 |
| Confirmed or reconstructed active hours | 0.00 h identified | Historical active intervals were not recorded, so the real total is unknown. |
| Total automated wait hours | Unknown | Workflow durations exist, but there is no evidence that execution waited on every run. |
| Total user-blocked hours | Unknown | Historical owner, device, approval, credential, and external waits lack complete pause/resume timestamps. |
| Total counted project hours | Unknown | Active execution plus automated wait cannot be reconstructed defensibly. |
| Total elapsed hours across completed milestones | At least 300.49 h | Lower-bound total across 25 observable milestone windows; 8 completed milestones have unknown elapsed duration. |
| Average counted hours per completed milestone | Unknown | Counted project time is unknown for every historical completion. |
| Median counted hours per completed milestone | Unknown | Counted project time is unknown for every historical completion. |
| Longest completed milestone | Unknown by counted time | M34 has the longest observable elapsed lower bound at 90.27 h. |
| Shortest completed milestone | Unknown by counted time | M31 has the shortest observable elapsed lower bound at 0.03 h. |
| Overall forecast versus actual variance | Unknown | Only M13, M14, and M17–M21 retained milestone-specific forecasts, and their actual counted time was not recorded. |
| Confidence classifications | Confirmed 0; Reconstructed 0; Estimated 28; Unknown 32 | One classification per canonical milestone. |
| Ledger last updated | `July 20, 2026 at 7:43:14 PM PDT (UTC−07:00)` | Full date, 12-hour time, seconds, timezone abbreviation, and UTC offset. |

Elapsed totals are arithmetic milestone totals, not unique calendar time. Milestones overlapped, so counted work must
be assigned to only one milestone while each milestone retains its own wall-clock elapsed period. The confidence counts
cover all 60 canonical entries: 25 completed observable windows, the M29/M30 start boundaries, and the M51 research-to-
retirement window are Estimated; every entry without a defensible historical duration is Unknown.

## Master-site program website milestones

Website work requested through the JamesJennison.net program is tracked here because it changes this repository, but it
does not alter the status, numbering, or cumulative totals of the canonical Android M01–M60 roadmap.

| Program milestone | Status | Started | Forecast | Expected completion | Model and reasoning | Active | Automated wait | User-blocked |
| --- | --- | --- | --- | --- | --- | ---: | ---: | ---: |
| Master M12 — Player local migration implementation | Complete | `July 22, 2026 at 1:51:13 PM PDT (UTC−07:00)` | 8–14 active h | By `July 23, 2026 at 3:51:13 AM PDT (UTC−07:00)` if uninterrupted | GPT-5; current default reasoning strength, unchanged | 0.61 h | 0.00 h | 0.00 h |
| Master M13 — Player origin staging | Complete | `July 22, 2026 at 2:50:15 PM PDT (UTC−07:00)` | 1.5–3 active h | By `July 22, 2026 at 5:50:15 PM PDT (UTC−07:00)` if uninterrupted | GPT-5; current default reasoning strength, unchanged | 0.21 h | 0.00 h | 0.00 h |
| Master M14 — Cloudflare and Player TLS verification | Complete | `July 22, 2026 at 3:07:22 PM PDT (UTC−07:00)` | 0.5–1.5 active h | Completed `July 22, 2026 at 3:54:26 PM PDT (UTC−07:00)` | GPT-5; current default reasoning strength, unchanged | 0.32 h | 0.00 h | 0.46 h |
| Master M15 — Player trusted-origin hardening | Complete | `July 22, 2026 at 3:59:38 PM PDT (UTC−07:00)` | 1–2 active h | Completed `July 22, 2026 at 4:24:19 PM PDT (UTC−07:00)` | GPT-5; current default reasoning strength, unchanged | 0.41 h | 0.00 h | 0.00 h |

### Master M12 — Player local migration implementation

- **Objective:** Reuse the existing Jekyll project portal while implementing the owner-approved dedicated-domain
  routes, James-Jennison identity, curated public content, GitHub-only privacy contact, metadata, accessibility,
  artifact validation, and migration documentation.
- **Authorization and start:** Authorized by the owner and started `July 22, 2026 at 1:51:13 PM PDT (UTC−07:00)`.
- **Model, reasoning strength, and original forecast:** GPT-5 with the current default reasoning strength, unchanged;
  8–14 active hours; expected completion by `July 23, 2026 at 3:51:13 AM PDT (UTC−07:00)` if uninterrupted.
- **Completion gates:** Reproducible static build; approved URL and identity migration; reviewed public content;
  keyboard, reduced-motion, responsive, link, metadata, and artifact checks; milestone documentation; and a local
  commit. GitHub push, GitHub Pages changes, Webuzo, DNS, Cloudflare, SSL, staging, and production remain excluded.
- **Completion:** Implementation and validation completed `July 22, 2026 at 2:27:41 PM PDT (UTC−07:00)`. The measured
  window was 0.61 active hours with no user-blocked interval. Short build and audit executions remained actively
  monitored and did not create a separately measured automated-wait interval.
- **Forecast variance:** 7.39 hours (92.4%) below the 8-hour lower bound. The original forecast reserved time for a
  broader redesign and uncertain portal remediation; owner approval to retain the already strong portal narrowed the
  work to a static-route migration, public-content review, accessibility hardening, validation, and deployment
  preparation.
- **Evidence:** Dedicated-domain and transition validators pass; the browser suite passes nine routes at four
  viewports with keyboard and pointer interaction checks; the final mobile Lighthouse audit is 99/100/100/100 and the
  desktop audit is 100/100/100/100; artifact inventory digest
  `db2d0047cc43168c1d5483df4afc88d0a016da34c81136bb76a27e06ab7e7a7b`.
- **Forecasting lesson:** When a polished static project site already exists, estimate migration separately from visual
  redesign and production infrastructure work. Keep DNS, SSL, Webuzo discovery, staging, and production rollout in
  their own approval-gated forecasts.

| Started | Ended | Category | Reason or work | Evidence | Hours |
| --- | --- | --- | --- | --- | ---: |
| `July 22, 2026 at 1:51:13 PM PDT (UTC−07:00)` | `July 22, 2026 at 2:27:41 PM PDT (UTC−07:00)` | Active | Create an isolated worktree, implement the approved website migration, validate it, and document the result. | Owner approval; branch `codex/player-site-migration`; base `c540571`; validation record | 0.61 h |

```text
Milestone Master M12 time:
Forecast: 8–14 active hours
Counted project time: 0.61 h
Total elapsed time: 0.61 h
User-blocked time excluded: 0.00 h
Forecast variance: 7.39 h below the lower bound (92.4%)
Cumulative counted project time through Milestone Master M12: 0.61 h (master-site program website milestones only; canonical Android cumulative remains Unknown)
```

### Master M13 — Player origin staging

- **Objective:** Create a verified pre-change backup, add only `player.jamesjennison.net` through Webuzo with the
  approved isolated document root, deploy the reviewed static artifact, and validate it through an origin-only method.
- **Authorization and start:** Authorized by the owner and started `July 22, 2026 at 2:50:15 PM PDT (UTC−07:00)`.
- **Model, reasoning strength, and original forecast:** GPT-5 with the current default reasoning strength, unchanged;
  1.5–3 active hours; expected completion by `July 22, 2026 at 5:50:15 PM PDT (UTC−07:00)` if uninterrupted.
- **Scope boundary:** Cloudflare, public DNS, certificate issuance or replacement, GitHub push, GitHub Pages changes,
  public staging, and production cutover remain excluded.
- **Completion:** Completed `July 22, 2026 at 3:02:42 PM PDT (UTC−07:00)`. The measured window was 0.21 active hours
  with no user-blocked or separately measured automated-wait interval.
- **Forecast variance:** 1.29 hours (86.2%) below the 1.5-hour lower bound. Existing root access, the installed Webuzo
  end-user API, a small static artifact, and a local Restic repository made the isolated staging operation faster than
  the original allowance for access discovery, large transfers, or service-level troubleshooting.
- **Evidence:** Webuzo owns the exact isolated domain and document root; artifact digest
  `b6f62600b4ab04a5f124a5f2547e4795fcf8be72225fc4af597809025686a9ca` matches locally and on the origin; all content
  routes, assets, protected paths, and the custom 404 pass; Restic snapshots `40412861`, `21c5494b`, and `0f7293db`
  pass a full data check and streamed restore check; public Player DNS remains absent.
- **Forecasting lesson:** Estimate a Webuzo static-origin stage separately from trusted certificate work, Cloudflare,
  public DNS, security headers, and production cutover. A pre-authorized, prebuilt artifact can be staged quickly when
  each of those higher-risk actions remains in its own gate.

| Started | Ended | Category | Reason or work | Evidence | Hours |
| --- | --- | --- | --- | --- | ---: |
| `July 22, 2026 at 2:50:15 PM PDT (UTC−07:00)` | `July 22, 2026 at 3:02:42 PM PDT (UTC−07:00)` | Active | Verify the deployment artifact, back up the origin configuration, create the isolated Webuzo domain, deploy, and validate origin-only staging. | Owner approval; branch `codex/player-site-migration`; source commit `4f3b540`; staging validation record | 0.21 h |

```text
Milestone Master M13 time:
Forecast: 1.5–3 active hours
Counted project time: 0.21 h
Total elapsed time: 0.21 h
User-blocked time excluded: 0.00 h
Forecast variance: 1.29 h below the lower bound (86.2%)
Cumulative counted project time through Milestone Master M13: 0.82 h (master-site program website milestones only; canonical Android cumulative remains Unknown)
```

### Master M14 — Cloudflare and Player TLS verification

- **Objective:** Verify available Cloudflare control-plane access and read the zone, DNS, proxy, SSL/TLS, certificate,
  and rule state needed to produce the exact approval-gated Player DNS and trusted-origin-certificate sequence.
- **Authorization and start:** Authorized by the owner and started `July 22, 2026 at 3:07:22 PM PDT (UTC−07:00)`.
- **Model, reasoning strength, and original forecast:** GPT-5 with the current default reasoning strength, unchanged;
  0.5–1.5 active hours; expected completion by `July 22, 2026 at 4:37:22 PM PDT (UTC−07:00)` if uninterrupted.
- **Scope boundary:** Read-only Cloudflare and public-network verification plus local documentation. Cloudflare, DNS,
  Webuzo, certificates, GitHub, GitHub Pages, staging content, and production configuration remain unchanged.
- **Pause:** Active execution paused `July 22, 2026 at 3:16:20 PM PDT (UTC−07:00)` after live DNS, edge TLS, public
  redirects, legacy Page Rules, Webuzo certificates, and ACME behavior were verified. The available credentials lack
  Zone Settings, SSL and Certificates, and modern Rulesets read permissions. The exact SSL/TLS mode and rule state
  cannot be verified without a separately scoped read-only token or connected integration.
- **Resume:** Active execution resumed `July 22, 2026 at 3:36:21 PM PDT (UTC−07:00)` after the owner supplied the
  separately scoped read-only Cloudflare token. The token file was tightened from mode `0664` to `0600` before use.
- **Second pause:** Active execution paused `July 22, 2026 at 3:38:48 PM PDT (UTC−07:00)`. Cloudflare verifies the
  new credential as an active user API token, but rejects direct reads of the approved zone, DNS, Zone Settings,
  SSL/certificate, and Rulesets resources. The token therefore needs its resource selection or permission policy
  corrected before the read-only audit can continue.
- **Second resume:** Active execution resumed `July 22, 2026 at 3:46:23 PM PDT (UTC−07:00)` after the owner confirmed
  that the token policy had been reviewed and saved in Cloudflare.
- **Completion:** Completed `July 22, 2026 at 3:54:26 PM PDT (UTC−07:00)`. The read-only audit verified Full rather
  than Full (Strict), active Universal wildcard edge coverage, HTTPS and protocol settings, HSTS state, the complete
  zone/account Ruleset inventories, the narrow leaked-password rate limit, and the absence of Player DNS. No
  Cloudflare, DNS, certificate, Webuzo, GitHub, staging-content, or production setting changed.
- **Forecast variance:** 0.18 hours (35.1%) below the 0.5-hour lower bound based on 0.32 counted hours. The total
  elapsed time was 0.78 hours because two owner-credential intervals totaling 0.46 hours were excluded.
- **Evidence:** Active scoped-token verification; successful zone, DNS, settings, certificate-pack, and Ruleset API
  reads; public edge and origin-bound TLS/HTTP checks; clean secret scan and documentation diff; detailed sanitized
  record in `player-cloudflare-tls-audit.md`.
- **Forecasting lesson:** Separate credential-policy activation from control-plane auditing, probe one authorized
  endpoint before a broad read set, and use the complete Ruleset inventory plus product-specific phase checks to
  distinguish absent rules from permission-filtered details.

| Started | Ended | Category | Reason or work | Evidence | Hours |
| --- | --- | --- | --- | --- | ---: |
| `July 22, 2026 at 3:07:22 PM PDT (UTC−07:00)` | `July 22, 2026 at 3:16:20 PM PDT (UTC−07:00)` | Active | Discover authorized Cloudflare read access, verify the zone and TLS control plane, reconcile it with Webuzo, and document the next mutation gate. | Owner approval; branch `codex/player-site-migration`; staging commit `916ae9a`; read-only audit draft | 0.15 h |
| `July 22, 2026 at 3:16:20 PM PDT (UTC−07:00)` | `July 22, 2026 at 3:36:21 PM PDT (UTC−07:00)` | User-blocked | Await a read-only Cloudflare credential or integration with Zone Settings, SSL and Certificates, and modern Rulesets visibility. | Cloudflare API authorization responses; required permission list in `player-cloudflare-tls-audit.md`; owner continuation | 0.33 h excluded |
| `July 22, 2026 at 3:36:21 PM PDT (UTC−07:00)` | `July 22, 2026 at 3:38:48 PM PDT (UTC−07:00)` | Active | Validate the supplied credential and attempt the remaining zone, DNS, settings, certificate, and Rulesets reads. | Cloudflare token verification and sanitized API authorization responses | 0.04 h |
| `July 22, 2026 at 3:38:48 PM PDT (UTC−07:00)` | `July 22, 2026 at 3:46:23 PM PDT (UTC−07:00)` | User-blocked | Await correction of the active token's Cloudflare resource scope or read-only permission policy. | Active-token verification plus resource-specific `Invalid access token` responses; owner confirmation | 0.13 h excluded |
| `July 22, 2026 at 3:46:23 PM PDT (UTC−07:00)` | `July 22, 2026 at 3:54:26 PM PDT (UTC−07:00)` | Active | Revalidate the saved token policy, complete the remaining Cloudflare reads, reconcile public and origin behavior, and finalize the approval-gated sequence. | Scoped API reads; public probes; `player-cloudflare-tls-audit.md`; final secret scan | 0.13 h |

```text
Milestone Master M14 time:
Forecast: 0.5–1.5 active hours
Counted project time: 0.32 h
Total elapsed time: 0.78 h
User-blocked time excluded: 0.46 h
Forecast variance: 0.18 h below the lower bound (35.1%)
Cumulative counted project time through Milestone Master M14: 1.14 h (master-site program website milestones only; canonical Android cumulative remains Unknown)
```

### Master M15 — Player trusted-origin hardening

- **Objective:** Add and verify artifact-level security headers, create a fresh restorable pre-change backup, redeploy
  the exact validated static artifact to the existing isolated Player document root, and assign trusted wildcard
  origin coverage through Webuzo without making Player public.
- **Authorization and start:** The owner continued after receiving the exact origin-only scope, and execution started
  `July 22, 2026 at 3:59:38 PM PDT (UTC−07:00)`.
- **Model, reasoning strength, and original forecast:** GPT-5 with the current default reasoning strength, unchanged;
  1–2 active hours; expected completion by `July 22, 2026 at 5:59:38 PM PDT (UTC−07:00)` if uninterrupted.
- **Scope boundary:** The existing `player.jamesjennison.net` Webuzo domain and document root, artifact-local
  `.htaccess`, trusted wildcard assignment, backups, origin-only validation, and local documentation. Public DNS,
  Cloudflare settings, Full (Strict), new certificate issuance, master-site content, GitHub Pages, production cutover,
  and GitHub push remain excluded. Per owner direction, this website project does not use Discord milestone notices.
- **Completion gates:** Clean Git state; exact artifact and destination verification; fresh checked Restic snapshot;
  validated header syntax and browser behavior; Webuzo-supported certificate assignment with no generated VirtualHost
  edits; trusted direct-origin TLS; route, 404, ownership, permission, and existing-site health checks; rollback record;
  clean diff and local milestone commit.
- **Completion:** Completed `July 22, 2026 at 4:24:19 PM PDT (UTC−07:00)`. The measured window was 0.41 active hours
  with no user-blocked or separately measured automated-wait interval. Builds, browser checks, backups, and origin
  probes were short and actively monitored.
- **Forecast variance:** 0.59 hours (58.9%) below the 1-hour lower bound. The already verified Webuzo domain,
  compact static artifact, reusable wildcard, supported local SSO/API path, and incremental Restic repository removed
  most of the allowance for access discovery, certificate issuance, or deployment troubleshooting.
- **Evidence:** Artifact digest `3729bc53082966c0473e8fb04da820f731c0f1c3140a5f7d821a8717b7d79bb5`
  matches locally and at the origin; pre-change snapshot `bcefb231` passed a full repository data check and streamed
  restores; post-change snapshot `77f5810b` preserves the working state; the prior release remains available for
  atomic rollback; Webuzo installed the trusted wildcard without a generated VirtualHost edit; direct-origin chain,
  hostname, route, 404, header, responsive-browser, certificate-isolation, apex, `www`, and status checks pass; Player
  DNS remains absent.
- **Forecasting lesson:** Once an isolated static origin and rollback repository are verified, estimate artifact-local
  headers, an incremental backup, atomic promotion, and supported certificate reuse separately from DNS, Full
  (Strict), dedicated issuance, cache policy, and public cutover.

| Started | Ended | Category | Reason or work | Evidence | Hours |
| --- | --- | --- | --- | --- | ---: |
| `July 22, 2026 at 3:59:38 PM PDT (UTC−07:00)` | `July 22, 2026 at 4:24:19 PM PDT (UTC−07:00)` | Active | Verify preconditions, implement and validate headers, back up and redeploy the isolated artifact, assign trusted origin coverage through Webuzo, and document the result. | Owner continuation; branch `codex/player-site-migration`; base `b94eacd`; snapshots `bcefb231` and `77f5810b`; origin validation record | 0.41 h |

```text
Milestone Master M15 time:
Forecast: 1–2 active hours
Counted project time: 0.41 h
Total elapsed time: 0.41 h
User-blocked time excluded: 0.00 h
Forecast variance: 0.59 h below the lower bound (58.9%)
Cumulative counted project time through Milestone Master M15: 1.55 h (master-site program website milestones only; canonical Android cumulative remains Unknown)
```

## Definitions and confidence rules

- **Active execution time** is time spent examining, implementing, testing, documenting, or committing milestone work.
- **Automated wait time** is time spent waiting for builds, tests, installations, deployments, or GitHub Actions when
  that wait actually blocks execution.
- **User-blocked time** is time spent waiting for approval, credentials, external access, decisions, device action, or
  other user intervention.
- **Counted project time** is active execution time plus automated wait time. User-blocked time is excluded unless a
  milestone entry explicitly says otherwise.
- **Total elapsed time** is wall-clock time from actual milestone execution start through completion, including pauses.
- **Confirmed** means recorded start and completion timestamps support the duration.
- **Reconstructed** means reliable repository evidence supports the recorded time boundary.
- **Estimated** means incomplete evidence supports only a range or lower bound.
- **Unknown** means the available evidence does not support a defensible duration.

Displayed totals are rounded to two decimals. Source timestamps and calculation boundaries are preserved below in
12-hour format with the full date, seconds, AM/PM, timezone abbreviation, and UTC offset. Commit timestamps,
pull-request spans, workflow durations, and chat events are evidence points; none is silently treated as continuous
active execution.

## Historical reconstruction method

1. Canonical identity and status come from [ROADMAP.md](ROADMAP.md) and
   [MILESTONE_MIGRATION.md](MILESTONE_MIGRATION.md). GitHub Project #1 independently reports 60 canonical items and
   corroborates the 33 `Complete` states and their evidence checkpoints.
2. A `No later than` or `Earliest evidence` timestamp is the first milestone-specific repository checkpoint found, not
   a recorded actual execution start. Its start-to-completion span is therefore a **minimum observable window**.
3. Completion uses the explicit completion/acceptance commit or merged milestone PR named by the current validation
   documents. When only one checkpoint exists, elapsed duration is Unknown rather than `0.00 h`.
4. GitHub Actions runs corroborate validation, but an asynchronous run does not prove that Codex or a contributor
   waited for its full runtime. Historical automated wait therefore remains Unknown unless a future entry records the
   wait explicitly.
5. Gaps between commits are not classified as active, automated, or user-blocked time without pause evidence.
6. No tags or GitHub Releases existed when this reconstruction was performed. GitHub had 19 PRs; #14–#19 were open
   Dependabot proposals and were not milestone completion records.
7. Historical model labels are recorded only where a milestone document names them. Later planning routes are not
   backdated as the model used for earlier execution.

## Canonical milestone summary

`Unknown` never means zero work. `—` means the milestone is not completed or the field does not yet apply.

| Milestone | Status | Started | Completed | Forecast | Active | Automated Wait | User-Blocked | Counted Project Time | Total Elapsed | Variance | Confidence |
|-----------|--------|---------|-----------|----------|--------|----------------|--------------|----------------------|---------------|----------|------------|
| M01 | Complete | No later than `July 12, 2026 at 9:29:06 PM PDT (UTC−07:00)` | `July 13, 2026 at 2:17:48 PM PDT (UTC−07:00)` | Not recorded | Unknown | Unknown | Unknown | Unknown | ≥16.81 h | Unknown | Estimated |
| M02 | Complete | No later than `July 13, 2026 at 4:39:00 AM PDT (UTC−07:00)` | `July 13, 2026 at 2:48:48 PM PDT (UTC−07:00)` | Not recorded | Unknown | Unknown | Unknown | Unknown | ≥10.16 h | Unknown | Estimated |
| M03 | Complete | No later than `July 13, 2026 at 3:09:06 PM PDT (UTC−07:00)` | `July 13, 2026 at 3:53:17 PM PDT (UTC−07:00)` | Not recorded | Unknown | Unknown | Unknown | Unknown | ≥0.74 h | Unknown | Estimated |
| M04 | Complete | No later than `July 13, 2026 at 4:42:09 PM PDT (UTC−07:00)` | `July 13, 2026 at 5:25:46 PM PDT (UTC−07:00)` | Not recorded | Unknown | Unknown | Unknown | Unknown | ≥0.73 h | Unknown | Estimated |
| M05 | Complete | Unknown | `July 13, 2026 at 6:16:52 PM PDT (UTC−07:00)` | Not recorded | Unknown | Unknown | Unknown | Unknown | Unknown | Unknown | Unknown |
| M06 | Complete | No later than `July 13, 2026 at 6:47:29 PM PDT (UTC−07:00)` | `July 13, 2026 at 7:48:32 PM PDT (UTC−07:00)` | Not recorded | Unknown | Unknown | Unknown | Unknown | ≥1.02 h | Unknown | Estimated |
| M07 | Complete | No later than `July 13, 2026 at 8:08:37 PM PDT (UTC−07:00)` | `July 13, 2026 at 9:27:20 PM PDT (UTC−07:00)` | Not recorded | Unknown | Unknown | Unknown | Unknown | ≥1.31 h | Unknown | Estimated |
| M08 | Complete | No later than `July 13, 2026 at 9:38:35 PM PDT (UTC−07:00)` | `July 13, 2026 at 10:11:34 PM PDT (UTC−07:00)` | Not recorded | Unknown | Unknown | Unknown | Unknown | ≥0.55 h | Unknown | Estimated |
| M09 | Complete | No later than `July 13, 2026 at 10:46:18 PM PDT (UTC−07:00)` | `July 13, 2026 at 11:03:44 PM PDT (UTC−07:00)` | Not recorded | Unknown | Unknown | Unknown | Unknown | ≥0.29 h | Unknown | Estimated |
| M10 | Complete | No later than `July 14, 2026 at 5:48:13 AM PDT (UTC−07:00)` | `July 14, 2026 at 2:53:36 PM PDT (UTC−07:00)` | Not recorded | Unknown | Unknown | Unknown | Unknown | ≥9.09 h | Unknown | Estimated |
| M11 | Complete | Unknown | `July 14, 2026 at 8:41:24 PM PDT (UTC−07:00)` | Not recorded | Unknown | Unknown | Unknown | Unknown | Unknown | Unknown | Unknown |
| M12 | Complete | Unknown | `July 14, 2026 at 8:41:24 PM PDT (UTC−07:00)` | Not recorded | Unknown | Unknown | Unknown | Unknown | Unknown | Unknown | Unknown |
| M13 | Complete | Unknown | `July 14, 2026 at 10:07:42 PM PDT (UTC−07:00)` | 4–8 active h | Unknown | Unknown | Unknown | Unknown | Unknown | Unknown | Unknown |
| M14 | Complete | No later than `July 14, 2026 at 10:30:36 PM PDT (UTC−07:00)` | `July 14, 2026 at 10:32:47 PM PDT (UTC−07:00)` | 2–4 active h | Unknown | Unknown | Unknown | Unknown | ≥0.04 h | Unknown | Estimated |
| M15 | Complete | No later than `July 14, 2026 at 10:57:06 PM PDT (UTC−07:00)` | `July 14, 2026 at 11:05:19 PM PDT (UTC−07:00)` | Not recorded | Unknown | Unknown | Unknown | Unknown | ≥0.14 h | Unknown | Estimated |
| M16 | Complete | No later than `July 14, 2026 at 11:29:27 PM PDT (UTC−07:00)` | `July 14, 2026 at 11:38:09 PM PDT (UTC−07:00)` | Not recorded | Unknown | Unknown | Unknown | Unknown | ≥0.14 h | Unknown | Estimated |
| M17 | Complete | Unknown | `July 14, 2026 at 11:58:33 PM PDT (UTC−07:00)` | 2–4 active h | Unknown | Unknown | Unknown | Unknown | Unknown | Unknown | Unknown |
| M18 | Complete | No later than `July 15, 2026 at 12:14:10 AM PDT (UTC−07:00)` | `July 15, 2026 at 5:53:22 AM PDT (UTC−07:00)` | 4–7 active h | Unknown | Unknown | Unknown | Unknown | ≥5.65 h | Unknown | Estimated |
| M19 | Complete | No later than `July 15, 2026 at 12:37:03 AM PDT (UTC−07:00)` | `July 15, 2026 at 6:07:04 AM PDT (UTC−07:00)` | 4–7 active h | Unknown | Unknown | Unknown | Unknown | ≥5.50 h | Unknown | Estimated |
| M20 | Complete | No later than `July 15, 2026 at 5:09:42 AM PDT (UTC−07:00)` | `July 15, 2026 at 9:57:52 AM PDT (UTC−07:00)` | 6–10 active h | Unknown | Unknown | Unknown | Unknown | ≥4.80 h | Unknown | Estimated |
| M21 | Complete | No later than `July 15, 2026 at 10:22:34 AM PDT (UTC−07:00)` | `July 15, 2026 at 10:25:07 AM PDT (UTC−07:00)` | 4–7 active h | Unknown | Unknown | Unknown | Unknown | ≥0.04 h | Unknown | Estimated |
| M22 | Complete | No later than `July 15, 2026 at 12:09:53 PM PDT (UTC−07:00)` | `July 15, 2026 at 8:59:21 PM PDT (UTC−07:00)` | Not recorded | Unknown | Unknown | Unknown | Unknown | ≥8.82 h | Unknown | Estimated |
| M23 | Complete | Unknown | `July 15, 2026 at 10:32:39 PM PDT (UTC−07:00)` | Not recorded | Unknown | Unknown | Unknown | Unknown | Unknown | Unknown | Unknown |
| M24 | Complete | No later than `July 16, 2026 at 4:46:35 PM PDT (UTC−07:00)` | `July 16, 2026 at 5:01:06 PM PDT (UTC−07:00)` | Not recorded | Unknown | Unknown | Unknown | Unknown | ≥0.24 h | Unknown | Estimated |
| M25 | Complete | No later than `July 16, 2026 at 5:18:02 PM PDT (UTC−07:00)` | `July 16, 2026 at 5:50:41 PM PDT (UTC−07:00)` | Not recorded | Unknown | Unknown | Unknown | Unknown | ≥0.54 h | Unknown | Estimated |
| M26 | Complete | No later than `July 16, 2026 at 6:07:48 PM PDT (UTC−07:00)` | `July 16, 2026 at 6:21:05 PM PDT (UTC−07:00)` | Not recorded | Unknown | Unknown | Unknown | Unknown | ≥0.22 h | Unknown | Estimated |
| M27 | Complete | No later than `July 16, 2026 at 7:05:07 PM PDT (UTC−07:00)` | `July 16, 2026 at 7:09:16 PM PDT (UTC−07:00)` | Not recorded | Unknown | Unknown | Unknown | Unknown | ≥0.07 h | Unknown | Estimated |
| M28 | Complete | No later than `July 15, 2026 at 10:32:07 PM PDT (UTC−07:00)` | `July 18, 2026 at 3:04:15 PM PDT (UTC−07:00)` | Not recorded individually | Unknown | Unknown | Unknown | Unknown | ≥64.54 h | Unknown | Estimated |
| M29 | Waiting externally | No later than `July 15, 2026 at 10:42:33 PM PDT (UTC−07:00)` | — | Original not recorded; current 2–5 active d | Unknown | Unknown | Unknown | Unknown | Ongoing | — | Estimated |
| M30 | Waiting externally | No later than `July 15, 2026 at 10:59:35 PM PDT (UTC−07:00)` | — | Original not recorded; current 0.5–1 active d | Unknown | Unknown | Unknown | Unknown | Ongoing | — | Estimated |
| M31 | Complete | No later than `July 18, 2026 at 5:30:30 PM PDT (UTC−07:00)` | `July 18, 2026 at 5:32:28 PM PDT (UTC−07:00)` | Not recorded | Unknown | Unknown | Unknown | Unknown | ≥0.03 h | Unknown | Estimated |
| M32 | Complete | Unknown | `July 18, 2026 at 6:21:38 PM PDT (UTC−07:00)` | Not recorded | Unknown | Unknown | Unknown | Unknown | Unknown | Unknown | Unknown |
| M33 | Complete | Unknown | `July 18, 2026 at 7:43:32 PM PDT (UTC−07:00)` | Not recorded | Unknown | Unknown | Unknown | Unknown | Unknown | Unknown | Unknown |
| M34 | Complete | No later than `July 15, 2026 at 12:09:53 PM PDT (UTC−07:00)` | `July 19, 2026 at 6:25:48 AM PDT (UTC−07:00)` | Not recorded individually | Unknown | Unknown | Unknown | Unknown | ≥90.27 h | Unknown | Estimated |
| M35 | Complete | No later than `July 15, 2026 at 10:57:12 PM PDT (UTC−07:00)` | `July 19, 2026 at 5:41:32 AM PDT (UTC−07:00)` | Not recorded individually | Unknown | Unknown | Unknown | Unknown | ≥78.74 h | Unknown | Estimated |
| M36 | Waiting externally | — | — | 1–3 active Sol d after trigger | — | — | — | — | — | — | Unknown |
| M37 | Planned after M36 | — | — | 5–10 active d | — | — | — | — | — | — | Unknown |
| M38 | Planned after M37 | — | — | 5–10 active d | — | — | — | — | — | — | Unknown |
| M39 | Planned | — | — | 1–2 active d | — | — | — | — | — | — | Unknown |
| M40 | Planned after M39 | — | — | 3–10 calendar d | — | — | — | — | — | — | Unknown |
| M41 | Planned after M40 | — | — | 0.5–1 active d | — | — | — | — | — | — | Unknown |
| M42 | Planned after M41 | — | — | 3–5 calendar wk | — | — | — | — | — | — | Unknown |
| M43 | Planned after M42 | — | — | 1–2 calendar wk | — | — | — | — | — | — | Unknown |
| M44 | Planned after M43 | — | — | 1–4 calendar wk | — | — | — | — | — | — | Unknown |
| M45 | Planned recurring | — | — | Monthly, quarterly, and release-triggered | — | — | — | — | — | — | Unknown |
| M46 | Planned post-Alpha | — | — | 1–3 active wk per approved slice | — | — | — | — | — | — | Unknown |
| M47 | Deferred | — | — | External repair; then 1–3 active wk | — | — | — | — | — | — | Unknown |
| M48 | Deferred after M47 | — | — | 1–2 active wk | — | — | — | — | — | — | Unknown |
| M49 | Deferred after M48 | — | — | 1–2 active wk | — | — | — | — | — | — | Unknown |
| M50 | Deferred after M49/M38 | — | — | 2–4 active wk | — | — | — | — | — | — | Unknown |
| M51 | Retired | No later than `July 19, 2026 at 7:37:21 AM PDT (UTC−07:00)` | Retired `July 19, 2026 at 8:55:35 AM PDT (UTC−07:00)` | No forecast | Unknown | Unknown | Unknown | Unknown | ≥1.30 h decision window | — | Estimated |
| M52 | Retired | — | Retired `July 19, 2026 at 8:55:35 AM PDT (UTC−07:00)` | No forecast | — | — | — | — | — | — | Unknown |
| M53 | Retired | — | Retired `July 19, 2026 at 8:55:35 AM PDT (UTC−07:00)` | No forecast | — | — | — | — | — | — | Unknown |
| M54 | Retired | — | Retired `July 19, 2026 at 8:55:35 AM PDT (UTC−07:00)` | No forecast | — | — | — | — | — | — | Unknown |
| M55 | Planned research gate | — | — | 2–4 active wk; may be no-go | — | — | — | — | — | — | Unknown |
| M56 | Planned research gate | — | — | 1–3 active wk | — | — | — | — | — | — | Unknown |
| M57 | Planned research gate | — | — | 1–3 active wk | — | — | — | — | — | — | Unknown |
| M58 | Planned authorization gate | — | — | 2–4 active wk; contract time external | — | — | — | — | — | — | Unknown |
| M59 | Planned after M58 | — | — | 3–6 active wk | — | — | — | — | — | — | Unknown |
| M60 | Planned after M59 | — | — | 2–4 active wk | — | — | — | — | — | — | Unknown |

## Historical evidence inventory and GitHub corroboration

The final linked Action is evidence that validation automation ran; its duration is not counted as historical wait
without a contemporaneous record that milestone execution was blocked on it. PR #1 contains the original M01–M23
development series. M24–M27 have focused milestone PRs. M28 and M31–M35 completed through direct commits.

| Milestone | Earliest evidence | Completion/decision evidence | PR | Relevant final Action |
| --- | --- | --- | --- | --- |
| M01 | `252716e` | `e8eb7da` | #1 | [run 29285804400](https://github.com/codeframe78/24Seven.FM-Player/actions/runs/29285804400) |
| M02 | `b92da5a` | `5cdade0` | #1 | [run 29287729689](https://github.com/codeframe78/24Seven.FM-Player/actions/runs/29287729689) |
| M03 | `ce5f0f9` | `4206fee` | #1 | [run 29291330588](https://github.com/codeframe78/24Seven.FM-Player/actions/runs/29291330588) |
| M04 | `f4b9c77` | `0d9e6b3` | #1 | [run 29295894553](https://github.com/codeframe78/24Seven.FM-Player/actions/runs/29295894553) |
| M05 | — | `8133695` | #1 | [run 29298161047](https://github.com/codeframe78/24Seven.FM-Player/actions/runs/29298161047) |
| M06 | `f7ef354` | `3babdf9` | #1 | [run 29301974459](https://github.com/codeframe78/24Seven.FM-Player/actions/runs/29301974459) |
| M07 | `bb416d0` | `a897794` | #1 | [run 29306021007](https://github.com/codeframe78/24Seven.FM-Player/actions/runs/29306021007) |
| M08 | `82119ca` | `f99635d` | #1 | [run 29307897241](https://github.com/codeframe78/24Seven.FM-Player/actions/runs/29307897241) |
| M09 | `e748f27` | `5c50582` | #1 | [run 29310250996](https://github.com/codeframe78/24Seven.FM-Player/actions/runs/29310250996) |
| M10 | `635139d` | `73a0d89` | #1 | [run 29371171780](https://github.com/codeframe78/24Seven.FM-Player/actions/runs/29371171780) |
| M11 | — | `4735f13` | #1 | [run 29387151240](https://github.com/codeframe78/24Seven.FM-Player/actions/runs/29387151240) |
| M12 | — | `4735f13` | #1 | [run 29387151240](https://github.com/codeframe78/24Seven.FM-Player/actions/runs/29387151240) |
| M13 | — | `9ef1f1c` | #1 | No commit-matched run found |
| M14 | `81c2c4e` | `9100f10` | #1 | [run 29391738519](https://github.com/codeframe78/24Seven.FM-Player/actions/runs/29391738519) |
| M15 | `b19d5fe` | `f16f408` | #1 | [run 29393212571](https://github.com/codeframe78/24Seven.FM-Player/actions/runs/29393212571) |
| M16 | `90a7f98` | `d69c76d` | #1 | [run 29394803688](https://github.com/codeframe78/24Seven.FM-Player/actions/runs/29394803688) |
| M17 | — | `02dffa2` | #1 | [run 29395841997](https://github.com/codeframe78/24Seven.FM-Player/actions/runs/29395841997) |
| M18 | `f877adc` | `11f1ee3` | #1 | [run 29416969934](https://github.com/codeframe78/24Seven.FM-Player/actions/runs/29416969934) |
| M19 | `50541ea` | `5e985ae` | #1 | [run 29417887444](https://github.com/codeframe78/24Seven.FM-Player/actions/runs/29417887444) |
| M20 | `5a4bdb7` | `6757680` | #1 | [run 29434576408](https://github.com/codeframe78/24Seven.FM-Player/actions/runs/29434576408) |
| M21 | `ec5fb20` | `1e20eea` | #1 | [run 29436424981](https://github.com/codeframe78/24Seven.FM-Player/actions/runs/29436424981) |
| M22 | `252e3a4` | `44632f1` | #1 | No commit-matched final run found |
| M23 | — | `924a38c` | #1 | [run 29474194397](https://github.com/codeframe78/24Seven.FM-Player/actions/runs/29474194397) |
| M24 | `b713783` | `feb24f7` | [#7](https://github.com/codeframe78/24Seven.FM-Player/pull/7) | [run 29543579299](https://github.com/codeframe78/24Seven.FM-Player/actions/runs/29543579299) |
| M25 | `85fbca3` | `3588c7d` | [#8](https://github.com/codeframe78/24Seven.FM-Player/pull/8) | PR and local validation; no completion-commit run match |
| M26 | `7aedbfc` | `32b7355` | [#9](https://github.com/codeframe78/24Seven.FM-Player/pull/9) | [run 29547213689](https://github.com/codeframe78/24Seven.FM-Player/actions/runs/29547213689) |
| M27 | `d902cbd` | `afd563a` | [#11](https://github.com/codeframe78/24Seven.FM-Player/pull/11) | PR Actions recorded; no merge-commit run match |
| M28 | `c6a9450` | `9abd69f` | — | [run 29662811708](https://github.com/codeframe78/24Seven.FM-Player/actions/runs/29662811708) |
| M29 | `4aa36d9` | Open | — | Multiple current evidence runs; no completion run |
| M30 | `c0e37ae` | Open | — | No completion run |
| M31 | `9e340b9` | `91794bb` | — | [run 29667203553](https://github.com/codeframe78/24Seven.FM-Player/actions/runs/29667203553) |
| M32 | — | `2bc45f7` | — | Later evidence commits/runs exist; no completion-commit run match |
| M33 | — | `572d419` | — | Later evidence commits/runs exist; no completion-commit run match |
| M34 | `252e3a4` | `6ea021f` | #13 contributed TalkBack evidence | Later acceptance sync runs exist; no completion-commit run match |
| M35 | `2e839cc` | `97b732d` | #12 contributed verification evidence | [run 29687462040](https://github.com/codeframe78/24Seven.FM-Player/actions/runs/29687462040) |
| M51 | `cb37b03` | retired by `8b9980f` | — | [run 29693823405](https://github.com/codeframe78/24Seven.FM-Player/actions/runs/29693823405) |

Repository commit links can be formed as `https://github.com/codeframe78/24Seven.FM-Player/commit/<full-sha>`; short
hashes above match the local repository. There were no relevant tags or releases.

## Detailed milestone records

Unless a record overrides it, historical active, automated-wait, and user-blocked intervals are Unknown; counted time
and forecast variance are therefore Unknown. The default variance explanation is that a commit span measures neither
active execution nor blocking wait. The default forecasting lesson is to record authorization/start, validator
begin/end, pauses/resumes, and acceptance in this ledger during execution.

Each record follows the reference ledger's field order. Closely related historical fields remain grouped where the
same incomplete evidence supports all of them; this avoids repeating an unsupported value as though it were measured
independently.

### M01 — Buildable baseline

- **Objective:** Establish a buildable native Kotlin/Compose Android baseline with pinned tooling and verified Windows
  and Ubuntu development paths.
- **Start, completion, and intervals:** Earliest repository evidence `July 12, 2026 at 9:29:06 PM PDT (UTC−07:00)`; completed
  `July 13, 2026 at 2:17:48 PM PDT (UTC−07:00)`. Active, automated-wait, and user-blocked intervals are Unknown.
- **Model, reasoning strength, and original forecast:** Not recorded.
- **Counted project time, total elapsed time, and forecast variance:** Counted project time Unknown; minimum observable elapsed 16.81 h; forecast variance Unknown.
- **Evidence used and confidence classification:** `252716e`, `9184afd`, `e8eb7da`, [m1-validation.md](m1-validation.md), PR #1, and the M01
  Action in the evidence index. **Estimated** because the initial commit is only a start proxy.
- **Explanation of variance and forecasting lessons:** An overnight gap and later revalidation cannot be categorized. Future baseline
  work must open an execution interval before inspection or scaffolding begins.

### M02 — Five-station playback

- **Objective:** Deliver verified five-station Media3 playback, atomic switching, and bounded primary/source fallback.
- **Start, completion, and intervals:** Earliest evidence `July 13, 2026 at 4:39:00 AM PDT (UTC−07:00)`; completed
  `July 13, 2026 at 2:48:48 PM PDT (UTC−07:00)`. Active, automated-wait, and user-blocked intervals are Unknown.
- **Model, reasoning strength, and original forecast:** Not recorded.
- **Counted project time, total elapsed time, and forecast variance:** Counted time Unknown; minimum observable elapsed 10.16 h; variance Unknown.
- **Evidence used and confidence classification:** `b92da5a`, `7c7351d`, `d517713`, `5cdade0`, [m2-validation.md](m2-validation.md), PR #1,
  and the indexed Action. **Estimated**.
- **Explanation of variance and forecasting lessons:** Device/network validation spans cannot be separated from other gaps. Record each
  device test and automated validator as its own interval.

### M03 — Background and system media

- **Objective:** Validate service-owned background playback, MediaSession controls, audio focus, and accessory behavior.
- **Start, completion, and intervals:** Earliest evidence `July 13, 2026 at 3:09:06 PM PDT (UTC−07:00)`; completed
  `July 13, 2026 at 3:53:17 PM PDT (UTC−07:00)`. Historical category intervals are Unknown.
- **Model, reasoning strength, and original forecast:** Not recorded.
- **Counted project time, total elapsed time, and forecast variance:** Counted time Unknown; minimum observable elapsed 0.74 h; variance Unknown.
- **Evidence used and confidence classification:** `ce5f0f9`, `66e401c`, `9263993`, `4206fee`, [m3-validation.md](m3-validation.md), PR #1,
  and the indexed Action. **Estimated**.
- **Explanation of variance and forecasting lessons:** The first commit follows unrecorded validation work. Start timing before the
  first lifecycle or hardware check.

### M04 — Now Playing

- **Objective:** Add and validate live ICY metadata with safe same-station artwork enrichment.
- **Start, completion, and intervals:** Earliest evidence `July 13, 2026 at 4:42:09 PM PDT (UTC−07:00)`; completed
  `July 13, 2026 at 5:25:46 PM PDT (UTC−07:00)`. Historical category intervals are Unknown.
- **Model, reasoning strength, and original forecast:** Not recorded.
- **Counted project time, total elapsed time, and forecast variance:** Counted time Unknown; minimum observable elapsed 0.73 h; variance Unknown.
- **Evidence used and confidence classification:** `f4b9c77`, `0d9e6b3`, later addenda `ae34a26`/`7aced76`,
  [m4-metadata-research.md](m4-metadata-research.md), PR #1, and the indexed Action. **Estimated**.
- **Explanation of variance and forecasting lessons:** Later artwork hardening is a post-completion addendum and is not folded into the
  original window. Track follow-up scope under a named milestone or explicit addendum interval.

### M05 — Native navigation

- **Objective:** Deliver adaptive native destinations and the persistent mini-player.
- **Start, completion, and intervals:** Actual start Unknown; completed `July 13, 2026 at 6:16:52 PM PDT (UTC−07:00)`. All category intervals are
  Unknown.
- **Model, reasoning strength, and original forecast:** Not recorded.
- **Counted project time, total elapsed time, and forecast variance:** Counted and total elapsed time Unknown; variance Unknown.
- **Evidence used and confidence classification:** `8133695`, [m5-validation.md](m5-validation.md), PR #1, and the indexed Action. **Unknown**;
  a single completion checkpoint does not establish duration.
- **Explanation of variance and forecasting lessons:** Never infer zero duration from a one-commit milestone. Record execution start
  before the first UI change.

### M06 — Queue and History

- **Objective:** Implement bounded station-scoped Queue and History observation.
- **Start, completion, and intervals:** Earliest evidence `July 13, 2026 at 6:47:29 PM PDT (UTC−07:00)`; completed
  `July 13, 2026 at 7:48:32 PM PDT (UTC−07:00)`. Category intervals are Unknown.
- **Model, reasoning strength, and original forecast:** Not recorded.
- **Counted project time, total elapsed time, and forecast variance:** Counted time Unknown; minimum observable elapsed 1.02 h; variance Unknown.
- **Evidence used and confidence classification:** `f7ef354`, `3babdf9`, later presentation fixes `9a9ac02`/`dcae5a2`,
  [m6-validation.md](m6-validation.md), PR #1, and the indexed Action. **Estimated**.
- **Explanation of variance and forecasting lessons:** Later fixes are not assumed to extend the original milestone. Log follow-up work
  separately when it changes the accepted gate.

### M07 — Authentication

- **Objective:** Deliver native station login and Android-protected, station-isolated sessions.
- **Start, completion, and intervals:** Earliest evidence `July 13, 2026 at 8:08:37 PM PDT (UTC−07:00)`; completed
  `July 13, 2026 at 9:27:20 PM PDT (UTC−07:00)`. Category intervals are Unknown.
- **Model, reasoning strength, and original forecast:** Not recorded.
- **Counted project time, total elapsed time, and forecast variance:** Counted time Unknown; minimum observable elapsed 1.31 h; variance Unknown.
- **Evidence used and confidence classification:** `bb416d0` through `a897794`, [m7-validation.md](m7-validation.md), PR #1, and the indexed
  Action. **Estimated**.
- **Explanation of variance and forecasting lessons:** The dense commit series is evidence of activity but not a stopwatch. Preserve
  validator and any credential/user-input wait boundaries prospectively.

### M08 — Chat

- **Objective:** Implement native, bounded, station-scoped Chat.
- **Start, completion, and intervals:** Earliest evidence `July 13, 2026 at 9:38:35 PM PDT (UTC−07:00)`; completed
  `July 13, 2026 at 10:11:34 PM PDT (UTC−07:00)`. Category intervals are Unknown.
- **Model, reasoning strength, and original forecast:** Not recorded.
- **Counted project time, total elapsed time, and forecast variance:** Counted time Unknown; minimum observable elapsed 0.55 h; variance Unknown.
- **Evidence used and confidence classification:** `82119ca`, `f99635d`, [m8-validation.md](m8-validation.md), PR #1, and the indexed Action.
  **Estimated**.
- **Explanation of variance and forecasting lessons:** Pre-commit research and validation are absent from the timing record. Start the
  ledger before protocol inspection.

### M09 — Song requests

- **Objective:** Add native search, explicit confirmation, and one-shot song-request submission.
- **Start, completion, and intervals:** Earliest evidence `July 13, 2026 at 10:46:18 PM PDT (UTC−07:00)`; completed
  `July 13, 2026 at 11:03:44 PM PDT (UTC−07:00)`. Category intervals are Unknown.
- **Model, reasoning strength, and original forecast:** Not recorded.
- **Counted project time, total elapsed time, and forecast variance:** Counted time Unknown; minimum observable elapsed 0.29 h; variance Unknown.
- **Evidence used and confidence classification:** `e748f27`, `5c50582`, [m9-validation.md](m9-validation.md), PR #1, and the indexed Action.
  **Estimated**.
- **Explanation of variance and forecasting lessons:** The short window starts at an implementation commit, not actual execution.
  Record discovery, mutation-safety review, and test waits explicitly.

### M10 — Request attribution and messages

- **Objective:** Add Queue attribution and the verified SST optional request-message flow.
- **Start, completion, and intervals:** Earliest evidence `July 14, 2026 at 5:48:13 AM PDT (UTC−07:00)`; completed
  `July 14, 2026 at 2:53:36 PM PDT (UTC−07:00)`. Category intervals are Unknown.
- **Model, reasoning strength, and original forecast:** Not recorded.
- **Counted project time, total elapsed time, and forecast variance:** Counted time Unknown; minimum observable elapsed 9.09 h; variance Unknown.
- **Evidence used and confidence classification:** `635139d` through `73a0d89`, [m10-validation.md](m10-validation.md), PR #1, and the indexed
  Action. **Estimated**.
- **Explanation of variance and forecasting lessons:** The long checkpoint span includes unclassified gaps. Record live one-shot tests
  and recovery decisions without storing protected evidence.

### M11 — Adaptive Alpha UI

- **Objective:** Deliver the responsive Alpha shell, previews, semantics, and explicit exit flow while preserving the
  working native feature contracts.
- **Start, completion, and intervals:** Actual start Unknown; completed `July 14, 2026 at 8:41:24 PM PDT (UTC−07:00)`. Category intervals are
  Unknown.
- **Model, reasoning strength, and original forecast:** Not recorded.
- **Counted project time, total elapsed time, and forecast variance:** Counted and total elapsed time Unknown; variance Unknown.
- **Evidence used and confidence classification:** `4735f13`, [m11-validation.md](m11-validation.md),
  [m11-ui-preservation-plan.md](m11-ui-preservation-plan.md), PR #1, and the indexed Action. **Unknown**.
- **Explanation of variance and forecasting lessons:** M11 and M12 landed together, and no independent start was recorded. Open separate
  intervals when one commit completes multiple milestone gates.

### M12 — Queue-aware request availability

- **Objective:** Add conservative fresh Queue/History eligibility and fail-closed pre-submit checks.
- **Start, completion, and intervals:** Actual start Unknown; completed `July 14, 2026 at 8:41:24 PM PDT (UTC−07:00)`. Category intervals are
  Unknown.
- **Model, reasoning strength, and original forecast:** Not recorded.
- **Counted project time, total elapsed time, and forecast variance:** Counted and total elapsed time Unknown; variance Unknown.
- **Evidence used and confidence classification:** `4735f13`, [m12-validation.md](m12-validation.md), PR #1, and the indexed Action. **Unknown**.
- **Explanation of variance and forecasting lessons:** The shared completion commit does not reveal M12's start or effort share. Track
  concurrent milestone intervals independently.

### M13 — Independent Accounts UX

- **Objective:** Present five visible, independently actionable station-account states.
- **Start, completion, and intervals:** Actual start Unknown; implementation completed `July 14, 2026 at 10:07:42 PM PDT (UTC−07:00)`. Category
  intervals are Unknown.
- **Model, reasoning strength, and original forecast:** Execution model/reasoning not recorded. Original forecast 4–8 active h,
  task complexity level 2, size L.
- **Counted project time, total elapsed time, and forecast variance:** Counted and total elapsed time Unknown; forecast difference and percentage Unknown.
- **Evidence used and confidence classification:** `9ef1f1c`, documentation `fc0a3c0`, [m13-validation.md](m13-validation.md), and PR #1.
  **Unknown** because there is no start checkpoint.
- **Explanation of variance and forecasting lessons:** The forecast cannot be compared with a commit timestamp. Future forecasts must
  be snapshotted beside an actual start interval.

### M14 — Local personalization

- **Objective:** Add device-local startup/default-station preferences without mixing station-owned data.
- **Start, completion, and intervals:** Earliest evidence `July 14, 2026 at 10:30:36 PM PDT (UTC−07:00)`; documentation completed
  `July 14, 2026 at 10:32:47 PM PDT (UTC−07:00)`. Category intervals are Unknown.
- **Model, reasoning strength, and original forecast:** Execution model/reasoning not recorded. Original forecast 2–4 active h,
  task complexity level 2, size M.
- **Counted project time, total elapsed time, and forecast variance:** Counted time Unknown; minimum observable elapsed 0.04 h; forecast difference/percentage Unknown.
- **Evidence used and confidence classification:** `81c2c4e`, `9100f10`, [m14-validation.md](m14-validation.md), PR #1, and the indexed Action.
  **Estimated**.
- **Explanation of variance and forecasting lessons:** A two-minute commit window cannot be compared with an active-hours forecast;
  it demonstrates that the execution start predates the proxy.

### M15 — Request activity and membership

- **Objective:** Add SST request history, cooldown/readiness, and explicit membership evidence.
- **Start, completion, and intervals:** Earliest evidence `July 14, 2026 at 10:57:06 PM PDT (UTC−07:00)`; completed
  `July 14, 2026 at 11:05:19 PM PDT (UTC−07:00)`. Category intervals are Unknown.
- **Model, reasoning strength, and original forecast:** Not recorded.
- **Counted project time, total elapsed time, and forecast variance:** Counted time Unknown; minimum observable elapsed 0.14 h; variance Unknown.
- **Evidence used and confidence classification:** `b19d5fe`, `f16f408`, [m15-validation.md](m15-validation.md), PR #1, and the indexed Action.
  **Estimated**.
- **Explanation of variance and forecasting lessons:** The implementation commit is a late lower-bound proxy. Record protocol research
  and authenticated validation intervals prospectively.

### M16 — Secondary content access

- **Objective:** Add allowlisted same-station HTTPS Custom Tab access while retaining the no-WebView boundary.
- **Start, completion, and intervals:** Earliest evidence `July 14, 2026 at 11:29:27 PM PDT (UTC−07:00)`; completed
  `July 14, 2026 at 11:38:09 PM PDT (UTC−07:00)`. Category intervals are Unknown.
- **Model, reasoning strength, and original forecast:** Not recorded.
- **Counted project time, total elapsed time, and forecast variance:** Counted time Unknown; minimum observable elapsed 0.14 h; variance Unknown.
- **Evidence used and confidence classification:** `90a7f98`, `d69c76d`, [m16-validation.md](m16-validation.md), PR #1, and the indexed Action.
  **Estimated**.
- **Explanation of variance and forecasting lessons:** M31 later narrowed the shipping catalog but did not erase M16. Track later policy
  corrections under their own milestone time.

### M17 — StreamingSoundtracks.com certification

- **Objective:** Certify public, authenticated, VIP, request, Favorites, and Chat behavior for SST.
- **Start, completion, and intervals:** Actual start Unknown; certified `July 14, 2026 at 11:58:33 PM PDT (UTC−07:00)`. Category intervals are
  Unknown.
- **Model, reasoning strength, and original forecast:** Execution model/reasoning not recorded. Original forecast 2–4 active h,
  task complexity level 2, size S.
- **Counted project time, total elapsed time, and forecast variance:** Counted and total elapsed time Unknown; forecast difference/percentage Unknown.
- **Evidence used and confidence classification:** `02dffa2`, [m18-sst-certification.md](m18-sst-certification.md), PR #1, and the indexed Action.
  **Unknown** because only certification completion is timestamped.
- **Explanation of variance and forecasting lessons:** Evidence-only certification can still consume live-test time; record the
  certification start even when no code change is expected.

### M18 — 1980s.FM certification

- **Objective:** Certify independent public, authenticated, and station-specific 1980s.FM behavior.
- **Start, completion, and intervals:** Earliest evidence `July 15, 2026 at 12:14:10 AM PDT (UTC−07:00)`; completed
  `July 15, 2026 at 5:53:22 AM PDT (UTC−07:00)`. Category intervals are Unknown.
- **Model, reasoning strength, and original forecast:** Execution model/reasoning not recorded. Original forecast 4–7 active h,
  task complexity level 2, size M.
- **Counted project time, total elapsed time, and forecast variance:** Counted time Unknown; minimum observable elapsed 5.65 h; forecast difference/percentage Unknown.
- **Evidence used and confidence classification:** `f877adc`, `11f1ee3`, [m19-1980s-certification.md](m19-1980s-certification.md), PR #1, and
  the indexed Action. **Estimated**.
- **Explanation of variance and forecasting lessons:** The observable wall-clock window falls inside the active forecast but cannot be
  treated as active time. Record user-entered CAPTCHA/account pauses separately.

### M19 — Adagio.FM certification

- **Objective:** Certify classical metadata and independent authenticated Adagio.FM workflows.
- **Start, completion, and intervals:** Earliest evidence `July 15, 2026 at 12:37:03 AM PDT (UTC−07:00)`; completed
  `July 15, 2026 at 6:07:04 AM PDT (UTC−07:00)`. Category intervals are Unknown.
- **Model, reasoning strength, and original forecast:** Execution model/reasoning not recorded. Original forecast 4–7 active h,
  task complexity level 2, size M.
- **Counted project time, total elapsed time, and forecast variance:** Counted time Unknown; minimum observable elapsed 5.50 h; forecast difference/percentage Unknown.
- **Evidence used and confidence classification:** `50541ea`, `5e985ae`, [m20-adagio-certification.md](m20-adagio-certification.md), PR #1, and
  the indexed Action. **Estimated**.
- **Explanation of variance and forecasting lessons:** As with M18, a matching wall-clock range is not an actual-time measurement.
  Explicitly time live account and device work in future certifications.

### M20 — Death.FM certification

- **Objective:** Certify compact Queue, sparse metadata, RIP boundaries, and independent authenticated Death.FM flows.
- **Start, completion, and intervals:** Earliest evidence `July 15, 2026 at 5:09:42 AM PDT (UTC−07:00)`; completed
  `July 15, 2026 at 9:57:52 AM PDT (UTC−07:00)`. Category intervals are Unknown.
- **Model, reasoning strength, and original forecast:** Execution model/reasoning not recorded. Original forecast 6–10 active h,
  task complexity level 2, size L.
- **Counted project time, total elapsed time, and forecast variance:** Counted time Unknown; minimum observable elapsed 4.80 h; forecast difference/percentage Unknown.
- **Evidence used and confidence classification:** `5a4bdb7`, `6757680`, [m21-death-certification.md](m21-death-certification.md), PR #1, and
  the indexed Action. **Estimated**.
- **Explanation of variance and forecasting lessons:** The minimum window is shorter than the active forecast because work before the
  first evidence commit is missing; it is not proof of an underestimate or overestimate.

### M21 — Entranced.FM certification

- **Objective:** Certify extended Queue, legacy ICY punctuation, and authenticated Entranced.FM workflows.
- **Start, completion, and intervals:** Earliest evidence `July 15, 2026 at 10:22:34 AM PDT (UTC−07:00)`; completed
  `July 15, 2026 at 10:25:07 AM PDT (UTC−07:00)`. Category intervals are Unknown.
- **Model, reasoning strength, and original forecast:** Execution model/reasoning not recorded. Original forecast 4–7 active h,
  task complexity level 2, size M.
- **Counted project time, total elapsed time, and forecast variance:** Counted time Unknown; minimum observable elapsed 0.04 h; forecast difference/percentage Unknown.
- **Evidence used and confidence classification:** `ec5fb20`, `1e20eea`, [m22-entranced-certification.md](m22-entranced-certification.md), PR #1,
  and the indexed Action. **Estimated**.
- **Explanation of variance and forecasting lessons:** The very short final-commit window proves the start proxy is incomplete. Start
  the ledger before live relay and authenticated checks.

### M22 — Android 16 and API 36 readiness

- **Objective:** Validate API 36 targeting, edge-to-edge and predictive Back behavior, and platform regressions.
- **Start, completion, and intervals:** Earliest relevant device/layout evidence `July 15, 2026 at 12:09:53 PM PDT (UTC−07:00)`; completed
  `July 15, 2026 at 8:59:21 PM PDT (UTC−07:00)`. Category intervals are Unknown.
- **Model, reasoning strength, and original forecast:** Not recorded individually.
- **Counted project time, total elapsed time, and forecast variance:** Counted time Unknown; minimum observable elapsed 8.82 h; variance Unknown.
- **Evidence used and confidence classification:** `252e3a4`, `44632f1`, [m23-api36-readiness.md](m23-api36-readiness.md), PR #1, and related
  Actions. **Estimated**.
- **Explanation of variance and forecasting lessons:** Device work overlaps M34's later acceptance program. Concurrent milestones need
  separate interval attribution instead of duplicating the same active minutes.

### M23 — Adaptive launcher and store polish

- **Objective:** Deliver and validate legacy, adaptive, and monochrome launcher resources.
- **Start, completion, and intervals:** Actual start Unknown; completed `July 15, 2026 at 10:32:39 PM PDT (UTC−07:00)`. Category intervals are
  Unknown.
- **Model, reasoning strength, and original forecast:** Not recorded individually.
- **Counted project time, total elapsed time, and forecast variance:** Counted and total elapsed time Unknown; variance Unknown.
- **Evidence used and confidence classification:** `924a38c`, [m23-launcher-polish.md](m23-launcher-polish.md), PR #1, and the indexed Action.
  **Unknown**.
- **Explanation of variance and forecasting lessons:** Image/resource production time is absent from Git. Open the timer before asset
  inspection or generation.

### M24 — Sleep Timer

- **Objective:** Add a bounded, service-owned timer with restoration, adjustment, cancellation, and deterministic expiry.
- **Start, completion, and intervals:** Earliest evidence `July 16, 2026 at 4:46:35 PM PDT (UTC−07:00)`; merged/completed
  `July 16, 2026 at 5:01:06 PM PDT (UTC−07:00)`. Category intervals are Unknown.
- **Model, reasoning strength, and original forecast:** Not recorded.
- **Counted project time, total elapsed time, and forecast variance:** Counted time Unknown; minimum observable elapsed 0.24 h; variance Unknown.
- **Evidence used and confidence classification:** `b713783`, `1a0516e`, merge `feb24f7`, [m24-sleep-timer-validation.md](m24-sleep-timer-validation.md),
  PR #7, and the indexed Action. **Estimated**.
- **Explanation of variance and forecasting lessons:** The first commit already contains implementation; PR duration is not total
  execution. Begin measurement before code examination and record local Gradle/device waits.

### M25 — System Audio-Output Selection

- **Objective:** Add Android-managed output selection and accurate audio-policy route state without adding Cast.
- **Start, completion, and intervals:** Earliest evidence `July 16, 2026 at 5:18:02 PM PDT (UTC−07:00)`; merged/completed
  `July 16, 2026 at 5:50:41 PM PDT (UTC−07:00)`. Category intervals are Unknown.
- **Model, reasoning strength, and original forecast:** Not recorded.
- **Counted project time, total elapsed time, and forecast variance:** Counted time Unknown; minimum observable elapsed 0.54 h; variance Unknown.
- **Evidence used and confidence classification:** `85fbca3`, `183ccc0`, `68e5ab8`, merge `3588c7d`,
  [m25-audio-output-validation.md](m25-audio-output-validation.md), and PR #8. **Estimated**.
- **Explanation of variance and forecasting lessons:** The physical correction is visible in commits, but device handoff/test wait is
  unmeasured. Time physical validation separately from implementation.

### M26 — In-App Diagnostics

- **Objective:** Add a privacy-safe, user-reviewed, fixed-allowlist diagnostics snapshot with explicit copy/share.
- **Start, completion, and intervals:** Earliest evidence `July 16, 2026 at 6:07:48 PM PDT (UTC−07:00)`; merged/completed
  `July 16, 2026 at 6:21:05 PM PDT (UTC−07:00)`. Category intervals are Unknown.
- **Model, reasoning strength, and original forecast:** Not recorded.
- **Counted project time, total elapsed time, and forecast variance:** Counted time Unknown; minimum observable elapsed 0.22 h; variance Unknown.
- **Evidence used and confidence classification:** `7aedbfc`, `3aa0b74`, merge `32b7355`, [m26-diagnostics-validation.md](m26-diagnostics-validation.md),
  PR #9, and the indexed Action. **Estimated**.
- **Explanation of variance and forecasting lessons:** Implementation predates the start proxy. Record privacy review as active work and
  Jekyll/Gradle/device runs as automated waits when actually awaited.

### M27 — Local Chat-Mention Notifications

- **Objective:** Add opt-in exact-name notifications from the actively observed Chat feed without claiming closed-app push.
- **Start, completion, and intervals:** Earliest evidence `July 16, 2026 at 7:05:07 PM PDT (UTC−07:00)`; merged/completed
  `July 16, 2026 at 7:09:16 PM PDT (UTC−07:00)`. Category intervals are Unknown.
- **Model, reasoning strength, and original forecast:** Not recorded.
- **Counted project time, total elapsed time, and forecast variance:** Counted time Unknown; minimum observable elapsed 0.07 h; variance Unknown.
- **Evidence used and confidence classification:** `d902cbd`, merge `afd563a`, [m27-community-notification-validation.md](m27-community-notification-validation.md),
  and PR #11. **Estimated**.
- **Explanation of variance and forecasting lessons:** The four-minute commit-to-merge window excludes implementation. A focused PR is
  useful evidence but not a start timer.

### M28 — UGC Safety and Ongoing Moderation

- **Objective:** Add Terms/age/reveal safeguards, report and local block controls, a fixed-recipient user-reviewed email
  handoff, physical composer validation, and an owner-confirmed one-shot receipt check.
- **Start, completion, and intervals:** Earliest implementation evidence `July 15, 2026 at 10:32:07 PM PDT (UTC−07:00)`; completed
  `July 18, 2026 at 3:04:15 PM PDT (UTC−07:00)`. Active, automated-wait, and user-blocked intervals are Unknown; owner input is documented
  but its exact pause/resume boundary is not.
- **Model, reasoning strength, and original forecast:** Execution model/reasoning not recorded. The later M28–M35 umbrella estimate
  was 3–6 focused days plus owner/station input; no original individual M28 forecast was retained.
- **Counted project time, total elapsed time, and forecast variance:** Counted time Unknown; minimum observable elapsed 64.54 h; individual forecast variance Unknown.
- **Evidence used and confidence classification:** `c6a9450`, `465cb4d`, `9abd69f`, [m23-ugc-safety-research.md](m23-ugc-safety-research.md),
  [m23-ugc-safety-validation.md](m23-ugc-safety-validation.md), and the indexed Action. **Estimated**.
- **Explanation of variance and forecasting lessons:** The multi-day wall-clock window includes unmeasured owner/manual gates. Future
  externally assisted milestones must log the user-blocked pause when the request is sent and resume when evidence arrives.

### M29 — Play Declarations, Privacy, and Data Safety

- **Objective:** Reconcile the exact artifact, reviewer access, audience/content/UGC classification, privacy/Data Safety,
  station retention/deletion/processor/IP facts, account boundaries, and media-playback evidence.
- **Start, completion, and intervals:** Earliest Play-declaration evidence `July 15, 2026 at 10:42:33 PM PDT (UTC−07:00)`. Not completed. The
  milestone is waiting externally; exact historical active intervals and the start of the user-blocked pause are Unknown.
- **Model, reasoning strength, and original forecast:** Original execution model/reasoning and forecast were not recorded at start.
  Current routing, added `July 19, 2026 at 6:17:38 AM PDT (UTC−07:00)`, is Sol High with 2–5 active days after all named facts/candidate exist.
- **Counted project time, total elapsed time, and forecast variance:** Active, wait, blocked, counted, and total elapsed durations remain Unknown/ongoing; variance is
  not calculated before completion.
- **Evidence used and confidence classification:** `4aa36d9`, `0fa5040`, `59d6096`, `849b5a0`, `93e8110`, `d3467cf`,
  [m23-play-declaration-packet.md](m23-play-declaration-packet.md), and [m23-data-safety.md](m23-data-safety.md).
  **Estimated** start proxy.
- **Explanation of variance and forecasting lessons:** Do not backdate the July 19 forecast as original. On resume, record a real resume
  timestamp and retain the current external wait as Unknown rather than inventing a duration.

### M30 — Brand, Content, and Distribution Rights

- **Objective:** Obtain and privately retain written authorization for naming, branding, station assets/metadata,
  streams, screenshots, and Google Play testing/distribution.
- **Start, completion, and intervals:** Earliest rights-response packet evidence `July 15, 2026 at 10:59:35 PM PDT (UTC−07:00)`. Not completed.
  The milestone is waiting externally; exact active intervals and the user-blocked pause start are Unknown.
- **Model, reasoning strength, and original forecast:** Original model/reasoning and forecast were not recorded at start. Current
  routing, added July 19, is Sol High with 0.5–1 active day after written authorization arrives.
- **Counted project time, total elapsed time, and forecast variance:** Active, wait, blocked, counted, and total elapsed durations remain Unknown/ongoing; variance is
  not calculated before completion.
- **Evidence used and confidence classification:** `c0e37ae`, [m23-owner-response-packet.md](m23-owner-response-packet.md), roadmap `e249071`,
  and forecast `f971a9e`. **Estimated** start proxy.
- **Explanation of variance and forecasting lessons:** External response time must remain separate from reconciliation work. Record the
  actual resume timestamp when authorization arrives.

### M31 — Payments, External Links, and Account Lifecycle Compliance

- **Objective:** Establish the Contact-only Alpha boundary and remove unapproved membership purchase, registration,
  account-management, and direct privacy browser routes.
- **Start, completion, and intervals:** Earliest implementation evidence `July 18, 2026 at 5:30:30 PM PDT (UTC−07:00)`; completed
  `July 18, 2026 at 5:32:28 PM PDT (UTC−07:00)`. Category intervals are Unknown.
- **Model, reasoning strength, and original forecast:** Sol Extra High policy/architecture audit, Terra-bounded implementation, and
  Sol High acceptance are recorded; original hours forecast was not recorded.
- **Counted project time, total elapsed time, and forecast variance:** Counted time Unknown; minimum observable elapsed 0.03 h; variance Unknown.
- **Evidence used and confidence classification:** `9e340b9`, `91794bb`,
  [m31-payments-account-lifecycle-validation.md](m31-payments-account-lifecycle-validation.md), and the indexed Action.
  **Estimated**.
- **Explanation of variance and forecasting lessons:** The two-minute commit window omits the audit/implementation before commit. Model
  routing is not time evidence; future audits must open intervals for review, implementation, and acceptance.

### M32 — Session, Controller, Network, and Supply-Chain Security

- **Objective:** Harden MediaSession authority, protected station sessions, redirects, canonical IDs, and build integrity,
  with adversarial physical-device evidence.
- **Start, completion, and intervals:** Actual start Unknown; completed `July 18, 2026 at 6:21:38 PM PDT (UTC−07:00)`. Category intervals are Unknown.
- **Model, reasoning strength, and original forecast:** Exact execution model/reasoning and original forecast not recorded.
- **Counted project time, total elapsed time, and forecast variance:** Counted and total elapsed time Unknown; variance Unknown.
- **Evidence used and confidence classification:** `2bc45f7`, follow-up evidence `1c50ca0`/`cec5d5e`,
  [m32-security-validation.md](m32-security-validation.md), and associated Actions. **Unknown**.
- **Explanation of variance and forecasting lessons:** A security milestone with one implementation checkpoint cannot be timed from Git.
  Separately time threat review, code, adversarial tests, and acceptance.

### M33 — Request Transaction Integrity

- **Objective:** Bind each one-shot request to fresh station, account, Queue, readiness, and track identity with bounded
  duplicate suppression.
- **Start, completion, and intervals:** Actual start Unknown; completed `July 18, 2026 at 7:43:32 PM PDT (UTC−07:00)`. Category intervals are Unknown.
- **Model, reasoning strength, and original forecast:** Sol acceptance is recorded; reasoning strength and original forecast are not.
- **Counted project time, total elapsed time, and forecast variance:** Counted and total elapsed time Unknown; variance Unknown.
- **Evidence used and confidence classification:** `572d419`, documentation `b7e10b6`,
  [m33-request-transaction-validation.md](m33-request-transaction-validation.md), and associated Actions. **Unknown**.
- **Explanation of variance and forecasting lessons:** The acceptance document proves outcome, not start. Record architecture review,
  implementation, Gradle, device, and acceptance intervals independently.

### M34 — Device and Accessibility Acceptance

- **Objective:** Accept adaptive window/foldable, physical hinge, human TalkBack, Voice Access, keyboard/pointer, large
  text, network recovery, and current-device evidence.
- **Start, completion, and intervals:** Earliest relevant evidence `July 15, 2026 at 12:09:53 PM PDT (UTC−07:00)`; completed
  `July 19, 2026 at 6:25:48 AM PDT (UTC−07:00)`. Exact active, automated-wait, and user-blocked intervals are Unknown; human/physical
  checkpoints are documented without full pause/resume timing.
- **Model, reasoning strength, and original forecast:** Sol High acceptance is recorded; original individual forecast was not.
- **Counted project time, total elapsed time, and forecast variance:** Counted time Unknown; minimum observable elapsed 90.27 h; variance Unknown.
- **Evidence used and confidence classification:** `252e3a4` through `6ea021f`, PR #13, [m23-device-compatibility.md](m23-device-compatibility.md),
  and [m23-accessibility-validation.md](m23-accessibility-validation.md). **Estimated**.
- **Explanation of variance and forecasting lessons:** This is the longest observable window because it aggregates many devices and
  human checkpoints, not because 90.27 active hours were measured. Log each matrix campaign and manual gate separately.

### M35 — Release Signing and Console Eligibility

- **Objective:** Prove current-head protected signing, exact upload identity, local clean/update lineage, package
  registration, eligible version code, and a repeatable signed build.
- **Start, completion, and intervals:** Earliest focused audit evidence `July 15, 2026 at 10:57:12 PM PDT (UTC−07:00)`; completed
  `July 19, 2026 at 5:41:32 AM PDT (UTC−07:00)`. Active, automated-wait, and user-blocked intervals are Unknown; owner confirmation and
  protected build checkpoints lack complete pause boundaries.
- **Model, reasoning strength, and original forecast:** Sol acceptance is recorded; reasoning strength and original individual
  forecast were not.
- **Counted project time, total elapsed time, and forecast variance:** Counted time Unknown; minimum observable elapsed 78.74 h; variance Unknown.
- **Evidence used and confidence classification:** `2e839cc`, `89795ef`, `3b775b7`, PR #12, `55413bd`, `97b732d`,
  [m23-release-candidate-audit.md](m23-release-candidate-audit.md), and the indexed Action. **Estimated**.
- **Explanation of variance and forecasting lessons:** Protected signing and owner/Console checks expanded calendar time, but their
  exact wait is unknown. Future signing milestones must log protected-build start/end and owner-blocked intervals.

### M36 — Notification Event-Source Authorization

- **Objective:** Accept an authorized station event source, webhook, or privacy-compatible relay contract covering
  ownership, payloads, authentication, retention/deletion, abuse, outage behavior, and station support.
- **Start, completion, and intervals:** Not started; no start, completion, active, automated-wait, or user-blocked interval is open.
- **Model, reasoning strength, and original forecast:** Current planned route Sol High; 1–3 active Sol days after an accountable operator
  supplies the triggering proposal. The original forecast will be snapshotted when authorization starts execution.
- **Time, variance, evidence, confidence, and forecasting lessons:** No actual time or variance yet. Roadmap `e249071` and forecast `f971a9e`; no
  PR, release, tag, or workflow. **Unknown** until start. Record the external trigger and actual execution start separately.

### M37 — Secure Closed-App Notification Delivery

- **Objective:** Implement minimal opt-in, station/token-isolated event delivery with duplicate, sign-out, reinstall,
  and protected-session safeguards and no perpetual polling.
- **Start, completion, and intervals:** Not started; all actual intervals and totals are not applicable.
- **Model, reasoning strength, and original forecast:** Terra High, escalating unresolved delivery/privacy choices to Sol High; current
  forecast 5–10 active days after M36 and a test environment are accepted.
- **Time, variance, evidence, confidence, and forecasting lessons:** No actual variance. Roadmap `e249071`, forecast `f971a9e`; no related delivery
  commit/PR/release/workflow. **Unknown**. Snapshot this range as original only when authorized.

### M38 — Notification Lifecycle and Privacy Certification

- **Objective:** Certify foreground/background/closed/idle/reboot/network behavior, latency, battery/traffic, payload
  privacy, deep-link safety, and all five station boundaries.
- **Start, completion, and intervals:** Not started; all actual intervals and totals are not applicable.
- **Model, reasoning strength, and original forecast:** Terra High execution followed by Sol High acceptance; current forecast 5–10 active
  days after M37 is stable.
- **Time, variance, evidence, confidence, and forecasting lessons:** No actual variance. Roadmap `e249071`, forecast `f971a9e`; no implementation
  evidence yet. **Unknown**. Time device campaigns and automated suites as separate intervals.

### M39 — Alpha Candidate and Documentation Freeze

- **Objective:** Freeze exact signed artifacts, version, signer, dependencies, permissions, listing, release notes,
  roadmap/testing, and all release-critical findings.
- **Start, completion, and intervals:** Not started; all actual intervals and totals are not applicable.
- **Model, reasoning strength, and original forecast:** Sol High; current forecast 1–2 active days after M29, M30, and M36–M38 complete.
- **Time, variance, evidence, confidence, and forecasting lessons:** No actual variance. Roadmap `e249071`, forecast `f971a9e`; no candidate-finish
  commit/PR/release/workflow. **Unknown**. Record the exact freeze authorization as start, not prior planning.

### M40 — Play Alpha Delivery and Pre-launch Remediation

- **Objective:** Upload the M39 artifact, inspect Play splits, validate Play install/update, complete reviewer access,
  and resolve pre-review/pre-launch findings.
- **Start, completion, and intervals:** Not started; all actual intervals and totals are not applicable.
- **Model, reasoning strength, and original forecast:** Terra High, with Sol High release decisions; current forecast 3–10 calendar days
  after M39, with extra time only for actual findings.
- **Time, variance, evidence, confidence, and forecasting lessons:** No actual variance. Roadmap `e249071`, forecast `f971a9e`; no Play workflow,
  tag, or release exists. **Unknown**. Separate Play processing as automated/external wait from remediation work.

### M41 — Explicit Alpha Publication

- **Objective:** Publish only after explicit owner authorization, then verify availability, instructions, support, and
  rollback records.
- **Start, completion, and intervals:** Not started; all actual intervals and totals are not applicable.
- **Model, reasoning strength, and original forecast:** Sol High; current forecast 0.5–1 active day after M40 and explicit authorization.
- **Time, variance, evidence, confidence, and forecasting lessons:** No actual variance. Roadmap `e249071`, forecast `f971a9e`; no publication,
  release, tag, PR, or workflow. **Unknown**. Authorization wait must be user-blocked and excluded from counted time.

### M42 — Closed-Test Operations and Stabilization

- **Objective:** Maintain qualifying tester continuity, collect structured feedback, fix/retest findings, review Play
  health, and satisfy campaign exit criteria.
- **Start, completion, and intervals:** Not started; all actual intervals and totals are not applicable.
- **Model, reasoning strength, and original forecast:** Terra Medium with Sol High checkpoints; current forecast 3–5 calendar weeks,
  including the mandatory continuity window and a feedback/fix buffer.
- **Time, variance, evidence, confidence, and forecasting lessons:** No actual variance. Roadmap `e249071`, forecast `f971a9e`; no campaign record.
  **Unknown**. Separate active remediation, Play/test automation, and tester/user-blocked calendar time.

### M43 — Production Access and Policy Approval

- **Objective:** Submit the production-access questionnaire with testing, feedback, change, value, policy, and readiness
  evidence and resolve the Play response.
- **Start, completion, and intervals:** Not started; all actual intervals and totals are not applicable.
- **Model, reasoning strength, and original forecast:** Sol Extra High; current forecast 1–2 calendar weeks after M42 evidence is complete.
- **Time, variance, evidence, confidence, and forecasting lessons:** No actual variance. Roadmap `e249071`, forecast `f971a9e`; no application or
  workflow record. **Unknown**. Track Play review separately from active response work.

### M44 — Production Release and Staged Rollout

- **Objective:** Run an explicitly authorized staged production rollout with health thresholds, pause/rollback,
  release notes, and update validation.
- **Start, completion, and intervals:** Not started; all actual intervals and totals are not applicable.
- **Model, reasoning strength, and original forecast:** Sol Extra High; current forecast 1–4 calendar weeks after production access and
  rollout approval.
- **Time, variance, evidence, confidence, and forecasting lessons:** No actual variance. Roadmap `e249071`, forecast `f971a9e`; no production release,
  tag, PR, or workflow. **Unknown**. Count monitoring work and waits in their actual categories.

### M45 — Operational Reliability and Recertification

- **Objective:** Maintain Play health/support, privacy handling, outage/contract-drift procedures, rights/moderation,
  target/API/dependencies, and signing-key recovery.
- **Start, completion, and intervals:** Not started as a recurring program; no actual interval is open.
- **Model, reasoning strength, and original forecast:** Terra Medium for routine work and Sol High for material policy/security; current
  cadence is monthly light, quarterly recertification, and release-triggered review.
- **Time, variance, evidence, confidence, and forecasting lessons:** No actual variance. Roadmap `e249071`, forecast `f971a9e`; no recurring report.
  **Unknown**. Each review cycle should be a dated sub-entry so recurring time is not one endless elapsed interval.

### M46 — Architecture Sustainability

- **Objective:** Address measured architecture/performance hotspots in approved slices while preserving immutable state,
  repository contracts, and native boundaries.
- **Start, completion, and intervals:** Not started; all actual intervals and totals are not applicable.
- **Model, reasoning strength, and original forecast:** Terra Medium, with Sol High for major refactors; current forecast 1–3 active weeks
  per approved slice after Alpha evidence identifies a hotspot.
- **Time, variance, evidence, confidence, and forecasting lessons:** No actual variance. Roadmap `e249071`, forecast `f971a9e`; no slice evidence.
  **Unknown**. Forecast and time every approved slice separately.

### M47 — Private Messages Server Repair and Protocol Certification

- **Objective:** Wait for site-owner delivery repair, then certify limits, station isolation, error/indeterminate behavior,
  and operator-confirmed production behavior.
- **Start, completion, and intervals:** Not started; no repair-trigger or actual interval is recorded.
- **Model, reasoning strength, and original forecast:** Sol Extra High; no calendar forecast before external repair, then 1–3 active weeks.
- **Time, variance, evidence, confidence, and forecasting lessons:** No actual variance. Roadmap `e249071`, forecast `f971a9e`; no repair evidence.
  **Unknown**. Do not start elapsed time until the repaired production-verification trigger is accepted.

### M48 — Native Private Message Reading

- **Objective:** Add bounded protected Inbox/Sent models and immutable native UI with complete states and privacy tests.
- **Start, completion, and intervals:** Not started; all actual intervals and totals are not applicable.
- **Model, reasoning strength, and original forecast:** Terra High; current forecast 1–2 active weeks after M47.
- **Time, variance, evidence, confidence, and forecasting lessons:** No actual variance. Roadmap `e249071`, forecast `f971a9e`; no implementation.
  **Unknown**. Snapshot forecast/model when M47 closes and M48 is authorized.

### M49 — Private Message Compose, Reply, and Send

- **Objective:** Add explicit preview/confirmation, one-shot mutation, limits, CSRF/session safety, and indeterminate
  delivery recovery.
- **Start, completion, and intervals:** Not started; all actual intervals and totals are not applicable.
- **Model, reasoning strength, and original forecast:** Terra High; current forecast 1–2 active weeks after M48 and explicit mutation authority.
- **Time, variance, evidence, confidence, and forecasting lessons:** No actual variance. Roadmap `e249071`, forecast `f971a9e`; no mutation evidence.
  **Unknown**. User/owner authorization waits must be recorded separately.

### M50 — Private Message Five-Station and Notification Certification

- **Objective:** Certify independent station behavior and optional privacy-minimized delivery through the authorized
  notification architecture.
- **Start, completion, and intervals:** Not started; all actual intervals and totals are not applicable.
- **Model, reasoning strength, and original forecast:** Sol Extra High; current forecast 2–4 active weeks after M49 and M38.
- **Time, variance, evidence, confidence, and forecasting lessons:** No actual variance. Roadmap `e249071`, forecast `f971a9e`; no certification.
  **Unknown**. Time each station/device campaign rather than relying on one final commit.

### M51 — Verified Forum Links

- **Objective:** Historically, verify Forum links and browser fallback; the project subsequently retired all Forum access
  from Player scope.
- **Start, completion, and intervals:** Earliest focused blocker evidence `July 19, 2026 at 7:37:21 AM PDT (UTC−07:00)`; retirement decision recorded
  `July 19, 2026 at 8:55:35 AM PDT (UTC−07:00)`. Category intervals are Unknown.
- **Model, reasoning strength, and original forecast:** Not recorded; no implementation forecast remains after retirement.
- **Time, variance, evidence, confidence, and forecasting lessons:** Counted time Unknown; minimum observable research-to-decision window 1.30 h;
  forecast variance not applicable. `cb37b03`, `8b9980f`, [m51-forum-links-research.md](m51-forum-links-research.md),
  and the indexed Action. **Estimated**.
- **Explanation of variance and forecasting lessons:** This is a retired decision window, not a completed objective. Timeboxed research
  that ends in no-go/retirement should record its start and decision acceptance like any other milestone.

### M52 — Native Forum Read-Only Foundation

- **Objective:** Former native Forum retrieval/rendering scope; permanently retired.
- **Start, completion, and intervals:** Never started; retired by project decision `July 19, 2026 at 8:55:35 AM PDT (UTC−07:00)`; no actual intervals.
- **Model, reasoning strength, and original forecast:** None; no forecast after retirement.
- **Time, variance, evidence, confidence, and forecasting lessons:** No counted or elapsed time and no variance can be assigned. Roadmap `e249071`
  and retirement `8b9980f`; no implementation/PR/release/workflow. **Unknown** rather than zero.

### M53 — Authenticated Forum Participation

- **Objective:** Former Forum sign-in/post/reply/moderation scope; permanently retired.
- **Start, completion, and intervals:** Never started; retired by project decision `July 19, 2026 at 8:55:35 AM PDT (UTC−07:00)`; no actual intervals.
- **Model, reasoning strength, and original forecast:** None; no forecast after retirement.
- **Time, variance, evidence, confidence, and forecasting lessons:** No counted or elapsed time and no variance can be assigned. Roadmap `e249071`
  and retirement `8b9980f`; no implementation/PR/release/workflow. **Unknown** rather than zero.

### M54 — Forum Five-Station and Notification Certification

- **Objective:** Former Forum certification/notification scope; permanently retired.
- **Start, completion, and intervals:** Never started; retired by project decision `July 19, 2026 at 8:55:35 AM PDT (UTC−07:00)`; no actual intervals.
- **Model, reasoning strength, and original forecast:** None; no forecast after retirement.
- **Time, variance, evidence, confidence, and forecasting lessons:** No counted or elapsed time and no variance can be assigned. Roadmap `e249071`
  and retirement `8b9980f`; no implementation/PR/release/workflow. **Unknown** rather than zero.

### M55 — Google Cast Feasibility and Certification

- **Objective:** Decide go/no-go only after permitted stream use, receiver compatibility, lifecycle/route behavior,
  rights review, and five-station validation.
- **Start, completion, and intervals:** Not started; all actual intervals and totals are not applicable.
- **Model, reasoning strength, and original forecast:** Sol High; current forecast 2–4 active weeks after permission/receiver triggers and
  may end in an explicit no-go.
- **Time, variance, evidence, confidence, and forecasting lessons:** No actual variance. Roadmap `e249071`, forecast `f971a9e`; no Cast evidence.
  **Unknown**. A no-go remains a valid timed outcome when the gate is authorized.

### M56 — Extended Station Capability Certification

- **Objective:** Independently verify non-SST request messages, activity/cooldown, membership, and capability differences.
- **Start, completion, and intervals:** Not started; all actual intervals and totals are not applicable.
- **Model, reasoning strength, and original forecast:** Sol High; current forecast 1–3 active weeks after controlled evidence/test accounts.
- **Time, variance, evidence, confidence, and forecasting lessons:** No actual variance. Roadmap `e249071`, forecast `f971a9e`; no certification.
  **Unknown**. Time each station separately and preserve account/user-blocked waits.

### M57 — Account Registration, Recovery, and Management Access

- **Objective:** Verify permitted station routes and deletion/recovery behavior, then choose an authorized trusted-browser
  or native contract without sharing browser/app sessions.
- **Start, completion, and intervals:** Not started; all actual intervals and totals are not applicable.
- **Model, reasoning strength, and original forecast:** Sol High; current forecast 1–3 active weeks after route authorization and account facts.
- **Time, variance, evidence, confidence, and forecasting lessons:** No actual variance. Roadmap `e249071`, forecast `f971a9e`; no route evidence.
  **Unknown**. Record authorization and test-account waits as user-blocked, not active.

### M58 — Membership Commerce Authorization and Billing Architecture

- **Objective:** Establish station/merchant authority, billing/activation architecture, products, tax/support/refund,
  privacy, and security obligations.
- **Start, completion, and intervals:** Not started; all actual intervals and totals are not applicable.
- **Model, reasoning strength, and original forecast:** Sol Extra High; current forecast 2–4 active weeks for architecture after written
  authority and a viable partner; contract timing remains external.
- **Time, variance, evidence, confidence, and forecasting lessons:** No actual variance. Roadmap `e249071`, forecast `f971a9e`; no commerce evidence.
  **Unknown**. Keep contract waiting outside counted project time.

### M59 — Native VIP/RIP Purchase and Activation

- **Objective:** Implement approved native purchase, pending, acknowledgement, activation, restore, and failure flows
  with server-verified, station/account-isolated entitlement.
- **Start, completion, and intervals:** Not started; all actual intervals and totals are not applicable.
- **Model, reasoning strength, and original forecast:** Terra High with Sol Extra High escalation for unresolved policy/security; current
  forecast 3–6 active weeks after M58's contract is accepted.
- **Time, variance, evidence, confidence, and forecasting lessons:** No actual variance. Roadmap `e249071`, forecast `f971a9e`; no implementation.
  **Unknown**. Record sandbox/provider waits separately from engineering work.

### M60 — Subscription Lifecycle and Five-Station Certification

- **Objective:** Certify renewal, cancellation, grace, refund, revocation, restore, reinstall, device/account switching,
  accessibility, support, and independent station product behavior.
- **Start, completion, and intervals:** Not started; all actual intervals and totals are not applicable.
- **Model, reasoning strength, and original forecast:** Sol Extra High acceptance; current forecast 2–4 active weeks after M59 and five-station
  sandbox availability.
- **Time, variance, evidence, confidence, and forecasting lessons:** No actual variance. Roadmap `e249071`, forecast `f971a9e`; no certification.
  **Unknown**. Time lifecycle matrices and external sandbox availability independently.

## Historical milestone and legacy-ID crosswalk

The roadmap was renumbered on July 18, 2026. Historical records and commit messages retain their original identifiers,
but their work is represented by the canonical rows above and is not added a second time to cumulative totals. The
complete translation layer remains in [MILESTONE_MIGRATION.md](MILESTONE_MIGRATION.md).

| Historical record or scope | Canonical treatment | Duration treatment and assumptions |
| --- | --- | --- |
| Legacy M1–M16 | M01–M16 | Same completed work; IDs below M10 gained zero padding. |
| Legacy M18–M22 | M17–M21 | Same station-certification work shifted down one canonical ID. |
| Legacy M23.1–M23.7 slices | M22, M23, M28–M30, M34, and M35 | Each canonical row uses its own evidence boundary; no umbrella M23 duration is added. |
| Legacy M24–M26 and M27.1 | M24–M27 | Same work; M27.1 became canonical M27. |
| Legacy M27.2 authorization, implementation, and validation slices | M36–M38 | Planned work; no actual interval has started. |
| Legacy M28 release slices | M39–M41 | Planned work; no actual interval has started. |
| Legacy M17 private-message repair and delivery slices | M47–M50 | Deferred work; no actual interval has started. |
| Forum F1–F4 | M51–M54 | M51 preserves the research-to-retirement window; M52–M54 never started. |

## Prospective operating procedure

### Start

When the owner authorizes a milestone, immediately add or update its summary row and detailed section with:

1. the actual execution-start timestamp in 12-hour format with the full date, seconds, AM/PM, timezone abbreviation,
   and UTC offset;
2. the approved model and reasoning strength;
3. the original counted-project-time forecast in hours or an hours range and the original expected completion date/time;
4. the authorized objective and completion gates; and
5. the first active interval.

Planning, clarification, and the reasoning gate before authorization do not start the milestone clock.

### Pause and resume

Append each interval rather than overwriting history:

| Started | Ended | Category | Reason or work | Evidence | Hours |
| --- | --- | --- | --- | --- | ---: |
| Full date and 12-hour timestamp with timezone | Full date and 12-hour timestamp with timezone | Active / Automated wait / User-blocked / Other | Concise description | Commit, run, report, or decision | `(ended - started) / 3600` |

Record the pause timestamp, category, and reason at the pause. Record the resume timestamp when work continues. Long
user-response delays are user-blocked, not active execution. An automated job counts as automated wait only when
execution is actually waiting for it. Close an active interval before opening a wait interval so time is not counted
twice.

### Completion

At completion:

1. record the exact completion timestamp;
2. total active, automated-wait, and user-blocked intervals separately;
3. calculate counted project time as active plus automated wait;
4. calculate total elapsed time from execution start through completion;
5. compare counted project time with the original hours forecast in hours and percentage;
6. explain material variance and record forecasting lessons;
7. update cumulative totals, the roadmap, README, Project cards, and public-safe overview where appropriate; and
8. commit this ledger with the rest of the milestone completion documentation.

For a point forecast, variance is `actual - forecast` and percentage variance is
`(actual - forecast) / forecast × 100`. For a range, compare actual with the nearest bound when outside the range and
report `within range` when inside it.

Every milestone completion report must include exactly this information:

```text
Milestone [NUMBER] time:
Forecast: [HOURS OR RANGE]
Counted project time: [HOURS]
Total elapsed time: [HOURS]
User-blocked time excluded: [HOURS]
Forecast variance: [AMOUNT AND PERCENTAGE]
Cumulative counted project time through Milestone [NUMBER]: [HOURS]
```

If an actual value remains unsupported, use `Unknown` and explain why; never replace missing evidence with `0.00`.
Do not call a duration precise unless interval timestamps or an equivalent time-tracking record support that claim.
