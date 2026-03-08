Section 7: Concurrent Collections 🗂️
Sections 1-6 covered synchronization primitives and async patterns. Section 7 is about data structures built for concurrency — the containers your threads share. FAANG interviewers love this section because it reveals whether you understand why these work, not just that they work.

The Problem — Why Regular Collections Break Under Concurrency
ArrayList, HashMap, LinkedList — designed for single-threaded use.
Under concurrent access without external synchronization:

HashMap:
put() during resize → infinite loop (Java 6) or data loss (Java 7)
Two threads resize simultaneously → corrupted internal array
get() during put() → may return null for existing key

ArrayList:
add() during add() → ArrayIndexOutOfBoundsException
iterator() during add() → ConcurrentModificationException
size() during add() → wrong count → wrong loop bounds

The WRONG fix: Collections.synchronizedXxx()
synchronized(list) { list.add(x); }
synchronized(list) { list.get(0); }
→ ONE lock for EVERYTHING
→ All reads block all writes
→ All writes block all reads
→ Zero concurrency
→ Often worse than single-threaded

The RIGHT fix: purpose-built concurrent data structures
→ Internal fine-grained locking
→ Lock-free algorithms (CAS)
→ Copy-on-write
→ Each designed for specific access patterns

Part 1: ConcurrentHashMap — The Most Asked Collection
Internal Architecture — Java 7 vs Java 8+
JAVA 7: Segment-based locking (striped locking)
┌────────────────────────────────────────────────┐
│  Segment[0]  │  Segment[1]  │ ... │ Segment[15]│
│  (lock)      │  (lock)      │     │  (lock)    │
│  [bucket...) │  [bucket...) │     │  [bucket..)|
└────────────────────────────────────────────────┘
16 segments by default → 16 threads can write simultaneously
Each segment is a mini-HashMap with its own ReentrantLock
Concurrency level = number of segments (tunable)

JAVA 8+: Node-level CAS + synchronized on first node
┌─────────────────────────────────────────────────┐
│ [bucket 0] [bucket 1] [bucket 2] ... [bucket N] │
│     ↓           ↓          ↓                    │
│  Node(CAS)  Node(sync)  Node(CAS)               │
└─────────────────────────────────────────────────┘
Empty bucket: CAS to insert first node (lock-free)
Non-empty bucket: synchronized on first node of that bucket
→ Each bucket is its own lock
→ N buckets = N independent locks
→ Much finer granularity than 16 segments
→ Collision chains: linked list → Red-Black Tree at length ≥ 8
How put() Works — Step by Step
java// Simplified logic of ConcurrentHashMap.put() in Java 8+:

// Step 1: Compute hash
int hash = spread(key.hashCode());  // XOR with upper bits to spread distribution

// Step 2: Find bucket
int index = hash & (table.length - 1);

// Step 3a: Bucket is EMPTY — use CAS (lock-free)
Node expected = null;
Node newNode  = new Node(hash, key, value);
if (CAS(table[index], expected, newNode)) {
// Success! No lock needed for empty bucket insertion
return;
}
// If CAS fails: another thread inserted first → fall through to Step 3b

// Step 3b: Bucket has nodes — synchronize on first node
synchronized (firstNodeInBucket) {
// Re-check (double-check under lock)
// Walk linked list: update if key exists, append if new
// If chain length ≥ 8 and table.length ≥ 64: treeify to Red-Black Tree
}

// Step 4: Check if resize needed
if (++size > threshold) resize();  // threshold = capacity × 0.75
Complete API — Every Method
javaConcurrentHashMap<String, Integer> map = new ConcurrentHashMap<>();

// ── Basic operations — all thread-safe ───────────────────────────
map.put("key", 1);
map.get("key");                          // Returns null if absent (no NPE)
map.remove("key");
map.containsKey("key");
map.size();                              // ⚠️ Approximate under concurrency!
map.isEmpty();

// ── Atomic compound operations — THE KEY ADVANTAGE ───────────────

// putIfAbsent: atomic check-then-put
// Returns: existing value if present, null if we just inserted
Integer prev = map.putIfAbsent("counter", 0);
// If "counter" absent: inserts 0, returns null
// If "counter" present: does nothing, returns existing value
// Use for: cache population, idempotent initialization


// computeIfAbsent: atomic lazy initialization
// Function called ONLY if key is absent
List<String> tags = map.computeIfAbsent("user:1",
k -> new ArrayList<>());             // Only creates list if needed
// Use for: building multi-value maps, lazy cache loading
// ⚠️ Function must be FAST and SIDE-EFFECT-FREE
//    (may be called multiple times under contention in Java 7, not in Java 8+)


// computeIfPresent: atomic update if key exists
map.computeIfPresent("counter",
(k, v) -> v + 1);                   // Increment only if key exists
// If key absent: does nothing, returns null
// If key present: applies function, stores result


// compute: atomic read-modify-write (most powerful)
map.compute("counter", (k, v) -> {
if (v == null) return 1;            // Key absent: initialize
return v + 1;                       // Key present: increment
});
// Atomic: no other thread can read or modify this key between function read and write
// Use for: counters, accumulators, any read-modify-write pattern


// merge: atomic upsert with combining function
map.merge("word", 1, Integer::sum);
// If "word" absent:  puts 1
// If "word" present: applies Integer::sum(existing, 1) → increments by 1
// Perfect for: word frequency counting, aggregating metrics


// replaceAll: atomic transform of all values
map.replaceAll((k, v) -> v * 2);        // Double all values


// ── The classic mistake — NOT atomic ──────────────────────────────
// ❌ RACE CONDITION: get and put are separate operations
Integer val = map.get("counter");       // Thread A reads 5
// Thread B also reads 5
map.put("counter", val + 1);            // Thread A writes 6
// Thread B writes 6 → LOST UPDATE

// ✅ CORRECT: use compute (single atomic operation)
map.compute("counter", (k, v) -> v == null ? 1 : v + 1);


