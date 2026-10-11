# Google Play store listing draft

## 1.0.3 listing refresh (drafted October 11, 2026; to be submitted with the 1.0.3 release so the 1.0.2 review clock is not restarted)

The listing live on October 11, 2026 dates from August 25, 2026 and describes the pre-1.0.2 app. In the 28 days to
October 5 it had 42 visitors, 23 install clicks (55% click-through) and 48.6% install conversion: the funnel converts,
the reach is the problem. The refresh puts the station names and genre words into the searchable text and replaces
the August screenshots with 1.0.2 captures.

**App name (unchanged, 30/30):** 24Seven.FM: Internet Radio App. The description refers to the app by this store title, not by the project name 24Seven.FM Player.

**Short description options (80 max; owner to pick one):**

- Option A (77/80): 1980s.FM, StreamingSoundtracks.com, Adagio, Death.FM, Entranced.FM live radio
- Option B (77/80): Live internet radio: 80s hits, film soundtracks, classical, metal and trance.
- Option C (76/80): 24Seven.FM stations in one native player: 80s, soundtracks, classical, metal

**Full description (2915/4000):**

```text
24Seven.FM: Internet Radio App is the native Android player for the five 24Seven.FM internet radio stations: 1980s.FM, Adagio.FM, Death.FM, Entranced.FM and StreamingSoundtracks.com. Pick a station, press Play, and keep listening while you use other apps, lock the screen, or drive.

FIVE STATIONS, ONE PLAYER
• 1980s.FM: 80s hits, synth-pop, new wave and the decade's big anthems
• StreamingSoundtracks.com: film, TV and video game soundtracks
• Adagio.FM: classical music
• Death.FM: extreme metal
• Entranced.FM: trance and electronic dance music
Swipe the artwork to change station. The Player takes on each station's colour and logo, so you always know what you are listening to.

LIVE RADIO THAT FITS ANDROID
• Background playback with media controls on the lock screen, in the notification, and on headsets and Bluetooth
• A home-screen widget with the current track and Play/Stop
• A Quick Settings tile to start and stop the station from the shade
• Long-press the app icon for a shortcut to each station
• Android Auto: browse the five stations in the car, see title, artist and album, and open the Queue of upcoming tracks

KNOW WHAT IS PLAYING
• Live track, artist, album and cover art as the station publishes them
• Who requested the track, how many are listening, and how far the track has played
• The upcoming Queue and the recently played History for each station
• Recently added albums, with covers that open the album and its tracks

JOIN THE STATION COMMUNITY (OPTIONAL)
Sign in with your existing station account to use the station's own features inside the Player:
• Station chat, shown like an IRC client with the station's role colours
• Song requests with the station's confirmation step, plus your request history and request clock
• Your favourites: add the current track, rate albums, and manage your ranked favourites list
• Album reviews from the station's album pages
• Private messages with an unread badge
• Member profiles, the members list, who is online now, and the station's events and birthdays calendar
• Edit your station profile from the Player
Station accounts are separate: signing in to one station does not sign you in to the others. Everything works for listening without an account.

BUILT FOR PHONES, TABLETS AND FOLDABLES
Layouts adapt from compact phones to large tablets and desktop windows. A persistent mini player keeps the controls close on every screen.

PRIVATE BY DESIGN
No ads, no analytics, no tracking SDKs, no developer-operated server. The app talks only to the station you choose. Community content is shown only after an age screen and your explicit choice, and there are report and block controls.

24Seven.FM: Internet Radio App is an unofficial, non-commercial community client. It is not affiliated with or endorsed by 24Seven.FM or its individual stations. Feature availability depends on each station's public pages and your station account.
```

**Screenshots (captured October 11, 2026 from the 1.0.2 release build, signed out, dark theme, on the API 35 phone
and tablet emulators):** `docs/play-store-assets/screenshots-1.0.3/`. The owner chose the dark theme for the store set
because the light theme reads as loud in listing thumbnails. Phone captures are 1080×2160 (the 2:1 limit Play
enforces; the emulator was set to 1080×2160 at 380 dpi so the full hero layout renders) and tablet captures are
1600×2560 / 2560×1600. All files are 24-bit PNG without alpha.

| File | Shows |
| --- | --- |
| `phone-01-player-sst.png` | Player on StreamingSoundtracks.com: live artwork, listener count, track progress, station row |
| `phone-02-player-1980s.png` | Player after switching to 1980s.FM; station colour palette changes |
| `phone-03-queue.png` | Queue: Up next with artwork and durations, Recently played below |
| `phone-04-more.png` | More tab: signed-out account card and grouped station sections |
| `phone-05-quick-settings-tile.png` | Playback tile in Quick Settings next to the system media card |
| `tablet-01-player-portrait.png` | Tablet portrait Player with navigation rail |
| `tablet-02-queue-portrait.png` | Tablet portrait Queue with the mini player |
| `tablet-03-player-landscape.png` | Tablet landscape Player: hero card, controls, Explore the network |
| `tablet-04-queue-landscape.png` | Tablet landscape Queue |

Not included on purpose: Chat (a fresh install sits behind the community age gate), Favorites (signed-out prompt only),
Recently added (its "added by" line names a staff account), and the notification shade (emulator system notices).
No account identities, request messages or private content appear.

**Not changed by this refresh:** category, contact details, feature graphic, icon, content rating, Data safety.

**Submission note:** enter the chosen short description, the full description and the new screenshots in Play Console
only together with the 1.0.3 release under managed publishing, so the store page and the installed app change on the
same day. Listing text and graphics also go through Play review.


Prepared for the `24Seven.FM Player` application created in Play Console on July 15, 2026. Keep the listing accurate to the artifact being submitted and do not imply official endorsement.

## Default listing — English (United States)

**App name (30/30)**

24Seven.FM: Internet Radio App

(The store title was changed from `24Seven.FM Player` to `24Seven.FM: Internet Radio App` before the 1.0.1 listing; the Play Console app record still carries the original name.)

**Short description (79/80)**

Native live radio for five 24Seven.FM stations, with queues, chat and requests.

**Full description**

Listen to all five 24Seven.FM radio stations in one fully native Android player:

- 1980s.FM
- Adagio.FM
- Death.FM
- Entranced.FM
- StreamingSoundtracks.com

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
  available under `docs/play-store-assets/screenshots/`: live Player, Favorite Tracks, live Queue, and More/privacy
  tools. They exclude account identities, community content, and status-bar notifications.
- 7-inch tablet screenshot: `docs/play-store-assets/screenshots/tablet-7-player2.png` is a genuine 720dp
  medium-width adaptive-layout capture
- 10-inch tablet screenshot: `docs/play-store-assets/screenshots/tablet-10-player.png` is a genuine 960dp
  expanded-width adaptive-layout capture
- Expanded landscape screenshot: `docs/play-store-assets/screenshots/tablet-landscape-player.png` verifies a
  1707dp freely resized window and all five station cards
- No preview video is planned for Alpha

The feature graphic and tablet captures remain the M23-approved assets. The four phone captures were refreshed after
the August 11 compact inner-player and Razr cover-layout work so the listing represents the current Player, Favorites,
Queue, and More surfaces.

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
