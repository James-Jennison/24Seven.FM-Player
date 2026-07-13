# M4 metadata research

Research performed on July 13, 2026 against the ten stream URLs already verified and committed from station-provided PLS files. No endpoint was guessed or added.

Each relay was requested with `Icy-MetaData: 1`. The servers use a legacy ICY response, so headers and the first metadata block were decoded from a short-lived byte sample. Temporary samples were deleted immediately after extraction.

| Station | Relay | Content type | Advertised bitrate | Metadata interval | Non-empty title |
| --- | --- | --- | ---: | ---: | --- |
| StreamingSoundtracks.com | Primary | `audio/aacp` | 128 kbps | 32,768 bytes | Yes |
| StreamingSoundtracks.com | Source | `audio/aacp` | 128 kbps | 8,192 bytes | Yes |
| 1980s.FM | Primary | `audio/aacp` | 128 kbps | 32,768 bytes | Yes |
| 1980s.FM | Source | `audio/aacp` | 128 kbps | 8,192 bytes | Yes |
| Adagio.FM | Primary | `audio/aacp` | 128 kbps | 32,768 bytes | Yes |
| Adagio.FM | Source | `audio/aacp` | 128 kbps | 8,192 bytes | Yes |
| Death.FM | Primary | `audio/aacp` | 128 kbps | 32,768 bytes | Yes |
| Death.FM | Source | `audio/aacp` | 128 kbps | 8,192 bytes | Yes |
| Entranced.FM | Primary | `audio/aacp` | 128 kbps | 32,768 bytes | Yes |
| Entranced.FM | Source | `audio/aacp` | 128 kbps | 8,192 bytes | Yes |

The sampled primary and source title matched for every station. This is a point-in-time protocol check, not a guarantee that relays will always be synchronized.

## Field constraints

The ICY `StreamTitle` is a single composite string. Samples resembled performer, album, title, and duration separated by punctuation, but ICY supplies no trustworthy field boundaries. The application must display the raw title and must not split it heuristically into artist, album, composer, or duration.

Media3 1.10.1 exposes the metadata through `Player.Listener.onMetadata` and `IcyInfo.title`. `IcyInfo.url` is not used because no semantics or permitted artwork behavior have been verified for it.

The response content type verifies AAC-family audio and the catalog records `StreamFormat.Aac`. The 128 kbps value is server-advertised ICY evidence. Artwork and separately structured track, album, artist, and composer fields remain unverified.

## Initial implementation validation

The service-owned player publishes non-empty `IcyInfo.title` values into immutable, station-scoped domain state. The Compose Now Playing screen displays the raw title and verified `AAC • 128 kbps` quality. The same title is explicitly merged into MediaSession metadata while the station identity remains the artist/album context.

The debug and release unit tests, Android lint, debug APK, and instrumentation APK passed. The API 35 MediaSession service stop/reconnect test also passed after the metadata integration was added.

On the Motorola Razr 2023, all five stations displayed a fresh title after selection, replaced the previous station's title, remained in `PLAYING`, and completed an isolated rerun with no playback or fatal application errors. A separate check confirmed that the Compose title and Android MediaSession title matched. Playback was stopped after each validation run.

A transient malformed AAC packet occurred during an earlier rapid all-station pass; the existing fallback policy recovered to `PLAYING`, and the error did not reproduce in the isolated rerun. This is recorded as transient stream behavior rather than hidden.
