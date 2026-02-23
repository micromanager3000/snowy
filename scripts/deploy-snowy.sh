#!/usr/bin/env bash
# Deploy ZeroClaw + config to the Android device via ADB.
#
# Usage:
#   ./scripts/deploy-snowy.sh          # deploy config + SOUL only
#   ./scripts/deploy-snowy.sh --build  # cross-compile and deploy binary too
#   ./scripts/deploy-snowy.sh --apk    # build APK (includes binary) and install
#
# Prerequisites:
#   - adb connected to device
#   - android/config.toml exists (copy from config.toml.example, add API key)
#   - For --build: cargo-ndk installed, Android NDK available
#   - For --apk: JDK 17 (brew install openjdk@17), Android SDK

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
REPO_DIR="$(dirname "$SCRIPT_DIR")"
ANDROID_DIR="$REPO_DIR/android"
ZEROCLAW_DIR="$REPO_DIR/zeroclaw"
APK_DIR="$REPO_DIR/snowy-android"
SKILLS_DIR="$REPO_DIR/agent/skills"
DEVICE_HOME="/data/local/tmp"
DEVICE_CONFIG="$DEVICE_HOME/.zeroclaw"
DEVICE_WORKSPACE="$DEVICE_CONFIG/workspace"
# APK uses app-private storage for config
APK_DEVICE_CONFIG="/data/data/com.snowy.pet/files/.zeroclaw"
APK_DEVICE_WORKSPACE="/data/data/com.snowy.pet/files/.zeroclaw/workspace"

# Verify ADB connection
if ! adb get-state >/dev/null 2>&1; then
    echo "Error: No ADB device connected"
    exit 1
fi

# Verify config exists
if [ ! -f "$ANDROID_DIR/config.toml" ]; then
    echo "Error: android/config.toml not found"
    echo "  cp android/config.toml.example android/config.toml"
    echo "  Then add your API key."
    exit 1
fi

# APK mode: build binary, package APK, install, deploy config to app-private storage
if [[ "${1:-}" == "--apk" ]]; then
    echo "==> Building ZeroClaw for Android ARM64..."
    export ANDROID_NDK_HOME="${ANDROID_NDK_HOME:-$HOME/Library/Android/sdk/ndk/27.2.12479018}"
    (cd "$ZEROCLAW_DIR" && cargo ndk --platform 30 -t arm64-v8a build --bin zeroclaw --release)

    echo "==> Copying binary into APK jniLibs..."
    mkdir -p "$APK_DIR/app/src/main/jniLibs/arm64-v8a"
    cp "$ZEROCLAW_DIR/target/aarch64-linux-android/release/zeroclaw" \
       "$APK_DIR/app/src/main/jniLibs/arm64-v8a/libzeroclaw.so"

    echo "==> Building APK..."
    export JAVA_HOME="${JAVA_HOME:-/opt/homebrew/opt/openjdk@17}"
    export ANDROID_HOME="${ANDROID_HOME:-$HOME/Library/Android/sdk}"
    (cd "$APK_DIR" && ./gradlew assembleDebug)

    echo "==> Installing APK..."
    adb install -r "$APK_DIR/app/build/outputs/apk/debug/app-debug.apk"

    echo "==> Deploying config to app-private storage..."
    adb shell run-as com.snowy.pet mkdir -p files/.zeroclaw/workspace 2>/dev/null || true
    # Use push + cp since run-as can't receive push directly
    adb push "$ANDROID_DIR/config.toml" "/data/local/tmp/_snowy_config.toml"
    adb shell "run-as com.snowy.pet cp /data/local/tmp/_snowy_config.toml files/.zeroclaw/config.toml"
    adb shell "run-as com.snowy.pet chmod 600 files/.zeroclaw/config.toml"
    adb shell rm /data/local/tmp/_snowy_config.toml

    for md in "$ANDROID_DIR"/*.md; do
        [ -f "$md" ] || continue
        local_name="$(basename "$md")"
        echo "    $local_name"
        adb push "$md" "/data/local/tmp/_snowy_$local_name"
        adb shell "run-as com.snowy.pet cp /data/local/tmp/_snowy_$local_name files/.zeroclaw/workspace/$local_name"
        adb shell rm "/data/local/tmp/_snowy_$local_name"
    done

    echo "==> Deploying skills..."
    adb shell "run-as com.snowy.pet mkdir -p files/.zeroclaw/workspace/skills" 2>/dev/null || true
    for skill_dir in "$SKILLS_DIR"/*/; do
        [ -d "$skill_dir" ] || continue
        skill_name="$(basename "$skill_dir")"
        echo "    skills/$skill_name/"
        adb shell "run-as com.snowy.pet mkdir -p files/.zeroclaw/workspace/skills/$skill_name" 2>/dev/null || true
        for file in "$skill_dir"*; do
            [ -f "$file" ] || continue
            fname="$(basename "$file")"
            adb push "$file" "/data/local/tmp/_snowy_skill_$fname"
            adb shell "run-as com.snowy.pet cp /data/local/tmp/_snowy_skill_$fname files/.zeroclaw/workspace/skills/$skill_name/$fname"
            adb shell rm "/data/local/tmp/_snowy_skill_$fname"
        done
        # Make .sh files executable
        adb shell "run-as com.snowy.pet sh -c 'chmod +x files/.zeroclaw/workspace/skills/$skill_name/*.sh'" 2>/dev/null || true
    done

    echo "==> Whitelisting from battery optimization..."
    adb shell dumpsys deviceidle whitelist +com.snowy.pet 2>/dev/null || true

    echo ""
    echo "Done! Open the Snowy app on the device to start the service."
    echo "Snowy will auto-start on boot after first launch."
    exit 0
