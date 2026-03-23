# Bản đồ Tích hợp Chức năng Dịch Thuật (VietPhrase) trong Legado (Master Branch)

Phiên bản này là **bản đồ tra cứu cạn kiệt (exhaustive mapping)**. Mọi thứ liên quan đến chức năng dịch của dự án `legado` (từ lớp logic cốt lõi, assets tĩnh, tới các dòng mã rải rác trong tầng UI) đều được ghi nhận tại đây dựa trên mã nguồn gốc.

---

## 1. Lớp Cốt lõi & Quản lý (Core Logic & File Management)

Lớp này nằm ở `io.legado.app.utils` và `io.legado.app.model`, chịu trách nhiệm tải, lưu trữ bộ đệm và cung cấp hàm API dịch nội bộ.

### TranslateUtils.kt
**Đường dẫn:** `app/src/main/java/io/legado/app/utils/TranslateUtils.kt`

Trái tim của hệ thống dịch thuật (Dictionary-based).

| Hàm | Mô tả | Vị trí |
|-----|-------|--------|
| `isTranslateEnabled()` | Quản lý cờ đọc từ `PreferKey.translateEnable` | line 68-71 |
| `translateMeta(text: String?)` | Dịch Siêu dữ liệu (tên sách, tác giả...) | line 172-174 |
| `translateContent(text: String?)` | Dịch Văn bản truyện | line 179-181 |
| `translateCode(text: String?)` | Dịch Mã nguồn có cấu trúc (giữ syntax) | line 186-201 |
| `translateChapterTitle(raw: String)` | Pattern Regex dịch số Trung Quốc (第12章 → Chương 12) | line 77-117 |
| `translateView(...)` | Hàm chuyên dụng cho Adapter/UI Thread, dịch bất đồng bộ vào TextView | line 244-314 |
| `performTranslation()` | Hàm thuật toán cốt lõi: tách từ, quét mảng, khớp Trie | line 319-366 |
| `performCodeTranslation()` | Dịch cho code (bỏ qua format) | line 370-418 |
| `searchInDictionaries(key, data)` | Tìm trong từ điển (Names → VietPhrase) | line 420-430 |
| `tokenize(text, data)` | Tách từng từ sử dụng Trie | line 432-477 |
| `convertPunctuation(text)` | Chuyển dấu câu Trung → Việt | line 484-490 |
| `processText(input)` | Xử lý text sau dịch (capitalize, trim) | line 492-528 |
| `clearCache()` | Xóa cache dịch | line 542-545 |

### TranslationLoader.kt
**Đường dẫn:** `app/src/main/java/io/legado/app/model/TranslationLoader.kt`

Quản lý file I/O và nạp dữ liệu trên `Dispatchers.IO`.

| Hàm | Mô tả | Vị trí |
|-----|-------|--------|
| `loadTranslationData()` | Lazy load singleton - tải 3 từ điển | line 42-109 |
| `loadDictionary(...)` | Tải từ điển Trie (binary/text) | line 115-237 |
| `loadPhoneticDictionary(...)` | Tải từ điển phiên âm (HashMap) | line 242-302 |
| `prebuildAll()` | Pre-load tất cả từ điển khi khởi động | line 304-308 |
| `clearCache()` | Xóa cache | line 471-473 |

**Cơ chế Cache tối ưu:**
- Lần đầu: Đọc file Text ~30MB, đóng gói thành cây Trie, lưu `.dat` tại `appCtx.filesDir/translate/binary`
- Lần sau: Nạp file `.dat` siêu tốc

### DictManager.kt
**Đường dẫn:** `app/src/main/java/io/legado/app/utils/DictManager.kt`

Quản trị "Từ điển nhóm" (User Dictionary).

