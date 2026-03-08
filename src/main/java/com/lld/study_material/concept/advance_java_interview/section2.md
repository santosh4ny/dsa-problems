Section 2: Synchronization — synchronized, volatile, Atomic Classes 🔒
Section 1 taught you WHY we need synchronization (CPU caches, reordering, happens-before). Now we build the tools to FIX those problems. This section is where most candidates lose points — they know the keywords but miss the deep gotchas.

The Problem We're Solving — Quick Recap
Three distinct problems need three distinct solutions:

PROBLEM 1: VISIBILITY
Thread A writes x = 5 → Thread B still reads x = 0
(CPU cache, no flush to main memory)
FIX → volatile

PROBLEM 2: ATOMICITY
counter++ looks like 1 op, it's 3 ops (read-modify-write)
Two threads interleave → lost update
FIX → synchronized / AtomicInteger

PROBLEM 3: ORDERING
CPU reorders x = 42; flag = true to flag = true; x = 42
Other thread sees flag=true but x=0
FIX → volatile / synchronized (both create memory barriers)

Most bugs involve ALL THREE problems simultaneously.
You need to identify which you're solving.

Part 1: synchronized — The Fundamental Lock
Every Java object has a hidden lock called a monitor (or intrinsic lock). synchronized uses it.
The 3 Forms
java// ─── Form 1: Synchronized INSTANCE method — locks on 'this' ──────
class Counter {
private int count = 0;

    public synchronized void increment() {
        count++;
        // Acquires lock on THIS Counter object
        // Only ONE thread can be in ANY synchronized method of this object
    }

    public synchronized int get() {
        return count;
        // SAME lock as increment() — they're mutually exclusive
    }

    public void unsafeRead() {
        System.out.println(count);
        // NO lock — can read while increment() is running!
        // If you want thread safety, ALL accesses must use the lock
    }
}


// ─── Form 2: Synchronized STATIC method — locks on Class object ──
class IdGenerator {
private static int nextId = 0;

    public static synchronized int getNextId() {
        return ++nextId;
        // Locks on IdGenerator.class — ONE lock for ALL instances
    }
}

// IMPORTANT: instance lock ≠ class lock
// These two can run SIMULTANEOUSLY:
new Counter().increment();    // locks on instance
IdGenerator.getNextId();      // locks on IdGenerator.class
// They use DIFFERENT locks — no mutual exclusion between them


// ─── Form 3: Synchronized BLOCK — most flexible ───────────────────
class BankAccount {
private double balance;
private final Object balanceLock = new Object();  // Dedicated private lock
private String owner;
private final Object ownerLock = new Object();    // Separate lock for owner

    // Separate locks for separate concerns — higher concurrency!
    public void deposit(double amount) {
        synchronized (balanceLock) {     // Only locks balance operations
            balance += amount;
        }
        // Owner operations can proceed concurrently with balance operations
    }

    public void setOwner(String name) {
        synchronized (ownerLock) {       // Only locks owner operations
            owner = name;
        }
    }

    // ✅ Why private lock object over 'this'?
    // If you lock on 'this', EXTERNAL code can also lock on 'this':
    BankAccount account = new BankAccount();
    synchronized (account) {  // External code holds account's lock!
        // Now deposit() blocks — external code can cause starvation/deadlock
    }
    // Private lock = only YOUR class controls it = safer
}

Part 2: What synchronized Actually Does
Most candidates say "synchronized makes operations thread-safe." That's incomplete. Here's what it ACTUALLY does:
javaclass Explained {
private int x = 0;
private final Object lock = new Object();

    void writer() {
        synchronized (lock) {   // ← LOCK ACQUIRE
            // Memory barrier: flush everything from this thread's cache
            // to main memory BEFORE entering

            x = 99;             // Critical section
        }                       // ← LOCK RELEASE
        // Memory barrier: flush all writes to main memory
        // Another thread that subsequently acquires this lock
        // is GUARANTEED to see x = 99
    }

    void reader() {
        synchronized (lock) {   // ← LOCK ACQUIRE
            // Memory barrier: invalidate this thread's cache,
            // re-read everything from main memory

            System.out.println(x);  // GUARANTEED to see 99
        }
    }
}

// synchronized gives you ALL THREE:
// 1. MUTUAL EXCLUSION: only one thread in critical section at a time
// 2. VISIBILITY: writes flushed on exit, cache invalidated on entry
// 3. ORDERING: no reordering across lock acquire/release

