Section 3: Deadlock, Livelock & Starvation 

Section 1 taught you WHY concurrency is hard. Section 2 taught you the tools to fix it. Section 3 is what happens when you USE those tools WRONG. Every FAANG interviewer will probe this — because deadlocks are silent, non-reproducible, and bring down production systems at 3am.

The Big Picture — Three Ways Concurrency Dies
DEADLOCK:    Threads frozen forever — each waiting for the other
CPU: 0%  (nobody doing anything)
Symptom: System hangs, no progress, no error

LIVELOCK:    Threads running but making zero progress
CPU: 100% (everyone busy, nobody advancing)
Symptom: System appears alive but nothing completes

STARVATION:  One thread never gets CPU/lock despite being ready
CPU: high (others are running, one is ignored)
Symptom: One operation times out, others succeed

Part 1: Deadlock — The Full Story
The Four Coffman Conditions
Deadlock is mathematically impossible unless ALL FOUR conditions hold simultaneously. This is the most important framework to know.
CONDITION 1: Mutual Exclusion
A resource is held by only one thread at a time.
Example: synchronized lock — only one thread can hold it.

CONDITION 2: Hold and Wait
A thread holds one resource while waiting for another.
Example: Thread holds lockA, waits for lockB.

CONDITION 3: No Preemption
Resources cannot be forcibly taken from a thread.
Thread must voluntarily release them.
Example: JVM cannot steal a lock from a thread.

CONDITION 4: Circular Wait
Thread A waits for Thread B,
Thread B waits for Thread A.
A cycle in the wait-for graph.

BREAK ANY ONE → Deadlock is impossible.
The Classic Bank Transfer Deadlock
java// This is THE canonical deadlock example — know it cold

class Account {
private final int id;
private double balance;

    Account(int id, double balance) {
        this.id      = id;
        this.balance = balance;
    }

    int    getId()      { return id; }
    double getBalance() { return balance; }
    void   debit(double amount)  { balance -= amount; }
    void   credit(double amount) { balance += amount; }
}

class DeadlockDemo {
void transfer(Account from, Account to, double amount) {
synchronized (from) {            // Thread A locks Account-1
System.out.println(Thread.currentThread().getName()
+ " locked " + from.getId());

            // ← Thread B is HERE: has Account-2, waiting for Account-1

            synchronized (to) {          // Thread A waits for Account-2
                // DEADLOCK:
                // Thread A holds Account-1, waits for Account-2
                // Thread B holds Account-2, waits for Account-1
                // Neither can proceed. Ever.
                from.debit(amount);
                to.credit(amount);
            }
        }
    }

    public static void main(String[] args) {
        Account account1 = new Account(1, 1000);
        Account account2 = new Account(2, 1000);

        // Thread A: transfer 1 → 2
        Thread threadA = new Thread(() ->
            new DeadlockDemo().transfer(account1, account2, 100), "Thread-A");

        // Thread B: transfer 2 → 1 (opposite direction!)
        Thread threadB = new Thread(() ->
            new DeadlockDemo().transfer(account2, account1, 100), "Thread-B");

        threadA.start();
        threadB.start();

        // System hangs here forever. No exception. No error.
        // This is what makes deadlock so dangerous.
    }
}
```
```
Visualizing the deadlock:

Thread A:  holds [Account-1] ──── waiting for ──── [Account-2]
│
held by
│
Thread B:  holds [Account-2] ──── waiting for ──── [Account-1]
│
held by
│
Thread A (full circle!)

This circular wait graph = DEADLOCK

Part 2: The Four Fixes for Deadlock
Fix 1 — Consistent Lock Ordering (Break Circular Wait)
java// The cleanest fix. If ALL threads always acquire locks in the SAME order,
// circular wait is mathematically impossible.

class SafeTransfer {
void transfer(Account from, Account to, double amount) {
// ALWAYS lock the lower-id account first
// Same order regardless of transfer direction
Account first  = from.getId() < to.getId() ? from : to;
Account second = from.getId() < to.getId() ? to   : from;

        synchronized (first) {
            synchronized (second) {
                from.debit(amount);
                to.credit(amount);
            }
        }
    }
}

