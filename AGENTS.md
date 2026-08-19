# Contributor guidance

- Keep the project fully native. Do not introduce a WebView.
- Compose screens receive immutable UI state and emit actions upward.
- ViewModels depend on repository interfaces, never network or Media3 implementations.
- Keep station-specific behavior behind capability flags and repository contracts.
- Never commit cookies, credentials, CSRF tokens, private endpoints, or HAR files.
- Do not add a stream URL until it has been verified and its use is permitted.

## Repository boundaries and legacy web material

- The native Android application belongs here. New public-website work belongs in `24Seven.FM-Website`; protected tester-onboarding work belongs in private `24Seven.FM-Onboarding`.
- `privacy-site/` and the legacy website/onboarding scripts remain only as a recovery reference for the unchanged deployed artifact. Do not modify, deploy, or validate them for new feature work.
- A separately authorized recovery may inspect this legacy copy. Any website server-side investigation uses only the canonical SSH alias `website-vm-admin`; never use `webuzo-production-admin`.
- No repository split authorizes a live deployment, DNS/TLS/Cloudflare change, database migration, or removal of the legacy recovery copy.
- `dev.jamesjennison.net` is the independently served Product Development Workspace. It is a protected live surface: never use it as a substitute target for Product or Onboarding work, and do not change it without separately naming that hostname and scope.

## Android validation

- Never run `gradlew clean` during routine development or validation. Preserve Gradle caches, incremental outputs, and existing build products.
- Use the smallest relevant `:app` task. For code-only changes, start with `./gradlew :app:compileDebugKotlin` (or `.\gradlew.bat :app:compileDebugKotlin` on Windows).
- Run unit tests only for affected modules when possible, and run lint only when the change can affect lint results.
- Use `:app:assembleDebug` only when an APK is needed. Reserve the full build for milestones, release preparation, or an explicit request.
- Do not reinstall or update Android SDK packages during normal validation.
- Do not repeat a successful validator unless later changes could affect its result.
- Combine overlapping Gradle tasks into one invocation when that avoids duplicate configuration and work.

## Milestone communication

- After completing and committing a roadmap milestone, post a concise completion update to the configured Discord project thread and verify delivery before reporting the milestone complete.
- Keep Discord milestone updates free of secrets, credentials, tester identities, private endpoint details, and other sensitive data.

## Milestone time ledger

- Treat `docs/MILESTONE_TIME_LEDGER.md` as the canonical cumulative record of milestone time.
- When milestone execution is authorized, immediately record the actual start timestamp in unambiguous 12-hour format with the full date, seconds, timezone abbreviation, and UTC offset; also record the approved model and reasoning strength, original forecast, and an in-progress entry.
- Record active, automated-wait, and user-blocked pause/resume intervals as they occur; never count an unmeasured user-response gap as active work.
- At completion, calculate counted project time, total elapsed time, user-blocked exclusion, forecast variance, lessons, and cumulative totals, then commit the ledger with the milestone documentation and include the ledger's required time block in the completion report.
