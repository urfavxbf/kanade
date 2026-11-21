# Kanade privacy information

Kanade stores local favorites, playlists, playback queue, history, and settings on the device. The app does not operate an account analytics server. Android backups are disabled.

Choosing Sign in with Google sends a Google credential to Firebase Authentication. Firebase maintains the Kanade account identifier and associated Google profile information, including the email address. Settings shows the signed-in identity and offers sign-out. Guest access keeps the listening library on the device and does not require an account.

When online music is used, search terms, requests for track information and audio, device network information such as IP address, and requests for thumbnails are sent to YouTube and its service providers. Their privacy practices apply. Kanade uses the unofficial NewPipeExtractor library for these requests. The app does not require a YouTube login or API key for this feature.

Saved Audius tracks from earlier versions may still connect to Audius when played. Clear app storage in Android settings to remove the app's local data.

Google privacy policy: https://policies.google.com/privacy