// WHY this works:
// Transfer A→B: first=A(id=1), second=B(id=2)  — locks A then B
// Transfer B→A: first=A(id=1), second=B(id=2)  — locks A then B
//
// Both threads lock in ORDER: A first, B second
// Thread A: locks A → locks B → transfers → unlocks
// Thread B: tries to lock A → BLOCKED (Thread A has it)
//           Thread A finishes → Thread B locks A → locks B
// Sequential, safe, no circular wait!
//
// Key requirement: the ordering must be GLOBAL and CONSISTENT
// Using System.identityHashCode() when no natural ordering exists:

void transferByHashCode(Account from, Account to, double amount) {
int fromHash = System.identityHashCode(from);
int toHash   = System.identityHashCode(to);

    if (fromHash < toHash) {
        synchronized (from) {
            synchronized (to) {
                from.debit(amount); to.credit(amount);
            }
        }
    } else if (fromHash > toHash) {
        synchronized (to) {
            synchronized (from) {
                from.debit(amount); to.credit(amount);
            }
        }
    } else {
        // Hash collision (rare) — use a tiebreaker lock
        synchronized (Account.class) {     // Global tiebreaker
            synchronized (from) {
                synchronized (to) {
                    from.debit(amount); to.credit(amount);
                }
            }
        }
    }
}
Fix 2 — tryLock() with Timeout (Break Hold and Wait)
javaimport java.util.concurrent.locks.*;

class TryLockTransfer {
private final ReentrantLock lockA = new ReentrantLock();
private final ReentrantLock lockB = new ReentrantLock();

    void transfer(double amount) throws InterruptedException {
        while (true) {                          // Retry loop
            boolean gotA = false;
            boolean gotB = false;

            try {
                gotA = lockA.tryLock(50, TimeUnit.MILLISECONDS);
                gotB = lockB.tryLock(50, TimeUnit.MILLISECONDS);

                if (gotA && gotB) {
                    doTransfer(amount);         // Got both locks — safe!
                    return;                     // Done
                }
                // Didn't get both — fall through to finally, release, retry

            } finally {
                // ALWAYS release in finally — even if exception thrown
                if (gotB) lockB.unlock();
                if (gotA) lockA.unlock();
            }

            // Small random delay before retry — prevents livelock!
            Thread.sleep(10 + (long)(Math.random() * 20));
        }
    }
}

// WHY this prevents deadlock:
// Thread A: gets lockA, tries lockB, timeout after 50ms
//           RELEASES lockA (in finally)
//           Waits random delay, retries
// Thread B: gets lockB, tries lockA, timeout after 50ms
//           RELEASES lockB (in finally)
//           Waits random delay, retries
// Eventually one thread gets BOTH locks first — succeeds
//
// CRITICAL: The random delay is ESSENTIAL
// Without it: both threads retry at exactly the same time
// Both grab their first lock simultaneously → livelock (try forever)
// Random delay breaks the symmetry
Fix 3 — Lock Hierarchy (Formal Approach)
java// Assign a numeric level to every lock in the system
// Rule: a thread can only acquire a lock with HIGHER level than any it currently holds

class LockHierarchy {
// Assign levels at design time
static final int LEVEL_DB_CONNECTION = 1;    // Lowest level
static final int LEVEL_CACHE         = 2;
static final int LEVEL_USER_SESSION  = 3;
static final int LEVEL_TRANSACTION   = 4;    // Highest level

    // Thread must acquire lower-level locks first
    // If you need DB_CONNECTION + TRANSACTION:
    //   Acquire DB_CONNECTION (level 1) first
    //   Then TRANSACTION (level 4)
    // Can NEVER acquire a LOWER level lock after a HIGHER one
    // This rules out circular wait by construction

    // Enforcement via ThreadLocal (tracks current max held level):
    static final ThreadLocal<Integer> maxHeldLevel =
        ThreadLocal.withInitial(() -> 0);

    static void acquireWithCheck(Lock lock, int level) {
        int currentMax = maxHeldLevel.get();
        if (level <= currentMax) {
            throw new LockOrderViolationException(
                "Trying to acquire level " + level +
                " but already hold level " + currentMax +
                " — deadlock risk!");
        }
        lock.lock();
        maxHeldLevel.set(Math.max(currentMax, level));
    }
}
Fix 4 — Eliminate Lock Nesting (Best When Possible)
java// Best fix: restructure code so you never hold two locks simultaneously

