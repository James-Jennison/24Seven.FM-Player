# 24Seven.FM Player closed-testing guide

24Seven.FM Player is an independently developed, unofficial player for the 24Seven.FM network of internet radio stations.

## Supported devices

- Android 8.0 (API 26) or newer.
- Phones, landscape phones, tablets, foldables, multi-window, and freely resized Android windows are supported through
  responsive layouts. Compact, medium, expanded portrait, and expanded landscape evidence is recorded in
  `docs/m23-device-compatibility.md`. The primary physical validation device is a Motorola Razr 2023 on Android 16;
  the app targets API 36.

## Installation and updates

Google Play Closed Testing is the authoritative distribution channel. Install and update the closed-test build only through its Google Play opt-in link; do not use a development debug APK or a separately shared release APK for this program.

The owner confirms the current M42 two-week closed-test phase is active. Keep the qualifying tester-continuity window running while recording, fixing, and retesting verified feedback; do not treat the active closed test as production approval.

Guest closed-test volunteers use their own Google account only to opt in to Google Play. They do not need a 24Seven.FM station account, must not create or share one for this program, and receive only the account-free Guest testing tasks.

An update installs over an earlier Alpha only when all of the following match:

- application ID `com.codeframe78.twentyfourseven.player`;
- the same signing identity;
- a higher version code.

Do not distribute the development debug APK to closed testers. Its machine-local debug signature will not match the Google Play signing identity, forcing testers to uninstall and lose protected station sessions before changing builds.

## First-run checklist

The tester portal’s initial smoke confirmation covers a short version of steps 1, 4, and 5 below: launch the Player, play one station for a few minutes, switch stations, background playback and try Android media controls, then return to the Player. It is a tester self-confirmation, not automated installation or activity telemetry.

1. Confirm the launcher shows the purple 24Seven.FM icon.
2. Open the app and grant notification permission when desired.
3. Confirm all five station cards are visible by horizontal scrolling.
4. Start one station, verify audio and artwork/title behavior, then switch stations.
5. Leave the app and verify the media notification and background controls. Tap the notification body outside
   play/pause and confirm it returns to the existing player task.
6. On the Player, confirm **Audio output** reports the current route and opens Android's system output chooser. If an accessory is available, switch to it, disconnect it, and confirm playback returns to an available local route.
7. Check Queue and recently played content.
8. If using a test account, verify sign in, session restoration, Chat, and Favorites. On SST, open More and refresh Request activity to verify recent requests, readiness, and the explicit membership indicator. Confirm eligible Favorites show a green `Request Now`; recently played or queued tracks show a red `Track Recently Played` and cannot be selected. Switch Favorites between Library order and Play state, including on a large list when available.
9. Search the station library by Title, Album, Artist, and Genre. Submit an eligible favorite or catalog track only through the explicit confirmation while respecting station cooldowns.
10. In More, open **Contact Us** and confirm Android offers an email draft addressed to the monitored Player contact. Cancel without sending. Confirm there is no VIP/RIP membership, payment, registration, recovery, management, or deletion browser card in the Play candidate.
11. Press Back twice and verify the exit confirmation. Choose **Keep listening** unless intentionally stopping playback.

## Suggested configuration coverage

- Compact portrait and landscape.
- Light and dark system themes.
- Large font size.
- Wi-Fi and mobile data transitions.
- Bluetooth or headset connection and removal.
- Process stop/relaunch and device lock/unlock.
- At least one station other than StreamingSoundtracks.com.

## Reporting a problem

Use your private tester portal for assigned-task reports whenever possible; it keeps reports visible only to the coordinator. The public [product-testing workspace](https://player.jamesjennison.net/product-testing/) and structured GitHub form remain supplementary routes for non-sensitive results. The canonical catalog has 34 PT cases organized into 23 Tester Tasks; 19 current bundles are assignable and four future bundles remain blocked by their milestones. A Tester Task can contain more than one PT case, but every PT case needs its own result. The coordinator distributes focused tasks through the private Tester Queue rather than asking one volunteer to complete the full catalog.

Include:

- app version and version code from Android app information;
- device model and Android version;
- selected station and visible playback state;
- exact steps and whether the issue repeats;
- a screenshot only after checking it for personal information.

Never include passwords, security-code answers or images, cookies, session values, private messages, private network addresses, or full network captures. Do not repeatedly submit a song request while investigating an indeterminate response; check Queue first.

## Coordinator task assignments

The protected Tester Queue keeps Alpha enrollment separate from assignment state. Coordinators can give an accepted tester one or more focused `TT-*` bundles, record the assigned station and device/accessory scope, and mark each assignment Assigned, In progress, Complete, or Blocked. Saving a new assignment sends that tester an individual assignment email through the project mail transport; the Queue records the mail-transport handoff outcome and permits an explicit resend if needed. Future / Blocked tasks cannot be assigned.

For a controlled assignment, the Queue shows the task-specific boundary before it is saved. In particular, `TT-09` allows one authorized request only—never retry an indeterminate result and inspect Queue first. `TT-11` defaults to read-only Chat validation unless one harmless post is explicitly authorized; `TT-12` never grants moderation-email delivery authority; and `TT-13` requires the approved two-account setup.

## Known Alpha boundaries

- M47 Private Messages is deferred because of underlying website/server issues. Inbox and Sent Box discovery worked,
  but New Message selection remains suspect and a profile-originated MorgHubby test was not delivered; the site owner
  has the reproduced result.
- Representative authenticated certification is complete for all five stations; natural server-side session expiry was not forcibly induced.
- M24 Sleep Timer, M25's dedicated Android audio-output path, M26's user-reviewed privacy-safe diagnostics, and M27's local actively-observed Chat mentions are included. Google Cast is permanently out of scope; Bluetooth and other standard Android-managed output routes remain supported. M36–M38 closed-app delivery is deferred and nonblocking for this Alpha unless JERIC authorizes an official station-app program.
- Station accounts are currently station-specific.
- M31 removes station VIP/RIP purchase and account-creation browser routes from the global Play candidate. Existing
  membership status may still be shown when the station reports it, but the Player does not sell or upgrade membership.
- Public station interfaces can change independently of the app.
- Physical Razr playback and UI survive a measured open/tabletop/closed/reopened hinge cycle, and the signed release
  package passes the human spoken TalkBack and Bluetooth keyboard/pointer checks. Play-delivered install/update validation and the Play pre-launch
  report remain M40 release checks.
