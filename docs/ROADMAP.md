# 24Seven.FM Player roadmap

Updated July 18, 2026 after the full milestone, implementation, security, Google Play, release, and operations audit.

This is the authoritative milestone sequence for the Player repository. Milestones use dependency-ordered, zero-padded
canonical IDs. The renumbering does not rewrite Git history: legacy IDs remain traceable through
[the milestone migration ledger](MILESTONE_MIGRATION.md), the original commits, and validation evidence.

## State vocabulary

- **Complete** — the milestone reached its documented gate and has committed evidence.
- **In progress** — authorized local or owner work remains.
- **Waiting externally** — completion requires station, administrator, rights-holder, provider, or Play Console input.
- **Planned** — accepted roadmap scope whose dependencies are not yet closed.
- **Deferred** — deliberately excluded from the shipping build until a named external condition is repaired or authorized.

Completion is milestone-specific. A completed historical milestone is not erased when a later audit finds a new release
requirement; the new work receives its own milestone and regression evidence.

## Phase 1 — Native foundation and shared product capabilities

| ID | Milestone | State | Completion signal |
| --- | --- | --- | --- |
| M01 | Buildable baseline | Complete | Native Kotlin/Compose project, pinned Android toolchain, and verified Windows/Ubuntu paths |
| M02 | Five-station playback | Complete | Verified station streams, atomic switching, and bounded fallback |
| M03 | Background and system media | Complete | Service-owned Media3 player, MediaSession, audio focus, and system controls |
| M04 | Now Playing | Complete | Live ICY metadata and same-station artwork enrichment |
| M05 | Native navigation | Complete | Adaptive destinations and persistent mini-player |
| M06 | Queue and History | Complete | Bounded, station-scoped Queue/History observation |
| M07 | Authentication | Complete | Native station login and Android-protected station sessions |
| M08 | Chat | Complete | Native, memory-bounded, station-scoped Chat |
| M09 | Song requests | Complete | Native search, confirmation, and one-shot submission |
| M10 | Request attribution and messages | Complete | Queue attribution and verified SST request messages |
| M11 | Adaptive Alpha UI | Complete | Responsive UI, previews, semantics, and exit flow |
| M12 | Queue-aware request availability | Complete | Conservative station/Queue availability and pre-submit checks |
| M13 | Independent Accounts UX | Complete | Five visible, isolated station account states |
| M14 | Local personalization | Complete | Startup/default-station preferences separated from station data |
| M15 | Request activity and membership | Complete | SST history, cooldown/readiness, and explicit membership evidence |
| M16 | Secondary content access | Complete | Allowlisted same-station HTTPS Custom Tabs; no WebView |

## Phase 2 — Five-station certification

| ID | Milestone | State | Completion signal |
| --- | --- | --- | --- |
| M17 | StreamingSoundtracks.com certification | Complete | Public, authenticated, VIP, request, Favorites, and Chat workflows |
| M18 | 1980s.FM certification | Complete | Independent authenticated and station-specific behavior |
| M19 | Adagio.FM certification | Complete | Classical metadata plus authenticated workflows |
| M20 | Death.FM certification | Complete | Compact Queue, sparse metadata, RIP boundaries, and authenticated workflows |
| M21 | Entranced.FM certification | Complete | Extended Queue, legacy ICY punctuation, and authenticated workflows |

## Phase 3 — Platform and pre-release product polish

| ID | Milestone | State | Completion signal |
| --- | --- | --- | --- |
| M22 | Android 16 and API 36 readiness | Complete | API 36 target, edge-to-edge, predictive Back, and device regression evidence |
| M23 | Adaptive launcher and store polish | Complete | Legacy, adaptive, and monochrome launcher resources |
| M24 | Sleep Timer | Complete | Service-owned bounded timer with restoration and deterministic expiry |
| M25 | System Audio-Output Selection | Complete | Android-managed output chooser and accurate route state; Cast remains separate |
| M26 | In-App Diagnostics | Complete | User-reviewed fixed-allowlist snapshot with explicit copy/share |
| M27 | Local Chat-Mention Notifications | Complete | Opt-in exact-name detection for actively observed Chat with privacy-minimized alerts |

## Phase 4 — Alpha readiness and release integrity

These milestones must close before the final Alpha candidate is frozen. Work may proceed in parallel where dependencies
allow, but no partial checkpoint should be described as release-ready.

| ID | Milestone | State | Required gate |
| --- | --- | --- | --- |
| M28 | UGC Safety and Ongoing Moderation | Complete | Existing safeguards plus fixed-recipient Contact/report email handoff, physical composer validation, and one owner-confirmed one-shot receipt check |
| M29 | Play Declarations, Privacy, and Data Safety | Waiting externally | Exact-artifact declarations, reviewer access, content rating, retention/deletion answers, final media-playback video, and privacy consistency |
| M30 | Brand, Content, and Distribution Rights | Waiting externally | Written authorization for app/station naming, logos, artwork/metadata, streams, screenshots, and Play testing/distribution |
| M31 | Payments, External Links, and Account Lifecycle Compliance | Planned | Audit every Custom Tab destination; remove or lawfully implement transaction/account-creation routes; reconcile deletion and Play declarations |
| M32 | Session, Controller, Network, and Supply-Chain Security | Planned | MediaSession controller policy, coherent cookie rotation/expiry, trusted redirects, canonical station-ID migration, dependency/action integrity, and adversarial tests |
| M33 | Request Transaction Integrity | Planned | Fresh station/Queue/account/membership/cooldown/limit/identity validation, exact availability semantics, station confirmation, and one-shot result handling |
| M34 | Device, Accessibility, and Pre-launch Acceptance | In progress | Human audible TalkBack, keyboard/pointer/assistive access, multi-window/foldable coverage, Play delivery/update, and pre-launch evidence |
| M35 | Release Signing and Console Eligibility | In progress | Protected upload identity, new-account physical-device verification, package registration, eligible version code, and reproducible signed build path |

