Section 1: Java Memory Model & Threading Fundamentals 

    Let's build from the absolute foundation. Every concurrency bug you'll ever debug traces back to what we cover here.

Part 1: What Even IS a Thread?

Before JMM, you need to understand what's actually happening in hardware.
Your Java Program runs on a CPU that looks like this:
```
┌─────────────────────────────────────────────────────────┐
│                     COMPUTER                            │
│                                                         │
│   Core 1              Core 2              Core 3        │
│ ┌─────────┐         ┌─────────┐         ┌─────────┐    │
│ │  L1     │         │  L1     │         │  L1     │    │
│ │ Cache   │         │ Cache   │         │ Cache   │    │
│ │ (fast)  │         │ (fast)  │         │ (fast)  │    │
│ └────┬────┘         └────┬────┘         └────┬────┘    │
│      │                   │                   │         │
│      └──────────┬─────────┘         ┌────────┘         │
│                 │                   │                   │
│            ┌───┴───────────────────┴───┐               │
│            │      MAIN MEMORY (RAM)    │               │
│            │   (shared, slow)          │               │
│            └───────────────────────────┘               │
└─────────────────────────────────────────────────────────┘
```
Thread A runs on Core 1 → uses Core 1's L1 cache
Thread B runs on Core 2 → uses Core 2's L1 cache

If Thread A writes x = 5:
→ It writes to Core 1's L1 cache FIRST
→ Main memory update may be DELAYED
→ Thread B reads x → gets Core 2's cache → still sees x = 0 !!!

This is the ROOT CAUSE of every visibility bug.

This is not a Java bug. This is how CPUs work for performance. Java Memory Model is the set of rules that tell you WHEN these caches are guaranteed to be synchronized.


Part 2: The Java Memory Model (JMM)
JMM answers one question:

"When is a write by Thread A guaranteed to be visible to Thread B?"

The answer is: only when there is a happens-before relationship.
Happens-Before means:
If action A happens-before action B
→ then ALL of A's writes are VISIBLE to B
→ and A happens BEFORE B in time

Without happens-before between two threads:
→ Result is UNDEFINED
→ Could see stale values, partial writes, or anything
→ This is called a DATA RACE
The 5 Ways to Create Happens-Before
java// ── Rule 1: Thread.start() ────────────────────────────────────────
// Everything BEFORE start() happens-before ANYTHING in the new thread

int data = 42;                    // Write BEFORE start()
Thread t = new Thread(() -> {
System.out.println(data);     // GUARANTEED to see 42
// Because: write → start() → thread's first action
});
t.start();


// ── Rule 2: Thread.join() ─────────────────────────────────────────
// Everything IN the thread happens-before join() RETURNS

int[] result = new int[1];
Thread worker = new Thread(() -> {
result[0] = expensiveCompute();   // Write inside thread
});
worker.start();
worker.join();                        // join() returns → happens-after thread

System.out.println(result[0]);        // GUARANTEED to see computed value
// Without join(): UNDEFINED — main thread may see result[0] = 0


// ── Rule 3: synchronized unlock → lock ───────────────────────────
// Unlock of monitor M happens-before every subsequent lock of M

class SharedState {
private int value = 0;
private final Object lock = new Object();

    void write() {
        synchronized (lock) {
            value = 99;
        }                     // UNLOCK ← happens-before
    }

    void read() {
                              // ← LOCK
        synchronized (lock) {
            System.out.println(value); // GUARANTEED to see 99
        }
    }
}


// ── Rule 4: volatile write → volatile read ───────────────────────
// Write to volatile X happens-before every subsequent read of volatile X

class FlagExample {
private volatile boolean ready = false;
private int data = 0;

    void producer() {
        data  = 42;           // Regular write
        ready = true;         // volatile WRITE ← memory barrier here
    }

    void consumer() {
        if (ready) {          // volatile READ ← if this sees true
            System.out.println(data); // GUARANTEED to see 42
            // Because: volatile write → volatile read → happens-before chain
        }
    }
}


// ── Rule 5: Static initializer ────────────────────────────────────
// Class initialization completes happens-before first use of class

class Config {
static final String ENV;
static {
ENV = System.getenv("APP_ENV");  // Runs once, thread-safe
}
}
// Any thread using Config.ENV is GUARANTEED to see the initialized value

