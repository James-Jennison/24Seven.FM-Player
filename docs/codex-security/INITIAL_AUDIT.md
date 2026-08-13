# Initial hardening audit — 2026-08-13

This record was captured before the authoritative hardening edits. It contains
only configuration metadata and filenames; it contains no credential values,
tokens, cookies, or auth material.

| Item | Observed state |
|---|---|
| Repository root | `/mnt/faststorage/24Seven.FM-Player` |
| Branch | `codex/private-tester-queue` |
| HEAD | `5b3bed2e047cc56f3a8964dc985c5ab5065d711d` |
| Codex CLI | `0.147.0` |
| `CODEX_HOME` environment variable | Unset; default effective path `/home/jjennison/.codex` |
| Player project trust | Trusted in user `config.toml` |
| Global active guidance | `/home/jjennison/.codex/AGENTS.md`; no global override |
| Project active guidance | root `AGENTS.md`; no project/nested override |
| Guidance size limit | No custom `project_doc_max_bytes`; documented default is 32 KiB |
| Managed requirements/config | No `/etc/codex/requirements.toml`, `/etc/codex/managed_config.toml`, user requirements, or user managed config |
| Initial approval policy | `never` |
| Initial sandbox model | legacy `workspace-write` |
| Initial browser-related feature states | `in_app_browser`, `browser_use`, external browser use, full CDP, and Computer Use were enabled |
| Initial local MCP state | `computer-use` and `node_repl` enabled; configured Cloudflare and OpenAI developer-docs MCPs visible |

The initial working tree included unrelated pre-existing modifications and
untracked Turnstile-related files. They were preserved and are not part of this
hardening change:

```text
M  privacy-site/.htaccess
M  privacy-site/alpha-tester-interest.php
M  scripts/build-project-site.sh
M  scripts/test-alpha-tester-signup-confirmation.php
M  scripts/validate-project-site.py
M  scripts/validate-project-site.sh
?? .wrangler/
?? privacy-site/turnstile-test.php
?? privacy-site/turnstile-test/
```

The only earlier hardening-related worktree modification was the in-progress
milestone ledger entry. This authoritative milestone supersedes the cancelled
partial attempt.
