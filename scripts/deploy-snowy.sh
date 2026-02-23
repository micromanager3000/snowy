#!/usr/bin/env bash
# Deploy ZeroClaw + config to the Android device via ADB.
#
# Usage:
#   ./scripts/deploy-snowy.sh          # deploy config + SOUL only
#   ./scripts/deploy-snowy.sh --build  # cross-compile and deploy binary too
#
# Prerequisites:
#   - adb connected to device
#   - android/config.toml exists (copy from config.toml.example, add API key)
#   - For --build: cargo-ndk installed, Android NDK available

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
REPO_DIR="$(dirname "$SCRIPT_DIR")"
ANDROID_DIR="$REPO_DIR/android"
ZEROCLAW_DIR="$REPO_DIR/zeroclaw"
DEVICE_HOME="/data/local/tmp"
DEVICE_CONFIG="$DEVICE_HOME/.zeroclaw"
DEVICE_WORKSPACE="$DEVICE_CONFIG/workspace"

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

# Build if requested
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

echo ""
echo "Done! Test with:"
echo "  adb shell \"HOME=$DEVICE_HOME $DEVICE_HOME/zeroclaw agent -m 'Hi Snowy!'\""
