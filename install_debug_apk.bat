@echo off
setlocal enabledelayedexpansion

:: Change to the script's directory
cd /d "%~dp0"

:: Path to the debug APK directory
set "APK_DIR=app\build\outputs\apk\app\debug"

echo [INFO] Searching for APK in: %APK_DIR%

if not exist "%APK_DIR%" (
    echo [ERROR] Directory not found.
    echo [INFO] Make sure you have built the project using: gradlew assembleDebug
    pause
    exit /b
)

:: Find the APK file (taking the most recent one if multiple, or just the first found)
set "APK_FILE="
for %%f in ("%APK_DIR%\*.apk") do (
    set "APK_FILE=%%f"
)

if "%APK_FILE%"=="" (
    echo [ERROR] No APK file found in %APK_DIR%.
    pause
    exit /b
)

echo [INFO] Found APK: !APK_FILE!
echo [INFO] Starting installation...

:: Install the APK
:: -r: reinstall if needed
:: -t: allow test APKs
:: -d: allow version downgrade
adb install -r -t -d "!APK_FILE!"

if errorlevel 1 (
    echo [ERROR] Installation failed. Check if your device is connected and authorized.
    adb devices
) else (
    echo [SUCCESS] Installation complete.
)

pause
