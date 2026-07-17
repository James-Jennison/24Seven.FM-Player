# Comprehensive project portal validation

Validated July 16, 2026.

## Outcome

The GitHub Pages surface now provides a comprehensive, accessible project portal while preserving the canonical privacy notice at the Pages root. The portal includes:

- a product overview and detailed feature/station boundaries;
- native architecture, source organization, engineering principles, and the milestone workflow;
- automated, emulator, physical-device, accessibility, network, performance, signing, and CI evidence;
- a tester-facing product workspace with bounded smoke, member, device, resilience, and adaptive sessions plus a structured public result form for passes, failures, notes, and blocked tests;
- all 25 achieved milestones in verified chronological order, including completed M24 Sleep Timer and M25 Audio-Output Selection;
- the seven M23 Play-testing workstreams, deferred M17 boundary, completed M24–M25, remaining M26–M27 pre-Alpha feature gates, and M28 Alpha publication milestone;
- a public resource index for architecture, protocol research, station certification, Play readiness, release notes, testing, and contribution.

Repeated navigation and milestone history are generated from `privacy-site/_data/project.yml`. The existing `PRIVACY.md` generation and `/` permalink are unchanged.

## Verification

- Official `ghcr.io/actions/jekyll-build-pages:v1.0.13` container: pass.
- Eight generated HTML pages, including the root privacy notice: pass.
- Internal page, asset, and fragment link contract: pass.
- Liquid rendering and one active navigation destination per project page: pass.
- Workflow/data YAML parsing and 25-entry chronological milestone contract: pass.
- Chromium review at 1440 px for Overview, Development, Testing, Product Testing, and Resources: pass.
- Chromium review at 390 px for Overview, Roadmap, and the complete Product Testing page: pass.
- Product-testing issue-form YAML with 17 blocks and 16 unique field IDs: pass.
- Full 1440 × 9000 Roadmap review through M17 and the reordered M24–M28 sequence: pass.
- Every requested local stylesheet, icon, and screenshot returned HTTP 200 with no missing assets.
- `git diff --check`: pass.

This is a static documentation change. It does not alter Android application behavior, station traffic, permissions, signing material, or the release candidate.

## Visual evidence

<table>
  <tr>
    <td width="50%" align="center" valign="top"><img src="screenshots/project-portal-overview.png" alt="Comprehensive project portal Overview page with six-section navigation and native Android hero" width="560"><br><strong>Project overview</strong><br><sub>Shared navigation into Features, Development, Testing, Roadmap, and Resources.</sub></td>
    <td width="50%" align="center" valign="top"><img src="screenshots/project-portal-development.png" alt="Development process page describing evidence-backed vertical slices and native MVVM architecture" width="560"><br><strong>Development process</strong><br><sub>Architecture, principles, milestone lifecycle, source map, and toolchain.</sub></td>
  </tr>
  <tr>
    <td colspan="2" align="center" valign="top"><img src="screenshots/project-portal-roadmap.png" alt="Roadmap page showing M24 Sleep Timer, M25 Cast and audio-output selection, M26 In-App Diagnostics, M27 Community Push Notifications, and M28 Alpha Publication" width="760"><br><strong>Reordered pre-Alpha roadmap</strong><br><sub>Four product milestones before publication, including the capability-gated Private Message notification contract.</sub></td>
  </tr>
  <tr>
    <td colspan="2" align="center" valign="top"><img src="screenshots/project-portal-product-testing.png" alt="Product Testing workspace with preparation steps, three bounded test sessions, and an evidence matrix" width="760"><br><strong>Product testing workspace</strong><br><sub>Focused sessions and reproducible evidence before the structured public GitHub result form.</sub></td>
  </tr>
</table>
