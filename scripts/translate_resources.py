import os
import re
import json
import time
import urllib.request
import urllib.parse
from concurrent.futures import ThreadPoolExecutor

VALUES_DIR = '../app/src/main/res/values'
VALUES_VI_DIR = '../app/src/main/res/values-vi'
VALUES_EN_DIR = '../app/src/main/res/values-en'

STRINGS_FILE = 'strings.xml'
ARRAYS_FILE = 'arrays.xml'

def translate_text(text, target_lang):
    if not text.strip():
        return text
    # Avoid translating placeholders like %1$s, %d, @string/xxx etc if not mixed with text, but actually we just pass it
    # We can handle format specifiers partially, but let's try direct translate first
    try:
        url = 'https://translate.googleapis.com/translate_a/single?client=gtx&sl=auto&tl=' + target_lang + '&dt=t&q=' + urllib.parse.quote(text)
        req = urllib.request.Request(url, headers={'User-Agent': 'Mozilla/5.0'})
        res = urllib.request.urlopen(req, timeout=10)
        data = json.loads(res.read().decode('utf-8'))
        result = ''.join([part[0] for part in data[0] if part[0]])
        # Fix some common mangling of Android string placeholders
        result = re.sub(r'%\s+(\d+)\s+\$\s+s', r'%\1$s', result)
        result = re.sub(r'%\s+(\d+)\s+\$\s+d', r'%\1$d', result)
        result = result.replace('...', '…')
        return result
    except Exception as e:
        print(f"Failed to translate '{text}': {e}")
        return text

def has_chinese(text):
    return bool(re.search(r'[\u4e00-\u9fff]', text))

def map_languages_array(tag_name, items, lang):
    # Specialized translation for 'language' array entries to ensure exact matching
    if lang == 'vi':
        return ['Theo hệ thống', 'Tiếng Anh', 'Tiếng Việt']
    elif lang == 'en':
        return ['Auto', 'English', 'Vietnamese']
    return items

