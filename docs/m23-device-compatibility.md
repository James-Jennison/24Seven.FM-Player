# M23 modern Android device compatibility matrix

Validated July 15, 2026 for the `0.1.0-alpha01` candidate. The product requirement is adaptive support across
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
| Motorola Razr 2023, Android 16 / API 36 | 411×731dp compact portrait | `phone-player-live-playing.png`, `phone-queue-live.png`, `phone-stations.png` | Bottom navigation, artwork, metadata, playback, mini-player, live Queue, and horizontally scrollable station cards remain reachable. |
| API 35 Pixel 7 emulator | 411×731dp compact portrait | `phone-more.png`; connected UI suite | Compact navigation and supporting destinations render at an exact 1080×1920 Play screenshot size. |
| API 26 phone AVD | Minimum supported runtime | 27/27 connected tests | Minimum-SDK installation, launch, compact navigation, playback UI, and established feature suite pass without changing SDK packages or clearing build caches. |
| API 35 16 KB phone AVD | 411×731dp compact portrait; 16,384-byte runtime page size | 27/27 connected tests; `PAGE_SIZE=16384` | The app installs, launches, and passes the established feature suite on a genuine 16 KB runtime. |
| API 35 Pixel 7 emulator resized as a 7-inch class window | 720×1280dp portrait | `tablet-7-player2.png` | Navigation rail replaces bottom navigation; Player controls and station cards remain visible. |
| API 35 Pixel 7 emulator resized as a 10-inch class window | 960×1707dp portrait | `tablet-10-player.png` | Expanded two-pane Player and navigation rail render without stretching the compact layout. |
| API 35 Pixel 7 emulator, expanded landscape/freeform | 1707×960dp landscape | `tablet-landscape-player.png` | Two-pane Player, navigation rail, controls, and all five station cards are simultaneously reachable. |
| API 35 Pixel Fold emulator, open inner display | 2208×1840 physical display | Focused adaptive connected tests and live emulator inspection | Expanded layout renders correctly on the genuine foldable inner display. |
| API 35 Pixel Fold emulator, half-open inner display | Device-state 1; 2208×1840 physical display | 33/33 connected tests and live inspection | The full current suite passes across the hinge-state transition. Intermittent gfxstream black tiles cleared after an activity rerender and produced no application crash or semantic-test failure. |
| API 35 Pixel Fold emulator, closed outer display | Device-state 0; 1080×2092 physical display, inner display off | 33/33 connected tests and cold-launch screenshot inspection | Compact Player controls and the horizontally reachable station carousel are visible; the complete current suite passes on the genuine outer display. |
| API 35 Pixel Tablet emulator, native landscape | 2560×1600 physical display; 1280×800dp | 33/33 connected tests and cold-launch screenshot inspection | Expanded two-pane Player, navigation rail, artwork, controls, and station carousel render cleanly. |
| API 35 Pixel Tablet emulator, native portrait | 1600×2560 physical display; 800×1280dp | 34/34 connected tests and rotation screenshot inspection | Medium-width rail layout retains visible artwork, controls, and a horizontally reachable station carousel after rotation; the final suite includes duplicate-report retry suppression. |
| API 36 phone AVD targeting API 36 | 411×914dp compact portrait | 27/27 connected tests plus cold-launch/two-Back inspection | Adaptive navigation, Favorites lazy scrolling, MediaSession service, edge-to-edge Player, and the exit dialog pass under Android 16 target behavior. |

All display overrides were temporary. The Razr returned to its physical 1080×2640 display and 420dpi density; the
emulator returned to 1080×2400 and 420dpi after capture.

## Automated coverage

- Compact width selects the bottom navigation and excludes the rail.
- Medium/tablet width selects the rail.
- A live width change from compact to medium preserves the active Queue destination.
- Short-wide layout keeps playback controls scroll-reachable.
- 1.5× font scale keeps the compact Player's primary action reachable.
- Folded portrait keeps playback controls and the complete station carousel visible without scrolling.
- The 33-test connected suite passes on the Pixel Fold half-open inner display, closed outer display, and Pixel Tablet
  native landscape. The final 34-test suite, with duplicate-report retry suppression added, passes on Pixel Tablet portrait.
- A generated 1,500-track Favorites list remains browsable and supports stable play-state sorting; the preserved full
  MorG list remained responsive during physical Razr inspection.
- Existing previews cover compact playing, phone landscape, expanded buffering, long metadata, missing artwork,
  reconnecting/error states, light mode, and large type.
- The earlier API 35 phone suite passed 24/24 tests. The current M23.2-inclusive suite passes 33/33 on each fold/tablet
  posture listed above, including compact/medium navigation selection, live width-change state preservation,
  short-wide scrolling, 1.5× font-scale reachability, community access gating, and native report/block flows.
- The API 36 connected suite passes 27/27 tests after correcting one viewport-dependent lazy-test assumption; the
  installed candidate reports target API 36 and the cold-launch two-Back flow reaches the expected exit dialog.

## Foldable and OEM boundary

The Razr validates the app on current foldable hardware in its normal main-display state. The API 35 Pixel Fold now
validates the open inner display, half-open state, and fully closed outer display, while the dedicated Pixel Tablet
validates its native landscape and portrait windows. The layout responds to the available window and naturally leaves
the center gutter between expanded Player panes. A mechanically changed physical Razr half-open/tabletop posture was
not available during this pass, so that OEM-specific hinge inspection remains an Alpha test item and is not represented
as independently certified here. Play-delivered installation/update and the Play pre-launch report also remain external
M23.6 gates.

No application can guarantee behavior on every future OEM configuration. For Alpha, "all modern Android devices"
means the supported API range and adaptive window classes above are release requirements, regressions in those classes
are defects, and newly reported hinge/cutout/OEM cases are added to this matrix rather than handled with device-model
conditionals.
