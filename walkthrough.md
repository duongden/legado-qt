# Translation Feature Verification Walkthrough

## Overview
This walkthrough guides you through verifying the integration of the native Kotlin-based Chinese-to-Vietnamese translation feature in the Legado Android app. The implementation replaces the previous JavaScript-based approach, offering better performance and deeper integration.

## Features to Verify

### 1. Translation Settings
- **Location**: Settings -> Read Config -> Translation
- **Verify**:
    - The "Enable Translation" toggle (`translateEnable`) is present and functional.
    - The "Translation Provider" option has been removed (as the JS engine is no longer used).
    - Toggling "Enable Translation" updates the translation state immediately or on the next refresh.

### 2. Book Content Translation
- **Action**: Open a Chinese book or import a Chinese text file.
- **Verify**:
    - **Content**: The main text of the book should be translated into Vietnamese.
    - **Punctuation**: Chinese punctuation (e.g., `。`, `，`, `“`) should be converted to Vietnamese/Latin style (e.g., `. `, `, `, `"`).
    - **Names**: Proper names should be recognized and translated/capitalized correctly according to the `Names` dictionary.
    - **Phrases**: Common phrases should be translated using the `VietPhrase` dictionary.
    - **Phonetics**: Untranslated Chinese characters should be replaced with their Pinyin/Sino-Vietnamese pronunciation (Phien Am).
    - **Formatting**: Spacing and capitalization logic should be applied (e.g., capitalizing the first letter of sentences).

### 3. Performance & Stability
- **Action**: Scroll through chapters, open large books.
- **Verify**: The app should remain responsive. No crashes or significant lag should occur during translation.

### 4. UI Translation Verification
- **Bookshelf**: Check that book titles and author names are translated in both list and grid views.
- **Table of Contents**: Open the chapter list (both in reading view and detailed view) and verify chapter titles are translated.
- **Book Info**: Verify book name, author, intro, and latest chapter title are translated on the book details screen.
- **Search & Explore**: Search for a book or browse the explore tab. Verify titles, authors, and intros are translated.
- **Bookmarks**: Check that bookmark chapter names and content snippets are translated.
- **Change Source**: Verifiy source names and chapter titles in the "Change Source" dialog.

## Technical Changes Verified

- **Core Logic**: `TranslateUtils.kt` now contains the native Kotlin implementation of the translation algorithms.
- **UI Integration**: `setTranslatedText` extension is used across `ChapterListAdapter`, `SearchAdapter`, `BookInfoActivity`, `ReadMenu`, `BookmarkAdapter`, and others to automatically translate text.
- **Integration**: `ContentProcessor.kt` hooks into the content loading pipeline to apply translation when enabled.
- **Dictionary Loading**: `TranslationLoader.kt` efficiently loads `Names.txt`, `VietPhrase.txt`, and `ChinesePhienAmWords.txt` from assets.
- **Cleanup**: `Names2` dictionary and `Duktape` dependencies have been removed to reduce app size and complexity.
- **Preferences**: `pref_config_translate.xml` and `PreferKey.kt` have been updated to reflect the removal of the provider selection.

## Manual Verification Steps
1.  **Launch the App**: Open the Legado app on your Android device or emulator.
2.  **Enable Translation**: Go to Settings and enable the translation feature.
3.  **Open a Book**: Select a book with Chinese content.
4.  **Check Translation**: Confirm the text is translated to Vietnamese.
5.  **Disable Translation**: Go back to Settings and disable it.
6.  **Verify Original Text**: Return to the book and refresh the chapter; the text should revert to the original Chinese.
