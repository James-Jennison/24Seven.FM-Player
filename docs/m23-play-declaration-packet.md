# M29 Google Play declaration packet

Date: July 19, 2026

Status: exact-artifact and current-policy audit complete; owner, station, final-candidate, and Play Console gates remain

This packet is the non-secret working record for M29. Reconcile every answer with the exact signed AAB and active Play
Console wording immediately before saving or submitting it. Never commit reviewer credentials, Console captures containing
private data, private correspondence, or an unlisted reviewer-video URL that exposes account information.

## Exact artifact audit — July 18, 2026

The audit used the current `main` source and release variant. `:app:processReleaseMainManifest` and the
`releaseRuntimeClasspath` dependency report both passed.

| Surface | Audited fact | Declaration consequence |
| --- | --- | --- |
| Identity | Package `com.codeframe78.twentyfourseven.player`; version `0.1.0-alpha01` (2); min SDK 26; target/compile SDK 36 | Re-run against the exact protected pre-M39 candidate because later source or version changes supersede this checkpoint |
| Permissions | Internet, network state, media foreground service, media-playback foreground-service permission, notifications, Media3 wake lock, and Android's signature-protected dynamic-receiver permission | No location, contacts, microphone, camera, photos, phone, SMS, advertising ID, or broad-storage declaration is supported by the artifact |
| Foreground service | One Media3 `MediaSessionService`, declared `mediaPlayback`, owns user-started live radio | Declare only the Media playback use case and supply its final-candidate demonstration video |
| Dependencies | AndroidX, Compose, Media3, Jsoup, Coil, OkHttp, Kotlin/coroutines, and test libraries | No ads, analytics, crash-reporting, tracking, billing, social-login, or developer-backend SDK is present |
| Storage | Station sessions/display identities use Android Keystore protection; preferences are local; app-private data is excluded from Android backup and device transfer | Describe local retention accurately and do not imply that Sign out deletes station-side accounts or content |
| Network | Credentials, sessions, Chat, search, Favorites, request, Queue, History, artwork, and other app data use approved same-station HTTPS paths with same-origin enforcement | Final probes must confirm that every user-data path and redirect remains HTTPS before answering that all collected data is encrypted in transit |
| Live audio | Only the five verified public live-radio hosts are permitted cleartext; those requests carry no Player-added credentials, cookies, Chat/request text, or report content | Do not describe all network traffic as HTTPS; distinguish unauthenticated public live audio from user-data transmission |
| External handoffs | Contact/report opens a fixed-recipient email draft; diagnostics Copy/Share uses Android clipboard/chooser; M31 removed station browser routes from the candidate | Reconcile on-device transfer to another app and the specific user-initiated sharing exception in the active Data Safety form |
| Accounts | The app signs into independently existing station accounts and has no native or linked account-creation or station-side deletion operation | M31 verified the candidate's Contact-only external boundary; re-audit this row if M51, M57, or M58–M60 later adds a route |

## Console decision ledger

| Console area | Current evidence-backed posture | Remaining gate |
| --- | --- | --- |
| Ads, government, financial, health | Saved as No/None; artifact audit agrees | Recheck only if the candidate or Console wording changes |
| App access | Public playback, Queue, and History work signed out; protected features require one station-specific session at a time | Supply and validate five reusable, least-privileged reviewer accounts only in Play Console |
| Target audience | Owner selected 18+; the app uses a neutral age screen and is not designed for children | Verify the saved selection and decide whether to enable Restrict Minor Access |
| Content rating | Music & Audio app with public text Chat, requester identity/message, possible mature themes/profanity, and no user image/video upload | Owner must answer actual content frequency/intensity and submit the generated IARC ratings |
| UGC | M28 provides Terms acceptance, objectionable-content rules, content/user reporting, blocking, ongoing monitored moderation, and a separate mature-content reveal | Reconcile the exact UGC and incidental-content answers with the candidate and active questionnaire |
| Child Safety Standards | Current category is Music & Audio; radio is the core function, and authenticated station Chat is neither anonymous nor random | Current scope covers anonymous/random-chat apps and apps in Social/Dating categories. If active Console classification or functionality puts the Player in scope, do not self-certify until public CSAE standards, an operational CSAM response/reporting process, and a designated point of contact are verified |
| Data Safety | Field inventory and a 36-entry five-station public/read-only TLS probe are complete in `m23-data-safety.md`; there is no developer backend or telemetry SDK | Station retention/deletion/IP-use facts, final authenticated candidate probes, and owner review remain |
| Account deletion | No account creation in the native app or current external catalog; Sign out removes only the local protected session | M31 audit complete for the current candidate; any later account-creation route requires approved in-app and web deletion paths before release |
| Privacy policy | Public HTTPS policy and native notice exist and disclose the station/operator boundary | Align the exact Play developer identity/contact and station retention/deletion path; keep native and public wording consistent |
| Foreground service | Manifest, service ownership, copy, and rehearsal sequence are ready | Record and host the final protected-candidate video, then save the declaration in Console |

