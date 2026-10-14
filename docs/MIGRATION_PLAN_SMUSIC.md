# Kanade migration plan: SMusic as the foundation

Status: upstream source snapshot imported into `smusic-base/` on `migration/smusic-base`; CI build of that imported snapshot succeeded on 2026-10-14. This is a source/build baseline only, not yet a Kanade-branded or feature-complete migration, and it has not been verified on the user's phone.

## Goals

Use [SMusic](https://github.com/devxrohan/SMusic) as the technical foundation for Kanade's playback and online music experience while preserving Kanade-specific features and user data. Keep the existing `main` branch stable until the migration passes validation.

## Source and license

- Upstream: https://github.com/devxrohan/SMusic
- Upstream license: GPL-3.0 (review the complete license and comply with source/distribution obligations before publishing a derived build).
- Keep upstream attribution and license notices. Track upstream changes and document Kanade modifications.
- Do not copy secrets, signing keys, local configuration, or private user data.

## Current Kanade capabilities to preserve or deliberately replace

- Local audio scanning and playback
- Queue, shuffle, repeat, previous/next, mini-player/full-player navigation
- Background playback service, media notification and lock-screen controls
- Songs, artists, albums, playlists, favorites and listening/library browsing
- Album artwork, cached artwork and dynamic artwork colors
- Metadata editing, normalization and local metadata overrides
- Music identification via Chromaprint/AcoustID and metadata lookup via MusicBrainz
- Crash/debug screen
- Existing navigation, settings and Kanade branding
- Existing user data, especially playlists, favorites, metadata overrides and preferences

Audius discovery is intended to be replaced as the primary online source by SMusic's YouTube Music integration, subject to availability and testing.

## Migration stages

### 0. Protect the baseline

- Keep `main` unchanged during migration.
- Work on `migration/smusic-base`.
- Record current commit/build status and known bugs.
- Do not remove the existing implementation until a replacement is verified.

### 1. Baseline SMusic build

- [x] Import the complete upstream project and required modules into `smusic-base/`; preserve its license and notices.
- [x] Build the imported snapshot in GitHub Actions (run [38498523231](https://github.com/urfavxbf/kanade/actions/runs/38498523231)).
- [ ] Install and smoke-test the imported baseline on the user's phone.
- Inspect Gradle wrapper, Android Gradle Plugin, Kotlin, KSP, compile SDK, JDK, and dependency requirements against the actual Code On The Go environment.
- Resolve build compatibility in the migration branch only; avoid blind global version upgrades.
- Build and install an unmodified or minimally branded baseline before porting Kanade-specific features. CI build success does not prove phone installation or runtime playback.
- Record exact build command, tool versions, and failures. Do not call the baseline successful until an APK builds and runs on the target phone.

### 2. Kanade identity and UX

- Apply Kanade app name, icon, theme and branding without breaking upstream screens.
- Decide package/application ID deliberately. Changing it can install as a separate app and will not automatically inherit the old app's private data.
- Keep navigation coherent across home/library, search/discovery, now-playing, downloads, lyrics and settings.

### 3. Preserve Kanade-only features

Port and test one feature at a time, adapting Java logic to Kotlin/Compose and SMusic's architecture where appropriate:
1. Music identification (Chromaprint/AcoustID and MusicBrainz)
2. Metadata normalization/editing and local overrides
3. Album-art and dynamic-color behavior
4. Kanade-specific dashboard/navigation and debug tooling where still useful

Avoid copying old service/controller classes wholesale if that creates two competing playback engines. SMusic should own the primary playback path unless a feature requires a carefully defined adapter.

### 4. Data migration

- Inventory the old database, SharedPreferences, files and app-private storage before writing migration code.
- Define export/import formats for playlists, favorites, metadata overrides and settings.
- Map old records to SMusic's Room entities; handle duplicate tracks and missing files safely.
- Make imports idempotent and transactional where practical.
- Preserve originals and provide backup/restore until migration is verified.
- Test fresh install, upgrade/import, interrupted import, duplicate import and rollback scenarios.
- Never claim existing user data will survive an app-ID/database change without a tested migration path.

### 5. Regression and release gate

Required checks:
- Local music scan, permission flow and local playback
- YouTube Music search and stream playback on the target network/device
- Background playback, notification, lock screen, queue, shuffle and repeat
- Downloads/offline playback, lyrics, equalizer, sleep timer and history where provided
- Kanade music identification and metadata tools
- Favorites/playlists/data import and backup/restore
- Theme, navigation, settings, rotation/backgrounding and process restart
- Build/lint/tests and installation on the actual phone

Only merge to `main` after critical checks pass. Keep a known-good commit and a rollback path.

## Risks and decisions

- **COTG compatibility:** SMusic is a multi-module Kotlin/Compose project and may exceed the current mobile IDE's supported build toolchain. Validate before committing to full migration.
- **Unofficial online API:** YouTube Music/InnerTube behavior may change or fail; streaming is not guaranteed.
- **License:** GPL-3.0 obligations apply to derivative distribution. Review compliance before sharing APKs or source.
- **Data compatibility:** SMusic and Kanade have different architectures and persistence models; a deliberate migration is required.
- **Scope:** Prioritize a working build and playback first, then feature parity. Do not attempt a one-shot rewrite.

## Definition of done

A reproducible build installs on the target phone, local and online playback work, preserved Kanade-specific features pass regression checks, user data migration is verified, and the license/attribution requirements are met. Until then, the existing `main` branch remains the fallback.