Part 3: The Visibility Bug — Live Example
java// This code has a visibility bug. Can you spot it?

public class StopTask {
private static boolean stop = false;   // ← no volatile!

    public static void main(String[] args) throws InterruptedException {

        Thread worker = new Thread(() -> {
            int count = 0;
            while (!stop) {      // Worker reads 'stop' from ITS CPU cache
                count++;
            }
            System.out.println("Stopped! count = " + count);
        });

        worker.start();
        Thread.sleep(1000);

        stop = true;             // Main thread writes to ITS CPU cache
        System.out.println("stop = true set");
    }
}
```
```
What actually happens on modern JVM:

Timeline:
t=0:    Worker starts, loads stop=false into Core 2's L1 cache
t=1s:   Main sets stop=true in Core 1's L1 cache
t=1s+:  Worker STILL reads stop=false from Core 2's cache
t=∞:    Worker loops forever — JVM may even hoist the read OUT of the loop
(JIT optimization: "stop never changes in this thread → cache in register")

On some JVMs/OSes: might work fine. On others: infinite loop.
This is what "undefined behavior" means in JMM.
java// THE FIX: volatile

public class StopTaskFixed {
private static volatile boolean stop = false;  // volatile!
//                     ^^^^^^^^

    // volatile guarantees:
    // 1. Write to 'stop' flushes to main memory immediately
    // 2. Read of 'stop' always reads from main memory
    // 3. Write happens-before subsequent read (no JIT hoisting)

    public static void main(String[] args) throws InterruptedException {
        Thread worker = new Thread(() -> {
            int count = 0;
            while (!stop) {      // Always reads fresh value from main memory
                count++;
            }
            System.out.println("Stopped! count = " + count);
        });

        worker.start();
        Thread.sleep(1000);
        stop = true;             // Immediately visible to ALL threads
        worker.join();
    }
}

Part 4: Instruction Reordering — The Sneaky Problem
Even trickier than visibility is reordering. The CPU and JVM can reorder your instructions — as long as the result is the same for a single thread.
java// This looks fine. It isn't.

class Writer {
int    x = 0;
boolean flag = false;

    void write() {
        x    = 42;       // Step 1
        flag = true;     // Step 2
    }
}

class Reader {
void read(Writer w) {
if (w.flag) {           // Sees flag = true
System.out.println(w.x);  // What does this print?
}
}
}
```
```
You'd expect: if flag is true, x must be 42.
WRONG.

The CPU can reorder write() to:
flag = true;    ← Step 2 FIRST
x = 42;         ← Step 1 SECOND

Because: from a single-thread view, the result of write() is identical.
But from Reader's view:
flag = true  ← Reader sees this
x = 42       ← Reader reads x BEFORE this executes!

Reader prints 0 instead of 42. Or anything else.
java// FIX: volatile on flag creates a memory barrier

class WriterFixed {
int             x    = 0;
volatile boolean flag = false;   // volatile!

    void write() {
        x    = 42;        // Must complete BEFORE...
        flag = true;      // ...this volatile write
        // volatile write = memory barrier
        // All writes BEFORE the barrier are flushed to memory FIRST
        // Reordering across volatile write is FORBIDDEN by JMM
    }
}

class ReaderFixed {
void read(WriterFixed w) {
if (w.flag) {              // volatile read = memory barrier
System.out.println(w.x);  // GUARANTEED to see 42
// All writes before volatile write are visible after volatile read
}
}
}

Part 5: The Double-Checked Locking Bug (FAANG Classic)
This is one of the most famous Java bugs. Every interviewer knows it.
java// BROKEN — the most common wrong Singleton

class BrokenSingleton {
private static BrokenSingleton instance;

    public static BrokenSingleton getInstance() {
        if (instance == null) {                      // Check 1 — no lock
            synchronized (BrokenSingleton.class) {
                if (instance == null) {              // Check 2 — inside lock
                    instance = new BrokenSingleton();  // ← THE PROBLEM
                }
            }
        }
        return instance;
    }
}
```
```
Why is 'instance = new BrokenSingleton()' a problem?

It looks like 1 instruction. It's actually 3 CPU operations:
Op 1: Allocate memory block
Op 2: Initialize the object (call constructor, set fields)
Op 3: Assign reference to 'instance'

