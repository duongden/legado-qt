@echo off
REM Build script for DoubleArrayTrie dictionaries (Windows version)
REM This script converts text dictionaries to binary format for better memory efficiency

setlocal enabledelayedexpansion

set SCRIPT_DIR=%~dp0
set PROJECT_ROOT=%SCRIPT_DIR%..
set ASSETS_DIR=%PROJECT_ROOT%\app\src\main\assets\translate\vietphrase
set SCRIPT_PATH=%SCRIPT_DIR%\DictionaryBuilder.kt

echo === DoubleArrayTrie Dictionary Build Script ===
echo Project Root: %PROJECT_ROOT%
echo Assets Dir: %ASSETS_DIR%
echo Script: %SCRIPT_PATH%

REM Check if input files exist
if not exist "%ASSETS_DIR%\Names.txt" (
    echo Error: Names.txt not found in %ASSETS_DIR%
    exit /b 1
)

if not exist "%ASSETS_DIR%\VietPhrase.txt" (
    echo Error: VietPhrase.txt not found in %ASSETS_DIR%
    exit /b 1
)

if not exist "%SCRIPT_PATH%" (
    echo Error: DictionaryBuilder.kt not found in %SCRIPT_DIR%
    exit /b 1
)

REM Check if kotlin is available
kotlin -version >nul 2>&1
if errorlevel 1 (
    echo Error: kotlin command not found. Please install Kotlin compiler.
    echo Download from: https://github.com/JetBrains/kotlin/releases
    exit /b 1
)

echo.
echo Converting text dictionaries to binary format...

REM Run the conversion
kotlin "%SCRIPT_PATH%" "%ASSETS_DIR%" "%ASSETS_DIR%"

echo.
echo Verifying generated files...

REM Check if binary files were created
if exist "%ASSETS_DIR%\Names.dat" (
    for %%A in ("%ASSETS_DIR%\Names.dat") do set NAMES_SIZE=%%~zA
    echo ✓ Names.dat created (!NAMES_SIZE! bytes)
) else (
    echo ✗ Names.dat not created
    exit /b 1
)

if exist "%ASSETS_DIR%\VietPhrase.dat" (
    for %%A in ("%ASSETS_DIR%\VietPhrase.dat") do set VIETPHRASE_SIZE=%%~zA
    echo ✓ VietPhrase.dat created (!VIETPHRASE_SIZE! bytes)
) else (
    echo ✗ VietPhrase.dat not created
    exit /b 1
)

REM Show compression stats
if exist "%ASSETS_DIR%\Names.txt" (
    for %%A in ("%ASSETS_DIR%\Names.txt") do set NAMES_TXT_SIZE=%%~zA
    if defined NAMES_TXT_SIZE (
        set /a COMPRESSION=(100 - (!NAMES_SIZE! * 100 / !NAMES_TXT_SIZE!))
        echo   Names compression: !COMPRESSION!%%
    )
)

if exist "%ASSETS_DIR%\VietPhrase.txt" (
    for %%A in ("%ASSETS_DIR%\VietPhrase.txt") do set VIETPHRASE_TXT_SIZE=%%~zA
    if defined VIETPHRASE_TXT_SIZE (
        set /a COMPRESSION=(100 - (!VIETPHRASE_SIZE! * 100 / !VIETPHRASE_TXT_SIZE!))
        echo   VietPhrase compression: !COMPRESSION!%%
    )
)

echo.
echo Build completed successfully!
echo Binary dictionaries are ready for use with DoubleArrayTrie.
echo.
echo Next steps:
echo 1. Rebuild the Android app
echo 2. Test translation functionality
echo 3. Monitor memory usage improvements

pause
