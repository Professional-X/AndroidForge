# XIVOX Video Player — Android Studio Project Build Guide

**Package:** `com.xivox.player`
**Tagline:** Beyond Playback.
**Architecture:** Kotlin · Jetpack Compose · Material 3 · Media3 ExoPlayer · Room · DataStore · MediaStore · SAF · Coil

## Privacy & Offline Guarantee
- `AndroidManifest.xml` intentionally omits `android.permission.INTERNET` and `ACCESS_NETWORK_STATE`.
- No analytics, no crash reporting SDKs, no login, no ads, and no cloud uploads.
- All playback history (Continue Watching), hidden folders, favorites, and user preferences persist 100% locally via Room (`xivox_player.db`) and DataStore (`xivox_settings`).

## Build & Install APK

### Option 1: Android Studio
1. Open the repository root folder in **Android Studio Ladybug (2024.2+)** or newer.
2. Ensure **JDK 17** and **Android SDK Platform 35** are installed via SDK Manager.
3. Let Gradle sync using `gradle/libs.versions.toml` and `gradle/wrapper/gradle-wrapper.properties` (Gradle 8.9).
4. Select **Build > Build Bundle(s) / APK(s) > Build APK(s)** or run directly on a physical Android device / emulator.

### Option 2: Command Line
```bash
# Run unit tests (subtitle parser & formatters)
./gradlew testDebugUnitTest

# Build debug APK
./gradlew assembleDebug
# Output: app/build/outputs/apk/debug/app-debug.apk

# Build minified release APK
./gradlew assembleRelease
# Output: app/build/outputs/apk/release/app-release.apk
```
