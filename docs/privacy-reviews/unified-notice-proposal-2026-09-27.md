# Unified privacy notice proposal — September 27, 2026

**Status:** Approved by the owner for publication on September 27, 2026, as drafted. The owner also accepted the
recommendations recorded under "Decisions" below; those are a separate follow-up change and are not part of this
publication.

## Purpose

The website notice and the native notice on `main` describe the same application and disagree. This proposal produces
one source by applying the authority rule already recorded in `docs/PRIVACY_RECONCILIATION.md`: the native source
governs statements about application behavior, and owner attestation governs operational statements.

## Native behavior statements adopted from `main`

Each replacement is copied verbatim from `PRIVACY.md` at `main`.

| Statement | Website notice before | Reason |
| --- | --- | --- |
| Local Chat-mention notifications and monitor | Said the app does not "add background polling". | Contradicts the application. It includes an optional, user-started foreground monitor that fetches one station's Chat about once a minute. |
| Community safety and notification preferences | Omitted the stored monitor station. | The application stores one station identifier when the monitor is chosen. |
| App feedback | Absent. | The application prepares a user-reviewed problem-report draft. |
| In-app diagnostics | Omitted the stream-start or station-switch duration. | The snapshot includes it. |
| Foreground special-use service permission | Absent. | The application declares it for the optional monitor. |
| On-device retention paragraph | Omitted the monitor station and feedback drafts. | Follows from the statements above. |

## Operational statements kept from the website notice

| Statement | Basis |
| --- | --- |
| Deletion or anonymization within 30 days; logs, Chat, and request records kept no more than 90 days; backups no more than 90 days | Owner attestation recorded August 28, 2026 in `docs/WEBSITE_FACTS_CONTRACT.json`. |
| Tester-program records removed or anonymized within 90 days | Same attestation. |
| Closed-test tester-interest form section | Reviewed extraction recorded in `docs/privacy-reviews/tester-program-addendum-c00c2f.md`. |

## Decisions

The owner accepted these recommendations on September 27, 2026. None is applied by this publication.

1. **Supported privacy-request contact.** Use the email contact named in the native notice, and keep the station's
   Contact/Feedback system as an alternative. The website shows the address as plain text because the site validator
   forbids email links.
2. **Investigation records.** Remove "kept for no more than one year" unless the network confirms it, and state that a
   record is retained only while an active investigation requires it.
3. **Retention periods.** Both notices must say the same thing. Adopt the 30-day and 90-day periods in the native
   notice if the August 28 attestation was made with the network's authority; otherwise use the "unknown" wording in
   both until the network confirms the periods in writing. Which case applies is still to be confirmed.
4. **Email addresses on the website.** The native notice's email links are not carried over as links.

## This publication

This publication changes only the application-behavior statements listed above. The facts contract's privacy
publication gate remains open for the follow-up change.
