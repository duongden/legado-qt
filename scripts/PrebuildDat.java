import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.*;

/**
 * Pre-build .dat (DoubleArrayTrie binary) files from .txt dictionary files.
 * Compile & run: javac scripts/PrebuildDat.java && java -cp scripts -Xmx2g PrebuildDat
 */
public class PrebuildDat {

    static final int MAGIC = 0x44415432; // "DAT2"
    static final int VERSION = 2;

    // DoubleArrayTrie fields
    int[] base, check, valueIndex;
    boolean[] used;
    String[] values;
    int size, nextCheckPos, maxCharValue;
    HashMap<Character, Integer> charMap = new HashMap<>();

    void build(List<String[]> entries) {
        if (entries.isEmpty()) { size = 0; return; }
        entries.sort(Comparator.comparing(e -> e[0]));
        String[] keys = new String[entries.size()];
        String[] vals = new String[entries.size()];
        for (int i = 0; i < entries.size(); i++) { keys[i] = entries.get(i)[0]; vals[i] = entries.get(i)[1]; }

        size = keys.length;
        values = vals;
        buildCharMapping(keys);

        int initSize = calcInitSize(keys);
        base = new int[initSize]; check = new int[initSize]; valueIndex = new int[initSize]; used = new boolean[initSize];
        Arrays.fill(check, -1); Arrays.fill(valueIndex, -1);
        nextCheckPos = 0;

        check[1] = 0;
        base[1] = 1;

        int[][] root = fetch(0, 0, keys.length, keys);
        insert(root, 1, keys, vals);
        compactArrays();
        System.out.println("  Built trie: " + keys.length + " entries, base size: " + base.length);
    }

    void buildCharMapping(String[] keys) {
        TreeSet<Character> chars = new TreeSet<>();
        for (String k : keys) for (char c : k.toCharArray()) chars.add(c);
        charMap.clear(); int code = 1;
        for (char c : chars) charMap.put(c, code++);
        maxCharValue = code;
    }

    int calcInitSize(String[] keys) {
        int total = 0; for (String k : keys) total += k.length();
        return (int)(total * 1.8) + keys.length + 2048;
    }

    // returns int[][] where each row is [code, depth, left, right]
    int[][] fetch(int depth, int left, int right, String[] keys) {
        ArrayList<int[]> siblings = new ArrayList<>();
        int prevCode = -1;
        for (int i = left; i < right; i++) {
            int code = depth < keys[i].length() ? charMap.getOrDefault(keys[i].charAt(depth), 0) : 0;
            if (code != prevCode) {
                siblings.add(new int[]{code, depth + 1, i, i + 1});
                prevCode = code;
            } else {
                int[] last = siblings.get(siblings.size() - 1);
                last[3] = i + 1;
            }
        }
        return siblings.toArray(new int[0][]);
    }

    void insert(int[][] siblings, int parentIndex, String[] keys, String[] vals) {
        if (siblings.length == 0) return;
        int begin, pos = Math.max(nextCheckPos, siblings[0][0] + 1) - 1;

        outer:
        while (true) {
            pos++;
            ensureCapacity(pos + 1);
            if (check[pos] != -1) continue;
            begin = pos - siblings[0][0];
            if (begin <= 0) continue;
            if (begin + siblings[siblings.length - 1][0] >= check.length)
                ensureCapacity(begin + siblings[siblings.length - 1][0] + 1);
            if (used.length <= begin) ensureCapacity(begin + siblings[siblings.length - 1][0] + 1);
            if (used[begin]) continue;
            for (int[] s : siblings) {
                int idx = begin + s[0];
                if (idx >= check.length) ensureCapacity(idx + 1);
                if (check[idx] != -1) continue outer;
            }
            break;
        }

        used[begin] = true;
        base[parentIndex] = begin;
        if (pos + 1 > nextCheckPos) nextCheckPos = pos;
        for (int[] s : siblings) check[begin + s[0]] = parentIndex;
        for (int[] s : siblings) {
            int idx = begin + s[0];
            if (s[0] == 0) { valueIndex[idx] = s[2]; continue; }
            int[][] newSiblings = fetch(s[1], s[2], s[3], keys);
            if (newSiblings.length == 0) continue;
            insert(newSiblings, idx, keys, vals);
        }
    }

