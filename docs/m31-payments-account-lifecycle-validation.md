# M31 Payments, External Links, and Account Lifecycle validation

Date: July 18, 2026

Model/review structure: Sol Extra High policy and architecture audit; Terra-bounded implementation; Sol High acceptance

Implementation commit: `9e340b9`

Status: complete

## Decision

The global Google Play Alpha does not expose station membership, subscription, donation, registration, recovery,
account-management, or account-deletion browser routes. It continues to support native sign-in to independently
existing station accounts and can display a station-reported membership state where that read-only contract was
already verified. **Sign out** removes only the local protected session.

This is the conservative compliant baseline. The Player is not enrolled in a region-specific external-content,
external-offers, or alternative-billing program; it has no Play Billing implementation, regional link gating,
external-link API integration, transaction reporting, refund/dispute workflow, or authorization to operate station
payments. M31 therefore removes the purchase-capable routes instead of representing them as informational links.

## Exact shipped-route inventory before M31

| User-facing route | Artifact behavior before M31 | Live/public finding | M31 result |
| --- | --- | --- | --- |
| Contact Us, all five stations | Fixed monitored recipient through `ACTION_SENDTO`; station-specific subject/body; user-controlled send | No payment or account creation; M28 handoff and receipt validated | Retained |
| VIP membership, SST/1980s/Adagio/Entranced | Exact same-station `VIP_Subscribe` page in a Custom Tab | HTTP 200 on July 18; advertises a digital subscription with additional request frequency, request history/readiness, Favorites capacity, forum access, and stream access; signed-out page directs users to register | Removed from every station catalog |
| RIP membership, Death.FM | Exact same-station `RIP_Subscribe` page in a Custom Tab | HTTP 200 on July 18; same digital benefit and registration pattern under RIP branding | Removed from the Death.FM catalog |
| Privacy-dialog GitHub Issues button | Compose opened the repository issue list directly through `LocalUriHandler` | External support route can require an unrelated GitHub account and bypassed the established activity-owned Contact handoff | Removed; native notice directs users to More → Contact Us |
| Diagnostics Share | Explicit Android chooser containing the user-reviewed allowlist snapshot | No fixed purchase/account destination | Retained |
| Moderation report | Fixed-recipient transient email draft after explicit review | No payment/account creation; no delivery claim | Retained |

`Station.websiteUrl` remains internal station-origin metadata used for trust comparisons and network repositories; it is
not a user-facing launcher. `StationPageKind` retains historical/future kinds for stable domain compatibility, but the
current bootstrap catalog contains only Contact. The trust policy explicitly rejects `Membership` even if it is
accidentally catalogued.

## Policy basis

Google Play classifies music/content subscriptions and app functionality as digital goods or services. Outside an
applicable exception or enrolled regional program, an in-app button, link, message, sign-up flow, or other call to
action may not lead users to a non-Play payment method. The current US external-content-links program also requires
prior enrollment, US-only availability, API integration, disclosures, customer support, refund/dispute handling, and
other program obligations that this project has neither implemented nor been authorized to assume.

The five station pages are not pure donations: the public pages expressly connect payment to digital benefits usable
in or alongside the Player. The tax-exempt-donation exception therefore is not used. No transaction was attempted and
no account, payment, or personal data was entered during this audit.

Google Play requires every developer to answer the Data Safety deletion questions. The additional in-app and web
account-deletion paths apply when an app enables account creation. The M31 artifact does not create a station account
or direct users to create one. Station registration, recovery, management, and deletion remain unavailable pending
the separately authorized M57 program. This conclusion does not convert local Sign out into station-side deletion and
does not resolve M29's operator retention/deletion facts.

## Implementation boundary

- `BootstrapStationRepository` exposes exactly one Contact page per station and no VIP/RIP membership page.
- `StationPageTrustPolicy` fails closed for `Membership`, even if an entry is supplied outside the bootstrap catalog.
- The More directory describes only the reviewed email-draft handoff.
- The native privacy notice has no direct browser launcher and points to the existing Contact Us action.
- Native authentication, Favorites, requests, Chat, SST listener activity, and read-only membership evidence are unchanged.
- No WebView, Play Billing dependency, alternate payment SDK, regional inference, or external-link API was added.

