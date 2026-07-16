# M23 Google Play Data Safety worksheet

Checked against the current Google Play definitions on July 15, 2026. This is a conservative implementation worksheet, not a substitute for the product owner's final declaration in Play Console. Reconcile it with the exact active artifact and the Console form immediately before submission.

## Scope and decision rules

- Google Play defines collection as transmitting user data from the app off the device, including transmission directly to a third-party server.
- A transfer initiated by the user to a third party may qualify for the Data Safety **sharing** exception, but the underlying off-device **collection** still needs to be evaluated.
- Data used only on device is outside collection scope.
- Data transmitted for real-time use can be marked ephemeral only when it is kept in memory no longer than necessary to serve the request. Do not assume station-side processing is ephemeral without station confirmation.
- Apps exclusively active on Internal testing are exempt from completing the form. Closed, open, and production tracks are not exempt.

## Implementation inventory

| Data or behavior | Leaves device | Local handling | Provisional Play data type | Required/optional | Purpose | Provisional sharing answer |
| --- | --- | --- | --- | --- | --- | --- |
| Station username/account name | Yes, during sign-in and authenticated requests | Display identity and session linkage encrypted with Android Keystore | Personal info → User IDs | Optional; public playback works signed out | Account management; app functionality | No, if the direct station transfer qualifies for the user-initiated-action exception |
| Password and CAPTCHA/security-code answer | Yes, only during explicit sign-in | Memory only for the attempt; never persisted | Reconcile under User IDs/account information in the active form; no dedicated password type is currently offered | Optional | Account management; app functionality | No, subject to the same user-initiated direct-station transfer analysis |
| Station session cookie | Yes, returned to the originating station on authenticated calls | Encrypted locally with Android Keystore until sign-out/data clear/uninstall | User IDs or other account identifier, depending on the active form wording | Optional | Account management; app functionality | No; sent only to the originating station |
| Chat message | Yes, after explicit Send | Current chat history is memory-only | Messages → Other in-app messages | Optional | App functionality | No, if the explicit user-initiated station post exception applies |
| Optional song-request message | Yes, after explicit confirmation | Pending text is transient | Messages → Other in-app messages | Optional | App functionality | No, if the explicit user-initiated station post exception applies |
| Abuse report: reporter name/email, category, reported username, bounded Chat/request snapshot, timestamp, and optional details | Yes, only after explicit Send report | Form state and submitted report are transient and are not persisted by the app | Personal info → Name and Email address; Messages → Other in-app messages; potentially Other user-generated content under the active form | Optional | App functionality; fraud prevention, security, and compliance | No, if the explicit user-initiated direct-station transfer exception applies; confirm against the active form |
| Track request/selection action | Yes, after explicit confirmation | No developer backend; station-reported results are transient | App activity → Other actions | Optional | App functionality | No, if the explicit user-initiated station action exception applies |
| Station-library search text | Yes, when Search is explicitly used | Results and query UI state are transient | App activity → In-app search history | Optional | App functionality | No, if the direct station query qualifies for the user-initiated-action exception |
| Public playback, queue, history, artwork, chat, and station catalog responses | Requests leave the device, but the returned public content is not user data supplied by this app | Cached only by normal in-memory/network/media mechanisms | No user-data type identified | Core functionality | App functionality | No |
| Favorite lists, request activity, and membership indicators returned by a station | The user identifier needed to fetch them is covered above | Memory-only and cleared from UI on sign-out | No additional outgoing type beyond User IDs identified | Optional | App functionality | No |
| App preferences, selected/default/last station, adult age-screen result, accepted Terms version, community-content visibility, station-scoped blocked identities, and UI state | No developer or analytics server | On-device preferences only; date of birth is not saved; all app-private data is excluded from Android cloud backup and device-to-device transfer | Out of scope for collection | Core/optional by setting | App functionality; safety | No |
| Crash, analytics, advertising, location, contacts, files, microphone, camera, phone, or SMS data | No | No corresponding SDK or permission | Not collected | N/A | N/A | No |

## Provisional form posture

- **Does the app collect or share required user-data types?** Yes—declare the applicable optional types above because they leave the device when account/community/request features are used.
- **Is all collected data encrypted in transit?** Answer Yes only after final release probes confirm every endpoint carrying the listed user data remains HTTPS/TLS across all five stations and redirects. Otherwise answer No until corrected.
- **Can users request deletion?** The app can delete its protected local session through Sign out or Android data clearing. It does not create or control the pre-existing station account or station-side posts. The public privacy page must explain the station/operator boundary and provide an appropriate contact path before claiming a broader deletion mechanism.
- **Ephemeral processing:** Treat local password, security-code, search, and pending-message handling as transient, but do not mark their end-to-end collection ephemeral unless station-side retention is confirmed.
- **Independent security review:** No.
- **Ads/analytics:** None.

## Console verification gate

Before completing the form:

1. Re-run the release dependency and merged-manifest audits against the exact signed AAB.
2. Probe every authenticated/user-submission endpoint for HTTPS-only transmission and redirect behavior without recording credentials or cookies.
3. Confirm with station administration whether server logs or feature storage retain search terms, login submissions, chat/request messages, request actions, and abuse reports.
4. Compare every question in the active Console form with this table; the Console wording is authoritative.
5. Have the product owner attest to the final answers before saving or submitting them.

Official references:

- [Provide information for Google Play's Data Safety section](https://support.google.com/googleplay/android-developer/answer/10787469)
- [Google Play User Data policy](https://support.google.com/googleplay/android-developer/answer/10144311)
- [Testing requirements for new personal developer accounts](https://support.google.com/googleplay/android-developer/answer/14151465)