| Hàm | Mô tả |
|-----|-------|
| `getCustomDictDir()` | Lấy thư mục `files/translate/custom/` |
| `getCustomDictFile(type)` | Lấy file từ điển theo type |
| `importDict(context, uri, type)` | Import từ điển tùy chỉnh |
| `hasCustomDict(type)` | Kiểm tra từ điển tùy chỉnh có tồn tại |

**Enum DictType:**
- `NAMES` → Names.txt
- `VIETPHRASE` → VietPhrase.txt
- `PHIENAM` → ChinesePhienAmWords.txt

---

## 2. Dữ liệu & Cấu trúc Thuật toán (Data Structures & Assets)

### DoubleArrayTrie.kt
**Đường dẫn:** `app/src/main/java/io/legado/app/model/DoubleArrayTrie.kt`

Lõi máy trạng thái mảng đôi (Double-array DFA). Tìm kiếm O(1) để quét hàng chục nghìn đoạn văn liên tục không bị nghẽn.

| Hàm | Mô tả |
|-----|-------|
| `build(entries)` | Xây dựng Trie từ list key-value |
| `findLongestMatch(text, startIndex)` | Tìm từ dài nhất khớp với text |

### TranslationData.kt
**Đường dẫn:** `app/src/main/java/io/legado/app/model/TranslationData.kt`

Data class bao gồm 3 cây từ điển:

```kotlin
data class TranslationData(
    val names: DoubleArrayTrie,                    // Tên người, địa danh
    val vietPhrase: DoubleArrayTrie,             // Từ điển chính
    val chinesePhienAm: Map<String, String>       // Phiên âm
)
```

### Thư mục Asset gốc (Tĩnh) - VietPhrase Dictionary

**Legado-with-MD3 Branch (Mới nhất):**
Thư viện từ điển ở bản MD3 được tối ưu bằng cách đóng gói các mảng nhị phân (.bin) sẵn vào APK để tải siêu tốc, bên cạnh các file text dự phòng.

**Đường dẫn Pre-built Binary:** `app/src/main/assets/dict/`
| File | Mô tả |
|------|-------|
| `names.bin` | Binary Cache của tên người/địa danh |
| `vietphrase.bin` | Binary Cache của từ điển chính |
| `phienam.bin` | Binary Cache của từ điển phiên âm |

**Đường dẫn Text Fallback:** `app/src/main/assets/translate/vietphrase/`
| File | Mô tả |
|------|-------|
| `Names.txt` | Hỗn hợp đại từ, tên riêng (Dạng Text) |
| `VietPhrase.txt` | Từ điển Hán-Việt siêu to (Dạng Text) |
| `ChinesePhienAmWords.txt` | Từ Phiên âm chuẩn (Dạng Text) |

**Legado-qt Branch:**
Sử dụng đuôi `.dat` trong cùng thư mục `assets/translate/vietphrase/` cho cả binary và text.

| File | Mô tả |
|------|-------|
| `Names.dat` / `Names.txt` | Binary/Text Tên người, địa danh |
| `VietPhrase.dat` / `VietPhrase.txt` | Binary/Text Từ điển Hán-Việt |
| `ChinesePhienAmWords.txt` | File text phiên âm (Map) |

**Định dạng file Text:** UTF-8, mỗi dòng `key=value`, ngăn cách bằng `/` cho nhiều nghĩa.
**Định dạng file Binary:** Custom Java Serialization (Trie DFA / DataOutputStream HashMap). Tải vào RAM với độ trễ gần như bằng O.

---

## 3. AI Translation (Vietnamese to Chinese) - ONNX Models

Hệ thống dịch AI sử dụng MarianMT model với ONNX Runtime.

### AITranslationService.kt
**Đường dẫn:** `app/src/main/java/io/legado/app/service/AITranslationService.kt`

Service chạy nền cho dịch AI (Tiếng Việt → Tiếng Trung).

| Hàm/Thuộc tính | Mô tả |
|----------------|-------|
| `loadModel()` | Tải ONNX models |
| `translate(text)` | Dịch văn bản |
| `translationCache` | Cache kết quả (100 entries) |
| `isModelLoaded` | Trạng thái model |

