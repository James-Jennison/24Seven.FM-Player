# Codex guardrail enforcement model

Last reconciled on 2026-08-13 against the active managed requirements and
official OpenAI Codex managed-configuration documentation.

## Active system-wide baseline

- `/etc/codex/requirements.toml` is the active managed source. It disables the
  `gmail` app system-wide.
- The managed profile allowlist permits `:workspace` and
  `:danger-full-access`; `on-request` remains permitted, as do the prerequisites
  for a deliberate owner `--yolo` invocation. No custom
  `owner_local_development` profile is active.
- Normal local filesystem, private/LAN routing, Docker, ADB, and local-device
  capability are intentionally available. Safety is provided by approval gates,
  command rules, hard rules, and separate app restrictions—not a restrictive
  local sandbox.
- System-wide command policy prompts for SSH, `git push`, `scp`, `rsync`, and
  `systemctl`; it retains destructive-Git protections.

## Canonical SSH roles

| Machine role | Canonical alias | Compatibility state |
| --- | --- | --- |
| Player/site production | `website-vm-admin` | Canonical for Player/site work |
| Mail/Webuzo production | `mail-vm-admin` | Canonical for new mail/Webuzo work |
| Former mail alias | `webuzo-production-admin` | Deprecated compatibility alias only |

The Player repository is authorized to use `website-vm-admin` only for its
website origin. That project-specific constraint does not globally prohibit
the mail role. In Player policy, both mail aliases are blocked solely to prevent
their substitution for the Player website origin.

## Project controls

Player retains its native-architecture requirements, Gmail prohibition,
capability-aware native-browser/Computer Use routing, and Android validation
restrictions. The retired Linux workaround that disabled the native browser and
`node_repl` is not active: ordinary authorized browser navigation uses the
native Browser route, while actual desktop/window interaction uses Computer Use.
These controls add to, rather than replace, the active managed Gmail disablement.

## Validation

Run the system-wide local-only audit from any project:

```bash
/home/jjennison/.codex/audit_system_guardrails.sh
```

Then run the Player-specific audit:

```bash
./scripts/audit_codex_guardrails.sh
```

Both audits avoid Gmail, remote SSH connections, production, push, deployment,
and service mutation. The retired 2026-08-13 proposal files remain only as
historical evidence and must not be installed.
