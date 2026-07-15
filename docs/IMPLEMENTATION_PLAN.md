# Implementation plan

Updated July 14, 2026 after adopting explicit certification milestones for all five stations. Estimates are active Codex elapsed time in this environment, including inspection, implementation, Gradle validation, documentation, Git, and remote confirmation—not traditional human developer time.

## Planning model

- M1–M13 are complete and retain their existing evidence and published commits.
- M14–M17 complete the remaining shared product capabilities once, using immutable Compose state, repository contracts, capability flags, and station-isolated protected sessions.
- M18–M22 certify the shared implementation against each station. They are hardening and evidence gates, not station-specific application forks.
- M23–M24 are the final distribution and publication gates.
- Private Messages remains numbered as M17 but deferred pending legacy server repair and verified production limits.
- Early M23 readiness artifacts are preserved, but they must be refreshed after M14–M22. The Play developer account is approved; Play App Signing/app setup, final upload signing, and explicit publication authorization remain dependencies.

## Task complexity protocol

Before code changes for every roadmap task, state its Task Complexity Level and a brief milestone-specific approach:

- Level 1 — Scaffold & Boilerplate: low reasoning; prioritize speed, Android best practices, clean architecture, concise production-ready code, and minimal explanation.
- Level 2 — Feature Logic & API Integration: medium reasoning; emphasize robust errors, immutable ViewModel state, coroutine/thread safety, implementation detail, test recommendations, and clear state-flow explanation.
- Level 3 — Architectural Refactoring & System Design: high reasoning; compare alternatives, justify the chosen scalable pattern, and assess performance, dependency-injection, maintenance, and side effects.

This classification supplements the required XS–XXL T-shirt size, rationale, estimated duration, confidence check, and high-level task breakdown.

## Current milestone

### M14 — Local personalization and station preferences

- Task Complexity Level: 2 — Feature Logic & API Integration
- Size: M
- Estimated elapsed time: 2–4 hours
- Usage intensity: Medium
- Confidence: Medium
- Outcome: persist bounded device-local default/last-station preferences and clearly distinguish those preferences from station-owned Favorites and account data.
- Expected layers: preference repository contract and implementation, immutable ViewModel state/actions, settings/personalization UI, restoration tests, accessibility/UI tests, documentation, and wired-device smoke tests.
- Dependencies: the existing station catalog, selected-station source of truth, and Compose Settings/More surfaces.
- Principal risk: migration/restoration ordering must never briefly select or start the wrong station, and corrupt/removed station IDs must fall back safely.
- Completion gate: default and last-station behavior is explicit, lifecycle-safe, tested, and does not alter authentication, Favorites, playback ownership, or station-scoped remote state.
- Status: preflight is next; implementation has not started.

## Latest completed milestone

M13 Independent Accounts UX and isolation tests completed July 14, 2026 in `9ef1f1c`. All five states are independently visible and actionable; explicit expiration and pairwise logout/restoration isolation are covered at repository, ViewModel, Compose, in-memory store, and Android Keystore layers. Unit tests, lint, debug assembly, and 15/15 wired Razr instrumentation tests pass. See [m13-validation.md](m13-validation.md).

## Shared feature milestones

| Milestone | Size | Estimate | Usage | Rationale and outcome | Primary confidence variable |
| --- | --- | --- | --- | --- | --- |
| M15 Request history, cooldown, and membership state | L | 4–8 hours | High | Model station-scoped history, cooldown, VIP/RIP, and membership presentation without inferring unsupported state | Verified authenticated evidence and per-station rule differences |
| M16 Secondary community/content access | M | 2–4 hours | Medium | Add capability-aware native or Custom Tab routes for selected verified public modules without using a WebView replacement | Product prioritization and safe route verification |
| M17 Private Messages | L (provisional) | 4–8 hours after server repair | High | Add native station-isolated inbox/read/compose/reply/refresh and explicit user-initiated send over existing protected sessions | Website repair, production limits, and consistent authenticated forms |

Each shared milestone includes repository/ViewModel/UI work as applicable, lifecycle-safe state, accessibility semantics, focused tests, affected-module validation, wired Razr inspection, documentation, a focused commit, push, and remote confirmation.

## Station certification milestones

The certification program would be XL if treated as one unit, so it is split into five reviewable S–L milestones. Total expected active elapsed time is 20–35 hours, approximately 3–6 focused days. Confidence is Medium-Low because live accounts, CAPTCHA, station rules, rate limits, metadata quality, and legacy server behavior can differ.

| Milestone | Size | Estimate | Usage | Rationale and focus | Primary confidence variable |
| --- | --- | --- | --- | --- | --- |
| M18 StreamingSoundtracks.com certification | S | 2–4 hours | Medium | Most live coverage already exists; certify VIP/non-admin behavior, 30-row Queue, request messages, Favorites, chat, and authenticated workflows | Legacy PM stability and privileged accounts masking ordinary-member restrictions |
| M19 1980s.FM certification | M | 4–7 hours | High | Establish equivalent live evidence for playback/fallback, metadata, account isolation, Queue/history, chat, Favorites, requests, and membership behavior | Availability of a representative station account and undocumented rules |
| M20 Adagio.FM certification | M | 4–7 hours | High | Certify classical metadata presentation plus playback/fallback, account isolation, Queue/history, chat, Favorites, requests, and membership behavior | Metadata shape and availability of a representative station account |
| M21 Death.FM certification | L | 6–10 hours | High | Harden the compact Queue feed, sparse metadata/artwork behavior, RIP membership differences, playback/fallback, chat, Favorites, and requests | Reduced identifiers/metadata and station-specific membership behavior |
| M22 Entranced.FM certification | M | 4–7 hours | High | Establish live evidence for playback/fallback, metadata, account isolation, Queue/history, chat, Favorites, requests, and membership behavior | Availability of a representative station account and undocumented rules |

### Common station task breakdown and completion gate

1. Reconfirm permitted primary/fallback playback and Media3 behavior without changing working URLs casually.
2. Verify metadata, artwork, Queue/history limits, stale/error handling, and station-scoped parsing.
3. Verify independent authentication, restoration, expiration, logout, and account capability presentation.
4. Verify chat read/post limits, Favorites discovery, requests, eligibility, cooldown, attribution/messages, and Private Messages when available.
5. Preserve unsupported differences as explicit capability-unavailable states; never guess endpoints or rules.
6. Add or update parser, repository, ViewModel, Compose, accessibility, and isolation tests for station-specific evidence.
7. Run focused and broad validators, perform a wired Razr smoke test, update matrices/handoff, commit, push, and confirm the remote branch.

Unblocked M18–M22 work may proceed while M17 is deferred, but a station cannot receive final certification until every in-scope capability passes or the user explicitly changes Alpha scope.

## Distribution milestones

| Milestone | Size | Estimate | Usage | Rationale and outcome | Primary confidence variable |
| --- | --- | --- | --- | --- | --- |
| M23 Alpha Test Distribution Readiness | M | 1–2 focused days | Medium | Refresh privacy, tester guidance, signing guardrails, versioning, release artifacts, bundle checks, and Play readiness after M14–M22 | Custody/configuration of the upload signing identity and Console declarations |
| M24 Alpha publication completion | M | 1–3 hours after Console setup | Medium | Produce and verify the authorized signed Play bundle and internal/closed test release | Signing authorization, tester-track configuration, and release review outcome |

No item is classified XL. Any future phase that exceeds L will be divided before implementation.
