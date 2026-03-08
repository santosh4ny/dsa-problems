Section 12: Collections Deep Dive

Sections 7 and 11 touched collections from concurrency and streams angles. Section 12 goes inside the data structures themselves. FAANG interviewers use collections questions to test algorithmic thinking — they want to know if you understand why a HashMap is O(1) amortized, what breaks that guarantee, and how to choose the right structure under constraints. This is where candidates separate themselves.

The Big Picture — Java Collections Hierarchy
Iterable
└── Collection
├── List              (ordered, indexed, duplicates OK)
│   ├── ArrayList
│   ├── LinkedList
│   └── ArrayDeque    (also implements Deque)
│
├── Set               (no duplicates)
│   ├── HashSet           → backed by HashMap
│   ├── LinkedHashSet     → backed by LinkedHashMap
│   └── TreeSet           → backed by TreeMap (sorted)
│
└── Queue / Deque     (ordered for retrieval)
├── ArrayDeque        (resizable array — best Deque impl)
├── LinkedList        (also implements List)
└── PriorityQueue     (heap-based)

Map (NOT a Collection, but part of the framework)
├── HashMap               → hash table
├── LinkedHashMap         → hash table + doubly linked list
├── TreeMap               → Red-Black tree (sorted)
├── EnumMap               → array-backed (Enum keys only)
└── IdentityHashMap       → == instead of .equals() for keys

KEY DESIGN PRINCIPLE:
Program to INTERFACES (List, Map, Set), not implementations
List<String> list = new ArrayList<>();   ✅
ArrayList<String> list = new ArrayList<>();  ❌ (locks you in)

Part 1: HashMap — Complete Internals
The Hash Table Data Structure
java// HashMap<K, V> answers: given a key, find its value in O(1) average
// Magic: hash function maps key → array index in O(1)

// INTERNAL STRUCTURE (Java 8+):
// Node<K,V>[] table   — array of buckets (size always power of 2)
// Each bucket: null | single Node | linked list of Nodes | TreeNode (red-black tree)

// Node structure:
static class Node<K,V> {
final int    hash;    // Cached hashCode (don't recompute on resize)
final K      key;
V            value;
Node<K,V>    next;    // Linked list pointer (chaining for collisions)
}


// ── Step-by-step: how put(key, value) works ───────────────────────

// Step 1: Compute hash
// HashMap doesn't use key.hashCode() directly — it spreads bits:
static final int hash(Object key) {
int h;
return (key == null) ? 0 :
(h = key.hashCode()) ^ (h >>> 16);
// XOR high 16 bits into low 16 bits
// WHY: table.length is often small (16, 32, 64...)
//      Only LOW bits of hashCode determine bucket (index = hash & (n-1))
//      If hashCode has poor low bits → many collisions
//      Spreading high bits into low bits → better distribution
}

// Step 2: Determine bucket index
int index = hash & (table.length - 1);
// WHY & (n-1) instead of % n?
//   table.length is ALWAYS a power of 2: 16, 32, 64, 128...
//   For power of 2: hash % n  ==  hash & (n-1)
//   But & is 5x faster than % (single CPU instruction vs division)
//   This is why HashMap ALWAYS uses power-of-2 sizes

// Step 3: Handle the bucket
// CASE A: table[index] == null → create new Node, done O(1)
// CASE B: table[index] exists → collision! Walk the chain:
//   For each existing Node:
//     if (node.hash == hash && (node.key == key || key.equals(node.key)))
//       → FOUND: update value, done
//   If not found: append new Node to tail of linked list


// ── The treeification threshold (Java 8 key improvement) ──────────
// Problem: with many collisions, linked list → O(n) per operation
// Fix: convert linked list to Red-Black tree when list gets long

static final int TREEIFY_THRESHOLD    = 8;  // List → Tree when chain length ≥ 8
static final int UNTREEIFY_THRESHOLD  = 6;  // Tree → List when chain shrinks to ≤ 6
static final int MIN_TREEIFY_CAPACITY = 64; // ONLY treeify if table.length ≥ 64
// Below 64: resize instead

// After treeification: worst-case per-bucket is O(log n) not O(n)
// Protects against hash collision attacks (malicious inputs forcing O(n²))
// Before Java 8: HashMap was vulnerable to DoS via crafted keys


// ── get(key) ──────────────────────────────────────────────────────
// 1. Compute hash(key)
// 2. index = hash & (n-1)
// 3. Scan bucket chain/tree for matching hash + equals
// Average: O(1) — usually 0 or 1 node in bucket
// Worst:   O(log n) — if bucket is a tree (Java 8+)
//          O(n)     — if all keys hash to same bucket AND size < 64 (pre-Java 8 or small map)
Resizing — The Hidden Cost
java// LOAD FACTOR: how full the table is allowed to get before resizing
// Default: 0.75  (resize when 75% of buckets are occupied)
// Default initial capacity: 16
// Resize threshold: 16 × 0.75 = 12 entries → triggers resize

// WHY 0.75?
// Low load factor (e.g., 0.5): fewer collisions but wastes memory (half empty)
// High load factor (e.g., 0.9): more collisions, worse performance but less memory
// 0.75: empirically optimal trade-off (from birthday paradox probability analysis)

// RESIZE PROCESS:
// 1. New array: double the size (16 → 32 → 64 → 128...)
// 2. REHASH: every existing entry must be re-inserted
//    (index = hash & (newLength - 1) — new length means new bucket assignment)
// 3. Cost: O(n) for the resize step!

// AMORTIZED ANALYSIS:
// Each resize doubles capacity → entries rehashed = n
// Time between resizes: n/2 insertions (added half the capacity)
// Amortized cost per insert: O(n) / (n/2) = O(2) = O(1) amortized
// Similar to ArrayList append — occasional expensive operation, O(1) amortized overall


// ── Pre-sizing: eliminate resizes ────────────────────────────────
// If you know approximate size upfront: set initial capacity

// ❌ Default — will resize multiple times for 1000 entries:
Map<String, User> map = new HashMap<>();   // capacity=16, resizes at 12, 24, 48, 96, 192...
for (int i = 0; i < 1000; i++) map.put(...);
// Resize count: ~6 resizes, each O(n) → wasted work

// ✅ Pre-sized: eliminates all resizes
int expectedSize = 1000;
int initialCapacity = (int)(expectedSize / 0.75) + 1;  // = 1334
Map<String, User> map2 = new HashMap<>(initialCapacity);
// Next power of 2 ≥ 1334 = 2048
// 2048 × 0.75 = 1536 → won't resize until 1536 entries
// No resize for our 1000 entries ✅

// Guava shortcut:
Map<String, User> map3 = Maps.newHashMapWithExpectedSize(1000);
// Guava computes the capacity for you

// Java 19+:
Map<String, User> map4 = HashMap.newHashMap(1000);  // Exact expected size


