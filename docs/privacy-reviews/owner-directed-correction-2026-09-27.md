# Owner-directed privacy notice correction — September 27, 2026

## Direction

On September 27, 2026, the owner directed that the website privacy notice:

1. include the Google Cast SDK diagnostics disclosure; and
2. stop describing the Player as pre-release software.

## Changes to `PRIVACY.md` on the website source

| Served wording | Replacement | Source of replacement |
| --- | --- | --- |
| "The application does not include advertising, analytics, crash-reporting, tracking SDKs, or a developer-operated data server." | The same sentence corrected to "developer-operated analytics", followed by the Google Cast Android Sender SDK diagnostics disclosure. | Copied verbatim from `PRIVACY.md` on `main`, where it was introduced for the first production candidate. |
| Heading "Alpha limitations" and "This is pre-release software. Testers should use a non-administrator station account where practical and should not include credentials, private messages, session values, or security-code images in bug reports." | Heading "Reporting problems" and "When reporting a problem, do not include credentials, private messages, session values, or security-code images. Closed-test participants should use a non-administrator station account where practical." | Written for this correction; the guidance is unchanged and only the pre-release claim is removed. |
| "The current Alpha exposes only **Contact Us**" | "The current Player exposes only **Contact Us**" | Matches `PRIVACY.md` on `main`. |
| "Last updated: August 15, 2026." | "Last updated: September 27, 2026." | Date of this correction. |

## Unchanged

- Station-side retention, deletion, and backup statements are unchanged.
- The closed-test tester-interest form section is unchanged.
- The Android TV alpha notice at `/privacy/tv/` is unchanged.

## Open items

This correction does not close the privacy publication gate in `docs/WEBSITE_FACTS_CONTRACT.json`. That gate still
records a pending confirmation of the supported privacy-request contact and one served retention claim awaiting owner
attestation. The website notice and the native notice on `main` also still differ on station-side retention wording
and contact details; reconciling them remains a separate reviewed change.