// ❌ Requires two locks at once
void processAndTransfer(Account from, Account to) {
synchronized (from) {
synchronized (to) {           // Nested — deadlock risk!
transfer(from, to);
}
}
}

// ✅ Restructure: do operations sequentially with single locks
void safeProcessAndTransfer(Account from, Account to) {
double amount;
synchronized (from) {             // One lock at a time
amount = calculateAmount(from);
from.debit(amount);
}                                 // Release BEFORE acquiring next

    synchronized (to) {               // Only ONE lock at a time — no deadlock possible
        to.credit(amount);
    }
}
// If atomicity of both operations is required → different design needed
// (use database transactions, event sourcing, or a single orchestrating lock)

Part 3: Detecting Deadlocks at Runtime
java// The JVM can detect deadlocks involving synchronized and ReentrantLock!

import java.lang.management.*;

class DeadlockDetector {
private final ScheduledExecutorService scheduler =
Executors.newSingleThreadScheduledExecutor();

    void startMonitoring() {
        scheduler.scheduleAtFixedRate(this::checkForDeadlocks,
            0, 10, TimeUnit.SECONDS);
    }

    private void checkForDeadlocks() {
        ThreadMXBean bean = ManagementFactory.getThreadMXBean();

        // Find threads involved in deadlock
        long[] deadlockedIds = bean.findDeadlockedThreads();
        // findDeadlockedThreads(): finds deadlocks involving BOTH
        //   synchronized AND java.util.concurrent locks (ReentrantLock)
        //
        // findMonitorDeadlockedThreads(): finds ONLY synchronized deadlocks

        if (deadlockedIds != null) {
            ThreadInfo[] threadInfos = bean.getThreadInfo(
                deadlockedIds,
                true,    // include monitors (synchronized)
                true     // include synchronizers (ReentrantLock)
            );

            StringBuilder sb = new StringBuilder("🔴 DEADLOCK DETECTED!\n");
            for (ThreadInfo ti : threadInfos) {
                sb.append("\nThread: ").append(ti.getThreadName())
                  .append("\n  State: ").append(ti.getThreadState())
                  .append("\n  Waiting for: ").append(ti.getLockName())
                  .append("\n  Held by: ").append(ti.getLockOwnerName())
                  .append("\n  Stack trace:\n");

                for (StackTraceElement ste : ti.getStackTrace()) {
                    sb.append("    ").append(ste).append("\n");
                }
            }

            log.error(sb.toString());
            alertOpsTeam("DEADLOCK in production!", sb.toString());
        }
    }
}

// Also: kill -3 <pid> on Linux prints full thread dump including deadlock info
// Or: jstack <pid>  — dedicated JVM tool for thread dumps
// Or: VisualVM → Threads tab → Detect Deadlock button
```

---

## Part 4: Livelock — The Sneaky Cousin

### What Is Livelock?
```
DEADLOCK:  Threads are BLOCKED — doing nothing, waiting forever
LIVELOCK:  Threads are RUNNING — doing something, but making NO PROGRESS

Like two people in a narrow corridor:
Person A steps right to let B pass
Person B steps left to let A pass
Both now blocking each other again
Person A steps left...
Person B steps right...
They mirror each other forever — both moving, neither passing
Livelock in Code
java// Classic livelock: both threads "politely" back off at exactly the same time

class LivelockDemo {
private volatile boolean resourceFree = true;

    // Thread A and Thread B both run this logic simultaneously
    void tryToWork(String name) throws InterruptedException {
        while (true) {
            // Both check resource simultaneously
            if (!resourceFree) {
                System.out.println(name + ": resource busy, yielding...");
                Thread.yield();         // "You go first"
                continue;
            }

            // Both see it's free simultaneously
            resourceFree = false;       // Both set false "simultaneously"
            System.out.println(name + ": got resource! working...");
            doWork();
            resourceFree = true;        // Both release

            // But then both try again at the same time...
            // Infinitely polite, infinitely stuck
        }
    }
}
java// Real-world livelock: transaction retry loop