// ── Resize in Java 8: clever optimization ────────────────────────
// OLD (Java 7): rehash every key — recompute hash(key) % newSize
// NEW (Java 8): hash is CACHED in Node
//   Old index: hash & (oldSize - 1)  e.g., hash & 15   (4 bits)
//   New index: hash & (newSize - 1)  e.g., hash & 31   (5 bits)
//   Difference: just bit 4 of the hash
//   If bit 4 = 0: new index = old index (stay in same bucket)
//   If bit 4 = 1: new index = old index + oldSize (move to upper half)
//   Split each bucket into two groups with a single bit check
//   No recomputation of hash — just read the cached hash value
//   Result: resize is faster AND preserves insertion order within bucket
hashCode and equals — The Critical Contract
java// THE CONTRACT:
// a.equals(b) == true  →  a.hashCode() == b.hashCode()   REQUIRED
// a.hashCode() == b.hashCode()  →  a.equals(b) MAY be false (collision OK)

// ── Breaking the contract: the most dangerous Java bug ────────────
class BadKey {
int value;

    BadKey(int v) { this.value = v; }

    @Override
    public boolean equals(Object o) {
        return o instanceof BadKey bk && this.value == bk.value;
    }
    // ❌ NO hashCode override!
    // Default hashCode: based on IDENTITY (memory address)
    // new BadKey(5).hashCode() ≠ new BadKey(5).hashCode() (different objects!)
}

Map<BadKey, String> map = new HashMap<>();
BadKey key = new BadKey(5);
map.put(key, "hello");

map.get(new BadKey(5));    // ❌ Returns NULL!
// new BadKey(5): different hashCode (different object)
// → looks in wrong bucket
// → can't find it
// Even though equals() would return true!

map.get(key);              // ✅ Returns "hello"
// Same object → same identity hashCode → correct bucket


// ✅ Correct implementation
class GoodKey {
final int value;  // ALWAYS use final for map keys (immutable!)

    GoodKey(int v) { this.value = v; }

    @Override
    public boolean equals(Object o) {
        return o instanceof GoodKey gk && this.value == gk.value;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(value);  // Consistent with equals
    }
}

// ── Mutable keys: the other catastrophe ──────────────────────────
class MutableKey {
int id;
MutableKey(int id) { this.id = id; }

    @Override public boolean equals(Object o) { return o instanceof MutableKey mk && id == mk.id; }
    @Override public int hashCode() { return id; }
}

Map<MutableKey, String> map2 = new HashMap<>();
MutableKey key2 = new MutableKey(1);
map2.put(key2, "value");

key2.id = 99;                        // Mutate key AFTER insertion!
map2.get(key2);                      // ❌ NULL!
// hashCode() is now 99 → different bucket
// Key is at bucket 1, looked in bucket 99
// Entry permanently lost in the map!
map2.containsKey(key2);              // false — it's there but unreachable
map2.size();                         // 1 — it's still in the map, just unfindable

// Rule: HashMap keys should ALWAYS be immutable
// Best keys: String, Integer, Long, UUID, record, enum


// ── Writing a good hashCode ───────────────────────────────────────
class Order {
final String orderId;
final String customerId;
final LocalDate date;

    // ✅ Java 7+: Objects.hash — concise, reasonable distribution
    @Override
    public int hashCode() {
        return Objects.hash(orderId, customerId, date);
        // Calls: orderId.hashCode() * 31 + customerId.hashCode() * 31 + date.hashCode()
        // The 31 multiplier: prime number, spreads bits well, JIT optimizes to bit shift
        // 31 * h == (h << 5) - h → fast!
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Order other
            && Objects.equals(orderId, other.orderId)
            && Objects.equals(customerId, other.customerId)
            && Objects.equals(date, other.date);
    }
}

// ✅ Even better: use a record (auto-generates correct hashCode/equals)
record Order2(String orderId, String customerId, LocalDate date) {}


// ── hashCode quality matters for performance ──────────────────────
// BAD hashCode — returns constant:
@Override public int hashCode() { return 42; }
// ALL keys hash to same bucket → linked list of n elements
// get/put: O(n) not O(1) — HashMap becomes a linked list!

// BAD hashCode — only uses one field when multiple define identity:
record Point(int x, int y) {
@Override public int hashCode() { return x; }  // Ignores y!
// All Points with same x → same bucket
// (0,0), (0,1), (0,2)... → all same bucket
}

// GOOD hashCode: uses ALL fields that define identity, spreads bits
HashMap Iteration and Views
javaMap<String, Integer> map = Map.of("a", 1, "b", 2, "c", 3);

// ── Three views — all LIVE (reflect map changes) ─────────────────
Set<String>                  keys    = map.keySet();    // Set<K>
Collection<Integer>          values  = map.values();    // Collection<V>
Set<Map.Entry<String,Integer>> entries = map.entrySet(); // Set<Entry<K,V>>

// ── Iterating entries — ALWAYS use entrySet() ─────────────────────
// ❌ Slow: two lookups per entry
for (String key : map.keySet()) {
Integer value = map.get(key);   // Second lookup! Unnecessary.
}

// ✅ Fast: one lookup
for (Map.Entry<String, Integer> entry : map.entrySet()) {
String  key   = entry.getKey();
Integer value = entry.getValue();
}

// ✅ Java 8+: forEach
map.forEach((key, value) -> process(key, value));


// ── Atomic operations — putIfAbsent, computeIfAbsent, merge ───────
// putIfAbsent: insert only if key is absent
map.putIfAbsent("d", 4);   // Inserts "d"→4 only if "d" not present

// computeIfAbsent: compute and insert only if absent (lazy initialization)
Map<String, List<String>> multiMap = new HashMap<>();

// ❌ Verbose:
if (!multiMap.containsKey("group1")) {
multiMap.put("group1", new ArrayList<>());
}
multiMap.get("group1").add("member");

// ✅ Atomic:
multiMap.computeIfAbsent("group1", k -> new ArrayList<>()).add("member");
// computeIfAbsent: if absent → call lambda → insert result → return it
// if present → return existing value
// ATOMIC: no race condition even in non-concurrent maps (for single-threaded use)

// compute: update regardless of presence
// (null return from function = remove the entry)
Map<String, Integer> wordCount = new HashMap<>();
for (String word : words) {
wordCount.compute(word, (k, v) -> v == null ? 1 : v + 1);
// k=word, v=current count (null if absent)
// Returns: 1 for new words, v+1 for existing
}

// merge: combine new value with existing (most concise for accumulation)
for (String word : words) {
wordCount.merge(word, 1, Integer::sum);
// If absent: put word→1
// If present: put word→existingValue + 1
// Most concise way to implement frequency counting
}

