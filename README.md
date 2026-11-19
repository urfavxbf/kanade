# Kanade

Kanade is a native Android music player for **offline local music** and **licensed/authorized online streams**, built with Kotlin, Media3 and custom Compose Foundation UI (**no Material 3**).

## What works

- Home opens on your music. Play a file from the Android picker, resume your queue, or browse recently played songs, favorites and new additions. Tap the mini-player for full-screen Now Playing.
- Offline library: songs, folders, albums, artists, sorting, favorites, playlists, local display metadata, and listening history.
- Search across songs, artists, albums and playlists. Home includes playable trending music from Audius while online.
- Native playback: background media session, editable queue, shuffle/repeat, seek, sleep timer, audio presets on supported devices, and full Now Playing UI.
- Audius: independent music search and authorized streams **playing inside Kanade** when available.
- **Search → Catalog** uses YouTube Data API v3 to discover music titles. Find recording searches local music and Audius. Tap an available recording to play it in the native player.

## YouTube setup — no paid service required

Follow **[YouTube Data API setup](docs/youtube-api.md)** to create your own Google Cloud project, enable YouTube Data API v3, and configure a restricted API key inside Kanade. There is **no API key bundled into the APK**, so YouTube discovery cannot search until you supply one. Google's default quota (currently documented as 100 `search.list` calls per day) is subject to change.

The feature requires active acceptance of **[Kanade Privacy Policy](docs/privacy-policy.md)** and **[Kanade Terms of Use](docs/terms.md)** before the first YouTube API call. Google [Privacy Policy](https://policies.google.com/privacy) and [YouTube Terms](https://www.youtube.com/t/terms) are linked in app.

## Product principles

- No separate external music player is required for local or authorized Audius songs.
- No YouTube ripping, extracting audio, hidden/background video player, unauthorized download or caching, or embedded credentials in source.
- All playable songs use the same native audio session.
- The playback queue has a single owner in the Android media service.
- Local music continues offline.
- Key setup, permissions and network errors are explicit.

## Build and tests

GitHub Actions runs unit tests, debug APK build and Android lint; retrieve the `kanade-debug` artifact from successful runs. Real-device playback and API credential restrictions need manual Android testing.
