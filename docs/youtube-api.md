# YouTube Data API discovery (zero paid integration)

Kanade uses **YouTube Data API v3** for optional Philippine music discovery (not streaming YouTube audio). The **native Kanade player** remains responsible for local songs and authorized Audius tracks. The YouTube API does not supply audio URLs for Media3/ExoPlayer, and YouTube prohibits hidden/background video players, audio-only extraction, and unauthorized caching.

## Setup on an Android phone

1. In [Google Cloud Console](https://console.cloud.google.com/apis/library/youtube.googleapis.com), create/select your own project and enable **YouTube Data API v3**.
2. In [API Credentials](https://console.cloud.google.com/apis/credentials), create an API key. Apply **API restrictions: YouTube Data API v3**, and set **Android application restrictions** with the package `com.urfavxbf.kanade` and the installed APK's SHA-1 signing certificate.
3. Install the Kanade APK. In Online > YouTube, review Kanade's privacy policy and YouTube Terms, explicitly agree, then paste the key into **API key settings** in Kanade (not in chat or GitHub).
4. Search OPM songs/artists. Results are sourced from `youtube/v3/search` with `regionCode=PH`, `videoCategoryId=10`, and `type=video`.
5. Metadata results may be opened on YouTube in the official app or browser only by tapping an explicit action; they **cannot** be inserted into Kanade's native playback queue.

## Quota and security

Google's currently documented default Search Queries quota is **100 `search.list` requests/day** (1 unit each, with its own bucket); limits can change. Kanade makes requests only on explicit searches, not on every keystroke. Restrict keys by API and application and monitor usage in Google Cloud. **A key stored on a client device is never fully secret.** For a public/multiuser release, prefer a maintained backend with server-side credential storage and per-user abuse protection instead of sharing a key across APKs.

GitHub Actions debug builds may use different temporary signing certificates, so each APK's SHA-1 may need an allowed entry. A stable production keystore is recommended.

No YouTube API key is included with Kanade; **live searches do not work until a project key is configured.** Paid music licensing or YouTube audio access is not implied.

Read [Privacy Policy](privacy-policy.md) and [Terms](terms.md).
