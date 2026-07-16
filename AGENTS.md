# Contributor guidance

- Keep the project fully native. Do not introduce a WebView.
- Compose screens receive immutable UI state and emit actions upward.
- ViewModels depend on repository interfaces, never network or Media3 implementations.
- Keep station-specific behavior behind capability flags and repository contracts.
- Never commit cookies, credentials, CSRF tokens, private endpoints, or HAR files.
- Do not add a stream URL until it has been verified and its use is permitted.

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