// ── Bulk operations (Java 8+) — parallel processing ───────────────
// forEach with parallelism threshold
map.forEach(
1,                                   // parallelismThreshold: 1 = use all cores
(k, v) -> System.out.println(k + "=" + v)
);

// search: find first matching element
String found = map.search(
1,                                   // parallelismThreshold
(k, v) -> v > 100 ? k : null        // Return non-null to stop search
);

// reduce: aggregate all values
int total = map.reduceValues(
1,                                   // parallelismThreshold
v -> v,                              // transformer
Integer::sum                         // reducer
);


// ── ConcurrentHashMap limitations ────────────────────────────────
// ❌ No null keys:    map.put(null, 1)  → NullPointerException!
// ❌ No null values: map.put("k", null) → NullPointerException!
// Why: null would be ambiguous — does get() return null because key is absent
//      or because value is null? No way to tell without separate containsKey() call
//      which would be a TOCTOU race condition anyway.

// ⚠️ size() is approximate:
//    Uses multiple counters (like LongAdder) to reduce contention
//    sum of counters at any moment ≈ actual size
//    Use mappingCount() for long value (same accuracy, no overflow)

// ⚠️ Iterators are weakly consistent:
//    Reflect state at or after iterator creation time
//    May see updates made after iterator created
//    Never throw ConcurrentModificationException
//    Use for: read snapshots, not for critical exact counts
The Internal Resizing — Why It Matters
java// When size > capacity × 0.75 → RESIZE (double capacity, rehash all)
// This is expensive: O(n) operation

// PROBLEM: Multiple threads may trigger resize simultaneously
// Java 8+ solution: cooperative resizing (transfer)
//   → First thread starts resize, initializes new table
//   → Other threads JOIN the resize (each transfers a portion)
//   → Threads finding ForwardingNode during put help transfer
//   → True parallel rehashing!

// WHY YOU SHOULD PRE-SIZE:
ConcurrentHashMap<String, User> users = new ConcurrentHashMap<>();
// Default capacity: 16, load factor: 0.75
// Resize at: 12 entries
// Each resize: allocate new array + rehash ALL entries

// If you know you'll have ~1000 entries:
int expectedSize = 1000;
int initialCapacity = (int)(expectedSize / 0.75) + 1;  // ~1334
ConcurrentHashMap<String, User> preSize = new ConcurrentHashMap<>(initialCapacity);
// Result: no resize for first 1000 entries → much faster startup

