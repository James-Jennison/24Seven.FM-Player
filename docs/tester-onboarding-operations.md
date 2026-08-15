# Tester recruitment and closed-testing operations

Last reviewed: 2026-08-14

This document matures the existing tester system. It does not establish a second distribution path or a replacement portal.

## Product identity

24Seven.FM Player is an independently developed, unofficial player for the 24Seven.FM network of internet radio stations. The developer may be an authorized network/station administrator, but the Player is not represented as published, owned, sponsored, or endorsed by the network or an individual station.

## Existing-system inventory

| Component | Decision | Existing behavior retained or extended |
| --- | --- | --- |
| Public tester-interest form | EXTEND | Keeps same-origin validation, Turnstile, rate limiting, honeypot, bounded input, consent, and private mail delivery. Adds validated recruitment-source attribution. |
| Private SQLite tester roster | EXTEND | Keeps the email-deduplicated roster, mailbox-import idempotency, existing coverage fields, and inactive state. Adds source attribution without a duplicate roster. |
| Private coordinator queue | EXTEND | Keeps password + TOTP protection, CSRF, individual mail handoff, assignment safety controls, and mail archive. Shows recruitment source and tester-provided opt-in/smoke evidence. |
| Tester portal | EXTEND | Keeps one-time-link authentication, separate session, no roster access, profile updates, opt-in confirmation, and private feedback. Adds a bounded first-use smoke confirmation and feedback categories. |
| Tester task registry | KEEP | Remains the single assignment and PT-case authority. `TT-01` through `TT-06` provide the account-free baseline; no replacement checklist is treated as a task result. |
| GitHub structured test form | KEEP | Remains a public, opt-in supplementary feedback route. The private portal is the confidential route for assigned-task reports. |
| Google Play link | KEEP | Google Play Closed Testing is the only qualifying external distribution path. No recruitment source supplies an alternate build. |
| Crash reporting / Firebase | KEEP ABSENT | No Firebase, analytics, or crash SDK is present. Do not add one without a privacy, Data Safety, architecture, and dependency review. |

### What exists today

Applicants can provide their Google Play email, Android coverage, optional station familiarity, safe testing preferences, and consent. The coordinator can review a private roster, send individual orientation and assignment emails, assign only permitted task bundles, and review portal feedback. Testers can use a short-lived sign-in link to update their own profile, self-confirm a completed Google Play opt-in, complete the short initial smoke check, and submit private assigned-task feedback.

An invitation or a mail-transport acceptance does **not** prove a Play opt-in, installation, or ongoing use. Those states remain explicitly unknown unless the tester self-confirms them or the operator has separate Play Console evidence.

## Cohort and recruitment model

Operate toward 20–25 active, opted-in testers, with recruiting headroom for normal attrition. The source labels are deliberately small and validated:

- `testers-community` — primary free baseline recruitment;
- `betabound` — qualitative / enthusiast recruitment;
- `betafamily` — supplemental coverage-gap recruitment only;
- `direct` and `other` — manual or approved additional recruitment.

All sources use the same public application and converge on the same private roster, Google Play closed-test opt-in, tester portal, task registry, and feedback path. Source attribution changes neither privileges nor the build a tester receives. Duplicate records are rejected by the case-insensitive Google Play email identity in the existing roster.

Suggested source links are:

```text
https://player.jamesjennison.net/product-testing/?source=testers-community
https://player.jamesjennison.net/product-testing/?source=betabound
https://player.jamesjennison.net/product-testing/?source=betafamily
```

Unknown query values fall back to `direct`; the server validates the resulting form value again.

## Lifecycle evidence

The existing onboarding state remains the coordinator-controlled workflow. The following evidence is recorded alongside it rather than inventing a competing status system:

| Lifecycle point | Evidence | Do not infer |
| --- | --- | --- |
| Applicant | Valid private application imported to roster | Acceptance or Play eligibility |
| Profile complete | Existing coverage-completeness check | Play access |
| Play access granted | Coordinator records an invitation / orientation step | Opt-in or installation |
| Opted in | Tester’s dated portal self-confirmation | Installation or use |
| Initial test completed | Tester’s dated smoke-test self-confirmation | Broader task completion |
| Active testing | Assigned task plus feedback / coordinator evidence | Activity from an email address alone |
| Withdrawn / deletion requested | Support request; coordinator must deactivate portal access before completing the request | Automatic deletion or a legal retention outcome |

