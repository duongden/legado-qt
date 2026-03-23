import os
import struct
import collections

# CONFIG
SRC_DIR = r"app/src/main/assets/translate/vietphrase"
DST_DIR = r"app/src/main/assets/dict"
FILES = {
    "Names.txt": "names.bin",
    "VietPhrase.txt": "vietphrase.bin",
    "ChinesePhienAmWords.txt": "phienam.bin"
}

MAGIC = 0x54434944 # "DICT" in Little Endian
VERSION = 1

class BuildNode:
    def __init__(self):
        self.value = None
        self.children = {} # char -> BuildNode

def to_utf16_units(s):
    """Converts string to a list of UTF-16 code units (ints)."""
    return [c for c in struct.unpack('<' + 'H' * (len(s.encode('utf-16-le')) // 2), s.encode('utf-16-le'))]

def build_trie(filepath):
    """Parses text file and builds an in-memory Trie."""
    root = BuildNode()
    print(f"Reading {filepath}...")
    count = 0
    with open(filepath, 'r', encoding='utf-8') as f:
        for line in f:
            line = line.strip()
            if not line or '=' not in line:
                continue
            
            # Legacy format handling
            parts = line.split('=', 1)
            key = parts[0].strip()
            val = parts[1].strip()
            
            if not key or not val:
                continue
                
            # Insert using UTF-16 units
            node = root
            for char_code in to_utf16_units(key):
                # We use the integer code unit as the key in children dict
                if char_code not in node.children:
                    node.children[char_code] = BuildNode()
                node = node.children[char_code]
            node.value = val
            count += 1
    print(f"Loaded {count} entries.")
    return root

class FlatNode:
    def __init__(self, char_code):
        self.char_code = char_code
        self.children_count = 0
        self.children_offset = 0
        self.value_offset = -1

def compile_bin(src_path, dst_path):
    root = build_trie(src_path)
    
    # Flatten
    print("Flattening Trie...")
    nodes = [] # List of FlatNode
    
    # String Pool
    string_pool = bytearray()
    string_map = {} # value -> offset
    
    # Root Node (Dummy) - Index 0
    nodes.append(FlatNode(0))
    
    # Queue for BFS: (BuildNode, flat_index)
    queue = collections.deque()
    queue.append((root, 0))
    
    while queue:
        build_node, flat_index = queue.popleft()
        flat_node = nodes[flat_index]
        
        # Handle Value
        if build_node.value is not None:
            val = build_node.value
            if val not in string_map:
                # Encode string
                encoded = val.encode('utf-8')
                offset = len(string_pool)
                # Length (Short)
                l = len(encoded)
                string_pool.extend(struct.pack('<H', l))
                string_pool.extend(encoded)
                string_map[val] = offset
            
            flat_node.value_offset = string_map[val]
            
        # Handle Children
        if build_node.children:
            sorted_chars = sorted(build_node.children.keys()) # These are ints now
            flat_node.children_count = len(sorted_chars)
            flat_node.children_offset = len(nodes) # Children start at end of current list
            
            for char_code in sorted_chars:
                child_node = build_node.children[char_code]
                nodes.append(FlatNode(char_code))
                # Enqueue child for processing (BFS)
                # Important: The child's flat_index is the one we just appended (nodes[-1])
                # BUT since we are appending multiple children in a loop, their indices are sequential.
                child_flat_idx = len(nodes) - 1
                queue.append((child_node, child_flat_idx))

    # Write
    print(f"Writing to {dst_path}...")
    # Ensure dir
    os.makedirs(os.path.dirname(dst_path), exist_ok=True)
    
    with open(dst_path, 'wb') as f:
        # Header (16 bytes)
        # Magic(4), Version(4), NodeCount(4), StringPoolSize(4)
        f.write(struct.pack('<IIII', MAGIC, VERSION, len(nodes), len(string_pool)))
        
        # Node Table
        # Char(2), Count(2), ChildOff(4), ValOff(4)
        for node in nodes:
            f.write(struct.pack('<HHii', node.char_code, node.children_count, node.children_offset, node.value_offset))
            
        # String Pool
        f.write(string_pool)
        
    print(f"Done. Size: {os.path.getsize(dst_path) / 1024 / 1024:.2f} MB")

def main():
    for src_name, dst_name in FILES.items():
        src = os.path.join(SRC_DIR, src_name)
        dst = os.path.join(DST_DIR, dst_name)
        if os.path.exists(src):
            compile_bin(src, dst)
        else:
            print(f"Error: {src} not found")

if __name__ == '__main__':
    main()
