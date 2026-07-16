# M23.4 Google Play declaration packet

Date: July 15, 2026

Status: non-secret local preparation complete; owner-controlled Console inputs remain

This packet contains copy-ready evidence for the current artifact. Reconcile every answer with the exact active Play
Console wording and signed AAB immediately before submission. Never commit reviewer credentials, Console screenshots
containing private data, or an unlisted video URL that exposes account information.

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
only in Play Console or the private release record. The foreground-service declaration video is separate from an
optional public store-listing preview video.

## App access and reviewer instructions

Google requires sign-in details to remain active, reusable, valid regardless of reviewer location, and written in
English. Provide credentials only through Play Console's protected App access form.

**Account preparation required from the owner**

- Create or designate a least-privileged, non-administrator reviewer account for each of StreamingSoundtracks.com,
  1980s.FM, Adagio.FM, Death.FM, and Entranced.FM.
- Prefer the same reviewer username/password across the five station accounts if the network permits it, but confirm
  each account independently.
- Do not require an expiring password, email OTP, IP allowlist, or moderator role.
- Keep the accounts active throughout review and future updates.
- Preconfigure a small harmless Favorites list where possible so the reviewer sees a non-empty authenticated surface.
- The station CAPTCHA is not a secret or second factor: reviewers read the current three-character image in the app
  and enter it for that sign-in attempt.

**Copy-ready navigation instructions**

> Public radio playback, Queue, and History work without an account. To review restricted features, open More → Account. The selected station account appears first. Enter the reviewer username and password supplied in this protected form, enter the current three-character security code shown by the app (case-sensitive), and tap Sign in. Use “Manage other station accounts” to reach the other four independent station accounts. Favorites is available from the primary navigation. Station-library search and song requests are under More. To review Chat and public request attribution, open Chat, complete the adult age screen, read and accept the Terms of Participation, then separately choose “Show community content.” Each public message/request attribution provides distinct Report content, Report user, and Block user actions; blocked users are managed under More → Community safety. StreamingSoundtracks.com also exposes signed-in request activity under More.

**Owner-only fields still required**

- reviewer usernames/passwords for all five stations;
- confirmation that every supplied account can sign in from a clean install and non-owner network;
- a private maintenance owner for password/account expiry; and
- any Console-specific field limits that require shortening the instructions.

## Target audience and content rating evidence

The owner selected an 18+ target audience. Do not infer the content-rating questionnaire answers solely from that
selection. Answer the active questionnaire accurately from these artifact facts:

- The app is a Music & Audio radio client, not a game and not designed for children.
- Users can exchange public text through station Chat and can publish a requester identity and optional request message.
- Community content can contain mature themes or explicit language and is hidden by default behind an adult age screen,
  Terms acceptance, and a separate reveal action.
- The app supplies in-app report-content, report-user, block-user, and block-management functionality backed by the
  authorized station moderation route.
- The app opens only fixed, allowlisted same-station HTTPS pages in the user's browser; it is not a general web browser.
- The app has no ads, purchases, gambling, simulated gambling, location features, violence mechanics, or user image/video uploads.

The owner must answer any questionnaire questions about the real frequency/intensity of sexual themes, profanity,
drug references, violence, or other mature station/community content. Do not minimize those answers to obtain a lower
rating. Save the generated multi-authority rating result with the private release record.

## Data Safety and retention

Use `m23-data-safety.md` for the field-by-field worksheet and `../PRIVACY.md` for the canonical public disclosure.
The remaining owner/station answer is whether and for how long station systems retain login submissions, search terms,
Chat/request submissions, request actions, and abuse reports. Do not claim end-to-end ephemeral processing without that
answer. A copy-ready retention table, reviewer-account confirmation, rights grant, report-receipt response, and PM-repair
confirmation are consolidated in `m23-owner-response-packet.md`.

## Official references

- [Foreground service declaration requirements](https://support.google.com/googleplay/android-developer/answer/13392821)
- [Permissions for Foreground Services policy](https://support.google.com/googleplay/android-developer/answer/16559646)
- [Requirements for providing sign-in details](https://support.google.com/googleplay/android-developer/answer/15748846)
- [Content rating requirements](https://support.google.com/googleplay/android-developer/answer/9859655)
- [Data Safety requirements](https://support.google.com/googleplay/android-developer/answer/10787469)
- [Store listing best practices](https://support.google.com/googleplay/android-developer/answer/13393723)
- [Metadata policy](https://support.google.com/googleplay/android-developer/answer/9898842)