## Foreground service permission

**Declared permission/type**

- `android.permission.FOREGROUND_SERVICE`
- `android.permission.FOREGROUND_SERVICE_MEDIA_PLAYBACK`
- Service type: `mediaPlayback`
- Play Console use case: **Media playback — continue audio playback from the background, including streaming**

**Functionality description**

> 24Seven.FM Player streams one live radio station selected by the user. After the user presses Play, a Media3 playback service and MediaSession keep that user-requested stream playing when the app is backgrounded or the screen is locked. The ongoing media notification, headset controls, Bluetooth controls, and system media controls show the active station and allow the user to pause or resume playback. Tapping the notification returns to the existing player task.

**Impact if start is deferred**

> The user presses Play expecting the selected live station to begin immediately. If the task is deferred, no requested audio begins and the core radio-player action appears broken.

**Impact if interrupted**

> If the task is interrupted, the live stream stops, current media controls and live metadata are lost, and the user must return to the app and start playback again. Because this is live radio, the interrupted portion cannot be resumed from a buffered playback position.

**Why the use is perceptible and user controlled**

- Playback starts only from an explicit user Play action.
- Active audio is inherently perceptible and is accompanied by an ongoing Media3 notification.
- Pause is available in the app, notification, headset/Bluetooth, and system media controls.
- Stop is available in the app.
- The service exists for the active playback session rather than unrelated background work.

**Reviewer-accessible video shot list**

Record one continuous, unedited demonstration with no credentials or public Chat content visible:

1. Cold-launch the app on the Player screen while signed out.
2. Tap **Play** on StreamingSoundtracks.com and wait for visible `Playing live` state.
3. Press Home while playback remains active.
4. Open notifications and show the 24Seven.FM media notification with station/track information and Pause.
5. Tap the body of the notification and show that it returns to the existing Player task.
6. Return to the background and use Pause; show that playback state changes.
7. Return to the app, resume once, then use **Stop** to end the session.

Host the final video at a stable reviewer-accessible URL that requires no account or permission request. Add that URL
only in Play Console or the private release record. The declaration video is separate from an optional public
store-listing preview video.

**Local rehearsal evidence — July 15, 2026**

A credential-free rehearsal was captured on the API 35 Pixel Tablet emulator and inspected as a 25.8-second,
2560×1600 H.264 recording. Key frames show the initial Player, buffering, active playback, Home, the expanded media
notification, a notification-body tap returning to the same Player task, pause, resume, and stop/not-connected state.
No credentials or Chat content appear. The recording remains a transient local draft and is not committed or hosted.
It is not the final reviewer video because its notification identifies the debug build and its live station artwork
can change. Repeat the same sequence using an exact protected candidate after the M30 rights gate closes and the
intended artifact is selected, inspect the final frames, and host only that final copy. That M29 declaration artifact precedes M39; M39 later
freezes the fully accepted candidate and must not be made a dependency of M29.

## App access and reviewer instructions

Google requires sign-in details to remain active, reusable, valid regardless of reviewer location, and written in
English. Provide credentials only through Play Console's protected App access form.

**Account preparation required from the owner**

- Create or designate a least-privileged, non-administrator reviewer account for each of StreamingSoundtracks.com,
  1980s.FM, Adagio.FM, Death.FM, and Entranced.FM.
- Prefer the same reviewer username/password across the five station accounts if the network permits it, but validate
  each account independently.
- Do not require an expiring password, email OTP, IP allowlist, location restriction, or moderator role.
- Keep the accounts active throughout review and future updates.
- Preconfigure a small harmless Favorites list where possible so the reviewer sees a non-empty authenticated surface.
- The station CAPTCHA is not a secret or second factor: reviewers read the current three-character image in the app
  and enter it for that sign-in attempt.

**Copy-ready navigation instructions**

> Public radio playback, Queue, and History work without an account. To review restricted features, open More → Account. The selected station account appears first. Enter the reviewer username and password supplied in this protected form, enter the current three-character security code shown by the app (case-sensitive), and tap Sign in. Use “Manage other station accounts” to reach the other four independent station accounts. Favorites is available from the primary navigation. Station-library search and song requests are under More. To review Chat and public request attribution, open Chat, complete the adult age screen, read and accept the Terms of Participation, then separately choose “Show community content.” Each public message/request attribution provides distinct Report content, Report user, and Block user actions; blocked users are managed under More → Community safety. StreamingSoundtracks.com also exposes signed-in request activity under More.