Part 2: TreeMap — Red-Black Tree Internals
java// TreeMap<K, V>: sorted map
// Backed by a RED-BLACK TREE — a self-balancing BST
// All operations: O(log n) guaranteed (not amortized)

// ── Red-Black Tree properties ─────────────────────────────────────
// 1. Every node is RED or BLACK
// 2. Root is BLACK
// 3. Every null leaf is BLACK
// 4. If a node is RED: both children are BLACK
// 5. All paths from any node to its null leaves have same number of BLACK nodes
//
// These properties guarantee: height ≤ 2×log₂(n+1)
// → O(log n) for search, insert, delete — GUARANTEED (no worst case)

// ── Creating TreeMap ──────────────────────────────────────────────
// Natural ordering (keys must implement Comparable):
TreeMap<String, Integer> byName = new TreeMap<>();

// Custom ordering via Comparator:
TreeMap<String, Integer> byLength = new TreeMap<>(
Comparator.comparingInt(String::length).thenComparing(Comparator.naturalOrder())
);

// ── NavigableMap API — TreeMap's unique power ─────────────────────
TreeMap<Integer, String> scores = new TreeMap<>();
scores.put(60, "D"); scores.put(70, "C");
scores.put(80, "B"); scores.put(90, "A"); scores.put(100, "A+");

// Exact match
scores.get(80);               // "B"

// Ceiling/Floor (≥ or ≤):
scores.ceilingKey(75);        // 80 — smallest key ≥ 75
scores.floorKey(75);          // 70 — largest key ≤ 75
scores.ceilingEntry(75);      // Entry(80, "B")
scores.floorEntry(75);        // Entry(70, "C")

// Higher/Lower (strictly > or <):
scores.higherKey(80);         // 90 — smallest key STRICTLY > 80
scores.lowerKey(80);          // 70 — largest key STRICTLY < 80

// First/Last:
scores.firstKey();            // 60 — minimum key
scores.lastKey();             // 100 — maximum key
scores.firstEntry();          // Entry(60, "D")
scores.lastEntry();           // Entry(100, "A+")

// Range views (LIVE subsets of the map):
NavigableMap<Integer, String> high = scores.subMap(80, true, 100, true);
// Keys from 80 (inclusive) to 100 (inclusive)
// {80→"B", 90→"A", 100→"A+"}

NavigableMap<Integer, String> top  = scores.tailMap(80);     // ≥ 80
NavigableMap<Integer, String> low  = scores.headMap(80);     // < 80

// Descending (reverse iteration — no copy!):
NavigableMap<Integer, String> desc = scores.descendingMap();
scores.descendingKeySet().forEach(k -> process(k, scores.get(k)));


// ── Classic interview application: Grade calculator ───────────────
class GradeCalculator {
// TreeMap: key=minimum score, value=grade
private final NavigableMap<Integer, String> gradeMap = new TreeMap<>(Map.of(
0,  "F",
60, "D",
70, "C",
80, "B",
90, "A"
));

    String getGrade(int score) {
        // floorEntry: largest key ≤ score → the grade bracket this score falls into
        Map.Entry<Integer, String> entry = gradeMap.floorEntry(score);
        return entry != null ? entry.getValue() : "Invalid";
    }
}

new GradeCalculator().getGrade(85);   // "B" — floor(85) = 80 → "B"
new GradeCalculator().getGrade(95);   // "A" — floor(95) = 90 → "A"
new GradeCalculator().getGrade(55);   // "F" — floor(55) = 0  → "F"


// ── When to use TreeMap vs HashMap ───────────────────────────────
// HashMap:  O(1) average, unordered         → most use cases
// TreeMap:  O(log n) guaranteed, SORTED     → need ordering, ranges, floor/ceiling
//
// Use TreeMap when:
//   → Need keys in sorted order (iteration, display)
//   → Need range queries (subMap, headMap, tailMap)
//   → Need floor/ceiling/higher/lower lookups
//   → Implementing a scheduler (key=timestamp, find next scheduled event)
//   → Implementing a price range lookup
//
// Real interview applications of floor/ceiling:
//   → LeetCode 715: Range Module
//   → Calendar scheduling: find next available slot
//   → Stock prices: find closest price to target
//   → Elevator controller: find nearest floor to destination

Part 3: LinkedHashMap — Insertion Order and LRU
java// LinkedHashMap<K,V>: HashMap + doubly-linked list
// Maintains INSERTION ORDER (default) or ACCESS ORDER (LRU mode)
// All HashMap operations + O(1) ordered iteration

// ── Internal structure ────────────────────────────────────────────
// Extends HashMap.Node with before/after pointers:
static class Entry<K,V> extends HashMap.Node<K,V> {
Entry<K,V> before;   // Previous entry in linked list
Entry<K,V> after;    // Next entry in linked list
}
// head → oldest entry
// tail → newest entry
// Every put/remove: update the linked list links + hash table

// ── Insertion order (default) ─────────────────────────────────────
Map<String, Integer> insertionOrder = new LinkedHashMap<>();
insertionOrder.put("Charlie", 3);
insertionOrder.put("Alice",   1);
insertionOrder.put("Bob",     2);

insertionOrder.keySet().forEach(System.out::println);
// Charlie, Alice, Bob — insertion order preserved!
// HashMap would give: arbitrary order (bucket-based)


// ── Access order: LRU Cache implementation ────────────────────────
// new LinkedHashMap<>(capacity, loadFactor, accessOrder=true)
// Access order: every get/put moves entry to TAIL (most recently used)
// Head = LEAST recently used → evict from head

class LRUCache<K, V> extends LinkedHashMap<K, V> {
private final int maxSize;

    LRUCache(int maxSize) {
        super(maxSize, 0.75f, true);   // accessOrder = true!
        this.maxSize = maxSize;
    }

    @Override
    protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
        return size() > maxSize;
        // Called after every put
        // If true: remove eldest (head of linked list = LRU entry)
    }

    // Optional: expose get with Optional
    public Optional<V> getOptional(K key) {
        return Optional.ofNullable(get(key));
        // get() in access-order mode moves entry to tail (marks as recently used)
    }
}

LRUCache<Integer, String> cache = new LRUCache<>(3);
cache.put(1, "one");
cache.put(2, "two");
cache.put(3, "three");
// Order: [1, 2, 3] (head=oldest=1)

cache.get(1);           // Access key 1 → moves to tail
// Order: [2, 3, 1]   (head=oldest=2)

cache.put(4, "four");   // Over capacity → evict head (key 2)
// Order: [3, 1, 4]   key 2 is gone!

cache.get(2);           // null — was evicted

// ── Verify with size and containsKey:
System.out.println(cache.size());           // 3
System.out.println(cache.containsKey(2));   // false
System.out.println(cache.containsKey(1));   // true


// ── Thread-safe LRU Cache ─────────────────────────────────────────
// LinkedHashMap is NOT thread-safe
// For concurrent use:
class ConcurrentLRUCache<K, V> {
private final Map<K, V> cache;