Part 3: The Hidden Gotchas
Gotcha 1 — Locking on Different Objects
javaclass WrongLock {
private int count = 0;

    void increment() {
        synchronized (new Object()) {   // ❌ NEW object every call!
            count++;
        }
        // Every thread gets a DIFFERENT lock object
        // No mutual exclusion at all — same as no synchronized
    }

    // Also wrong: locking on a String literal
    void alsoWrong() {
        synchronized ("LOCK") {         // ❌ String pool — shared globally!
            count++;
            // String literals are interned — ALL classes that lock on "LOCK"
            // compete for the SAME lock — potential global deadlock
        }
    }

    // Also wrong: locking on boxed primitives
    private Integer id = 0;
    void alsoAlsoWrong() {
        synchronized (id) {             // ❌ Autoboxing creates new objects!
            id++;  // id is now a DIFFERENT object — lock changed mid-flight
        }
    }

    // ✅ Lock on a dedicated final object
    private final Object lock = new Object();  // final = never changes identity
    void correct() {
        synchronized (lock) {
            count++;
        }
    }
}
Gotcha 2 — Synchronized ≠ Atomic for Multiple Operations
javaclass UnsafeCompound {
private final Map<String, Integer> map = new HashMap<>();

    // ❌ Each operation is atomic but together they're NOT
    public void conditionalPut(String key, int value) {
        if (!map.containsKey(key)) {     // Atomic check
            // ← Another thread can insert the key RIGHT HERE
            map.put(key, value);         // Atomic put — but too late!
        }
        // Two threads can both pass the if → both put → one overwrites
    }

    // ✅ FIX: Synchronize the COMPOUND operation
    public synchronized void safePut(String key, int value) {
        if (!map.containsKey(key)) {
            map.put(key, value);
        }
        // Now check AND put are one atomic unit
    }

    // ✅ OR: Use ConcurrentHashMap's atomic method
    private final ConcurrentHashMap<String, Integer> cmap = new ConcurrentHashMap<>();
    public void chmPut(String key, int value) {
        cmap.putIfAbsent(key, value);    // Atomic check-then-put built-in
    }
}
Gotcha 3 — Synchronized is Reentrant (Saves You From Deadlock)
javaclass Reentrant {
// If synchronized was NOT reentrant, this would deadlock:
// methodA acquires lock → calls methodB → tries to acquire same lock → BLOCKED

    public synchronized void methodA() {
        System.out.println("methodA");
        methodB();   // Same thread, already holds lock — can re-enter!
    }

    public synchronized void methodB() {
        System.out.println("methodB");
        // Same thread just "increments" the lock count — no block
    }
}

// JVM tracks: lock holder + hold count
// methodA: Thread A acquires lock, count = 1
// methodB: Thread A re-acquires, count = 2
// methodB exits: count = 1
// methodA exits: count = 0 → lock released
// Another thread can now acquire the lock
Gotcha 4 — Holding Locks Too Long
javaclass LockScope {
private double balance = 1000.0;
private final Object lock = new Object();

    // ❌ BAD: Lock held during expensive I/O
    public synchronized void badWithdraw(double amount) {
        if (balance >= amount) {
            balance -= amount;
            sendEmailNotification(amount);   // 200ms network call
            writeToAuditLog(amount);         // 50ms disk I/O
            // Lock held for 250ms — ALL other threads starve!
        }
    }

    // ✅ GOOD: Minimize critical section
    public void goodWithdraw(double amount) {
        boolean success;
        synchronized (lock) {               // Lock only for state change
            success = (balance >= amount);
            if (success) balance -= amount;
        }                                   // Lock released IMMEDIATELY

        // Do expensive work OUTSIDE lock
        if (success) {
            sendEmailNotification(amount);   // Other threads can proceed
            writeToAuditLog(amount);         // concurrently during this
        }
    }
}
Gotcha 5 — Don't Call Unknown Methods While Holding a Lock (Alien Call)
javaclass EventSystem {
private final List<Runnable> listeners = new ArrayList<>();
private final Object lock = new Object();

    public void addListener(Runnable r) {
        synchronized (lock) { listeners.add(r); }
    }

    // ❌ BAD: Calling unknown external code while holding lock
    public void notifyAll_WRONG() {
        synchronized (lock) {
            for (Runnable listener : listeners) {
                listener.run();  // ← ALIEN CALL!
                // You don't know what listener.run() does
                // It might: acquire another lock → deadlock
                // It might: take 10 seconds → starvation
                // It might: call back into THIS class → reentrant issues
            }
        }
    }

    // ✅ GOOD: Copy, then call outside lock
    public void notifyAll_CORRECT() {
        List<Runnable> snapshot;
        synchronized (lock) {
            snapshot = new ArrayList<>(listeners);  // Copy under lock
        }                                           // Release lock!

        for (Runnable listener : snapshot) {
            listener.run();  // Call alien code WITHOUT holding lock
        }
    }
}

