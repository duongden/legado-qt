#!/bin/bash

# Change to the script's directory
cd "$(dirname "$0")"

# Find the best ADB candidate
if command -v adb.exe &> /dev/null; then
    ADB_CMD="adb.exe"
elif command -v adb &> /dev/null; then
    ADB_CMD="adb"
else
    # Fallback to SDK directory
    SDK_DIR=$(grep 'sdk.dir' local.properties | cut -d'=' -f2)
    [ -z "$SDK_DIR" ] && SDK_DIR="$HOME/Android/sdk"
    if [ -d "$SDK_DIR/platform-tools" ]; then
        export PATH="$SDK_DIR/platform-tools:$PATH"
        ADB_CMD="adb"
    else
        echo "[ERROR] adb not found. Please install Android Platform Tools."
        exit 1
    fi
fi

# Path to the debug APK directory
APK_DIR="app/build/outputs/apk/app/debug"

echo "[INFO] Searching for APK in: $APK_DIR"

if [ ! -d "$APK_DIR" ]; then
    echo "[ERROR] Directory not found."
    echo "[INFO] Make sure you have built the project using: ./gradlew assembleDebug"
    exit 1
fi

# Find the APK file (taking the most recent one if multiple)
APK_FILE=$(ls -t "$APK_DIR"/*.apk 2>/dev/null | head -n 1)

if [ -z "$APK_FILE" ]; then
    echo "[ERROR] No APK file found in $APK_DIR."
    exit 1
fi

echo "[INFO] Found APK: $APK_FILE"
echo "[INFO] Starting installation..."

# Install the APK
# -r: reinstall if needed
# -t: allow test APKs
# -d: allow version downgrade
"$ADB_CMD" install -r -t -d "$APK_FILE"

if [ $? -ne 0 ]; then
    echo "[ERROR] Installation failed. Check if your device is connected and authorized."
    "$ADB_CMD" devices
else
    echo "[SUCCESS] Installation complete."
fi
