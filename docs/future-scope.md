# Deferred and future product scope

This file expands M46–M60 from the canonical [roadmap](ROADMAP.md). None of these milestones blocks M41 Alpha
Publication unless the owner explicitly promotes it into the Alpha contract. All remain fully native; no phase may add
a WebView, share Administrator sessions, or infer one station's capability from another.

## M46 — Architecture Sustainability

- Split oversized Compose and ViewModel files only along established feature, immutable-state, and repository boundaries.
- Preserve service-owned playback, protected-session isolation, and station capability flags.
- Add modules only when a boundary is stable enough to justify the build and ownership cost.
- Use performance evidence before adding Baseline Profile, Macrobenchmark, or optimization infrastructure.
- Preserve source compatibility and regression evidence while improving testability and iteration speed.

## M47–M50 — Private Messages

### M47 — Server Repair and Protocol Certification

Private Messages remain deferred. Resume only after the site owner repairs the reproduced server delivery failure and
production behavior can establish authenticated routes, send limits, station isolation, error/indeterminate results,
moderation boundaries, and representative accounts. Do not expose a partial shipping interface.

### M48 — Native Private Message Reading

- Define bounded station-scoped Inbox/Sent domain models and repository contracts.
- Reuse only the originating protected station session.
- Provide loading, empty, stale, expired, error, and unsupported states.
- Keep message content memory-bounded and exclude it from diagnostics, logs, notifications, and public evidence.

### M49 — Compose, Reply, and Send

- Research CSRF, reply/recipient discovery, encoding, server limits, and success/rejection/indeterminate responses.
- Require explicit preview and confirmation.
- Submit once with no automatic retry; an indeterminate result directs the user to inspect Inbox/Sent before any action.
- Keep drafts and tokens transient unless a separate encrypted-draft design is approved.

### M50 — Five-Station and Notification Certification

- Certify read/send/logout/expiry independently for all five stations.
- Validate device, accessibility, lifecycle, and account-isolation behavior.
- Add new-message notifications only through M36–M38 after JERIC authorizes the official station-app program and their authorization and privacy gates pass.

## M51–M54 — Retired Forum scope

Forum access is permanently excluded from the Player by project decision. M51–M54 remain reserved historical IDs so
past research, roadmap references, and tester evidence stay traceable; they have no implementation forecast and must
not be revived by station authorization alone. The Player must not expose Forum links, Custom Tabs, browser fallback,
native Forum retrieval, authenticated Forum participation, or Forum notifications. The July 19 route and policy audit
remains in `m51-forum-links-research.md` as historical evidence only.

## M55 — Google Cast Feasibility and Certification (retired)

Google Cast is permanently out of Player scope by project decision. M25's Android-managed audio-output path remains
the supported boundary for local speakers, Bluetooth, wired/USB, and other system-managed device routes.

- Do not add a Google Cast sender, receiver application, Cast control, relay, rebroadcast proxy, or Cast test scope.
- Preserve M25's Media3 ownership and Android system output selection; these are not Google Cast implementations.
- Retain the prior M25 route-boundary documentation as historical evidence only; M55 has no implementation forecast
  and must not be revived by station authorization alone.

## M56 — Extended Station Capability Certification

- Independently research non-SST request messages, request activity/cooldown, membership, and other station differences.
- Use explicit unsupported states where live evidence is absent.
- Do not enable capability flags from structural similarity or a different station's result.
- Add parser fixtures, repository tests, authenticated device evidence, and privacy/declaration updates for each capability.

## M57 — Account Registration, Recovery, and Management Access

- Verify exact public HTTPS registration, password recovery, account management, and deletion routes per station.
- Reconcile every route with M31 payments/account-creation/deletion compliance before exposing it.
- Prefer trusted Custom Tabs initially; app and browser sessions remain separate.
- A native interface requires explicit authorization, repository contracts, protected-session design, error/indeterminate
  behavior, privacy review, and independent five-station certification.

## M58–M60 — VIP/RIP membership commerce program

The current Alpha has no membership purchase, payment, activation, or account-registration route. This future program
may add a fully native purchase and activation experience only after the legal, station, merchant, Play, and server
contracts are explicit. It must never trust a client-only entitlement or share credentials with the Administrator app.

### M58 — Membership Commerce Authorization and Billing Architecture

Recommended model: **Sol Extra High** because this milestone controls payment policy, authorization, protected account
state, external systems, and release eligibility.

- Obtain written station-owner authorization to sell and activate VIP/RIP for each station and identify the merchant of
  record, product owner, support owner, and financial/reconciliation owner.
- Choose Google Play Billing or a formally enrolled and approved regional alternative. Document country/build gating,
  disclosures, fees, taxes, refunds, chargebacks, cancellation, and customer-support obligations.
- Define station-specific products, durations, prices, renewal behavior, account prerequisites, and whether one purchase
  can ever apply to more than one station. Never infer one station's offering from another.
- Establish an authorized server-side purchase-verification and entitlement-activation contract. The Android client
  must not grant VIP/RIP from a local purchase result alone.
- Threat-model replay, duplicate purchase, pending payment, account mismatch, station mismatch, refund/revocation,
  device change, reinstall, offline, outage, and indeterminate activation behavior.
- Reconcile privacy, Data Safety, account deletion, Play declarations, Terms, support, and release evidence before M59.

### M59 — Native VIP/RIP Purchase and Activation

Recommended model: **Terra High**, returning to Sol Extra High for any unresolved billing, authorization, activation,
privacy, or station-contract decision.

- Implement only the M58-approved billing path in native Kotlin/Compose; do not add a WebView or unapproved external
  payment link.
- Keep purchase and entitlement state immutable, station-scoped, and account-scoped behind repository contracts.
- Cover product loading, eligibility, explicit confirmation, purchase launch, pending, acknowledgement/consumption as
  appropriate, server verification, activation, restore, already-owned, cancellation, rejection, and indeterminate
  results without automatic duplicate purchase.
- Show verified entitlement state from the trusted station/server contract and keep Administrator credentials,
  sessions, endpoints, and capabilities outside the Player.
- Add sandbox/unit/instrumentation evidence without committing purchase tokens, tester identities, financial data, or
  private station endpoints.

### M60 — Subscription Lifecycle and Five-Station Certification

Recommended model: **Sol Extra High** for the final security, policy, lifecycle, and release acceptance review.

- Certify VIP independently for StreamingSoundtracks.com, 1980s.FM, Adagio.FM, and Entranced.FM, and RIP independently
  for Death.FM; unsupported offerings remain explicit and unavailable.
- Validate renewal, non-renewing expiry, cancellation, grace/account hold where applicable, pending completion, refund,
  chargeback, revocation, upgrade/downgrade if supported, restore, reinstall, device switch, and station/account switch.
- Prove that activation is idempotent, server-verified, privacy-safe, isolated from other stations, and removed or
  downgraded when the trusted entitlement source requires it.
- Complete physical-device, adaptive-layout, 2× text, assistive-access, network/lifecycle, Play sandbox, support,
  refund/cancellation, privacy, and Console evidence before any production commerce rollout.
