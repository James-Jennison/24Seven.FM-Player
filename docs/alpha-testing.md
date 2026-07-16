# 24Seven.FM Player Alpha testing guide

## Supported devices

- Android 8.0 (API 26) or newer.
- Phones, landscape phones, tablets, foldables, multi-window, and freely resized Android windows are supported through
  responsive layouts. Compact, medium, expanded portrait, and expanded landscape evidence is recorded in
  `docs/m23-device-compatibility.md`. The primary physical validation device is a Motorola Razr 2023 on Android 16;
  the app targets API 36.

## Installation and updates

Only install an APK supplied through the approved project distribution channel. Android may require the tester to permit installs from that source.

An update installs over an earlier Alpha only when all of the following match:

- application ID `com.codeframe78.twentyfourseven.player`;
- the same signing identity;
- a higher version code.

Do not distribute the development debug APK as the public Alpha. Its machine-local debug signature will not match a future production signing identity, forcing testers to uninstall and lose protected station sessions before changing builds.

## First-run checklist

1. Confirm the launcher shows the purple 24Seven.FM icon.
2. Open the app and grant notification permission when desired.
3. Confirm all five station cards are visible by horizontal scrolling.
4. Start one station, verify audio and artwork/title behavior, then switch stations.
5. Leave the app and verify the media notification and background controls. Tap the notification body outside
   play/pause and confirm it returns to the existing player task.
6. Check Queue and recently played content.
7. If using a test account, verify sign in, session restoration, Chat, and Favorites. On SST, open More and refresh Request activity to verify recent requests, readiness, and the explicit membership indicator. Confirm eligible Favorites show a green `Request Now`; recently played or queued tracks show a red `Track Recently Played` and cannot be selected. Switch Favorites between Library order and Play state, including on a large list when available.
8. Search the station library by Title, Album, Artist, and Genre. Submit an eligible favorite or catalog track only through the explicit confirmation while respecting station cooldowns.
9. In More, open one verified **More from…** card. Confirm it opens in a browser tab and Back returns to the app. Browser sign-in is intentionally separate from the protected app session.
10. Press Back twice and verify the exit confirmation. Choose **Keep listening** unless intentionally stopping playback.

## Suggested configuration coverage

- Compact portrait and landscape.
- Light and dark system themes.
- Large font size.
- Wi-Fi and mobile data transitions.
- Bluetooth or headset connection and removal.
- Process stop/relaunch and device lock/unlock.
- At least one station other than StreamingSoundtracks.com.

## Reporting a problem

Include:

- app version and version code from Android app information;
- device model and Android version;
- selected station and visible playback state;
- exact steps and whether the issue repeats;
- a screenshot only after checking it for personal information.

Never include passwords, security-code answers or images, cookies, session values, private messages, private network addresses, or full network captures. Do not repeatedly submit a song request while investigating an indeterminate response; check Queue first.

## Known Alpha boundaries

- M17 Private Messages is deferred because of underlying website/server issues. Inbox and Sent Box discovery worked,
  but New Message selection remains suspect and a profile-originated MorgHubby test was not delivered; the site owner
  has the reproduced result.
- Representative authenticated certification is complete for all five stations; natural server-side session expiry was not forcibly induced.
- Sleep timer, Cast/audio-output selection, and diagnostics are not implemented.
- Station accounts are currently station-specific.
- Public station interfaces can change independently of the app.
- Physical Razr playback and UI survive a measured open/tabletop/closed/reopened hinge cycle. Play-delivered install/update validation, the Play pre-launch report, and a complete spoken TalkBack pass remain release checks.