### AiModelManager.kt
**Đường dẫn:** `app/src/main/java/io/legado/app/utils/AiModelManager.kt`

Quản lý download/cập nhật AI models.

| Hàm | Mô tả |
|------|-------|
| `getModelDir()` | Thư mục lưu model (`files/aimodel/`) |
| `isModelReady()` | Kiểm tra model đã tải |
| `downloadModels()` | Download tất cả models |
| `deleteModels()` | Xóa models |
| `MODEL_FILES` | Danh sách files cần tải |

**Model Files (ONNX):**
- `encoder_model.onnx`
- `decoder_model.onnx`
- `decoder_with_past_model.onnx`

### Thư mục AI Model Assets
**Đường dẫn:** `app/src/main/assets/aimodel/`

| File | Mô tả |
|------|-------|
| `encoder_model.onnx` | Encoder model |
| `decoder_model.onnx` | Decoder model |
| `decoder_with_past_model.onnx` | Decoder with past |
| `source.spm` | Vietnamese tokenizer |
| `target.spm` | Chinese tokenizer |
| `vocab.json` | Vocabulary |
| `config.json` | Model config |
| `tokenizer_config.json` | Tokenizer config |
| `special_tokens_map.json` | Special tokens |
| `generation_config.json` | Generation config |

---

## 4. Giao diện Cấu hình & Menu XML (Settings & UI XML)

### Preferences - Toggle Bật/Tắt
- **Khai báo hằng số:** `io.legado.app.constant.PreferKey.translateEnable` (line 172)
- **AI Model Toggle:** `io.legado.app.constant.PreferKey.aiModelEnabled` (line 173)
- **Màn hình cấu hình XML:** `app/src/main/res/xml/pref_main.xml`

**pref_main.xml:**
```xml
<!-- line 37-39 -->
<SwitchPreference
    android:key="translateEnable"
    android:summary="@string/translate_enable_summary"
    android:title="@string/translate_setting" />

<!-- line 44-48 -->
<Preference
    android:icon="@drawable/ic_translate"
    app:dependency="translateEnable" />
```

### Menu XML Files

| File | Mô tả | ID Menu |
|------|-------|---------|
| `res/menu/rss_read.xml` | Menu đọc RSS | `@+id/menu_translate_page` |
| `res/menu/web_view.xml` | Menu WebView | `@+id/menu_translate_page` |
| `res/menu/book_search.xml` | Menu tìm kiếm sách | `@+id/menu_ai_translation` |
| `res/menu/book_read.xml` | Menu đọc sách | `@drawable/ic_translate` icon |

### String Resources
**File:** `app/src/main/res/values/strings.xml`

| Key | Giá trị |
|-----|---------|
| `translate` | "Translate" |
| `translate_page` | "Translate page" |
| `translate_setting` | "Translation" |
| `translate_enable` | "Enable QT Translation" |
| `translate_enable_summary` | "Translate Chinese book metadata and content to Vietnamese" |
| `ai_translation` | "AI Translation (Vietnamese to Chinese)" |

**File:** `app/src/main/res/values-vi/strings.xml`

| Key | Giá trị |
|-----|---------|
| `translate` | "Dịch thuật" |
| `translate_enable` | "Bật Dịch QT" |
| `translate_enable_summary` | "Dịch siêu dữ liệu và nội dung sách tiếng Trung sang tiếng Việt" |
| `translate_setting` | "Cài đặt Dịch thuật" |

### MainActivity / MyFragment
- **File:** `app/src/main/java/io/legado/app/ui/main/MainActivity.kt` (Observer `PreferKey.translateEnable`)
- **File:** `app/src/main/java/io/legado/app/ui/main/my/MyFragment.kt`

---

## 5. Hook Hiển thị - Các điểm can thiệp tự động (Integration Flow)

