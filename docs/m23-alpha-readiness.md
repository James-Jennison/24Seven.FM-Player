# M23 Alpha Test Distribution readiness

Prepared early on July 14, 2026 and activated July 15 after M15–M22 completed. A July 15 Play-readiness audit expanded
M23 into the seven reviewable sub-milestones maintained in `README.md` and `docs/IMPLEMENTATION_PLAN.md`. This file
retains the detailed release/signing evidence and records the additional completion gates without representing them
as already complete.

## Estimate

- Task Complexity Level: 2 — Feature Logic & API Integration
- T-shirt size: XL umbrella divided into M23.1–M23.7 S–L milestones
- Estimated duration: 3–6 focused days plus owner and station-side input
- Rationale: the app implementation is stable, but Play readiness now explicitly spans current-head artifact integrity,
  UGC moderation, API 36 behavior, privacy/data-safety declarations, rights evidence, Play configuration, tester guidance,
  broad device validation, and an upgrade-safe signing lineage.
- Primary confidence variables: station moderation capabilities, rights evidence, reusable reviewer access, and protected
  signing/Play delivery

## Task breakdown

1. Reconcile privacy, data-safety, tester, release-note, permission, and dependency documentation with M18–M22.
2. Confirm version/application identity and release Gradle behavior without upgrading the toolchain.
3. Create or locate the upload key outside Git, configure complete local-only signing inputs, and record private fingerprints.
4. Build and validate the signed release AAB with `scripts/validate-play-bundle.ps1`.
5. Create/configure the Play app, accept Play App Signing, supply the public privacy URL and required declarations.
6. Install or distribute a same-key candidate, run the final physical smoke test, and publish only the readiness evidence.
7. Resolve public UGC policy requirements through real Terms/report/block/moderation behavior or remove the affected surfaces from the Play candidate.
8. Target API 36 and validate Android 16 predictive Back, edge-to-edge, adaptive, and background-media behavior.
9. Complete the Play media-playback foreground-service declaration and demonstration evidence.
10. Privately retain explicit brand, artwork, stream, screenshot, and Play-distribution authorization.
11. Close API 26, 16 KB runtime, genuine tablet/foldable, physical hinge, Play pre-launch, and Play-delivery coverage.
12. Add adaptive/monochrome launcher resources before a public store listing.

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
- The current protected `0.1.0-alpha01` AAB from commit `2086ab9` validates with SHA-256 `1C6C43BF947B844F5D8708DF368635CFE34B6A690DD826B87663B66C5C6C518F`; its signer exactly matches upload-certificate SHA-256 `F6E8E81271964FFC3F8A0D548B49B4DB93AEFC48CCB74B8744512670F4279E3F`. The companion signed APK has SHA-256 `923C26621FC998CAA4D1099B7BCA5A118C7EB8256057D96E9971C4AFE29825D0` and verifies with the same certificate. AAB hashes are recorded per build because signed output contains per-build data.
- The PowerShell 7 recovery workflow exports a PBKDF2/AES-256-GCM package, rejects wrong passphrases and repository-local targets, validates the recovered keystore/certificate, and restores a new DPAPI envelope. Its tested round trip reproduced the keystore byte-for-byte and signed/verified a release AAB from the restored copy; all temporary test artifacts were removed.
- The Ubuntu protected-signing helper consumed that same authenticated recovery format without changing the upload identity, rejected the wrong passphrase/hash/certificate and repository-local packages, kept its mode-`600` keystore only on memory-backed `/dev/shm`, used a non-persistent Gradle process, and produced the current signed AAB/APK. Eight focused tests pass, including realistic verification of a bundle using a self-signed upload certificate.
- The actual encrypted recovery package is now stored on the owner-selected off-PC volume and independently validated with the owner-held passphrase. The encrypted package SHA-256 is `361E6A85452DBF9ACDC816F554569E3B7DBA0F98B60C30DB255E54C2644C4D1C`; neither its location nor its passphrase is committed.
- The signed release APK was installed cleanly on the API 35 emulator, cold-launched as version code 2, exposed the expected adaptive Player semantics, and reached `Playing live` on StreamingSoundtracks with the Pause action available. The emulator was restored to the debug build afterward; the wirelessly connected Razr and its sessions were not modified.
- The release dependency graph contains no advertising, analytics, Crashlytics, App Center, or Sentry SDK.
- M23.1's current local release audit confirms the production manifest, complete resolved dependency inventory, 16 KB
  APK/ELF packaging, explicit cloud/device-transfer backup exclusions, and intentionally unsigned release AAB/APK.
  Apache-2.0, MIT, GPLv2-with-Classpath-Exception, and MPL-2.0 notices are recorded in
  `THIRD_PARTY_NOTICES.md` and are reachable inside the app under More → Privacy → Open-source licenses. See
  `m23-release-candidate-audit.md`; protected current-head signing is complete, while version-code eligibility and
  Play-delivered installation remain open.
