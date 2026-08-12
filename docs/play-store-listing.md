# Google Play store listing draft

Prepared for the `24Seven.FM Player` application created in Play Console on July 15, 2026. Keep the listing accurate to the artifact being submitted and do not imply official endorsement.

## Default listing — English (United States)

**App name (17/30)**

24Seven.FM Player

**Short description (79/80)**

Native live radio for five 24Seven.FM stations, with queues, chat and requests.

**Full description**

Listen to all five 24Seven.FM radio stations in one fully native Android player:

- StreamingSoundtracks.com
- 1980s.FM
- Adagio.FM
- Death.FM
- Entranced.FM

Move between stations without losing the player, continue listening in the background, and control playback from Android media controls, notifications, headsets, and Bluetooth devices. The player shows available live metadata and artwork, clearly communicates buffering or reconnection states, and preserves the selected station across restarts.

Explore each station's upcoming queue and recently played tracks. Where the station supports them, signed-in members can use native chat, browse favorites, search the station library by title, album, artist, or genre, sort requestable tracks by station order or play state, and explicitly confirm song requests and optional request messages. Station accounts and capabilities remain station-specific.

Community content is hidden by default behind an adult age screen, Terms acceptance, and a separate reveal action. The app provides distinct report-content, report-user, and block-user controls, including station-scoped local block management.

Device-local startup preferences can resume the last station or use a chosen station. Responsive layouts support phones, tablets, foldables, rotation, and resizable windows.

Public listening does not require an account. The app contains no ads, analytics, tracking SDKs, or developer-operated data server.

24Seven.FM Player is an unofficial, non-commercial community client. It is not affiliated with or endorsed by 24Seven.FM or its individual stations. Feature availability depends on each station's public interfaces and account eligibility rules.

## Classification and contact proposal

- Type: App
- Price: Free
- Category: Music & Audio
- Suggested tags: Radio, Music & Audio, Music streaming (only if offered by Console)
- Suggested website: `https://github.com/James-Jennison/24Seven.FM-Player`
- Public support email: `24sevenplayer@jamesjennison.net`
- Phone number: omit unless the product owner wants it public
- External marketing: off during Alpha to avoid promotion outside the controlled testing audience

The owner confirmed the public support email on July 15, 2026. Console now records the email and Music & Audio category; the optional website and tags remain unsaved.

## Graphic inventory

- App icon: complete at `docs/play-store-assets/app-icon-512.png` (512×512 PNG, 282,959 bytes)
- Installed launcher icon: density-specific legacy PNGs plus adaptive foreground/background resources and an Android 13 monochrome radio-wave layer are complete; see `m23-launcher-polish.md`
- Feature graphic: complete at `docs/play-store-assets/feature-graphic-1024x500.png` (1024×500 PNG, 628,135 bytes)
- Phone screenshots: four privacy-reviewed 1080×1920 captures from the current August 11 Razr 2026 build are
  available under `docs/play-store-assets/screenshots/`: live Player, compact station selector, live Queue, and
  More/privacy tools. They exclude account identities, community content, and status-bar notifications.
- 7-inch tablet screenshot: `docs/play-store-assets/screenshots/tablet-7-player2.png` is a genuine 720dp
  medium-width adaptive-layout capture
- 10-inch tablet screenshot: `docs/play-store-assets/screenshots/tablet-10-player.png` is a genuine 960dp
  expanded-width adaptive-layout capture
- Expanded landscape screenshot: `docs/play-store-assets/screenshots/tablet-landscape-player.png` verifies a
  1707dp freely resized window and all five station cards
- No preview video is planned for Alpha

The feature graphic and tablet captures remain the M23-approved assets. The four phone captures were refreshed after
the August 11 compact inner-player and Razr cover-layout work so the listing represents the current Player, station
selector, Queue, and More surfaces.

Screenshots must show the actual app, avoid credentials or protected sessions, and avoid using public chat/requester content that could create unnecessary personal-data exposure in marketing assets.

## Accessibility text draft

- App icon: `Purple 24Seven.FM radio-wave app icon.`
- Feature graphic: `Five colored radio signals crossing a dark 24Seven.FM player backdrop.`
- Player screenshot: `Now Playing screen with live artwork, station controls, and five-station selector.`
- Queue screenshot: `Upcoming and recently played station tracks in the native Queue screen.`
- More screenshot: `Station capabilities, account access, device preferences, and community safety controls.`
- Tablet screenshot: `Expanded Now Playing layout with navigation rail and supporting station content.`

## Source requirements checked

- [Store listing best practices](https://support.google.com/googleplay/android-developer/answer/13393723)
- [Preview asset specifications](https://support.google.com/googleplay/android-developer/answer/9866151)
- [Metadata policy](https://support.google.com/googleplay/android-developer/answer/9898842)
- [Play Console sign-in details requirements](https://support.google.com/googleplay/android-developer/answer/15748846)