### 5.1. Chức năng Đọc truyện & Xử lí văn bản (Reader Engine)

| File | Hàm gọi | Mô tả |
|------|---------|-------|
| `ContentProcessor.kt` | `TranslateUtils.translateContent()` | Can thiệp vào Byte/String Text |
| `ReadBook.kt` | `TranslateUtils.translateChapterTitle()` | Dịch tên chương tiếp/lùi |
| `ReadMenu.kt` | `TranslateUtils.translateMeta()` | Dịch metadata trong menu đọc |
| `ChapterListAdapter.kt` | `TranslateUtils.translateChapterTitle()` | Dịch Mục lục truyện |
| `ReadBookActivity.kt` | - | Activity đọc sách chính |

### 5.2. Chức năng Tìm kiếm & Giao diện sách (Book Discover Engine)

| File | Hàm gọi | Mô tả |
|------|---------|-------|
| `BookInfoActivity.kt` | `TranslateUtils.translateMeta()` | Dịch Intro, Author, Kind |
| `SearchAdapter.kt` | `TranslateUtils.translateMeta()` | Dịch kết quả tìm kiếm |
| `SearchContentViewModel.kt` | `TranslateUtils.translateChapterTitle()` | Dịch tiêu đề chương |
| `ExploreShowActivity.kt` | `TranslateUtils.translateMeta()` | Dịch tiêu đề khám phá |
| `ExploreShowAdapter.kt` | `TranslateUtils.translateMeta()` | Dịch item khám phá |
| `SearchActivity.kt` | `AiModelManager`, `PreferKey.aiModelEnabled` | AI Translation trong tìm kiếm |

### 5.3. Cấu hình Nguồn & Mã nguồn tuỳ chỉnh (Source Manager & JS Engine)

| File | Hàm gọi | Mô tả |
|------|---------|-------|
| `JsExtensions.kt` | `TranslateUtils.translateContent()` | JS Extension cho nguồn |
| `ImportBookSourceViewModel.kt` | `TranslateUtils.translateMeta()`, `translateCode()` | Dịch khi import nguồn sách |
| `ImportRssSourceViewModel.kt` | `TranslateUtils.translateMeta()`, `translateCode()` | Dịch khi import RSS |
| `SourceLoginDialog.kt` | `TranslateUtils.translateView()` | Dịch form đăng nhập |
| `MainViewModel.kt` | `TranslateUtils.translateMeta()`, `translateCode()` | Dịch nguồn trong Main |
| `RssSourceEditActivity.kt` | `TranslateUtils.translateContent()` | Dịch RSS source |
| `RssSortActivity.kt` | `TranslateUtils.translateContent()` | Dịch RSS article |
| `RssSourceAdapter.kt` | `TranslateUtils.translateView()` | Dịch adapter RSS |

### 5.4. Web Server & Cổng giao tiếp mạng (Web Server Interface)

| File | Hàm gọi | Mô tả |
|------|---------|-------|
| `BookSearchWebSocket.kt` | `TranslateUtils.translateMeta()` | Dịch qua WebSocket |
| `BookController.kt` | `TranslateUtils.translateMeta()`, `translateChapterTitle()` | API bookshelf/chapter |
| `ContentProcessor.kt` | `TranslateUtils.translateContent()` | Xử lý nội dung API |

### 5.5. RSS & Các tính năng khác

| File | Hàm gọi | Mô tả |
|------|---------|-------|
| `ReadRssActivity.kt` | `TranslateUtils.translateCode()` | Dịch RSS feed |
| `WebViewActivity.kt` | `TranslateUtils.translateCode()` | Dịch trong WebView |
| `ExportBookService.kt` | - | Dịch khi xuất sách |
| `LocalBook.kt` | - | Dịch sách local |

### 5.6. Quản lý Từ điển & AI Models

