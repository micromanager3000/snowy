# 006: ZeroClaw Foreground Service APK

**Status:** In Progress
**Created:** 2026-02-22
**Related projects:** Builds on 002_zeroclaw-on-android.md (Phase 1: ADB binary)

---

## Initial Prompt

> ok let's go ahead on phase 2 of zeroclaw on android to make it so that snowy will run continuously on there and if the phone reboots will startup on boot

## Background

Phase 1 (project 002) got ZeroClaw cross-compiled and running on the Motorola via `adb shell`. It works but has limitations:
- Process dies when ADB session ends or screen locks
- Must manually start via ADB each time
- No boot survival

Phase 2 wraps ZeroClaw in a minimal Android APK with a Foreground Service so it runs continuously and auto-starts on boot.

## Context

Snowy needs to be always-on — a robot pet that dies when you unplug the USB cable isn't useful. The Foreground Service approach is the standard Android pattern for long-running background work. Combined with a BOOT_COMPLETED receiver, Snowy will start automatically after any reboot.

## Approach

### Key Decision: exec() Binary, Not JNI

The simplest approach is to ship the existing ZeroClaw binary inside the APK and `exec()` it from a Kotlin Foreground Service. This avoids refactoring ZeroClaw into a JNI library.

**How it works:**
1. Name the binary `libzeroclaw.so` and place it in `jniLibs/arm64-v8a/`
2. Android extracts it to `nativeLibraryDir` (which is executable, unlike `filesDir`)
3. The Foreground Service spawns it via `ProcessBuilder`
4. `START_STICKY` tells Android to restart the service if killed

This is the same approach Termux uses for shipping native binaries.

### Resolved Decisions

1. **ZeroClaw mode:** Use `zeroclaw daemon` (long-running with scheduler/channels).
2. **Config deployment:** Keep using ADB push for now — easier for rapid iterations via Claude Code.
3. **API key handling:** Keep it simple — stays in config.toml, deployed via ADB push.
4. **Process monitoring:** Exponential backoff, 10 retries max.
5. **Build tools:** Need to install Android SDK command-line tools + platform 33.

### Step 1: Create Android Project

Minimal Kotlin Android project. No Android Studio needed — just Gradle + command line.

```
snowy-android/
  app/
    src/main/
      java/com/snowy/pet/
        MainActivity.kt          # Minimal launcher (starts service, requests permissions)
        ZeroClawService.kt       # Foreground Service that exec()s zeroclaw
        BootReceiver.kt          # BOOT_COMPLETED receiver to auto-start
      res/
        drawable/
          ic_notification.xml    # Simple notification icon
        values/
          strings.xml
      AndroidManifest.xml
    build.gradle.kts
  build.gradle.kts
  settings.gradle.kts
  gradle.properties
```

### Step 2: AndroidManifest.xml

Key permissions and declarations:
- `FOREGROUND_SERVICE` + `FOREGROUND_SERVICE_DATA_SYNC` — run in background
- `POST_NOTIFICATIONS` — show persistent notification (Android 13+)
- `RECEIVE_BOOT_COMPLETED` — auto-start on boot
- `android:extractNativeLibs="true"` — extract libzeroclaw.so to executable path
- `targetSdk 33` — avoids stricter API 34 foreground service type enforcement

### Step 3: Foreground Service (ZeroClawService.kt)

The service:
1. Creates a persistent notification ("Snowy is running")
2. Calls `startForeground()` within 5 seconds (Android requirement)
3. Spawns `libzeroclaw.so` via `ProcessBuilder` with:
   - `HOME` set to `context.filesDir` (where config/SQLite live)
   - Reads API key from config on device
4. Monitors the process — restarts with exponential backoff (10 retries max)
5. Returns `START_STICKY` so Android restarts the service if killed

### Step 4: Boot Receiver (BootReceiver.kt)

Simple broadcast receiver:
- Listens for `BOOT_COMPLETED`
- Calls `startForegroundService()` to launch ZeroClawService
- Note: app must be opened at least once before boot receiver works (Android security)

### Step 5: Build Pipeline

```bash
# 1. Cross-compile ZeroClaw
cargo ndk --platform 30 -t arm64-v8a build --bin zeroclaw --release

# 2. Copy + rename binary into APK jniLibs
cp target/aarch64-linux-android/release/zeroclaw \
   snowy-android/app/src/main/jniLibs/arm64-v8a/libzeroclaw.so

# 3. Build APK
cd snowy-android && ./gradlew assembleDebug

# 4. Install via ADB
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Config + SOUL.md continue to be deployed via `deploy-snowy.sh` (ADB push).

### Step 6: Deploy Script Update

Update `scripts/deploy-snowy.sh` with a `--apk` flag that builds and installs the APK.

### Step 7: Battery Optimization

Prompt user (or automate via ADB) to whitelist the app from battery optimization:
```bash
adb shell dumpsys deviceidle whitelist +com.snowy.pet
```

### Limitations

- App must be opened once after install before boot auto-start works
- OEM battery optimization may still interfere (Motorola is relatively friendly here)
- No UI beyond notification — Phase 3 adds the pet face

## Files Changed

```
# New in snowy repo:
snowy-android/                               # Android project
  app/src/main/
    java/com/snowy/pet/
      MainActivity.kt
      ZeroClawService.kt
      BootReceiver.kt
    res/...
    AndroidManifest.xml
  build.gradle.kts
  settings.gradle.kts

# Modified:
scripts/deploy-snowy.sh                      # Add --apk flag
.gitignore                                   # Add snowy-android build artifacts
```

## Discussion Log

- **User:** Requested Phase 2 — continuous running + boot survival for ZeroClaw on Android.
- **Claude:** Researched approaches. Key finding: exec() the binary from a Foreground Service is far simpler than JNI. Ship binary as `libzeroclaw.so` in jniLibs, Android extracts it to executable nativeLibraryDir. BOOT_COMPLETED receiver for auto-start. targetSdk 33 to avoid API 34 strictness.
- **User:** Answered open questions: use daemon mode, keep ADB push for config, keep API key in config.toml, exponential backoff with 10 retries, unsure about build tools.
- **Decision:** All 5 open questions resolved. Moving to implementation.

## Progress

- 2026-02-22: Implementation complete. All files created, APK builds cleanly.
  - Installed JDK 17 (`brew install openjdk@17`) and Android SDK platform 33 + build-tools 33.0.3
  - Created snowy-android/ project: settings.gradle.kts, build.gradle.kts, app/build.gradle.kts, gradle.properties
  - Generated Gradle wrapper (8.5)
  - Wrote AndroidManifest.xml with FOREGROUND_SERVICE, POST_NOTIFICATIONS, BOOT_COMPLETED permissions
  - Wrote MainActivity.kt (permission request + service launch)
  - Wrote ZeroClawService.kt (foreground service, ProcessBuilder exec of libzeroclaw.so, exponential backoff with 10 retries)
  - Wrote BootReceiver.kt (BOOT_COMPLETED → startForegroundService)
  - Added ic_notification.xml (snowflake vector), strings.xml
  - Updated deploy-snowy.sh with --apk flag (builds binary, packages APK, installs, deploys config via run-as)
  - Updated .gitignore for snowy-android build artifacts
  - Clean build succeeds: 6.8MB debug APK
  - **Next step:** Cross-compile ZeroClaw binary, run `deploy-snowy.sh --apk` with device connected
- 2026-02-22: Resolved open questions, moved to in-progress. Starting implementation.