    ConcurrentLRUCache(int maxSize) {
        this.cache = Collections.synchronizedMap(
            new LinkedHashMap<K, V>(maxSize, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
                    return size() > maxSize;
                }
            }
        );
    }
    // synchronizedMap wraps every method with synchronized
    // Simple, but coarse-grained locking

    // For high-throughput: use Caffeine library instead:
    // Cache<K,V> cache = Caffeine.newBuilder()
    //     .maximumSize(maxSize)
    //     .expireAfterAccess(10, TimeUnit.MINUTES)
    //     .build();
}

Part 4: ArrayList vs LinkedList — The Real Trade-offs
java// The interview trap: "ArrayList is good for random access, LinkedList for insert/delete"
// This is WRONG in practice. LinkedList is almost always worse.

// ── ArrayList internal ────────────────────────────────────────────
// Backed by Object[] array
// get(i): array[i] → O(1) — direct memory access
// add(e): array[size] = e; size++ → O(1) amortized (occasional resize)
// add(i, e): shift array[i..size] right → O(n) for shifts
// remove(i): shift array[i+1..size] left → O(n) for shifts

// ── LinkedList internal ───────────────────────────────────────────
// Doubly-linked list of Node objects
// Each Node: value + prev pointer + next pointer (3 object fields)
class Node<E> {
E    item;
Node<E> next;
Node<E> prev;
}

// get(i): traverse from head or tail → O(n/2) = O(n)! Not O(1)!
// add(e) at tail: O(1) — maintain tail pointer
// add(i, e): O(n) to FIND position, then O(1) to insert
// remove(node): O(1) IF you have the node reference
//               O(n) to FIND the node by index

// ── The cache efficiency problem ─────────────────────────────────
// ArrayList: Object[] in CONTIGUOUS memory
// CPU loads cache line (64 bytes) → gets ~8 adjacent elements
// Sequential iteration: CPU prefetcher works perfectly → near hardware speed

// LinkedList: Nodes scattered across heap (random memory locations)
// Each .next pointer: random memory jump → cache MISS per element
// Cache miss: ~100 CPU cycles vs ~4 for cache hit
// For large lists: LinkedList iteration is 10-100x SLOWER than ArrayList

// Proof via benchmark: iterating 1M element list
// ArrayList:   ~3ms  (sequential memory → cache friendly)
// LinkedList: ~150ms (random memory → cache thrashing)


// ── When LinkedList is ACTUALLY better ───────────────────────────
// Only case: you have an Iterator at a specific position
//            and need O(1) insert/remove AT THAT POSITION

Iterator<String> it = linkedList.listIterator(500);
it.add("new element");    // O(1) — you already have the node reference
it.remove();              // O(1) — same

// But: getting the iterator to position 500 → O(n) traversal
// So the total is still O(n) for the first operation

// In practice:
// Use ArrayList for EVERYTHING except:
//   → Queue/Deque operations (ArrayDeque is better than LinkedList for this too!)
//   → You genuinely need O(1) insert at arbitrary positions
//     (and have the iterator — not the index)


// ── ArrayList growth strategy ─────────────────────────────────────
// Initial capacity: 10 (default)
// Growth factor: 1.5× (newCapacity = oldCapacity + oldCapacity/2)
// Java: newCapacity = oldCapacity + (oldCapacity >> 1)

// Growth sequence: 10 → 15 → 22 → 33 → 49 → 73 → ...
// (vs HashMap's 2× growth)

// Pre-sizing:
List<String> list = new ArrayList<>(10_000);  // Reserve capacity upfront
// No copies until 10,000 elements added

// Trim to size (reduce memory after done adding):
((ArrayList<String>) list).trimToSize();
// Releases unused array capacity → reduces memory if list won't grow more


// ── CopyOnWriteArrayList (revisited from Section 7) ───────────────
// add(): copies entire array, adds element, swaps reference
// get(): reads from current snapshot — NEVER locks
// iterator(): returns iterator over current snapshot — won't see future changes

// Use: read-heavy, write-rare (event listeners, observer patterns)
// Don't use: write-heavy (every write = full array copy = O(n))

Part 5: ArrayDeque — The Overlooked Champion
java// ArrayDeque: resizable circular array
// Implements both Deque<E> AND Queue<E>
// FASTER than LinkedList for all queue/stack/deque operations
// FASTER than Stack for stack operations

// ── Internal structure: circular array ────────────────────────────
// Object[] elements  — the backing array
// int head           — index of first element
// int tail           — index after last element

// Example (capacity=8):
// elements: [_, _, A, B, C, D, _, _]
//                  ^              ^
//                 head           tail (next insertion point)

// addFirst (push to head):
//   head = (head - 1) & (elements.length - 1)   // Wrap around circularly
//   elements[head] = e

// addLast (enqueue at tail):
//   elements[tail] = e
//   tail = (tail + 1) & (elements.length - 1)   // Wrap around

// pollFirst (dequeue from head):
//   result = elements[head]
//   elements[head] = null   // Help GC
//   head = (head + 1) & (elements.length - 1)

// All these: O(1) amortized — no shifting, circular wrapping is O(1) (bitwise AND)


// ── ArrayDeque vs LinkedList ──────────────────────────────────────
// ArrayDeque: contiguous array (cache friendly), no node allocation overhead
// LinkedList: pointer-chasing, Node objects (GC pressure, cache misses)

// ArrayDeque is 2-5× faster for queue/deque operations in practice
// Use ArrayDeque: ALWAYS preferred over LinkedList for queue/deque/stack


// ── Stack operations ──────────────────────────────────────────────
Deque<Integer> stack = new ArrayDeque<>();
stack.push(1);        // addFirst(1)   → push to TOP
stack.push(2);        // addFirst(2)   → push to TOP
stack.push(3);        // addFirst(3)   → push to TOP
stack.peek();         // peekFirst()   → 3 (top, no remove)
stack.pop();          // removeFirst() → 3 (remove from TOP)
stack.pop();          // removeFirst() → 2
// Remaining: [1]

// Why NOT Stack class:
// Stack extends Vector (synchronized, slow)
// Every push/pop: acquires a lock — unnecessary overhead
// ✅ ArrayDeque for stack — faster, not synchronized (use if single-threaded)
// ✅ Deque<> interface — can swap implementation without API change


// ── Queue operations ──────────────────────────────────────────────
Queue<String> queue = new ArrayDeque<>();
queue.offer("first");   // addLast  — enqueue
queue.offer("second");
queue.offer("third");
queue.peek();           // peekFirst — "first" (no remove)
queue.poll();           // removeFirst — "first" (dequeue from front)
// Remaining: ["second", "third"]


