Section 6: Thread Pools & ExecutorService
```
This is the section that separates junior devs from seniors at FAANG. 
Everyone knows new Thread(). Seniors know WHY it's wrong, HOW thread pools work internally, 
and HOW to size, monitor, and shut them down correctly.
```

Why Thread Pools Exist — The Real Cost of new Thread()
```
Every time you call 'new Thread()':
→ OS allocates a new kernel thread          (~1ms overhead)
→ JVM allocates a stack                     (~512KB – 1MB memory)
→ Scheduler registers the new thread
→ Thread runs task, then is DESTROYED       (~1ms cleanup)
→ GC eventually collects the Thread object

For 1,000 requests/second:
→ 1,000 threads created and destroyed per second
→ 500MB-1GB stack memory churning constantly
→ Scheduler overwhelmed managing thousands of threads
→ Context switching becomes more work than actual computation

"In production code, I never write new Thread() directly. Creating and destroying threads for every task is like hiring and firing a contractor for every 5-minute job — the hiring overhead exceeds the work. Thread pools keep workers alive and assign them jobs — dramatically lower overhead."

```

The Architecture of a Thread Pool
```
┌─────────────────────────────────┐
Task submitted    │ THREAD POOL   │
─────────────────▶│               │
│  ┌──────────┐  ┌──────────────┐ │
│  │  QUEUE   │  │   THREADS    │ │
│  │          │  │              │ │
│  │ Task 1   │  │ Worker-1 ██  │ │
│  │ Task 2   │  │ Worker-2 ██  │ │
│  │ Task 3   │  │ Worker-3 ██  │ │
│  │ Task 4   │  │ Worker-4 ──  │ │
│  │ ...      │  │ (idle)       │ │
│  └──────────┘  └──────────────┘ │
│                                 │
│  Workers pick tasks from queue  │
│  Workers are REUSED — not killed│
└─────────────────────────────────┘
```
ThreadPoolExecutor — The Core Engine
```
Every Executors.newXxx() factory method is just a convenience wrapper around ThreadPoolExecutor. 
Knowing this constructor cold is a FAANG differentiator.

javaThreadPoolExecutor pool = new ThreadPoolExecutor(
    int    corePoolSize,          // Always-alive threads
    int    maximumPoolSize,       // Max threads under load
    long   keepAliveTime,         // How long idle EXTRA threads live
    TimeUnit unit,                // keepAliveTime unit
    BlockingQueue<Runnable> queue,// Where tasks wait
    ThreadFactory threadFactory,  // How threads are created
    RejectedExecutionHandler handler // What to do when overwhelmed
);

```

### How Task Submission Works — The Decision Tree

```
New task arrives via execute() or submit():

Is activeThreads < corePoolSize?
    YES → Create a new thread. Run task on it.
    NO  ↓
Is the work queue full?
    NO  → Add task to queue. An idle worker will pick it up.
    YES ↓
Is activeThreads < maximumPoolSize?
    YES → Create a new (non-core) thread. Run task on it.
    NO  ↓
→ RejectedExecutionHandler fires!
```
Key insight: 

    Extra threads (above core) are created ONLY when
    the queue is full — not when tasks are simply waiting.
    This surprises many candidates.

java// Full production-grade ThreadPoolExecutor
```
int cores = Runtime.getRuntime().availableProcessors();

ThreadPoolExecutor pool = new ThreadPoolExecutor(
    cores,                              // Core threads — always alive
    cores * 2,                          // Max threads — created when queue fills
    60L, TimeUnit.SECONDS,              // Idle non-core threads die after 60s
    new ArrayBlockingQueue<>(500),      // BOUNDED queue — critical for safety
    new ThreadFactory() {               // Named threads — invaluable for debugging
    private final AtomicInteger count = new AtomicInteger(0);
    @Override
    public Thread newThread(Runnable r) {
        Thread t = new Thread(r, "order-processor-" + count.incrementAndGet());
        t.setDaemon(false);         // Non-daemon: finish work before JVM exits
        t.setUncaughtExceptionHandler((thread, ex) -> {
            log.error("Thread {} died unexpectedly", thread.getName(), ex);
            alertOpsTeam(ex);
        });
        return t;
    }
},
    new ThreadPoolExecutor.CallerRunsPolicy() // Rejected tasks run on caller thread
    // = natural backpressure — slows down submitter instead of dropping work
);

    // Allow core threads to time out too (useful for bursty workloads)
    pool.allowCoreThreadTimeOut(true);
```
Queue Types — The Most Important Decision

