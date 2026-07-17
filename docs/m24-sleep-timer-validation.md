# M24 Sleep Timer validation

Date: July 16, 2026
Status: Complete
Implementation commit: `b713783`

## Outcome

M24 adds a native, service-owned Sleep Timer to the existing Media3 playback session. The timer does not create another player and does not depend on `MainActivity` remaining alive. Members can choose 15, 30, 45, 60, or 90 minutes, enter a custom duration from 1 through 720 minutes, view the live countdown, replace the deadline, or cancel it.

The playback service owns the deadline and expiry action. It publishes immutable timer state through MediaSession extras, exposes an active-timer cancellation command to compatible system media surfaces, persists enough bounded state to recover across service recreation, and stops the existing ExoPlayer when the deadline expires.

## Behavior contract

| Event | Result |
| --- | --- |
| Start or adjust | Replaces any prior deadline and immediately publishes the new remaining time |
| Pause/resume | Countdown continues toward the original deadline |
| Station change | Countdown remains active while the existing player changes media items |
| Manual Stop | Playback stops and the timer is cancelled |
| Explicit cancel | Timer state, persistence, and the system cancel action are cleared without changing playback |
| Deadline expiry | Existing ExoPlayer stops, timer state clears, and connected controllers receive an expiry event |
| Activity leaves foreground | Service countdown continues and the UI restores from MediaSession state on return |
| Playback-service recreation | A still-valid same-boot deadline is restored from monotonic and wall-clock checkpoints |
| Device sleep | `SystemClock.elapsedRealtime()` includes deep sleep, so elapsed sleep time counts toward the deadline |
| Reboot or material clock discontinuity | Inconsistent persisted clocks fail safe by clearing the timer instead of extending it unexpectedly |

## Architecture

- `PlaybackController` exposes only timer commands and immutable `SleepTimerState`; the ViewModel remains independent of Media3.
- `Media3PlaybackController` sends bounded custom commands and translates session extras/expiry events into domain state.
- `RadioPlaybackService` validates 1–720 minute durations, owns the coroutine/deadline, persists restoration checkpoints, updates session state once per second, and calls `player.stop()` at expiry.
- The notification/session preference contains a **Cancel sleep timer** command only while a timer is active. Exact placement remains controlled by the Android system media surface.
- Compact and expanded Compose players share the same action/state contract, presets, custom numeric input, countdown, adjust, and cancel controls.

## Verification

- `./gradlew :app:compileDebugKotlin` — pass.
- `./gradlew :app:testDebugUnitTest` — pass, 136/136 tests. New coverage verifies restoration/expiry clock decisions, countdown formatting, and ViewModel command delegation.
- `./gradlew :app:lintDebug` — pass.
- Focused connected tests on the physical Motorola Razr 2023 running Android 16 — pass, 4/4 unique M24 tests:
  - start through the real MediaSession custom command and cancel through manual Stop;
  - restore an active deadline after stopping and recreating `RadioPlaybackService`;
  - wait through the production-minimum 60-second duration and verify the expiry event plus cleared state;
  - select a Compose preset, display the countdown, and dispatch cancel.
- Manual Razr review — pass for the compact timer dialog, horizontal preset access, custom numeric field, active countdown, adjust/cancel controls, live playback, and scroll reachability.

## Visual evidence

<table>
  <tr>
    <td width="50%" align="center" valign="top"><img src="screenshots/m24-sleep-timer.png" alt="M24 Sleep Timer dialog on the physical Razr with presets and custom minute input" width="300"><br><strong>Timer setup</strong><br><sub>Accessible presets and a bounded custom duration.</sub></td>
    <td width="50%" align="center" valign="top"><img src="screenshots/m24-sleep-timer-active.png" alt="M24 active Sleep Timer on the physical Razr during live playback with countdown, adjust, cancel, and stop controls" width="300"><br><strong>Active countdown</strong><br><sub>Service-published remaining time during live playback.</sub></td>
  </tr>
</table>

## Remaining release checks

The MediaSession cancellation command is connected-device tested, but its exact notification-button placement should be included in the broader M28 system-UI matrix because Android and OEM media surfaces choose how custom commands are rendered. M28 must also re-run release-candidate packaging and the wider emulator/device suite after M26–M27 are complete.
