# Kanade Music Player

> A modern Android music player focused on local music, clean playback, metadata tools, and a dynamic player experience.

**Kanade** is a Java-based Android music player built around the user's local music library. It scans audio stored on the device, provides queue-based playback, favorites and playlists, album/artist browsing, metadata identification and editing, album-art caching, and a full-screen player experience.

<p align="center">
  <strong>Local Music • Playback • Metadata • Album Art • Playlists</strong>
</p>

## Features

- 🎵 **Local music library** — Scans audio files available through Android's MediaStore.
- 🔎 **Music discovery** — Browse songs, artists, albums/dashboard content, and playlists.
- ▶️ **Playback** — Play, pause, previous, next, queue management, shuffle, repeat, and playback state handling.
- 🎚️ **Foreground playback service** — Music playback is handled by `MusicPlayerService` with Android foreground-service support for media playback.
- 🎛️ **Full Player** — Dedicated Now Playing screen with animated transition from the mini player.
- 📱 **Mini Player** — Persistent playback controls while navigating the main sections.
- ❤️ **Favorites** — Save favorite tracks locally.
- 📚 **Playlists** — Create and manage local playlists.
- 🖼️ **Album artwork** — Uses embedded artwork and application-cached artwork with a placeholder fallback.
- 🎨 **Dynamic album colors** — Album artwork can drive the player's accent and background colors.
- ✏️ **Metadata editing** — Edit track metadata through the app while keeping local override data separate from the original audio file.
- 🔍 **Music identification** — Supports fingerprint-based identification using Chromaprint/AcoustID and metadata lookup through MusicBrainz.
- 🧹 **Metadata normalization** — Normalizes metadata and ranks identification candidates using available track information.
- 🛠️ **Crash/debug screen** — Captures uncaught application errors and presents debugging information through `DebugActivity`.
- ▶️ **YouTube section** — A YouTube navigation destination is present; the current implementation is a placeholder and is not yet a complete YouTube client.

## Architecture

Kanade uses a single main activity with Navigation Component destinations for the major sections of the application. Playback is separated into a controller and foreground service, while library and metadata functionality are handled by focused managers and clients.

```text
app/src/main/java/com/urfavxbf/kanade/
├── MainActivity.java
├── AudioFile.java
├── AudioListAdapter.java
├── MusicScanner.java
├── MusicRepository.java
├── MusicPlayerController.java
├── MusicPlayerService.java
├── PlaylistManager.java
├── PlaylistActivity.java
├── PlayerActivity.java
├── AlbumArtManager.java
├── AlbumColorManager.java
├── MetadataOverrideManager.java
├── MetadataNormalizer.java
├── MetadataResolver.java
├── MusicIdentifier.java
├── MusicMetadataCandidate.java
├── MusicBrainzClient.java
├── AcoustIdClient.java
├── ChromaprintFingerprintGenerator.java
├── AcoustIdTestHelper.java
├── ChromaprintTestHelper.java
├── MusicBrainzTestHelper.java
├── MusicIdentifierTestHelper.java
├── EditMetadataDialog.java
├── DebugActivity.java
└── ui/
    ├── songs/
    ├── artist/
    ├── dashboard/
    ├── playlist/
    ├── player/
    ├── youtube/
    └── notifications/
```

### Navigation

The main navigation currently contains:

- **Songs** — local music library
- **Artists** — artist browsing
- **Dashboard** — library/dashboard content
- **Playlists** — playlist browsing and management
- **YouTube** — currently a placeholder destination
- **Now Playing** — full player

The global bottom navigation is controlled by `MainActivity`, while each section is implemented as a Fragment destination.

## Playback

Playback is built around Android media APIs rather than an external streaming platform.

`MusicPlayerController` provides the application-facing playback controls, while `MusicPlayerService` owns the long-running media playback and foreground-service behavior.

The application also supports a mini-player/full-player flow. Opening the full player uses an animated transition from the mini player, and the global bottom navigation is hidden while the Now Playing destination is active.

## Music Identification & Metadata

Kanade includes a multi-stage music identification system.

### Audio fingerprinting

The project includes Chromaprint fingerprint generation through the `fpcalc` dependency and uses AcoustID for fingerprint-based identification.

### Metadata lookup

MusicBrainz is used for metadata lookup and candidate information. Identification can use track information such as title, artist, and duration to improve matching.

### Metadata overrides

Edited metadata is managed separately through `MetadataOverrideManager`. This allows Kanade to present corrected metadata without treating the original audio file as the application's metadata database.

## Album Artwork

Kanade uses an application-managed album-art cache.

The artwork flow is designed around these sources:

1. Cached artwork stored by Kanade
2. Embedded artwork from the local audio file
3. Artwork obtained during metadata identification
4. Placeholder artwork when no artwork is available

