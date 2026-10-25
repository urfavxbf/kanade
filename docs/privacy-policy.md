# Kanade Privacy Policy

Effective: October 25, 2026

Kanade is a native Android offline/online music player maintained at https://github.com/urfavxbf/kanade.

## Your local music and listening data

Kanade requests access to local audio files to display and play music stored on your Android device. Favorites, playlists, queue information, playback history, local display-tag overrides, and listening statistics are saved on-device. Kanade does not operate a user-account analytics server. Android device backups are disabled in the current manifest.

## Optional online catalogs

- **Audius:** Searching and playing online Audius tracks sends search terms and stream requests over HTTPS to Audius and its hosting infrastructure. Refer to Audius's privacy policy for their handling of network requests.
- **YouTube Data API:** Only after you affirmatively enable YouTube discovery, Kanade sends the text you enter in YouTube search, the restricted API key you configured, and Android package/certificate-identifying headers to Google's YouTube Data API over HTTPS. The API provides video metadata (titles, channels, thumbnails and IDs). Kanade displays search results temporarily and does not store YouTube search history or YouTube audiovisual media. Loading thumbnails connects to YouTube/Google image hosts. If you choose to open a video on YouTube, the official YouTube app or browser governs playback and its data practices.
- The YouTube API key is stored in Kanade's private app preferences on your device; it is never committed into the source repository or embedded into the shared APK. API keys are not a substitute for account authentication. Restrict your key to YouTube Data API and your app signing certificate as advised by Google.
- You can erase the configured key and revoke YouTube discovery consent inside Kanade. To remove other on-device data, clear the app's storage in Android Settings or uninstall Kanade.

## External privacy policies

- Google Privacy Policy: https://policies.google.com/privacy
- YouTube Terms of Service: https://www.youtube.com/t/terms
- YouTube API developer policies: https://developers.google.com/youtube/terms/developer-policies

Contact the maintainer through the GitHub repository's Issues tab with privacy concerns. This policy describes the implemented app's behavior; it is not a claim that third-party providers collect no information.
