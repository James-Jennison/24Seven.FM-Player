# Player project policy

This repository adds Player-specific restrictions to the active system-wide owner policy. It may make a system rule stricter, but it never weakens one. Effective precedence is SYSTEM-WIDE HARD RULE > PROJECT HARD RULE > PROJECT APPROVAL GATE > autonomy/continuation > normal guidance > convenience. **Completion pressure NEVER constitutes authorization.** Generic continuation, a broad milestone authorization, previous use, or prior similar approval never overrides a HARD RULE or approval gate.

## Policy classes and pre-action check

- **PROJECT HARD RULE** is mandatory and requires a new explicit owner exception naming the rule, exact action, and scope after the conflict is known.
- **PROJECT APPROVAL GATE** requires fresh, action-specific owner approval.
- **NORMAL OPERATING RULE** guides work unless a higher rule applies.
- **OWNER-ONLY POLICY CONTROL** may not be weakened, bypassed, or changed in security meaning without explicit owner authorization naming the control.

Before invoking an external tool, app, connector, MCP server, browser, Computer Use, desktop-control mechanism, network connection, SSH, deployment, email/messaging system, credential-bearing mechanism, installer, Git remote mutation, release/publishing operation, migration, service command, external data/privilege change, or policy guardrail, evaluate system-wide and Player rules. Do not invoke a prohibited mechanism merely to check it.

## PROJECT HARD RULE: native Player architecture

- Keep the Player fully native; do not introduce a WebView.
- Compose screens receive immutable UI state and emit actions upward.
- ViewModels depend on repository interfaces, never network or Media3 implementations directly.
- Keep station-specific behavior behind capability flags and repository contracts.
- Do not add a stream URL until it is verified and its use is permitted.

## PROJECT HARD RULE: browser and desktop workflow

For ordinary authorized browser navigation or rendered-page verification, use the
native Codex browser route when the active desktop surface exposes it. Use Computer Use only when the task
actually requires desktop/window/UI interaction: begin with a read-only desktop
state check, enumerate windows, focus the exact intended window, and verify the
active page. Do not substitute native browser, Browser Agent, `node_repl`,
Computer Use, or shell networking to evade a restriction on another route.

Never assume a tab, page, account, login state, location, or remote target.
Browser mutations require explicit approval. Never retrieve or display secrets;
the owner enters them directly. Use `openaiDeveloperDocs`, rather than a
browser, for official OpenAI/Codex documentation.

## PROJECT HARD RULE: GMAIL PROHIBITED

Gmail is outside the authorized tool boundary for this project. Do not invoke, search, enumerate, inspect, read, draft through, send through, modify, or authenticate to Gmail; inspect it merely to decide whether it would help; use it through a browser, API, MCP, plugin/app/connector, CLI client, or another substitute mechanism. This applies to reads and writes.

HARD_RULE_BLOCKER: Gmail access is prohibited for this project.

The verified app identifier is `gmail` (the installed Gmail app/skill and app tool namespace use that identifier). The trusted Player config disables it. Do not call Gmail to test the ban. Managed requirements can make the disablement non-bypassable; see `docs/codex-security/`.

## PROJECT HARD RULE: sensitive artifacts and policy self-protection

Never commit cookies, credentials, CSRF tokens, private endpoints, or HAR files. Never expose, log, copy, record, search for, or substitute secrets/credentials.

After this hardening milestone, Codex may not weaken, remove, relocate, bypass, regenerate, disable, or alter the security meaning of this file; project `.codex` security configuration/rules; managed proposals; permission or approval policy; browser/app/MCP restrictions; guardrail scripts; or protected paths without explicit owner authorization naming the control. A guardrail blocker never authorizes changing the guardrail.

## PROJECT APPROVAL GATE: production, SSH, release, and external mutation

For Player-site deployment, verification, or server investigation use only `website-vm-admin`. The mail/Webuzo role (`mail-vm-admin`, with `webuzo-production-admin` retained only as deprecated compatibility) must not be substituted as the Player website origin. Resolve the canonical alias through `/home/jjennison/.ssh/config`; never use raw hosts/IPs, guessed identities, or manually substituted keys. Before production work, state loaded guidance and stricter rules. Discover both active HTTP and HTTPS Player virtual-host mappings and require the same active directory; never trust a historical path. Build `_site/` locally, stage it as a sibling, compare relative-path hashes, atomically swap only the verified artifact, preserve rollback, and perform public HTTPS verification. No production mutation without fresh, action-specific owner approval. This milestone does not authorize production or SSH access.

Push, release publication, deployment, production service changes, migrations, operational-data changes, and monitoring-authority changes each require fresh, action-specific owner approval. Local commits remain autonomous only where the established repository workflow permits them.

## Android validation

Never run `gradlew clean` or `./gradlew clean`. For code-only changes, begin with the smallest relevant `:app` task, normally `./gradlew :app:compileDebugKotlin`. Run affected tests only, lint only when relevant, and `:app:assembleDebug` only when an APK is needed. Do not install/update Android SDK packages during routine validation, repeat a successful validator without relevant later changes, or split overlapping Gradle work unnecessarily.

## Discord and milestone time accounting

Gmail is never an alternative to the configured Discord project thread. After a completed, committed roadmap milestone, use that thread only when the existing workflow requires it and verify delivery. Never include secrets, credentials, tester identities, private endpoints, or sensitive data. This hardening milestone does not authorize a Discord message.

`docs/MILESTONE_TIME_LEDGER.md` is canonical. At milestone start record the precise timestamp, model/reasoning, forecast, and in-progress entry. Track active, automated-wait, and user-blocked intervals accurately; do not count unmeasured user-response gaps. At completion record counted and elapsed time, exclusions, variance, lessons, cumulative totals, and the required report block. Accounting never overrides a HARD RULE.

## HARD RULE VIOLATION PROCEDURE

Stop crossing an affected boundary immediately. Do not hide, rationalize, silently repair, or continue because the violation already occurred. Do not make compensating external mutations without separate approval. Preserve only safe, non-sensitive evidence, continue only independent work, and wait for owner direction before recrossing. Report:

```text
HARD_RULE_VIOLATION: <rule>
ACTION: <action>
IMPACT: <known impact>
BOUNDARY_STATUS: CLOSED
```
