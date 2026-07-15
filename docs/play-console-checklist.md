# Google Play Alpha setup checklist

The Google Play developer account was approved on July 14, 2026. Use this during M23–M24; do not place Console credentials, reviewer credentials, upload keys, passwords, or tester email lists in Git.

## Local preparation already complete

- Application ID: `com.codeframe78.twentyfourseven.player`
- Candidate: `0.1.0-alpha01`, version code 2
- Target SDK: 35; compile SDK: 36; minimum SDK: 26
- Current Play target-level check (July 15, 2026): new mobile apps and updates must target API 35 or higher, so this candidate is compliant without a toolchain change.
- Privacy notice, Alpha testing guide, release notes, permission review, and device validation are complete.
- Gradle accepts Play upload signing only from four `TWENTYFOURSEVEN_UPLOAD_*` environment variables. Supplying a partial set fails configuration.
- `scripts/validate-play-bundle.ps1` builds the release AAB, requires a real signature, and prints its SHA-256 without revealing signing inputs.
- `scripts/initialize-play-upload-key.ps1` creates the separate upload key outside Git and stores its credentials in a Windows-current-user DPAPI envelope. It refuses to overwrite either artifact.
- `scripts/validate-protected-play-bundle.ps1` decrypts that envelope only in memory, supplies process-scoped Gradle inputs, restores the previous process environment, verifies the JAR signature and exact signer certificate, and can optionally build the signed release APK with `-BuildApk`.
- `scripts/manage-play-upload-recovery.ps1` uses PowerShell 7, PBKDF2-HMAC-SHA256, and AES-256-GCM to export, authenticate, and restore a passphrase-protected recovery package. It refuses repository-local and overwrite targets, verifies the embedded keystore/certificate, and recreates a new current-user DPAPI envelope after restore.

## Play Console setup

1. **Complete:** Play app created as `24Seven.FM Player`, package `com.codeframe78.twentyfourseven.player`, default language English (United States), classification App, and Free pricing.
2. **Complete:** Automatic installer protection disabled so approved local/open-source builds are not redirected to Google Play.
3. **Complete:** Initial Play App Signing Terms accepted. Prefer the default Google-generated app-signing key at first release; retain only the separate upload key under project-controlled secure backup.
4. **Complete locally:** A separate 4096-bit RSA upload key was created outside the repository. Its generated password and metadata are stored only in a current-user DPAPI envelope; no plaintext signing environment is persisted.
5. **Complete locally:** `powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\scripts\validate-protected-play-bundle.ps1 -BuildApk` built the signed APK/AAB, verified the AAB signature, and confirmed that the bundle signer matches the configured upload certificate.
6. Export and verify the encrypted recovery package to an owner-controlled location separate from this PC before uploading. The recovery workflow is tested, but the owner must choose the destination and enter a passphrase interactively; the local DPAPI envelope alone is not a machine-loss backup.
7. Upload the verified AAB, then start with Internal testing. It supports up to 100 testers and uses a private opt-in/share link rather than public search discovery.
8. Add a feedback email or URL and a tester email list in Console. Do not commit tester identities.
9. Complete the store listing and App content declarations needed by the selected track.

The latest validated July 15 signing run reported AAB SHA-256 `74A5F256111F4562453523899346610DA3B3B462C10E569B43A3C08372A27CFB`; its upload-certificate SHA-256 is `F6E8E81271964FFC3F8A0D548B49B4DB93AEFC48CCB74B8744512670F4279E3F`. AAB signatures include per-build data, so record the hash printed by the final upload build; the stable certificate fingerprint is the identity to compare with Play Console.

### Off-PC recovery handoff

Run these commands from the repository with PowerShell 7. The script prompts for the passphrase without echoing it; do not place the passphrase on the command line, in Git, or beside the package.

```powershell
pwsh.exe -NoProfile -File .\scripts\manage-play-upload-recovery.ps1 `
  -Action Export `
  -PackagePath E:\Backups\24seven-upload.24seven-recovery

pwsh.exe -NoProfile -File .\scripts\manage-play-upload-recovery.ps1 `
  -Action Verify `
  -PackagePath E:\Backups\24seven-upload.24seven-recovery
```

Replace `E:\Backups` with the selected external drive or encrypted synced folder. Store the passphrase separately in the owner's password manager. On a replacement Windows profile, `-Action Restore` verifies the package, restores the keystore outside Git, and creates a new DPAPI envelope without printing credentials.

### App-content declarations completed July 15, 2026

The following objective declarations are saved in Play Console and awaiting the normal Publishing overview review step:

- **Ads:** No. The release dependency graph and implemented behavior contain no advertising SDK or advertising surface.
- **Government apps:** No. The app is an independent community project and is not developed by or on behalf of a government.
- **Financial features:** None. The app provides no listed financial feature.
- **Health:** None. The app provides no health feature; Console reported that no regional health documentation is currently required.

These saved declarations have not been sent for review. The remaining App content forms require owner-provided or policy-sensitive inputs and must not be guessed: reviewer credentials/instructions, public privacy-policy URL, intended target audience, content-rating contact email, Data Safety confirmation, and public store contact details.

## App-content details to review

- **App access:** Public playback, Queue, and History do not require login. Authentication, Chat posting, and requests do. If Play requests reviewer access, provide a least-privileged non-administrator station account only through Play Console's protected reviewer instructions.
- **Ads:** Completed as **No** on July 15, 2026; the app contains no advertising SDK or advertising behavior.
- **Data safety:** Internal-only testing is currently exempt, but closed/open/production releases require an accurate declaration. Use `docs/m23-data-safety.md` as the project worksheet rather than answering from memory. Account credentials are submitted directly to the selected station, station sessions are encrypted locally, chat history is memory-only, and the app has no analytics or developer backend.
- **Privacy policy:** Play policy requires an active public, non-geofenced web URL for the privacy policy. Host `PRIVACY.md` as a stable web page before submitting the app. The same policy is available as native text under More in the app.
- **Account deletion:** The app does not create station accounts; it only signs into pre-existing station accounts and can remove the local session with Sign out. Confirm the appropriate Console declaration when completing App content.
- **Target audience/content rating:** Complete these from the actual intended audience and station content; do not infer a child-directed audience.

## Release progression

Internal testing is the first rollout and is currently exempt from the Data Safety form, but not from the underlying user-data/privacy-policy obligations. Closed, open, and production tracks require the form. A personal developer account created after November 13, 2023 must complete a closed test with at least 12 testers continuously opted in for 14 days before applying for production access; Internal testing has no access requirement. Confirm the account type and creation-date treatment on the activated Console dashboard before planning the rollout.

Official references:

- [Create and set up an app](https://support.google.com/googleplay/android-developer/answer/9859152)
- [Set up internal, closed, or open testing](https://support.google.com/googleplay/android-developer/answer/9845334)
- [Upload an Android App Bundle](https://developer.android.com/studio/publish/upload-bundle)
- [Data safety requirements](https://support.google.com/googleplay/android-developer/answer/10787469)
- [Testing requirements for new personal accounts](https://support.google.com/googleplay/android-developer/answer/14151465)
- [Target API level requirements](https://support.google.com/googleplay/android-developer/answer/11926878)
- [Play App Signing](https://support.google.com/googleplay/android-developer/answer/9842756)