| File | Hàm gọi | Mô tả |
|------|---------|-------|
| `DictManageActivity.kt` | `TranslateUtils.clearCache()`, `AiModelManager` | UI quản lý từ điển & AI models |

---

## 6. Extensions & Utils

### TextViewExtensions.kt
**Đường dẫn:** `app/src/main/java/io/legado/app/utils/TextViewExtensions.kt`

Extension function cho phép gọi dịch trực tiếp trên TextView:

```kotlin
fun TextView.setTranslatedText(
    text: String?,
    transform: ((String) -> CharSequence)? = null
) = TranslateUtils.translateView(this, text, transform)
```

---

## 7. Web Module (Vue/TypeScript)

### API Calls
**Đường dẫn:** `modules/web/src/api/api.ts`

```typescript
const getBookShelf = (isTranslate?: boolean) => 
  ajax.get('getBookshelf' + (isTranslate ? '?translate=true' : ''))

const getChapterList = (bookUrl, isTranslate) => 
  ajax.get('getChapterList?url=' + encodeURIComponent(bookUrl) + 
    (isTranslate ? '&translate=true' : ''))

const getBookContent = (bookUrl, chapterIndex, isTranslate) => 
  ajax.get('getBookContent?url=' + bookUrl + '&index=' + chapterIndex + 
    (isTranslate ? '&translate=true' : ''))
```

### Store - Quản lý Trạng thái
**Đường dẫn:** `modules/web/src/store/bookStore.ts`

```typescript
state: {
  isTranslateMode: false  // line 46
}

actions: {
  toggleTranslateMode()
  loadBookShelf()
  loadWebCatalog()
}
```

### Components & Views

| File | Mô tả |
|------|-------|
| `TranslateToggle.vue` | UI toggle dịch (CN/VN) |
| `BookShelf.vue` | Hiển thị shelf với dịch |
| `BookChapter.vue` | Đọc chương với dịch |
| `ReadSettings.vue` | Cài đặt đọc sách với tùy chọn dịch |

---

## 8. Thư Viện Sử Dụng

| Thư viện | Mục đích |
|----------|----------|
| `kotlinx.coroutines` | Async translation |
| `splitties.init.appCtx` | Context trong object |
| `androidx.collection.LruCache` | Cache kết quả dịch (10MB) |
| `java.util.regex.Pattern` | Regex xử lý text |
| `ai.onnxruntime:onnxruntime-android` | AI model inference |
| `okhttp3` | Download AI models |

---

## 9. Cache Strategy

### VietPhrase Translation
- **Translation Cache:** LruCache 10MB (`TranslateUtils.kt` line 19)
- **Trie Binary Cache:** 
  - `filesDir/translate/binary/*.bin` (legado-with-MD3)
  - `filesDir/translate/binary/*.dat` (legado-qt)
- **Phonetic Binary Cache:** `filesDir/translate/binary/*.bin`

**Cache key format:**
- Meta: `translate|vietphrase|v2|meta|{md5}`
- Content: `translate|vietphrase|v2|content|{md5}`
- Code: `translate|vietphrase|v2|code|{md5}`

### AI Translation
- **Translation Cache:** LinkedHashMap (100 entries) trong `AITranslationService`
- **Model Files:** `filesDir/aimodel/*.onnx`

---

## 10. Vị Trí Tương Đối Quan Trọng

| Thành phần | Đường dẫn | Số dòng |
|------------|-----------|---------|
| Core dịch (VietPhrase) | `app/src/main/java/io/legado/app/utils/TranslateUtils.kt` | 554 |
| Loader từ điển | `app/src/main/java/io/legado/app/model/TranslationLoader.kt` | 511 |
| Trie implementation | `app/src/main/java/io/legado/app/model/DoubleArrayTrie.kt` | 434 |
| AI Translation Service | `app/src/main/java/io/legado/app/service/AITranslationService.kt` | 458 |
| AI Model Manager | `app/src/main/java/io/legado/app/utils/AiModelManager.kt` | 163 |
| API Controller | `app/src/main/java/io/legado/app/api/controller/BookController.kt` | 363 |
| Web store | `modules/web/src/store/bookStore.ts` | 219 |
| Web toggle | `modules/web/src/components/TranslateToggle.vue` | 64 |
| UI tiếng Việt | `app/src/main/res/values-vi/strings.xml` | - |