// Formula: new ConcurrentHashMap<>(expectedSize / 0.75 + 1)
```

---

## Part 2: `CopyOnWriteArrayList` — For Read-Heavy Lists

### The Mental Model
```
CopyOnWriteArrayList strategy:
→ Internal array is IMMUTABLE (from readers' perspective)
→ READS:  no locking, no synchronization — blazing fast
→ WRITES: copy the entire array, apply change, atomically swap reference

Before write:     array = [A, B, C, D]
Thread writes E:
Step 1: Copy → [A, B, C, D, E]  (new array)
Step 2: Atomically: array reference = new array
After write:      array = [A, B, C, D, E]

Meanwhile readers: still reading old [A, B, C, D]
→ No blocking, no inconsistency from THEIR perspective
→ They just see the snapshot that existed when they started
javaCopyOnWriteArrayList<String> list = new CopyOnWriteArrayList<>();

// ── READS — completely lock-free, blazing fast ────────────────────
String item = list.get(5);
int size     = list.size();
boolean has  = list.contains("item");
// Thread reads directly from the current array reference
// No synchronization whatsoever
// Multiple readers → zero contention


// ── WRITES — lock + copy + swap ───────────────────────────────────
list.add("new item");        // Lock → copy entire array → add → swap
list.remove("old item");     // Lock → copy entire array → remove → swap
list.set(3, "updated");      // Lock → copy entire array → update → swap
// ⚠️ Each write operation: allocates new array, copies ALL elements
// O(n) per write — EXPENSIVE for large lists or frequent writes


// ── Iteration — SNAPSHOT semantics ───────────────────────────────
for (String s : list) {
// Iterates over the array snapshot from when iteration started
// If another thread adds/removes: iterator NEVER sees it
// No ConcurrentModificationException — EVER
process(s);

    list.remove(s);  // OK! But removes from the LIVE list, not iterator's snapshot
                     // Iterator's view is unchanged
}

// Compare with synchronized ArrayList:
List<String> syncList = Collections.synchronizedList(new ArrayList<>());
synchronized (syncList) {              // Must lock for safe iteration!
for (String s : syncList) {        // Without lock → CME
process(s);
}
}
// CopyOnWriteArrayList: no external lock needed for iteration ✅


// ── addIfAbsent — atomic check-then-add ──────────────────────────
boolean added = list.addIfAbsent("unique-item");
// Returns true if added, false if already present
// Useful for de-duplication


// ── CopyOnWriteArraySet — same principle for sets ─────────────────
CopyOnWriteArraySet<String> set = new CopyOnWriteArraySet<>();
set.add("item");       // Lock + copy
set.contains("item");  // Lock-free read


// ── When to use CopyOnWriteArrayList ─────────────────────────────
// ✅ Reads are VERY frequent (90%+) — event listener lists
// ✅ Writes are RARE — register/unregister listeners
// ✅ Iteration is frequent — don't want to lock during iteration
// ✅ List is SMALL — copying is cheap
//
// ❌ Writes are frequent — O(n) copy on every write
// ❌ List is large — copying 1MB array on every write
// ❌ Memory is tight — always two copies exist during write
// ❌ Need to see writes immediately in iteration


// Classic use case: event listener management
class EventBus {
private final CopyOnWriteArrayList<EventListener> listeners =
new CopyOnWriteArrayList<>();

    public void register(EventListener l) {
        listeners.addIfAbsent(l);     // Rare write — one copy
    }

    public void unregister(EventListener l) {
        listeners.remove(l);          // Rare write — one copy
    }

    public void publish(Event event) {
        for (EventListener l : listeners) {  // Frequent read — lock-free!
            l.onEvent(event);                // Alien call SAFE: no lock held
        }
        // Snapshot iteration: listeners registered during publish
        // are NOT called (they'll get next event) — acceptable behavior
    }
}
```

---

## Part 3: `BlockingQueue` — The Producer-Consumer Foundation

### The Mental Model
```
BlockingQueue is a thread-safe queue with BLOCKING semantics:

put(e):   Add to queue — BLOCKS if queue is FULL (waits for space)
take():   Remove from queue — BLOCKS if queue is EMPTY (waits for item)

This is the key to producer-consumer coordination:
Producer too fast? → put() blocks → producer slows down (backpressure!)
Consumer too fast? → take() blocks → consumer waits for work

No busy-waiting, no polling, no manual synchronization needed.
The queue handles all of it.
All BlockingQueue Methods
javaBlockingQueue<Task> queue = new LinkedBlockingQueue<>(100);

// ── PUT side (producer) ──────────────────────────────────────────
queue.put(task);                              // BLOCKS if full — waits forever
queue.offer(task);                            // Returns FALSE if full — no block
queue.offer(task, 500, TimeUnit.MILLISECONDS); // Waits up to 500ms, then false
queue.add(task);                              // Throws IllegalStateException if full

// ── TAKE side (consumer) ─────────────────────────────────────────
Task t1 = queue.take();                       // BLOCKS if empty — waits forever
Task t2 = queue.poll();                       // Returns NULL if empty — no block
Task t3 = queue.poll(500, TimeUnit.MILLISECONDS); // Waits up to 500ms, then null
Task t4 = queue.remove();                     // Throws NoSuchElementException if empty
Task t5 = queue.peek();                       // Look without removing (null if empty)

// ── Bulk drain ───────────────────────────────────────────────────
List<Task> batch = new ArrayList<>();
int drained = queue.drainTo(batch);           // Move all available to list (non-blocking)
int limited = queue.drainTo(batch, 10);       // Move up to 10

// ── Monitoring ───────────────────────────────────────────────────
int waiting   = queue.size();                 // Items currently in queue
int remaining = queue.remainingCapacity();    // Space left
Queue Type Deep Dive
java// ── 1. LinkedBlockingQueue — MOST COMMON ──────────────────────────
// Two locks: putLock + takeLock → producers and consumers rarely contend
// Linked nodes → dynamic sizing (no pre-allocated array)

LinkedBlockingQueue<Task> q1 = new LinkedBlockingQueue<>();        // Unbounded ❌ prod
LinkedBlockingQueue<Task> q2 = new LinkedBlockingQueue<>(1000);    // Bounded ✅ prod

// Internal: putLock for head, takeLock for tail
// put() and take() can happen SIMULTANEOUSLY with minimal contention
// Best for: general producer-consumer, high throughput workloads


// ── 2. ArrayBlockingQueue — BOUNDED, FAIR OPTION ──────────────────
// Single lock for both put and take (more contention than Linked)
// Array: fixed memory, no GC pressure from node creation
// Supports fairness: FIFO among waiting threads

ArrayBlockingQueue<Task> q3 = new ArrayBlockingQueue<>(500);           // Unfair
ArrayBlockingQueue<Task> q4 = new ArrayBlockingQueue<>(500, true);     // Fair (FIFO)
// fair=true: threads are served in FIFO order → no starvation
// fair=false: OS scheduler decides → higher throughput, possible starvation
// Pre-allocates array: good for known, bounded load


// ── 3. SynchronousQueue — DIRECT HANDOFF ──────────────────────────
// Capacity: ZERO — no buffering at all
// Every put() DIRECTLY hands off to a waiting take()
// If no taker: put() blocks until someone takes
// If no putter: take() blocks until someone puts

SynchronousQueue<Task> q5 = new SynchronousQueue<>();

// Used by: Executors.newCachedThreadPool()
// Producer submits task → hands off to consumer thread DIRECTLY
// Latency: minimal (no queue delay)
// Use for: lowest-latency handoff, when you want direct thread-to-thread transfer
// Not for: buffering — there IS no buffer


// ── 4. PriorityBlockingQueue — PRIORITY ORDERED ───────────────────
// Elements ordered by natural ordering or Comparator
// UNBOUNDED (grows without limit) ← add size control externally!
// take() always returns HIGHEST priority item

PriorityBlockingQueue<Task> q6 = new PriorityBlockingQueue<>(100,
Comparator.comparingInt(Task::getPriority).reversed()); // High priority first

q6.offer(new Task("LOW",    1));
q6.offer(new Task("HIGH",   9));
q6.offer(new Task("MEDIUM", 5));

q6.take();  // Returns HIGH priority task (priority=9)
q6.take();  // Returns MEDIUM (priority=5)
q6.take();  // Returns LOW (priority=1)

// ⚠️ Not FIFO within same priority — no ordering guarantee for equal priorities
// ⚠️ Unbounded: add Semaphore to control max size


// ── 5. DelayQueue — TIMED RELEASE ──────────────────────────────────
// Elements are only available after their delay expires
// take() blocks until the element with smallest delay is ready

class DelayedTask implements Delayed {
private final String name;
private final long   executeAt;   // Absolute time in nanoseconds

    DelayedTask(String name, long delayMs) {
        this.name      = name;
        this.executeAt = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(delayMs);
    }

    @Override
    public long getDelay(TimeUnit unit) {
        long remaining = executeAt - System.nanoTime();
        return unit.convert(remaining, TimeUnit.NANOSECONDS);
        // Negative = past due → ready to be taken
    }

    @Override
    public int compareTo(Delayed other) {
        return Long.compare(
            this.getDelay(TimeUnit.NANOSECONDS),
            other.getDelay(TimeUnit.NANOSECONDS)
        );
    }
}

DelayQueue<DelayedTask> delayQ = new DelayQueue<>();
delayQ.offer(new DelayedTask("sendReminder",     60_000));  // 1 minute
delayQ.offer(new DelayedTask("cleanupSession",    5_000));  // 5 seconds
delayQ.offer(new DelayedTask("refreshCache",     30_000));  // 30 seconds

// Consumer blocks until next task is due
new Thread(() -> {
while (true) {
try {
DelayedTask task = delayQ.take();  // Blocks until delay expires
executeTask(task);
} catch (InterruptedException e) {
Thread.currentThread().interrupt();
return;
}
}
}).start();
// First taken: cleanupSession (5s), then refreshCache (30s), then sendReminder (60s)

// Use cases: session timeout management, scheduled task queues,
//            retry with exponential backoff, cache TTL eviction


// ── 6. LinkedTransferQueue — Most Powerful (Java 7+) ──────────────
// Combines: LinkedBlockingQueue + SynchronousQueue
// transfer(): like SynchronousQueue.put() — blocks until consumer takes
// offer():    like LinkedBlockingQueue.put() — adds to queue if no waiter
// tryTransfer(): non-blocking direct handoff attempt

LinkedTransferQueue<Task> q7 = new LinkedTransferQueue<>();

q7.transfer(task);       // Blocks until a consumer directly takes it
q7.offer(task);          // Add to queue (consumed later)
q7.tryTransfer(task);    // Attempt direct handoff — return false if no waiter

// Best throughput of all blocking queues (lock-free algorithm)
// Used by: ForkJoinPool internally
Producer-Consumer — Complete Implementation
javaclass PrintSpooler {
private final BlockingQueue<PrintJob> queue;
private final ExecutorService         printers;
private final ExecutorService         spoolerThread;
private volatile boolean              running = true;

    PrintSpooler(int printerCount, int queueCapacity) {
        this.queue         = new LinkedBlockingQueue<>(queueCapacity);
        this.printers      = Executors.newFixedThreadPool(printerCount);
        this.spoolerThread = Executors.newSingleThreadExecutor();
    }

    // ── Producer API ─────────────────────────────────────────────
    public boolean submit(PrintJob job) throws InterruptedException {
        // put() blocks if queue is full — backpressure on caller
        queue.put(job);
        return true;
    }

    public boolean submitWithTimeout(PrintJob job, long timeoutMs)
            throws InterruptedException {
        return queue.offer(job, timeoutMs, TimeUnit.MILLISECONDS);
    }

    // ── Consumer loop ────────────────────────────────────────────
    public void start() {
        for (int i = 0; i < getCorePoolSize(); i++) {
            printers.submit(this::consumerLoop);
        }
    }

    private void consumerLoop() {
        while (running || !queue.isEmpty()) {
            try {
                // poll with timeout: don't block forever if shutting down
                PrintJob job = queue.poll(100, TimeUnit.MILLISECONDS);

                if (job != null) {
                    processPrint(job);    // Slow I/O operation
                }

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;   // Graceful exit
            } catch (Exception e) {
                // Don't let one bad job kill the consumer loop!
                log.error("Print job failed: {}", e.getMessage(), e);
                metrics.increment("print.failures");
            }
        }
    }

    // ── Graceful shutdown ─────────────────────────────────────────
    public void shutdown() throws InterruptedException {
        running = false;               // Signal consumers to stop after draining

        printers.shutdown();
        if (!printers.awaitTermination(30, TimeUnit.SECONDS)) {
            printers.shutdownNow();
        }
    }

    // ── Monitoring ────────────────────────────────────────────────
    public int queueDepth() { return queue.size(); }
    public int remainingCapacity() { return queue.remainingCapacity(); }

    private int getCorePoolSize() {
        return ((ThreadPoolExecutor) printers).getCorePoolSize();
    }
}


// ── Multi-producer, multi-consumer with batching ─────────────────
class BatchProcessor {
private final BlockingQueue<Event> queue = new LinkedBlockingQueue<>(10_000);

    // Multiple producers — thread-safe (BlockingQueue handles it)
    public void produce(Event event) throws InterruptedException {
        queue.put(event);
    }

    // Consumer with batching — more efficient than one-at-a-time
    public void consumeInBatches(int batchSize, long maxWaitMs) {
        new Thread(() -> {
            List<Event> batch = new ArrayList<>(batchSize);

            while (!Thread.currentThread().isInterrupted()) {
                try {
                    // Block for first item
                    Event first = queue.poll(maxWaitMs, TimeUnit.MILLISECONDS);
                    if (first == null) continue;    // Timeout — no work
                    batch.add(first);

                    // Drain up to batchSize-1 more without blocking
                    queue.drainTo(batch, batchSize - 1);

                    // Process entire batch at once
                    processBatch(batch);
                    batch.clear();                  // Reset for next batch

                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    if (!batch.isEmpty()) processBatch(batch); // Flush remaining
                    return;
                }
            }
        }, "batch-consumer").start();
    }
}

Part 4: Other Concurrent Collections
ConcurrentLinkedQueue — Lock-Free Queue
java// Non-blocking, lock-free using CAS
// Unbounded — no blocking operations (no put/take)
// Use when: you don't need blocking semantics (don't need to wait for items)

ConcurrentLinkedQueue<Task> queue = new ConcurrentLinkedQueue<>();

queue.offer(task);      // Non-blocking add (always succeeds — unbounded)
Task t = queue.poll();  // Non-blocking remove — returns NULL if empty
Task h = queue.peek();  // Look at head without removing

// When to choose ConcurrentLinkedQueue vs LinkedBlockingQueue:
// ConcurrentLinkedQueue:  non-blocking, lock-free, for async polling
// LinkedBlockingQueue:    blocking, for producer-consumer coordination

// Use case: work-stealing, event queues where you poll frequently
class WorkStealingProcessor {
private final ConcurrentLinkedQueue<Task> workQueue = new ConcurrentLinkedQueue<>();

    void addWork(Task task) {
        workQueue.offer(task);
    }

    void processLoop() {
        while (running) {
            Task task = workQueue.poll();   // Non-blocking
            if (task != null) {
                process(task);
            } else {
                Thread.sleep(1);            // Brief pause — no work available
            }
        }
    }
}
ConcurrentSkipListMap and ConcurrentSkipListSet — Sorted + Concurrent
java// Sorted concurrent map — thread-safe alternative to TreeMap
// Lock-free using CAS — all operations O(log n)
// Maintains keys in SORTED order at all times

ConcurrentSkipListMap<String, Integer> scoreBoard = new ConcurrentSkipListMap<>();

scoreBoard.put("Alice", 95);
scoreBoard.put("Bob",   87);
scoreBoard.put("Carol", 92);

// Sorted navigation — all thread-safe
scoreBoard.firstKey();                    // "Alice" (lexicographically first)
scoreBoard.lastKey();                     // "Carol"
scoreBoard.headMap("Carol");              // All keys < "Carol"
scoreBoard.tailMap("Bob");                // All keys >= "Bob"
scoreBoard.subMap("Bob", "Carol");        // Keys in range [Bob, Carol)
scoreBoard.floorKey("Billy");             // Greatest key ≤ "Billy" → "Bob"
scoreBoard.ceilingKey("B");               // Smallest key ≥ "B" → "Bob"
scoreBoard.descendingMap();               // Reverse order view

// Use case: leaderboard, time-series data, range queries
class LeaderBoard {
// TreeMap<score, playerId> sorted by score
private final ConcurrentSkipListMap<Integer, String> board =
new ConcurrentSkipListMap<>(Comparator.reverseOrder()); // Highest first

    public void updateScore(String playerId, int score) {
        board.put(score, playerId);       // Thread-safe, stays sorted
    }

    public List<String> getTop(int n) {
        return board.values().stream().limit(n).collect(Collectors.toList());
    }

    public Map.Entry<Integer, String> getTopPlayer() {
        return board.firstEntry();        // Highest score
    }
}

// ConcurrentSkipListSet — sorted concurrent set
ConcurrentSkipListSet<Integer> sortedSet = new ConcurrentSkipListSet<>();
sortedSet.add(5); sortedSet.add(2); sortedSet.add(8);
sortedSet.first();                        // 2 (smallest)
sortedSet.last();                         // 8 (largest)
sortedSet.headSet(5);                     // [2] (elements < 5)
sortedSet.tailSet(5);                     // [5, 8] (elements ≥ 5)
ConcurrentLinkedDeque — Concurrent Double-Ended Queue
java// Thread-safe deque (double-ended queue) — CAS-based, lock-free
// Supports add/remove from BOTH ends

ConcurrentLinkedDeque<String> deque = new ConcurrentLinkedDeque<>();

// Add to either end
deque.addFirst("front");
deque.addLast("back");
deque.offerFirst("new-front");
deque.offerLast("new-back");

// Remove from either end
String first = deque.pollFirst();         // Remove from front (null if empty)
String last  = deque.pollLast();          // Remove from back  (null if empty)
deque.peekFirst();                        // Look at front (null if empty)
deque.peekLast();                         // Look at back  (null if empty)

// Use case: work-stealing deque (each thread owns a deque,
//           steals from back of other threads' deques)

Part 5: Choosing the Right Collection — The Decision Tree
java/*
┌────────────────────────────────────────────────────────────────────┐
│              CONCURRENT COLLECTION CHOOSER                        │
│                                                                    │
│  Is it a MAP?                                                      │
│    Need sorted order?                                              │
│      YES → ConcurrentSkipListMap  (TreeMap equivalent)            │
│      NO  → ConcurrentHashMap      (HashMap equivalent, default)   │
│                                                                    │
│  Is it a LIST/SET?                                                 │
│    Read-heavy (90%+ reads), rare writes, small size?               │
│      YES → CopyOnWriteArrayList / CopyOnWriteArraySet             │
│      NO  → ConcurrentSkipListSet (sorted set)                     │
│             OR synchronized ArrayList (if sorted not needed)      │
│                                                                    │
│  Is it a QUEUE?                                                    │
│    Need blocking (producer-consumer)?                              │
│      YES: Need fixed capacity?                                     │
│             YES → ArrayBlockingQueue(n)  (single lock, fair opt)  │
│             NO  → LinkedBlockingQueue(n) (two locks, higher tput) │
│           Need priority ordering?                                  │
│             YES → PriorityBlockingQueue (add size control!)        │
│           Need timed/delayed tasks?                                │
│             YES → DelayQueue                                       │
│           Need zero-buffer direct handoff?                         │
│             YES → SynchronousQueue (CachedThreadPool uses this)    │
│      NO (non-blocking): ConcurrentLinkedQueue                     │
│                                                                    │
│  Is it a DEQUE?                                                    │
│      → ConcurrentLinkedDeque (non-blocking)                       │
│      → LinkedBlockingDeque   (blocking)                           │
└────────────────────────────────────────────────────────────────────┘
*/

Part 6: Performance Comparison — Actual Benchmarks
java// Understanding relative performance shapes your architecture decisions

// ── ConcurrentHashMap vs synchronizedHashMap ──────────────────────
// 8 threads, 1M operations, 80% reads / 20% writes:

// Collections.synchronizedMap(new HashMap<>()):
//   ALL operations on ONE lock
//   8 threads → 7 blocked → 1 working
//   Throughput: ~2M ops/sec

// ConcurrentHashMap:
//   Node-level locking (Java 8+)
//   8 threads → all can read simultaneously, writes on different buckets
//   Throughput: ~40M ops/sec
//   → ~20× faster for read-heavy workloads

// Lesson: synchronizedMap is a correctness fix, not a concurrency solution


// ── LinkedBlockingQueue vs ArrayBlockingQueue ─────────────────────
// LinkedBlockingQueue:  two locks (putLock + takeLock)
//   → producers and consumers rarely contend
//   → higher throughput for concurrent put+take
//   → slight GC overhead (node objects)

// ArrayBlockingQueue:  one lock (everything)
//   → producer and consumer contend on same lock
//   → lower throughput than Linked
//   → better memory (no node objects, cache-friendly array)
//   → Use when: fixed size IS the feature (don't want unbounded growth)
//   → Use when: fairness (FIFO) among threads is required


// ── CopyOnWriteArrayList vs synchronizedList ──────────────────────
// 16 threads, 1M iterations, 99% reads / 1% writes:

// synchronizedList:
//   ALL operations (including reads) hold lock
//   Read iteration: must hold lock entire time → all writers blocked
//   Throughput: ~15M reads/sec

// CopyOnWriteArrayList:
//   Reads: completely lock-free
//   Throughput: ~200M reads/sec → 13× faster for read-heavy
//   Writes: O(n) copy → expensive if list is large or writes frequent

Part 7: Common Pitfalls — What Goes Wrong
java// ── Pitfall 1: Compound operations still need external sync ────────
ConcurrentHashMap<String, List<String>> multimap = new ConcurrentHashMap<>();

// ❌ RACE: check then act across two operations
if (!multimap.containsKey("users")) {
// Another thread may insert "users" here!
multimap.put("users", new ArrayList<>());
}
// Two threads both pass the if → two ArrayLists → one gets lost

// ✅ CORRECT: use computeIfAbsent (atomic)
multimap.computeIfAbsent("users", k -> new ArrayList<>()).add("alice");
// If "users" absent: create list, put it, get reference, add "alice" → atomic


// ── Pitfall 2: Iterating while mutating (wrong collection) ─────────
// ❌ Regular list: throws ConcurrentModificationException
List<String> regularList = new ArrayList<>(List.of("a", "b", "c"));
for (String s : regularList) {
regularList.remove(s);  // ConcurrentModificationException!
}

// ✅ CopyOnWriteArrayList: safe (iterator has snapshot)
CopyOnWriteArrayList<String> cowList = new CopyOnWriteArrayList<>(List.of("a","b","c"));
for (String s : cowList) {
cowList.remove(s);      // Safe! Iterator sees snapshot, list shrinks in background
}

// ✅ ConcurrentHashMap: safe iteration + modification
ConcurrentHashMap<String, Integer> chm = new ConcurrentHashMap<>();
chm.put("a", 1); chm.put("b", 2); chm.put("c", 3);
for (String key : chm.keySet()) {
chm.remove(key);        // Safe! Weakly-consistent iterator


// ── Pitfall 3: size() is unreliable for critical logic ─────────────
ConcurrentHashMap<String, Session> sessions = new ConcurrentHashMap<>();

// ❌ Don't use size() to make decisions
if (sessions.size() < MAX_SESSIONS) {
// Another thread may push size over MAX between check and put
sessions.put(sessionId, newSession);  // May exceed MAX_SESSIONS!
}

// ✅ Use a Semaphore or AtomicInteger for accurate counting
Semaphore sessionSlots = new Semaphore(MAX_SESSIONS);

boolean created = sessionSlots.tryAcquire();
if (created) {
sessions.put(sessionId, newSession);
} else {
throw new TooManySessionsException();
}
// Release in cleanup:
sessions.remove(sessionId);
sessionSlots.release();


// ── Pitfall 4: Mutable keys in ConcurrentHashMap ──────────────────
// ❌ NEVER use mutable objects as keys
ConcurrentHashMap<List<Integer>, String> map = new ConcurrentHashMap<>();
List<Integer> key = new ArrayList<>(Arrays.asList(1, 2, 3));
map.put(key, "value");

key.add(4);                  // Mutated AFTER insertion!
map.get(key);                // Returns null — hashCode changed!
// ✅ Always use immutable keys: String, Integer, record types


// ── Pitfall 5: Using wrong queue type ────────────────────────────
// ❌ Unbounded queue hides backpressure problem
ExecutorService pool = new ThreadPoolExecutor(
4, 4, 0, TimeUnit.SECONDS,
new LinkedBlockingQueue<>()   // Unbounded — will grow to OOM under load
);

// ✅ Bounded queue exposes backpressure
ExecutorService safePool = new ThreadPoolExecutor(
4, 8, 60, TimeUnit.SECONDS,
new ArrayBlockingQueue<>(500),
new ThreadPoolExecutor.CallerRunsPolicy()  // Backpressure on caller
);


// ── Pitfall 6: CopyOnWriteArrayList for large, frequently-written lists
// ❌ 10,000 element list with frequent writes → O(10,000) copy EVERY write
CopyOnWriteArrayList<Trade> trades = new CopyOnWriteArrayList<>();
// Receiving 1000 trades/sec → 1000 full copies per second
// Each copy: 10,000 × 8 bytes = 80KB
// Per second: 80MB allocation → GC pressure → latency spikes

// ✅ For large or frequently-written: use ConcurrentLinkedQueue
// or ReadWriteLock-protected ArrayList
ReadWriteLock rwl = new ReentrantReadWriteLock();
List<Trade>   tradeList = new ArrayList<>();

void addTrade(Trade t) {
rwl.writeLock().lock();
try { tradeList.add(t); }
finally { rwl.writeLock().unlock(); }
}

List<Trade> getTrades() {
rwl.readLock().lock();
try { return new ArrayList<>(tradeList); }  // Snapshot for safety
finally { rwl.readLock().unlock(); }
}

Part 8: Building Real Systems With Concurrent Collections
Pattern: Thread-Safe Cache With Eviction
javaclass BoundedCache<K, V> {
private final ConcurrentHashMap<K, V>   data;
private final ConcurrentLinkedDeque<K>  accessOrder;  // LRU tracking
private final int                       maxSize;
private final ReadWriteLock             evictionLock = new ReentrantReadWriteLock();

    BoundedCache(int maxSize) {
        this.maxSize     = maxSize;
        this.data        = new ConcurrentHashMap<>(maxSize * 2);
        this.accessOrder = new ConcurrentLinkedDeque<>();
    }

    public V get(String requestId, K key, Supplier<V> loader) {
        // Fast path: check cache first (lock-free read)
        V cached = data.get(key);
        if (cached != null) {
            // Update access order for LRU (non-blocking)
            accessOrder.remove(key);
            accessOrder.addLast(key);
            return cached;
        }

        // Slow path: load and cache
        return data.computeIfAbsent(key, k -> {
            V loaded = loader.get();        // Load from source

            // Track for eviction
            accessOrder.addLast(key);

            // Evict if over limit
            while (data.size() > maxSize) {
                K oldest = accessOrder.pollFirst();
                if (oldest != null) {
                    data.remove(oldest);
                    log.debug("Evicted key: {}", oldest);
                }
            }
            return loaded;
        });
    }

    public void invalidate(K key) {
        data.remove(key);
        accessOrder.remove(key);
    }

    public int size() { return data.size(); }
}


// Production note: for serious caching use Caffeine:
// Cache<String, User> cache = Caffeine.newBuilder()
//     .maximumSize(10_000)
//     .expireAfterWrite(5, TimeUnit.MINUTES)
//     .recordStats()
//     .build(key -> loadFromDB(key));
// Built on ConcurrentHashMap internals, far more sophisticated eviction
Pattern: Event Bus With Concurrent Collections
javaclass ConcurrentEventBus {
// Map: event type → list of listeners
// ConcurrentHashMap: thread-safe registration/lookup
// CopyOnWriteArrayList: thread-safe iteration without external lock
private final ConcurrentHashMap<Class<?>, CopyOnWriteArrayList<EventHandler<?>>>
handlers = new ConcurrentHashMap<>();

    // Thread-safe registration
    public <T> void register(Class<T> eventType, EventHandler<T> handler) {
        handlers.computeIfAbsent(eventType, k -> new CopyOnWriteArrayList<>())
                .add(handler);
    }

    // Thread-safe deregistration
    public <T> void deregister(Class<T> eventType, EventHandler<T> handler) {
        CopyOnWriteArrayList<?> list = handlers.get(eventType);
        if (list != null) list.remove(handler);
    }

    // Thread-safe dispatch — no locking needed during iteration!
    @SuppressWarnings("unchecked")
    public <T> void publish(T event) {
        CopyOnWriteArrayList<EventHandler<?>> list =
            handlers.get(event.getClass());

        if (list == null) return;

        // CopyOnWriteArrayList: safe to iterate while other threads register
        for (EventHandler<?> handler : list) {
            try {
                ((EventHandler<T>) handler).handle(event);
            } catch (Exception e) {
                log.error("Handler failed for event {}: {}", event, e.getMessage(), e);
                // Don't let one bad handler break others
            }
        }
    }
}
Pattern: Work Queue With Priority and Bounded Capacity
javaclass PriorityWorkQueue<T extends Comparable<T>> {
private final PriorityBlockingQueue<T> queue;
private final Semaphore               capacity;   // Add bounded-ness

    PriorityWorkQueue(int maxCapacity) {
        // PriorityBlockingQueue is unbounded — use Semaphore to bound it
        this.queue    = new PriorityBlockingQueue<>(maxCapacity);
        this.capacity = new Semaphore(maxCapacity);
    }

    // Producer: submit with priority
    public boolean submit(T task) throws InterruptedException {
        if (!capacity.tryAcquire(100, TimeUnit.MILLISECONDS)) {
            return false;  // Queue full — reject with false
        }
        queue.offer(task);
        return true;
    }

    // Consumer: take highest priority
    public T take() throws InterruptedException {
        T task = queue.take();  // Blocks for highest-priority item
        capacity.release();     // Release capacity slot
        return task;
    }

    public int size()             { return queue.size(); }
    public int availableSlots()   { return capacity.availablePermits(); }
}

Part 9: The Interview Q&A Round 🎤

Q1. How does ConcurrentHashMap achieve thread safety without locking the whole map?

"In Java 8+, ConcurrentHashMap uses two mechanisms depending on the bucket state. For an empty bucket, it uses a CAS operation to atomically insert the first node — completely lock-free. For a non-empty bucket, it synchronizes only on the first node of that bucket — so different buckets can be written simultaneously by different threads. This is effectively N independent locks where N is the number of buckets, far finer than the 16-segment approach of Java 7.
Reads use volatile reads of node references — no locking at all. Writes update volatile fields — ensures visibility without full lock. The result: under read-heavy workloads, throughput scales linearly with thread count. Under write-heavy workloads, different buckets can be written concurrently — only writes to the SAME bucket contend.
It also handles resizing cooperatively — when resize is triggered, multiple threads help transfer entries to the new table in parallel, each claiming a portion via CAS."


Q2. When would you use CopyOnWriteArrayList over Collections.synchronizedList()?

"Two different use cases for different access patterns. synchronizedList puts one lock on everything — all reads block all writes and vice versa. Under any contention it serializes everything.
CopyOnWriteArrayList makes reads completely lock-free by making the internal array effectively immutable from readers' perspective. Writers copy the entire array, apply the change, then atomically swap the reference. Readers always see a consistent snapshot.
I use CopyOnWriteArrayList specifically when: reads are the overwhelming majority (90%+), writes are rare (listener registration/deregistration), the list is small (copying is cheap), and I need to iterate without holding a lock — which is critical for event systems where you don't want to hold a lock while calling unknown listener code.
I would NOT use it for large lists with frequent writes — O(n) copy on every write would be expensive. There I'd use a ReadWriteLock protecting an ArrayList, which gives concurrent reads and exclusive writes with no copying overhead."


Q3. What is the difference between put() and offer() on a BlockingQueue?

"put() and offer() both add to the queue, but they differ in behavior when the queue is full. put() BLOCKS indefinitely — the calling thread waits until space becomes available. This is the key to natural backpressure in producer-consumer systems: when consumers are slow and the queue fills up, producers automatically slow down.
offer() is non-blocking — it returns false immediately if the queue is full. The offer(timeout, unit) variant waits up to the specified timeout.
Same duality on the take side: take() blocks forever if empty, poll() returns null immediately if empty.
In production, I use put() for normal producer-consumer pipelines (backpressure is desired), offer() with timeout when the producer has other work to do or needs to handle overload gracefully, and drainTo() for batch consumers that want to process multiple items at once."


Q4. Why can't ConcurrentHashMap have null keys or values?

"It's an intentional design decision to avoid an unresolvable ambiguity. With a regular HashMap, get() returning null means either the key doesn't exist, or it exists with a null value — you need a separate containsKey() call to distinguish them. In a single-threaded context that's just awkward. In a concurrent context it's a race condition — between your containsKey() and get() calls, another thread could remove the key.
ConcurrentHashMap was designed for concurrent use where atomic operations matter. Allowing null values would mean get() returning null is ambiguous, and there's no atomic way to resolve that ambiguity. By disallowing null, get() returning null unambiguously means 'key not present', and you can use containsKey() or getOrDefault() without race conditions because ConcurrentHashMap's atomic operations like compute() and putIfAbsent() make null-handling unnecessary anyway."


Q5. Design a thread-safe rate limiter using concurrent collections
java// Sliding window rate limiter: max N requests per second per user
// Use ConcurrentHashMap + ConcurrentLinkedDeque per user

class SlidingWindowRateLimiter {
private final ConcurrentHashMap<String, ConcurrentLinkedDeque<Long>>
userWindows = new ConcurrentHashMap<>();

    private final int maxRequests;     // Max requests per window
    private final long windowMs;       // Window size in milliseconds

    SlidingWindowRateLimiter(int maxRequests, long windowMs) {
        this.maxRequests = maxRequests;
        this.windowMs    = windowMs;
    }

    public boolean tryAcquire(String userId) {
        long now = System.currentTimeMillis();
        long windowStart = now - windowMs;

        // Get or create window for this user
        ConcurrentLinkedDeque<Long> window = userWindows
            .computeIfAbsent(userId, k -> new ConcurrentLinkedDeque<>());

        // Remove expired timestamps from the front of the window
        // (only one thread should clean, but multiple is safe — idempotent)
        while (!window.isEmpty() && window.peekFirst() < windowStart) {
            window.pollFirst();          // Thread-safe removal
        }

        // Check if under limit
        if (window.size() < maxRequests) {
            window.addLast(now);         // Record this request's timestamp
            return true;                 // Request allowed
        }

        return false;  // Rate limit exceeded
    }

    // Periodic cleanup of inactive users
    public void cleanupInactiveUsers() {
        long windowStart = System.currentTimeMillis() - windowMs;
        userWindows.entrySet().removeIf(entry -> {
            ConcurrentLinkedDeque<Long> window = entry.getValue();
            while (!window.isEmpty() && window.peekFirst() < windowStart) {
                window.pollFirst();
            }
            return window.isEmpty();    // Remove user entry if window is empty
        });
    }
}

// Usage:
SlidingWindowRateLimiter limiter = new SlidingWindowRateLimiter(100, 1000); // 100/sec
if (limiter.tryAcquire(userId)) {
processRequest();
} else {
return Response.tooManyRequests();
}
```

---

## Section 7 Master Summary 🧠
```
CONCURRENTHASHMAP (HashMap equivalent):
Java 8+: CAS for empty buckets + synchronized per bucket node
Reads: lock-free (volatile reads)
Writes: node-level locking (N independent locks)
Key atomic ops: putIfAbsent, computeIfAbsent, compute, merge
❌ No null keys or values (intentional design)
⚠️ size() is approximate — don't use for critical logic
⚠️ Pre-size: new ConcurrentHashMap<>(expectedSize / 0.75 + 1)

COPYONWRITEARRAYLIST (ArrayList equivalent):
Reads: completely lock-free (snapshot from current array)
Writes: O(n) copy entire array, then swap reference
Iteration: never throws ConcurrentModificationException
✅ Read-heavy (90%+), rare writes, small list, event listeners
❌ Large lists, frequent writes (O(n) copy is expensive)

BLOCKINGQUEUE (producer-consumer coordination):
put()  → block if FULL (backpressure)
take() → block if EMPTY (wait for work)
offer()/poll() → non-blocking variants
drainTo() → batch drain for efficient consumers

LinkedBlockingQueue(n): two locks, high throughput, general use
ArrayBlockingQueue(n):  one lock, fair option, bounded memory
SynchronousQueue:       zero buffer, direct handoff (CachedPool)
PriorityBlockingQueue:  priority ordered (add Semaphore for bound!)
DelayQueue:             timed release (scheduled tasks, TTL)

CONCURRENTSKIPLISTMAP (TreeMap equivalent):
Lock-free CAS, O(log n) all ops, always sorted
Use for: sorted concurrent maps, range queries, leaderboards

CONCURRENTLINKEDQUEUE:
Lock-free, unbounded, non-blocking
Use when: don't need blocking semantics, polling is fine

DECISION GUIDE:
Concurrent map?       → ConcurrentHashMap
Sorted concurrent map?→ ConcurrentSkipListMap
Read-heavy list?      → CopyOnWriteArrayList
Producer-consumer?    → LinkedBlockingQueue(n) (default choice)
Priority ordering?    → PriorityBlockingQueue + Semaphore
Direct handoff?       → SynchronousQueue
Timed release?        → DelayQueue

KEY PITFALLS:
❌ Compound operations: get() then put() — use compute() instead
❌ Mutable keys in ConcurrentHashMap → hashCode changes → entry lost
❌ Using size() for decisions → approximate, use Semaphore instead
❌ Unbounded queues → OOM under load
❌ CopyOnWriteArrayList for large lists or frequent writes
❌ No null in ConcurrentHashMap — use Optional or sentinel values