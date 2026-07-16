# Implementation plan

Updated July 16, 2026 after completing the protected Linux signing checkpoint for M23.1. Estimates are active Codex elapsed time in this environment, including inspection, implementation, Gradle validation, documentation, Git, and remote confirmation—not traditional human developer time.

## Planning model

- M1–M16 and M18 are complete and retain their existing evidence and published checkpoints.
- M17 is the remaining shared capability, using immutable Compose state, repository contracts, capability flags, and station-isolated protected sessions once its external blocker clears.
- M18–M22 certify the shared implementation against each station. They are hardening and evidence gates, not station-specific application forks.
- Each station's representative native login, protected restoration, isolation, authenticated surface, and logout flow is now a hard completion gate. Public/device evidence may be gathered early but does not advance the active station order.
- M23 is an XL Play-testing readiness program divided into M23.1–M23.7 below; M23.3 and M23.7 are complete, and M24 remains the authorized publication gate.
- Private Messages remains numbered as M17 but deferred pending legacy server repair and verified production limits. Inbox and Sent Box discovery worked, while New Message selection remained suspect and a profile-originated MorgHubby test appeared to submit without delivery; the site owner has the reproduced result.
- Early M23 readiness artifacts are preserved. M23.1's current audit covers the production manifest, dependency/license inventory, explicit backup exclusions, 16 KB APK/ELF packaging, and a protected Ubuntu build from commit `2086ab9` whose AAB/APK signer matches the registered upload certificate. Version-code eligibility and Play delivery remain open. The Play developer account is approved; UGC reconciliation, Play review declarations, explicit rights evidence, release-device closure, and publication authorization remain dependencies.

## Task complexity protocol

Before code changes for every roadmap task, state its Task Complexity Level and a brief milestone-specific approach:

- Level 1 — Scaffold & Boilerplate: low reasoning; prioritize speed, Android best practices, clean architecture, concise production-ready code, and minimal explanation.
- Level 2 — Feature Logic & API Integration: medium reasoning; emphasize robust errors, immutable ViewModel state, coroutine/thread safety, implementation detail, test recommendations, and clear state-flow explanation.
- Level 3 — Architectural Refactoring & System Design: high reasoning; compare alternatives, justify the chosen scalable pattern, and assess performance, dependency-injection, maintenance, and side effects.

This classification supplements the required XS–XXL T-shirt size, rationale, estimated duration, confidence check, and high-level task breakdown. Every pushed project commit must be posted as a concise change-and-verification summary to the configured Discord project thread, with delivery verified before final handoff; completed roadmap milestones must be identified explicitly. Significant visual changes must also include repository and Discord screenshots. A backend-only milestone should explicitly state that it introduced no meaningful visual surface instead of reusing an unrelated image.

## Current milestone

### M23 — Play-Testing Readiness Program

- Task Complexity Level: 2 — Feature Logic & API Integration
- Size: XL as an umbrella, divided into seven S–L sub-milestones
- Estimated elapsed time: 3–6 focused days plus owner and station-side input
- Usage intensity: Medium–High across the complete program
- Confidence: Medium
- Outcome: retain the completed Alpha preparation while closing every release-artifact, UGC, platform-target, Play-review, rights, device, and store-quality boundary needed for a defensible Google Play test release.
- Expected layers: release configuration, domain/repository/UI policy work for UGC, Android 16 behavior validation, secure local signing handoff, privacy/data-safety reconciliation, Play declarations, bundle/APK inspection, device testing, tester documentation, and final repository/Discord evidence.
- Dependencies: approved Google Play developer account, protected upload identity, administrator reconciliation of the authorized moderation-report test, written brand/content authorization, reusable reviewer accounts, Play App Signing, and explicit authorization before any publication action.
- Principal risks: shipping publicly accessible UGC without required moderation controls, missing the August 31, 2026 API 36 deadline, or submitting third-party branding/artwork without review-ready permission evidence.
- Completion gate: every required M23.1–M23.6 outcome passes; M23.7 is required before a public store listing but may follow tightly controlled internal QA. No store release is submitted during M23.
- Status: in progress. M23.1's audit and protected Ubuntu build are green with explicit backup exclusions, native open-source notices, 132 unit tests, lint, release AAB/APK signatures, exact upload-certificate verification, 16 KB APK/ELF checks, and Node 24-compatible official CI actions; version-code eligibility and Play delivery remain. M23.3 API 36 readiness and M23.7 launcher polish are complete. M23.2 is authorized and its native age/Terms/reveal, report, block, privacy, and duplicate-safe delivery implementation is green; only administrator receipt reconciliation remains. M23.6 coverage is green across API 26, API 36, 16 KB, Fold, and Tablet states; 2× text checks pass, the current Tablet suite passes 40/40, live phone/Tablet network cut-and-restore cycles automatically resume once, the 1,500-track Favorites path preserves stable list state and traverses end to end, and physical Razr playback/UI survive a measured open/tabletop/closed/reopened cycle. M17 Private Messages remains explicitly deferred and is not shipped.

#### M23 sub-milestone plan

