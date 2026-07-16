# M23 modern Android device compatibility matrix

Validated through July 16, 2026 for the `0.1.0-alpha01` candidate. The product requirement is adaptive support across
current Android phones, tablets, foldables, multi-window, and freely resized windows. This is a window-size contract,
not a list of hard-coded device models.

## Supported runtime and layout contract

- Android 8.0 / API 26 is the minimum runtime; Android 16 / API 36 is both the target and compile level.
- Compact windows below 600dp use bottom navigation. Medium and expanded windows use a navigation rail.
- The Player switches from a vertically scrollable compact layout to a two-pane artwork/details layout at 840dp.
- Selected station, destination, playback state, and the service-owned media session are not recreated or duplicated
  when the available window size changes.
- Edge-to-edge content, system bars, display cutouts, large text, portrait, landscape, and freeform resizing are handled
  from the available Compose window rather than from device-name checks.

## Runtime evidence

| Device/runtime | Effective window | Evidence | Result |
| --- | --- | --- | --- |
| Motorola Razr 2023, Android 16 / API 36 | 1080×2640 at 420dpi main display; 194×368 system-controlled external display | Current-head in-place install; measured 180° `OPENED`, 110° `HALF_OPENED_MAIN`, and 0° `CLOSED_HALL` cycle; `phone-player-live-playing.png`, `phone-queue-live.png`, `phone-stations.png` | Playback, the existing process/task, and live metadata remain continuous through open, tabletop, fully closed, and reopened states. Main-display artwork, metadata, controls, carousel, and navigation remain reachable without clipping; the small external display does not host normal app activities. No crash or ANR occurred. |
| API 35 Pixel 7 emulator | 411×731dp compact portrait | `phone-more.png`; connected UI suite | Compact navigation and supporting destinations render at an exact 1080×1920 Play screenshot size. |
| API 26 phone AVD | Minimum supported runtime | 36/36 current-head connected tests | Minimum-SDK installation, launch, compact navigation, playback UI, UGC safety, legal disclosures, and the established feature suite pass without changing SDK packages or clearing build caches. One test was corrected to scroll to a More-menu disclosure before asserting viewport visibility. |
| API 35 16 KB phone AVD | 411×731dp compact portrait; 16,384-byte runtime page size | 36/36 current-head connected tests; `PAGE_SIZE=16384` | The app installs, launches, and passes the complete current suite on a genuine 16 KB runtime. |
| API 35 Pixel 7 emulator resized as a 7-inch class window | 720×1280dp portrait | `tablet-7-player2.png` | Navigation rail replaces bottom navigation; Player controls and station cards remain visible. |
| API 35 Pixel 7 emulator resized as a 10-inch class window | 960×1707dp portrait | `tablet-10-player.png` | Expanded two-pane Player and navigation rail render without stretching the compact layout. |
| API 35 Pixel 7 emulator, expanded landscape/freeform | 1707×960dp landscape | `tablet-landscape-player.png` | Two-pane Player, navigation rail, controls, and all five station cards are simultaneously reachable. |
| API 35 Pixel Fold emulator, open inner display | 2208×1840 physical display | 36/36 current-head connected tests plus live emulator inspection | Expanded layout and the complete current feature suite pass on the genuine foldable inner display. |
| API 35 Pixel Fold emulator, half-open inner display | Device-state 1; 2208×1840 physical display | Earlier 33/33 full suite; 2/2 current-head More/disclosure tests | The full feature suite and the current-head More-menu paths pass across the hinge-state transition. Intermittent gfxstream black tiles observed in the earlier pass cleared after an activity rerender and produced no application crash or semantic-test failure. |
| API 35 Pixel Fold emulator, closed outer display | Device-state 0; 1080×2092 physical display, inner display off | Earlier 33/33 full suite; 2/2 current-head More/disclosure tests; cold-launch inspection | Compact Player controls and the horizontally reachable station carousel are visible; the full feature suite and current-head More-menu paths pass on the genuine outer display. |
| API 35 Pixel Tablet emulator, native landscape | 2560×1600 physical display; 1280×800dp | 40/40 current-head connected tests plus cold-launch inspection | Expanded two-pane Player, navigation rail, artwork, controls, station carousel, full 1,500-track Favorites traversal, and the complete current feature suite pass. |
| API 35 Pixel Tablet emulator, native portrait | 1600×2560 physical display; 800×1280dp | Earlier 34/34 full suite; 1/1 current-head open-source disclosure test; rotation inspection | Medium-width rail layout retains visible artwork, controls, and a horizontally reachable station carousel after rotation; the current legal-disclosure path is also scroll-reachable. |
| API 36 phone AVD targeting API 36 | 411×914dp compact portrait | 36/36 current-head connected tests plus cold-launch/two-Back inspection | Adaptive navigation, UGC safety, legal disclosures, Favorites lazy scrolling, MediaSession service, edge-to-edge Player, and the exit dialog pass under Android 16 target behavior. |

### Maximum tested accessibility settings