## Phase 5 — Authorized closed-app community notifications

M36 is an external authorization decision. M37 and M38 must not start by substituting background polling or forwarding
protected station sessions.

| ID | Milestone | State | Required gate |
| --- | --- | --- | --- |
| M36 | Notification Event-Source Authorization | Waiting externally | Authorized station event source, webhook, or privacy-compatible relay with ownership, payload, authentication, retention, deletion, abuse, and outage contracts |
| M37 | Secure Closed-App Notification Delivery | Planned after M36 | Opt-in station-scoped token/event delivery, minimal payloads, duplicate safety, sign-out/reinstall handling, and no perpetual polling |
| M38 | Notification Lifecycle and Privacy Certification | Planned after M37 | Foreground/background/closed/idle/reboot/network tests, delivery latency, battery/traffic, lock-screen privacy, deep-link gates, and all five station boundaries |

## Phase 6 — Alpha distribution

| ID | Milestone | State | Required gate |
| --- | --- | --- | --- |
| M39 | Alpha Candidate and Documentation Freeze | Planned | Current-head signed AAB/APK, exact version/signer/dependency audit, synchronized roadmap/testing/release notes/listing, and no open release-critical finding |
| M40 | Play Alpha Delivery and Pre-launch Remediation | Planned after M39 | Upload, Play-generated split inspection, fresh install/update, pre-review/pre-launch reconciliation, reviewer access, and artifact-specific evidence |
| M41 | Explicit Alpha Publication | Planned after M40 | Owner-authorized internal or closed release, tester instructions, support path, rollback record, and verified availability; never automatic |

## Phase 7 — Closed testing, production, and operations

| ID | Milestone | State | Required gate |
| --- | --- | --- | --- |
| M42 | Closed-Test Operations and Stabilization | Planned after M41 | Qualifying tester continuity, structured feedback, fixes/retests, Play vitals/pre-launch review, update delivery, and exit criteria |
| M43 | Production Access and Policy Approval | Planned after M42 | Production-access questionnaire, testing/feedback/change evidence, final policy review, and resolved Play response |
| M44 | Production Release and Staged Rollout | Planned after M43 | Explicit authorization, production candidate, staged percentages, health thresholds, pause/rollback, release notes, and update validation |
| M45 | Operational Reliability and Recertification | Planned | Play vitals/support/privacy handling, station outage and contract-drift procedures, rights/moderation recertification, target/API/dependency cadence, and key recovery |

## Phase 8 — Deferred and future programs

None of these milestones blocks M41 unless a later owner decision explicitly moves it into the Alpha scope.

| ID | Milestone | State | Required gate |
| --- | --- | --- | --- |
| M46 | Architecture Sustainability | Planned post-Alpha | Split oversized UI/ViewModel boundaries by feature contracts, preserve immutable state/repositories, and add performance work only where evidence warrants it |
| M47 | Private Messages Server Repair and Protocol Certification | Deferred | Site owner repairs delivery; limits, station isolation, error/indeterminate behavior, and operator confirmation become verifiable |
| M48 | Native Private Message Reading | Deferred after M47 | Protected station-scoped Inbox/Sent models, bounded retrieval, immutable state, loading/empty/error/expired states, and privacy tests |
| M49 | Private Message Compose, Reply, and Send | Deferred after M48 | Explicit preview/confirmation, one-shot mutation, limits, CSRF/session safety, and indeterminate-delivery recovery |
| M50 | Private Message Five-Station and Notification Certification | Deferred after M49/M38 | Independent station certification plus optional privacy-minimized delivery through the authorized notification architecture |
| M51 | Verified Forum Links | Planned | Independently verify exact public HTTPS Forum routes and expose only trusted station-scoped Custom Tabs |
| M52 | Native Forum Read-Only Foundation | Planned after M51 | Authorized repository contracts, bounded native category/thread reading, UGC gates, and browser fallback |
| M53 | Authenticated Forum Participation | Planned after M52 | Certified session reuse, previews, one-shot compose/reply, moderation actions, and indeterminate handling |
| M54 | Forum Five-Station and Notification Certification | Planned after M53/M38 | Independent station/device/accessibility certification and optional authorized notifications |
| M55 | Google Cast Feasibility and Certification | Planned research gate | Permitted stream use, receiver compatibility, lifecycle/route behavior, rights review, and five-station validation before implementation |
| M56 | Extended Station Capability Certification | Planned research gate | Independently verify non-SST request messages, activity/cooldown, membership, and other capability differences without inheritance |
| M57 | Account Registration, Recovery, and Management Access | Planned research gate | Verify permitted station routes and deletion/recovery behavior, then choose trusted browser or authorized native interfaces without sharing browser/app sessions |

## Program dependencies

1. M28 is complete; M29–M35 close the remaining Alpha policy, security, correctness, accessibility, and release-tooling gaps.
2. M36 authorizes the event source before M37 implementation; M38 certifies the result.
3. M39 freezes the exact candidate only after all intended Alpha code has landed.
4. M40 proves Play delivery and resolves automated/reviewer findings.
5. M41 requires explicit owner publication authorization.
6. M42–M45 form the production path; Alpha publication is not production readiness.
7. M47–M57 remain capability- and authorization-gated future work.

The manual acceptance catalog is maintained on the
[Product Testing page](../privacy-site/project/product-testing/index.html). `PT-*` identifiers do not change during
milestone renumbering so existing tester reports remain traceable.
