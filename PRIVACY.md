# Privacy notice for 24Seven.FM Player Alpha

Last updated: July 15, 2026.

24Seven.FM Player is an unofficial, non-commercial native Android client for the five public 24Seven.FM radio stations. It is not affiliated with or endorsed by 24Seven.FM or its stations.

## Data the app handles

- **Radio and public station data:** The app connects directly to the selected station to play audio and retrieve public now-playing, queue, history, artwork, catalog, chat, and the signed-in member's public favorite-track list.
- **Account credentials:** A username, password, and station security code are sent only to the selected station when the user explicitly signs in. Passwords and security-code answers are held only long enough to complete that sign-in attempt and are not written to app storage.
- **Station sessions:** Successful station cookies and the returned display identity are encrypted locally using an Android Keystore key. Sessions are scoped to the exact station and can be removed with Sign out or by clearing the app's data.
- **User submissions:** Chat posts, song requests, and optional request messages are sent only after an explicit user action. The app does not automate or bulk-submit requests.
- **Community safety preferences:** The app stores the adult/not-adult result from its age screen, the accepted Terms version, the mature-community-content visibility choice, and station-scoped blocked display identities on the device. It does not save the date of birth entered on the age screen.
- **Android backup and transfer:** All app-private data is excluded from Android cloud backup and device-to-device transfer. Protected station sessions and local safety preferences are not migrated through those Android mechanisms.
- **Abuse reports:** After an explicit **Send report** action, the app sends the selected station, report category, reported username, displayed timestamp, a bounded Chat/request snapshot, reporter name and email, and optional details directly to that station's authorized administrators through its HTTPS Contact Us form. Report form data, CAPTCHA values, submitted reports, and station responses are not persisted by the app. The station administration may retain and process a received report under its own moderation and privacy practices.
- **Chat history:** Chat messages are kept in memory for the current app session and are not persisted by the app.
- **Favorite tracks:** Favorite lists are loaded only after station sign-in, kept in memory, and cleared from the interface when the user signs out. They are not written to app storage.
- **Request activity and membership:** Where verified for a station, the app can load the signed-in member's recent request summaries, station-reported request readiness, and explicit membership indicator. These values are kept in memory, cleared from the interface on sign-out, and are not written to app storage.
- **External station pages:** Selected public station pages open only after an explicit tap in an Android browser Custom Tab. The app passes an allowlisted same-station HTTPS address but does not copy its protected station session into the browser. Browser cookies, history, and retention are controlled by the selected browser and station independently of this app.

The application does not include advertising, analytics, crash-reporting, tracking SDKs, or a developer-operated data server. Station operators and network providers may independently retain normal server or network logs; those systems are outside this application's control.

## Android permissions

- **Internet:** Required for live streams and station features.
- **Network state:** Used by the media/network libraries to detect connectivity changes and present connection or retry states.
- **Foreground media playback:** Required to continue audio while the app is in the background and to expose system media controls.
- **Notifications:** On Android 13 and newer, used for the media-playback notification. Denying it may reduce visible background playback controls.
- **Wake lock:** Used only by the media playback stack to keep an active live stream from being suspended while the screen is off. The app does not use it to keep the display awake.

Android also adds an app-specific, signature-protected dynamic-receiver permission during the build. It is an internal platform safety mechanism that prevents other apps from sending to non-exported receivers; users are not asked to grant it and it does not provide access to user data.

The app does not request contacts, location, microphone, camera, photos, phone, SMS, or broad file-storage access.

## Retention and deletion

Protected station sessions, the adult age-screen result, accepted Terms version, community-content visibility preference, and local block list remain on the originating device until the relevant in-app action, app-data clearing, or uninstall removes them. They are excluded from Android cloud backup and device-to-device transfer. Chat, favorite-track lists, request activity, membership indicators, pending song-request text, and abuse-report form/submission data are transient. Android's app settings can clear all locally retained application data.

## Alpha limitations

This is pre-release software. Testers should use a non-administrator station account where practical and should not include credentials, private messages, session values, or security-code images in bug reports.

The application is maintained by the community contributors to the `codeframe78/24Seven.FM-Player` project. Privacy questions can be sent to [24sevenplayer@jamesjennison.net](mailto:24sevenplayer@jamesjennison.net). Reports can also be submitted through [the project's GitHub issue channel](https://github.com/codeframe78/24Seven.FM-Player/issues) without sensitive account information.
