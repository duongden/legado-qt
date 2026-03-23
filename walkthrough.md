# Translation Feature Verification Walkthrough

## Overview
This walkthrough guides you through verifying the integration of the native Kotlin-based Chinese-to-Vietnamese translation feature in the Legado Android app. The implementation replaces the previous JavaScript-based approach, offering better performance (via Trie) and deeper integration.

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

### 3. Performance & Stability (Trie)
- **Context**: We reverted from an experimental SQLite approach to an optimized In-Memory Trie for better reliability.
- **Action**: Scroll through chapters, open large books (3000+ chapters).
- **Verify**: The app should remain responsive. Translation should be nearly instantaneous.

### 4. UI Translation Verification
- **Bookshelf**: Check that book titles and author names are translated in both list and grid views.
- **Table of Contents**: Open the chapter list (both in reading view and detailed view) and verify chapter titles are translated.
- **Book Info**: Verify book name, author, intro, and latest chapter title are translated on the book details screen.
- **Search & Explore**: Search for a book or browse the explore tab. Verify titles, authors, and intros are translated.
- **Bookmarks**: Check that bookmark chapter names and content snippets are translated.
- **Change Source**: Verifiy source names and chapter titles in the "Change Source" dialog.

### 5. Web Service Translation (New)
- **Setup**: Start Web Service (More -> Web Service). Open displayed IP in PC Browser.
- **Verify**:
    - **Bookshelf**: Book Titles, Authors, and Intros should be in Vietnamese.
    - **Reading**: Chapter content should be translated exactly as in the App.
    - **TOC**: Chapter list in the web reader should show Vietnamese titles.
    - *Note*: Verify that disabling translation in App also disables it on Web Service immediately (upon page refresh).

### 6. Dictionary Management UI
- **Location**: Settings -> Translation -> Manage Dictionaries.
- **Verify**:
    - **Import**: Can select a `.txt` file to import into Names or VietPhrase.
    - **Reset**: Can reset dictionaries to default assets.
    - **Delete**: Can delete custom dictionaries.
    - **Reload**: After Import/Reset, translation in the reader should update automatically (you may need to clear cache or reopen book).

## Technical Changes Verified

- **Core Logic**: `TranslateUtils.kt` implements the native translation algorithms.
- **Data Structure**: `TranslationData.kt` uses optimized `Trie` for fast lookups.
- **UI Integration**: `setTranslatedText` extension is used across Adapters.
- **Web Integration**: `BookController.kt` manually triggers translation for metadata JSON responses.
- **Dictionary Loading**: `TranslationLoader.kt` handles lazy loading and reloading of Trie structures from `assets` and `dict` folder.
- **Cleanup**: Removed unused `DictDatabase` (SQLite) and associated Room entities.

## Manual Verification Steps
1.  **Launch the App**: Open the Legado app on your Android device or emulator.
2.  **Enable Translation**: Go to Settings and enable the translation feature.
3.  **Open a Book**: Select a book with Chinese content.
4.  **Check Translation**: Confirm the text is translated to Vietnamese.
5.  **Test Web Service**: Start Web Service, check bookshelf on PC browser.
6.  **Import Dictionary**: Import a custom `Names.txt` with a unique entry (e.g., "Tiêu Phong" -> "Super Phong").
7.  **Verify Import**: Check if the name updates in the book content.
8.  **Disable Translation**: Go back to Settings and disable it.
9.  **Verify Original Text**: Web Service and App should show raw Chinese text.