// ── Deque operations (double-ended) ──────────────────────────────
Deque<Integer> deque = new ArrayDeque<>();
deque.addFirst(2);      // [2]
deque.addFirst(1);      // [1, 2]
deque.addLast(3);       // [1, 2, 3]
deque.addLast(4);       // [1, 2, 3, 4]
deque.peekFirst();      // 1
deque.peekLast();       // 4
deque.pollFirst();      // 1 → [2, 3, 4]
deque.pollLast();       // 4 → [2, 3]


// ── Method names: the confusing part ─────────────────────────────
// Queue interface:       offer(e), peek(), poll()  — return null/false on failure
// Deque/Stack:           push(e), peek(), pop()    — throw exception on failure
// Legacy (avoid):        add(e), element(), remove() — throw on failure
// Rule: use offer/poll/peek in production (no exceptions on empty)

// Mapping:
// Stack:           Deque (preferred):
// push(e)      →  addFirst(e)
// pop()        →  removeFirst()
// peek()       →  peekFirst()

// Queue:           Deque (preferred):
// offer(e)     →  addLast(e) / offerLast(e)
// poll()       →  removeFirst() / pollFirst()
// peek()       →  peekFirst()

Part 6: PriorityQueue — Heap Mechanics
java// PriorityQueue: min-heap by default (smallest element at top)
// poll() always returns the MINIMUM element
// Backed by array representation of a binary heap

// ── Binary heap structure ─────────────────────────────────────────
// Complete binary tree stored in an array
// For element at index i:
//   parent:       (i - 1) / 2
//   left child:   2 * i + 1
//   right child:  2 * i + 2

// Heap property: parent ≤ both children (min-heap)

// Array: [1, 3, 2, 7, 5, 9, 4]
// Tree:
//         1          ← root (minimum)
//        / \
//       3   2
//      / \ / \
//     7  5 9  4

// ── Operations ────────────────────────────────────────────────────
// offer(e) — O(log n):
//   1. Add e at end of array (next leaf position)
//   2. SIFT UP: while e < parent → swap(e, parent), move up
//   Terminates when e ≥ parent or reaches root

// poll() — O(log n):
//   1. Remove root (minimum) — save it to return
//   2. Move LAST element to root position
//   3. SIFT DOWN: while current > min(left, right) → swap(current, smaller child)
//   Terminates when current ≤ both children or reaches leaf

// peek() — O(1): just return array[0] (root = minimum)

// contains() — O(n): must scan entire array (no ordering for arbitrary elements)
// remove(Object) — O(n): find it, then sift up/down to restore heap

// ── Usage ─────────────────────────────────────────────────────────
PriorityQueue<Integer> minHeap = new PriorityQueue<>();
minHeap.offer(5);
minHeap.offer(1);
minHeap.offer(3);
minHeap.peek();    // 1 (minimum, no removal)
minHeap.poll();    // 1 (remove minimum)
minHeap.poll();    // 3
minHeap.poll();    // 5

// Max-heap: reverse comparator
PriorityQueue<Integer> maxHeap = new PriorityQueue<>(Comparator.reverseOrder());
maxHeap.offer(5);
maxHeap.offer(1);
maxHeap.offer(3);
maxHeap.poll();   // 5 (maximum)

// Custom comparator:
PriorityQueue<Task> taskQueue = new PriorityQueue<>(
Comparator.comparingInt(Task::getPriority)
.reversed()                          // Highest priority first
.thenComparing(Task::getCreatedAt)   // FIFO for same priority
);


// ── Classic interview patterns ────────────────────────────────────

// Pattern 1: Kth Largest Element (maintain size-K min-heap)
int findKthLargest(int[] nums, int k) {
PriorityQueue<Integer> minHeap = new PriorityQueue<>();
for (int num : nums) {
minHeap.offer(num);
if (minHeap.size() > k) {
minHeap.poll();   // Remove smallest — keeps K largest
}
}
return minHeap.peek();    // Root = Kth largest
// Time: O(n log k)    Space: O(k)
}

// Pattern 2: Merge K sorted lists (use heap to track frontier)
ListNode mergeKLists(ListNode[] lists) {
// Min-heap: always extract the globally smallest current element
PriorityQueue<ListNode> heap = new PriorityQueue<>(
Comparator.comparingInt(n -> n.val)
);

    // Initialize: add head of each list
    for (ListNode head : lists) {
        if (head != null) heap.offer(head);
    }

    ListNode dummy = new ListNode(0);
    ListNode curr  = dummy;

    while (!heap.isEmpty()) {
        ListNode node = heap.poll();         // Globally smallest
        curr.next = node;
        curr = curr.next;
        if (node.next != null) {
            heap.offer(node.next);           // Add next from same list
        }
    }
    return dummy.next;
    // Time: O(n log k) where n=total nodes, k=number of lists
}

// Pattern 3: Top K frequent elements
List<Integer> topKFrequent(int[] nums, int k) {
Map<Integer, Integer> freq = new HashMap<>();
for (int n : nums) freq.merge(n, 1, Integer::sum);

    // Min-heap by frequency (keep only K most frequent)
    PriorityQueue<Integer> heap = new PriorityQueue<>(
        Comparator.comparingInt(freq::get)
    );

    for (int num : freq.keySet()) {
        heap.offer(num);
        if (heap.size() > k) heap.poll();    // Remove least frequent
    }

    return new ArrayList<>(heap);
    // Time: O(n log k)
}

// Pattern 4: Median from data stream (two heaps)
class MedianFinder {
// maxHeap: lower half (top = max of lower half)
PriorityQueue<Integer> lower = new PriorityQueue<>(Comparator.reverseOrder());
// minHeap: upper half (top = min of upper half)
PriorityQueue<Integer> upper = new PriorityQueue<>();

    void addNum(int num) {
        lower.offer(num);                    // Always offer to lower first
        upper.offer(lower.poll());           // Balance: move max of lower to upper
        if (lower.size() < upper.size()) {   // Keep lower >= upper in size
            lower.offer(upper.poll());
        }
        // Invariant: lower.size() == upper.size() OR lower.size() == upper.size() + 1
    }

    double findMedian() {
        if (lower.size() > upper.size()) {
            return lower.peek();             // Odd count: top of lower half
        }
        return (lower.peek() + upper.peek()) / 2.0;  // Even count: average tops
    }
    // addNum: O(log n)   findMedian: O(1)
}

Part 7: HashSet, LinkedHashSet, TreeSet
java// All Sets: no duplicate elements
// Set.add(e): returns false if element already present (no exception)

// ── HashSet internal ──────────────────────────────────────────────
// Backed by HashMap<E, PRESENT> where PRESENT = new Object() singleton
// Every add(e) → map.put(e, PRESENT)
// Every contains(e) → map.containsKey(e)
// All HashMap characteristics: O(1) average, unordered, allows null

HashSet<String> set = new HashSet<>();
set.add("a");
set.add("b");
set.add("a");   // Duplicate — returns false, set unchanged
set.size();     // 2


