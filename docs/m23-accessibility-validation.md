# M34 accessibility and large-text validation

Validated July 16, 2026 for the `0.1.0-alpha01` candidate.

## Outcome

The native Compose shell remains usable with Android font scale 2.0 and an enlarged display across compact phone,
medium Fold, closed Fold, and expanded Tablet windows. The built-in Google TalkBack service also traverses the current
candidate on a phone, all three tested Fold postures, and Tablet landscape/portrait with descriptive accessibility
focus and no remaining unlabeled actionable nodes in the inspected surfaces. This closes the local large-text/reflow
and service-level TalkBack checkpoints within M34; human audible-speech review, Play-delivered testing, and the Play
pre-launch report remain external gates.

## Defects corrected

- Bottom-navigation labels no longer wrap into clipped fragments when five destinations share a narrow compact window.
  Every phone and rail item retains its full destination as an explicit clickable accessibility description.
- Station cards use a minimum height instead of a fixed height, allowing the two-line station description to grow with
  the user's text size.
- Account headers stack station identity above authentication status at enlarged text instead of forcing the station
  name into a nearly one-character-wide column.
- The compact Player exposes a stable scroll target so short-wide and Fold tests can verify that controls and station
  details remain reachable.
- The Community Terms agreement and station Chat-mention opt-in checkboxes now expose explicit TalkBack descriptions.
  Both previously appeared as unlabeled actionable nodes to the real accessibility service.

## Device evidence

| Surface | Accessibility profile | Result |
| --- | --- | --- |
| API 35 phone | 343×762dp effective compact window, font scale 2.0, 504dpi | Player, More, Favorites, Queue, and Chat inspection passed; focused compact navigation/station-card and account-header tests passed. |
| API 35 Pixel Fold, open | 701×584dp effective medium window, font scale 2.0, 504dpi | Navigation rail, playback action, scrolling Player, and station descriptions remained reachable; focused medium test passed. |
| API 35 Pixel Fold, closed | 1080×2092 physical outer display, font scale 2.0 | Compact Player controls, bottom navigation, and growing station cards remained reachable. |
| API 35 Pixel Tablet | 1067×667dp effective expanded window, font scale 2.0, 384dpi | Full connected suite passed 39/39; live Player and More inspection passed. |
| API 35 Pixel Tablet, restored defaults | 1280×800dp, font scale 1.0, 320dpi | Full connected regression suite passed 39/39 after all temporary overrides were removed. |

### Google TalkBack service traversal

| Surface | Posture/orientation | Result |
| --- | --- | --- |
| API 35 phone | Compact portrait | TalkBack focus reached all five destinations, playback controls, station cards, Sleep Timer, Audio output, community gates, More disclosures, and diagnostics Copy/Share actions. The corrected Terms checkbox announces `I Agree`. |
| API 35 Pixel Fold | Open inner, half-open inner, and closed outer displays | TalkBack focus retained descriptive navigation, playback, and station-card actions through each genuine emulator device-state transition. |
| API 35 Pixel Tablet | Native landscape and portrait | TalkBack focus reached navigation, playback controls, station cards, Sleep Timer, and Audio output in both adaptive layouts. |

The service was enabled from the API 35 system image already installed in the environment; no SDK or accessibility
package was installed. UIAutomator reported no `NAF=true` actionable node after the checkbox fix on the inspected
surfaces. This verifies service discovery, focus order, labels, roles, and actions. It does not substitute for a human
listening to speech pronunciation, pacing, and clarity on physical hardware.

## M33 confirmation-dialog follow-up

The M33 request confirmation now identifies the selected station and signed-in account before the one-shot request
submission. Its content body is vertically scrollable so the identity, long track metadata, optional message field,
and both actions remain reachable at maximum text scale rather than being clipped by a compact window.

| Device/runtime | Focused maximum-text evidence | Result |
| --- | --- | --- |
| Motorola Razr 2023, Android 16 / API 36 | Physical-device connected test at font scale 2.0: confirmation content, station identity, optional message, Cancel, and Send | 2/2 focused confirmation tests passed. |
| API 35 Pixel Tablet emulator | Expanded-window connected test at font scale 2.0 with a long track title and artist | 1/1 focused confirmation test passed. |

These fixture-only checks do not submit a request or expose an account. They close the current M33 dialog's automated
large-text reachability follow-up; the human audible/intelligibility and alternative-input M34 gates remain open.

## Voice Access physical follow-up

On July 18, 2026, the Google Voice Access service was installed from its Play listing and explicitly enabled on the
physical Motorola Razr 2023 running Android 16. Its microphone permission was granted; its optional phone-call control
permission was deliberately declined because it is not required to operate the Player.

The user confirmed that Voice Access activated the primary Player commands after the control semantics correction in
`03a310b`: Play/Pause, Previous station, and Next station. A direct UIAutomator inspection of that same Razr build
records each as a named, clickable Android button rather than a named non-clickable child. This is human-device
evidence for the primary playback controls, not a substitute for the remaining full alternative-input traversal of
station selection, destinations, dialogs, settings, and community surfaces.

## Verification

- `:app:connectedDebugAndroidTest`: 39/39 at maximum tested Tablet accessibility settings.
- `:app:connectedDebugAndroidTest`: 39/39 after restoring normal Tablet settings; the latest current-head Tablet
  regression passes 48/48.
- Focused compact phone, account-header, and medium Fold accessibility tests passed.
- Focused Terms and `MorG` Chat-mention setting tests passed 2/2 after the TalkBack label correction.
- `:app:testDebugUnitTest`: 146/146 passed, including exact/case-insensitive `MorG` mention matching and rejection of
  the longer `MorgHubby` name.
- `:app:lintDebug`: passed.
- Privacy-safe visual evidence is preserved in `docs/screenshots/m23-large-text-player.png` and
  `docs/screenshots/m23-large-text-account.png`.

Automated semantics confirm that all five navigation destinations remain descriptive click targets. Real TalkBack
service traversal is complete on the local API 35 device matrix; a human audible/intelligibility pass on physical
hardware remains an Alpha tester check and is not represented as completed by this checkpoint.
