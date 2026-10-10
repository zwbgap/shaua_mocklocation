# Qianjing (MockLocation)

> A mock location app for Android 8.0+ that works without ROOT, built on the Android Debug API + Baidu Maps SDK.

---

## About This Project

This project is forked and modified from the open-source project **影梭 (GoGoGo)**.

- **Original Project**: [影梭 GoGoGo](https://github.com/ZCShou/GoGoGo) — by ZCShou, licensed under GPL-3.0
- **This Project**: [迁境 (Qianjing)](https://github.com/zwbgap/shaua_mocklocation) — an architecture-refactored, feature-simplified, and UX-enhanced fork

This project is released under the GPL-3.0 license. See [LICENSE](./LICENSE) for details.

---

## Features

### Core Features

- **Mock Location**: Injects GPS and NETWORK providers simultaneously via the Android Debug API — no ROOT required
- **Baidu Map Picking**: Map display with tap / long-press / double-tap interaction, normal and satellite view switching
- **POI Reverse Geocoding**: Long-press the map to pop up location details with save, copy, share, and teleport actions
- **POI Suggestion Search**: Baidu Maps suggestion search, supporting nationwide and city-scoped queries
- **Manual Coordinate Input**: Supports BD09 / WGS84 coordinate input; auto-parses coordinate strings separated by commas, spaces, etc.
- **Runtime Location Update**: Re-select a point while mocking is running and the location updates in real time
- **Favorites**: SQLite local storage for favorite locations with full CRUD and search support

### Auxiliary Features

- **Altitude Configuration**: Customize the altitude of the mocked location
- **Random Offset**: Apply random latitude/longitude offset to favorite locations (max distance configurable)
- **Theme Customization**: Four color themes — Teal, Blue, Purple, Orange — applied globally including status and navigation bars
- **User Agreement & Privacy Policy**: Required reading on first launch
- **Runtime Permission Management**: Location, storage, and other permissions requested on demand
- **Local Logging**: XLog-based local runtime logs with toggle, viewer, and share support
- **History Records**: Search history auto-recorded with configurable expiration

### Removed Features (vs. original project)

To keep the experience focused, the following features from the original project were removed:

- Floating joystick (disc joystick + 8-direction button joystick)
- Simulated movement (walk / run / bike speed switching)
- Orientation sensor for map direction display

---

## Requirements

| Dependency | Version |
|---|---|
| JDK | 17 or higher |
| Android SDK | platform 34, build-tools 36.0.0 |
| Gradle | 8.13 (downloaded automatically by the Wrapper) |
| Android Gradle Plugin | 8.12.1 |
| Baidu Maps SDK | BaiduLBS_Android v7.6.5 |

---

## Build Instructions

### 1. Configure local.properties

Create a `local.properties` file in the project root (this file is not committed) with your Baidu Maps AK and signing info:

```properties
sdk.dir=path/to/your/Android/Sdk

# Baidu Maps AK
MAPS_API_KEY=your_baidu_maps_ak

# Signing config
storeFile=keystore/mocklocation.jks
storePassword=your_store_password
keyAlias=your_key_alias
keyPassword=your_key_password
```

### 2. Build the APK

```bash
# Debug build
./gradlew assembleDebug

# Release build
./gradlew assembleRelease
```

Build artifacts are output to `app/build/outputs/apk/` with the naming convention:

- Debug: `迁境_<version>_arm64-v8a_debug.apk`
- Release: `迁境_<version>_arm64-v8a_release.apk`

### 3. Baidu Maps AK Setup

Go to the [Baidu Maps Open Platform](https://lbsyun.baidu.com/) to create an Android app and obtain an AK. Configure the security code:

- **Package name**: `com.shaua.mocklocation`
- **SHA1**: Use your signing certificate's SHA1
- **Security code**: `SHA1;com.shaua.mocklocation`

---

## Usage

1. Install the APK and launch the app
2. On first launch, read and accept the User Agreement and Privacy Policy
3. Grant location permissions
4. In **Settings → Developer options → Select mock location app**, choose 迁境 (Qianjing)
5. Pick a target location on the map and tap the teleport button to start mocking
6. Tap the stop button to restore your real location

---

## Project Structure

```
GoGoGo/
├── app/
│   ├── libs/                              # Baidu Maps SDK (jar + arm64-v8a .so)
│   ├── src/main/
│   │   ├── java/com/shaua/mocklocation/
│   │   │   ├── data/                      # Data layer (Model + DAO + Repository)
│   │   │   ├── database/                  # Search history database
│   │   │   ├── service/                   # MockLocationService (location injection)
│   │   │   ├── utils/                     # Utilities (GoUtils / MapUtils / ShareUtils)
│   │   │   ├── BaseActivity.java          # Base Activity (theme application)
│   │   │   ├── MainActivity.java          # Main UI (map + pick + start mocking)
│   │   │   ├── MainViewModel.java         # ViewModel (LiveData state)
│   │   │   ├── FavoritesFragment.java     # Favorites list
│   │   │   ├── MineFragment.java          # Settings page
│   │   │   ├── HistoryActivity.java       # History records
│   │   │   ├── LogViewerActivity.java     # Runtime log viewer
│   │   │   ├── SettingsActivity.java      # Settings entry
│   │   │   ├── WelcomeActivity.java       # Agreement & permission onboarding
│   │   │   └── GoApplication.java         # Application entry
│   │   ├── res/                           # Resources (layouts, drawables, strings, themes)
│   │   └── AndroidManifest.xml
│   ├── build.gradle
│   └── proguard-rules.pro
├── gradle/wrapper/                        # Gradle Wrapper
├── keystore/
│   └── mocklocation.jks                   # Signing keystore (password in local.properties)
├── docs/
│   ├── CHANGELOG.md                       # Changelog
│   └── build-log.md                       # Build log
├── local.properties                       # AK + signing config (not committed)
├── build.gradle / settings.gradle / gradle.properties
├── gradlew / gradlew.bat
└── README.md / README.en.md
```

---

## Technical Details

### Mock Location Implementation

1. Add both GPS and NETWORK providers via `LocationManager.addTestProvider()`
2. Enable providers with `setTestProviderEnabled()`
3. Push a Location every 100ms via `setTestProviderLocation()` (driven by a HandlerThread)
4. Location fields: `latitude`, `longitude`, `altitude`, `accuracy`, `bearing`, `speed`, `time`, `elapsedRealtimeNanos`

### Coordinate System Conversion

- **BD09** (Baidu Maps) → **GCJ02** (Mars coordinates) → **WGS84** (raw GPS, used for system injection)
- `MapUtils.java` implements `bd092gcj`, `gcj2wgs`, `wgs2gcj`, `gcj2bd09`, and related conversions

### Theme System

- `BaseActivity` reads the color preference from SharedPreferences in `onCreate` and applies the theme dynamically
- Supports Teal, Blue, Purple, Orange — applied globally to status bar, navigation bar, buttons, etc.
- Switching theme restarts the current page to take effect

---

## FAQ

**Q: The map shows a blank screen?**
A: Verify your Baidu Maps AK is of the Android SDK type, and that the SHA1 and package name match the configuration in the Baidu console.

**Q: Mock location is not working?**
A: Make sure you selected 迁境 in **Developer options → Select mock location app**, and that device location services are enabled.

**Q: No search results?**
A: Check your network connection and confirm your Baidu Maps AK has POI search permission enabled.

**Q: Build fails with a signing error?**
A: Confirm the signing configuration in `local.properties` matches `keystore/mocklocation.jks`.

---

## License

This project is open-sourced under the GPL-3.0 license. Copyright notices:

- Original project: © 2020 ZCShou (影梭 GoGoGo) — https://github.com/ZCShou/GoGoGo
- Modifications: © 2026 shaua (迁境)

See [LICENSE](./LICENSE) for the full text.

---

## Acknowledgments

Thanks to ZCShou for open-sourcing 影梭, and to the following reference projects:

- [MockGPS](https://github.com/Hilaver/MockGPS)
- [together-go](https://github.com/bxxfighting/together-go)
- [Mocklation](https://github.com/P72B/Mocklation)