// ── TreeSet ───────────────────────────────────────────────────────
// Backed by TreeMap<E, PRESENT> — all operations O(log n)
// Maintains SORTED order, supports NavigableSet API

TreeSet<Integer> treeSet = new TreeSet<>();
treeSet.add(5); treeSet.add(2); treeSet.add(8); treeSet.add(1);
// Stored sorted: [1, 2, 5, 8]

treeSet.first();           // 1 (minimum)
treeSet.last();            // 8 (maximum)
treeSet.floor(6);          // 5 (largest ≤ 6)
treeSet.ceiling(6);        // 8 (smallest ≥ 6)
treeSet.higher(5);         // 8 (strictly > 5)
treeSet.lower(5);          // 2 (strictly < 5)

treeSet.headSet(5);        // [1, 2]      (< 5)
treeSet.tailSet(5);        // [5, 8]      (≥ 5)
treeSet.subSet(2, true, 8, false);  // [2, 5]  (2 ≤ x < 8)

// Classic application: sliding window maximum, event scheduling


// ── EnumSet — the performance winner for enums ────────────────────
enum Day { MON, TUE, WED, THU, FRI, SAT, SUN }

// ❌ Using HashSet for enum values — wastes memory and time
Set<Day> weekend = new HashSet<>(Set.of(Day.SAT, Day.SUN));

// ✅ EnumSet — backed by a LONG BITMASK (one bit per enum constant)
EnumSet<Day> weekendEnum = EnumSet.of(Day.SAT, Day.SUN);
EnumSet<Day> weekdays    = EnumSet.range(Day.MON, Day.FRI);
EnumSet<Day> all         = EnumSet.allOf(Day.class);
EnumSet<Day> workWeek    = EnumSet.copyOf(weekdays);

// Operations are BITWISE — O(1) regardless of set size
weekdayEnum.contains(Day.MON);    // Check bit 0: O(1)
weekdays.addAll(weekendEnum);     // Bitwise OR: O(1)
weekdays.retainAll(weekendEnum);  // Bitwise AND: O(1)
weekdays.removeAll(weekendEnum);  // Bitwise AND NOT: O(1)

// For up to 64 enum constants: uses single long — 64 bits
// For > 64: uses two longs
// Memory: 1 long (8 bytes) vs HashSet (hundreds of bytes)
// Performance: bitwise ops vs hash computation + pointer chasing


// ── EnumMap — same concept for maps ──────────────────────────────
EnumMap<Day, String> schedule = new EnumMap<>(Day.class);
schedule.put(Day.MON, "Standup");
schedule.put(Day.FRI, "Retro");
// Backed by Object[] indexed by enum ordinal
// O(1) get/put (array index = ordinal)
// Ordered by enum declaration order
// Memory: one Object[] of enum.values().length
// Faster AND smaller than HashMap for enum keys

Part 8: Collections Utility Class — Underused Power
javaimport java.util.Collections;

// ── Sorting ───────────────────────────────────────────────────────
List<Integer> list = new ArrayList<>(Arrays.asList(3, 1, 4, 1, 5, 9, 2, 6));
Collections.sort(list);                                   // Natural order
Collections.sort(list, Comparator.reverseOrder());        // Reverse
Collections.sort(list, Comparator.comparingInt(Math::abs)); // Custom

// Java 8+: list.sort() delegates to Arrays.sort() (TimSort)
list.sort(Comparator.naturalOrder());   // Same result, cleaner syntax


// ── Searching (binary search — REQUIRES sorted list) ──────────────
List<Integer> sorted = List.of(1, 3, 5, 7, 9);
int idx = Collections.binarySearch(sorted, 7);    // 3 (index)
int idx2 = Collections.binarySearch(sorted, 4);   // Negative: -(insertion point) - 1
// idx2 = -3 → insertion point = 2 (between 1 and 5)


// ── Min/Max ───────────────────────────────────────────────────────
Collections.min(list);                         // O(n) linear scan
Collections.max(list, Comparator.comparing(Object::toString));


// ── Frequency and disjoint ───────────────────────────────────────
Collections.frequency(list, 1);               // Count occurrences of 1
Collections.disjoint(list, List.of(10, 20));   // true if no common elements


// ── Shuffle and rotate ────────────────────────────────────────────
Collections.shuffle(list);                    // Random permutation
Collections.shuffle(list, new Random(42));    // Seeded (reproducible)
Collections.rotate(list, 2);                  // Rotate right by 2 positions
Collections.reverse(list);                    // Reverse in place
Collections.swap(list, 0, list.size()-1);     // Swap two elements
Collections.fill(list, 0);                    // Fill all with value
Collections.copy(dest, src);                  // Copy src into dest (dest.size() ≥ src.size())


// ── Unmodifiable wrappers ─────────────────────────────────────────
List<String> mutable = new ArrayList<>(List.of("a", "b", "c"));
List<String> immutable = Collections.unmodifiableList(mutable);
// immutable.add("d")   → UnsupportedOperationException at RUNTIME
// NOTE: Still mutable via original 'mutable' reference!
// mutable.add("d")   → modifies both! (same underlying list)

// ✅ For truly immutable: use List.of() / List.copyOf()
List<String> trueImmutable = List.copyOf(mutable);   // Full copy — independent


// ── Synchronized wrappers (prefer java.util.concurrent instead) ───
List<String> synced = Collections.synchronizedList(new ArrayList<>());
// Every method: synchronized on 'synced'
// But iteration is NOT thread-safe (must synchronize externally):
synchronized (synced) {
for (String s : synced) {   // Entire iteration must be synchronized
process(s);
}
}
// Prefer CopyOnWriteArrayList or ConcurrentHashMap for concurrent use


// ── Singleton and empty collections ──────────────────────────────
List<String>  singletonList = Collections.singletonList("only");   // Immutable, size=1
Map<K,V>      singletonMap  = Collections.singletonMap(key, val);  // Immutable, size=1
List<String>  emptyList     = Collections.emptyList();             // Immutable, size=0
Set<String>   emptySet      = Collections.emptySet();
Map<K,V>      emptyMap      = Collections.emptyMap();
// These are SINGLETONS — same instance returned every time — zero allocation!


// ── nCopies — fixed-size list of repeated values ─────────────────
List<String> fiveHellos = Collections.nCopies(5, "hello");
// ["hello", "hello", "hello", "hello", "hello"] — immutable
// Backed by single value + count (not 5 separate "hello" references)
// Use: initialize lists, testing, padding

Part 9: Java 9+ Immutable Collection Factories
java// Java 9: List.of(), Set.of(), Map.of()
// These are NOT Collections.unmodifiableList(new ArrayList<>())
// They are TRULY IMMUTABLE, internally optimized, and compact

