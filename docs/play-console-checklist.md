# Google Play Alpha setup checklist

The Google Play developer account was approved on July 14, 2026. Use this during M23–M28; do not place Console credentials, reviewer credentials, upload keys, passwords, or tester email lists in Git.

## Local preparation already complete

- Application ID: `com.codeframe78.twentyfourseven.player`
- Candidate: `0.1.0-alpha01`, version code 2
- Target SDK: 36; compile SDK: 36; minimum SDK: 26
- The current M23.1 audit verifies the production manifest, dependencies/licenses, explicit backup exclusions,
  protected signed release outputs, the exact registered upload certificate, and 16 KB APK/ELF packaging. The native notice is under More → Privacy →
  Open-source licenses; see `docs/m23-release-candidate-audit.md`.
- Google's July 16 Android developer-verification email confirms that the account's Play apps were automatically
  registered to the verified developer account. Sanitized evidence is retained in the M23.1 audit; per-app status must
  still be confirmed in Play Console Home.
- Current Play target-level check (July 15, 2026): target API 35 remains accepted until August 31, 2026, when new mobile apps and updates must target API 36. M23.3 migrated early so the closed-test and update path do not depend on that deadline.
- Privacy notice, Alpha testing guide, release notes, permission review, and device validation are complete.
- Gradle accepts Play upload signing only from four `TWENTYFOURSEVEN_UPLOAD_*` environment variables. Supplying a partial set fails configuration.
- `scripts/validate-play-bundle.ps1` builds the release AAB, requires a real signature, and prints its SHA-256 without revealing signing inputs.
- `scripts/initialize-play-upload-key.ps1` creates the separate upload key outside Git and stores its credentials in a Windows-current-user DPAPI envelope. It refuses to overwrite either artifact.
- `scripts/validate-protected-play-bundle.ps1` decrypts that envelope only in memory, supplies process-scoped Gradle inputs, restores the previous process environment, verifies the JAR signature and exact signer certificate, and can optionally build the signed release APK with `-BuildApk`.
- `scripts/validate-protected-play-bundle-linux.py` authenticates the same encrypted off-PC recovery format, enforces the registered certificate, materializes the keystore only in `/dev/shm`, uses a non-persistent Gradle process, and removes temporary material before reporting the signed AAB hash. Its passphrase is accepted only through an interactive hidden prompt.
- `scripts/manage-play-upload-recovery.ps1` uses PowerShell 7, PBKDF2-HMAC-SHA256, and AES-256-GCM to export, authenticate, and restore a passphrase-protected recovery package. It refuses repository-local and overwrite targets, verifies the embedded keystore/certificate, and recreates a new current-user DPAPI envelope after restore.

## Play Console setup

1. **Complete:** Play app created as `24Seven.FM Player`, package `com.codeframe78.twentyfourseven.player`, default language English (United States), classification App, and Free pricing.
2. **Complete:** Automatic installer protection disabled so approved local/open-source builds are not redirected to Google Play.
3. **Complete:** Initial Play App Signing Terms accepted. Prefer the default Google-generated app-signing key at first release; retain only the separate upload key under project-controlled secure backup.
4. **Complete locally:** A separate 4096-bit RSA upload key was created outside the repository. Its generated password and metadata are stored only in a current-user DPAPI envelope; no plaintext signing environment is persisted.
5. **Complete locally:** `powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\scripts\validate-protected-play-bundle.ps1 -BuildApk` built the signed APK/AAB, verified the AAB signature, and confirmed that the bundle signer matches the configured upload certificate.
6. **Complete:** the encrypted recovery package was exported to an owner-controlled off-PC volume and independently verified with the owner-held passphrase. Its encrypted-package SHA-256 is `361E6A85452DBF9ACDC816F554569E3B7DBA0F98B60C30DB255E54C2644C4D1C`; the passphrase is stored separately and is not in Git.
7. **Complete locally:** the Linux helper built the current protected AAB/APK from commit `2086ab9` without persisting plaintext signing material; subsequent AAB and APK verification matched both signatures to the registered upload certificate.
8. **Complete by account email; Console confirmation remains:** Google's July 16 notice says the account's Play apps
   were automatically registered for Android developer verification. Confirm this app's registration in Play Console
   Home. If the release is distributed outside Play, separately register its package-and-signing-key pair on the
   Android developer verification page.
