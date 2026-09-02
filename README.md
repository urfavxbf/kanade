# Kanade Music Player

> A modern, privacy-friendly, offline-first music player for Android.

**Kanade** is a local music player built around a simple idea: your music library should belong to you. It scans the music stored on your Android device, keeps your library data local, and provides a clean player experience without requiring an account or cloud music service.

<p align="center">
  <strong>Offline-first • Local library • Metadata tools • Album artwork</strong>
</p>

## Features

- 🎵 **Local music library** — Scan and play audio files stored on your device.
- 🔎 **Music search** — Quickly find tracks in your library.
- ▶️ **Playback controls** — Play, pause, previous, next, queue, shuffle, and repeat.
- ❤️ **Favorites** — Save favorite tracks locally.
- 📚 **Playlists** — Organize your music into playlists.
- 🖼️ **Album artwork** — Supports embedded artwork and locally cached artwork.
- ✏️ **Metadata editing** — Edit track information without modifying the original audio file.
- 🌐 **Metadata identification** — Uses online music metadata services when identification is requested.
- 💾 **Local persistence** — App-managed metadata and favorites are stored locally.
- 🔒 **Privacy-focused** — No Firebase, no advertising SDK, and no requirement for a user account.

## Album Artwork

Kanade uses a layered artwork strategy:

1. Locally cached artwork
2. Embedded artwork inside the audio file
3. Online artwork retrieved during metadata identification
4. Local placeholder when artwork is unavailable

Downloaded artwork is cached inside the app and does not modify the original music file.

## Metadata

Kanade can identify music and retrieve metadata such as:

- Title
- Artist
- Album
- Release information
- Album artwork

Search results are matched using the track title and artist when both are available, with additional information such as duration used to improve candidate ranking.

Metadata edits are stored as local overrides rather than rewriting the original audio file.

## Architecture

The project is organized around a small set of focused components:

```text
app/
└── src/main/java/com/urfavxbf/kanade/
    ├── AudioFile.java
    ├── AudioListAdapter.java
    ├── AlbumArtManager.java
    ├── MusicScanner.java
    ├── MusicRepository.java
    ├── MusicIdentifier.java
    ├── MusicMetadataCandidate.java
    ├── MusicBrainzClient.java
    ├── MetadataOverrideManager.java
    ├── MetadataNormalizer.java
    ├── MusicPlayerController.java
    ├── MusicPlayerService.java
    ├── PlaylistManager.java
    └── HistoryManager.java
```

The project follows an offline-first approach. Online services are used only for features that require external metadata, such as music identification and artwork lookup.

## Privacy

Kanade is designed with local ownership in mind.

- No Firebase
- No advertising SDK
- No mandatory account
- No music streaming service required for local playback
- User metadata overrides are stored locally
- Album artwork downloaded by Kanade is cached locally

Network access is used for optional metadata and artwork retrieval, not for basic local music playback.

## Tech Stack

- **Language:** Java
- **Platform:** Android
- **UI:** XML + Material Components
- **Build:** Gradle / Android Gradle Plugin
- **Local storage:** SharedPreferences / JSON
- **Playback:** Android media APIs
- **Metadata:** MusicBrainz and related metadata services

## Requirements

- Android Studio or a compatible Android build environment
- Android SDK with the project's configured compile SDK
- An Android device or emulator containing local music files

## Project Status

Kanade is an **active work in progress**. Features and UI are still being refined, and some parts of the application may change as development continues.

The project is currently focused on:

- Improving the music library experience
- Better metadata identification
- More reliable album artwork handling
- Playback and queue improvements
- UI polish

## Roadmap

Planned improvements include:

- [ ] More robust metadata matching
- [ ] Improved album-art fallback and caching
- [ ] Better playlist management
- [ ] Album and artist browsing improvements
- [ ] More polished full-player experience
- [ ] Additional playback customization
- [ ] Performance improvements for large music libraries

## Contributing

Suggestions, bug reports, and improvements are welcome.

If you want to contribute, please open an issue or pull request with a clear description of the change and, when possible, steps to reproduce bugs.

## Disclaimer

Kanade is a personal/open-source Android music player project. Metadata and artwork obtained from external services remain subject to the terms and policies of those services.

## License

License information will be added as the project is finalized.

---

<p align="center">
  Made for people who just want to listen to their own music.
</p>