java// ─── Unbounded Queue (DANGER for production) ─────────────────────
```
new LinkedBlockingQueue<>()          // No size limit — memory bomb!
// If tasks arrive faster than processed:
// Queue grows → heap fills → OutOfMemoryError → JVM crash
// Used by: Executors.newFixedThreadPool() ← this is why it's risky!
```
// ─── Bounded Queue (PRODUCTION SAFE) ──────────────────────────────
```
new ArrayBlockingQueue<>(500)         // Max 500 queued tasks
new LinkedBlockingQueue<>(500)        // Linked list, max 500
```
// ─── SynchronousQueue (Zero-buffer handoff) ──────────────────────
```
new SynchronousQueue<>()
// Capacity = ZERO — each submitted task must directly handoff to a thread
// If no idle thread AND at maxPoolSize → immediate rejection
// Used by: Executors.newCachedThreadPool()
// Effect: always creates new threads instead of queuing (good for short tasks)
```
// ─── PriorityBlockingQueue (Ordered) ─────────────────────────────
```
new PriorityBlockingQueue<>(500, taskPriorityComparator)
// Tasks processed by priority, not FIFO
// Use for: critical tasks jumping ahead of normal tasks
```
The 4 Factory Methods — When to Actually Use Each

java// ─── 1. newFixedThreadPool(n) ─────────────────────────────────────
```
ExecutorService fixed = Executors.newFixedThreadPool(4);
// Internally: ThreadPoolExecutor(4, 4, 0, MILLISECONDS, new LinkedBlockingQueue())
// ✅ Good for: CPU-bound tasks — predictable concurrency
// ❌ RISK: Unbounded LinkedBlockingQueue → OOM under sustained overload
// 🔧 When OK: internal background tasks with known bounded input
```
// ─── 2. newCachedThreadPool() ─────────────────────────────────────
```
ExecutorService cached = Executors.newCachedThreadPool();
// Internally: ThreadPoolExecutor(0, MAX_VALUE, 60s, SECONDS, new SynchronousQueue())
// ✅ Good for: many SHORT-LIVED async tasks (< 1 second each)
// ❌ RISK: max = Integer.MAX_VALUE → 2 billion threads → OOM
// ❌ Avoid for: long-running tasks or sustained high load
```
// ─── 3. newSingleThreadExecutor() ────────────────────────────────
```
ExecutorService single = Executors.newSingleThreadExecutor();
// Internally: Executors.finalizableDelegatedExecutorService(
//             new ThreadPoolExecutor(1, 1, 0, MILLISECONDS, new LinkedBlockingQueue()))
// ✅ Good for: tasks that MUST run sequentially in order (FIFO guarantee)
// ✅ Good for: single-threaded event loops, ordered log writing
// ❌ RISK: same OOM risk as fixed (unbounded queue)
```
// ─── 4. newScheduledThreadPool(n) ─────────────────────────────────
```
ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);

// One-time delay
    scheduler.schedule(() -> sendReminder(user), 24, TimeUnit.HOURS);

// Fixed RATE — gap between START times (regardless of execution duration)
    scheduler.scheduleAtFixedRate(() -> {
    collectMetrics();           // If this takes 3s and rate is 5s → 2s gap
    }, 0, 5, TimeUnit.SECONDS);    // Start → 5s → Start → 5s → ...

// Fixed DELAY — gap between END of one and START of next
    scheduler.scheduleWithFixedDelay(() -> {
    pollDatabase();             // If this takes 3s and delay is 5s → 8s total
    }, 0, 5, TimeUnit.SECONDS);    // End → 5s → Start → End → 5s → ...
// ⚠️ If one execution throws → STOPS the whole schedule! Always catch exceptions.

// Safe scheduled task
scheduler.scheduleAtFixedRate(() -> {
    try {
        heartbeat();
    } catch (Exception e) {
        log.error("Heartbeat failed", e);  // Log but DON'T rethrow — keeps schedule alive
    }
}, 0, 30, TimeUnit.SECONDS);
```
The 4 Rejection Policies — Critical to Know