// ── List.of() ────────────────────────────────────────────────────
List<String> list = List.of("a", "b", "c");
list.add("d");        // ❌ UnsupportedOperationException
list.set(0, "x");     // ❌ UnsupportedOperationException
list.get(0);          // ✅ "a"
list.contains("b");   // ✅ true

// Internal optimization: for 0-2 elements → specialized classes (no array!)
//   List.of()          → Collections.emptyList() equivalent
//   List.of("a")       → single-element specialized class
//   List.of("a","b")   → two-element specialized class
//   List.of("a","b","c") → array-backed ImmutableList
// Much more memory-efficient than new ArrayList<>(Arrays.asList(...))

// ❌ Does NOT allow nulls:
List.of("a", null, "c");   // NullPointerException!
// Use List.copyOf() or ArrayList if you need nulls

// ── Set.of() ─────────────────────────────────────────────────────
Set<String> set = Set.of("a", "b", "c");
// ❌ Duplicates throw immediately:
Set.of("a", "a");   // IllegalArgumentException at construction!

// Iteration ORDER: undefined and may change between JVM runs!
// Don't rely on Set.of() iteration order

// ── Map.of() and Map.ofEntries() ─────────────────────────────────
// Map.of(): up to 10 key-value pairs
Map<String, Integer> small = Map.of(
"one", 1, "two", 2, "three", 3
);

// Map.ofEntries(): any number of entries
Map<String, Integer> large = Map.ofEntries(
Map.entry("one",   1),
Map.entry("two",   2),
Map.entry("three", 3),
Map.entry("four",  4)
// ... unlimited
);

// ❌ Duplicate keys: IllegalArgumentException
Map.of("a", 1, "a", 2);   // Throws!

// ❌ Null keys or values: NullPointerException
Map.of("key", null);   // Throws!

// ── List.copyOf(), Set.copyOf(), Map.copyOf() ─────────────────────
List<String> original = new ArrayList<>(List.of("a", "b", "c"));
original.add("d");

List<String> snapshot = List.copyOf(original);   // Immutable snapshot
original.add("e");
snapshot.size();   // Still 4 — snapshot is independent of original


// ── When to use which ─────────────────────────────────────────────
// List.of(...)         : small, fixed, known-at-code-time collection
// List.copyOf(other)   : immutable snapshot of existing collection
// new ArrayList<>(...)  : you need to add/remove elements later
// Collections.unmodifiable...: wrapping existing mutable (BEWARE: not truly immutable)

Part 10: The Interview Q&A Round 🎤

Q1. How does HashMap handle collisions, and how did Java 8 improve it?

"HashMap uses chaining for collision resolution — when two keys hash to the same bucket, they're stored in a linked list at that bucket. Each node stores the cached hash, key, value, and a next pointer.
Before Java 8, if an attacker crafted input where all keys hash to the same bucket — which is entirely possible if they know your hash function — the entire HashMap degrades to O(n) per operation. That's a classic hash-flooding DoS attack.
Java 8 added treeification: when a single bucket's linked list reaches 8 nodes, and the overall table has at least 64 buckets, that linked list is converted to a Red-Black tree. This changes worst-case per-bucket from O(n) to O(log n). The tree converts back to a linked list when it shrinks to 6 nodes. Additionally, Java 8 improved the hash function by XOR-ing the high 16 bits into the low 16 bits, giving better bit distribution and reducing collisions in small tables.
The practical implication: HashMap is much more resilient to crafted inputs in Java 8+, but you should still use String or other well-distributed keys and pre-size maps when you know the expected entry count."


Q2. Why should HashMap keys be immutable?
java// Demonstrate the problem:
class MutableKey {
int id;
@Override public int hashCode() { return id; }
@Override public boolean equals(Object o) {
return o instanceof MutableKey mk && id == mk.id;
}
}

Map<MutableKey, String> map = new HashMap<>();
MutableKey k = new MutableKey();
k.id = 1;
map.put(k, "value");

k.id = 2;                  // Mutate key post-insertion
map.get(k);                // null — hashCode changed → wrong bucket
map.containsKey(k);        // false
map.size();                // 1 — entry still there, permanently lost!

// THE EXPLANATION:
// put(k) with id=1: hash=1, index=1&(n-1) → stores in bucket 1
// k.id=2: now k.hashCode()=2 → different bucket
// get(k): hash=2, index=2&(n-1) → looks in bucket 2 → finds nothing
// The entry is stuck in bucket 1 under hash=1
// No way to find or remove it — orphaned entry, memory leak

// BEST KEYS:
// String:         immutable, well-distributed hashCode
// Integer/Long:   immutable, hashCode=value (can't get more distinct)
// UUID:           immutable, effectively unique
// record:         auto-immutable (all fields final), auto-hashCode
// enum:           immutable, hashCode=ordinal, EnumMap is better anyway

Q3. What is the difference between HashMap, TreeMap, and LinkedHashMap? When do you use each?

"All three are Map implementations with very different characteristics. HashMap is a hash table — O(1) average for get/put/contains, no ordering guarantee. It's the right default choice for most use cases: you want fast lookups and don't care about order.
TreeMap is a Red-Black tree — O(log n) for all operations, but keys are maintained in SORTED order. The critical extra capability is the NavigableMap API: floorKey, ceilingKey, subMap, headMap, tailMap. Use TreeMap when you need to answer 'what's the next event after timestamp T?', 'all entries between price 100 and 200', or 'smallest key greater than X'. A classic application is a task scheduler keyed by execution time, or a grade calculator using floorEntry to find which grade bracket a score falls in.
LinkedHashMap is a HashMap plus a doubly-linked list threading through all entries. It preserves insertion order by default, making iteration predictable. In access-order mode (pass true as the third constructor argument), every get and put moves the accessed entry to the tail, so the head is always the least recently used entry. Override removeEldestEntry and you have a complete LRU cache in about 10 lines of code. Use LinkedHashMap when iteration order matters for correctness — like producing deterministic JSON output — or for LRU caching patterns."


Q4. How would you implement an LRU cache?
javaclass LRUCache<K, V> extends LinkedHashMap<K, V> {
private final int capacity;

    LRUCache(int capacity) {
        super(capacity, 0.75f, true);   // accessOrder = true
        this.capacity = capacity;
    }

    @Override
    protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
        return size() > capacity;
    }

    public V getOrDefault(K key, V defaultValue) {
        return super.getOrDefault(key, defaultValue);
    }
}

// How it works:
// accessOrder=true: every get/put → moves entry to tail (MRU end)
// head = LRU entry
// removeEldestEntry called after every put
// When size > capacity: removes head (LRU entry automatically)
// Time complexity: O(1) for get, put, and eviction
// Space complexity: O(capacity)

