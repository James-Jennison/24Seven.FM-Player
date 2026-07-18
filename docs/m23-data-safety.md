# M29 Google Play Data Safety worksheet

Checked against the current Google Play definitions and the release variant on July 18, 2026. This is a conservative
implementation worksheet, not the final owner declaration. Reconcile it with the exact signed candidate and active
Console form immediately before submission.

## Scope and decision rules

- Google Play defines collection as transmitting user data from the app off the device, including transmission directly
  to a third-party server.
- Google Play defines sharing separately. On-device transfer to another app counts as sharing unless an exception applies.
  A specific user-initiated action where the user reasonably expects the transfer can qualify for the exception; document
  the reason rather than assuming it.
- Data used only on device is outside collection scope.
- Data transmitted for real-time use can be marked ephemeral only when it is kept in memory no longer than necessary to
  serve the request. Do not assume station-side processing is ephemeral without station confirmation.
- The all-data-encrypted-in-transit answer is Yes only when every collected/transmitted user-data type uses encryption.
- Apps exclusively active on Internal testing are exempt from completing the form. Closed, open, and production tracks
  are not exempt, and the disclosure covers every version distributed through Google Play.
- A deletion-mechanism badge requires a real mechanism or qualifying automatic deletion/anonymization. Local Sign out
  is not a station-account or station-content deletion mechanism.

## Exact artifact basis

- The release manifest contains no sensitive-data permission beyond Internet/network state, notifications, and the
  foreground-media permissions needed for playback.
- The release dependency graph contains no advertising, analytics, crash-reporting, tracking, billing, or social-login SDK.
- The Player has no developer-operated server.
- Credentials, sessions, Chat, search, Favorites, request, Queue, History, and artwork paths enforce approved same-station
  HTTPS behavior. Final probes must repeat against the exact candidate.
- Only verified public live-audio hosts permit cleartext. Player-added credentials, cookies, messages, search terms,
  reports, and diagnostics are never attached to those stream requests.
- App-private data is excluded from Android cloud backup and device-to-device transfer.

## Implementation inventory

| Data or behavior | Transfer and local handling | Provisional Play type | Required / purpose | Sharing posture | Unresolved fact |
| --- | --- | --- | --- | --- | --- |
| Station username/account name | Sent to the selected station during sign-in/authenticated requests; display identity and session linkage are Keystore-protected locally | Personal info → User IDs | Optional; account management and app functionality | Direct station transfer may qualify for the user-initiated exception; reconcile in Console | Station retention, processors, deletion path |
| Password and CAPTCHA/security-code answer | Sent only during explicit sign-in; memory-only for the attempt and never persisted | Reconcile under account information/User IDs because the current form has no dedicated password type | Optional; account management and app functionality | Evaluate the explicit direct-station transfer against the current exception wording | Login/security log retention and processors |
| Station session cookie | Returned only to the originating station; encrypted locally until sign-out, data clear, or uninstall | User IDs or other account identifier, depending on active wording | Optional; account management and app functionality | No unrelated destination; reconcile direct-station treatment | Session/access-log retention and deletion |
| Chat message | Sent after explicit Send; loaded history is memory-only | Messages → Other in-app messages | Optional; app functionality | Evaluate the explicit station post under the user-initiated exception | Post/log retention, moderation access, deletion |
| Optional song-request message | Sent after explicit request confirmation; pending text is transient | Messages → Other in-app messages | Optional; app functionality | Evaluate the explicit station post under the user-initiated exception | Request/message retention and deletion |
| Song-request/track action | Sent once after explicit confirmation; result state is transient | App activity → Other actions | Optional; app functionality | Evaluate the explicit station action under the user-initiated exception | Action/request history retention and deletion |
| Station-library search text | Sent only when Search is explicitly used; query/results UI is transient | App activity → In-app search history | Optional; app functionality | Evaluate the direct station query under the user-initiated exception | Search/log retention and whether terms are linked to an account/IP |
| Abuse-report email handoff | Player transfers a bounded fixed-recipient draft to the chosen email app; Player does not persist it or send/read email | Personal info → Name/Email; Messages → Emails or Other in-app messages; possibly Other UGC under active wording | Optional; app functionality, security, and compliance | On-device app-to-app transfer is sharing, but the explicit Review email/send flow may qualify for the specific user-initiated exception | Email-app and receiving administration retention/processors/deletion |
| Diagnostic Copy/Share | Generated locally from a fixed allowlist; Copy uses Android clipboard and Share opens the chooser; no automatic upload or persistence | On-device processing is out of collection scope; selected external transfer must still be evaluated | Optional; support/app functionality | Explicit user-selected destination is provisionally within the user-initiated sharing exception | Confirm active Console treatment |
| Source IP/network identifier | The station/CDN necessarily receives connection-level network information; Player does not read or persist the source IP | Declare only according to actual station/CDN use, such as approximate location if inferred | Core/optional depending on request; app functionality and possibly security | Recipient and processor treatment depends on station/CDN facts | Whether IP is retained, shared, used for location/security, and deletion rule |
| Public playback, Queue, History, artwork, Chat, and station catalog responses | Requests leave device; returned public content is not user data supplied by this app | No additional user-data type identified | Core functionality | No additional sharing identified | Normal IP/server logging covered above |
| Favorites, request activity, and membership indicators | Existing account identifier fetches station-owned values; returned UI state is memory-only and clears on sign-out | No additional outgoing type beyond User IDs identified | Optional; app functionality | No additional sharing identified | Station-side retention is covered by account/request facts |
| App preferences and safety state | Selected/default/last station, age-screen result, Terms version, community reveal, blocked identities, mention opt-ins, and UI state remain local; date of birth is not saved | On-device processing outside collection | Core/optional; app functionality and safety | No | None for Data Safety; public/local retention remains disclosed |
| Local mention matching | Exact name matching, first-snapshot baselining, blocked filtering, and bounded SHA-256 fingerprints occur on-device; notification omits message text | On-device processing outside collection | Optional; app functionality | No additional transfer | None unless future closed-app delivery changes architecture |
| Crash, analytics, ads, location sensors, contacts, files, microphone, camera, phone, or SMS | No corresponding SDK, permission, or developer endpoint | Not collected | N/A | No | Re-audit the final candidate |

