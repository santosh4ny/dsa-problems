Section 9: Garbage Collection ♻️
Section 8 gave you the JVM's memory architecture. Section 9 is about how the JVM reclaims that memory. GC is where production systems live or die — a misconfigured GC causes latency spikes, throughput drops, and 3am pages. FAANG interviewers use GC questions to find engineers who can reason about invisible pauses and fix them.

The Big Picture — What GC Actually Solves
Manual memory management (C/C++):
malloc() → allocate
free()   → deallocate manually
Forget to free() → memory leak → OOM
Free too early  → dangling pointer → crash / security vulnerability
Free twice      → heap corruption → undefined behavior
Programmer manages memory → catastrophic bugs

Java GC: automatic memory management
new Object() → JVM allocates
Object becomes unreachable → JVM reclaims automatically
No dangling pointers, no double frees, no use-after-free
Cost: GC pauses, CPU overhead, non-deterministic timing

The fundamental question GC must answer:
"Is this object still reachable from the program?"
If YES → keep it
If NO  → it's garbage, reclaim the memory

How GC answers this: REACHABILITY ANALYSIS (not reference counting)

Part 1: Reachability — How GC Finds Garbage
GC Roots — The Starting Points
java// GC traces from ROOTS — objects that are ALWAYS considered reachable
// Everything reachable FROM roots is kept. Everything else is garbage.

// THE 4 GC ROOTS:
// 1. Active thread stacks: local variables in every live stack frame
// 2. Static variables: all class-level static references
// 3. JNI references: objects referenced from native (C/C++) code
// 4. Synchronized monitors: objects used as locks in synchronized blocks

// Tracing example:
static Map<String, User> sessionCache = new HashMap<>(); // GC Root (static)
//   → HashMap object    REACHABLE (referenced by static)
//     → Entry[] array   REACHABLE (referenced by HashMap)
//       → Entry objects REACHABLE (referenced by array)
//         → User objects REACHABLE (referenced by entries)

void processRequest() {
Order order = new Order();           // Local var → GC Root (stack)
order.setUser(sessionCache.get(id)); // order reachable, User reachable
}                                        // Method returns → order off stack
// order eligible for GC if no other refs


// Reference chain example:
class LeakExample {
static List<byte[]> leaked = new ArrayList<>();  // GC Root

    void allocate() {
        leaked.add(new byte[1024 * 1024]);  // 1MB → always reachable via static
        // These are NEVER collected — they're reachable from GC root
        // This IS a memory leak: objects reachable but logically unnecessary
    }
}

// Key insight:
// Memory leak in Java ≠ "object not freed"
// Memory leak in Java = "object REACHABLE but never USED again"
// Unreachable objects are ALWAYS collected
// The bug is: keeping references you don't need → objects stay reachable
Reference Types — Controlling Reachability
javaimport java.lang.ref.*;

// ── Strong Reference (default) ────────────────────────────────────
Object obj = new Object();   // Strong reference
// GC will NEVER collect obj as long as this reference exists
// Only collected when: reference set to null, goes out of scope,
//                      or object becomes otherwise unreachable


// ── SoftReference — collect when memory pressure is high ──────────
SoftReference<byte[]> softRef = new SoftReference<>(new byte[1024 * 1024]);

byte[] data = softRef.get();  // Returns object or null if GC'd
if (data == null) {
data = reloadData();       // Reload if GC'd
softRef = new SoftReference<>(data);
}
// GC collects ONLY when JVM is low on memory (before OOM)
// Use for: memory-sensitive caches — JVM manages cache size automatically
// JVM guarantees: all SoftReferences cleared before OOM thrown

// Soft cache pattern:
class SoftCache<K, V> {
private final Map<K, SoftReference<V>> cache = new ConcurrentHashMap<>();

    public V get(K key, Supplier<V> loader) {
        SoftReference<V> ref = cache.get(key);
        V value = (ref != null) ? ref.get() : null;

        if (value == null) {
            value = loader.get();              // Load (or reload after GC)
            cache.put(key, new SoftReference<>(value));
        }
        return value;
    }
}


// ── WeakReference — collect at NEXT GC regardless ─────────────────
WeakReference<Object> weakRef = new WeakReference<>(new Object());

// At next GC: object collected (if no strong references elsewhere)
Object obj2 = weakRef.get();  // Returns null if already collected

// Use for: canonicalization maps, object metadata without preventing GC

// WeakHashMap — keys are weakly referenced
WeakHashMap<Object, String> weakMap = new WeakHashMap<>();
Object key = new Object();
weakMap.put(key, "metadata");

key = null;          // Remove strong reference to key
System.gc();         // Hint to GC (not guaranteed, but for demonstration)
// weakMap.get(key) → entry automatically removed when key GC'd
// Use for: attaching metadata to objects without controlling their lifecycle
// Classic use: class-level instrumentation, aspect metadata


// ── PhantomReference — post-mortem notification ───────────────────
ReferenceQueue<Object> queue = new ReferenceQueue<>();
Object target = new Object();
PhantomReference<Object> phantomRef = new PhantomReference<>(target, queue);

target = null;  // Remove strong reference

// When GC finalizes object → enqueues phantomRef in queue
// phantomRef.get() ALWAYS returns null (can't resurrect)

// Monitor queue for cleanup:
new Thread(() -> {
while (true) {
try {
Reference<?> ref = queue.remove();  // Blocks until available
// Object associated with ref has been finalized
cleanupNativeResources(ref);        // Release native memory, file handles
} catch (InterruptedException e) {
Thread.currentThread().interrupt();
return;
}
}
}).start();

// Use for: Cleaner API (Java 9+) — better than finalize()
// Java 9+: java.lang.ref.Cleaner replaces PhantomReference boilerplate


// ── Reference Summary ─────────────────────────────────────────────
// Strong:  → never GC'd while referenced (default)
// Soft:    → GC'd before OOM (memory-sensitive caches)
// Weak:    → GC'd at next GC cycle (metadata, canonicalization)
// Phantom: → GC'd, then enqueued for cleanup notification (resource cleanup)

