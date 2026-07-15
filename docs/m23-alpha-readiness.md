# M23 Alpha Test Distribution readiness

Prepared early on July 14, 2026 and activated July 15 after M15–M22 completed. The remaining items below are the
current M23 distribution-readiness gate.

## Estimate

- Task Complexity Level: 2 — Feature Logic & API Integration
- T-shirt size: M
- Estimated duration: 1–2 focused days
- Rationale: the app implementation is stable, but release readiness spans Gradle signing inputs, artifact integrity,
  privacy/data-safety declarations, Play configuration, tester guidance, and an upgrade-safe signing lineage.
- Primary confidence variable: selection and custody of the stable release signing identity and distribution channel

## Task breakdown

1. Reconcile privacy, data-safety, tester, release-note, permission, and dependency documentation with M18–M22.
2. Confirm version/application identity and release Gradle behavior without upgrading the toolchain.
3. Create or locate the upload key outside Git, configure complete local-only signing inputs, and record private fingerprints.
4. Build and validate the signed release AAB with `scripts/validate-play-bundle.ps1`.
5. Create/configure the Play app, accept Play App Signing, supply the public privacy URL and required declarations.
6. Install or distribute a same-key candidate, run the final physical smoke test, and publish only the readiness evidence.

## Completed locally

- Version advanced to `0.1.0-alpha01` / version code 2.
- Privacy notice, Alpha tester guide, release notes, and known limitations documented.
- Required permissions reviewed: Internet, network-state awareness, foreground media playback, wake lock for uninterrupted screen-off playback, and Android 13+ notification access. Network-state and wake-lock access are contributed by the existing media/network stack. The build-generated app-specific dynamic-receiver permission is signature-protected and grants no user-data access.
- Application ID, minimum/target SDK, verified streams, network security, dependencies, and completed M1–M22 behavior remain unchanged.
- Debug and release candidates are built and inspected without committing APKs or signing material.
- Debug unit tests, affected-module lint, debug APK, unsigned release APK, and unsigned release AAB builds pass.
- The earlier 21-test connected suite passes on the Android 16 Razr. The expanded API 35 emulator suite passes 24/24,
  adding compact navigation, runtime compact-to-medium resizing with destination preservation, and 1.5× font-scale
  reachability coverage.
- Debug unit tests, affected-module lint, debug assembly, and debug Android-test compilation pass after the modern-device
  additions.