java// What happens when queue is full AND at max threads?

// ─── 1. AbortPolicy (DEFAULT) ─────────────────────────────────────
```
new ThreadPoolExecutor.AbortPolicy()
// throws RejectedExecutionException immediately
// Caller must handle this exception or task is LOST
// Use when: you need to know about overload immediately
```
// ─── 2. CallerRunsPolicy ─────────────────────────────────────────
```
new ThreadPoolExecutor.CallerRunsPolicy()
// Rejected task runs on the SUBMITTING thread itself
// Natural backpressure — submitter slows down when pool is overwhelmed
// ✅ Best for most cases — no data loss, self-regulating
// Example: HTTP request handler slows down when downstream is overwhelmed
```
// ─── 3. DiscardPolicy ────────────────────────────────────────────
```
new ThreadPoolExecutor.DiscardPolicy()
// Silently drops the rejected task — no exception!
// ⚠️ DANGEROUS: silent data loss
// Use ONLY for: truly optional work (metrics collection, non-critical logs)
```
// ─── 4. DiscardOldestPolicy ──────────────────────────────────────
```
new ThreadPoolExecutor.DiscardOldestPolicy()
// Drops the OLDEST waiting task, retries submitting the new task
// ⚠️ Older tasks discarded — may lose important work
// Use for: sensor readings where only the LATEST value matters
```
// ─── 5. Custom Rejection Handler ─────────────────────────────────
```
class MeteredRejectionHandler implements RejectedExecutionHandler {
    private final AtomicLong rejectedCount = new AtomicLong(0);
    private final BlockingQueue<Runnable> overflow;  // Secondary buffer

    @Override
    public void rejectedExecution(Runnable task, ThreadPoolExecutor executor) {
        long count = rejectedCount.incrementAndGet();
        metrics.increment("pool.rejections");           // Track it!

        if (!overflow.offer(task)) {                    // Try secondary buffer
            log.error("Task {} rejected! Total rejections: {}", task, count);
            // Could: save to DB, send to dead-letter queue, alert ops
        }
    }
}
```
## execute() vs submit() — Full Breakdown

    javaExecutorService pool = Executors.newFixedThreadPool(4);

// ─── execute() — Fire and forget ──────────────────────────────────
```
pool.execute(() -> sendEmail(user));
// Runnable ONLY (no return value)
// Uncaught exceptions → printed to stderr (and UncaughtExceptionHandler)
// No way to know if task succeeded or failed from caller
```
// ─── submit() — Track completion ──────────────────────────────────
```
// With Runnable (no result)
Future<?> f1 = pool.submit(() -> sendEmail(user));
f1.get();      // Blocks until done — throws ExecutionException if task threw
```
// With Callable (has result)
```
Future<User> f2 = pool.submit(() -> findUser("u123"));
```
// ── Future methods ────────────────────────────────────────────────
```
User user = f2.get();                           // Blocks indefinitely
User user2 = f2.get(5, TimeUnit.SECONDS);       // Blocks max 5s → TimeoutException
boolean done = f2.isDone();                     // Non-blocking check
boolean cancelled = f2.isCancelled();
f2.cancel(true);                                // Interrupt if running (true)
// or just skip if not started (false)

// ⚠️ IMPORTANT: ExecutionException wraps the original exception
try {
User u = f2.get();
} catch (ExecutionException e) {
Throwable cause = e.getCause();             // The REAL exception
if (cause instanceof UserNotFoundException) {
handleNotFound();
} else {
throw new RuntimeException("Unexpected", cause);
}
} catch (InterruptedException e) {
Thread.currentThread().interrupt();          // Restore interrupt flag!
throw new RuntimeException("Interrupted waiting for user", e);
}

invokeAll() vs invokeAny() — The Power Moves
javaList<Callable<String>> tasks = List.of(
() -> fetchFromDB("user:1"),
() -> fetchFromCache("user:2"),
() -> fetchFromAPI("user:3")
);
```
// ─── invokeAll() — Submit all, wait for ALL to finish ─────────────
```
List<Future<String>> results = pool.invokeAll(tasks);
// BLOCKS until every single task finishes (or throws)
// Returns List<Future> in SAME ORDER as input list — regardless of completion order
for (Future<String> r : results) {
try {
System.out.println(r.get());  // No blocking here — all already done
} catch (ExecutionException e) {
System.err.println("Task failed: " + e.getCause().getMessage());
// Other tasks' results are still accessible!
}
}
```

    // With timeout — cancel tasks that take too long
    List<Future<String>> timedResults = pool.invokeAll(tasks, 3, TimeUnit.SECONDS);
    // Any task not done in 3s is CANCELLED
    // Check future.isCancelled() before future.get()