CPU is ALLOWED to reorder Op 2 and Op 3:
Op 1: Allocate memory
Op 3: Assign reference  ← instance is now NON-NULL
Op 2: Initialize object ← but NOT constructed yet!

Thread A is inside synchronized, doing Op 1 → Op 3 → (not yet Op 2)
Thread B hits 'if (instance == null)' — Check 1, NO LOCK
→ instance is non-null (Op 3 happened)
→ Thread B RETURNS the half-constructed object
→ Thread B calls a method on it → NullPointerException or corrupt state
java// FIX: volatile on instance

class CorrectSingleton {
private static volatile BrokenSingleton instance;
//             ^^^^^^^^

    public static BrokenSingleton getInstance() {
        if (instance == null) {
            synchronized (BrokenSingleton.class) {
                if (instance == null) {
                    instance = new BrokenSingleton();
                    // volatile write = memory barrier
                    // Op 2 (initialize) MUST complete BEFORE Op 3 (assign)
                    // Any thread reading non-null instance sees fully-constructed object
                }
            }
        }
        return instance;
    }
}

// EVEN BETTER: Bill Pugh Holder — no volatile, no synchronized, cleanest
class BestSingleton {
private BestSingleton() {}

    private static class Holder {
        // Class loading is lazy AND thread-safe by JVM spec
        // This class loads only when getInstance() is first called
        static final BestSingleton INSTANCE = new BestSingleton();
    }

    public static BestSingleton getInstance() {
        return Holder.INSTANCE;  // Thread-safe, no lock overhead
    }
}

// ALSO GREAT: Enum Singleton
enum EnumSingleton {
INSTANCE;
// Safe against reflection attacks, serialization, multi-classloader
// Recommended by Joshua Bloch (Effective Java)
}
```

---

## Part 6: Thread Lifecycle — All 6 States
```
                    ┌─────────┐
              ──────│   NEW   │
              │     └────┬────┘
              │          │ start()
              │          ▼
              │     ┌──────────┐   ◄── CPU scheduler decides
              │  ┌──│ RUNNABLE │──┐     when this runs
              │  │  └──────────┘  │
              │  │                │
              │  │ synchronized   │ wait() / join()
              │  │ lock contention│ LockSupport.park()
              │  ▼                ▼
              │ ┌─────────┐  ┌─────────┐
              │ │ BLOCKED │  │ WAITING │
              │ └────┬────┘  └────┬────┘
              │      │            │
              │ got  │      notify() /
              │ lock │      join complete
              │      │            │
              │      └────────────┤
              │                   ▼
              │          ┌───────────────┐
              │          │ TIMED_WAITING │  sleep(ms)
              │          │               │  wait(ms)
              │          └───────┬───────┘  join(ms)
              │                  │ timeout
              │                  ▼
              └──────────▶ TERMINATED ◄── exception / done
java// Observe thread states yourself:

Thread t = new Thread(() -> {
try {
Thread.sleep(2000);         // Will be TIMED_WAITING
} catch (InterruptedException e) {
Thread.currentThread().interrupt();
}
});

System.out.println(t.getState());   // NEW
t.start();
Thread.sleep(100);
System.out.println(t.getState());   // TIMED_WAITING
t.join();
System.out.println(t.getState());   // TERMINATED


// BLOCKED vs WAITING — people confuse these constantly:

Object lock = new Object();

Thread t1 = new Thread(() -> {
synchronized (lock) {
try { lock.wait(); }        // WAITING — released lock, waiting for notify
catch (InterruptedException e) { Thread.currentThread().interrupt(); }
}
});

Thread t2 = new Thread(() -> {
synchronized (lock) {           // BLOCKED — trying to acquire lock
System.out.println("t2 got lock");
}
});

t1.start();
Thread.sleep(100);                  // Give t1 time to acquire lock and wait()
t2.start();
Thread.sleep(100);

System.out.println("t1: " + t1.getState());  // WAITING (released lock via wait())
System.out.println("t2: " + t2.getState());  // RUNNABLE (lock is free since t1 wait()'d)
// If t1 was in synchronized WITHOUT wait(): t2 would be BLOCKED

Part 7: Creating Threads — 4 Ways
java// ── Way 1: Extend Thread ──────────────────────────────────────────
// DON'T — wastes Java's single inheritance
class DownloadThread extends Thread {
private final String url;
DownloadThread(String url) { this.url = url; }

