# Google Play Alpha setup checklist

The Google Play developer account was approved on July 14, 2026. Use this during M29–M35 and M39–M41; do not place Console credentials, reviewer credentials, upload keys, passwords, private Console captures, unlisted reviewer-video URLs, or tester email lists in Git.

## Local preparation already complete

- Application ID: `com.codeframe78.twentyfourseven.player`
- Candidate: `0.1.0-alpha01`, version code 2
- Target SDK: 36; compile SDK: 36; minimum SDK: 26
- The current M35 audit verifies the production manifest, dependencies/licenses, explicit backup exclusions,
  protected signed release outputs, the exact registered upload certificate, and 16 KB APK/ELF packaging. The native notice is under More → Privacy →
  Open-source licenses; see `docs/m23-release-candidate-audit.md`.
- Google's July 16 Android developer-verification email confirms that the account's Play apps were automatically
  registered to the verified developer account. On July 19 the owner confirmed that this exact package is listed as
  Draft and has no uploaded versions/bundles, closing M35 without retaining private Console imagery.
- Current Play target-level check (July 15, 2026): target API 35 remains accepted until August 31, 2026, when new mobile apps and updates must target API 36. M22 migrated early so the closed-test and update path do not depend on that deadline.
- The July 18 M29 release-variant audit reconciles the manifest, dependency graph, network/data paths, external
  handoffs, and account boundary in `docs/m23-play-declaration-packet.md`. Repeat it against the exact protected
  pre-M39 candidate; M39 later freezes the accepted release and is not an M29 prerequisite.
- A July 19 credential-free probe reached 36 applicable public/read-only station entry points over HTTPS/TLS 1.2 or
  newer with HTTP 200, expected same-origin effective URLs, and no redirects or downgrades. Authenticated,
  server-discovered actions still require final-candidate validation without retaining secrets.
- Privacy notice, Alpha testing guide, release notes, permission review, and current device validation evidence exist;
  owner/station retention and deletion facts still need reconciliation before M29 closes.
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
7. **Complete locally:** the Linux helper built the current protected AAB/APK from commit `2e43a2b` without persisting plaintext signing material; subsequent AAB/APK verification and local physical-Razr clean-install/update checks matched the registered upload identity.
8. **Complete:** Google's July 16 notice says the account's Play apps were automatically registered for Android
   developer verification; on July 19 the owner confirmed the exact package is listed as Draft and has no uploaded
   versions/bundles. If the release is distributed outside Play, separately register its package-and-signing-key pair
   on the Android developer verification page.
9. **M40 after M39:** upload the verified AAB, then start with Internal testing. It supports up to 100 testers and uses a private opt-in/share link rather than public search discovery.
10. Add a feedback email or URL and a tester email list in Console. Do not commit tester identities.
11. Complete the store listing and App content declarations needed by the selected track.

The current protected July 18 AAB from commit `2e43a2b` has SHA-256 `FA923DB3C7CC8C44661030F5DCCE594B0415C676A718E358E105ADD772483CB5`; the companion APK has SHA-256 `D49FBD7454B8BBE4E58C2C3513074FA1370C22CDD9139DDB92CD28DBD928BA7A`. Both match upload-certificate SHA-256 `F6E8E81271964FFC3F8A0D548B49B4DB93AEFC48CCB74B8744512670F4279E3F`. AAB signatures include per-build data, so compare the selected artifact hash with the audit record and the stable certificate fingerprint with Play Console before M40 upload. The July 16 artifacts remain historical evidence in the M35 audit.

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

These saved declarations have not been sent for review. The remaining App content forms require owner-provided or
policy-sensitive inputs and must not be guessed: reviewer credentials/instructions, final target-audience and Restrict
Minor Access settings, content-rating/UGC answers, account-deletion treatment, Data Safety confirmation, and the
media-playback foreground-service declaration/video.

The copy-ready, non-secret portion of those declarations is maintained in `docs/m23-play-declaration-packet.md`.

## App-content details to review

- **App access:** Public playback, Queue, and History do not require login. Authentication, Chat posting, Favorites, and requests do. Console requires reusable sign-in details that give reviewers full access and will not unlock Target audience until this declaration is complete. Because accounts are station-specific, provide least-privileged non-administrator reviewer details for every station needed to exercise the restricted features, only through Play Console's protected reviewer form. Do not place them in Git or chat.
- **Ads:** Completed as **No** on July 15, 2026; the app contains no advertising SDK or advertising behavior.
- **Data safety:** Internal-only testing is currently exempt, but closed/open/production releases require an accurate declaration. Use `docs/m23-data-safety.md` rather than memory. Explicit user-data paths are same-station HTTPS; only public live audio is allowlisted cleartext and carries no Player-added user payload. The station/CDN still receives network information such as source IP. Contact/report and diagnostic exports transfer data to another app only after an explicit user action; reconcile the specific user-initiated sharing exception instead of assuming it. The Player has no analytics or developer backend. Station retention, processors, deletion paths, and IP use remain owner/operator facts.
- **Privacy policy:** The active public HTTPS page is `https://codeframe78.github.io/24Seven.FM-Player/`; a consistent native notice is under More. Before saving M29, align the exact Play developer identity/contact and station-side retention/deletion path in both surfaces.
- **Account deletion:** The app does not create station accounts, the M31 candidate exposes only Contact through email, and Sign out removes only the local protected session. If M51, M57, or M58–M60 later directs users to account creation, Play also requires approved in-app and web account-deletion paths. Do not claim station-account/post/log deletion until a verified mechanism exists.
- **Target audience/content rating:** The owner selected **18+** on July 15, 2026. Verify that saved selection and make an explicit Restrict Minor Access decision. Community content is hidden by default behind a neutral adult age screen, versioned Terms acceptance, and a separate reveal action. Accurately declare public text Chat, requester identity/messages, and the observed frequency/intensity of profanity or mature themes; retain the generated IARC result privately.
- **UGC and Child Safety:** M28 supplies Terms, objectionable-content rules, report content/user, block/unblock, monitored moderation, and a mature-content reveal. Current Child Safety Standards scope includes anonymous/random-chat apps and apps in Social/Dating categories. The Player is categorized **Music & Audio**, radio is its core function, station Chat requires a named account, and it neither hides participant identity nor randomly connects users. Re-evaluate that classification and the active Restrict Minor Access questions in Console, including the August 26, 2026 age-restriction expansion. If the app becomes in scope, do not self-certify without public CSAE standards, an operational CSAM response/reporting process, and a designated child-safety contact; an 18+ target does not provide an exemption.
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
- [Account deletion requirements](https://support.google.com/googleplay/android-developer/answer/13327111)
- [User Data and UGC policies](https://support.google.com/googleplay/android-developer/answer/17190352)
- [Child Safety Standards guidance](https://support.google.com/googleplay/android-developer/answer/14747720)
- [Age-Restricted Content and Functionality](https://support.google.com/googleplay/android-developer/answer/16302250)