```
// ─── invokeAny() — Submit all, return FIRST result, cancel rest ───

String fastest = pool.invokeAny(tasks);
// Returns value of FIRST task to complete successfully
// ALL OTHER tasks are CANCELLED immediately
// If ALL tasks fail → ExecutionException with last failure

// REAL USE CASE: Multi-region database reads
// Try primary DB, replica 1, replica 2 simultaneously
// Use whichever responds first — ultra-low latency reads
String userData = pool.invokeAny(List.of(
() -> primaryDB.find("u1"),     // Usually wins
() -> replica1.find("u1"),      // Fallback
() -> replica2.find("u1")       // Last resort
));
// If primary takes 500ms, replica1 takes 50ms → use replica1 result
// Primary and replica2 queries CANCELLED immediately

Thread Pool Sizing — The Formula FAANG Loves
javaint N = Runtime.getRuntime().availableProcessors();

// ─── CPU-BOUND TASKS ──────────────────────────────────────────────
// Tasks: heavy computation, sorting, encryption, image processing
// Threads compete for CPU — more threads = more context switching
// Optimal: N cores (+ 1 as spare for the occasional blocking call)

ExecutorService cpuPool = new ThreadPoolExecutor(
N,                              // Core: one per CPU
N + 1,                          // Max: one extra spare
60, TimeUnit.SECONDS,
new ArrayBlockingQueue<>(200),
new ThreadPoolExecutor.CallerRunsPolicy()
);

// ─── I/O-BOUND TASKS ──────────────────────────────────────────────
// Tasks: DB queries, HTTP calls, file reads
// Thread spends 90%+ time WAITING — CPU is idle during wait
// More threads → better CPU utilization during waits

// Formula: threads = N × (1 + wait_time / compute_time)
// Example: 95ms wait, 5ms compute → ratio = 19
// threads = N × (1 + 19) = N × 20

double waitTimeMs   = 95.0;  // avg time waiting for DB response
double computeMs    = 5.0;   // avg time processing result
double ratio        = waitTimeMs / computeMs;  // 19.0
int ioThreads       = (int) (N * (1 + ratio)); // N × 20

ExecutorService ioPool = new ThreadPoolExecutor(
ioThreads,
ioThreads,
60, TimeUnit.SECONDS,
new ArrayBlockingQueue<>(2000),  // Larger queue for I/O tasks
new ThreadPoolExecutor.CallerRunsPolicy()
);

// ─── MIXED WORKLOADS ──────────────────────────────────────────────
// Profile first! Start with N × 2, measure, tune.
// Watch these signals:
// Queue growing?           → Add more threads
// CPU near 100%?           → Don't add more threads
// Many TimeoutExceptions?  → Either add threads or investigate slow tasks
// Memory growing?          → Queue too large, reduce or add more threads

// ─── Monitor your pool ────────────────────────────────────────────
ThreadPoolExecutor tpe = (ThreadPoolExecutor) cpuPool;
ScheduledExecutorService monitor = Executors.newSingleThreadScheduledExecutor();
monitor.scheduleAtFixedRate(() -> {
log.info("Pool stats: active={}, queued={}, completed={}, poolSize={}, largest={}",
tpe.getActiveCount(),          // Currently running
tpe.getQueue().size(),         // Waiting in queue
tpe.getCompletedTaskCount(),   // Total finished
tpe.getPoolSize(),             // Current live threads
tpe.getLargestPoolSize()       // Historical peak
);
if (tpe.getQueue().size() > 400) {
log.warn("Queue filling up! Consider increasing pool size or reducing load");
metrics.gauge("pool.queue.depth", tpe.getQueue().size());
}
}, 0, 10, TimeUnit.SECONDS);

Proper Shutdown — Most Candidates Get This Wrong
javaExecutorService pool = Executors.newFixedThreadPool(4);

// ─── Graceful shutdown ────────────────────────────────────────────
pool.shutdown();
// - No new tasks accepted (submit() throws RejectedExecutionException)
// - Currently running tasks finish
// - Queued tasks finish
// - Returns IMMEDIATELY (doesn't wait!)

// Wait for completion with timeout
try {
if (!pool.awaitTermination(60, TimeUnit.SECONDS)) {
// Tasks still running after 60s — force shutdown
log.warn("Pool didn't terminate in time, forcing shutdown");
List<Runnable> neverStarted = pool.shutdownNow();
// shutdownNow() → interrupts running threads, returns QUEUED tasks
log.warn("{} tasks were never executed", neverStarted.size());

        // Give interrupted tasks a moment to clean up
        if (!pool.awaitTermination(10, TimeUnit.SECONDS)) {
            log.error("Pool did not terminate even after force shutdown!");
        }
    }
} catch (InterruptedException e) {
// Current thread was interrupted while waiting
pool.shutdownNow();
Thread.currentThread().interrupt();  // Restore interrupt status
}

// ─── JVM Shutdown Hook — ensure pool shuts down on app exit ───────
Runtime.getRuntime().addShutdownHook(new Thread(() -> {
log.info("JVM shutting down, draining thread pool...");
pool.shutdown();
try {
pool.awaitTermination(30, TimeUnit.SECONDS);
} catch (InterruptedException e) {
Thread.currentThread().interrupt();
}
log.info("Thread pool shut down cleanly");
}));

// ─── isShutdown() vs isTerminated() ──────────────────────────────
pool.isShutdown();     // true after shutdown() called — no new tasks
pool.isTerminated();   // true when ALL tasks done AND shutdown called
// Pool can be shutdown but NOT terminated (tasks still running)

Advanced: Custom ThreadFactory — Why You Need It
java// Default thread names: "pool-1-thread-1", "pool-1-thread-2"
// When you have 10 pools in production, thread dumps are UNREADABLE

// Custom ThreadFactory gives meaningful names + configuration
class ServiceThreadFactory implements ThreadFactory {
private final String serviceName;
private final AtomicInteger count = new AtomicInteger(0);
private final boolean daemon;
private final int priority;

    public ServiceThreadFactory(String serviceName, boolean daemon, int priority) {
        this.serviceName = serviceName;
        this.daemon      = daemon;
        this.priority    = priority;
    }

    @Override
    public Thread newThread(Runnable r) {
        Thread t = new Thread(r);

        // Meaningful name — visible in thread dumps, profilers, logs
        t.setName(serviceName + "-worker-" + count.incrementAndGet());

        // Daemon: if true, JVM exits without waiting for this thread
        t.setDaemon(daemon);

        // Priority: Thread.MIN_PRIORITY(1) to MAX_PRIORITY(10), NORM is 5
        t.setPriority(priority);

        // Catch and log unexpected crashes
        t.setUncaughtExceptionHandler((thread, ex) -> {
            log.error("Unexpected exception in {}", thread.getName(), ex);
            Metrics.counter("thread.crash", "pool", serviceName).increment();
        });

        return t;
    }
}
```
// Usage — before vs after
```
ExecutorService plain   = Executors.newFixedThreadPool(4);
// Thread dump: "pool-1-thread-1", "pool-1-thread-2"... uninformative!

ExecutorService named   = new ThreadPoolExecutor(4, 4, 0L, TimeUnit.MILLISECONDS,
new LinkedBlockingQueue<>(),
new ServiceThreadFactory("payment-processor", false, Thread.NORM_PRIORITY));
// Thread dump: "payment-processor-worker-1", "payment-processor-worker-2"
// Instantly tells you: which service owns this thread, what it does

CompletionService — Results in Completion Order
java// Problem with invokeAll(): results in SUBMISSION order
// Task 3 finishes first → you still wait for Task 1 and 2 before getting Task 3

// CompletionService: results available as soon as they complete
ExecutorService pool = Executors.newFixedThreadPool(4);
CompletionService<String> completionService = new ExecutorCompletionService<>(pool);

// Submit tasks
for (int i = 1; i <= 10; i++) {
final int taskId = i;
completionService.submit(() -> {
Thread.sleep((long)(Math.random() * 1000));  // Variable duration
return "Result-" + taskId;
});
}

// Retrieve in ORDER OF COMPLETION (not submission order!)
for (int i = 0; i < 10; i++) {
Future<String> completed = completionService.take();  // Blocks for next result
System.out.println("Just completed: " + completed.get());
// Results print as tasks finish — no waiting for slow tasks
}

// Non-blocking poll (returns null if nothing done yet)
Future<String> maybeResult = completionService.poll();
Future<String> timedResult = completionService.poll(500, TimeUnit.MILLISECONDS);

// USE CASE: Fastest-finger search
// Submit searches to 5 different indexes
// Process results as they come in — don't wait for all 5

ForkJoinPool — Divide and Conquer
java// ForkJoinPool is designed for recursive divide-and-conquer tasks
// Uses WORK STEALING — idle threads steal tasks from busy threads' queues

// Powers: parallel streams, CompletableFuture (default pool)
ForkJoinPool commonPool = ForkJoinPool.commonPool();
// Default size: N-1 threads (leave one CPU for main thread)

// Custom ForkJoinPool for CPU-bound parallel streams
ForkJoinPool customPool = new ForkJoinPool(4);
List<Long> results = customPool.submit(() ->
data.parallelStream()
.filter(n -> isPrime(n))
.collect(Collectors.toList())
).get();
```
// ─── RecursiveTask — custom divide-and-conquer ────────────────────
```
class MergeSort extends RecursiveTask<int[]> {
private final int[] array;

    MergeSort(int[] array) { this.array = array; }

    @Override
    protected int[] compute() {
        if (array.length <= 1000) {
            // Base case: small enough — sort directly (no more forking)
            return Arrays.copyOf(array, array.length); // actual sort here
        }

        // Fork: split and solve halves in parallel
        int mid = array.length / 2;
        MergeSort leftTask  = new MergeSort(Arrays.copyOfRange(array, 0, mid));
        MergeSort rightTask = new MergeSort(Arrays.copyOfRange(array, mid, array.length));

        leftTask.fork();                // Submit left subtask to pool
        int[] right = rightTask.compute(); // Compute right IN THIS THREAD
        int[] left  = leftTask.join();  // Wait for left subtask result

        return merge(left, right);      // Combine
    }
}
```
// Usage

