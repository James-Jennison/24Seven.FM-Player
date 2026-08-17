# Contributor guidance

- Keep the project fully native. Do not introduce a WebView.
- Compose screens receive immutable UI state and emit actions upward.
- ViewModels depend on repository interfaces, never network or Media3 implementations.
- Keep station-specific behavior behind capability flags and repository contracts.
- Never commit cookies, credentials, CSRF tokens, private endpoints, or HAR files.
- Do not add a stream URL until it has been verified and its use is permitted.

## Player project site deployment

- For any 24Seven.FM Player website deployment, verification, or server-side investigation, use only the canonical SSH alias `website-vm-admin`.
- Do not use `webuzo-production-admin` for this project. Resolve the required alias through the managed SSH configuration before connecting.
- Discover the live static artifact from the active HTTP and HTTPS `player.jamesjennison.net` virtual-host mappings; both mappings must resolve to the same directory. Do not rely on a stale documented path or infer a target from another site.
- Build the reviewed `_site/` artifact locally, stage it as a sibling directory, compare file hashes using relative paths, then atomically swap the verified staging directory into place. Retain the previous live directory as the rollback point and run public HTTPS verification before reporting success.

## Android validation

- Never run `gradlew clean` during routine development or validation. Preserve Gradle caches, incremental outputs, and existing build products.
- Use the smallest relevant `:app` task. For code-only changes, start with `./gradlew :app:compileDebugKotlin` (or `.\gradlew.bat :app:compileDebugKotlin` on Windows).
- Run unit tests only for affected modules when possible, and run lint only when the change can affect lint results.
- Use `:app:assembleDebug` only when an APK is needed. Reserve the full build for milestones, release preparation, or an explicit request.
- Do not reinstall or update Android SDK packages during normal validation.
- Do not repeat a successful validator unless later changes could affect its result.
- Combine overlapping Gradle tasks into one invocation when that avoids duplicate configuration and work.

## Milestone communication

- Milestone announcements are optional and occur only when the user explicitly requests one.
- Any requested announcement must remain free of secrets, credentials, tester identities, private endpoint details, and other sensitive data.

## Milestone time ledger

- Treat `docs/MILESTONE_TIME_LEDGER.md` as the canonical cumulative record of milestone time.
- When milestone execution is authorized, immediately record the actual start timestamp in unambiguous 12-hour format with the full date, seconds, timezone abbreviation, and UTC offset; also record the approved model and reasoning strength, original forecast, and an in-progress entry.
- Record active, automated-wait, and user-blocked pause/resume intervals as they occur; never count an unmeasured user-response gap as active work.
- At completion, calculate counted project time, total elapsed time, user-blocked exclusion, forecast variance, lessons, and cumulative totals, then commit the ledger with the milestone documentation and include the ledger's required time block in the completion report.
