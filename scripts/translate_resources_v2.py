import os
import re
import json
import urllib.request
import urllib.parse
from concurrent.futures import ThreadPoolExecutor

VALUES_DIR = '../app/src/main/res/values'
VALUES_VI_DIR = '../app/src/main/res/values-vi'
VALUES_EN_DIR = '../app/src/main/res/values-en'

def translate_text(text, target_lang):
    if not text.strip() or text.startswith('@string/'):
        return text
    try:
        url = 'https://translate.googleapis.com/translate_a/single?client=gtx&sl=auto&tl=' + target_lang + '&dt=t&q=' + urllib.parse.quote(text)
        req = urllib.request.Request(url, headers={'User-Agent': 'Mozilla/5.0'})
        res = urllib.request.urlopen(req, timeout=10)
        data = json.loads(res.read().decode('utf-8'))
        result = ''.join([part[0] for part in data[0] if part[0]])
        # Fix basic format specifiers
        result = re.sub(r'%\s+(\d+)\s+\$\s+s', r'%\1$s', result)
        result = re.sub(r'%\s+(\d+)\s+\$\s+d', r'%\1$d', result)
        result = result.replace('...', '…')
        return result
    except Exception as e:
        print(f"Failed to translate '{text}': {e}")
        return text

def parse_arrays(content):
    arrays = {}
    for match in re.finditer(r'<string-array\s+name="([^"]+)"[^>]*>(.*?)</string-array>', content, re.DOTALL):
        name = match.group(1)
        items = re.findall(r'<item[^>]*>(.*?)</item>', match.group(2))
        arrays[name] = items
    return arrays

def process_arrays():
    with open(f'{VALUES_DIR}/arrays.xml', 'r', encoding='utf-8') as f:
        src_content = f.read()
    with open(f'{VALUES_VI_DIR}/arrays.xml', 'r', encoding='utf-8') as f:
        vi_content = f.read()
        
    src_arrays = parse_arrays(src_content)
    vi_arrays = parse_arrays(vi_content)
    
    tasks_vi = []
    tasks_en = []
    
    for name, src_items in src_arrays.items():
        if name == "language": continue
        vi_items = vi_arrays.get(name, [])
        for i, item in enumerate(src_items):
            if i >= len(vi_items) and not item.startswith('@string/'):
                tasks_vi.append(item)
            if not item.startswith('@string/'):
                tasks_en.append(item)
                
    tasks_vi = list(set(tasks_vi))
    tasks_en = list(set(tasks_en))
    
    vi_translated = {}
    en_translated = {}
    
    print(f"Translating {len(tasks_vi)} array items to vi...")
    with ThreadPoolExecutor(max_workers=20) as executor:
        f_vi = {executor.submit(translate_text, t, 'vi'): t for t in tasks_vi}
        f_en = {executor.submit(translate_text, t, 'en'): t for t in tasks_en}
        for f in f_vi: vi_translated[f_vi[f]] = f.result()
        for f in f_en: en_translated[f_en[f]] = f.result()

    # Reconstruct vi and en array xml
    out_vi_lines = []
    out_en_lines = []
    current_array = None
    
    for line in src_content.split('\n'):
        m_arr = re.search(r'<string-array\s+name="([^"]+)"', line)
        m_item = re.search(r'(<item[^>]*>)(.*?)(</item>)', line)
        
        if m_arr:
            current_array = m_arr.group(1)
            current_idx = 0
            if current_array == 'language':
                out_vi_lines.append(line)
                out_en_lines.append(line)
                continue
                
        if m_item and current_array != 'language':
            orig = m_item.group(2)
            # VI
            vi_items = vi_arrays.get(current_array, [])
            if current_idx < len(vi_items):
                vi_t = vi_items[current_idx]
            else:
                vi_t = vi_translated.get(orig, orig)
            out_vi_lines.append(line.replace(m_item.group(0), f"{m_item.group(1)}{vi_t}{m_item.group(3)}"))
            
            # EN
            en_t = en_translated.get(orig, orig)
            out_en_lines.append(line.replace(m_item.group(0), f"{m_item.group(1)}{en_t}{m_item.group(3)}"))
            current_idx += 1
        else:
            if current_array == 'language' and '</string-array>' in line:
                current_array = None
            if current_array == 'language' and m_item:
                pass # skip items of language array as we replace the block
            elif current_array == 'language':
                pass # skip
            else:
                out_vi_lines.append(line)
                out_en_lines.append(line)
                
    # Insert language arrays
    vi_lang = '    <string-array name="language">\n        <item>Theo hệ thống</item>\n        <item>Tiếng Anh</item>\n        <item>Tiếng Việt</item>\n    </string-array>'
    en_lang = '    <string-array name="language">\n        <item>Auto</item>\n        <item>English</item>\n        <item>Vietnamese</item>\n    </string-array>'
    
    def repl_lang(lines, lang_str):
        j = '\n'.join(lines)
        j = re.sub(r'<string-array name="language">.*?</string-array>', lang_str, j, flags=re.DOTALL)
        return j

    vi_res = repl_lang(out_vi_lines, vi_lang)
    en_res = repl_lang(out_en_lines, en_lang)
    
    with open(f'{VALUES_VI_DIR}/arrays.xml', 'w', encoding='utf-8') as f:
        f.write(vi_res)
    with open(f'{VALUES_EN_DIR}/arrays.xml', 'w', encoding='utf-8') as f:
        f.write(en_res)
    with open(f'{VALUES_DIR}/arrays.xml', 'w', encoding='utf-8') as f:
        f.write(vi_res)

