# 24Seven.FM Player contributor guidance

## Architecture and data authority

- Keep the app fully native; do not introduce a WebView. Compose screens receive immutable UI state and emit actions upward. ViewModels depend on repository interfaces, not network or Media3 implementations.
- Keep station behavior behind explicit capability flags and repository contracts. Do not add a stream URL until it is verified and permitted.
- The Player consumes station-provided playback, metadata, and approved shared status. It must not become an authoritative writer of Now Playing, incidents, relay health, listener telemetry, or administrative state.
- Collect or expose listener/monitoring data only where PRIVACY.md and documented project contracts explicitly authorize it. Never commit credentials, cookies, CSRF tokens, private endpoints, HAR files, or tester identities.

## Android validation and release

- Never run `gradlew clean` during routine work. Start code-only checks with `./gradlew :app:compileDebugKotlin`; run affected unit tests for domain/ViewModel changes. Assemble an APK only when needed.
- Do not reinstall Android SDK packages or repeat unaffected successful validators.
- Signing, distribution, production stream settings, background-media behavior, and external publication require project release validation and explicit approval.

## Milestones

Keep `docs/MILESTONE_TIME_LEDGER.md` accurate during authorized milestone work. Update roadmap, evidence, and approved Discord milestone communication only after the relevant acceptance gate is complete; never include sensitive details.
