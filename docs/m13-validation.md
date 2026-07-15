# M13 independent accounts validation

Completed July 14, 2026 in implementation commit `9ef1f1c`.

## Task assessment

- Task Complexity Level: 2 — Feature Logic & API Integration
- T-shirt size: L
- Estimated duration: 4–8 active hours
- Primary confidence variable: live authentication and challenge differences among the five independently operated station sites

M13 retained the existing station-scoped repository, cookie-manager, and Android Keystore boundaries. It added an immutable aggregate account presentation in `MainViewModel`; Compose receives that state and emits actions carrying an explicit station ID.

## Delivered behavior

- All five station accounts are visible from the native More destination without changing the playback station.
- Compact layouts use a vertical dashboard; widths of 720dp and above use two account cards per row.
- Each account shows an accessible text status: signed out, loading, signing in, signed in, expired, or attention required.
- Sign-in challenges, transient credentials, retry, sign-out, and expired-session recovery are independently actionable for each station.
- Restored sessions that are proven invalid are cleared only for their station and surface an explicit expired state.
- Existing selected-station auth continues to gate Chat, requests, and Favorites; playback and navigation behavior are unchanged.

## Automated validation

```powershell
.\gradlew.bat :app:testDebugUnitTest :app:lintDebug :app:assembleDebug
.\gradlew.bat :app:connectedDebugAndroidTest
```

- Full debug unit suite: passed.
- Debug lint: passed.
- Debug assembly: passed.
- Wired Motorola Razr 2023, Android 16: 15/15 instrumentation tests passed.
- Focused account-dashboard Compose test: passed.
- Android Keystore round-trip and pairwise clear isolation tests: passed.

## Physical-device inspection

The debug app was installed and launched over wired ADB on device `motorola razr 2023`. A fresh install displayed all five signed-out station cards, station-specific controls, the selected playback marker, and the persistent mini-player. The page was scrolled end-to-end and every station card was present. See [`screenshots/accounts.png`](screenshots/accounts.png).

## Known limits

- A fresh debug install has no retained production session by design.
- Live sign-in across all five stations was not automated because credentials and CAPTCHA answers must remain user-entered and must not enter source, logs, or fixtures.
- Representative live accounts and session-expiration behavior remain station-certification checks in M18–M22.
