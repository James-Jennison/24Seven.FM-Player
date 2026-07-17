# M25 Cast / Audio-Output Selection validation

Status: Complete through the dedicated Android audio-output path; Google Cast remains capability-gated.

## Delivered scope

- The Player shows the active Android media route and distinguishes device, Bluetooth, wired/USB, and remote route categories.
- A single **Audio output** action opens Android's native system output switcher without leaving the app or replacing the service-owned Media3 player.
- Route changes flow into immutable `PlaybackState`; Compose receives that state and emits only the chooser action upward. Android audio policy supplies the actual media device, avoiding paired-but-inactive routes.
- Android's chooser owns supported device discovery, selection, volume, and handoff behavior. The app registers for route/device changes without active discovery scans and performs a short bounded state refresh only after the user opens the chooser.
- If the system chooser is unavailable, the app reports that result without exposing a dead control or failing playback.

Android documents the system Output Switcher as the route surface for local speakers and connected Bluetooth, wired, USB, HDMI, hearing-aid, and automotive outputs. The implementation uses stable AndroidX MediaRouter `1.8.1` and `SystemOutputSwitcherDialogController`, preserving the existing MediaSession and audio-focus behavior. On Android 13 and newer, `AudioManager.getAudioDevicesForAttributes()` supplies the route actually selected for media; earlier supported versions use the bounded legacy audio-device fallback.

## Cast and stream boundary

This milestone satisfies the roadmap's **Cast and/or dedicated audio-output selection** requirement through the supported Android system route path. It does not add a Google Cast sender, receiver application, relay, rebroadcast proxy, or stream URL.

Google Cast uses a separate sender/receiver architecture. The five station streams currently remain direct Media3 connections to the already verified station-provided relays; choosing a device route does not proxy or encrypt that source. A Cast control can be added only after receiver compatibility and permitted stream use are verified. Until then, the UI does not imply that Cast is available.

Official references:

- [Android media routing and Output Switcher](https://developer.android.com/media/routing)
- [`AudioManager.getAudioDevicesForAttributes`](https://developer.android.com/reference/android/media/AudioManager#getAudioDevicesForAttributes(android.media.AudioAttributes))
- [`SystemOutputSwitcherDialogController`](https://developer.android.com/reference/androidx/mediarouter/app/SystemOutputSwitcherDialogController)
- [AndroidX MediaRouter releases](https://developer.android.com/jetpack/androidx/releases/mediarouter)
- [Media3 CastPlayer sender/receiver requirements](https://developer.android.com/media/media3/cast/create-castplayer)

## Automated verification

| Check | Result |
| --- | --- |
| Route-category and privacy-safe fallback unit tests | Pass |
| Full `:app:testDebugUnitTest` suite | Pass, 139 tests |
| `:app:lintDebug` | Pass |
| Focused `RadioAppTest#audioOutputDisplaysCurrentRouteAndEmitsChooserAction` on Motorola Razr 2023, Android 16 | Pass, 1/1 |
| Debug Kotlin and Android-test compilation | Pass |

## Physical Razr integration

On the physical Motorola Razr 2023 running Android 16:

1. The installed debug build opened the native Player and reported the Razr as the active media output.
2. Tapping **Audio output** opened the Motorola/Android System UI media-output dialog, bound to the app's live Media3 session.
3. The dialog exposed **This phone**, **James' JBL Clip 5**, volume control, and **Connect a device**.
4. Selecting the JBL speaker changed Android audio policy to Bluetooth A2DP and updated the still-running Player to **Using James' JBL Clip 5**.
5. Selecting the phone changed the Player back to **Using motorola razr 2023** without restarting the app.
6. Disabling Bluetooth while the JBL route was active automatically returned Android audio policy and the Player label to the Razr speaker; Bluetooth was re-enabled after the check.
7. Dismissing the dialog returned to the existing Player task throughout the cycle.

The first visual pass caught an important platform distinction: AndroidX MediaRouter's legacy selected route could name a paired transferable speaker even while Android audio policy was still using the phone. The final implementation reads the device actually selected for media attributes, so the Player label now agrees with System UI before, during, and after handoff. This route validation does not authorize or imply Google Cast support.

## Visual evidence

<p align="center">
  <img src="screenshots/m25-audio-output-chooser.png" alt="Motorola Razr Android audio-output chooser with the JBL speaker connected while the Player behind it reports the same active output" width="420"><br>
  <strong>Physical Razr output handoff</strong><br>
  <sub>Android System UI and the native Player agree on the selected JBL Bluetooth route.</sub>
</p>

## Privacy and traffic result

The feature adds no app-operated service, account data, analytics, background polling, active route-discovery scan, or new network endpoint. Only the user-visible Android route name and broad output category are retained in memory as current playback state. Diagnostic export remains a separate M26 feature and must apply its own redaction contract.