fi

# Build if requested (legacy ADB binary mode)
if [[ "${1:-}" == "--build" ]]; then
    echo "==> Building ZeroClaw for Android ARM64..."
    export ANDROID_NDK_HOME="${ANDROID_NDK_HOME:-$HOME/Library/Android/sdk/ndk/27.2.12479018}"
    (cd "$ZEROCLAW_DIR" && cargo ndk --platform 30 -t arm64-v8a build --bin zeroclaw --release)

    echo "==> Pushing binary to device..."
    adb push "$ZEROCLAW_DIR/target/aarch64-linux-android/release/zeroclaw" "$DEVICE_HOME/"
    adb shell chmod 755 "$DEVICE_HOME/zeroclaw"
fi

# Create directories on device
adb shell mkdir -p "$DEVICE_CONFIG" "$DEVICE_WORKSPACE"

# Push config (with restrictive permissions)
echo "==> Deploying config.toml..."
adb push "$ANDROID_DIR/config.toml" "$DEVICE_CONFIG/config.toml"
adb shell chmod 600 "$DEVICE_CONFIG/config.toml"

# Push all .md files from android/ to workspace
echo "==> Deploying personality files..."
for md in "$ANDROID_DIR"/*.md; do
    [ -f "$md" ] || continue
    echo "    $(basename "$md")"
    adb push "$md" "$DEVICE_WORKSPACE/$(basename "$md")"
done

# Deploy skill scripts from agent/skills/
echo "==> Deploying skills..."
for skill_dir in "$SKILLS_DIR"/*/; do
    [ -d "$skill_dir" ] || continue
    skill_name="$(basename "$skill_dir")"
    echo "    skills/$skill_name/"
    adb shell mkdir -p "$DEVICE_WORKSPACE/skills/$skill_name"
    adb push "$skill_dir"* "$DEVICE_WORKSPACE/skills/$skill_name/"
    # Make .sh files executable
    adb shell "chmod +x $DEVICE_WORKSPACE/skills/$skill_name/*.sh" 2>/dev/null || true
done

echo ""
echo "Done! Test with:"
echo "  adb shell \"HOME=$DEVICE_HOME $DEVICE_HOME/zeroclaw agent -m 'Hi Snowy!'\""
