import os
import re

FILES = [
    '../app/src/main/res/values/strings.xml',
    '../app/src/main/res/values-vi/strings.xml',
    '../app/src/main/res/values-en/strings.xml',
    '../app/src/main/res/values/arrays.xml',
    '../app/src/main/res/values-vi/arrays.xml',
    '../app/src/main/res/values-en/arrays.xml',
]

def fix_xml_content(content):
    lines = content.split('\n')
    out_lines = []
    
    for line in lines:
        # Match <string name="...">...</string> or <item>...</item>
        m_str = re.search(r'(<string\s+name="[^"]+"[^>]*>)(.*?)(</string>)', line)
        m_item = re.search(r'(<item[^>]*>)(.*?)(</item>)', line)
        
        m = m_str or m_item
        if m:
            prefix, text, suffix = m.groups()
            
            # Escape ampersands not already escaped
            # Regex: & not followed by (amp;|lt;|gt;|quot;|apos;|#\d+;)
            text = re.sub(r'&(?!(amp|lt|gt|quot|apos|#\d+);)', '&amp;', text)
            
            # Escape apostrophes that are not escaped
            text = re.sub(r"(?<!\\)'", r"\'", text)
            
            # Fix broken format specifiers like % s -> %s, % d -> %d
            text = re.sub(r'%\s+s', '%s', text)
            text = re.sub(r'%\s+d', '%d', text)
            text = re.sub(r'%\s+(\d+)\s+\$\s+s', r'%\1$s', text)
            text = re.sub(r'%\s+(\d+)\s+\$\s+d', r'%\1$d', text)
            
            # Escape less/greater than if used as plain text (optional, usually Android AAPT complains about < if not proper tag, but let's be careful. Actually Android supports <b>, <i>, <u>)
            # Just fixing apostrophes and ampersands is usually enough.
            
            out_lines.append(line.replace(prefix + m.group(2) + suffix, prefix + text + suffix))
        else:
            out_lines.append(line)
            
    return '\n'.join(out_lines)

for fpath in FILES:
    if os.path.exists(fpath):
        with open(fpath, 'r', encoding='utf-8') as f:
            content = f.read()
        fixed = fix_xml_content(content)
        with open(fpath, 'w', encoding='utf-8') as f:
            f.write(fixed)

print("Parsed and fixed XML formatting!")