Downloaded artwork is stored inside the application's files area. `AlbumArtManager` validates downloaded image data, limits download size, caches it using a SHA-256-derived key, and can clear cached artwork without modifying the original music file.

## Dynamic Album Colors

`AlbumColorManager` provides application-wide album-based theme colors. `MainActivity` listens for color changes and animates the transition between accent and background colors.

This allows the player UI to adapt visually to the currently selected album artwork instead of using one fixed application color scheme.

## Permissions

The application declares permissions required for local music access, foreground playback, notifications, and optional online/identification features:

- `READ_MEDIA_AUDIO` on modern Android versions
- `READ_EXTERNAL_STORAGE` on Android versions where it is applicable
- `FOREGROUND_SERVICE`
- `FOREGROUND_SERVICE_MEDIA_PLAYBACK`
- `POST_NOTIFICATIONS`
- `INTERNET`
- `RECORD_AUDIO` for audio-related functionality used by the project

Basic local music playback does not require a cloud music account.

## Privacy & Network Usage

Kanade is designed primarily around local music ownership, but it is **not completely offline**.

Local library scanning, local playback, favorites, playlists, and locally stored metadata overrides are designed to work with data on the device. Network access is used for features that explicitly require online services, including music identification and related metadata/artwork retrieval.

The project does not use Firebase, does not require a user account for local playback, and does not include an advertising SDK in the application architecture.

> **Important:** The presence of the YouTube destination does not currently mean that Kanade provides full YouTube streaming functionality. The current YouTube screen is a placeholder.

## Tech Stack

- **Language:** Java
- **Platform:** Android
- **Namespace / Application ID:** `com.urfavxbf.kanade`
- **UI:** XML + AndroidX + Material Components
- **Navigation:** AndroidX Navigation Component
- **View binding:** Android View Binding
- **Build system:** Gradle + Android Gradle Plugin
- **Compile SDK:** 36
- **Target SDK:** 36
- **Minimum SDK:** 23
- **Java compatibility:** Java 17
- **Playback:** Android media APIs / foreground media service
- **Local data:** SharedPreferences and application-local files where applicable
- **Music metadata:** MusicBrainz
- **Music identification:** AcoustID + Chromaprint
- **Album artwork:** Embedded media artwork + local cache

## Build

The project uses the Android Gradle Plugin and Kotlin DSL.

```text
Kanade/
├── app/
│   ├── build.gradle.kts
│   ├── proguard-rules.pro
│   └── src/main/
├── settings.gradle.kts
└── README.md
```

The current application module uses Android Gradle Plugin `8.11.0`, compile/target SDK 36, minimum SDK 23, Java 17 compatibility, and Android View Binding.

A release signing configuration can be supplied through `release.properties`. If valid signing properties are present, the release build is configured to use them and enables R8/minification.

## Dependencies

The application currently uses AndroidX, Material Components, Navigation, lifecycle/support libraries, and the Chromaprint `fpcalc` dependency.

Notable dependencies include:

- AndroidX AppCompat
- AndroidX Media
- AndroidX Navigation
- AndroidX ConstraintLayout
- AndroidX Lifecycle
- AndroidX Palette
- AndroidX SwipeRefreshLayout
- Material Components
- Kotlin Coroutines runtime
- Chromaprint / `fpcalc`

## Project Status

Kanade is an **active work in progress**.

The current codebase has moved beyond the original simple local-player structure and now includes navigation-based screens, a dedicated full player, dynamic album colors, album-art caching, metadata editing, fingerprint-based identification, AcoustID/MusicBrainz integration, and debugging helpers.

Some areas are still incomplete, most notably the YouTube destination and parts of the broader UI/feature set.

## Roadmap

Planned improvements include:

- [ ] Complete the YouTube experience
- [ ] Improve metadata matching accuracy
- [ ] Improve AcoustID/Chromaprint identification reliability
- [ ] Improve album-art matching and caching
- [ ] Expand playlist management
- [ ] Improve artist and album browsing
- [ ] Continue refining the full-player UI and transitions
- [ ] Improve playback customization
- [ ] Optimize scanning and identification for large music libraries
- [ ] Expand automated testing around metadata and identification components

## Contributing

Suggestions, bug reports, and improvements are welcome.

When reporting a bug, include:

1. Android version
2. Device/emulator information when relevant
3. Steps to reproduce the problem
4. Relevant logcat output or the information shown by `DebugActivity`
5. Whether the problem affects local playback, metadata identification, artwork, navigation, or another subsystem

For pull requests, keep changes focused and explain the behavior being changed.

## Disclaimer

Kanade is an open-source/personal Android music player project. External metadata, artwork, AcoustID, MusicBrainz, and other third-party services are subject to their respective terms, availability, and policies.

Kanade does not provide ownership or licensing rights to music or metadata obtained from external sources.

## License

License information will be added as the project is finalized.

---

<p align="center">
  Made for people who want a capable player for their own music library.
</p>