---

## 11. Luồng Xử Lý Dịch (Tổng quan)

### VietPhrase Translation (Trung → Việt)
```
User bật toggle (Web/App)
         ↓
Kiểm tra isTranslateEnabled() [PreferKey.translateEnable]
         ↓
Gọi API với ?translate=true
         ↓
BookController.getBookshelf/ChapterList/Content
         ↓
TranslateUtils.translateMeta() hoặc translateContent()
         ↓
TranslationLoader.loadTranslationData() 
   (Ưu tiên load file nhị phân .bin/.dat, sau đó đến custom dict, cuối cùng dự phòng bằng file .txt)
         ↓
tokenize() → searchInDictionaries() → processText()
         ↓
Kết quả + Cache (LRU Cache 10MB)
```

### AI Translation (Việt → Trung)
```
User bật AI Translation trong Search
         ↓
Kiểm tra PreferKey.aiModelEnabled
         ↓
AITranslationService.loadModel() (nếu chưa load)
         ↓
SearchActivity.onQueryTextChange()
         ↓
AITranslationService.translate(query)
         ↓
ONNX Encoder → Decoder inference
         ↓
Trả về kết quả Chinese
         ↓
Search với query đã dịch
```

---

## 12. Danh sách đầy đủ các File liên quan

### Core Files (Utils & Model) - VietPhrase
- `app/src/main/java/io/legado/app/utils/TranslateUtils.kt` - Trái tim hệ thống dịch
- `app/src/main/java/io/legado/app/utils/DictManager.kt` - Quản lý từ điển tùy chỉnh
- `app/src/main/java/io/legado/app/model/TranslationLoader.kt` - Tải dữ liệu từ điển
- `app/src/main/java/io/legado/app/model/TranslationData.kt` - Data class
- `app/src/main/java/io/legado/app/model/DoubleArrayTrie.kt` - Cấu trúc Trie
- `app/src/main/java/io/legado/app/model/DoubleArrayTrieTest.kt` - Unit test

### AI Translation Files
- `app/src/main/java/io/legado/app/utils/AiModelManager.kt` - Quản lý AI models
- `app/src/main/java/io/legado/app/service/AITranslationService.kt` - Service dịch AI

### API & Controllers
- `app/src/main/java/io/legado/app/api/controller/BookController.kt` - API bookshelf/chapter
- `app/src/main/java/io/legado/app/web/socket/BookSearchWebSocket.kt` - WebSocket translation
- `app/src/main/java/io/legado/app/help/book/ContentProcessor.kt` - Xử lý nội dung
- `app/src/main/java/io/legado/app/help/JsExtensions.kt` - JS Extensions