def process_file(source_path, vi_path, en_path, is_array=False):
    with open(source_path, 'r', encoding='utf-8') as f:
        source_content = f.read()
    
    vi_dict = {}
    if os.path.exists(vi_path):
        with open(vi_path, 'r', encoding='utf-8') as f:
            if is_array:
                # Basic array parser for vi_path
                array_matches = re.finditer(r'<string-array\s+name="([^"]+)"[^>]*>(.*?)</string-array>', f.read(), re.DOTALL)
                for m in array_matches:
                    items = re.findall(r'<item[^>]*>(.*?)</item>', m.group(2))
                    vi_dict[m.group(1)] = items
            else:
                for match in re.finditer(r'<string\s+name="([^"]+)"[^>]*>(.*?)</string>', f.read()):
                    vi_dict[match.group(1)] = match.group(2)
    
    # We will do concurrent translation
    tasks_vi = []
    tasks_en = []
    
    if is_array:
        array_pattern = r'(<string-array\s+name="([^"]+)"[^>]*>\s*)(.*?)(\s*</string-array>)'
        
        matches = list(re.finditer(array_pattern, source_content, re.DOTALL))
        for m in matches:
            name = m.group(2)
            content = m.group(3)
            # Find all items
            items_matches = list(re.finditer(r'(<item>)(.*?)(</item>)', content))
            if name == "language":
                continue # Handled explicitly later
            
            for im in items_matches:
                orig_text = im.group(2)
                # target for vi
                if name in vi_dict and len(vi_dict[name]) > len(tasks_vi):
                    # We have it in vi_dict? Array indexing is hard. Better extract by order.
                    pass
                if has_chinese(orig_text) and not orig_text.startswith("@string/"):
                    tasks_vi.append(orig_text)
                
                if not orig_text.startswith("@string/") and orig_text.strip() != "":
                    tasks_en.append(orig_text)
                    
    else:
        for match in re.finditer(r'<string\s+name="([^"]+)"[^>]*>(.*?)</string>', source_content):
            name = match.group(1)
            orig_text = match.group(2)
            
            # vi needs translation if missing or has chinese
            vi_text = vi_dict.get(name)
            if (vi_text is None or has_chinese(vi_text)) and has_chinese(orig_text) and not orig_text.startswith("@string/"):
                tasks_vi.append((name, orig_text))
                
            # en needs translation if it's translatable
            if not orig_text.startswith("@string/") and orig_text.strip() != "" and name != "version_name":
                tasks_en.append((name, orig_text))
                
    # Dedup and submit
    vi_to_translate = {text: "" for name, text in tasks_vi if not isinstance(tasks_vi[0], str)} if not is_array else {t: "" for t in tasks_vi}
    en_to_translate = {text: "" for name, text in tasks_en if not isinstance(tasks_en[0], str)} if not is_array else {t: "" for t in tasks_en}
    
    print(f"Translating {len(vi_to_translate)} items to vi...")
    print(f"Translating {len(en_to_translate)} items to en...")

    with ThreadPoolExecutor(max_workers=20) as executor:
        futures_vi = {executor.submit(translate_text, t, 'vi'): t for t in vi_to_translate.keys()}
        futures_en = {executor.submit(translate_text, t, 'en'): t for t in en_to_translate.keys()}
        
        for f in futures_vi:
            vi_to_translate[futures_vi[f]] = f.result()
            
        for f in futures_en:
            en_to_translate[futures_en[f]] = f.result()

    print("Translation done.")
    
    # Now build new content
    vi_content = source_content
    en_content = source_content

    if is_array:
        # Complex to reconstruct arrays line by line, let's just do text replace for vi_content
        for orig, translated in vi_to_translate.items():
            vi_content = vi_content.replace(f"<item>{orig}</item>", f"<item>{translated}</item>")
        for orig, translated in en_to_translate.items():
            en_content = en_content.replace(f"<item>{orig}</item>", f"<item>{translated}</item>")
            
        # Hardcode Language Array Replace
        lang_pattern_src = r'<string-array name="language">.*?</string-array>'
        vi_lang_str = '<string-array name="language">\n        <item>Theo hệ thống</item>\n        <item>Tiếng Anh</item>\n        <item>Tiếng Việt</item>\n    </string-array>'
        en_lang_str = '<string-array name="language">\n        <item>Auto</item>\n        <item>English</item>\n        <item>Vietnamese</item>\n    </string-array>'
        
        vi_content = re.sub(lang_pattern_src, vi_lang_str, vi_content, flags=re.DOTALL)
        en_content = re.sub(lang_pattern_src, en_lang_str, en_content, flags=re.DOTALL)
    else:
        # String replacement line by line to keep formatting
        vi_lines = []
        en_lines = []
        for line in source_content.split('\n'):
            m = re.search(r'(<string\s+name="([^"]+)"[^>]*>)(.*?)(</string>)', line)
            if m:
                prefix, name, orig, suffix = m.groups()
                
                # VI
                vi_t = vi_dict.get(name, orig)
                if has_chinese(vi_t) and orig in vi_to_translate:
                    vi_t = vi_to_translate[orig]
                vi_lines.append(line.replace(prefix + orig + suffix, prefix + vi_t + suffix))
                
                # EN
                en_t = orig
                if name != "version_name" and orig in en_to_translate:
                    en_t = en_to_translate[orig]
                en_lines.append(line.replace(prefix + orig + suffix, prefix + en_t + suffix))
            else:
                vi_lines.append(line)
                en_lines.append(line)
                
        vi_content = '\n'.join(vi_lines)
        en_content = '\n'.join(en_lines)
        
    os.makedirs(os.path.dirname(vi_path), exist_ok=True)
    os.makedirs(os.path.dirname(en_path), exist_ok=True)
    
    with open(vi_path, 'w', encoding='utf-8') as f:
        f.write(vi_content)
    with open(en_path, 'w', encoding='utf-8') as f:
        f.write(en_content)
        
    # Also overwrite the original default values with vi_content so it's Vietnamese!
    with open(source_path, 'w', encoding='utf-8') as f:
        f.write(vi_content)

if __name__ == '__main__':
    print("Processing strings.xml...")
    process_file(os.path.join(VALUES_DIR, STRINGS_FILE), os.path.join(VALUES_VI_DIR, STRINGS_FILE), os.path.join(VALUES_EN_DIR, STRINGS_FILE), False)
    print("Processing arrays.xml...")
    process_file(os.path.join(VALUES_DIR, ARRAYS_FILE), os.path.join(VALUES_VI_DIR, ARRAYS_FILE), os.path.join(VALUES_EN_DIR, ARRAYS_FILE), True)
    
    # Update array_values.xml
    av_path = os.path.join(VALUES_DIR, 'array_values.xml')
    with open(av_path, 'r', encoding='utf-8') as f:
        av_content = f.read()
    
    av_lang_pattern = r'(<string-array name="language_value">.*?)</string-array>'
    av_lang_new = '<string-array name="language_value">\n        <item>auto</item>\n        <item>en</item>\n        <item>vi</item>\n    </string-array>'
    av_content = re.sub(av_lang_pattern, av_lang_new, av_content, flags=re.DOTALL)
    
    with open(av_path, 'w', encoding='utf-8') as f:
        f.write(av_content)
    
    print("Done!")
