# Kanade

Kanade is a Kotlin-first Android music player for **offline local music** and **online Audius discovery**. The interface uses a custom Compose design system rather than Material 3.

The new implementation is being developed through pull requests. The first milestone establishes the build, Media3 playback service, MediaStore music library, Audius search, custom player UI, and automated CI.

## Product principles

- Local music keeps playing without a network connection.
- Online streaming is clearly labeled and gracefully handles connectivity errors.
- The playback queue has one source of truth.
- Audio runs in a dedicated media service; the UI is independent of playback lifetime.
- No YouTube ripping, covert API scraping, or embedded account secrets.

See open pull requests for implementation progress.
