# Implementation plan

Updated July 18, 2026 after the complete roadmap renumbering and release-gap audit.

The canonical milestone sequence, state, dependency, and completion gate live in [ROADMAP.md](ROADMAP.md). Historical
IDs translate through [MILESTONE_MIGRATION.md](MILESTONE_MIGRATION.md). This file describes how the remaining work is
executed; it does not create a competing roadmap.

## Planning and model routing

- Sol plans architecture, authorization, security, privacy, release, and final acceptance decisions.
- Terra implements approved bounded changes, tests, and documentation.
- Luna handles source inventories, mechanical synchronization, and structured formatting that is checked against the
  authoritative source.
- Any unresolved playback ownership, authentication/session, request eligibility, UGC, notification delivery, signing,
  or publication decision returns to Sol before implementation continues.

No milestone may introduce a WebView, a second player, administrator runtime code, cross-station session sharing,
unverified stream addresses, background polling represented as push, or undocumented server behavior.

## Current program

M01–M28 are achieved checkpoints. Their historical commits and validation remain intact. M28 closed after the fixed-recipient native Contact/report handoff passed automated and physical Razr validation and one authorized harmless report was sent exactly once with owner-confirmed receipt; the sanitized evidence is recorded in [m23-ugc-safety-validation.md](m23-ugc-safety-validation.md).

The active program is M29–M35 Alpha readiness:

| ID | Size | State | Implementation focus | Principal dependency |
| --- | --- | --- | --- | --- |
| M29 Play Declarations, Privacy, and Data Safety | M | Waiting externally | Exact-artifact privacy/Data Safety, content rating, reviewer access, retention/deletion, and foreground-media evidence | Owner answers and Play Console |
| M30 Brand, Content, and Distribution Rights | S | Waiting externally | Preserve private rights evidence and record only sanitized completion facts | Written rights-holder authorization |
| M31 Payments, External Links, and Account Lifecycle | M | Planned | Audit every shipped Custom Tab and downstream purchase/account path; remove, gate, or lawfully implement any incompatible route | Live destination behavior and Play program eligibility |
| M32 Session, Controller, Network, and Supply-Chain Security | XL, split | Planned | Controller command policy, protected-session coherence, trusted redirects, station-ID migration, dependency/action integrity, and adversarial tests | Sol security decisions and representative server behavior |
| M33 Request Transaction Integrity | L | Planned | Fresh station/Queue/account/membership/cooldown/limit checks, exact status contract, station identity, and one-shot submission | Verified station capability data |
| M34 Device, Accessibility, and Pre-launch Acceptance | L | In progress | Human audible TalkBack, alternative input, adaptive/device coverage, Play delivery/update, and pre-launch evidence | Test hardware, humans, and Play artifact |
| M35 Release Signing and Console Eligibility | M | In progress | Protected signer path, physical-device verification, package registration, version-code eligibility, and repeatable signed build | Owner-only Console state |

M31–M33 are audit additions and must close before M39 freezes a candidate. M34 must be rerun where those changes alter
observable behavior. M35's current signing checkpoint is valuable evidence, but it is not a final candidate because the
codebase has changed since that artifact was built.

## Notification delivery program

| ID | Size | State | Implementation focus |
| --- | --- | --- | --- |
| M36 Notification Event-Source Authorization | Architecture gate | Waiting externally | Identify an authorized station event source, webhook, or privacy-compatible relay and document ownership, payloads, authentication, retention/deletion, abuse, outage, and station support |
| M37 Secure Closed-App Notification Delivery | L | Planned after M36 | Minimal opt-in events, station/token isolation, duplicate safety, sign-out/reinstall behavior, and no protected-session forwarding or perpetual polling |
| M38 Notification Lifecycle and Privacy Certification | L | Planned after M37 | Foreground/background/closed/idle/reboot/network testing, latency, battery/traffic, payload privacy, deep-link safety, and all-station coverage |

M27 remains complete for exact-name local notifications produced from the actively observed Chat feed. It is not
closed-app push and does not authorize M37.

## Alpha distribution program

| ID | Size | State | Implementation focus |
| --- | --- | --- | --- |
| M39 Alpha Candidate and Documentation Freeze | M | Planned | Build the current-head signed artifacts, freeze version/listing/release notes/roadmap/testing, verify signer/dependencies/permissions/16 KB packaging, and require no open release-critical finding |
| M40 Play Alpha Delivery and Pre-launch Remediation | M plus external time | Planned after M39 | Upload, inspect Play splits, install/update through Play, complete reviewer access, and reconcile pre-review/pre-launch findings |
| M41 Explicit Alpha Publication | S plus review time | Planned after M40 | Publish only after explicit owner authorization; verify availability, instructions, support, and rollback record |

Publication is never an automatic consequence of a passing local build or CI run.

## Production program

| ID | Size | State | Implementation focus |
| --- | --- | --- | --- |
| M42 Closed-Test Operations and Stabilization | Campaign | Planned after M41 | Maintain qualifying tester continuity, collect structured feedback, fix/retest findings, review Play vitals, and meet exit criteria |
| M43 Production Access and Policy Approval | External gate | Planned after M42 | Submit testing, feedback, change, value, and readiness evidence; reconcile any additional testing request |
| M44 Production Release and Staged Rollout | Campaign | Planned after M43 | Final production candidate, explicit authorization, staged percentages, health thresholds, pause/rollback, release notes, and update validation |
| M45 Operational Reliability and Recertification | Recurring | Planned | Monitor Play quality/support without invasive tracking, recertify station contracts/rights/moderation, maintain policies/toolchain/dependencies, and exercise key recovery |

## Deferred and future programs

M46–M57 are detailed in [future scope](future-scope.md). They are nonblocking for M41 unless the owner explicitly moves
one into the Alpha contract.

- M46 — Architecture Sustainability.
- M47–M50 — repaired and certified Private Messages.
- M51–M54 — Forum links, native reading, authenticated participation, and certification/notifications.
- M55 — Google Cast feasibility and certification.
- M56 — extended station capability certification.
- M57 — account registration, recovery, and management access.

## Validation strategy

- Start with the smallest affected Gradle task and never run routine `clean`.
- Unit-test repository, parser, state, security, and migration behavior at their owning boundaries.
- Use connected tests for Compose semantics, service/MediaSession behavior, lifecycle, permissions, and adaptive UI.
- Use the physical Razr for meaningful phone/fold/accessory/accessibility evidence.
- Use Play-delivered artifacts for M40–M44; a local APK does not substitute for Play split/install/update evidence.
- Keep the manual catalog synchronized at the [Product Testing page](../privacy-site/project/product-testing/index.html).
- Preserve `PT-*` IDs across milestone renumbering and release cycles so tester results remain comparable.
- Record backend-only work as having no meaningful visual change rather than attaching unrelated screenshots.

## GitHub and Discord

- Repository roadmap files, the public portal, and GitHub Project #1 must use the canonical IDs from ROADMAP.md.
- Each GitHub Project card stores one canonical milestone. Legacy IDs appear only in migration/evidence notes.
- Commit only intentional files and preserve unrelated user changes.
- Post to the configured Discord project thread after a completed, committed roadmap milestone or when the user explicitly
  requests a project announcement. Do not announce every push.
- Discord updates must contain no secrets, tester identities, private endpoint details, credentials, or protected evidence,
  and delivery must be verified before reporting success.
