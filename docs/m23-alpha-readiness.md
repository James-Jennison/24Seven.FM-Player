# M23 Alpha Test Distribution readiness

Prepared early on July 14, 2026. Final milestone review is deferred until M15–M22 are complete.

## Estimate

- T-shirt size: M
- Estimated duration: 1–2 focused days
- Primary confidence variable: selection and custody of the stable release signing identity and distribution channel

## Completed locally

- Version advanced to `0.1.0-alpha01` / version code 2.
- Privacy notice, Alpha tester guide, release notes, and known limitations documented.
- Required permissions reviewed: Internet, network-state awareness, foreground media playback, and Android 13+ notification access only. Network-state access is contributed by the existing media/network dependency manifest.
- Application ID, minimum/target SDK, verified streams, network security, dependencies, and M1–M11 behavior remain unchanged.
- Debug and release candidates are built and inspected without committing APKs or signing material.
- Debug unit tests, affected-module lint, debug APK, unsigned release APK, and unsigned release AAB builds pass.
- All 13 connected instrumentation tests pass on API 35, including native privacy-notice reachability and accessible Favorites availability indicators/request confirmation.
- The Razr upgraded in place from version code 1 / `0.1.0` to the debug-signed version code 2 / `0.1.0-alpha01`, and the native Player launched with its expected accessibility tree.
- Signature inspection confirms the debug APK has only the standard local Android Debug identity; the release APK and AAB remain unsigned as intended.
- Authenticated Favorites browsing is available for all five stations, discovers the current member's station-specific list identifier without hard-coding it, keeps list data in memory only, and reuses the existing explicit one-shot request confirmation.

## Signing boundary

The project intentionally contains no keystore, password, alias, signing token, or signing configuration. The default debug APK is installable for local device validation but is not an appropriate tester artifact: it uses a machine-local debug identity and cannot be upgraded in place to an APK signed by a different production identity.

The selected route is Google Play internal/closed testing with Play App Signing. Gradle now accepts the separate upload-key inputs only through a complete set of process environment variables; partial configuration fails immediately, and the release remains unsigned when none are supplied. Keystore patterns are ignored by Git, and `scripts/validate-play-bundle.ps1` rejects missing, repository-local, or unsigned upload bundles without printing credentials.

Before external distribution, choose one:

Google Play internal/closed testing with Play App Signing is selected. Retain an upload key outside Git and allow Google Play to manage the app-signing key.

Signing files and secrets must never be committed. Gradle should receive their paths and credentials from local ignored properties or CI secrets only after the distribution decision is made.

## Remaining completion work

- Google approved the Play developer account on July 14, 2026. Create the app and accept Play App Signing when M23 begins.
- Create and securely back up the separate upload key outside the repository.
- Produce and verify the signed release APK or AAB.
- Record SHA-256 and signing-certificate fingerprints in the private release record or distribution service.
- Install the signed candidate on a clean test device and verify a same-key version-code update.
- Host `PRIVACY.md` at a stable public web URL before submitting the app in Play Console.
- Publish only after explicit authorization.