Part 4: volatile — Deep Dive
What volatile Actually Does at the CPU Level
java// Without volatile:

class NoVolatile {
private boolean flag = false;   // No volatile

    void writer() {
        // CPU may:
        // 1. Write flag=true to Core 1's L1 cache
        // 2. NOT flush to main memory immediately
        // 3. JIT may hoist the read OUTSIDE the loop in reader
        flag = true;
    }

    void reader() {
        // JIT optimization can transform this:
        while (!flag) { }

        // Into this (if it "proves" flag never changes in this thread):
        if (!flag) { while(true) { } }  // Infinite loop!
        // This is legal by JMM without volatile
    }
}


// With volatile:

class WithVolatile {
private volatile boolean flag = false;

    void writer() {
        // 1. Write to cache
        // 2. IMMEDIATELY flush to main memory (store-store barrier)
        // 3. All writes BEFORE this are also flushed
        flag = true;
    }

    void reader() {
        // 1. ALWAYS reads from main memory (load-load barrier)
        // 2. All reads AFTER this also read fresh values
        // 3. JIT CANNOT hoist this read — knows it may change
        while (!flag) { }  // Always checks real value
    }
}
volatile — Exactly When to Use It
javaclass VolatileUseCases {

    // ✅ USE CASE 1: Status / control flag (single writer)
    private volatile boolean stopRequested = false;

    void requestStop() { stopRequested = true; }   // One thread
    boolean shouldStop() { return stopRequested; } // Many threads


    // ✅ USE CASE 2: Safe publication of immutable objects
    private volatile Config config = null;

    void updateConfig(Config newConfig) {
        // newConfig is immutable — thread that built it is done
        config = newConfig;   // volatile write publishes it safely
    }

    Config getConfig() {
        return config;        // volatile read — always sees latest
    }


    // ✅ USE CASE 3: Double-checked locking (with volatile)
    private volatile ExpensiveObject lazy = null;

    ExpensiveObject getLazy() {
        if (lazy == null) {                   // Check 1: no lock
            synchronized (this) {
                if (lazy == null) {           // Check 2: with lock
                    lazy = new ExpensiveObject();  // volatile: no reorder
                }
            }
        }
        return lazy;
    }


    // ❌ WRONG USE: compound operations
    private volatile int counter = 0;

    void increment() {
        counter++;   // ❌ NOT atomic! volatile doesn't help here
        // READ from memory, ADD in register, WRITE to memory
        // Another thread can interleave between READ and WRITE
    }

    // ✅ CORRECT: use AtomicInteger for compound ops
    private final AtomicInteger atomicCounter = new AtomicInteger(0);

    void safeIncrement() {
        atomicCounter.incrementAndGet();  // Single atomic CAS instruction
    }
}
```

### volatile vs synchronized — The Decision
```
Ask yourself: What problem am I solving?

VISIBILITY only (one writer, multiple readers, simple assignment)?
→ volatile
→ Faster: no lock, no thread scheduling overhead

ATOMICITY (compound operation: read-modify-write)?
→ synchronized or AtomicInteger
→ volatile alone is NOT enough

BOTH visibility + atomicity + ordering?
→ synchronized
→ Gives you all three