class TransactionLivelock {
// ❌ WRONG: Both transactions retry at exactly the same time
void processTransaction(Transaction txn) {
while (true) {
try {
txn.begin();
doWork(txn);
txn.commit();
return;                      // Success!
} catch (ConflictException e) {
txn.rollback();
Thread.sleep(FIXED_RETRY_DELAY_MS);  // FIXED delay — livelock!
// Both transactions sleep 100ms
// Both wake up simultaneously
// Both conflict again
// Both sleep 100ms again → infinite cycle
}
}
}
}
Fixing Livelock — Random Exponential Backoff
javaclass LivelockFix {
private static final Random random = new Random();

    // ✅ FIX: Random + exponential backoff breaks symmetry
    void processWithBackoff(Transaction txn) throws InterruptedException {
        long delayMs = 10;                   // Starting delay
        int  maxRetries = 10;

        for (int attempt = 0; attempt < maxRetries; attempt++) {
            try {
                txn.begin();
                doWork(txn);
                txn.commit();
                return;                      // Success!
            } catch (ConflictException e) {
                txn.rollback();

                if (attempt == maxRetries - 1) {
                    throw new RuntimeException("Max retries exceeded", e);
                }

                // RANDOM delay: different threads sleep different amounts
                // Breaks the synchronized retry that causes livelock
                long jitter = (long)(random.nextDouble() * delayMs);
                Thread.sleep(delayMs + jitter);

                // EXPONENTIAL backoff: each retry waits twice as long
                // Thread A might sleep: 10ms, 22ms, 47ms, 95ms...
                // Thread B might sleep: 15ms, 28ms, 63ms, 112ms...
                // They naturally desynchronize → one gets through
                delayMs = Math.min(delayMs * 2, 1000);  // Cap at 1 second
            }
        }
    }

    // This is EXACTLY what:
    // - TCP uses for collision avoidance (Ethernet CSMA/CD)
    // - AWS SDK uses for API retry logic
    // - Google's gRPC uses for reconnection
    // - Database drivers use for transaction retry
    // Industry-standard pattern. Know it cold.
}
```

---

## Part 5: Starvation — When Fairness Breaks Down

### What Is Starvation?
```
A thread is READY to run but NEVER gets CPU time or a lock
because other threads are always preferred over it.

It's alive. It wants to work. It just never gets to.

Causes:
1. Thread priority: high-priority threads dominate CPU scheduler
2. Unfair locks: barging — new arrivals skip the queue
3. Long critical sections: low-priority threads wait forever
4. Repeated lock acquisition: one thread re-acquires before others get a chance
   Starvation with Unfair Locks
   javaclass StarvationDemo {
   // Default ReentrantLock is UNFAIR — allows barging
   private final ReentrantLock unfairLock = new ReentrantLock(false); // unfair

    // Scenario:
    // Thread A: acquires lock, releases, immediately tries to reacquire
    // Thread B: waiting for lock since 5 seconds ago
    // Thread A may barge in AHEAD of Thread B every single time!
    // Thread B starves — waiting indefinitely despite being "next"

    void demonstrateStarvation() {
        // Thread A — greedy thread
        new Thread(() -> {
            while (true) {
                unfairLock.lock();
                try {
                    doQuickWork();  // Very fast — releases quickly
                } finally {
                    unfairLock.unlock();
                }
                // Immediately tries to reacquire
                // Gets it BEFORE Thread B (which has been waiting) because barging
            }
        }, "Greedy-Thread").start();

        // Thread B — starved thread
        new Thread(() -> {
            unfairLock.lock();   // Has been waiting... and waiting...
            try {
                System.out.println("Finally got the lock after: ???ms");
                // May take seconds or minutes to get in
            } finally {
                unfairLock.unlock();
            }
        }, "Starved-Thread").start();
    }
}
Fix 1 — Fair Lock
javaclass FairnessDemo {
// Fair lock: threads get lock in strict FIFO order — no barging
private final ReentrantLock fairLock = new ReentrantLock(true); // fair!

    void fairWork() {
        fairLock.lock();
        try {
            doWork();
        } finally {
            fairLock.unlock();
        }
        // Longest-waiting thread ALWAYS gets lock next
        // Thread B waited before Thread C → Thread B gets it first
        // No starvation — every thread eventually gets its turn
    }

    // ⚠️ COST of fairness:
    // OS must maintain a queue of waiting threads
    // Context switching to wake specific thread (vs any thread = faster)
    // Throughput drops ~30% compared to unfair lock
    //
    // Use fair lock WHEN:
    //   → Long-running tasks where one thread could starve for minutes
    //   → Equal priority threads that should share resources evenly
    //   → Preventing starvation is more important than peak throughput
    //
    // Use unfair lock (default) WHEN:
    //   → Short critical sections (milliseconds)
    //   → High throughput is the goal
    //   → Starvation is unlikely in practice (tasks finish quickly)
}
Fix 2 — Worker Thread Pool (Fairness by Design)
java// Instead of threads competing for locks — use a queue
// FIFO queue = natural fairness — first submitted, first processed

