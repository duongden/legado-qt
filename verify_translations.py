import os, re

files_to_check = [
    "app/src/main/java/io/legado/app/help/book/ContentProcessor.kt",
    "app/src/main/java/io/legado/app/model/ReadBook.kt",
    "app/src/main/java/io/legado/app/ui/book/read/ReadMenu.kt",
    "app/src/main/java/io/legado/app/ui/book/toc/ChapterListAdapter.kt",
    "app/src/main/java/io/legado/app/ui/book/info/BookInfoActivity.kt",
    "app/src/main/java/io/legado/app/ui/book/search/SearchAdapter.kt",
    "app/src/main/java/io/legado/app/ui/book/searchContent/SearchContentViewModel.kt",
    "app/src/main/java/io/legado/app/ui/book/explore/ExploreShowActivity.kt",
    "app/src/main/java/io/legado/app/ui/book/explore/ExploreShowAdapter.kt",
    "app/src/main/java/io/legado/app/help/JsExtensions.kt",
    "app/src/main/java/io/legado/app/ui/association/ImportBookSourceViewModel.kt",
    "app/src/main/java/io/legado/app/ui/association/ImportRssSourceViewModel.kt",
    "app/src/main/java/io/legado/app/ui/login/SourceLoginDialog.kt",
    "app/src/main/java/io/legado/app/ui/main/MainViewModel.kt",
    "app/src/main/java/io/legado/app/ui/rss/source/edit/RssSourceEditActivity.kt",
    "app/src/main/java/io/legado/app/ui/rss/article/RssSortActivity.kt",
    "app/src/main/java/io/legado/app/ui/rss/source/manage/RssSourceAdapter.kt",
    "app/src/main/java/io/legado/app/web/socket/BookSearchWebSocket.kt",
    "app/src/main/java/io/legado/app/api/controller/BookController.kt",
    "app/src/main/java/io/legado/app/ui/rss/read/ReadRssActivity.kt",
    "app/src/main/java/io/legado/app/ui/browser/WebViewActivity.kt"
]

missing = []
for f in files_to_check:
    if os.path.exists(f):
        content = open(f, 'r').read()
        if "TranslateUtils" not in content and "setTranslatedText" not in content and "translateView" not in content:
            missing.append(f)
    else:
        print(f"File not found: {f}")

print("\n--- RESULTS ---")
if missing:
    print("Files MISSING translation code:")
    for m in missing:
        print(m)
else:
    print("All files contain translation references!")
