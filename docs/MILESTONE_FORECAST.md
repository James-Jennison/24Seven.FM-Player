# Milestone forecast

Forecast baseline: July 19, 2026. This is the planning companion to [ROADMAP.md](ROADMAP.md): the roadmap owns
scope, dependencies, and acceptance gates; this document records the current best forecast without converting an
external approval into a promised date.

## How to read this forecast

- **Active days** are focused engineering or coordination days after every named input is available. They are not
  elapsed calendar days and do not include waiting for another organization.
- **Conditional** means the range begins only when the listed trigger is true. No calendar date is projected while
  that trigger is outstanding.
- **External** means a station, rights holder, Google Play, or the owner controls the next event. The forecast is a
  response-time target after the input, not a prediction of when the input will arrive.
- Every forecast is revised when its trigger, scope, or policy changes. A range is intentionally widened rather than
  hiding uncertainty behind a single optimistic date.

## Critical path and calendar floor

The current roadmap makes M39 depend on M28–M38. Therefore the Alpha path is blocked by the slowest of these three
external inputs: M29 operator/Console facts, M30 written rights, and M36 an authorized notification event source.
After M36, M37 and M38 remain required before M39 under the current scope. If the owner later chooses to make
closed-app notifications nonblocking for Alpha, that is a scope decision requiring a roadmap change; this forecast
does not assume it.

Once M41 starts a qualifying closed test, a newly created personal Play account has a fixed minimum of 12 opted-in
testers for 14 continuous days before production access can be requested. Google says review of that request usually
takes seven days or less, while allowing that it can take longer. The earliest credible production-access floor is thus
21 calendar days after the qualifying closed test begins, before any remediation or staged-rollout time. See Google's
[current testing requirement](https://support.google.com/googleplay/android-developer/answer/14151465) and
[test-track guidance](https://support.google.com/googleplay/android-developer/answer/9845334).

## Alpha, notification, and distribution forecast

| ID | Trigger to start the range | Forecast | Confidence | Model at the next substantive step |
| --- | --- | --- | --- | --- |
| M29 | All station/operator retention, processor, deletion, IP-use, and reviewer-account facts are supplied; an intended protected candidate is selected | 2–5 active days | Conditional | Sol High, with Terra Medium documentation support |
| M30 | Written authorization covers naming, artwork, metadata, streams, screenshots, and Play testing/distribution | 0.5–1 active day to sanitize, reconcile, and accept | Conditional | Sol High |
| M34 | A charged physical keyboard or pointer is attached and a human completes the remaining fair-input traversal | 0.5–1 active day, plus 0.5 day Sol review | High once hardware is attached | Terra Medium, then Sol High acceptance |
| M36 | An operator supplies an authorized event-source proposal with an accountable owner | 1–3 active Sol days to accept or reject the contract | Conditional | Sol High |
| M37 | M36 contract, event schema, and test environment are accepted | 5–10 active days | Medium | Terra High; return unresolved delivery/privacy choices to Sol High |
| M38 | M37 implementation is stable on all intended stations | 5–10 active days for lifecycle, privacy, battery, and device certification | Medium | Terra High, then Sol High acceptance |
| M39 | M29, M30, M34, and M36–M38 are complete and no intended Alpha work remains | 1–2 active days | High once triggered | Sol High |
| M40 | M39 frozen AAB and required Console fields are ready | 3–10 calendar days; add time only for actual Play/pre-launch findings | Medium | Terra High, with Sol High for release decisions |
| M41 | M40 evidence is reconciled and the owner explicitly authorizes the selected Alpha track | 0.5–1 active day; tester availability follows Play processing | High once authorized | Sol High |

M29, M30, and M36 can progress in parallel. M34 is currently the shortest owner-controlled Alpha gate: human audible
TalkBack already passes, and the remaining keyboard/pointer test can resume as soon as the charged accessory is
connected.

## Production forecast

| ID | Trigger to start the range | Forecast | Confidence | Model at the next substantive step |
| --- | --- | --- | --- | --- |
| M42 | A qualifying closed-test release is available and at least 12 testers have opted in | 3–5 calendar weeks, including the mandatory 14-day continuity window and a feedback/fix buffer | Medium | Terra Medium, with Sol High checkpoint reviews |
| M43 | M42 evidence and the production-access questionnaire are complete | 1–2 calendar weeks; Google's usual review target is seven days or less, but longer remains possible | Medium | Sol Extra High |
| M44 | M43 access is granted and the owner approves a staged rollout plan | 1–4 calendar weeks, chosen by health thresholds and rollout pace | Medium | Sol Extra High |
| M45 | M41 is published | Recurring: monthly light review, quarterly station/policy recertification, and release-triggered review | High | Terra Medium; Sol High for material policy or security changes |

## Post-Alpha and deferred forecast

These milestones are intentionally nonblocking for M41. Their ranges are implementation estimates only; the external
authorization or repair trigger is still authoritative.

| ID | Trigger to start the range | Forecast | Confidence | Model at the next substantive step |
| --- | --- | --- | --- | --- |
| M46 | Alpha feedback identifies a measured architecture or performance hotspot | 1–3 active weeks per approved slice | Medium | Terra Medium; Sol High for major refactors |
| M47 | Site owner repairs private-message delivery and authorizes production verification | No calendar forecast before repair; 1–3 active weeks for protocol certification afterward | External | Sol Extra High |
| M48 | M47 is complete | 1–2 active weeks | Medium | Terra High |
| M49 | M48 is complete and mutation authorization is explicit | 1–2 active weeks | Medium | Terra High |
| M50 | M49 and M38 are complete | 2–4 active weeks | Medium | Sol Extra High |
| M51 | Each station's public Forum route and permitted use are supplied or independently verified | 2–5 active days | Medium | Sol High |
| M52 | M51 and native read authorization are complete | 2–3 active weeks | Medium | Terra High |
| M53 | M52 and authenticated mutation authorization are complete | 2–3 active weeks | Medium | Terra High |
| M54 | M53 and M38 are complete | 2–4 active weeks | Medium | Sol High |
| M55 | Stream-use permission and a compatible receiver are available | 2–4 active weeks; may end with an explicit no-go | Conditional | Sol High |
| M56 | Controlled station evidence and approved test accounts are available | 1–3 active weeks | Medium | Sol High |
| M57 | Station route authorization and account-lifecycle facts are available | 1–3 active weeks | Medium | Sol High |
| M58 | Written station/merchant authority and a viable billing/activation partner are available | 2–4 active weeks for architecture; full contract timing is external | Conditional | Sol Extra High |
| M59 | M58's billing and server-activation contract is accepted | 3–6 active weeks | Medium | Terra High, with Sol Extra High escalation |
| M60 | M59 is complete and all independent station products are available in sandbox | 2–4 active weeks | Medium | Sol Extra High |

## Forecast review triggers

Refresh this document immediately when any of the following changes:

1. A station/operator/rightsholder responds to M29, M30, M36, M47, M51, M55, M56, M57, or M58.
2. A candidate scope decision changes M39's current M28–M38 dependency.
3. The Play Console changes the account's test or production-access requirements.
4. A pre-launch, policy, device, accessibility, or tester finding adds a release-critical remediation.
5. An intended milestone completes, is deferred, or is re-scoped.

No forecast permits bypassing an authorization, security, privacy, station-isolation, testing, or explicit-publication
gate.