```
ForkJoinPool fjPool = new ForkJoinPool();
int[] sorted = fjPool.invoke(new MergeSort(largeArray));

ThreadLocal — Per-Thread State (and Its Dangers)
java// ThreadLocal: each thread gets its OWN copy of the variable
// No synchronization needed — threads never share the same copy

// ─── Classic use: Request context in web apps ─────────────────────
public class RequestContext {
// Each thread (each HTTP request handler) has its own User
private static final ThreadLocal<User> currentUser = new ThreadLocal<>();
private static final ThreadLocal<String> requestId  = new ThreadLocal<>();
private static final ThreadLocal<Long>   startTime  = new ThreadLocal<>();

    public static void set(User user, String reqId) {
        currentUser.set(user);
        requestId.set(reqId);
        startTime.set(System.currentTimeMillis());
    }

    public static User    getUser()      { return currentUser.get(); }
    public static String  getRequestId() { return requestId.get();   }
    public static long    getStartTime() { return startTime.get();   }

    // ⚠️ CRITICAL: Always clear in finally! Threads are REUSED in pools.
    // Not clearing = old user leaks to next request on same thread = security bug!
    public static void clear() {
        currentUser.remove();
        requestId.remove();
        startTime.remove();
    }
}

// In your web framework (servlet filter, Spring interceptor):
try {
RequestContext.set(authenticatedUser, generateRequestId());
chain.doFilter(request, response);     // Handle request
} finally {
RequestContext.clear();                // ALWAYS clean up in finally!
}
```
// ─── InheritableThreadLocal ────────────────────────────────────────
```
// Child threads automatically inherit parent's values at creation time
private static final InheritableThreadLocal<String> traceId =
new InheritableThreadLocal<>();

traceId.set("trace-abc-123");          // Set in parent thread
new Thread(() -> {
System.out.println(traceId.get()); // "trace-abc-123" — inherited!
}).start();

// ⚠️ Thread pools: InheritableThreadLocal inherits at THREAD CREATION
// Not at TASK SUBMISSION — may inherit stale parent values!
// For distributed tracing: use explicit context propagation instead

// ─── ThreadLocal with initialValue ────────────────────────────────
// Avoid null-check boilerplate
private static final ThreadLocal<SimpleDateFormat> dateFormat =
ThreadLocal.withInitial(() -> new SimpleDateFormat("yyyy-MM-dd"));
// SimpleDateFormat is NOT thread-safe — ThreadLocal gives each thread its own copy
// Pattern: use ThreadLocal for non-thread-safe objects that are expensive to create
String formatted = dateFormat.get().format(new Date()); // Always safe, never shared
```
The Q&A Round 🎤