| Milestone | Size | Estimate | Usage | Outcome and completion gate | Primary confidence variable |
| --- | --- | --- | --- | --- | --- |
| M23.1 Current-head signed release candidate | M, protected Ubuntu build green | 2–4 hours | Medium | Commit `2086ab9` produced the protected signed AAB/APK through the Linux helper; the exact registered upload certificate, production manifest, dependencies/notices, backup exclusions, and 16 KB packaging pass. Validate version-code eligibility and Play-delivered install/update; see `m23-release-candidate-audit.md` | Whether version code 2 has already reached Play and Play-generated delivery behavior |
| M23.2 UGC safety and moderation | L, implementation green; receipt pending | 6–12 hours after station input | High | **In progress:** authorized native age/Terms/reveal gates, separate report-content/report-user/block-user actions, local block management, privacy boundaries, and duplicate-safe indeterminate delivery are implemented; see `m23-ugc-safety-validation.md` | Administrator confirmation of whether the single harmless report reached the monitored inbox |
| M23.3 Android 16 / API 36 readiness | M | 2–4 hours | Medium | **Complete:** target API 36 with predictive Back, edge-to-edge, adaptive UI, connected MediaSession, lint/release, and 27/27 API 36 device coverage; see `m23-api36-readiness.md` | Closed with one corrected viewport-dependent lazy-list test assumption |
| M23.4 Play review declarations | M, local packet/video rehearsal prepared | 2–4 hours plus owner input | Medium | Accurate listing, copy-ready media-playback declaration, reviewer navigation, content-rating boundaries, privacy/Data Safety worksheets, and a frame-inspected notification video rehearsal are prepared; complete protected credentials, the signed-candidate video/link, owner attestations, and Console submission | CAPTCHA-compatible reviewer accounts, final questionnaire wording, and station retention answers |
| M23.5 Brand and content-rights evidence | S, request packet prepared | 1–2 hours plus external confirmation | Low locally | Copy-ready authorization and sanitized evidence handling now cover app/station names, logos, artwork/metadata, authorized stream access, screenshots, and Play testing/distribution; privately retain the authorized response and submit advance evidence to Play; see `m23-owner-response-packet.md` | Scope and availability of the rights holder's written authorization |
| M23.6 Release device matrix and pre-launch report | M, accessibility/network/performance/device checkpoints green | 3–6 hours | Medium | Emulator coverage passes across API 26, API 36, 16 KB, Fold, and Tablet states; large-text reflow, one-shot network restoration, 1,500-track Favorites performance, and physical Razr hinge checks pass. Complete Play delivery/update and pre-launch remediation | Play crawler/delivery access and spoken TalkBack pass |
| M23.7 Adaptive launcher/store polish | S | 1–2 hours | Low | **Complete:** density-specific legacy, masked adaptive, and Android 13 monochrome launcher resources preserve the selected logo; store graphics and screenshots were revalidated without unnecessary recapture | Closed with APK resource inspection, API 35 drawable test, lint, and launcher inspection |

## Latest completed checkpoint

The M23.6 startup and large-Favorites performance checkpoint completed July 16, 2026. Availability resolution is isolated from unrelated metadata/request emissions, unchanged 1,500-track state and default ordering reuse their existing list instances, and the connected test now traverses to track 1,500 before returning to sort. Five API 35 debug-emulator cold launches measured 948–986 ms with a 969 ms median; this is diagnostic evidence rather than a Play/physical performance claim. The 132-test unit suite, 40/40 Tablet connected suite, and debug lint pass. See [m23-performance-validation.md](m23-performance-validation.md).

The earlier M23.6 network-loss and playback-reconnection checkpoint remains green; see [m23-network-recovery-validation.md](m23-network-recovery-validation.md).

The earlier M23.6 accessibility checkpoint remains green at normal and maximum tested accessibility settings; see [m23-accessibility-validation.md](m23-accessibility-validation.md).

M23.7 Adaptive launcher/store polish remains the latest completed full sub-milestone. It completed July 15, 2026; see [m23-launcher-polish.md](m23-launcher-polish.md).

## Shared feature milestones

| Milestone | Size | Estimate | Usage | Rationale and outcome | Primary confidence variable |
| --- | --- | --- | --- | --- | --- |
| M17 Private Messages | L (provisional) | 4–8 hours after server repair | High | Add native station-isolated inbox/read/compose/reply/refresh and explicit user-initiated send over existing protected sessions after the reproduced profile-send non-delivery is repaired | Website repair, delivery confirmation, production limits, and consistent authenticated forms |

Each shared milestone includes repository/ViewModel/UI work as applicable, lifecycle-safe state, accessibility semantics, focused tests, affected-module validation, physical Razr inspection, documentation, a focused commit, push, and remote confirmation.

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
7. Run focused and broad validators, perform a physical Razr smoke test, update matrices/handoff, commit, push, and confirm the remote branch.

M18–M22 are complete while M17 remains deferred; every in-scope station capability now has passing evidence or an explicit certified unavailable/unverified boundary.

## Distribution milestones

| Milestone | Size | Estimate | Usage | Rationale and outcome | Primary confidence variable |
| --- | --- | --- | --- | --- | --- |
| M23 Alpha Test Distribution Readiness | M | 1–2 focused days | Medium | Refresh privacy, tester guidance, signing guardrails, versioning, release artifacts, bundle checks, and Play readiness after M15–M22 | Custody/configuration of the upload signing identity and Console declarations |
| M24 Alpha publication completion | M | 1–3 hours after Console setup | Medium | Produce and verify the authorized signed Play bundle and internal/closed test release | Signing authorization, tester-track configuration, and release review outcome |

No item is classified XL. Any future phase that exceeds L will be divided before implementation.
