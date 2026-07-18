# Privacy notice for 24Seven.FM Player Alpha

Last updated: July 18, 2026.

24Seven.FM Player is an unofficial, non-commercial native Android client for the five public 24Seven.FM radio stations. It is not affiliated with or endorsed by 24Seven.FM or its stations.

## Data the app handles

- **Radio and public station data:** The app connects directly to the selected station to play audio and retrieve public now-playing, queue, history, artwork, catalog, chat, and the signed-in member's public favorite-track list. As with ordinary internet connections, the station, stream host, content-delivery network, and their network providers can receive connection information such as the source IP address under their own practices; the Player does not read or persist that address itself.
- **Account credentials:** A username, password, and station security code are sent only to the selected station when the user explicitly signs in. Passwords and security-code answers are held only long enough to complete that sign-in attempt and are not written to app storage.
- **Station sessions:** Successful station cookies and the returned display identity are encrypted locally using an Android Keystore key. Sessions are scoped to the exact station and can be removed with Sign out or by clearing the app's data.
- **Account boundary:** The Player signs into pre-existing station accounts; it does not create or delete them. **Sign out** removes only the protected session from this device. It does not delete the station account, public Chat posts, request history, station/server logs, or email already sent through another app. Station-side access, correction, retention, and deletion are controlled by the applicable station or network operator.
- **User submissions:** Chat posts, song requests, and optional request messages are sent only after an explicit user action. The app does not automate or bulk-submit requests.
- **Community safety and notification preferences:** The app stores the adult/not-adult result from its age screen, the accepted Terms version, the mature-community-content visibility choice, station-scoped blocked display identities, and the stations for which the user enables local Chat-mention notifications. It does not save the date of birth entered on the age screen.
- **Local Chat-mention notifications:** When enabled, exact signed-in display-name mentions are matched on the device against the actively observed Chat feed. The first snapshot is only a baseline, duplicate fingerprints are bounded in memory, blocked authors are filtered, and Android notification text contains the station and sender but not the Chat message. The current implementation does not provide closed-app push delivery or add background polling.
- **Android backup and transfer:** All app-private data is excluded from Android cloud backup and device-to-device transfer. Protected station sessions and local safety preferences are not migrated through those Android mechanisms.
- **Abuse reports:** After an explicit **Review email** action, the app prepares a bounded draft containing the selected station, report category, reported username, displayed timestamp, a bounded Chat/request snapshot, reporter name or station nickname, and optional details. Android opens the user's chosen email app with the monitored moderation recipient, subject, and draft body prefilled. The user may review, edit, cancel, or explicitly send it there. The Player cannot read the user's email account or confirm whether the draft was sent or delivered, and it does not persist the draft or report. The selected email app and receiving administration may retain a sent report under their own practices.
- **Chat history:** Chat messages are kept in memory for the current app session and are not persisted by the app.
- **Favorite tracks:** Favorite lists are loaded only after station sign-in, kept in memory, and cleared from the interface when the user signs out. They are not written to app storage.
- **Request activity and membership:** Where verified for a station, the app can load the signed-in member's recent request summaries, station-reported request readiness, and explicit membership indicator. These values are kept in memory, cleared from the interface on sign-out, and are not written to app storage.
- **External contact and station pages:** The **Contact Us** entry opens an Android email composer addressed to the monitored Player contact only after an explicit tap; the app does not send the message itself. Other selected public station pages open in an Android browser Custom Tab with an allowlisted same-station HTTPS address. The app does not copy its protected station session into either external app. Email drafts, sent mail, browser cookies, history, and retention are controlled independently by the selected email/browser app and recipient or station.
- **In-app diagnostics:** The app can generate a local support snapshot containing app/build version, Android/API and coarse device model, selected station, bounded playback/error category, validated-network availability, broad audio-output category, and up to five recent non-sensitive playback transitions. The preview excludes account data, messages, request/report content, URLs, route names, stable device identifiers, raw errors, and logs. It remains on the device unless the user explicitly copies it to Android's clipboard or opens Android's Share chooser and selects a recipient; the receiving app then handles the text under its own privacy practices.

The application does not include advertising, analytics, crash-reporting, tracking SDKs, or a developer-operated data server. Station operators and network providers may independently retain normal server or network logs; those systems are outside this application's control.

## Android permissions

- **Internet:** Required for live streams and station features.
- **Network state:** Used by the media/network libraries to detect connectivity changes and present connection or retry states.
- **Foreground media playback:** Required to continue audio while the app is in the background and to expose system media controls.
- **Notifications:** On Android 13 and newer, requested contextually for visible media-playback controls and optional local Chat-mention alerts. Denying it does not prevent public playback or Chat use, but may remove those notification surfaces.
- **Wake lock:** Used only by the media playback stack to keep an active live stream from being suspended while the screen is off. The app does not use it to keep the display awake.

Android also adds an app-specific, signature-protected dynamic-receiver permission during the build. It is an internal platform safety mechanism that prevents other apps from sending to non-exported receivers; users are not asked to grant it and it does not provide access to user data.

The app does not request contacts, location, microphone, camera, photos, phone, SMS, or broad file-storage access.

## Retention and deletion

Protected station sessions, the adult age-screen result, accepted Terms version, community-content visibility preference, local block list, and station-scoped Chat-mention opt-ins remain on the originating device until the relevant in-app action, app-data clearing, or uninstall removes them. They are excluded from Android cloud backup and device-to-device transfer. Chat, mention-deduplication fingerprints, favorite-track lists, request activity, membership indicators, pending song-request text, abuse-report draft data, and generated diagnostic previews are transient. A report or Contact Us draft handed to an email app is then controlled by that app and, if sent, by the recipient. A copied diagnostic snapshot is controlled by Android's clipboard, and a shared snapshot is controlled by the recipient selected by the user. Android's app settings can clear all locally retained application data. Clearing local data does not remove station-side accounts, content, or logs; any applicable station-side request must use a contact or mechanism supplied by that operator.

## Alpha limitations

This is pre-release software. Testers should use a non-administrator station account where practical and should not include credentials, private messages, session values, or security-code images in bug reports.

The application is maintained by the community contributors to the `codeframe78/24Seven.FM-Player` project. Privacy questions can be sent to [24sevenplayer@jamesjennison.net](mailto:24sevenplayer@jamesjennison.net). Reports can also be submitted through [the project's GitHub issue channel](https://github.com/codeframe78/24Seven.FM-Player/issues) without sensitive account information.
