# Comprehensive project portal validation

Validated July 16, 2026.

## Outcome

The GitHub Pages surface now provides a comprehensive, accessible project portal while preserving the canonical privacy notice at the Pages root. The portal includes:

- a product overview and detailed feature/station boundaries;
- native architecture, source organization, engineering principles, and the milestone workflow;
- automated, emulator, physical-device, accessibility, network, performance, signing, and CI evidence;
- all 23 achieved checkpoints in verified chronological order;
- the seven M23 Play-testing workstreams, deferred M17 boundary, M24 publication gate, and planned M25 Chat Mention Notifications discovery milestone;
- a public resource index for architecture, protocol research, station certification, Play readiness, release notes, testing, and contribution.

Repeated navigation and milestone history are generated from `privacy-site/_data/project.yml`. The existing `PRIVACY.md` generation and `/` permalink are unchanged.

## Verification

- Official `ghcr.io/actions/jekyll-build-pages:v1.0.13` container: pass.
- Seven generated HTML pages, including the root privacy notice: pass.
- Internal page, asset, and fragment link contract: pass.
- Liquid rendering and one active navigation destination per project page: pass.
- Workflow/data YAML parsing and 23-entry chronological milestone contract: pass.
- Chromium review at 1440 × 1000 for Overview, Development, Testing, and Resources: pass.
- Chromium review at 390 × 844 for Overview and Roadmap: pass.
- Full 1440 × 9000 Roadmap review through M17, M24, and M25: pass.
- Every requested local stylesheet, icon, and screenshot returned HTTP 200 with no missing assets.
- `git diff --check`: pass.

This is a static documentation change. It does not alter Android application behavior, station traffic, permissions, signing material, or the release candidate.

## Visual evidence

<table>
  <tr>
    <td width="50%" align="center" valign="top"><img src="screenshots/project-portal-overview.png" alt="Comprehensive project portal Overview page with six-section navigation and native Android hero" width="560"><br><strong>Project overview</strong><br><sub>Shared navigation into Features, Development, Testing, Roadmap, and Resources.</sub></td>
    <td width="50%" align="center" valign="top"><img src="screenshots/project-portal-development.png" alt="Development process page describing evidence-backed vertical slices and native MVVM architecture" width="560"><br><strong>Development process</strong><br><sub>Architecture, principles, milestone lifecycle, source map, and toolchain.</sub></td>
  </tr>
</table>