// Interview follow-up: "How would you make it thread-safe?"
// Option 1: Collections.synchronizedMap(new LRUCache<>(...)) — coarse lock
// Option 2: Caffeine library — state-of-the-art concurrent LRU, O(1) operations
// Option 3: ConcurrentLinkedHashMap (Google Guava) — concurrent LRU

// Interview follow-up: "Implement from scratch without LinkedHashMap"
class LRUCacheFromScratch {
private final int capacity;
private final Map<Integer, Node> map = new HashMap<>();
private final Node head = new Node(0, 0);  // Dummy head (LRU end)
private final Node tail = new Node(0, 0);  // Dummy tail (MRU end)

    LRUCacheFromScratch(int capacity) {
        this.capacity = capacity;
        head.next = tail; tail.prev = head;  // Initialize doubly linked list
    }

    int get(int key) {
        if (!map.containsKey(key)) return -1;
        Node node = map.get(key);
        moveToTail(node);    // Mark as recently used
        return node.val;
    }

    void put(int key, int val) {
        if (map.containsKey(key)) {
            Node node = map.get(key);
            node.val = val;
            moveToTail(node);
        } else {
            if (map.size() == capacity) {
                Node lru = head.next;    // LRU = node after dummy head
                removeNode(lru);
                map.remove(lru.key);
            }
            Node newNode = new Node(key, val);
            insertAtTail(newNode);
            map.put(key, newNode);
        }
    }

    private void removeNode(Node n) {
        n.prev.next = n.next; n.next.prev = n.prev;
    }
    private void insertAtTail(Node n) {
        n.prev = tail.prev; n.next = tail;
        tail.prev.next = n; tail.prev = n;
    }
    private void moveToTail(Node n) { removeNode(n); insertAtTail(n); }

    static class Node {
        int key, val;
        Node prev, next;
        Node(int k, int v) { key = k; val = v; }
    }
}
```

---

**Q5. When would you use PriorityQueue and what is its time complexity?**

> *"PriorityQueue is the right tool whenever you need to repeatedly extract the minimum or maximum from a dynamic set. The canonical pattern is 'top K' problems: maintain a min-heap of size K while scanning — every new element that's larger than the heap's minimum gets added, and the heap's minimum gets evicted to maintain K elements. After scanning all n elements, the heap contains the K largest, and the root is the Kth largest.*
>
> *Time complexities: `offer` and `poll` are O(log n) — they sift up or sift down through the heap. `peek` is O(1) — the root is always at index 0. `contains` and `remove(Object)` are O(n) — the heap has no ordering on arbitrary elements, only the parent-child relationship. Building a heap from n elements via `addAll` is O(n) (heapify algorithm, not n log n) — a common interview question.*
>
> *The most important interview patterns: top-K with a size-K heap in O(n log k), merge-K-sorted-lists in O(n log k), median from data stream with two complementary heaps in O(log n) per insert and O(1) for median, and Dijkstra's shortest path which uses a min-heap to always process the closest unvisited node."*

---

## Section 12 Master Summary 🧠
```
HASHMAP INTERNALS:
Backing:     Node<K,V>[] table — array of buckets
Bucket:      null | Node | linked list | Red-Black tree (≥8 nodes, table≥64)
Hash:        hashCode() ^ (hashCode() >>> 16) — spread high bits
Index:       hash & (n-1) — fast modulo for power-of-2 sizes
Resize:      at 75% capacity → double table, O(n) rehash, O(1) amortized
Treeify:     Java 8 — protects against hash-flooding O(n) attacks
Pre-size:    new HashMap<>((int)(expectedSize / 0.75) + 1)
Keys:        MUST be immutable — mutable keys silently lose entries

HASHCODE/EQUALS CONTRACT:
equals=true → MUST have same hashCode (or HashMap breaks)
All fields in equals MUST be in hashCode
Use Objects.hash(f1, f2, f3) for multi-field hashCode
Best keys: String, Integer, UUID, records, enums

TREEMAP:
Backing:     Red-Black tree
Operations:  O(log n) guaranteed — no amortized caveats
Sorted:      always in key order (natural or Comparator)
NavigableMap: floor/ceiling/higher/lower/subMap/headMap/tailMap
Use when:    need sorted order, range queries, nearest-key lookups

LINKEDHASHMAP:
Backing:     HashMap + doubly-linked list
Default:     insertion order
Access order: new LinkedHashMap<>(cap, 0.75f, true) → LRU semantics
LRU cache:   extend + override removeEldestEntry(eldest) → return size() > cap

ARRAYLIST vs LINKEDLIST:
ArrayList:   O(1) get, O(1) amortized add-to-end, O(n) insert/remove middle
contiguous memory → cache friendly → fast iteration
LinkedList:  O(n) get (traverse), O(1) insert IF you have the iterator
pointer-chasing → cache unfriendly → 10-100x slower iteration
Rule:        Use ArrayList for EVERYTHING. LinkedList almost never wins.
For queues:  Use ArrayDeque — faster than LinkedList for all queue ops

ARRAYDEQUE:
Circular array: O(1) amortized addFirst/addLast/pollFirst/pollLast
Cache friendly, no node allocation overhead
Best implementation for: Stack, Queue, Deque
Replaces: Stack class (synchronized = slow), LinkedList (cache misses)

PRIORITYQUEUE:
Backing:     binary min-heap (array representation)
offer/poll:  O(log n) — sift up / sift down
peek:        O(1) — array[0] is always minimum
contains:    O(n) — no ordering guarantee for arbitrary elements
Max-heap:    Comparator.reverseOrder()
Patterns:    top-K in O(n log k), merge-K-sorted in O(n log k),
median stream with two heaps

ENUM COLLECTIONS:
EnumSet:    bitmask operations O(1) — 64× faster than HashSet<Enum>
EnumMap:    array indexed by ordinal — faster than HashMap<Enum, V>
Always use EnumSet/EnumMap when keys are enums

IMMUTABLE FACTORIES (Java 9+):
List.of(...):        null-safe ❌, duplicates OK, TRULY immutable
Set.of(...):         null ❌, duplicates throw IllegalArgumentException
Map.of(k,v,...):     null ❌, duplicate keys throw, ≤10 pairs
Map.ofEntries(...):  unlimited entries
List.copyOf(c):      immutable snapshot, null ❌

COLLECTION SELECTION GUIDE:
Key-value, fast lookup           → HashMap
Key-value, sorted/range queries  → TreeMap
Key-value, insertion order       → LinkedHashMap
Key-value, enum keys             → EnumMap
Unique elements, fast lookup     → HashSet
Unique elements, sorted          → TreeSet
Unique elements, enum            → EnumSet
Indexed sequence, random access  → ArrayList
Queue/Stack/Deque                → ArrayDeque
Priority retrieval               → PriorityQueue
Thread-safe key-value            → ConcurrentHashMap (Section 7)
Thread-safe queue                → ArrayBlockingQueue (Section 5)