// Strength: Strong > Soft > Weak > Phantom
// Reachability: if object only has Soft refs → "softly reachable"
//               if object only has Weak refs → "weakly reachable"
//               if object only has Phantom refs → "phantom reachable"
```

---

## Part 2: Generational GC — The Core Insight

### The Generational Hypothesis
```
Empirical observation across millions of Java programs:

"MOST objects die young"

Objects created in a loop body → dead by end of loop
StringBuilder used to build a string → dead after toString()
HTTP request objects → dead after request processing
Stream intermediates → dead after terminal operation

Measurement: 90-98% of objects die before their first GC

IMPLICATION: optimize for the common case
→ Separate young objects from old objects
→ Collect young objects FREQUENTLY and CHEAPLY (Minor GC)
→ Collect old objects RARELY (Major GC)
→ Don't scan old objects when doing young-only collection

This is why: Minor GC is FAST (milliseconds)
Major GC is SLOW (hundreds of milliseconds)
```

### Minor GC — Young Generation Collection
```
BEFORE Minor GC:
Eden: [A][B][C][D][E][F][G] ← FULL
S0:   [X][Y] ← survivors from last GC
S1:   [] ← empty

GC traces from ROOTS:
Reachable from roots: A, C, E (in Eden) + X (in S0)
Dead objects: B, D, F, G (in Eden) + Y (in S0)

DURING Minor GC (copying/evacuation):
1. Scan Eden for live objects → copy A, C, E to S1
2. Scan S0 for live objects → copy X to S1 (if age < threshold)
   OR: X.age >= 15 → promote X to Old Gen
3. Increment age of A, C, E, X (age++)
4. CLEAR entire Eden and S0 (dead objects vanish — no explicit deallocation!)