- Google Play accepts target API 35 on July 15, 2026 but requires API 36 for new apps and updates beginning August 31, 2026. M23.3 therefore migrates before the closed-test/update window instead of treating API 35 as the final Alpha target.
- The project-specific provisional Data Safety worksheet is recorded in `docs/m23-data-safety.md`; it includes the M23.2 age/Terms/block preferences and transient abuse-report fields. Final answers remain gated on the active Play Console form and station retention confirmation.
- The non-secret M23.4 declaration packet now contains copy-ready media-playback foreground-service text, an exact demonstration shot list, reviewer navigation instructions, content-rating evidence boundaries, and owner-only fields. The draft store listing was corrected to remove unimplemented sleep timer, shortcuts, sharing, appearance preferences, and diagnostics.
- A credential-free M23.4 media-playback video rehearsal passed frame inspection on the API 35 Pixel Tablet: start,
  buffering/playback, Home, expanded media notification, notification-body return, pause, resume, and stop are visible.
  It remains an uncommitted debug-build rehearsal; the final hosted video must use the protected signed candidate after
  M23.1 and M23.5 close.
- The non-secret `m23-owner-response-packet.md` now provides exact report-receipt choices, a rights/stream/distribution
  authorization request, a station-retention table, reviewer-account confirmation, and the evidence required before
  Private Messaging research can resume. No private response, credential, or rights evidence is committed.
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
- The compact More experience now prioritizes the selected station account, hides other station accounts until requested,
  and places song requests, request activity, and device preferences behind accessible saved-state disclosures. Browser
  links are limited to the mobile-relevant Contact and Membership pages.
- Trusted HTTPS chat emoticons under each station's verified ClearChat smiley path render inline without permitting
  cross-origin images; bounded alt text remains the plain-text and accessibility fallback. Submitted phpBB emoticon
  codes are normalized only for the post-send visible-message confirmation.
- The earlier Chat/More refinement checkpoint passed debug Kotlin compilation, all 111 debug unit tests, debug lint,
  and Android-test Kotlin compilation. Updated Chat and More screenshots are preserved with that change; the later
  post-fix count and validation are recorded below.
- The Android media notification now carries an immutable activity `PendingIntent`; tapping anywhere outside the
  play/pause action reopens the existing one-activity player task instead of creating a second playback process.
- A focused API 35 Pixel Fold test verifies that folded portrait shows playback controls and the complete five-station
  carousel without scrolling. The physical Razr remains the OEM/foldable hardware baseline in its normal main-display
  state; a mechanically half-open posture remains an Alpha test item.
- Station-library searches now preserve the selected Title, Album, Artist, or Genre field through native navigation,
  and Library/Favorites expose stable station-order and play-state sorting.
- Favorite availability resolution is indexed across the complete result set. A generated 1,500-track render/sort test
  passed, and the preserved full MorG Favorites list loaded and switched sorting on the physical Razr without an ANR.
- The post-fix suite contains 117 unit-test methods. GitHub Android CI passed unit tests, lint, and debug assembly after
  the changes; focused folded-layout, notification-navigation, multi-field search, and full-list checks also passed.
- M23.3 migrated the app to target API 36 without changing the compile or minimum SDK. All 117 unit tests, debug lint,
  release bundle/lint-vital checks, and 27/27 connected tests pass on the API 36 AVD. A cold launch reported target 36,
  and two Back actions opened the existing exit dialog with edge-to-edge content intact. See `m23-api36-readiness.md`.
- M17 browser investigation reached Inbox and Sent Box, found a suspect New Message selection path, and reproduced a
  profile-originated MorgHubby send that appeared to submit but was absent from the recipient account. The result was
  reported to the site owner; no native PM endpoint or behavior was guessed.

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
- Before upload, confirm whether version code 2 remains available and verify that the selected artifact hash matches the
  protected candidate recorded in `m23-release-candidate-audit.md`.
- Verify Play-delivered installation on the physical Razr and a same-key Play update after a subsequent version code exists.
- Complete the owner-input Console forms: least-privileged reviewer credentials/instructions, save the selected 18+ target audience, content rating, and final Data Safety answers.
- Confirm administrator receipt of the single authorized M23.2 harmless report. Native age/Terms/mature-content gates, separate report-content/report-user/block-user actions, local block management, and duplicate-safe report handling are implemented and test-green.
- **Complete:** API 36 target migration and Android 16 regression pass; see `m23-api36-readiness.md`.
- Save the media-playback foreground-service declaration with its description, interruption impact, use case, and demonstration video.
- Privately confirm written rights for the app/station branding, artwork, streams, screenshots, and Play distribution.
- Close minimum-API, 16 KB runtime, genuine tablet/foldable, physical Razr hinge, and Play pre-launch coverage.
- **Complete:** adaptive/monochrome launcher resources preserve the established logo across legacy, masked, and Android 13 themed-icon surfaces; see `m23-launcher-polish.md`.
- Publish only after explicit authorization.