- The Razr upgraded in place from version code 1 / `0.1.0` to the debug-signed version code 2 / `0.1.0-alpha01`, and the native Player launched with its expected accessibility tree.
- Before upload signing was configured, signature inspection confirmed the debug APK had only the standard local Android Debug identity and the release outputs were unsigned as intended.
- The unsigned release baseline built successfully without signing inputs and was correctly rejected by the signing validator. That historical artifact was 17,304,660 bytes with SHA-256 `9C7128A422BCE81351A40FFCE239F941CD584C62D3993209AFC0CB35F8A4D1D8`; it has since been replaced by the signed candidate below.
- A separate 4096-bit RSA Play upload key now exists outside Git. Its generated credentials are held in a current-user Windows DPAPI envelope, are injected only into a child build process, and are cleared afterward. The initializer is atomic and refuses to replace an existing identity.
- The signed `0.1.0-alpha01` AAB validates; the latest signing run reported SHA-256 `74A5F256111F4562453523899346610DA3B3B462C10E569B43A3C08372A27CFB`, and its signer exactly matches upload-certificate SHA-256 `F6E8E81271964FFC3F8A0D548B49B4DB93AEFC48CCB74B8744512670F4279E3F`. AAB hashes are recorded per build because signed output contains per-build data.
- The PowerShell 7 recovery workflow exports a PBKDF2/AES-256-GCM package, rejects wrong passphrases and repository-local targets, validates the recovered keystore/certificate, and restores a new DPAPI envelope. Its tested round trip reproduced the keystore byte-for-byte and signed/verified a release AAB from the restored copy; all temporary test artifacts were removed.
- The actual encrypted recovery package is now stored on the owner-selected off-PC volume and independently validated with the owner-held passphrase. The encrypted package SHA-256 is `361E6A85452DBF9ACDC816F554569E3B7DBA0F98B60C30DB255E54C2644C4D1C`; neither its location nor its passphrase is committed.
- The signed release APK was installed cleanly on the API 35 emulator, cold-launched as version code 2, exposed the expected adaptive Player semantics, and reached `Playing live` on StreamingSoundtracks with the Pause action available. The emulator was restored to the debug build afterward; the wirelessly connected Razr and its sessions were not modified.
- The release dependency graph contains no advertising, analytics, Crashlytics, App Center, or Sentry SDK.
- Google Play's current target-level requirement accepts new mobile apps targeting API 35 or higher, so the unchanged target SDK 35 is compliant as checked July 15, 2026.
- The project-specific provisional Data Safety worksheet is recorded in `docs/m23-data-safety.md`; its final answers remain gated on the active Play Console form and station transmission/retention confirmation.
- The activated Play Console was inspected read-only on July 15: it is a newly activated personal account with no existing apps. The intended public name `24Seven.FM Player` fits the 30-character limit, and Console reports `com.codeframe78.twentyfourseven.player` as available.
- The Play Console app was created on July 15 as an English (United States), App, Free listing with the intended package. The owner explicitly accepted the Developer Program Policies, Play App Signing Terms, and US export declaration; automatic installer protection was disabled to preserve approved sideload testing.
- Play Console App content now records the four evidence-backed objective declarations: no ads, not a government app, no financial features, and no health features. The changes are saved in Publishing overview but have not been sent for review.
- GitHub Pages now publishes the canonical privacy notice at `https://codeframe78.github.io/24Seven.FM-Player/`; HTTPS is enforced, the live page returns HTTP 200, and the URL is saved in Play Console. The public support email and Music & Audio category are also saved.
- Console requires reusable reviewer sign-in details before Target audience can be completed. The app has restricted, station-specific account features, so the declaration must contain least-privileged credentials and instructions sufficient to review those surfaces across the station family.
- The owner selected an **18+** target audience on July 15, consistent with the predominantly adult legacy-station membership and the existing authenticated freeform chat. The app is not designed as a children's product, and no child age band will be claimed without a separate Families-compliance milestone. Console entry remains locked until reviewer-access details are saved.
- Console confirms the account-specific production gate: publish a closed-test release, keep at least 12 testers opted in continuously for at least 14 days, then apply for production access.
- The policy-conscious default store copy, category/contact proposal, asset inventory, and accessibility-text draft are recorded in `docs/play-store-listing.md`.
- Play-ready icon, feature graphic, four phone screenshots, medium/expanded tablet screenshots, and an expanded
  landscape capture are present under `docs/play-store-assets/` and satisfy their required pixel dimensions.
- The modern-device compatibility matrix in `docs/m23-device-compatibility.md` records compact phone, medium tablet,
  expanded tablet, landscape/freeform, API 35 emulator, and API 36 Razr evidence. Display overrides were restored.
- Authenticated Favorites browsing is available for all five stations, discovers the current member's station-specific list identifier without hard-coding it, keeps list data in memory only, and reuses the existing explicit one-shot request confirmation.

## Signing boundary

The project intentionally contains no keystore, password, alias, signing token, or signing configuration. The default debug APK is installable for local device validation but is not an appropriate tester artifact: it uses a machine-local debug identity and cannot be upgraded in place to an APK signed by a different production identity.

The selected route is Google Play internal/closed testing with Play App Signing. Gradle now accepts the separate upload-key inputs only through a complete set of process environment variables; partial configuration fails immediately, and the release remains unsigned when none are supplied. Keystore patterns are ignored by Git, and `scripts/validate-play-bundle.ps1` rejects missing, repository-local, or unsigned upload bundles without printing credentials.

Before external distribution, choose one:

Google Play internal/closed testing with Play App Signing is selected. Retain an upload key outside Git and allow Google Play to manage the app-signing key.

Signing files and secrets must never be committed. Gradle should receive their paths and credentials from local ignored properties or CI secrets only after the distribution decision is made.

## Remaining completion work

- Google approved the personal Play developer account on July 14, 2026; the app and initial legal/signing declarations are now established in Console.
- Because this is a newly activated personal account, plan for a closed test with at least 12 testers continuously opted in for 14 days before applying for production access. Internal testing itself has no access requirement.
- Upload the verified signed AAB to Play and confirm that Console reports the expected upload-certificate fingerprint.
- Verify Play-delivered installation on the physical Razr and a same-key Play update after a subsequent version code exists.
- Complete the owner-input Console forms: least-privileged reviewer credentials/instructions, save the selected 18+ target audience, content rating, and final Data Safety answers.
- Publish only after explicit authorization.