class FairTaskProcessor {
// LinkedBlockingQueue is FIFO — no starvation by design
private final BlockingQueue<Runnable> taskQueue =
new LinkedBlockingQueue<>(1000);

    private final ExecutorService workers =
        Executors.newFixedThreadPool(4);

    public void submit(Runnable task) throws InterruptedException {
        taskQueue.put(task);   // Added at end of queue
    }

    public void start() {
        for (int i = 0; i < 4; i++) {
            workers.submit(() -> {
                while (!Thread.currentThread().isInterrupted()) {
                    try {
                        Runnable task = taskQueue.take();  // FIFO order
                        task.run();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                }
            });
        }
    }
    // Every submitted task eventually runs — guaranteed by FIFO queue
    // No thread competes with another thread — they cooperate via queue
}
Fix 3 — PriorityQueue with Aging
java// Problem: what if some tasks ARE higher priority but low-priority tasks still need to run?
// Solution: Aging — priority increases the longer a task waits

class Task implements Comparable<Task> {
final String    name;
final int       originalPriority;
final long      submitTime;
volatile int    effectivePriority;

    Task(String name, int priority) {
        this.name              = name;
        this.originalPriority  = priority;
        this.effectivePriority = priority;
        this.submitTime        = System.currentTimeMillis();
    }

    // Higher effectivePriority = runs first
    @Override
    public int compareTo(Task other) {
        return Integer.compare(other.effectivePriority, this.effectivePriority);
    }
}

class AgingScheduler {
private final PriorityBlockingQueue<Task> queue =
new PriorityBlockingQueue<>();

    // Background thread ages waiting tasks
    private final ScheduledExecutorService ager =
        Executors.newSingleThreadScheduledExecutor();

    AgingScheduler() {
        // Every second: increase priority of waiting tasks
        ager.scheduleAtFixedRate(() -> {
            for (Task task : queue) {
                long waitMs = System.currentTimeMillis() - task.submitTime;

                // Increase priority by 1 for every second waited
                task.effectivePriority = task.originalPriority
                    + (int)(waitMs / 1000);

                // After 60 seconds of waiting: even low-priority task
                // has priority increased by 60 — eventually beats anyone
            }
        }, 1, 1, TimeUnit.SECONDS);
    }

    // A task with priority 1 that waits 60 seconds
    // becomes effectively priority 61 — beats a fresh priority 50 task
    // STARVATION IMPOSSIBLE: every task eventually rises to the top
}

Part 6: Putting It All Together — Real Scenario
java// FAANG interview scenario:
// "Design a thread-safe transfer service that handles concurrent transfers
// between multiple accounts without deadlock or starvation."

class ProductionTransferService {
private final ConcurrentHashMap<Integer, ReentrantLock> accountLocks =
new ConcurrentHashMap<>();

    private final ConcurrentHashMap<Integer, Double> balances =
        new ConcurrentHashMap<>();

    // Get or create a lock for each account
    private ReentrantLock getLock(int accountId) {
        return accountLocks.computeIfAbsent(
            accountId,
            id -> new ReentrantLock(true)  // fair=true: no starvation
        );
    }

