# Kanade feature porting matrix

Date: 2026-10-14
Branch: `migration/smusic-base`

This matrix tracks feature parity; it is not a claim that any item has already been ported or verified on a device.

| Existing Kanade feature | SMusic foundation / likely integration point | Migration approach | Acceptance check |
|---|---|---|---|
| Local audio library and scanning | SMusic app library, Room entities and media access | Keep SMusic as the single library/playback owner; map old data rather than running two scanners | Scan on Android 13+ and older permission paths; library counts and metadata are correct |
| Playback, queue, shuffle/repeat | Media3 playback and SMusic queue | Do not transplant the old Java playback service; use the SMusic playback stack | Next/previous, queue edits, shuffle/repeat and seeking work |
| Background playback and notification | SMusic Media3 session/service | Preserve platform integration from SMusic; verify notification permission and lockscreen | Playback continues in background; notification and lockscreen controls work |
| YouTube Music discovery and streams | SMusic InnerTube module and online search | Prefer SMusic's own client instead of the old Audius/stream URL resolver | Search, select and play several tracks on the target network; expired stream recovery is sensible |
| Playlists and favorites | SMusic Room/library models | Build explicit export/import mapping; never silently discard old records | Import is repeatable, duplicates are handled, counts match |
| Metadata editing, normalization and overrides | SMusic song metadata and Room layer | Port as repository/service-level logic adapted to SMusic's model | Edit, normalize, restart app, verify persisted values and local file metadata |
| Chromaprint/AcoustID and MusicBrainz | Existing Kanade identification implementation | Port provider/service logic after playback/library baseline; keep keys/config out of source | Identify a known track; gracefully handle network, empty and API error results |
| Album art and dynamic colors | SMusic Coil/artwork and player styles | Reuse SMusic art pipeline unless a Kanade-specific behavior is demonstrably missing | Art cache, missing-art fallback and colors remain stable across tracks/theme changes |
| Lyrics | SMusic lyrics providers and synchronized player | Prefer built-in providers; assess whether any Kanade-specific lyric behavior needs porting | Correct lyrics load, sync, missing lyrics and provider failure paths behave |
| Downloads/offline playback | SMusic downloads subsystem | Use SMusic download/storage model; don't duplicate it with Kanade's old service | Download, play offline, delete, and recover after process restart |
| Equalizer and audio effects | SMusic equalizer/audio-session integration | Retain SMusic's implementation | Effect panel opens when supported; unsupported devices fail gracefully |
| Settings, theme, navigation and branding | Compose screens and settings in SMusic | Rebrand deliberately after baseline; port only settings that have no equivalent | Settings persist; dark/light themes and navigation work on phone |
| Debug/crash diagnostics | Existing Kanade DebugActivity | Port selectively as a Compose screen or debug-only tools | No sensitive data or secrets in diagnostic output |
| User data / settings | Old Kanade app-private storage vs SMusic Room/DataStore | Design explicit export/import before changing app ID or asking users to switch | Backup first; fresh import, interrupted import, repeat import and rollback are tested |

## Gates

1. CI build of the imported source passes (confirmed for run [38498523231](https://github.com/urfavxbf/kanade/actions/runs/38498523231)).
2. Install and smoke-test the imported baseline on the user's actual phone. CI alone does not prove device behavior.
3. Port features incrementally with a build after each coherent slice.
4. Implement and test data export/import before asking the user to replace the installed app.
5. Keep `main` unchanged until the phone checklist passes.

## Known risks

- SMusic uses Kotlin/Compose, multiple Gradle modules, Room and Media3; old Kanade Java/XML classes cannot be copied in wholesale without creating conflicting playback or persistence paths.
- YouTube Music is an unofficial integration and can change independently.
- Upstream is GPL-3.0; retain notices and meet source/distribution obligations for derived builds.
- Application ID changes can cause Android to treat the app as a separate installation and do not automatically migrate private app data.
