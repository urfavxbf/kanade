# Kanade

Kanade is a native Android music player for **offline local music** and **licensed/authorized online streams**, built with Kotlin, Media3 and custom Compose Foundation UI (**no Material 3**).

## What works

- Offline library: songs, folders, albums, artists, sorting, favorites, playlists, local display metadata, and listening history.
- Browse album covers in Library → Albums. Choose **Find music video** from a track’s actions or Now Playing to search YouTube by title and artist. Selecting a video switches playback.
- Native playback: background media session, editable queue, shuffle/repeat, seek, sleep timer, audio presets on supported devices, and full Now Playing UI.
- Audius: independent music search and authorized streams **playing inside Kanade** when available.
- **YouTube Listening room** uses the YouTube Data API v3 for Philippine music search and the official visible YouTube embed for playback. Open a watch link without a search key, play a search result, or build a session queue from videos and links. Reorder it and press Play queue for continuous foreground playback with Previous, Next and Retry inside Kanade.

## YouTube setup — no paid service required

Follow **[YouTube Data API setup](docs/youtube-api.md)** to create your own Google Cloud project, enable YouTube Data API v3, and configure a restricted API key inside Kanade. There is **no API key bundled into the APK**, so YouTube discovery cannot search until you supply one. Google's default quota (currently documented as 100 `search.list` calls per day) is subject to change.

The feature requires active acceptance of **[Kanade Privacy Policy](docs/privacy-policy.md)** and **[Kanade Terms of Use](docs/terms.md)** before the first YouTube API call. Google [Privacy Policy](https://policies.google.com/privacy) and [YouTube Terms](https://www.youtube.com/t/terms) are linked in app.

## Product principles

- No separate external music player is required for local or authorized Audius songs.
- No YouTube ripping, extracting audio, hidden/background video player, unauthorized download or caching, or embedded credentials in source.
- YouTube mixes play in the foreground. Local and Audius audio use the native media session.
- The playback queue has a single owner in the Android media service.
- Local music continues offline.
- Key setup, permissions and network errors are explicit.

## Build and tests

GitHub Actions runs unit tests, debug APK build and Android lint; retrieve the `kanade-debug` artifact from successful runs. Real-device playback and API credential restrictions need manual Android testing.
