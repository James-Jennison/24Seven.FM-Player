# Contributor guidance

- Keep the project fully native. Do not introduce a WebView.
- Compose screens receive immutable UI state and emit actions upward.
- ViewModels depend on repository interfaces, never network or Media3 implementations.
- Keep station-specific behavior behind capability flags and repository contracts.
- Never commit cookies, credentials, CSRF tokens, private endpoints, or HAR files.
- Do not add a stream URL until it has been verified and its use is permitted.