The five-minute initial smoke check is: launch, play a station for a few minutes, switch stations, briefly background playback and exercise Android media controls, return to the Player, then report any failure or confusion. Distribute station coverage across StreamingSoundtracks.com, 1980s.FM, Adagio.FM, Death.FM, and Entranced.FM through the existing `TT-02` assignment.

## Feedback and safety

The private portal’s existing feedback storage now categorizes reports as playback, connection/reconnection, station switching, metadata/artwork, favorites, media controls, audio accessories, layout/accessibility, performance/battery, crash/freeze, or other. It stores only the tester’s entered report plus the existing task association and timestamp. It does not attach logs, credentials, tokens, private files, or device identifiers.

Testers must never create, share, or submit station credentials. Guest testers remain limited to account-free tasks. Death.FM membership terminology remains station-specific: do not conflate RIP and VIP features.

## Privacy, retention, and deletion

The existing public privacy notice covers the private application/queue and deletion-support route. The new source label, self-confirmation timestamps, and feedback category are collected only to manage recruitment, cohort coverage, and focused testing; they are not analytics or advertising data.

The adopted tester-program policy is to delete or anonymize a withdrawn or rejected applicant’s private tester record, related task feedback, and invitation correspondence within 90 days of a verified request or decision. An active tester’s record is retained only through the closed test and for no more than 90 days after the program closes. Only irreversibly anonymized aggregate coverage and test counts may remain afterward. A documented security, abuse, or legal hold is a separate, time-bounded exception.

The tester portal records dated withdrawal and record-deletion requests; the protected coordinator queue shows those requests without treating them as already completed. The coordinator verifies the requester, stops program access promptly for a withdrawal, and completes deletion or anonymization within the 90-day policy period. No request should contain credentials or authentication secrets. These tester-program records are separate from any station-account data, which remains under the applicable station or network process.

## Google Play requirements checked

Verified 2026-08-14 from Google Play Help:

| Requirement | Applicability |
| --- | --- |
| Closed tests can use email lists or Google Groups; testers need a Google or Workspace account; Console exposes the feedback URL/email on the opt-in page. | Applies to the planned closed track. |
| A personal developer account created after 2023-11-13 needs at least 12 testers continuously opted in for 14 days before it can apply for production access. | Applies only if this developer account meets Google’s personal-account/date condition. Confirm in the owner’s Console; the repository cannot prove that private account fact. |
| Pre-launch reports are capacity-dependent and assess stability, compatibility, performance, and accessibility for uploaded artifacts. | Use when the Play artifact is uploaded; not a substitute for human closed testing. |
| Closed/open/production tracks require an accurate Data Safety form and privacy policy; internal-only testing is exempt. | Applies before closed-track release. |

Sources: [test tracks](https://support.google.com/googleplay/android-developer/answer/9845334), [new personal-account testing](https://support.google.com/googleplay/android-developer/answer/14151465), [pre-launch reports](https://support.google.com/googleplay/android-developer/answer/9842757), and [Data Safety](https://support.google.com/googleplay/android-developer/answer/10787469).

## Reusable recruitment copy

Use this baseline for Testers Community, Betabound, or BetaFamily. Do not submit it externally without the owner’s approval.

> Help test 24Seven.FM Player, an independently developed, unofficial Android player for the 24Seven.FM network of internet radio stations. Testers join one Google Play Closed Test, use their own Google account to opt in, complete a short first-use smoke test, and use the app normally for about two weeks. Feedback on playback, station switching, media controls, device compatibility, accessibility, and usability is especially useful. No station account is required for guest testing, and you must never share credentials. Apply at the tagged program link supplied for this recruitment source.

For BetaFamily, request only a specific manufacturer/form factor/Android-version gap shown by the private coverage view. Do not buy testers or promise rewards without explicit owner authorization.

## Closed-test readiness gates

- exact tested build and relevant automated validation pass;
- public application, private portal, opt-in instructions, and feedback path work;
- independent/unofficial disclosure and privacy notice are accurate;
- Data Safety and Console feedback configuration are reconciled for the selected track;
- no known blocker playback issue; all five stations have planned coverage;
- operator preserves evidence without recording secrets or tester identities in Git.

Production readiness additionally requires the applicable Google requirement, cohort headroom, feedback and blocker review, representative Android coverage, Pre-launch Report review where available, and a release candidate descended from the tested lineage. A Console production-access control is not itself a release approval.
