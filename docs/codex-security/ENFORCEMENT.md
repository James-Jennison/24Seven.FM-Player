# Codex guardrail enforcement model

Last validated against Codex CLI `0.147.0` on 2026-08-13. This document is an
honest state record: local/user/project controls are active; proposed managed
requirements are not active until the owner installs them at the documented
system location.

Configuration syntax and limits were verified against current official OpenAI
Codex documentation: [managed configuration](https://learn.chatgpt.com/docs/enterprise/managed-configuration), [permissions](https://learn.chatgpt.com/docs/permissions), [advanced configuration](https://learn.chatgpt.com/docs/config-file/config-advanced), and [rules](https://learn.chatgpt.com/docs/agent-configuration/rules).

## Active layers

- System-wide policy: `/home/jjennison/.codex/AGENTS.md`.
- System-wide command rules: `/home/jjennison/.codex/rules/default.rules`.
- System-wide local default: `owner_local_development` permission profile,
  `on-request` approval policy, and app writes set to `writes` approval mode.
- Player policy: root `AGENTS.md`, trusted `.codex/config.toml`, and
  `.codex/rules/player-hard-rules.rules`.
- No `/etc/codex/requirements.toml` was present at validation time.

## Enforcement levels

| Boundary | Scope | Desired state | Actual enforcement | Status |
|---|---|---|---|---|
| Destructive Git (`reset --hard`, selected `clean`, checkout discard) | SYSTEM-WIDE | Block | User rules; managed proposal adds non-bypassable rules | ENFORCED |
| Push, SSH, remote transfer, service/package/release commands | SYSTEM-WIDE | Fresh approval | User `prompt` rules plus `on-request` | APPROVAL-GATED |
| Credentials and policy paths | SYSTEM-WIDE | No ordinary access/mutation | Active default profile denies known user credential paths and keeps workspace policy paths read-only | ENFORCED |
| Full-access profile | SYSTEM-WIDE | Not normally selectable | Active local default is least privilege; managed allowlist is only proposed | OWNER ACTION REQUIRED |
| Gmail | PLAYER | Completely unavailable | Trusted project config disables verified `gmail` app; Player HARD RULE prohibits every path | ENFORCED |
| Prohibited in-app browser | PLAYER | Unavailable | Trusted project config sets `in_app_browser = false` and disables `node_repl`; Computer Use remains enabled | ENFORCED |
| Desktop Computer Use | PLAYER | Preserved, remote mutations gated | Separate `computer-use` MCP remains enabled; policy requires read-only discovery/focus/page check | APPROVAL-GATED |
| Player SSH and production mutation | PLAYER | Only canonical alias and fresh approval | Global SSH prompt, Player forbidden alias rules, explicit Player policy | APPROVAL-GATED |
| Player `gradlew clean` | PLAYER | Block | Project command rules | ENFORCED |
| Policy self-protection | SYSTEM-WIDE + PLAYER | Prevent weakening | Profile read-only paths, selected command rules, and HARD RULE; all-path tamper resistance needs managed policy | OWNER ACTION REQUIRED |
| Player architecture and stream constraints | PLAYER | Preserve design | AGENTS.md guidance; not mechanically decidable | BEHAVIORAL ONLY |
| Discord completion update | PLAYER | Only authorized configured project thread | AGENTS.md approval boundary; no Discord mutation authorized for this milestone | BEHAVIORAL ONLY |

## Managed owner action

`requirements.toml.proposed` is valid only when installed by the owner at
`/etc/codex/requirements.toml` on Linux, then verified in a new Codex session.
It uses the supported `allowed_permission_profiles` mechanism introduced in
Codex `0.138.0`; this installation is `0.147.0`. It intentionally omits
`:danger-full-access`, makes `on-request` the only allowed approval policy, and
adds enforced Git/SSH/release-oriented command boundaries.

Managed requirements are system-wide. `player-managed-extension.toml.proposed`
contains the verified `gmail` app and `in_app_browser` controls, but it must not
be installed unless the owner intends that Gmail and the built-in browser pane
to be disabled for every project. The active Player project config already
applies those restrictions to normal trusted Player sessions.

## Known limitations

- A project config/rule is active only for a trusted project and can be bypassed
  by an owner deliberately supplying higher-precedence CLI options; it is not an
  admin lock. Managed requirements are the required second key for tamper
  resistance.
- Prefix rules inspect local command argument prefixes. They cannot prove SSH
  alias semantics under arbitrary flag ordering or inspect remote shell text.
- `codex execpolicy check` returned no decision for the tested `bash -lc 'git
  status && git push origin main'` wrapper. The audit records direct-command
  enforcement only; shell-wrapper coverage is not claimed and needs a managed
  PreToolUse hook if the owner needs a non-bypassable command-text control.
- Command rules govern commands outside the sandbox. They do not make an
  intrinsically behavioral engineering constraint mechanically decidable.
- No Gmail or browser invocation is used to validate these controls; validation
  is configuration/status inspection only.

## Two-key exception model

1. The owner gives a new, explicit exception naming the HARD RULE, exact action,
   and exact scope after the conflict is known.
2. The applicable prompt/permission/tool boundary is deliberately approved or
   opened.

Neither key authorizes the other. Generic continuation language never supplies
either key.
