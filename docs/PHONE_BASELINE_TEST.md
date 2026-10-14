# SMusic baseline phone smoke test

Use this checklist with the debug APK produced by the `SMusic upstream baseline` workflow. This APK is the imported SMusic baseline, not yet the finished Kanade migration.

## Before installing
- Keep the current Kanade installation and its music/data intact.
- Install the baseline only if Android allows it alongside Kanade. If the package IDs conflict, do not uninstall Kanade just to test this build; stop and report the conflict.
- The APK is a debug/test build, not a release.

## First launch
- [x] App opens without crashing (user-confirmed on phone).
- [ ] Grant audio/media access when prompted.
- [x] Local library/playback baseline is working (user-confirmed on phone; detailed artist/album counts not yet checked).
- [ ] Denying media permission does not crash the app; granting it later allows scanning.
- [ ] Dark/light theme and navigation render correctly.

## Playback
- [x] Local music playback starts (user-confirmed on phone). Pause/resume, seek and skip still need explicit checks.
- [ ] Next/previous work with multiple tracks.
- [ ] Queue, shuffle and repeat behave correctly.
- [ ] Leave the app / lock the screen; playback continues as expected.
- [ ] Notification and lock-screen controls reflect the current track and operate.
- [ ] Disconnect/reconnect headphones or Bluetooth if available.

## Online and extras
- [ ] Search YouTube Music and play a result (network required; unofficial service may be unavailable).
- [ ] Open lyrics for a supported track.
- [ ] Test download/offline playback if available.
- [ ] Open equalizer/audio effects if supported by the device.
- [ ] Restart the app and verify local library/settings remain available.

## Report back
Please send only the failed step numbers and any visible error text/screenshots. Do not clear app storage or uninstall the current Kanade during this test. CI success is not a substitute for these device checks.
