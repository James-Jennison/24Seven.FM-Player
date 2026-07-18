# M28 UGC safety implementation validation

Date: July 18, 2026

Status: complete

## Delivered native safeguards

- Community Chat and request attribution are hidden by default and are not fetched for display until the user completes an adult age screen, accepts the current Terms of Participation, and separately chooses to reveal mature community content.
- Terms acceptance is versioned. Updating `CURRENT_COMMUNITY_TERMS_VERSION` requires renewed acceptance before community content can be viewed or contributed.
- Chat messages and Queue/History requester attribution expose separate **Report content**, **Report user**, and **Block user** actions.
- Blocks are device-local, station-scoped, identity-normalized, immediately filter Chat and requester attribution, and can be reviewed or removed under More → Community safety.
- The report dialog prepares only the authorized bounded fields as a fixed-recipient email draft. Reporter-entered names, content snapshots, optional details, and drafts are never persisted by the app.
- Android `ACTION_SENDTO` opens the user's email app for review and explicit sending. The Player has no email credentials or silent sender and never describes composer opening as proof of sending or delivery.
- Contact Us uses the same allowlisted recipient for every selected station while the draft subject/body retain the selected station identity. Approved non-Contact pages continue to use same-origin HTTPS Custom Tabs.

## Persistence and privacy boundary

Persisted on device:

- adult/not-adult age-screen result;
- accepted Terms version;
- community-content visibility preference; and
- station-scoped blocked display identities.

Not persisted:

- date of birth;
- reporter name or station nickname;
- reported content snapshot, category, or details;
- prepared email subject/body; or
- email-composer or delivery state after the transient dialog is dismissed.

## Verification

- Debug Kotlin compilation: passed.
- Targeted contact/report draft, fixed-recipient trust, station-catalog, repository, and Compose tests: passed.
- Android-test Kotlin compilation: passed.
- API 35 Pixel Fold half-open and closed-outer-display connected suites: 33/33 passed in each state, including the complete age → Terms → reveal flow, separate report/block actions, native report fields, and persistence boundaries.
- API 35 Pixel Tablet portrait connected suite: final 34/34 passed, including explicit UI verification that indeterminate delivery offers **Done** and never **Try again**.
- Historical Contact Us form evidence remains recorded: one authorized harmless StreamingSoundtracks.com report was submitted once, returned an indeterminate response, and was not retried.
- On July 18, 2026, the owner confirmed that a dedicated moderation destination is monitored and received a separately sent sanitized harmless report. Original evidence is retained privately.
- Debug Kotlin compilation after the email-handoff implementation: passed.
- Full debug unit suite: 147/147 passed.
- Debug lint: passed with no blocking findings.
- Debug APK assembly and streamed update install on the physical Motorola Razr 2023/API 36: passed.
- M28-focused Razr connected suite: 6/6 passed, covering transient repository state, bounded fixed-recipient drafts, Contact Us semantics, report input, and honest email-handoff copy.
- A broader 48-test Razr run passed 47 tests; the sole failure was the existing simulated 701 dp tablet-navigation assertion running under the phone's physical root constraints. It is tracked as M34 device-matrix evidence and did not affect the six M28 checks.
- Physical Contact Us handoff: the Razr offered compatible email apps; Gmail displayed the exact monitored recipient, `StreamingSoundtracks.com` subject/body context, and editable draft. The first probe exposed Gmail ignoring plain intent extras, so the implementation was corrected to encoded `mailto:` subject/body parameters and retested successfully. The draft was discarded without sending, and the existing Player task/station state remained intact on return.
- Physical report delivery: on July 18, 2026, the owner authorized one clearly labeled harmless M28 report from the installed Razr build. The fixed recipient, station-scoped subject, bounded report body, and explicit user-controlled Send action were reviewed; the message was sent exactly once, the Player made no delivery claim, and the owner confirmed receipt at the monitored destination. No retry was performed.

## Completion decision

- M28 is accepted complete. The native safeguards, fixed-recipient handoff, privacy boundary, honest composer state, physical-device behavior, one-shot sending rule, and monitored-destination receipt have all been validated.
- Repository evidence remains sanitized and contains no mailbox headers, sender identity, report content, or private correspondence.

This evidence supports implementation readiness but does not itself constitute legal advice, a guarantee of Google Play approval, or completion of the owner-controlled Play Console declarations.