    void ensureCapacity(int minSize) {
        if (minSize <= base.length) return;
        int newSize = base.length;
        while (newSize < minSize) { newSize *= 2; if (newSize <= 0) { newSize = minSize; break; } }
        int oldSize = base.length;
        base = Arrays.copyOf(base, newSize);
        check = Arrays.copyOf(check, newSize);
        valueIndex = Arrays.copyOf(valueIndex, newSize);
        used = Arrays.copyOf(used, newSize);
        Arrays.fill(check, oldSize, newSize, -1);
        Arrays.fill(valueIndex, oldSize, newSize, -1);
    }

    void compactArrays() {
        int maxUsed = 0;
        for (int i = 0; i < check.length; i++) if (check[i] != -1) maxUsed = i;
        if (maxUsed < base.length - 1) {
            int s = maxUsed + 1;
            base = Arrays.copyOf(base, s); check = Arrays.copyOf(check, s);
            valueIndex = Arrays.copyOf(valueIndex, s); used = Arrays.copyOf(used, s);
        }
    }

    void save(File datFile) throws IOException {
        try (DataOutputStream dos = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(datFile), 1024 * 1024))) {
            dos.writeInt(MAGIC); dos.writeInt(VERSION); dos.writeInt(size);
            dos.writeInt(base.length); dos.writeInt(charMap.size()); dos.writeInt(maxCharValue);
            for (Map.Entry<Character, Integer> e : charMap.entrySet()) { dos.writeInt((int) e.getKey()); dos.writeInt(e.getValue()); }

            ByteBuffer buf = ByteBuffer.allocate(base.length * 4).order(ByteOrder.BIG_ENDIAN);
            buf.asIntBuffer().put(base); dos.write(buf.array());
            buf.clear(); buf.asIntBuffer().put(check); dos.write(buf.array());

            ArrayList<int[]> pairs = new ArrayList<>();
            for (int i = 0; i < valueIndex.length; i++) {
                if (valueIndex[i] >= 0 && valueIndex[i] < values.length) pairs.add(new int[]{i, valueIndex[i]});
            }
            dos.writeInt(pairs.size());
            for (int[] p : pairs) { dos.writeInt(p[0]); dos.writeUTF(values[p[1]]); }
        }
    }

    static List<String[]> loadEntries(File txtFile) throws IOException {
        ArrayList<String[]> entries = new ArrayList<>();
        int total = 0;
        try (BufferedReader r = new BufferedReader(new InputStreamReader(new FileInputStream(txtFile), "UTF-8"), 32768)) {
            String line;
            while ((line = r.readLine()) != null) {
                total++;
                if (!line.isBlank() && line.contains("=")) {
                    String clean = line.replace("\u0000", "").replace("\u0001", "").replace("\u0004", "").trim();
                    if (!clean.isEmpty() && clean.contains("=")) {
                        int eq = clean.indexOf('=');
                        String k = clean.substring(0, eq).trim();
                        String v = clean.substring(eq + 1).trim();
                        if (!k.isEmpty() && !v.isEmpty()) entries.add(new String[]{k, v});
                    }
                }
                if (total % 100000 == 0) System.out.println("  Read " + total + " lines...");
            }
        }
        System.out.println("  Total lines: " + total + ", valid entries: " + entries.size());
        return entries;
    }

    public static void main(String[] args) throws Exception {
        String assetsDir = "app/src/main/assets/translate/vietphrase";
        String[][] dicts = {{"Names", "Names"}, {"VietPhrase", "VietPhrase"}};

        for (String[] d : dicts) {
            File txtFile = new File(assetsDir, d[0] + ".txt");
            File datFile = new File(assetsDir, d[1] + ".dat");
            if (!txtFile.exists()) { System.out.println("ERROR: " + txtFile + " not found!"); continue; }

            System.out.println("Building " + d[1] + ".dat from " + d[0] + ".txt...");
            List<String[]> entries = loadEntries(txtFile);
            PrebuildDat trie = new PrebuildDat();
            trie.build(entries);
            trie.save(datFile);
            System.out.println("Saved " + datFile + " (" + datFile.length() / 1024 + "KB)\n");
        }
        System.out.println("Done!");
    }
}