Q1. What happens if you submit tasks to a shutdown pool?
```
"Once shutdown() is called, the pool no longer accepts new tasks. Calling execute() or submit() after shutdown() throws RejectedExecutionException — it goes through the rejection handler just like overload. The key distinction: isShutdown() returns true as soon as shutdown() is called, but isTerminated() only returns true after ALL tasks have finished executing. A pool can be shutdown but not yet terminated — shutdown requested but work still in progress. I always pair shutdown() with awaitTermination() in a finally block, and add a fallback to shutdownNow() if termination takes too long."
```

Q2. What is the difference between shutdown() and shutdownNow()?
```
"shutdown() is graceful — no new tasks accepted, currently running and queued tasks complete normally. Returns immediately without waiting for completion. shutdownNow() is forceful — sends interrupt signal to all running threads, and returns the list of queued tasks that were never started. It's like telling running workers 'stop what you're doing' (they may or may not respond to the interrupt depending on how they handle InterruptedException). In production I always try shutdown() + awaitTermination() first, fall back to shutdownNow() only if graceful drain takes too long — like a 60-second SLA for shutdown during deployment."
```

Q3. Why is Executors.newFixedThreadPool() considered dangerous in production?
    
    "The risk is the unbounded queue. newFixedThreadPool(4) creates a LinkedBlockingQueue with no size limit. If tasks are submitted faster than the 4 threads can process them, the queue grows without bound. Each queued task holds references to objects — eventually the heap fills up and you get OutOfMemoryError. In production I always use ThreadPoolExecutor directly with a bounded ArrayBlockingQueue and a CallerRunsPolicy rejection handler. The bounded queue caps memory usage. CallerRunsPolicy provides natural backpressure — when the pool is overwhelmed, the HTTP thread that's submitting tasks slows down, which naturally throttles incoming traffic. It's self-regulating."
    

