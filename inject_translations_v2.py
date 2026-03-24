import os, re

files = [
    "app/src/main/java/io/legado/app/ui/book/search/SearchAdapter.kt",
    "app/src/main/java/io/legado/app/ui/book/toc/ChapterListAdapter.kt",
    "app/src/main/java/io/legado/app/ui/book/info/BookInfoActivity.kt",
    "app/src/main/java/io/legado/app/ui/book/explore/ExploreShowActivity.kt",
    "app/src/main/java/io/legado/app/ui/book/explore/ExploreShowAdapter.kt",
    "app/src/main/java/io/legado/app/ui/rss/source/manage/RssSourceAdapter.kt"
]

for f in files:
    if not os.path.exists(f): continue
    with open(f, 'r') as file:
        content = file.read()
        
    if "setTranslatedText" not in content:
        # Avoid duplicate imports
        if "import io.legado.app.utils.setTranslatedText" not in content:
            content = content.replace("package ", "package \n\nimport io.legado.app.utils.setTranslatedText\n", 1)
            
        # tvName
        content = re.sub(r'tvName\.text\s*=\s*(.+)', r'tvName.setTranslatedText(\1)', content)
        # tvAuthor
        content = re.sub(r'tvAuthor\.text\s*=\s*(.+)', r'tvAuthor.setTranslatedText(\1)', content)
        # tvIntroduce
        content = re.sub(r'tvIntroduce\.text\s*=\s*(.+)', r'tvIntroduce.setTranslatedText(\1)', content)
        # tvLasted
        content = re.sub(r'tvLasted\.text\s*=\s*(.+)', r'tvLasted.setTranslatedText(\1)', content)
        # tvKind
        content = re.sub(r'tvKind\.text\s*=\s*(.+)', r'tvKind.setTranslatedText(\1)', content)
        # tvTitle
        content = re.sub(r'tvTitle\.text\s*=\s*(.+)', r'tvTitle.setTranslatedText(\1)', content)
        # title
        content = re.sub(r'binding\.title\.text\s*=\s*(.+)', r'binding.title.setTranslatedText(\1)', content)
        
        with open(f, 'w') as file:
            file.write(content)

print("Injected UI setter strings.")