    // Thread-safe transfer: handles deadlock, starvation, concurrent access
    public boolean transfer(int fromId, int toId, double amount)
            throws InterruptedException {

        // Fix 1: Consistent ordering — always lock lower ID first
        int firstId  = Math.min(fromId, toId);
        int secondId = Math.max(fromId, toId);

        ReentrantLock firstLock  = getLock(firstId);
        ReentrantLock secondLock = getLock(secondId);

        // Fix 2: tryLock with timeout — no indefinite blocking
        int  maxAttempts = 3;
        long delayMs     = 10;

        for (int attempt = 0; attempt < maxAttempts; attempt++) {
            boolean gotFirst  = false;
            boolean gotSecond = false;

            try {
                gotFirst  = firstLock.tryLock(100, TimeUnit.MILLISECONDS);
                gotSecond = gotFirst &&
                            secondLock.tryLock(100, TimeUnit.MILLISECONDS);

                if (gotFirst && gotSecond) {
                    // Got both locks — perform transfer safely
                    double fromBalance = balances.getOrDefault(fromId, 0.0);

                    if (fromBalance < amount) {
                        return false;   // Insufficient funds
                    }

                    balances.put(fromId, fromBalance - amount);
                    balances.merge(toId, amount, Double::sum);

                    log.info("Transferred {} from {} to {}", amount, fromId, toId);
                    return true;
                }

            } finally {
                // Fix 3: ALWAYS release in finally — no lock leaks
                if (gotSecond) secondLock.unlock();
                if (gotFirst)  firstLock.unlock();
            }

            // Fix 4: Exponential backoff with jitter — prevent livelock
            if (attempt < maxAttempts - 1) {
                long jitter = (long)(Math.random() * delayMs);
                Thread.sleep(delayMs + jitter);
                delayMs *= 2;    // 10ms → 20ms → 40ms
            }
        }

        log.warn("Transfer from {} to {} failed after {} attempts", fromId, toId, maxAttempts);
        return false;
    }
}

Part 7: The Interview Q&A Round 🎤

Q1. What are the four conditions for deadlock? How do you prevent it?

"Deadlock requires all four Coffman conditions simultaneously: mutual exclusion (only one thread holds a resource), hold and wait (thread holds one resource while waiting for another), no preemption (resources can't be forcibly taken), and circular wait (Thread A waits for B, B waits for A — a cycle).
To prevent deadlock, break any one condition. In practice, the easiest to break is circular wait — impose a consistent global lock ordering. If all threads always acquire lock A before lock B, circular wait is impossible. You can also break hold-and-wait using tryLock() with timeout — if you can't get both locks, release everything and retry with exponential backoff. That retry delay must be random, otherwise both threads retry simultaneously and you get livelock instead."


Q2. What is the difference between deadlock and livelock?

"In deadlock, threads are BLOCKED — permanently suspended waiting for each other. CPU usage drops to zero because no thread is executing. The system appears completely frozen.
In livelock, threads are actively RUNNING but making no progress — they're responding to each other in a way that perpetually undoes their own work. CPU usage is high. Classically, like two people stepping aside for each other in a corridor — both are moving, neither gets through.
Deadlock is detected by thread dump analysis or ThreadMXBean.findDeadlockedThreads(). Livelock is harder to detect — the system looks alive but nothing completes. You find it through metrics: high CPU with zero throughput, or log lines showing infinite retries.
The fix for livelock is always random exponential backoff — break the perfect symmetry that causes both threads to conflict repeatedly."


Q3. How would you find a deadlock in a production JVM?
java// Method 1: ThreadMXBean (programmatic — can run inside the app)
ThreadMXBean bean = ManagementFactory.getThreadMXBean();
long[] deadlocked = bean.findDeadlockedThreads();  // Returns null if none
if (deadlocked != null) {
ThreadInfo[] info = bean.getThreadInfo(deadlocked, true, true);
for (ThreadInfo ti : info) {
System.err.println("Deadlocked: " + ti.getThreadName());
System.err.println("Waiting for: " + ti.getLockName());
System.err.println("Held by: " + ti.getLockOwnerName());
for (StackTraceElement ste : ti.getStackTrace())
System.err.println("  at " + ste);
}
}

// Method 2: jstack — command line tool
// jstack <pid>
// Look for: "Found one Java-level deadlock:"
// Shows: which threads, which locks, full stack traces

// Method 3: kill -3 <pid> (Linux)
// Sends SIGQUIT → JVM prints thread dump to stdout
// Same info as jstack, no separate tool needed

// Method 4: VisualVM (GUI)
// Connect to running JVM → Threads tab → "Detect Deadlock" button
// Visual graph showing the circular dependency

// Method 5: Read the log pattern
// "Thread-A: locked Account-1, waiting for Account-2"
// "Thread-B: locked Account-2, waiting for Account-1"
// This pattern in logs = deadlock

Q4. Can you have a deadlock with just one lock?
java// YES — if the lock is NOT reentrant

// With a non-reentrant lock (hypothetical):
class NonReentrantLock {
// If Java's synchronized were not reentrant...