## Provisional Console posture

- **Does the app collect or share user-data types?** Yes. Declare applicable optional off-device types; do not answer No
  merely because data goes directly to stations instead of a developer backend.
- **Optional or required?** Account/community/request features are optional because public playback, Queue, and History
  work signed out. Network data needed for the user's selected request may still be required for that feature.
- **Is all collected data encrypted in transit?** Provisionally Yes for explicit user-data payloads based on the current
  source audit, but final release probes across every station/redirect are mandatory. Public cleartext live audio carries
  no Player-added user payload; confirm the active form's treatment before saving Yes.
- **Can users request deletion?** The app can remove local protected sessions and application data. It cannot presently
  delete pre-existing station accounts, posts, request records, server logs, or sent email. Do not claim a broader
  mechanism until the station supplies one. M31 must also confirm that no external route enables account creation.
- **Ephemeral processing:** Password/security-code, local search UI, and pending-message handling are transient inside the
  Player, but end-to-end processing is not known to be ephemeral while station retention is unknown.
- **Independent security review:** No.
- **Ads/analytics:** None.

## Owner/station fact table

The final answer is blocked until the authorized operator provides these facts for all five stations, or explicitly
confirms one network-wide rule:

| Process | Required fact |
| --- | --- |
| Sign-in, session, and access/security logs | Retention duration/deletion rule, access roles, processors, and user request path |
| Source IP/network logs | Retention, purpose, whether location is inferred, processors, and deletion rule |
| Search | Whether terms are logged/linked, retention, processors, and deletion rule |
| Chat | Post/log retention, moderation access, processors, edit/deletion path |
| Song requests/messages/activity | Retention, account linkage, processors, and deletion path |
| Moderation email | Recipient/email-provider retention, access, processors, and request/deletion path |

## Console verification gate

1. Re-run the release dependency and merged-manifest audits against the exact signed M39 AAB.
2. Probe every user-data endpoint and redirect for HTTPS/TLS without recording credentials, cookies, or private content.
3. Confirm the owner/station facts above and update the public/native privacy wording where needed.
4. Audit every M31 external destination for account creation/deletion implications.
5. Compare every active Console question with this worksheet; Console wording is authoritative.
6. Have the product owner attest to the final answers before saving or submitting them.
7. Retain only sanitized completion evidence in Git.

Official references:

- [Provide information for Google Play's Data Safety section](https://support.google.com/googleplay/android-developer/answer/10787469)
- [Google Play User Data policy](https://support.google.com/googleplay/android-developer/answer/17190352)
- [Account deletion requirements](https://support.google.com/googleplay/android-developer/answer/13327111)
