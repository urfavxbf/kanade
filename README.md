# Kanade

Kanade is a native Android music player built with Kotlin, Media3, and Compose Foundation. It plays local music and includes an experimental, unofficial YouTube search and audio player.

## Music

- Browse local songs, folders, albums, and artists; create playlists and favorites.
- Search YouTube without an API key and play available results inside Kanade. Search and stream resolution use [NewPipeExtractor](https://github.com/TeamNewPipe/NewPipeExtractor), a GPL-3.0-or-later dependency. This integration is unofficial and may stop working when YouTube changes.
- The Media3 session manages the queue, background playback, seek, shuffle, repeat and sleep timer. Stream URLs are resolved at playback time rather than saved as permanent links.
- Existing saved Audius entries are retained, though Audius search is removed. Their availability is not guaranteed.

See [privacy](docs/privacy-policy.md) and [terms](docs/terms.md). YouTube's API policies restrict separating audio and background playback; this unofficial integration is not an official YouTube API implementation. Check provider terms before distributing or using it.

## Build and tests

GitHub Actions runs unit tests, debug APK build and Android lint; retrieve the `kanade-debug` artifact from successful runs. Real-device playback requires separate Android testing. NewPipeExtractor is distributed under GPL-3.0-or-later; comply with its license if distributing a build containing it.

### Kanade account setup

Enable the Google provider in Firebase Authentication. Register the Android app
`com.urfavxbf.kanade` with its signing certificate SHA-1, then download the updated
`google-services.json` containing the web OAuth client and place it under `app/`.
A build without the configuration supports guest listening.

For Android CI, store the JSON contents in the repository Actions secret
`GOOGLE_SERVICES_JSON`. For a repeatable debug signing certificate, store a
dedicated Android debug keystore (standard Android debug alias and password)
encoded as base64 in `KANADE_DEBUG_KEYSTORE_BASE64` and register its SHA-1.
Keep release signing keys separate. To verify sign-in, install the matching
build on a device with Google Play services, sign in, reopen Kanade, then sign
out from Settings. Check that guest playback also works offline.