| Device/posture | Stress profile | Evidence | Result |
| --- | --- | --- | --- |
| API 35 phone AVD | 343×762dp effective compact window; Android font scale 2.0; 504dpi display density | Focused accessibility tests; `m23-large-text-player.png`; `m23-large-text-account.png`; live Player, More, Favorites, Queue, and Chat inspection | All five navigation destinations remain distinct and clickable; playback, growing station descriptions, account identity/status, list/error states, and age-gate inputs remain reachable without clipped text. |
| API 35 Pixel Fold, open inner display | 701×584dp effective medium window; Android font scale 2.0; 504dpi display density | Focused medium-layout test and live inspection | The navigation rail, playback action, scrolling Player, and full station descriptions remain reachable in the short-wide inner-display layout. |
| API 35 Pixel Fold, closed outer display | 1080×2092 physical outer display; Android font scale 2.0 | Live closed-state inspection | Compact controls, bottom navigation, and growing station cards remain reachable after the genuine fold-state transition. |
| API 35 Pixel Tablet, native landscape | 1067×667dp effective expanded window; Android font scale 2.0; 384dpi display density | 39/39 connected tests and live Player/More inspection | The complete suite passes at the maximum tested text scale and enlarged display; two-pane controls, station descriptions, navigation semantics, and stacked account headers remain reachable. |

All display and font overrides were temporary. The Razr retained its physical 1080×2640 display and 420dpi density;
the final API 35 Tablet normal-settings regression used font scale 1.0 and its default 320dpi density.

### Network-loss and restoration evidence

| Device | Sequence | Result |
| --- | --- | --- |
| API 35 phone AVD | Start live playback → disable the active emulator interface → wait through bounded Media3/fallback failure → restore the interface | The compact Player moved from `Playing live` through buffering/fallback to `No network · playback will resume automatically`, retained a Pause action, and returned to `Playing live` without another tap. |
| API 35 Pixel Tablet | Start live playback → disable both emulator interfaces → confirm no route → restore the simulated mobile network | The expanded Player followed the same terminal waiting and one-shot automatic recovery path. The post-recovery state retained live metadata, artwork, controls, station selection, and navigation. |

The implementation observes only the app's validated default-network capability. It does not poll, request a specific
transport, retry continuously, or treat ordinary online station errors as device-network loss. Pause, Stop, and station
changes cancel a pending recovery.

## Automated coverage

- Compact width selects the bottom navigation and excludes the rail.
- Medium/tablet width selects the rail.
- A live width change from compact to medium preserves the active Queue destination.
- Short-wide layout keeps playback controls scroll-reachable.
- 2× font scale with an enlarged display keeps compact and medium navigation, playback actions, station descriptions,
  and account identity/status content reachable.
- Folded portrait keeps playback controls and the complete station carousel visible without scrolling.
- The current 40-test connected suite passes on the API 35 Pixel Tablet at restored normal settings; the prior 39-test
  suite passes at maximum tested accessibility settings. The previous complete 36-test current-head suite passes on API 26, API 36, the API 35
  16 KB runtime, Pixel Fold open, and Pixel Tablet native landscape. The current More-menu preference and legal-disclosure paths also pass after
  live transitions to the Fold half-open and closed/outer-display states, and the disclosure path passes after rotating
  the Tablet to portrait.
- A generated 1,500-track Favorites list scrolls to track 1,500, returns to its controls, and supports stable sorting;
  unchanged availability and default position order reuse the exact list instance. The preserved full MorG list
  remained responsive during physical Razr inspection. Startup/memory diagnostics and boundaries are recorded in
  [the performance checkpoint](m23-performance-validation.md).
- Existing previews cover compact playing, phone landscape, expanded buffering, long metadata, missing artwork,
  fallback reconnection, offline automatic recovery, errors, light mode, and large type.
- Earlier full posture suites remain recorded above. The July 16 current-head pass adds the packaged open-source notice,
  backup/privacy hardening state, and all M23.2 UGC safety paths to the 36-test baseline. It also corrects a test-only
  viewport assumption so the Device preferences disclosure is scrolled into view before asserting visibility.
- The API 36 connected suite now passes 36/36; the installed candidate reports target API 36 and the earlier cold-launch
  two-Back flow reaches the expected exit dialog.
- Automated semantics verify descriptive click targets for all five navigation items even when enlarged text requires
  labels to yield their limited navigation-bar space. A spoken TalkBack traversal remains a physical-device Alpha check.
- Three recovery-policy tests cover offline one-shot recovery, online server-error suppression, and pause/cancel intent;
  a connected Compose test verifies the waiting explanation and available Pause action.

## Foldable and OEM boundary

The Razr validates the current app on physical foldable hardware through a measured 180° open → 110° tabletop → 0°
closed → 180° reopened cycle. Playback, the Media3 session, process, task, and full-screen Player survive the complete
cycle without a crash, ANR, restart, or clipped main-display controls. The Razr 2023's 194×368 external display is an
OEM-controlled presentation display that does not host normal app activities; continuing audio is therefore the app's
supported fully folded behavior on this model. The API 35 Pixel Fold validates the open inner display, half-open state,
and fully closed outer display, while the dedicated Pixel Tablet validates its native landscape and portrait windows.
The layout responds to the available window and naturally leaves the center gutter between expanded Player panes.
Play-delivered installation/update and the Play pre-launch report remain external M23.6 gates.

No application can guarantee behavior on every future OEM configuration. For Alpha, "all modern Android devices"
means the supported API range and adaptive window classes above are release requirements, regressions in those classes
are defects, and newly reported hinge/cutout/OEM cases are added to this matrix rather than handled with device-model
conditionals.