Q4. What is work stealing in ForkJoinPool?

    "In a regular thread pool, each thread has one shared queue — contention on that queue under high load. In ForkJoinPool, each thread has its OWN deque (double-ended queue) of tasks. When a thread runs out of its own tasks, it STEALS tasks from the END of another busy thread's deque. The busy thread adds to its own deque from one end; the thief steals from the other end — minimal contention. This makes ForkJoinPool extremely efficient for recursive divide-and-conquer tasks that generate many small subtasks. It's what powers parallel streams under the hood."


Q5. How would you implement a rate-limited executor that processes max 100 tasks per second?
```
javaclass RateLimitedExecutor {
private final ExecutorService delegate;
private final RateLimiter limiter;  // Guava RateLimiter

    public RateLimitedExecutor(ExecutorService delegate, double tasksPerSecond) {
        this.delegate = delegate;
        this.limiter  = RateLimiter.create(tasksPerSecond);  // Guava
    }

    public <T> Future<T> submit(Callable<T> task) {
        limiter.acquire();   // Blocks until a permit is available
        // This slows the submitting thread — natural rate control
        return delegate.submit(task);
    }
}

// Or with Java Semaphore (no Guava):
class TokenBucketExecutor {
private final ExecutorService pool = Executors.newFixedThreadPool(4);
private final Semaphore semaphore  = new Semaphore(100);  // 100 permits/second

    public TokenBucketExecutor() {
        // Refill permits every second
        Executors.newSingleThreadScheduledExecutor()
            .scheduleAtFixedRate(
                () -> semaphore.release(100 - semaphore.availablePermits()),
                0, 1, TimeUnit.SECONDS
            );
    }

    public void submit(Runnable task) {
        if (semaphore.tryAcquire()) {
            pool.execute(task);
        } else {
            throw new RateLimitExceededException("Rate limit: 100 req/s");
        }
    }
}
```
Q6. What is a ThreadPoolExecutor hook method? How would you add tracing?
```java// ThreadPoolExecutor has 3 hook methods for monitoring/instrumentation
class TracingThreadPoolExecutor extends ThreadPoolExecutor {

    public TracingThreadPoolExecutor(int core, int max, long keepAlive,
            TimeUnit unit, BlockingQueue<Runnable> queue) {
        super(core, max, keepAlive, unit, queue);
    }

    // Called before a task starts running (in the worker thread)
    @Override
    protected void beforeExecute(Thread t, Runnable r) {
        super.beforeExecute(t, r);
        MDC.put("poolThread", t.getName());   // For log correlation
        Metrics.timer("task.wait").record(/* queue time */);
        log.debug("Task starting on {}", t.getName());
    }

    // Called after a task completes (always, even if it threw)
    @Override
    protected void afterExecute(Runnable r, Throwable t) {
        super.afterExecute(r, t);
        MDC.clear();
        if (t != null) {
            log.error("Task threw exception", t);
            Metrics.counter("task.failures").increment();
        } else {
            Metrics.counter("task.success").increment();
        }
    }

    // Called when pool terminates (after all tasks done + shutdown called)
    @Override
    protected void terminated() {
        super.terminated();
        log.info("Pool terminated. Total completed: {}", getCompletedTaskCount());
    }
}
```
Q7. How does Virtual Threads (Java 21) change thread pool design?
```
"Virtual threads are JVM-managed lightweight threads — you can create millions of them. They're perfect for I/O-bound workloads because when a virtual thread blocks on I/O, it doesn't block the underlying OS thread (carrier thread) — the JVM parks the virtual thread and runs another on the same carrier. This means the old 'thread pool sizing for I/O' problem largely goes away for I/O-bound code. Executors.newVirtualThreadPerTaskExecutor() creates a new virtual thread per task — something unthinkable with platform threads. HOWEVER: virtual threads still have contention on synchronized blocks (pinning — carrier thread is pinned during synchronized), so CPU-bound tasks and code with synchronized still benefit from platform thread pools. The mental model shift: for I/O-bound, use virtual threads freely; for CPU-bound, use fixed platform thread pools sized to core count."

java// Java 21 Virtual Threads
ExecutorService virtualPool = Executors.newVirtualThreadPerTaskExecutor();

// Create 100,000 virtual threads — impossible with platform threads!
for (int i = 0; i < 100_000; i++) {
virtualPool.submit(() -> {
Thread.sleep(1000);   // Virtual thread parks — doesn't block OS thread!
return fetchFromDB(); // I/O — perfectly suited for virtual threads
});
}
// All 100k tasks run "simultaneously" with minimal OS resources
// Underlying: maybe 8 carrier threads handling 100k virtual threads via parking
```

---

## Section 6 Master Summary 🧠
```
ThreadPoolExecutor:
corePoolSize   → always-alive threads (even if idle)
maxPoolSize    → max threads (only created when queue is FULL)
queue          → ALWAYS use bounded (prevents OOM)
rejection      → CallerRunsPolicy = safest for most cases

Sizing formula:
CPU-bound  → N_cores threads
I/O-bound  → N_cores × (1 + wait/compute)
Mixed      → profile, start at N_cores × 2

Factory methods:
newFixedThreadPool     → ❌ unbounded queue risk, use for known-bounded tasks
newCachedThreadPool    → ❌ unbounded threads risk, only for tiny short tasks
newSingleThreadExec    → ✅ sequential guaranteed
newScheduledThreadPool → ✅ for periodic tasks

Submit methods:
execute()    → fire-and-forget, Runnable only
submit()     → returns Future, track result + exception
invokeAll()  → wait for ALL, results in submission order
invokeAny()  → first result, cancel rest

Shutdown:
shutdown() + awaitTermination() → graceful (always prefer)
shutdownNow() → interrupt + drain (last resort)

ThreadLocal:
Per-thread state, no sync needed
ALWAYS remove() in finally when using with thread pools!
