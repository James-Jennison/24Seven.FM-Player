# Consumer site and developer-workspace split

## Purpose

The Player website now has two explicit audiences. The public root is a listener-first Android app-marketing site. The `/dev/` workspace retains the project material that is useful to contributors, assigned testers, and release coordinators without making it primary visitor content.

## Chosen mechanism

The split stays in the existing Jekyll source and produces one static artifact. This preserves the current digest-pinned build and validation path, avoids a second deployment surface, and keeps public and developer links reviewable together. The `/dev/` pages use their own layout and navigation and are marked `noindex, nofollow`; the consumer sitemap contains only Home, Features, Privacy, and the public Closed Alpha application.

## Route map

| Consumer route | Purpose |
| --- | --- |
| `/` | Listener-first landing page |
| `/features/` | Consumer feature and station overview |
| `/privacy/` | Canonical generated privacy notice, unchanged pipeline |
| `/product-testing/` | Public Closed Alpha application and existing-tester sign-in entry point |

| Developer-workspace route | Retained material |
| --- | --- |
| `/dev/development/` | Engineering and source-map record |
| `/dev/testing/` | Quality and validation evidence |
| `/dev/tester-workspace/` | Assigned task catalog, safety material, and result reporting guidance |
| `/dev/roadmap/` | Milestone and dependency roadmap |
| `/dev/resources/` | Research, architecture, and release references |

The prior `/development/`, `/testing/`, `/roadmap/`, and `/resources/` routes remain as noindex handoff pages to avoid breaking existing links. The developer footer links back to the public site; public footers link to the developer workspace and the canonical existing tester sign-in endpoint.

## Boundaries

No PHP endpoint or portal behavior moved or changed. In particular, the application form still submits to its existing endpoint, and the canonical privacy pipeline remains `PRIVACY.md` to `privacy-site/privacy/index.md`. This change is source and local-static-validation work only; it does not deploy an artifact.
