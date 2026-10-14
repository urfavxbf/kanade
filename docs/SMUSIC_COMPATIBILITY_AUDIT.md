# SMusic baseline compatibility audit

Date: 2026-10-14
Branch: `migration/smusic-base`

## Source inspection findings

Inspected the upstream public repository files:
- `settings.gradle.kts`
- `gradle/libs.versions.toml`
- `gradle/wrapper/gradle-wrapper.properties`
- `app/build.gradle.kts`

Observed configuration in the current upstream `main` snapshot:
- Gradle wrapper distribution: Gradle 8.13
- Android Gradle Plugin: 8.13.2
- Kotlin: 2.3.21
- KSP: 2.3.9
- Compose compiler plugin is enabled
- App uses compileSdk 36, minSdk 24, targetSdk 35
- App's Java source/target compatibility and Kotlin JVM toolchain are set to Java 21
- Project includes multiple modules: app, innertube, kugou, lrclib, material-color-utilities, jossredconnect, betterlyrics and shazamkit

## Initial compatibility assessment

This is not a drop-in replacement for the existing Java/XML Kanade project. It is a multi-module Kotlin/Compose project and the toolchain must be validated as a complete set. Do not change versions simply because the mobile IDE reports a newer version; use the actual COTG build environment and logs to choose a compatible toolchain.

The Java 21 toolchain requirement is an important compatibility checkpoint. Verify whether the COTG Gradle runtime and installed JDK can support the upstream configuration before attempting feature ports.

## Current Kanade baseline

The current Kanade `main` branch's latest observed CI run:
- Run: https://github.com/urfavxbf/kanade/actions/runs/38496920227
- Commit: `82959d944cc2faa8af8dfcb2a51b43b7110d0aa2`
- Result observed: success

This confirms the current branch's CI result only; it does not verify SMusic compatibility or the new migration branch.

## Next technical steps

1. Fetch or clone the complete upstream source into a working directory/branch, preserving all required files and license notices.
2. Run the upstream Gradle wrapper using the actual available JDK and Android SDK.
3. Record the first concrete build failure, if any, and fix the smallest compatible set of configuration issues.
4. Build and install the baseline before changing package identity or adding Kanade features.
5. Do not claim a successful migration until an APK is built and playback is verified on a physical device.