### UI & Activities
- `app/src/main/java/io/legado/app/ui/main/MainActivity.kt` - Main activity
- `app/src/main/java/io/legado/app/ui/main/MainViewModel.kt` - Main ViewModel
- `app/src/main/java/io/legado/app/ui/main/my/MyFragment.kt` - Account fragment
- `app/src/main/java/io/legado/app/ui/dict/manage/DictManageActivity.kt` - Quản lý từ điển
- `app/src/main/java/io/legado/app/ui/rss/read/ReadRssActivity.kt` - RSS reader
- `app/src/main/java/io/legado/app/ui/browser/WebViewActivity.kt` - WebView
- `app/src/main/java/io/legado/app/ui/book/read/ReadBookActivity.kt` - Đọc sách
- `app/src/main/java/io/legado/app/ui/book/read/ReadMenu.kt` - Menu đọc sách
- `app/src/main/java/io/legado/app/ui/book/toc/ChapterListAdapter.kt` - Adapter chương
- `app/src/main/java/io/legado/app/ui/book/info/BookInfoActivity.kt` - Thông tin sách
- `app/src/main/java/io/legado/app/ui/book/search/SearchActivity.kt` - Tìm kiếm (AI)
- `app/src/main/java/io/legado/app/ui/book/search/SearchAdapter.kt` - Adapter tìm kiếm
- `app/src/main/java/io/legado/app/ui/book/searchContent/SearchContentViewModel.kt` - VM tìm kiếm
- `app/src/main/java/io/legado/app/ui/book/explore/ExploreShowActivity.kt` - Khám phá
- `app/src/main/java/io/legado/app/ui/book/explore/ExploreShowAdapter.kt` - Adapter khám phá
- `app/src/main/java/io/legado/app/ui/login/SourceLoginDialog.kt` - Login dialog
- `app/src/main/java/io/legado/app/ui/association/ImportBookSourceViewModel.kt` - Import source
- `app/src/main/java/io/legado/app/ui/association/ImportRssSourceViewModel.kt` - Import RSS
- `app/src/main/java/io/legado/app/ui/rss/source/edit/RssSourceEditActivity.kt` - Edit RSS
- `app/src/main/java/io/legado/app/ui/rss/article/RssSortActivity.kt` - RSS sort
- `app/src/main/java/io/legado/app/ui/rss/source/manage/RssSourceAdapter.kt` - RSS adapter
- `app/src/main/java/io/legado/app/ui/main/bookshelf/style1/books/BooksFragment.kt` - Shelf fragment
- `app/src/main/java/io/legado/app/model/ReadBook.kt` - Model đọc sách

### Preferences & Config
- `app/src/main/java/io/legado/app/constant/PreferKey.kt` - Key cấu hình
- `app/src/main/res/xml/pref_main.xml` - Menu preferences
- `app/src/main/res/menu/rss_read.xml` - Menu RSS
- `app/src/main/res/menu/web_view.xml` - Menu WebView
- `app/src/main/res/menu/book_search.xml` - Menu tìm kiếm (AI translation)
- `app/src/main/res/menu/book_read.xml` - Menu đọc sách

### Assets & Resources
- `app/src/main/assets/dict/*.bin` - Các file từ điển nhị phân build sẵn (legado-with-MD3)
- `app/src/main/assets/translate/vietphrase/*.dat` - Các file từ điển nhị phân build sẵn (legado-qt)
- `app/src/main/assets/translate/vietphrase/VietPhrase.txt` - Từ điển chính (Text Fallback)
- `app/src/main/assets/translate/vietphrase/Names.txt` - Tên người, địa danh (Text Fallback)
- `app/src/main/assets/translate/vietphrase/ChinesePhienAmWords.txt` - Phiên âm (Text Fallback)
- `app/src/main/assets/aimodel/` - AI models (ONNX)
- `app/src/main/res/values-vi/strings.xml` - Strings tiếng Việt
- `app/src/main/res/values/strings.xml` - Strings mặc định
- `app/src/main/res/values/ids.xml` - IDs (tag_translate_key)
- `app/src/main/res/drawable/ic_translate.xml` - Icon dịch

### Web Module
- `modules/web/src/store/bookStore.ts` - Pinia store
- `modules/web/src/api/api.ts` - API calls
- `modules/web/src/components/TranslateToggle.vue` - Toggle component
- `modules/web/src/views/BookShelf.vue` - Bookshelf view
- `modules/web/src/views/BookChapter.vue` - Chapter view
- `modules/web/src/components/ReadSettings.vue` - Reading settings

### Documentation
- `AI_MODEL_INTEGRATION.md` - Tài liệu tích hợp AI
- `translation_architecture.md` - Tài liệu kiến trúc dịch
