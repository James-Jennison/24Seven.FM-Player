# M14 local personalization validation

Completed July 14, 2026 in implementation commit `81c2c4e`.

## Task assessment

- Task Complexity Level: 2 — Feature Logic & API Integration
- T-shirt size: M
- Estimated duration: 2–4 active hours
- Primary confidence variable: startup restoration ordering and safe fallback for corrupt or removed station IDs

## Delivered behavior

- The last selected station is stored as a non-sensitive, device-local preference.
- Users can resume the last station or set the currently selected Player station as a fixed startup station.
- Fixed/last startup selection is resolved synchronously inside the station repository before its first emission, preventing a transient wrong-station selection from reaching playback.
- Invalid, removed, blank, or legacy preference values fall through validated catalog candidates and ultimately the safe first-station fallback.
- Choosing a fixed startup station does not switch current playback.
- The native More surface explicitly distinguishes these settings from station accounts, server Favorites, and membership data.
- No credential, cookie, listening history, remote Favorite, or membership state is written to this preference store.

## Validation

```powershell
.\gradlew.bat :app:testDebugUnitTest :app:lintDebug :app:assembleDebug
.\gradlew.bat :app:connectedDebugAndroidTest
```

- Full debug unit suite: passed.
- Debug lint: passed.
- Debug assembly: passed.
- Wired Motorola Razr 2023, Android 16: 18/18 instrumentation tests passed.
- SharedPreferences recreation and unknown-mode fallback tests: passed on device.
- ViewModel first-playback-emission, no-switch fixed-default, and immutable-state tests: passed.
- Compose accessibility/action test for both startup choices: passed.

## Physical-device inspection

The debug app was installed and launched over wired ADB. The Device preferences card remained reachable above the five-station Accounts dashboard, both actions had full-size targets, local-versus-server ownership was legible, and the persistent mini-player/navigation remained intact. See [`screenshots/preferences.png`](screenshots/preferences.png).

## Known limits

- M14 persists station IDs only; it does not persist playback intent or automatically start audio.
- Clearing app data or uninstalling the app removes these local preferences.
- This is not a local track-Favorites or listening-history feature.