Multiple related variables that must be consistent together?
→ synchronized (volatile can't protect multiple variables atomically)

class Example {
// ❌ Wrong: volatile protects each variable independently
volatile int x = 0;
volatile int y = 0;
// Thread can see x = 1, y = 0 (partial update) — inconsistent!

    // ✅ Correct: synchronized protects them as a unit
    int x = 0;
    int y = 0;
    synchronized void setPoint(int newX, int newY) {
        x = newX;
        y = newY;
        // Atomic: other threads see EITHER (old x, old y) OR (new x, new y)
        // Never (new x, old y) — which would be inconsistent
    }
}
```

---

## Part 5: Atomic Classes — Lock-Free with CAS

### How CAS Works
```
Compare-And-Swap (CAS) is a SINGLE CPU INSTRUCTION (CMPXCHG on x86)

CAS(memory_location, expected_value, new_value):
1. Read current value at memory_location
2. If current == expected:
   Write new_value ATOMICALLY
   Return true (success)
3. If current != expected:
   Do nothing
   Return false (another thread changed it — retry)

Because it's ONE CPU instruction:
No interleaving possible
No lock needed
No thread scheduling overhead
Much faster than synchronized under low-to-medium contention
java// Manual CAS loop — what AtomicInteger.incrementAndGet() actually does:

AtomicInteger ai = new AtomicInteger(5);

int newValue;
do {
int expected = ai.get();          // Read current: 5
newValue     = expected + 1;      // Compute new: 6
// Another thread might change ai HERE
} while (!ai.compareAndSet(expected, newValue));
// If ai is still 5: write 6, return true, exit loop
// If ai changed to 7: return false, retry loop with new expected=7
// Keeps retrying until THIS thread wins the CAS — guaranteed progress

// This is called OPTIMISTIC LOCKING:
// Assume no conflict, try operation, retry if conflict
// vs PESSIMISTIC LOCKING (synchronized): assume conflict, take lock first
Complete Atomic Classes Reference
java// ─── AtomicInteger ────────────────────────────────────────────────
AtomicInteger ai = new AtomicInteger(0);

ai.get();                        // Read (volatile read)
ai.set(10);                      // Write (volatile write)
ai.getAndSet(20);                // Swap: returns 10, sets 20
ai.incrementAndGet();            // ++i: returns new value
ai.getAndIncrement();            // i++: returns old value
ai.decrementAndGet();            // --i
ai.addAndGet(5);                 // += 5, returns new value
ai.getAndAdd(5);                 // += 5, returns old value

// The most powerful: compareAndSet
boolean success = ai.compareAndSet(20, 99); // If 20 → set 99, return true
// If not 20 → no-op, return false

// Java 9+: compareAndExchange (returns WITNESS value not boolean)
int witness = ai.compareAndExchange(99, 100); // If 99 → set 100, return 99
// If not 99 → return actual value
// Useful: if witness != expected, you know exactly what the current value is


// ─── AtomicLong ───────────────────────────────────────────────────
AtomicLong requestCount = new AtomicLong(0);
long id = requestCount.incrementAndGet();  // Thread-safe unique ID generator


// ─── AtomicBoolean ────────────────────────────────────────────────
AtomicBoolean initialized = new AtomicBoolean(false);

// Classic: ensure init runs exactly once, even under concurrency
if (initialized.compareAndSet(false, true)) {
// EXACTLY ONE thread gets true here — the one that won the CAS
expensiveInitialization();
}
// All other threads get false — skip init


// ─── AtomicReference ─────────────────────────────────────────────
AtomicReference<Config> configRef = new AtomicReference<>(loadConfig());

// Atomic hot-swap — no lock needed
Config oldConfig = configRef.get();
Config newConfig = buildNewConfig();
boolean swapped = configRef.compareAndSet(oldConfig, newConfig);
// If config didn't change since we read it: swap succeeds
// If another thread already swapped: our swap fails → retry with new config


// ─── AtomicIntegerArray / AtomicLongArray / AtomicReferenceArray ──
AtomicIntegerArray scores = new AtomicIntegerArray(10);  // 10 atomic ints
scores.incrementAndGet(5);       // Atomically increment index 5
scores.compareAndSet(3, 0, 100); // At index 3: if 0 → set 100


// ─── AtomicStampedReference — solve the ABA problem ──────────────
// ABA Problem:
// Thread A reads value = "A"
// Thread B changes "A" → "B" → "A" (two changes, back to original)
// Thread A does CAS: expected="A", current="A" → SUCCESS
// But Thread A doesn't know value changed twice — may be wrong!

AtomicStampedReference<String> stamped =
new AtomicStampedReference<>("A", 0);  // value="A", stamp=0 (version number)

int[] stampHolder = new int[1];
String value = stamped.get(stampHolder);   // Get value AND stamp
int   stamp  = stampHolder[0];

// CAS on both value AND stamp — ABA impossible
stamped.compareAndSet("A", "B", stamp, stamp + 1);
// Only succeeds if value="A" AND stamp=currentStamp
// Even if value went A→B→A, stamp would be 2, not 0 → CAS fails

Part 6: LongAdder & LongAccumulator — High-Contention Counters
java// Problem with AtomicLong under HIGH contention:
// 100 threads all trying to CAS the same memory location
// Under high contention: most CAS attempts fail → retry → fail → spin
// Called "CAS thrashing" — huge CPU waste

// ─── LongAdder (Java 8+) — solves high contention ────────────────
// Key idea: Instead of ONE shared counter, maintain MULTIPLE cells
// Each thread mostly updates ITS OWN cell — minimal contention
// sum() aggregates all cells at the end

LongAdder hitCounter = new LongAdder();

// Multiple threads can call these simultaneously — minimal contention
hitCounter.increment();        // Add 1 to a cell
hitCounter.add(10);            // Add 10 to a cell
hitCounter.decrement();        // Subtract 1

long total = hitCounter.sum(); // Aggregate all cells — eventual consistency
hitCounter.reset();            // Reset all cells to 0
long sumAndReset = hitCounter.sumThenReset(); // Atomic-ish sum + reset


// ─── When to use which: ───────────────────────────────────────────
// AtomicLong:  when you need compareAndSet() — CAS semantics
//              low-to-medium contention
//              single reader, single writer

// LongAdder:   when you ONLY need increment/add and final sum
//              high contention (many threads incrementing)
//              metrics, counters, statistics
//              request counters, error counters, hit counts

// Benchmark (approximate):
// 10 threads, 10M increments each:
//   AtomicLong:  ~8s  (heavy CAS contention)
//   LongAdder:   ~1s  (8× faster — cell striping)


// ─── LongAccumulator — generalize LongAdder ──────────────────────
// Provide any accumulation function (not just addition)

LongAccumulator maxSalary = new LongAccumulator(Long::max, Long.MIN_VALUE);
// accumulator: (currentMax, newValue) → Math.max(currentMax, newValue)
// identity: Long.MIN_VALUE (neutral element for max)

maxSalary.accumulate(75000);   // Thread 1
maxSalary.accumulate(92000);   // Thread 2
maxSalary.accumulate(65000);   // Thread 3

long max = maxSalary.get();    // 92000 — thread-safe max across all threads

Part 7: ReentrantLock — synchronized's Powerful Sibling
javaimport java.util.concurrent.locks.*;

class AdvancedLocking {
private final ReentrantLock lock = new ReentrantLock();
private final ReentrantLock fairLock = new ReentrantLock(true); // fair=true
private double balance = 1000.0;

    // ─── Basic usage — like synchronized but explicit ─────────────
    public void basicDeposit(double amount) {
        lock.lock();             // Acquire — blocks if held by other thread
        try {
            balance += amount;   // Critical section
        } finally {
            lock.unlock();       // ALWAYS in finally — never skip!
        }                        // If you forget: lock NEVER released → deadlock
    }


    // ─── tryLock() — don't block forever ─────────────────────────
    public boolean tryDeposit(double amount) {
        if (lock.tryLock()) {    // Try to acquire — returns IMMEDIATELY
            try {
                balance += amount;
                return true;
            } finally {
                lock.unlock();
            }
        }
        return false;            // Lock busy — do something else instead
    }


    // ─── tryLock(timeout) — block for limited time ────────────────
    public boolean timedDeposit(double amount) {
        try {
            if (lock.tryLock(100, TimeUnit.MILLISECONDS)) {
                try {
                    balance += amount;
                    return true;
                } finally {
                    lock.unlock();
                }
            }
            // Gave up after 100ms — avoid deadlock, try later
            return false;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }


    // ─── lockInterruptibly() — can be interrupted while waiting ──
    public void interruptibleDeposit(double amount) throws InterruptedException {
        lock.lockInterruptibly();  // Blocks, but responds to interrupt()
        try {
            balance += amount;
        } finally {
            lock.unlock();
        }
        // synchronized: once waiting for monitor, can't be interrupted
        // lockInterruptibly(): interrupt() wakes thread with InterruptedException
        // Use for: tasks that should be cancellable while waiting for lock
    }


    // ─── Fair lock — FIFO ordering ────────────────────────────────
    // new ReentrantLock(false) — DEFAULT, unfair: threads barging allowed
    // new ReentrantLock(true)  — fair: strict FIFO, no thread starves
    public void fairDeposit(double amount) {
        fairLock.lock();
        try {
            balance += amount;
        } finally {
            fairLock.unlock();
        }
        // Fair lock: longest-waiting thread always gets lock next
        // Cost: lower throughput (~30%) — worth it only to prevent starvation
    }


    // ─── Diagnostic methods ───────────────────────────────────────
    void diagnostics() {
        lock.isLocked();              // Is anyone holding it?
        lock.isHeldByCurrentThread(); // Does THIS thread hold it?
        lock.getHoldCount();          // How deep is current thread's reentrance?
        lock.getQueueLength();        // How many threads waiting?
        lock.hasQueuedThreads();      // Anyone waiting?
    }
}
Multiple Conditions — synchronized's Big Limitation
java// synchronized has ONE wait set per object — notify() wakes a RANDOM thread
// Problem: notifyAll() wakes producers AND consumers — wasteful

// ReentrantLock gives you MULTIPLE Conditions — precise signaling

class BoundedBuffer<T> {
private final Queue<T>      buffer = new LinkedList<>();
private final int           capacity;
private final ReentrantLock lock    = new ReentrantLock();
private final Condition     notFull  = lock.newCondition(); // Producers wait here
private final Condition     notEmpty = lock.newCondition(); // Consumers wait here

    BoundedBuffer(int capacity) { this.capacity = capacity; }

    public void put(T item) throws InterruptedException {
        lock.lock();
        try {
            while (buffer.size() == capacity) {
                notFull.await();         // Producer waits — releases lock
                // Only woken by: notFull.signal() or notFull.signalAll()
                // NOT woken by notEmpty.signal() — precise!
            }
            buffer.add(item);
            notEmpty.signal();           // Wake ONE consumer (not producers!)
        } finally {
            lock.unlock();
        }
    }

    public T take() throws InterruptedException {
        lock.lock();
        try {
            while (buffer.isEmpty()) {
                notEmpty.await();        // Consumer waits
            }
            T item = buffer.poll();
            notFull.signal();            // Wake ONE producer (not consumers!)
            return item;
        } finally {
            lock.unlock();
        }
    }
}

// With synchronized + wait/notifyAll:
// notifyAll() wakes EVERYONE — producers AND consumers
// They all wake up, check condition, most go back to sleep
// Thundering herd problem — wasted context switches

// With ReentrantLock + Conditions:
// notFull.signal() wakes ONLY producers
// notEmpty.signal() wakes ONLY consumers
// Much more efficient

Part 8: ReadWriteLock — For Read-Heavy Workloads
java// synchronized: only ONE thread can read OR write at a time
// ReadWriteLock: MULTIPLE readers simultaneously, OR one exclusive writer

class UserRepository {
private final Map<String, User>  store   = new HashMap<>();
private final ReadWriteLock      rwLock  = new ReentrantReadWriteLock();
private final Lock               readLock  = rwLock.readLock();
private final Lock               writeLock = rwLock.writeLock();

    // ─── Multiple threads can call get() simultaneously ──────────
    public User get(String id) {
        readLock.lock();
        try {
            return store.get(id);     // Concurrent reads: fine!
        } finally {
            readLock.unlock();
        }
    }

    // ─── Write blocks ALL readers until done ─────────────────────
    public void save(User user) {
        writeLock.lock();             // Exclusive: blocks readers + writers
        try {
            store.put(user.getId(), user);
        } finally {
            writeLock.unlock();
        }
    }

    // ─── Cache-aside pattern with lock upgrade ────────────────────
    public User getOrLoad(String id) {
        // Try read first
        readLock.lock();
        try {
            User cached = store.get(id);
            if (cached != null) return cached;  // Cache hit — fast path
        } finally {
            readLock.unlock();
        }

        // Cache miss — need to load and store (write lock)
        writeLock.lock();
        try {
            // Double-check: another thread may have loaded while we waited
            User cached = store.get(id);
            if (cached != null) return cached;

            User loaded = loadFromDatabase(id);
            store.put(id, loaded);
            return loaded;
        } finally {
            writeLock.unlock();
        }
    }

    // ─── When ReadWriteLock pays off: ────────────────────────────
    // 90% reads, 10% writes → 90% of the time, full concurrency
    // 50/50 reads/writes → might be slower than synchronized (lock overhead)
    // Profile before using!
}

Part 9: StampedLock — Java 8, Even Faster
java// StampedLock: Even more optimistic than ReadWriteLock
// Adds OPTIMISTIC READ — no actual lock acquisition for reads!

class PointCache {
private double x, y;
private final StampedLock sl = new StampedLock();

    public void move(double newX, double newY) {
        long stamp = sl.writeLock();      // Exclusive write lock
        try {
            x = newX;
            y = newY;
        } finally {
            sl.unlockWrite(stamp);
        }
    }

    // ─── Optimistic read — no lock at all! ────────────────────────
    public double[] getPosition() {
        long stamp = sl.tryOptimisticRead();  // Just reads a stamp (version number)
        double curX = x, curY = y;            // Read WITHOUT a lock

        if (!sl.validate(stamp)) {
            // A write happened while we were reading — values may be inconsistent
            // Fall back to a real read lock
            stamp = sl.readLock();
            try {
                curX = x;
                curY = y;
            } finally {
                sl.unlockRead(stamp);
            }
        }

        return new double[]{ curX, curY };
    }
}

// Performance hierarchy (reads): StampedLock > ReadWriteLock > synchronized
// Use StampedLock when: very hot read path, writes are rare

// ⚠️ StampedLock is NOT reentrant — calling readLock() twice on same thread → deadlock!
// ⚠️ More complex API — easier to get wrong
// ⚠️ Doesn't integrate with Condition variables
// Use synchronized/ReentrantLock unless you've profiled and need the extra perf

Part 10: The Interview Q&A Round 🎤

Q1. What is the difference between synchronized and ReentrantLock?

"synchronized is built into the JVM — simpler, automatic unlock (even on exception via bytecode), reentrant. But it has limitations: you can't try to acquire without blocking, you can't be interrupted while waiting, and you get only one condition variable per lock.
ReentrantLock gives you explicit control: tryLock() for non-blocking acquisition, tryLock(timeout) to avoid blocking forever, lockInterruptibly() for cancellable waits, multiple Condition objects for precise signaling, and a fair mode to prevent starvation.
My rule: use synchronized for 90% of cases — simpler and safer (automatic unlock). Reach for ReentrantLock when I specifically need tryLock() to avoid deadlock, or multiple conditions for efficient producer-consumer signaling."


Q2. What is the ABA problem and how do you solve it?
java// ABA Problem:
// T1 reads counter = "A" (value 5)
// T2 changes: 5 → 3 → 5  (two changes, back to 5)
// T1 does CAS: expected=5, current=5 → SUCCESS
// T1 doesn't know the value changed twice
// In some algorithms (lock-free stack, queue) this causes corruption

// Solution: AtomicStampedReference — CAS on value + version stamp
AtomicStampedReference<Integer> ref =
new AtomicStampedReference<>(5, 0);  // value=5, stamp=0

// Thread 1 reads
int[] stamp1 = new int[1];
int val1 = ref.get(stamp1);              // val1=5, stamp1[0]=0

// Thread 2 changes 5 → 3 → 5
ref.compareAndSet(5, 3, 0, 1);           // stamp: 0→1
ref.compareAndSet(3, 5, 1, 2);           // stamp: 1→2

// Thread 1 tries CAS — FAILS because stamp changed (0 → 2)
boolean success = ref.compareAndSet(
val1, 10,          // value: 5 → 10
stamp1[0],         // expected stamp: 0
stamp1[0] + 1      // new stamp: 1
);
// success = FALSE — stamp is 2, not 0 — ABA detected!

Q3. Why is notify() usually wrong and notifyAll() safer?
javaclass WaitNotify {
private boolean ready = false;
private final Object lock = new Object();

    // ─── notify() — wakes ONE RANDOM waiting thread ──────────────
    void signalOne() {
        synchronized (lock) {
            ready = true;
            lock.notify();       // Wakes ONE random thread
        }
        // Problem: if woken thread can't proceed (spurious or wrong condition),
        // and other waiting threads CAN proceed — they stay asleep!
        // Missed signal → threads stuck forever
    }

    // ─── notifyAll() — wakes ALL waiting threads ──────────────────
    void signalAll() {
        synchronized (lock) {
            ready = true;
            lock.notifyAll();    // Wake everyone — let them all recheck condition
        }
        // All threads wake, recheck 'while (!ready)' condition
        // The ones that can proceed → proceed
        // The rest → go back to wait
        // No missed signals — safer by default
    }

    // ─── ALWAYS use while, not if ─────────────────────────────────
    void waitForReady() throws InterruptedException {
        synchronized (lock) {
            while (!ready) {     // ✅ while — recheck after waking
                lock.wait();     // May wake spuriously!
            }
            // if (!ready) { wait(); } ← ❌ if — might miss spurious wakeup
        }
    }
}

// Rule: notify() is only safe when:
// 1. ALL waiting threads check the SAME condition
// 2. Exactly ONE thread should proceed per notification
// notifyAll() is always safe (just slightly less efficient)
// When in doubt: use notifyAll() + while loop

Q4. What is a spurious wakeup and why does the while loop pattern exist?

"A spurious wakeup is when a thread waiting on wait() wakes up without being notified — notify() was never called. This is not a Java bug; it's an artifact of how POSIX condition variables work on some operating systems. The JVM spec explicitly allows it.
This is why the pattern is always while (!condition) { wait(); } never if (!condition) { wait(); }. With if, a spurious wakeup would let the thread proceed even though the condition isn't met. With while, the thread rechecks the condition — if it's still false, it waits again. This also handles the case where notifyAll() wakes multiple threads but only one can proceed — the others recheck and go back to sleep."


Q5. What is false sharing and how do you fix it?
java// False sharing: two unrelated variables end up in the same CPU cache line
// When Thread A writes varA, it invalidates Thread B's cache line
// Thread B's varB wasn't changed — but B must reload from memory anyway!

// Cache line = 64 bytes typically
// Two adjacent longs = 16 bytes = same cache line

class FalseSharing {
// ❌ BAD: counter1 and counter2 share a cache line
volatile long counter1 = 0;  // ─┐ Same 64-byte cache line!
volatile long counter2 = 0;  // ─┘
// Thread A increments counter1 → invalidates Thread B's cache line
// Thread B must reload counter2 from memory — even though it didn't change!
// Under high contention: MASSIVE performance hit
}

// ✅ FIX 1: @Contended annotation (Java 8+, JVM flag needed)
class NoFalseSharing {
@jdk.internal.vm.annotation.Contended
volatile long counter1 = 0;  // Padded to its own cache line

    @jdk.internal.vm.annotation.Contended
    volatile long counter2 = 0;  // Different cache line
}

// ✅ FIX 2: Manual padding (no JVM flags needed)
class PaddedCounter {
volatile long value = 0;
// Pad to 64 bytes: 8 bytes (value) + 56 bytes padding = 64 bytes
long p1, p2, p3, p4, p5, p6, p7;  // 56 bytes padding
}

// ✅ FIX 3: Use LongAdder — designed to avoid false sharing
// Each thread writes to its own cell — cells are padded!
LongAdder counter = new LongAdder();

// Why this matters in interviews:
// FAANG systems have 100+ cores pounding the same counters
// False sharing can 10× your latency under load
// It's invisible in tests (single thread = no sharing) — production only

Q6. You have a counter read by 1000 threads and written by 10 threads. What do you use?
java// Option A: synchronized
class SynchronizedCounter {
private long count = 0;
public synchronized void increment() { count++; }
public synchronized long get() { return count; }
// Simple but: ALL 1000 reads + 10 writes serialized — poor read concurrency
}

// Option B: ReadWriteLock
class RWCounter {
private long count = 0;
private final ReadWriteLock rwl = new ReentrantReadWriteLock();

    public void increment() {
        rwl.writeLock().lock();
        try { count++; }
        finally { rwl.writeLock().unlock(); }
    }

    public long get() {
        rwl.readLock().lock();
        try { return count; }
        finally { rwl.readLock().unlock(); }
    }
    // Better: 1000 reads can run concurrently
    // Writes still exclusive — 10 writers serialized
}

// Option C: LongAdder + AtomicLong (depends on requirements)
class BestCounter {
private final LongAdder adder = new LongAdder();

    public void increment() { adder.increment(); }

    // sum() is eventual — may not see ALL increments instantly
    // Use for: metrics, statistics where slight lag is OK
    public long get() { return adder.sum(); }
}

// Option D: AtomicLong
class AtomicCounter {
private final AtomicLong count = new AtomicLong(0);
public void increment() { count.incrementAndGet(); }
public long get() { return count.get(); }
// Good for moderate contention — CAS spinning under high contention
}

// Answer for the interview:
// 1000 readers, 10 writers, reads need to be EXACT → ReadWriteLock
// 1000 readers, 10 writers, reads can be APPROXIMATE → LongAdder
// Extreme contention on writes → LongAdder always wins
```

---

## Section 2 Master Summary 🧠
```
synchronized:
✅ Mutual exclusion (one thread at a time)
✅ Visibility (flush on unlock, reload on lock)
✅ Ordering (memory barrier at acquire/release)
✅ Reentrant (same thread can re-enter)
❌ Cannot tryLock, cannot interrupt while waiting
❌ Only one condition variable per lock

volatile:
✅ Visibility (all writes immediately visible)
✅ Ordering (no reordering across volatile access)
❌ NOT atomic for compound operations (i++)
Use for: status flags, safe publication, DCL

AtomicXxx (CAS-based):
✅ Atomic compound ops (incrementAndGet, compareAndSet)
✅ Lock-free — no thread scheduling overhead
✅ Fast under low-to-medium contention
Use for: counters, accumulators, one-off CAS

LongAdder:
✅ Fastest for high-contention counters
✅ Cell striping reduces CAS thrashing
Use for: metrics, statistics, hit counts

ReentrantLock (over synchronized when you need):
→ tryLock() — avoid blocking / deadlock
→ tryLock(timeout) — give up if too slow
→ lockInterruptibly() — cancellable waits
→ Multiple Condition objects — precise signaling
→ Fair mode — prevent starvation

ReadWriteLock:
→ Read-heavy workloads (90%+ reads)
→ Multiple readers concurrently
→ One exclusive writer

Key rules:
ALWAYS unlock() in finally block (ReentrantLock)
ALWAYS use while loop with wait() — never if
ALWAYS lock on the SAME object for mutual exclusion
NEVER call external methods while holding a lock
MINIMIZE critical section size — do I/O outside lock