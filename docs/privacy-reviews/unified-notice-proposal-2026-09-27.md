# Unified privacy notice proposal — September 27, 2026

**Status:** Proposal for owner review. Not published. The live notice at `https://24sevenfmplayer.com/privacy/` is
unchanged by this branch.

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

## Decisions still required from the owner

1. **Supported privacy-request contact.** The website notice directs station-side requests to the station's
   Contact/Feedback system. The native notice names an email contact. The facts contract records this as pending
   confirmation, so the website wording is kept unchanged here.
2. **Investigation records.** "Kept for no more than one year" is served today and is recorded as awaiting owner
   attestation. It is kept unchanged here.
3. **Native notice retention wording.** `PRIVACY.md` on `main` says station-side retention periods are unknown. If the
   August 28 attestation stands, `main` should adopt the attested periods so the two notices agree.
4. **Email addresses on the website.** The site validator forbids email links in the published artifact. The native
   notice's email contacts are therefore not carried over.

## Before publication

Publishing this proposal requires the owner's decisions above, a recorded review digest, an updated facts contract, and
the atomic deployment procedure. This branch changes none of those.
