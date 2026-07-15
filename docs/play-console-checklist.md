# Google Play Alpha setup checklist

The Google Play developer account was approved on July 14, 2026. Use this during M23–M24; do not place Console credentials, reviewer credentials, upload keys, passwords, or tester email lists in Git.

## Local preparation already complete

- Application ID: `com.codeframe78.twentyfourseven.player`
- Candidate: `0.1.0-alpha01`, version code 2
- Target SDK: 35; compile SDK: 36; minimum SDK: 26
- Privacy notice, Alpha testing guide, release notes, permission review, and device validation are complete.
- Gradle accepts Play upload signing only from four `TWENTYFOURSEVEN_UPLOAD_*` environment variables. Supplying a partial set fails configuration.
- `scripts/validate-play-bundle.ps1` builds the release AAB, requires a real signature, and prints its SHA-256 without revealing signing inputs.

## Play Console setup

1. Create the app in Play Console with the intended public name, app/game classification, free pricing, support email, and required declarations.
2. Accept Play App Signing. Prefer a Google-generated app-signing key; retain only the separate upload key under project-controlled secure backup.
3. Create the upload key outside this repository and load its path/password/alias through process-scoped environment variables or protected CI secrets.
4. Run `powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\scripts\validate-play-bundle.ps1` and upload the verified AAB.
5. Start with Internal testing. It supports up to 100 testers and uses a private opt-in/share link rather than public search discovery.
6. Add a feedback email or URL and a tester email list in Console. Do not commit tester identities.
7. Complete the store listing and App content declarations needed by the selected track.

## App-content details to review

- **App access:** Public playback, Queue, and History do not require login. Authentication, Chat posting, and requests do. If Play requests reviewer access, provide a least-privileged non-administrator station account only through Play Console's protected reviewer instructions.
- **Ads:** The app contains no advertising SDK or advertising behavior.
- **Data safety:** Internal-only testing is currently exempt, but closed/open/production releases require an accurate declaration. Account credentials are submitted directly to the selected station, station sessions are encrypted locally, chat history is memory-only, and the app has no analytics or developer backend.
- **Privacy policy:** Play policy requires an active public, non-geofenced web URL for the privacy policy. Host `PRIVACY.md` as a stable web page before submitting the app. The same policy is available as native text under More in the app.
- **Account deletion:** The app does not create station accounts; it only signs into pre-existing station accounts and can remove the local session with Sign out. Confirm the appropriate Console declaration when completing App content.
- **Target audience/content rating:** Complete these from the actual intended audience and station content; do not infer a child-directed audience.

## Release progression

Internal testing is the first rollout and is currently exempt from the Data safety section, but not from the underlying user-data/privacy-policy obligations. Closed testing or production adds the Data safety declaration and may add—for some newer personal developer accounts—testing-duration/participation requirements shown by the activated Console. Treat the Console dashboard as authoritative for the account.

Official references:

- [Create and set up an app](https://support.google.com/googleplay/android-developer/answer/9859152)
- [Set up internal, closed, or open testing](https://support.google.com/googleplay/android-developer/answer/9845334)
- [Upload an Android App Bundle](https://developer.android.com/studio/publish/upload-bundle)
- [Data safety requirements](https://support.google.com/googleplay/android-developer/answer/10787469)