def process_strings():
    with open(f'{VALUES_DIR}/strings.xml', 'r', encoding='utf-8') as f:
        src_content = f.read()
    with open(f'{VALUES_VI_DIR}/strings.xml', 'r', encoding='utf-8') as f:
        vi_content = f.read()
        
    vi_dict = {}
    for match in re.finditer(r'<string\s+name="([^"]+)"[^>]*>(.*?)</string>', vi_content):
        vi_dict[match.group(1)] = match.group(2)
        
    tasks_vi = []
    tasks_en = []
    
    for match in re.finditer(r'<string\s+name="([^"]+)"[^>]*>(.*?)</string>', src_content):
        name = match.group(1)
        orig = match.group(2)
        if name not in vi_dict and not orig.startswith('@string/'):
            tasks_vi.append((name, orig))
        if not orig.startswith('@string/') and name != 'version_name':
            tasks_en.append((name, orig))
            
    tasks_vi_dedup = list(set([t[1] for t in tasks_vi]))
    tasks_en_dedup = list(set([t[1] for t in tasks_en]))
    
    print(f"Translating {len(tasks_vi_dedup)} strings to vi...")
    with ThreadPoolExecutor(max_workers=20) as executor:
        f_vi = {executor.submit(translate_text, t, 'vi'): t for t in tasks_vi_dedup}
        f_en = {executor.submit(translate_text, t, 'en'): t for t in tasks_en_dedup}
        vi_translated = {f_vi[f]: f.result() for f in f_vi}
        en_translated = {f_en[f]: f.result() for f in f_en}

    out_vi_lines = []
    out_en_lines = []
    
    for line in src_content.split('\n'):
        m = re.search(r'(<string\s+name="([^"]+)"[^>]*>)(.*?)(</string>)', line)
        if m:
            prefix, name, orig, suffix = m.groups()
            
            # VI
            if name in vi_dict:
                vi_t = vi_dict[name]
            else:
                vi_t = vi_translated.get(orig, orig)
            out_vi_lines.append(line.replace(prefix + orig + suffix, prefix + vi_t + suffix))
            
            # EN
            en_t = en_translated.get(orig, orig) if name != 'version_name' else orig
            out_en_lines.append(line.replace(prefix + orig + suffix, prefix + en_t + suffix))
        else:
            out_vi_lines.append(line)
            out_en_lines.append(line)
            
    vi_res = '\n'.join(out_vi_lines)
    en_res = '\n'.join(out_en_lines)
    
    with open(f'{VALUES_VI_DIR}/strings.xml', 'w', encoding='utf-8') as f:
        f.write(vi_res)
    with open(f'{VALUES_EN_DIR}/strings.xml', 'w', encoding='utf-8') as f:
        f.write(en_res)
    with open(f'{VALUES_DIR}/strings.xml', 'w', encoding='utf-8') as f:
        f.write(vi_res)

if __name__ == '__main__':
    os.makedirs(VALUES_EN_DIR, exist_ok=True)
    process_arrays()
    process_strings()
    print("Done generating V2 translations!")