**Owner-only fields still required**

- reviewer usernames/passwords for all five stations;
- confirmation that every account signs in from a clean install and non-owner network;
- a private maintenance owner for password/account expiry; and
- any Console field limit that requires shortening the instructions without removing access steps.

## Target audience, content rating, and UGC evidence

The owner selected an 18+ target audience. Do not infer the content-rating answers solely from that selection. Answer
the active questionnaire accurately from these artifact facts:

- The app is a Music & Audio radio client, not a game and not designed for children.
- Users can exchange public text through station Chat and publish a requester identity and optional request message.
- Community content can contain mature themes or explicit language and is hidden by default behind a neutral adult age
  screen, Terms acceptance, and a separate reveal action.
- The app supplies report-content, report-user, block-user, and local block-management functionality backed by a
  monitored moderation handoff.
- The app opens only fixed, allowlisted same-station HTTPS pages in the user's browser; it is not a general browser.
- The app has no ads, purchases, gambling, simulated gambling, location features, violence mechanics, or user
  image/video uploads.

The owner must answer questions about the real frequency/intensity of sexual themes, profanity, drug references,
violence, or other mature station/community content. Do not minimize those answers to obtain a lower rating. Retain
the generated multi-authority rating result privately.

Google's current Child Safety Standards scope includes anonymous/random-chat apps and apps declared in the Social or
Dating categories. The saved category is Music & Audio, radio remains the app's core functionality, station Chat
requires a named authenticated station account, and the app does not randomly connect people. Treat this as a
classification gate rather than a permanent exemption: re-evaluate it against the active Console and current
functionality. If the app becomes in scope, an 18+ target and age gate do not exempt it. Google's announced
Age-Restricted Content and Functionality expansion for anonymous/random-chat apps takes effect August 26, 2026; the
current Player does not match those definitions, but confirm the active Restrict Minor Access questions before saving.

## Data Safety, privacy, and deletion

Use `m23-data-safety.md` for the field-by-field worksheet and `../PRIVACY.md` for the canonical public disclosure.
The remaining station/operator answers are:

- whether and for how long login submissions, sessions/access logs, IP/security logs, search terms, Chat/request
  submissions, request actions, and abuse reports are retained;
- whether IP addresses or other network identifiers are used to infer location or for another declared purpose;
- which processors or other parties receive those data;
- which contact or mechanism handles station-side access, correction, or deletion; and
- the exact developer identity/contact shown on the Play listing.

Do not claim end-to-end ephemeral processing, broad station-side deletion, or a Data Safety deletion badge without
those facts. The app does not create station accounts. **Sign out** removes the protected local session; it does not
delete the pre-existing station account, public posts, request history, server logs, or sent email.

## M29 completion gate

M29 remains **Waiting externally** until all of the following are true:

1. PT-35 is run against the exact signed candidate and its release manifest/dependencies/data flows match this packet.
2. Five reusable reviewer accounts and English access instructions pass a clean-install/non-owner-network check.
3. Station retention, deletion/contact, processor, and IP-use facts are received and reflected in the privacy policy
   and Data Safety form.
4. Ads, app access, target audience, Restrict Minor Access decision, content rating, UGC, account deletion, privacy,
   and Data Safety answers are saved and reviewed in the active Console.
5. Any applicable Child Safety Standards self-certification has an actual operational process and designated contact;
   otherwise the documented out-of-scope classification is verified.
6. The final signed-candidate foreground-media video is hosted privately and its declaration is saved.
7. Only sanitized evidence is committed; credentials, correspondence, private Console captures, and unlisted URLs
   remain outside the repository.

## Official references

- [Foreground service declaration requirements](https://support.google.com/googleplay/android-developer/answer/13392821)
- [Permissions for Foreground Services policy](https://support.google.com/googleplay/android-developer/answer/16559646)
- [Requirements for providing sign-in details](https://support.google.com/googleplay/android-developer/answer/15748846)
- [Target audience and app content](https://support.google.com/googleplay/android-developer/answer/9867159)
- [Content rating requirements](https://support.google.com/googleplay/android-developer/answer/9859655)
- [Data Safety requirements](https://support.google.com/googleplay/android-developer/answer/10787469)
- [User Data and UGC policies](https://support.google.com/googleplay/android-developer/answer/17190352)
- [Account deletion requirements](https://support.google.com/googleplay/android-developer/answer/13327111)
- [Child Safety Standards guidance](https://support.google.com/googleplay/android-developer/answer/14747720)
- [Age-Restricted Content and Functionality](https://support.google.com/googleplay/android-developer/answer/16302250)