    @Override
    public void run() {
        System.out.println("Downloading " + url + " on " + getName());
    }
}
new DownloadThread("https://api.example.com").start();


// ── Way 2: Implement Runnable ─────────────────────────────────────
// Separates task from thread mechanism — better
class ParseTask implements Runnable {
private final String data;
ParseTask(String data) { this.data = data; }

    @Override
    public void run() {
        System.out.println("Parsing on " + Thread.currentThread().getName());
    }
}
new Thread(new ParseTask("json...")).start();


// ── Way 3: Callable — when you need a result ──────────────────────
// Runnable can't return a value and can't throw checked exceptions
// Callable solves both
class PrimeChecker implements Callable<Boolean> {
private final int n;
PrimeChecker(int n) { this.n = n; }

    @Override
    public Boolean call() throws Exception {    // can throw!
        if (n < 2) return false;
        for (int i = 2; i <= Math.sqrt(n); i++)
            if (n % i == 0) return false;
        return true;
    }
}

ExecutorService exec = Executors.newFixedThreadPool(4);
Future<Boolean> result = exec.submit(new PrimeChecker(97));
System.out.println("97 is prime: " + result.get());  // true


// ── Way 4: Lambda (Java 8+) ───────────────────────────────────────
// Cleanest for simple tasks
new Thread(() -> System.out.println("Hello from lambda thread!")).start();

// With executor (THE REAL PRODUCTION WAY)
ExecutorService pool = Executors.newFixedThreadPool(4);
pool.submit(() -> processOrder(order));
pool.submit(() -> sendEmail(user));

Part 8: Critical Thread Methods
java// ── sleep() — pause execution ────────────────────────────────────
Thread.sleep(1000);        // Pause 1 second
// ✅ Releases CPU to other threads
// ❌ Does NOT release locks — synchronized blocks stay locked!
// ❌ Must handle InterruptedException
// Use for: simple delays, retry backoff, polling

// ── join() — wait for another thread ─────────────────────────────
Thread worker = new Thread(() -> doHeavyWork());
worker.start();
worker.join();             // This thread BLOCKS until worker finishes
worker.join(5000);         // Wait MAX 5 seconds (then continue regardless)
// Use for: fan-out + fan-in patterns, waiting for initialization

// ── interrupt() — cooperative cancellation ───────────────────────
// Does NOT kill the thread forcefully
// Sets an interrupt FLAG on the thread
// The thread must CHECK the flag and stop voluntarily

Thread t = new Thread(() -> {
while (!Thread.currentThread().isInterrupted()) {  // Check flag
try {
doWork();
Thread.sleep(100);     // sleep() ALSO checks interrupt flag
// and throws InterruptedException
} catch (InterruptedException e) {
// sleep() cleared the flag when it threw
// RESTORE it so callers can see we were interrupted
Thread.currentThread().interrupt();
System.out.println("Gracefully shutting down");
return;   // Clean exit
}
}
});

t.start();
Thread.sleep(500);
t.interrupt();   // Request stop — doesn't kill it, sets the flag

// ── setDaemon() — background threads ─────────────────────────────
Thread monitor = new Thread(() -> { while(true) collectMetrics(); });
monitor.setDaemon(true);   // JVM doesn't wait for daemon threads to finish
monitor.start();           // MUST call before start()!
// When all non-daemon threads finish → JVM exits → daemons are killed
// Examples of daemon threads: GC thread, JIT compiler thread
```

---

## Part 9: Common Interview Questions 🎤

---

**Q1. What is a race condition? And what causes it at the hardware level?**

> *"A race condition is when the correctness of a program depends on the relative timing or interleaving of multiple threads — and some interleavings produce wrong results.*
>
> *At the hardware level, it happens because modern CPUs have per-core caches and can reorder instructions. Thread A writes a value — it sits in Core 1's cache. Thread B reads on Core 2 — sees the old value. Or Thread A does read-modify-write in 3 steps, Thread B interleaves between those steps.*
>
> *The classic forms are: check-then-act (you check if balance >= 100, then another thread withdraws, then you withdraw — balance goes negative), and read-modify-write (counter++ is 3 non-atomic ops — two threads both read 5, both write 6, one increment is lost).*
>
> *Fix: synchronize the critical section, use atomic classes, or eliminate shared mutable state entirely.*"

---

**Q2. What is the difference between a process and a thread?**
```
Process:
- Independent program in execution
- Own memory space (heap, stack, code, data)
- Isolated — cannot directly access another process's memory
- Inter-process: sockets, pipes, shared memory (explicit)
- Heavy to create — OS must set up entire address space
- Crash in one process doesn't affect others