AFTER Minor GC:
Eden: [] ← empty, ready for new allocations
S0:   [] ← empty (will receive next Minor GC's survivors)
S1:   [A][C][E][X] ← survivors (age 1 for A,C,E; age+1 for X)
Old:  [...][X if promoted]

COST:
Only scans LIVE objects in Young Gen (not dead ones!)
Dead objects cost nothing — just clear the space
Typical duration: 5-50ms
Typical frequency: every 5-30 seconds (depends on allocation rate)

STOP-THE-WORLD (STW):
ALL application threads PAUSE during Minor GC
Necessary: GC needs a consistent heap view — can't have threads
creating/modifying objects while GC is scanning
Major GC and Full GC
java// MAJOR GC: collects Old Generation
// Triggered when: Old Gen fills up
// Much more expensive: large space, many live objects to scan

// Algorithms for Old Gen:
//   Serial GC:    single-threaded sweep — ancient, for tiny heaps
//   Parallel GC:  multi-threaded — throughput focused
//   CMS:          concurrent sweep — low pause, deprecated Java 9, removed Java 14
//   G1:           regional, concurrent — default Java 9+
//   ZGC/Shenandoah: nearly all concurrent — sub-millisecond pauses

// FULL GC: collects BOTH Young AND Old generation
// Triggered by:
//   1. Explicit System.gc() call (❌ never do this in production)
//   2. Concurrent GC can't keep up with allocation rate
//   3. Old Gen is full (promotion failure)
//   4. Metaspace is full
//   5. JVM decides it needs to compact heap
// Full GC is the WORST: long STW pause (seconds for large heaps)

// How to see GC activity:
// -Xlog:gc*:file=gc.log:time:filecount=5,filesize=20m
// GC log line example:
// [2024-01-15T10:30:45.123+0000] GC(42) Pause Young (Normal) (G1 Evacuation Pause)
//   35.2ms                                                              ↑ pause!
// [2024-01-15T10:31:15.456+0000] GC(43) Pause Full (Ergonomics)
//   2847.6ms  ← 2.8 SECOND Full GC pause!
```

---

## Part 3: GC Algorithms — Every Collector Explained

### Serial GC (Ancient, Educational)
```
Single-threaded — does everything sequentially
-XX:+UseSerialGC

Young Gen: Copy algorithm (Eden → Survivors)
Old Gen:   Mark-Compact algorithm

Mark-Compact:
1. MARK: traverse from roots, mark all reachable objects
2. COMPACT: slide live objects to one end of Old Gen
3. Update all references to point to new locations

Pros: simple, minimal overhead per collection
Cons: single thread → CPU waste on multi-core
long STW pauses (all work happens during pause)

Use: single-core machines, very small heaps (<256MB)
Don't use in production server JVMs
```

### Parallel GC (Throughput Collector)
```
Multi-threaded — uses all CPU cores for GC
-XX:+UseParallelGC  (default before Java 9)

Young Gen: Parallel copy (multiple threads evacuate simultaneously)
Old Gen:   Parallel Mark-Compact

Goal: MAXIMIZE THROUGHPUT
Minimize total time spent in GC as fraction of total time
Acceptable: long individual pauses, if total pause time is low

Typical: 100-500ms pauses, every 30-120 seconds
Trade-off: high throughput, but pauses are noticeable

Best for:
Batch jobs: data processing, ETL, report generation
Where: throughput matters more than any individual pause
Where: pauses of 200ms are acceptable (no human interaction)

NOT for: interactive applications, APIs with SLA < 500ms
```

### G1 GC — The Default (Java 9+)
```
G1 = Garbage First
-XX:+UseG1GC  (default since Java 9)

KEY INSIGHT: Don't treat heap as Young/Old monolithic regions
Split heap into ~2048 equal REGIONS (1-32MB each)
Each region is DYNAMICALLY assigned role: Eden/Survivor/Old/Humongous

HEAP LAYOUT:
┌───┬───┬───┬───┬───┬───┬───┬───┬───┬───┬───┬───┬───┬───┬───┬───┐
│ E │ E │ O │ S │ E │ O │ O │ H │ H │ E │ O │ S │ E │ E │ O │   │
└───┴───┴───┴───┴───┴───┴───┴───┴───┴───┴───┴───┴───┴───┴───┴───┘
E=Eden  S=Survivor  O=Old  H=Humongous(large objects)  =Free

KEY FEATURE: Predictable pause target
-XX:MaxGCPauseMillis=200  (default 200ms)
G1 tries to stay within this budget by:
→ Choosing which regions to collect (prioritize highest garbage %)
→ "Garbage First" = collect regions with most garbage first
→ If 200ms budget allows collecting 50 regions, collect 50
→ Leave densely-live regions for later

G1 COLLECTION CYCLE:

Phase 1: MINOR/YOUNG GC (STW, fast)
→ Evacuate Eden + Survivor regions
→ Live objects copied to new Survivor/Old regions
→ STW pause: 10-100ms

Phase 2: CONCURRENT MARKING (mostly concurrent, no STW)
→ While application runs: trace object graph
→ Mark all reachable objects in Old Gen
→ Uses snapshot-at-the-beginning (SATB) algorithm
→ Application keeps allocating — G1 tracks new allocations

Phase 3: MIXED GC (STW, bounded)
→ Collect Young Gen + SELECTED Old Gen regions
→ Selects Old regions with most garbage (Garbage First!)
→ Bounded by MaxGCPauseMillis budget
→ Repeats until enough Old Gen space reclaimed

Phase 4: FULL GC (STW, emergency fallback)
→ If concurrent marking can't keep up with allocation
→ Single-threaded full heap collection (parallel in Java 10+)
→ AVOID AT ALL COSTS — indicates tuning needed
G1 Tuning — The Critical Flags
java// ── Primary knob: pause time goal ────────────────────────────────
// -XX:MaxGCPauseMillis=200        (default 200ms)
// Lower = more frequent smaller pauses
// Higher = less frequent longer pauses
// G1 tries but CANNOT GUARANTEE this — it's a goal, not a limit

// For interactive APIs: 100-200ms
// For latency-sensitive: 50-100ms
// For batch systems: 500-1000ms (higher throughput)


// ── Heap sizing ───────────────────────────────────────────────────
// -Xms4g -Xmx4g          Set min = max to prevent resize pauses
// Why equal: if heap needs to grow → GC must first try to free space
//            → extra Full GC just for heap sizing
//            Fixed size → no resize GCs

// -XX:InitiatingHeapOccupancyPercent=45  (default 45%)
// When Old Gen reaches 45% full → start concurrent marking
// Lower = more frequent marking → more CPU but avoid Full GC
// Higher = less marking → risk of Full GC if marking finishes too late
// If seeing Full GCs: lower IHOP to 35-40%


// ── Region sizing ─────────────────────────────────────────────────
// -XX:G1HeapRegionSize=8m      (auto-calculated, 1-32MB)
// Rule: 2048 regions target. 8GB heap → 8GB/2048 = 4MB regions
// Large objects (>= 0.5 × region size) → Humongous regions
// Humongous objects: bad! Allocated directly in Old Gen, collected only in
//                   concurrent cycle or Full GC
// Fix humongous: increase region size OR avoid large allocations

// Diagnosing humongous allocations:
// -XX:+G1PrintRegionLivenessInfo
// Look for: "Humongous" regions in GC log


// ── Survivor tuning ───────────────────────────────────────────────
// -XX:MaxTenuringThreshold=15      (default 15)
// Object survives 15 Minor GCs → promoted to Old Gen
// Lower → faster promotion → Old Gen fills faster → more concurrent marking
// Higher → objects stay in Young Gen longer → more Minor GC overhead
// -XX:+PrintTenuringDistribution   See age distribution of survivors


// ── G1 GC diagnostic flags ───────────────────────────────────────
String g1Flags = String.join(" ",
"-XX:+UseG1GC",
"-Xms8g", "-Xmx8g",                           // Fixed heap
"-XX:MaxGCPauseMillis=200",                    // Pause goal
"-XX:InitiatingHeapOccupancyPercent=40",       // Start marking earlier
"-XX:G1HeapRegionSize=8m",                     // Region size
"-XX:+G1UseAdaptiveIHOP",                      // Auto-tune IHOP (Java 9+)
"-Xlog:gc*:file=gc.log:time:filecount=5,filesize=20m"  // GC logging
);
```

### ZGC — Sub-Millisecond Pauses (Java 15+ Production Ready)
```
ZGC = Z Garbage Collector
-XX:+UseZGC

DESIGN GOAL: pause < 1ms regardless of heap size (up to 16TB!)
TECHNIQUE: Do ALMOST EVERYTHING concurrently with application threads

KEY INNOVATION: Colored Pointers + Load Barriers

Colored Pointers:
Object references encode GC metadata IN THE POINTER BITS
64-bit pointer has spare bits: use for GC state flags
Bits: [GC metadata 4 bits][object address 42 bits] = up to 4TB
Flags: Marked0, Marked1, Remapped, Finalizable
JVM reads these flags → knows if reference needs updating
Without colored pointers: need to scan all objects to find outdated refs
With colored pointers: each reference tells GC its own state

Load Barrier:
Code injected by JIT at EVERY object reference load:
Object ref = obj.field;  // becomes:
Object ref = obj.field;
if (ref.needsFixup()) {  // injected by JIT
ref = fixup(ref);    // remap to new location
}

This is how concurrent compaction works:
GC moves object to new location
DOESN'T immediately update all references (too slow)
When ANY thread loads the reference: load barrier fixes it on the fly
Application threads do GC's reference-fixing work for it!

ZGC PAUSE POINTS (only 2):
1. Initial Mark: scan GC roots → pause 0.5-1ms
2. Final Mark: process new references found concurrently → pause 0.5-1ms

Everything else: concurrent with application threads
Concurrent: marking, relocation, reference updating

ZGC THROUGHPUT COST:
Load barriers on every reference load: ~5-15% throughput overhead
Application does some GC work via load barriers
Worth it for latency-sensitive applications

BEST FOR:
→ APIs with strict latency SLAs (< 10ms p99)
→ Real-time applications: trading, gaming, auctions
→ Large heaps where G1 pauses are too long
→ Where 5-15% throughput reduction is acceptable
```

### Shenandoah GC — Concurrent Compaction
```
Shenandoah: concurrent compaction (different approach from ZGC)
-XX:+UseShenandoahGC

KEY TECHNIQUE: Brooks Forwarding Pointers
Every object gets an EXTRA field: forwarding pointer
Initially points to object itself (self-reference)
When object relocated: forwarding pointer → new location
Any access through old reference: follows forwarding pointer → transparent

DIFFERENCE FROM ZGC:
ZGC:        colored pointers (metadata in reference bits)
Shenandoah: forwarding pointer (extra field in each object)

ZGC: lower overhead per access (bits already in pointer)
Shenandoah: higher overhead per access (extra indirection) but simpler JVM integration
Shenandoah: available in OpenJDK distributions including Red Hat builds

PAUSE PHASES: ~1-5ms (similar to ZGC but slightly higher)
THROUGHPUT OVERHEAD: 5-10%

CHOOSING ZGC vs SHENANDOAH:
Both: sub-10ms pauses, concurrent compaction
ZGC: lower overhead per access (JDK 15+ production)
Shenandoah: available in older JDKs, RedHat-maintained
JDK 17+: ZGC is typically preferred
Test both with YOUR workload — numbers vary by application
```

### GC Algorithm Comparison
```
┌─────────────────┬──────────────┬────────────────┬──────────────────┐
│ Collector       │ Max Pause    │ Throughput     │ Best For         │
├─────────────────┼──────────────┼────────────────┼──────────────────┤
│ Serial          │ Seconds      │ Low            │ Tiny heaps, test │
│ Parallel        │ 100ms-1s     │ HIGHEST        │ Batch processing │
│ G1 (default)    │ 50-200ms     │ Good           │ Most workloads   │
│ ZGC             │ < 1ms        │ -5 to -15%     │ Latency critical │
│ Shenandoah      │ 1-5ms        │ -5 to -10%     │ Latency critical │
└─────────────────┴──────────────┴────────────────┴─────────────────-┘

DECISION GUIDE:
Batch jobs, ETL, data processing → Parallel GC (max throughput)
Web apps, microservices, APIs    → G1 GC (good balance, default)
Trading, gaming, real-time       → ZGC or Shenandoah (< 1ms pauses)
Heap > 32GB, latency matters     → ZGC (scales to TB without pause growth)

Part 4: Memory Leaks — The Real Patterns
java// Java memory leaks = objects REACHABLE but NEVER USED AGAIN
// GC can't collect them — they're still referenced
// Heap fills up → Full GC → OOM

// ── Leak Pattern 1: Static Collection That Grows Forever ──────────
class CacheManager {
// ❌ Cache that never evicts — memory leak!
private static final Map<String, byte[]> cache = new HashMap<>();

    public static void cache(String key, byte[] data) {
        cache.put(key, data);  // Added but NEVER removed
    }
    // Over time: cache grows → heap fills → Full GC → OOM
}

// ✅ Fix: Use a cache with eviction policy
private static final Map<String, byte[]> cache =
Collections.synchronizedMap(new LinkedHashMap<String, byte[]>(1000, 0.75f, true) {
@Override
protected boolean removeEldestEntry(Map.Entry<String, byte[]> eldest) {
return size() > 1000;  // LRU eviction at 1000 entries
}
});

// Production: use Caffeine
Cache<String, byte[]> cache = Caffeine.newBuilder()
.maximumSize(10_000)
.expireAfterWrite(10, TimeUnit.MINUTES)
.build();


// ── Leak Pattern 2: Listeners Never Removed ───────────────────────
class EventSystem {
private static final List<EventListener> listeners = new ArrayList<>();

    public static void register(EventListener l) {
        listeners.add(l);          // Strong reference in static list
    }

    // ❌ NO deregister method!
    // Every listener registered → held forever by static list
    // Listener objects → their referents → entire object graphs → never GC'd
}

// ✅ Fix 1: Always provide deregister
public static void deregister(EventListener l) {
listeners.remove(l);
}
// Caller must remember to call deregister (AutoCloseable pattern)

// ✅ Fix 2: Use WeakReference — listeners collected when no other reference exists
private static final List<WeakReference<EventListener>> listeners = new ArrayList<>();

public static void register(EventListener l) {
listeners.add(new WeakReference<>(l));
}

public static void notifyAll(Event e) {
listeners.removeIf(ref -> {
EventListener l = ref.get();
if (l == null) return true;   // Already GC'd — remove dead ref
l.onEvent(e);
return false;
});
}


// ── Leak Pattern 3: ThreadLocal Not Cleaned Up ────────────────────
class RequestProcessor {
private static final ThreadLocal<RequestContext> context = new ThreadLocal<>();

    public void process(Request req) {
        context.set(new RequestContext(req));  // Set for this thread
        try {
            doWork();
        } finally {
            context.remove();  // ✅ CRITICAL: remove in finally!
        }
    }

    // ❌ Without remove():
    // Thread returns to pool → ThreadLocal still holds RequestContext
    // Thread processes next request → old RequestContext still attached
    // 1000 threads × 100KB RequestContext = 100MB permanent leak
    // Worse: data from old request visible in new request → DATA LEAK!
}

// Why ThreadLocal leaks in thread pools:
// Thread pool threads NEVER die → ThreadLocal entries NEVER cleared automatically
// ThreadLocal uses the Thread object as implicit key
// Thread stays alive → ThreadLocal value stays alive


// ── Leak Pattern 4: Inner Class Holding Outer Reference ──────────
class Activity {
private byte[] largeData = new byte[10 * 1024 * 1024]; // 10MB

    // ❌ Non-static inner class: holds implicit reference to outer Activity
    class DataTask implements Runnable {
        @Override
        public void run() {
            // Even if it doesn't USE largeData...
            processData();
        }
    }

    void scheduleTask() {
        executor.submit(new DataTask());  // DataTask submitted to long-lived executor
        // DataTask holds ref to Activity → Activity holds 10MB largeData
        // Activity can't be GC'd as long as DataTask is alive or queued!
    }
}

// ✅ Fix: static nested class (no implicit outer reference)
static class DataTask implements Runnable {
private final byte[] data;  // Only hold what you actually need

    DataTask(byte[] data) {
        this.data = data;       // Explicit, minimal reference
    }

    @Override
    public void run() { processData(data); }
}

void scheduleTask() {
executor.submit(new DataTask(largeData));
// DataTask holds only largeData ref (what it actually needs)
// Activity object can be GC'd independently
}


// ── Leak Pattern 5: Unclosed Resources ───────────────────────────
// ❌ Not a heap leak per se, but native memory / file descriptor leak
class ResourceLeak {
void openAndLeak() throws Exception {
InputStream is = new FileInputStream("file.txt");
// Exception thrown here → is.close() never called
processStream(is);
is.close();   // Never reached if processStream throws
}
}

// ✅ Fix: try-with-resources (calls close() in generated finally)
void openSafely() throws Exception {
try (InputStream is = new FileInputStream("file.txt")) {
processStream(is);
}  // is.close() guaranteed — even on exception
}


// ── Leak Pattern 6: HashMap with Mutable Keys ────────────────────
class MutableKeyLeak {
Map<List<Integer>, String> map = new HashMap<>();

    void leak() {
        List<Integer> key = new ArrayList<>(Arrays.asList(1, 2, 3));
        map.put(key, "value");

        key.add(4);          // ❌ Mutate key AFTER insertion!
        // Key's hashCode changed → it's in the WRONG bucket
        // map.get(key)   → can't find it (looks in bucket for new hashCode)
        // map.remove(key)→ can't remove it → entry lives in map FOREVER
        // Object: permanently unreachable but strongly reachable → LEAK
    }
}
// Fix: ALWAYS use immutable keys (String, Integer, record)


// ── Detecting Memory Leaks ────────────────────────────────────────
// 1. Heap dump analysis (Eclipse MAT)
//    jcmd <pid> GC.heap_dump /tmp/heap.hprof
//    Open in MAT → "Leak Suspects Report" → shows largest retained sets

// 2. Memory profilers (continuous monitoring)
//    JFR (Java Flight Recorder) + JMC (Java Mission Control)
//    jcmd <pid> JFR.start duration=60s filename=recording.jfr
//    Opens in JMC → Memory → Allocation Profiling → find hotspots

// 3. GC log analysis
//    If heap usage after each Full GC keeps increasing → leak!
//    After GC: heap should return to stable baseline
//    Baseline rising over time = leak

// 4. Heap usage monitoring (simple)
MemoryMXBean memBean = ManagementFactory.getMemoryMXBean();
MemoryUsage heapUsage = memBean.getHeapMemoryUsage();
log.info("Heap: used={}MB, committed={}MB, max={}MB",
heapUsage.getUsed() / 1024 / 1024,
heapUsage.getCommitted() / 1024 / 1024,
heapUsage.getMax() / 1024 / 1024);
// Collect this every minute → plot it
// Steadily rising "used after GC" = memory leak

Part 5: Finalization and Cleaners
java// ── finalize() — DEPRECATED, NEVER USE ───────────────────────────
class BadFinalize {
@Override
protected void finalize() throws Throwable {
// ❌ DON'T USE finalize():
// 1. No guaranteed timing — may never run before JVM exit
// 2. Resurrect objects: if finalize() stores 'this' in a static
//    → object becomes reachable again → GC doesn't collect!
// 3. Causes object to survive EXTRA GC cycle
//    → promoted to Old Gen unnecessarily
// 4. Finalization queue is single-threaded → bottleneck
// 5. Exceptions in finalize() are SILENTLY IGNORED
closeResources();  // ← will this run? When? Unknown.
super.finalize();
}
}

// ✅ Use try-with-resources + AutoCloseable instead
class GoodResource implements AutoCloseable {
private final NativeResource nativeResource;

    GoodResource() {
        this.nativeResource = allocateNativeResource();
    }

    public void doWork() {
        checkOpen();
        nativeResource.use();
    }

    @Override
    public void close() {
        nativeResource.free();  // Deterministic cleanup
    }
}

try (GoodResource r = new GoodResource()) {
r.doWork();
}  // r.close() guaranteed — DETERMINISTIC timing


// ✅ Java 9+: Cleaner API for native resource cleanup
import java.lang.ref.Cleaner;

class NativeWrapper {
private static final Cleaner cleaner = Cleaner.create();

    private final NativeHandle handle;
    private final Cleaner.Cleanable cleanable;

    NativeWrapper() {
        this.handle    = NativeHandle.allocate();

        // Register cleanup action — runs when NativeWrapper is GC'd
        // MUST use static class or lambda that doesn't capture 'this'!
        // (capturing 'this' would prevent GC)
        this.cleanable = cleaner.register(this, new CleanupAction(handle));
    }

    // Static — does NOT capture NativeWrapper reference
    private static class CleanupAction implements Runnable {
        private final NativeHandle handle;
        CleanupAction(NativeHandle h) { this.handle = h; }

        @Override
        public void run() {
            handle.free();   // Runs when NativeWrapper is GC'd
            log.info("Native handle freed by Cleaner");
        }
    }

    // Explicit close (preferred — deterministic)
    public void close() {
        cleanable.clean();   // Runs action immediately + won't run again on GC
    }
}

Part 6: GC Tuning — Production Methodology
java// STEP 1: Identify your problem
//
// Parse GC log for:
//   Total GC time fraction:  if > 5% → GC overhead issue
//   Individual pause times:  if > SLA → latency issue
//   Full GC frequency:       if > once/hour → tuning needed
//   Heap usage after GC:     if rising → memory leak

// GC log analysis commands:
// grep "Pause Full" gc.log | awk '{print $NF}' | sort -n | tail -20
//   → Shows 20 longest Full GC pauses

// grep "Pause Young" gc.log | awk '{print $NF}' | sort -n | tail -20
//   → Shows 20 longest Minor GC pauses


// STEP 2: Match symptoms to causes

// ── Symptom: Frequent Full GCs ────────────────────────────────────
// Cause A: Heap too small → increase -Xmx
// Cause B: Memory leak → heap dump, find leak
// Cause C: IHOP too high → lower -XX:InitiatingHeapOccupancyPercent
// Cause D: Humongous allocations → increase G1HeapRegionSize
// Cause E: High allocation rate → optimize code to allocate less

// ── Symptom: Minor GC pauses too long (> 50ms) ────────────────────
// Cause: Too many live objects in Young Gen
// Fix A: Increase Young Gen size (-XX:NewRatio or -Xmn)
// Fix B: Increase survivor spaces to reduce premature promotion
// Fix C: Reduce object lifetime (refactor code to create fewer objects)

// ── Symptom: API p99 latency spikes at GC time ───────────────────
// Cause: STW GC pauses visible in latency percentiles
// Fix A: Switch to ZGC/Shenandoah for < 1ms pauses
// Fix B: Lower MaxGCPauseMillis
// Fix C: If p99 spike = Full GC → fix Full GC root cause first

// ── Symptom: High CPU usage during GC ─────────────────────────────
// Cause: GC threads consuming CPU → competing with app threads
// Fix A: Reduce GC frequency (reduce allocation rate)
// Fix B: Reduce -XX:ConcGCThreads if GC stealing too much CPU
// Fix C: Switch to Parallel GC if throughput > latency (batch jobs)


// STEP 3: Tuning cheatsheet

// ── G1 production baseline ────────────────────────────────────────
String g1Production = String.join("\n",
// Heap: fixed size, equal min=max
"-Xms8g -Xmx8g",

    // GC selection
    "-XX:+UseG1GC",

    // Pause target
    "-XX:MaxGCPauseMillis=200",

    // Start concurrent marking earlier (avoid Full GC)
    "-XX:InitiatingHeapOccupancyPercent=40",

    // Adaptive IHOP (Java 9+): JVM auto-tunes IHOP
    "-XX:+G1UseAdaptiveIHOP",

    // Region size (tune based on heap size and object sizes)
    "-XX:G1HeapRegionSize=8m",

    // Survivor tuning
    "-XX:MaxTenuringThreshold=6",

    // GC threads (default = 1/4 of CPUs for concurrent phases)
    "-XX:ConcGCThreads=4",

    // Logging (Java 9+)
    "-Xlog:gc*:file=/var/log/gc.log:time,uptime:filecount=5,filesize=20m"
);


// ── ZGC production baseline ───────────────────────────────────────
String zcgProduction = String.join("\n",
"-Xms16g -Xmx16g",
"-XX:+UseZGC",
// ZGC mostly self-tunes — minimal flags needed
// Key option: soft max heap (ZGC can return memory to OS)
"-XX:SoftMaxHeapSize=12g",  // Try to stay under 12GB, allow up to 16GB
"-Xlog:gc*:file=/var/log/gc.log:time:filecount=5,filesize=20m"
);


// STEP 4: Monitoring in production
class GCMonitor {
public void setupGCNotifications() {
List<GarbageCollectorMXBean> gcBeans =
ManagementFactory.getGarbageCollectorMXBeans();

        for (GarbageCollectorMXBean gcBean : gcBeans) {
            NotificationEmitter emitter = (NotificationEmitter) gcBean;
            emitter.addNotificationListener((notification, handback) -> {
                if (!notification.getType().equals(
                        GarbageCollectionNotificationInfo.GARBAGE_COLLECTION_NOTIFICATION))
                    return;

                GarbageCollectionNotificationInfo info =
                    GarbageCollectionNotificationInfo.from(
                        (CompositeData) notification.getUserData());

                String gcName   = info.getGcName();
                String gcCause  = info.getGcCause();
                long   duration = info.getGcInfo().getDuration();
                long   usedBefore = info.getGcInfo()
                    .getMemoryUsageBeforeGc()
                    .values().stream()
                    .mapToLong(MemoryUsage::getUsed).sum() / 1024 / 1024;
                long   usedAfter = info.getGcInfo()
                    .getMemoryUsageAfterGc()
                    .values().stream()
                    .mapToLong(MemoryUsage::getUsed).sum() / 1024 / 1024;

                log.info("GC: name={} cause={} duration={}ms before={}MB after={}MB freed={}MB",
                    gcName, gcCause, duration, usedBefore, usedAfter,
                    usedBefore - usedAfter);

                // Alert if Full GC
                if (gcName.contains("Full") || duration > 1000) {
                    log.error("SLOW GC ALERT: {} took {}ms", gcName, duration);
                    metrics.counter("gc.full").increment();
                }

                metrics.timer("gc.duration", "gc", gcName)
                    .record(duration, TimeUnit.MILLISECONDS);
                metrics.gauge("gc.heap.after", usedAfter);

            }, null, null);
        }
    }
}

Part 7: Allocation Optimization — Reducing GC Pressure
java// Less allocation = less GC = lower latency

// ── Object Pooling ────────────────────────────────────────────────
// Reuse expensive-to-create objects instead of allocating new ones

class ByteBufferPool {
private final Deque<ByteBuffer> pool = new ConcurrentLinkedDeque<>();
private final int               bufferSize;
private final int               maxPoolSize;
private final AtomicInteger     poolSize = new AtomicInteger(0);

    ByteBufferPool(int bufferSize, int maxPoolSize) {
        this.bufferSize  = bufferSize;
        this.maxPoolSize = maxPoolSize;
    }

    public ByteBuffer acquire() {
        ByteBuffer buf = pool.pollFirst();
        if (buf != null) {
            buf.clear();    // Reset position/limit for reuse
            return buf;
        }
        return ByteBuffer.allocateDirect(bufferSize);  // Allocate if pool empty
    }

    public void release(ByteBuffer buf) {
        if (poolSize.get() < maxPoolSize) {
            pool.addFirst(buf);
            poolSize.incrementAndGet();
        }
        // If pool full: let buf be GC'd — pool is big enough
    }
}

// Use:
ByteBuffer buf = pool.acquire();
try {
processData(buf);
} finally {
pool.release(buf);  // ALWAYS return to pool
}


// ── StringBuilder reuse ───────────────────────────────────────────
// ❌ Creates new StringBuilder every call
String buildMessage(String user, int code, String detail) {
return new StringBuilder()
.append("[").append(user).append("] ")
.append(code).append(": ")
.append(detail)
.toString();
}

// ✅ Reuse via ThreadLocal (StringBuilder is NOT thread-safe)
private static final ThreadLocal<StringBuilder> SB =
ThreadLocal.withInitial(() -> new StringBuilder(256));

String buildMessageFast(String user, int code, String detail) {
StringBuilder sb = SB.get();
sb.setLength(0);  // Reset without deallocating internal buffer
return sb.append("[").append(user).append("] ")
.append(code).append(": ")
.append(detail)
.toString();
}


// ── Value-Based Objects (Java 16+ records, Java 21 value objects) ──
// Records: no behavior, pure data, naturally immutable
record Point(double x, double y) {}
record Interval(long start, long end) {}

// Future: Java project Valhalla value types
// Value objects: no identity, stored inline (no heap allocation!)
// Today: records + primitives + avoid boxing where possible


// ── Avoid autoboxing in hot paths ────────────────────────────────
// ❌ Autoboxing in tight loop
Map<String, Integer> counts = new HashMap<>();
for (String word : words) {
Integer current = counts.get(word);              // Unbox
counts.put(word, current == null ? 1 : current + 1);  // New Integer object per word!
}

// ✅ Use compute (no explicit boxing required)
for (String word : words) {
counts.merge(word, 1, Integer::sum);
}

// ✅ Use primitive collections for hot paths (Eclipse Collections)
MutableObjectIntMap<String> primitiveCounts = ObjectIntMaps.mutable.empty();
for (String word : words) {
primitiveCounts.addToValue(word, 1);  // No Integer boxing at all!
}


// ── String allocation reduction ───────────────────────────────────
// ❌ String concatenation in loop
String result = "";
for (String item : items) {
result += item + ", ";  // Creates new String object every iteration!
}

// ✅ StringBuilder
StringBuilder sb = new StringBuilder();
for (String item : items) {
sb.append(item).append(", ");
}
String result2 = sb.toString();  // One allocation at end

// ✅ String.join for simple cases
String result3 = String.join(", ", items);


// ── Escape Analysis helping hand ─────────────────────────────────
// JIT's escape analysis works better when:
// Objects don't escape the method they're created in
// Objects aren't stored in instance/static fields
// Objects aren't passed to unknown/virtual methods

// ❌ Prevents escape analysis (passed to unknown method)
void notOptimized() {
Point p = new Point(1, 2);
unknownMethod(p);              // JIT: p might escape — heap allocate
}

// ✅ Escape analysis can optimize (point used locally)
void optimized() {
// JIT: p never escapes this method → stack allocate (or eliminate entirely)
double sum = new Point(1, 2).x + new Point(1, 2).y;
// JIT may fold this to: double sum = 3.0; (constant at compile time!)
}
```

---

## Part 8: The Interview Q&A Round 🎤

---

**Q1. Explain the generational hypothesis and how it drives GC design**

> *"The generational hypothesis is the empirical observation that most objects die young — typically 90-98% of objects become unreachable before their first minor GC. This happens because most objects are temporary: loop variables, intermediate computation results, builders, request objects.*
>
> *This drives GC design in a fundamental way. Instead of scanning the entire heap every collection, the JVM divides heap into generations. The Young Generation — Eden plus two Survivor spaces — is collected frequently because that's where new objects are born and most die quickly. Young GC only scans Young Gen, which is small and mostly garbage, so it's fast — typically 5-50ms.*
>
> *Objects that survive enough minor GCs get promoted to the Old Generation. Old Gen is collected rarely, with more expensive algorithms, because objects there are assumed to be long-lived.*
>
> *The key insight: by concentrating collection effort on where garbage IS (Young Gen), we avoid wasting time scanning objects that are RARELY garbage (Old Gen). The design is optimized for the common case."*

---

**Q2. What is the difference between Minor GC, Major GC, and Full GC?**

> *"Minor GC collects only the Young Generation — Eden and Survivor spaces. It's triggered when Eden fills up. It's fast, typically 5-50ms, because Young Gen is small and mostly garbage. All application threads stop during Minor GC — it's stop-the-world.*
>
> *Major GC collects the Old Generation. It's triggered when Old Gen fills up. It's expensive — Old Gen is large, mostly live objects that must be scanned and possibly compacted. Depending on the collector: G1 uses concurrent marking to do most work concurrently, minimizing STW. Parallel GC stops all threads for the entire collection.*
>
> *Full GC collects BOTH Young and Old Generation simultaneously, often with compaction. It's the most expensive — can pause for seconds on large heaps. Triggers include System.gc(), promotion failure (Old Gen can't fit promoted objects), and concurrent GC falling behind allocation rate. Full GC is always bad in production — if you see frequent Full GCs, the system needs tuning.*
>
> *The goal of modern GC algorithms like G1 and ZGC is to NEVER trigger a Full GC — doing all collection work incrementally with bounded pauses."*

---

**Q3. How does G1 GC differ from Parallel GC? When do you choose each?**
```
PARALLEL GC:
Goal: maximize throughput (minimize total GC time)
Method: use all CPU cores during STW pause
Pauses: 100ms-1s but infrequent (does a lot per pause)
Concurrent work: NONE — all GC work during STW
Heap: traditional Young/Old layout
Tune: -XX:GCTimeRatio (throughput vs pause trade-off)

G1 GC:
Goal: predictable pause time
Method: regional heap, concurrent marking, bounded evacuations
Pauses: 10-200ms, frequent small pauses
Concurrent work: marking, refinement — happen while app runs
Heap: dynamic regions assigned as Eden/Survivor/Old/Humongous
Tune: -XX:MaxGCPauseMillis (pause time goal)

WHEN TO USE EACH:
Parallel GC:
→ Batch processing, ETL, data transformations
→ No human interaction, latency doesn't matter
→ Pauses of 500ms are fine
→ Want maximum work per unit of CPU
→ Typically: nightly jobs, report generation, ML training

G1 GC:
→ Web services, APIs, microservices
→ Users are waiting — 200ms+ pauses are visible
→ Need reasonable latency AND good throughput
→ Default choice for most production applications

ZGC/Shenandoah:
→ Strict SLAs (p99 < 10ms)
→ Real-time: trading, gaming, auctions
→ Large heaps where G1 pauses grow with heap size

Q4. What is a memory leak in Java and how do you find one?

"In Java, a memory leak isn't an object that wasn't freed — the GC handles freeing. A Java memory leak is an object that's REACHABLE but never USED again. The GC can't collect it because there's still a reference, but the application will never actually access it. Heap fills up with objects that are technically accessible but logically dead.
The most common patterns: static collections that grow without eviction (caches with no expiry), event listeners registered but never deregistered, ThreadLocal values not removed before thread returns to pool, non-static inner classes holding implicit outer references when submitted to long-lived thread pools, and HashMap entries with mutable keys that changed after insertion.
To find a leak: enable -XX:+HeapDumpOnOutOfMemoryError so you get a dump when OOM occurs. Analyze with Eclipse MAT — run the 'Leak Suspects Report'. It shows the dominator tree: which objects retain the most heap, and what's keeping them alive. Look for the retention path — it tells you what chain of references prevents collection. The fix is always: find the unnecessary reference and remove it, typically by using weak references, adding eviction, or removing from a collection when done."


Q5. Why is System.gc() considered harmful in production?
java// System.gc() — why it's almost always wrong

// 1. It's a HINT, not a command
// JVM may ignore it entirely
// -XX:+DisableExplicitGC flag (common in production) makes it a no-op

// 2. When it DOES run: it triggers a FULL GC
// Full GC: collect Young + Old + compact
// Full GC on 32GB heap: can pause for 10-30 seconds
// User requests pile up → timeout → errors
// One System.gc() call can take down a production service

// 3. It defeats adaptive GC strategies
// G1 learns your workload → builds a model → optimizes collection timing
// System.gc() fires when YOU think it's time, not when GC thinks
// Interrupts G1's natural rhythm → worse overall performance

// 4. It's usually wrong reasoning
// Code that calls System.gc() usually has a memory concern:
// "I just cleared a big cache, let's GC now"
// But GC will collect that cache on its own schedule
// System.gc() doesn't help — GC would have collected it anyway

// 5. Common WRONG pattern: NIO DirectByteBuffer cleanup
// DirectByteBuffer uses Cleaner (PhantomReference) for off-heap cleanup
// Some code calls System.gc() to "help" clean up direct buffers
// This is a sign that direct buffers aren't being closed properly
// Fix: close() direct buffers explicitly, not via GC hints

// When System.gc() MIGHT be acceptable:
// Performance testing: force GC between test runs for clean baseline
// Long-running batch: explicit GC between major phases (and you've measured)
// Memory leak diagnosis: in a test environment only

// Never in production APIs, never in library code (affects all users!)
```

---

**Q6. What are the key differences between ZGC and G1?**

> *"The fundamental difference is WHERE and WHEN the GC work happens. G1 does most of its work during stop-the-world pauses — application threads stop, GC threads scan and evacuate objects, then application resumes. G1 does concurrent marking in the background, but the actual copying/evacuation is STW. Pause times scale with the amount of live data in regions being collected — typically 50-200ms.*
>
> *ZGC does almost ALL work concurrently while the application runs. It uses two key innovations: colored pointers — encoding GC state in unused bits of 64-bit pointers — and load barriers — JIT-injected code that runs at every object reference load to transparently fix any outdated references. This means ZGC can move objects concurrently without stopping threads, because any thread that loads an old reference gets it transparently corrected by the load barrier.*
>
> *ZGC has only two tiny STW points: initial mark (scan GC roots, <1ms) and final mark (process any concurrently-found references, <1ms). Everything else is concurrent. The result: pauses under 1ms regardless of heap size — 8GB or 8TB.*
>
> *The cost: load barriers add ~5-15% throughput overhead — every reference load has a tiny bit of extra code. And ZGC uses more CPU overall because GC work that G1 batches into pauses is instead spread across all threads all the time. For latency-sensitive workloads, this is the right trade-off. For throughput-critical batch jobs, G1 or Parallel GC is better."*

---

## Section 9 Master Summary 🧠
```
REACHABILITY:
GC traces from ROOTS: thread stacks, statics, JNI, monitors
Live = reachable from roots
Garbage = unreachable (no path from any root)
Memory leak = reachable but never used again

REFERENCE TYPES:
Strong   → never GC'd while referenced (default)
Soft     → GC'd before OOM (memory-sensitive caches)
Weak     → GC'd at next GC (WeakHashMap, metadata)
Phantom  → GC'd then enqueued (resource cleanup via Cleaner)

GENERATIONAL HYPOTHESIS:
90-98% of objects die young
→ Young Gen: collected frequently, fast (5-50ms)
→ Old Gen: collected rarely, expensive (100ms-seconds)
Minor GC: Young only — fast, frequent
Major GC: Old Gen — expensive, less frequent
Full GC: both — AVOID — pauses seconds — fix root cause

GC ALGORITHMS:
Serial:    single thread, seconds pause, tiny heaps only
Parallel:  multi-thread STW, max throughput, batch jobs
G1:        regional, 50-200ms pauses, default Java 9+
ZGC:       colored pointers + load barriers, <1ms, Java 15+
Shenandoah:forwarding pointers, 1-5ms, RedHat-maintained

G1 KEY FLAGS:
-XX:MaxGCPauseMillis=200           Pause time goal
-XX:InitiatingHeapOccupancyPercent=40  Start marking at 40%
-XX:G1HeapRegionSize=8m            Region size
-Xms8g -Xmx8g                      Fixed heap (min=max)
-XX:+G1UseAdaptiveIHOP             Auto-tune IHOP

COMMON MEMORY LEAKS:
Static collection with no eviction   → use Caffeine/bounded cache
Listeners never deregistered         → WeakReference or explicit remove
ThreadLocal not removed              → ALWAYS remove() in finally
Non-static inner class in executor   → use static nested class
HashMap with mutable keys            → use immutable keys

ALLOCATION OPTIMIZATION:
Object pooling           → reuse expensive objects (ByteBuffers)
ThreadLocal StringBuilder→ reuse StringBuilder without sync
Avoid autoboxing         → primitive collections for hot paths
String.join/StringBuilder→ avoid string concatenation in loops
Let JIT help             → don't let objects escape methods

GC SELECTION GUIDE:
Batch/ETL/data jobs         → Parallel GC (max throughput)
Web APIs/microservices       → G1 GC (balance, default)
Strict latency SLA < 10ms   → ZGC (Java 17+)
Heap > 32GB + latency        → ZGC (pause doesn't grow with heap)

TUNING METHODOLOGY:
1. Measure first — GC log, JFR
2. Identify: Full GCs? Long pauses? High CPU?
3. Fix root cause: leak? IHOP too high? Wrong collector?
4. Tune: one flag at a time, measure impact
5. Alert: GC duration > SLA, Full GC events

NEVER:
❌ System.gc() in production (triggers Full GC, pauses seconds)
❌ Heap min ≠ max (resize triggers unnecessary GC)
❌ Ignore GC logs (they tell you everything)
❌ Tune without measuring (intuition is wrong, data is right)