    synchronized void outer() {
        inner();  // Tries to acquire the same lock!
    }

    synchronized void inner() {
        // outer() holds the lock
        // inner() tries to acquire the same lock
        // Thread blocks waiting for itself → SELF-DEADLOCK
    }
}
// Java's synchronized IS reentrant — this works fine
// But: if you use a non-reentrant locking mechanism, this IS a deadlock

// Also: database-level deadlock with one table
// Transaction 1: locks row A, tries to lock row B
// Transaction 2: locks row B, tries to lock row A
// → Deadlock even though it's "one table"
// This is why databases have deadlock detection and transaction rollback


// Also: indirect deadlock through callbacks
class EventBus {
private final Object lock = new Object();

    synchronized void publish(Event e) {
        for (Listener l : listeners) {
            l.onEvent(e);    // Alien call WHILE holding lock!
        }
    }

    synchronized void subscribe(Listener l) {
        listeners.add(l);
    }
}

class MyListener implements Listener {
void onEvent(Event e) {
eventBus.subscribe(newListener);  // Tries to acquire 'lock' in subscribe()!
// eventBus.publish() holds 'lock' and called us
// subscribe() needs 'lock'
// DEADLOCK — with just one lock object!
}
}
// Fix: copy listeners before calling (as shown in Section 2 alien call fix)

Q5. Design a dining philosophers solution (classic interview puzzle)
java// 5 philosophers sit at a round table
// Each needs TWO forks to eat
// Each fork is shared between two adjacent philosophers
// Naive implementation → deadlock

// PROBLEM: all philosophers pick up LEFT fork simultaneously
// All then try to pick up RIGHT fork → all waiting → DEADLOCK

class DiningPhilosophers {
private final ReentrantLock[] forks = new ReentrantLock[5];

    DiningPhilosophers() {
        for (int i = 0; i < 5; i++)
            forks[i] = new ReentrantLock();
    }

    // ❌ DEADLOCK: All pick up left fork, all wait for right
    void naiveEat(int philosopherId) throws InterruptedException {
        int leftFork  = philosopherId;
        int rightFork = (philosopherId + 1) % 5;

        forks[leftFork].lock();    // All get their left fork
        forks[rightFork].lock();   // All wait → DEADLOCK
        eat();
        forks[rightFork].unlock();
        forks[leftFork].unlock();
    }

    // ✅ FIX 1: Lock ordering — one philosopher picks RIGHT first
    void orderedEat(int id) throws InterruptedException {
        int leftFork  = id;
        int rightFork = (id + 1) % 5;

        // Philosopher 4 picks in OPPOSITE order — breaks circular wait
        int firstFork  = id == 4 ? rightFork : leftFork;
        int secondFork = id == 4 ? leftFork  : rightFork;

        forks[firstFork].lock();
        try {
            forks[secondFork].lock();
            try {
                eat();
            } finally { forks[secondFork].unlock(); }
        } finally { forks[firstFork].unlock(); }
    }

    // ✅ FIX 2: tryLock with backoff — always safe
    void tryLockEat(int id) throws InterruptedException {
        int leftFork  = id;
        int rightFork = (id + 1) % 5;

        while (true) {
            if (forks[leftFork].tryLock()) {
                try {
                    if (forks[rightFork].tryLock()) {
                        try {
                            eat();
                            return;
                        } finally { forks[rightFork].unlock(); }
                    }
                } finally { forks[leftFork].unlock(); }
            }
            // Back off: random sleep breaks symmetry → prevents livelock
            Thread.sleep(10 + (long)(Math.random() * 20));
        }
    }

    // ✅ FIX 3: Semaphore to limit to 4 concurrent eaters
    // At most 4 of 5 can try simultaneously → at least one ALWAYS eats
    private final Semaphore maxEaters = new Semaphore(4);

    void semaphoreEat(int id) throws InterruptedException {
        int leftFork  = id;
        int rightFork = (id + 1) % 5;

        maxEaters.acquire();        // Max 4 can attempt simultaneously
        try {
            forks[leftFork].lock();
            try {
                forks[rightFork].lock();
                try {
                    eat();
                } finally { forks[rightFork].unlock(); }
            } finally { forks[leftFork].unlock(); }
        } finally {
            maxEaters.release();
        }
        // With max 4 eaters and 5 forks:
        // At least one eater always gets both forks → no deadlock
    }