Thread:
- Unit of execution WITHIN a process
- Shares process's HEAP memory with other threads
- Each thread has its own STACK (local variables, call frames)
- Lightweight — no new address space
- Fast to create (~1ms vs ~10ms for process)
- Crash in one thread → can bring down entire process

JVM is one process.
All your threads: GC thread, JIT thread, main thread, your threads
→ all inside that one JVM process, sharing the heap.

Q3. What happens if two threads call start() on the same Thread object?

"The second call throws IllegalThreadStateException. A Thread object is a one-time-use wrapper. Once started, it transitions to RUNNABLE and eventually to TERMINATED — it can never go back to NEW. If you need to run the same task again, create a new Thread instance. This is another reason to prefer ExecutorService — it handles thread lifecycle and reuse for you."


Q4. Why is volatile not enough to make counter++ thread-safe?
java// counter++ is NOT one operation — it's THREE:
private volatile int counter = 0;

void increment() {
counter++;
// Compiled to:
// Step 1: READ  — load counter from memory into register
// Step 2: ADD   — register = register + 1
// Step 3: WRITE — store register back to counter
}

// volatile only makes Step 1 read from main memory
// and Step 3 write to main memory
// But another thread can interleave between Step 1 and Step 3!

Thread A: READ counter = 5
Thread B: READ counter = 5    ← sees same value
Thread A: ADD → 6
Thread B: ADD → 6
Thread A: WRITE counter = 6
Thread B: WRITE counter = 6  ← overwrites A's write!
// Result: 6 instead of 7 — one increment LOST

// FIX: AtomicInteger — uses CAS (Compare-And-Swap) CPU instruction
// CAS is a SINGLE atomic hardware instruction — cannot be interleaved
AtomicInteger counter = new AtomicInteger(0);
counter.incrementAndGet();    // Atomic — no race condition possible
```

---

**Q5. What is the difference between BLOCKED and WAITING state?**
```
BLOCKED:
Thread is waiting to ACQUIRE a synchronized lock
Another thread currently HOLDS that lock
Thread has NO control — scheduler moves it to RUNNABLE the moment lock is free
Caused by: trying to enter synchronized block/method

WAITING:
Thread VOLUNTARILY gave up control
Has the lock (entered synchronized), then called wait()
Or called join() or LockSupport.park()
Will not wake until explicitly notified: notify(), notifyAll(), join completes

Key difference:
BLOCKED  = "I want the lock, someone else has it"
WAITING  = "I have the lock but I'm voluntarily pausing, wake me when ready"

TIMED_WAITING = same as WAITING but with a timeout (sleep, wait(ms), join(ms))
```

---

## Section 1 Summary — What to Take Away 🧠
```
Core mental model:
Each CPU core has its own cache
Threads on different cores may see different values for same variable
JMM defines WHEN writes become visible: happens-before

5 happens-before guarantees:
1. Thread.start()      — writes before start are visible in new thread
2. Thread.join()       — writes in thread are visible after join() returns
3. synchronized        — unlock happens-before subsequent lock on same monitor
4. volatile            — write happens-before subsequent read of same variable
5. Static initializer  — happens-before first class use

volatile:
✅ Visibility: writes go to main memory, reads from main memory
✅ Ordering: prevents reordering across volatile access
❌ NOT atomic for compound ops (i++, check-then-act)

Thread states (6):
NEW → RUNNABLE → BLOCKED / WAITING / TIMED_WAITING → TERMINATED

Key methods:
sleep()    — pause, holds locks
wait()     — pause AND releases lock (must be in synchronized)
join()     — wait for thread to finish
interrupt()— request cooperative stop (sets a flag)
start()    — creates thread + calls run() [never call run() directly!]

Interview phrases:
"Without a happens-before relationship, this is a data race..."
"volatile fixes visibility but not atomicity — for compound ops use AtomicInteger..."
"The CPU is free to reorder these instructions..."
"BLOCKED means waiting for a lock, WAITING means voluntarily pausing..."