Any future browser route must be audited through its reachable downstream navigation, not only its initial URL. The
legacy station shell exposes subscription and registration navigation from otherwise unrelated pages, so M51 Forum
links and M57 account routes remain gated until their exact Play, authorization, session, privacy, and deletion
contracts are approved.

The requested future ability to purchase and activate VIP/RIP is now preserved as a separate nonblocking program:

- **M58 Membership Commerce Authorization and Billing Architecture** — Sol Extra High establishes station/merchant
  authority, the Play Billing or enrolled regional program choice, station products, server verification/activation,
  support/refund/tax, privacy, security, and account-lifecycle contracts.
- **M59 Native VIP/RIP Purchase and Activation** — Terra High implements only the approved native purchase, pending,
  verification, activation, restore, and failure states with server-verified station/account isolation.
- **M60 Subscription Lifecycle and Five-Station Certification** — Sol Extra High accepts renewal, cancellation,
  expiry, refund, revocation, restore/reinstall, device/account switching, accessibility, Play, support, and independent
  VIP/RIP evidence across all five stations.

M58–M60 do not add billing behavior to this Alpha and do not weaken the M31 Contact-only boundary.

## Acceptance matrix

| Gate | Required evidence | Result |
| --- | --- | --- |
| Catalog | Every station exposes Contact and no Membership | Pass — repository contract and 148/148 unit tests |
| Trust policy | An accidentally catalogued membership route returns unavailable | Pass — focused fail-closed policy test |
| UI | More shows Contact only; Privacy directs questions to Contact and contains no browser button | Pass — 2/2 focused connected tests and live physical Razr inspection |
| Account lifecycle | No native or external account creation/recovery/management/deletion route; Sign out remains local-session removal | Pass by source and route inventory |
| Payment boundary | No in-app purchase, external purchase link, donation link, billing SDK, or transaction claim | Pass by source/dependency inventory |
| Regression | Authentication, playback, requests, Favorites, Chat, and read-only membership evidence remain unchanged | Pass — 148/148 unit tests, debug lint, Android-test compilation, and focused Razr suite |
| Documentation | README, privacy, architecture, endpoint/matrix, release notes, 38-case tester catalog, M01–M60 roadmap, portal, and GitHub Project agree | Pass — synchronized to implementation commit `9e340b9` |

## Local validation

- `:app:testDebugUnitTest` — **148/148 passed**.
- `:app:lintDebug` — passed.
- `:app:compileDebugAndroidTestKotlin` — passed.
- Focused `RadioAppTest` on the physical Motorola Razr 2023 running Android 16 — **2/2 passed** after correcting
  the long-dialog scroll assertion; the Contact-only test passed in both runs.
- `:app:installDebug` — passed on the Razr for live visual inspection.
- Physical Razr inspection — More showed one unclipped Contact card and no Membership/browser card; the native Privacy
  dialog displayed the no-station-site/no-VIP/RIP/no-account-route statement. Playback was never started and the app
  was force-stopped after inspection.
- Official `ghcr.io/actions/jekyll-build-pages:v1.0.13` container — seven source portal pages rendered successfully;
  the Product Testing output contained 38 unique cases and the roadmap rendered M01–M60/M58–M60.
- `node --check privacy-site/assets/project.js` and `git diff --check` — passed.

## Official references

- [Google Play Payments policy](https://support.google.com/googleplay/android-developer/answer/9858738)
- [Understanding Google Play's Payments policy](https://support.google.com/googleplay/android-developer/answer/10281818)
- [US external content links program](https://support.google.com/googleplay/android-developer/answer/16470497)
- [Google Play account deletion requirements](https://support.google.com/googleplay/android-developer/answer/13327111)

This is a technical policy-boundary record, not legal advice or a guarantee of Play approval. Re-audit the exact
candidate and active Console/program terms before any future transaction or account-lifecycle route is introduced.