    private void eat() throws InterruptedException {
        System.out.println(Thread.currentThread().getName() + " eating...");
        Thread.sleep(100);
    }
}

Q6. What is priority inversion and why does it matter?
java// Priority inversion: a HIGH priority thread is effectively blocked by a LOW priority thread
// Not deadlock, not starvation — but a subtle form of the same problem

// Scenario:
// Thread L (low  priority): holds a lock
// Thread M (medium priority): running, no lock needed
// Thread H (high  priority): needs the lock L holds

// What happens:
// Thread H is highest priority — scheduler gives it CPU
// Thread H waits for lock held by Thread L
// Thread L can't run because Thread M (medium) keeps getting CPU
// Thread H is blocked waiting for Thread L
// Thread L is blocked waiting for CPU (preempted by Thread M)
// Result: MEDIUM priority thread effectively blocks HIGH priority thread!

// Real example: Mars Pathfinder mission (1997)
// Low-priority task: meteorological data gathering (held shared bus lock)
// Medium-priority tasks: many background tasks kept running
// High-priority task: system watchdog (needed bus lock, kept timing out)
// Watchdog triggered system resets — Mars rover kept rebooting!
// Fix: applied priority ceiling protocol via software patch from Earth

// Java Fix: Priority Inheritance (java.util.concurrent handles this better)
// Or: don't rely on thread priorities for correctness
// Or: use timed locks to detect and recover

class PriorityInversionFix {
private final ReentrantLock lock = new ReentrantLock();

    // High priority thread with timeout
    void highPriorityTask() throws InterruptedException {
        if (!lock.tryLock(500, TimeUnit.MILLISECONDS)) {
            // Low priority thread has held it too long
            log.warn("Priority inversion suspected — lock held > 500ms");
            escalateLowPriorityHolder();  // Temporarily boost priority of lock holder
        }
        try {
            doCriticalWork();
        } finally {
            lock.unlock();
        }
    }
}
```

---

## Section 3 Master Summary 🧠
```
DEADLOCK:
Requires ALL 4 Coffman conditions simultaneously:
1. Mutual Exclusion    → resource held by one thread
2. Hold and Wait       → hold one, wait for another
3. No Preemption       → can't forcibly take resources
4. Circular Wait       → A waits B, B waits A (cycle)

Break any one → deadlock impossible:
Break Circular Wait  → consistent lock ordering (always lock A before B)
Break Hold and Wait  → tryLock() with timeout + release all + retry
Break No Preemption  → not possible in Java without redesign
Break Mutual Exclusion→ use lock-free data structures

Detect at runtime:
→ ThreadMXBean.findDeadlockedThreads()
→ jstack <pid> or kill -3 <pid>
→ VisualVM Detect Deadlock

LIVELOCK:
Threads RUNNING, zero PROGRESS
Cause: symmetric retry — both threads retry at same time
Fix: random exponential backoff
delay = baseDelay + random(0, baseDelay)
baseDelay *= 2 each retry (cap at ~1 second)
Industry use: TCP, AWS SDK, gRPC, database drivers

STARVATION:
Thread ready but never runs
Causes: unfair locks, priority imbalance, long critical sections
Fixes:
→ Fair ReentrantLock(true)     — FIFO ordering
→ Priority aging               — priority grows with wait time
→ Work queue (FIFO)            — natural fairness by design
→ Minimize critical sections   — reduce lock hold time

KEY DECISION TREE:
System hangs, 0% CPU  → DEADLOCK
→ jstack, look for circular lock dependency
→ Fix: lock ordering or tryLock()

System churns, 100% CPU, no output → LIVELOCK
→ Look for infinite retry loops in logs
→ Fix: add random jitter to retry delay

One operation always slow/timeout, others fine → STARVATION
→ Look for one thread never getting scheduled
→ Fix: fair lock or priority aging

Interview phrases:
"Break circular wait by imposing global lock ordering..."
"tryLock() with timeout prevents indefinite blocking..."
"Random backoff breaks the symmetry that causes livelock..."
"Fair lock guarantees FIFO — no thread waits forever..."
"All four Coffman conditions must hold simultaneously for deadlock..."