9. Upload the verified AAB, then start with Internal testing. It supports up to 100 testers and uses a private opt-in/share link rather than public search discovery.
10. Add a feedback email or URL and a tester email list in Console. Do not commit tester identities.
11. Complete the store listing and App content declarations needed by the selected track.

The current protected July 16 AAB from commit `2086ab9` has SHA-256 `1C6C43BF947B844F5D8708DF368635CFE34B6A690DD826B87663B66C5C6C518F`; the companion APK has SHA-256 `923C26621FC998CAA4D1099B7BCA5A118C7EB8256057D96E9971C4AFE29825D0`. Both match upload-certificate SHA-256 `F6E8E81271964FFC3F8A0D548B49B4DB93AEFC48CCB74B8744512670F4279E3F`. AAB signatures include per-build data, so compare the selected artifact hash with the audit record and the stable certificate fingerprint with Play Console before upload.

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

The following objective declarations and listing settings are saved in Play Console and awaiting the normal Publishing overview review step:

- **Ads:** No. The release dependency graph and implemented behavior contain no advertising SDK or advertising surface.
- **Government apps:** No. The app is an independent community project and is not developed by or on behalf of a government.
- **Financial features:** None. The app provides no listed financial feature.
- **Health:** None. The app provides no health feature; Console reported that no regional health documentation is currently required.
- **Privacy policy:** `https://codeframe78.github.io/24Seven.FM-Player/`; the HTTPS page is public and returns the canonical `PRIVACY.md` content.
- **Store contact/category:** public support email `24sevenplayer@jamesjennison.net`; category **Music & Audio**.

These saved declarations have not been sent for review. The remaining App content forms require owner-provided or policy-sensitive inputs and must not be guessed: reviewer credentials/instructions for restricted station features, a policy-compatible target audience, content-rating answers, and final Data Safety confirmation.

The copy-ready, non-secret portion of those declarations is maintained in `docs/m23-play-declaration-packet.md`.

## App-content details to review

- **App access:** Public playback, Queue, and History do not require login. Authentication, Chat posting, Favorites, and requests do. Console requires reusable sign-in details that give reviewers full access and will not unlock Target audience until this declaration is complete. Because accounts are station-specific, provide least-privileged non-administrator reviewer details for every station needed to exercise the restricted features, only through Play Console's protected reviewer form. Do not place them in Git or chat.
- **Ads:** Completed as **No** on July 15, 2026; the app contains no advertising SDK or advertising behavior.
- **Data safety:** Internal-only testing is currently exempt, but closed/open/production releases require an accurate declaration. Use `docs/m23-data-safety.md` as the project worksheet rather than answering from memory. Account credentials and explicit abuse reports are submitted directly to the selected station, station sessions are encrypted locally, chat/report data is not persisted by the app, and the app has no analytics or developer backend.
- **Privacy policy:** Completed on July 15, 2026. The active public HTTPS page is `https://codeframe78.github.io/24Seven.FM-Player/`; the same canonical policy remains available as native text under More in the app.
- **Account deletion:** The app does not create station accounts; it only signs into pre-existing station accounts and can remove the local session with Sign out. Confirm the appropriate Console declaration when completing App content.
- **Target audience/content rating:** The owner selected **18+** on July 15, 2026. The service community is predominantly long-standing adult members, and the app is not designed or presented as a children's product. M23.2 now hides community content by default behind an adult age screen, versioned Terms acceptance, and a separate reveal action; accurate content-rating answers are still required. Console entry remains blocked until reusable reviewer-access details are saved.
- **Foreground service:** Declare only **Media playback** for `FOREGROUND_SERVICE_MEDIA_PLAYBACK`. Use the copy and demonstration sequence in `docs/m23-play-declaration-packet.md`; upload or link the final reviewer-accessible video through Console without committing account data.

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
- [Manage target audience and app content](https://support.google.com/googleplay/android-developer/answer/9867159)
- [Google Play Families policies](https://support.google.com/googleplay/android-developer/answer/9893335)
