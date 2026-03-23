#!/bin/bash

# Build script for DoubleArrayTrie dictionaries
# This script converts text dictionaries to binary format for better memory efficiency

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
ASSETS_DIR="$PROJECT_ROOT/app/src/main/assets/translate/vietphrase"
SCRIPT_PATH="$SCRIPT_DIR/DictionaryBuilder.kt"

echo "=== DoubleArrayTrie Dictionary Build Script ==="
echo "Project Root: $PROJECT_ROOT"
echo "Assets Dir: $ASSETS_DIR"
echo "Script: $SCRIPT_PATH"

# Check if input files exist
if [ ! -f "$ASSETS_DIR/Names.txt" ]; then
    echo "Error: Names.txt not found in $ASSETS_DIR"
    exit 1
fi

if [ ! -f "$ASSETS_DIR/VietPhrase.txt" ]; then
    echo "Error: VietPhrase.txt not found in $ASSETS_DIR"
    exit 1
fi

if [ ! -f "$SCRIPT_PATH" ]; then
    echo "Error: DictionaryBuilder.kt not found in $SCRIPT_DIR"
    exit 1
fi

# Check if kotlin is available
if ! command -v kotlin &> /dev/null; then
    echo "Error: kotlin command not found. Please install Kotlin compiler."
    echo "On macOS: brew install kotlin"
    echo "On Ubuntu: sudo apt install kotlin"
    exit 1
fi

echo ""
echo "Converting text dictionaries to binary format..."

# Run the conversion
kotlin "$SCRIPT_PATH" "$ASSETS_DIR" "$ASSETS_DIR"

echo ""
echo "Verifying generated files..."

# Check if binary files were created
if [ -f "$ASSETS_DIR/Names.dat" ]; then
    NAMES_SIZE=$(stat -f%z "$ASSETS_DIR/Names.dat" 2>/dev/null || stat -c%s "$ASSETS_DIR/Names.dat" 2>/dev/null || echo "unknown")
    echo "✓ Names.dat created ($NAMES_SIZE bytes)"
else
    echo "✗ Names.dat not created"
    exit 1
fi

if [ -f "$ASSETS_DIR/VietPhrase.dat" ]; then
    VIETPHRASE_SIZE=$(stat -f%z "$ASSETS_DIR/VietPhrase.dat" 2>/dev/null || stat -c%s "$ASSETS_DIR/VietPhrase.dat" 2>/dev/null || echo "unknown")
    echo "✓ VietPhrase.dat created ($VIETPHRASE_SIZE bytes)"
else
    echo "✗ VietPhrase.dat not created"
    exit 1
fi

# Show compression stats
if [ -f "$ASSETS_DIR/Names.txt" ] && [ -f "$ASSETS_DIR/Names.dat" ]; then
    NAMES_TXT_SIZE=$(stat -f%z "$ASSETS_DIR/Names.txt" 2>/dev/null || stat -c%s "$ASSETS_DIR/Names.txt" 2>/dev/null || echo "0")
    if [ "$NAMES_TXT_SIZE" != "0" ] && [ "$NAMES_SIZE" != "unknown" ]; then
        COMPRESSION=$(echo "scale=1; (1 - $NAMES_SIZE / $NAMES_TXT_SIZE) * 100" | bc -l 2>/dev/null || echo "N/A")
        echo "  Names compression: $COMPRESSION%"
    fi
fi

if [ -f "$ASSETS_DIR/VietPhrase.txt" ] && [ -f "$ASSETS_DIR/VietPhrase.dat" ]; then
    VIETPHRASE_TXT_SIZE=$(stat -f%z "$ASSETS_DIR/VietPhrase.txt" 2>/dev/null || stat -c%s "$ASSETS_DIR/VietPhrase.txt" 2>/dev/null || echo "0")
    if [ "$VIETPHRASE_TXT_SIZE" != "0" ] && [ "$VIETPHRASE_SIZE" != "unknown" ]; then
        COMPRESSION=$(echo "scale=1; (1 - $VIETPHRASE_SIZE / $VIETPHRASE_TXT_SIZE) * 100" | bc -l 2>/dev/null || echo "N/A")
        echo "  VietPhrase compression: $COMPRESSION%"
    fi
fi

echo ""
echo "Build completed successfully!"
echo "Binary dictionaries are ready for use with DoubleArrayTrie."
echo ""
echo "Next steps:"
echo "1. Rebuild the Android app"
echo "2. Test translation functionality"
echo "3. Monitor memory usage improvements"
