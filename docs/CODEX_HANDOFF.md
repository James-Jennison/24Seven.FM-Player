# Windows development handoff

## Mission and repository

Complete **24Seven.FM Player**, an unofficial, fully native Android client for the 24seven.FM radio network. The app uses Kotlin, Jetpack Compose, MVVM, and Jetpack Media3. It must not use a WebView.

- Repository: `https://github.com/codeframe78/24Seven.FM-Player`
- Working branch: `agent/initial-android-scaffold`
- Draft pull request: `https://github.com/codeframe78/24Seven.FM-Player/pull/1`

Clone the current handoff branch on the new PC:

```powershell
git clone https://github.com/codeframe78/24Seven.FM-Player.git
Set-Location '24Seven.FM-Player'
git switch agent/initial-android-scaffold
git status
```

Read `AGENTS.md`, `README.md`, `CONTRIBUTING.md`, `docs/architecture.md`, and `docs/m2-validation.md` before editing. Preserve the existing station-first domain model and dependency boundaries.

## Required Windows environment

- A current Android Studio release
- JDK 17
- Android SDK Platform 36
- Android SDK Build Tools 36.1.0
- Android SDK Platform Tools
- Optional Android Emulator image for API 35 or 36

The app compiles against API 36 because the selected AndroidX Activity and Media3 versions require it. It targets API 35 to match the primary Motorola Razr 2023 running Android 15. Minimum SDK is API 26.

Install SDK packages from Android Studio's SDK Manager or with `sdkmanager`:

```powershell
sdkmanager "platforms;android-36" "build-tools;36.1.0" "platform-tools"
```

Do not commit `local.properties`. If Android Studio does not discover the SDK automatically, set `ANDROID_HOME` for the current PowerShell session:

```powershell
$env:ANDROID_HOME = "$env:LOCALAPPDATA\Android\Sdk"
```

The complete Gradle wrapper is committed. Do not install a separate Gradle distribution. Validate the new PC from the repository root:

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\scripts\validate-windows.ps1
```

The script checks JDK and SDK prerequisites, redirects build output to `%TEMP%`, and runs `test lint assembleDebug`. The equivalent manual command is documented in `README.md`.

## Current implementation

M1 is complete and CI builds the project with JDK 17 and the committed Gradle wrapper. M2 currently includes:

- five-station catalog with station-provided primary and source-fallback URLs;
- immutable playback state behind a domain `PlaybackController` contract;
- Activity-to-service Media3 controller connection;
- one service-owned `ExoPlayer` and `MediaSession`;
- play, pause, stop, and atomic station switching;
- one-step fallback from the primary relay to the source stream;
- Android 13+ notification permission request;
- foreground/background playback with system media pause and resume;
- station metadata in the media notification;
- notification controls that do not expose the internal fallback playlist.

The latest successful validation ran unit tests for debug and release, Android lint, and `assembleDebug`. See `docs/m1-validation.md` and `docs/m2-validation.md` for exact evidence.

## Physical Razr setup

The primary device is a Motorola Razr 2023:

- Android 15 / API 35
- device model reported by ADB: `motorola_razr_2023`
- physical display: 1080 x 2640 at 420 dpi

ADB trust keys are specific to each development PC. The new PC must pair again; do not copy old ADB keys. On the phone:

1. Enable Developer options.
2. Open **Settings > System > Developer options > Wireless debugging**.
3. Choose **Pair device with pairing code**.
4. Keep the pairing screen open while running `adb pair <phone-ip>:<pairing-port>` on the new PC.
5. Enter the six-digit code shown by the phone.
6. Return to the main Wireless debugging screen and run `adb connect <phone-ip>:<debug-port>`.
7. Confirm the connection with `adb devices -l`.

The pairing port and debugging port are different and can change. Never commit the phone's LAN/Tailscale address, pairing code, device serial, or screenshots of the Wireless debugging screen. If both PCs and the phone use Tailscale, the phone's current Tailscale address can be used explicitly with `adb pair` and `adb connect`; mDNS discovery is not required.

## Verified device behavior

The Razr has verified:

- StreamingSoundtracks.com and Adagio.FM primary-stream playback;
- station switching while playback is active;
- background continuation with a foreground media service;
- notification permission and visible media notification;
- system media pause/resume;
- in-app stop;
- correct session metadata and audio focus;
- no user-facing Next action for the internal fallback item.

Before testing, use `adb devices -l` and pass `-s <device>` to ADB commands when an emulator is also running.

## Immediate next objective

Finish M2 device validation:

1. Verify 1980s.FM, Death.FM, and Entranced.FM on the Razr.
2. Exercise primary-to-source fallback under a controlled, local test failure without changing or inventing production URLs.
3. Verify switching repeatedly among all five stations.
4. Record results and any stream-specific failures in `docs/m2-validation.md`.

Then proceed to M3 hardening: notification/lock-screen behavior, Bluetooth/headset controls, audio focus transitions, noisy-output handling, and service lifecycle tests.

## Station stream evidence

The exact URLs below came from station-provided PLS files. The trailing semicolon is part of each supplied URL and must not be removed without testing and documentation.

| Station | Primary relay | Source fallback |
| --- | --- | --- |
| StreamingSoundtracks.com | `http://hi5.streamingsoundtracks.com/;` | `http://hi.streamingsoundtracks.com/;` |
| 1980s.FM | `http://hi5.1980s.fm/;` | `http://hi.1980s.fm/;` |
| Adagio.FM | `http://hi5.adagio.fm/;` | `http://hi.adagio.fm/;` |
| Death.FM | `http://hi5.death.fm/;` | `http://hi.death.fm/;` |
| Entranced.FM | `http://hi5.entranced.fm/;` | `http://hi.entranced.fm/;` |

Codec and bitrate remain unverified. Cleartext is disabled globally and allowed only for the five station domains. Do not broaden the network security policy or guess HTTPS/API endpoints.

## Architectural invariants

- No WebView.
- Compose receives immutable state and emits actions upward.
- ViewModels depend on repository/controller interfaces, never Media3 or network implementations.
- ExoPlayer remains exclusively service-owned.
- Every station-specific operation accepts or derives a `StationId`.
- Unsupported station features use capability flags.
- Only one station may play at a time.
- Public audio playback must not depend on login, chat, or metadata availability.
- Never commit credentials, cookies, tokens, private endpoints, private network addresses, pairing codes, HAR files, screenshots containing device identifiers, SDK paths, APKs, or build output.

## Git workflow

Continue on `agent/initial-android-scaffold` while PR #1 remains the active draft. Before editing, fetch and confirm the branch has not advanced elsewhere:

```powershell
git fetch origin
git status
git log --oneline --decorate -5
```

Keep commits focused. Do not force-push. Run `git diff --check` and `powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\scripts\validate-windows.ps1` before publishing implementation changes. The bypass applies only to that PowerShell process and does not change the PC's saved execution policy.
