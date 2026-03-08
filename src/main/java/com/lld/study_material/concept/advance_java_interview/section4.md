Section 4: java.util.concurrent Utilities 

Sections 1-3 were about problems and low-level tools. Section 4 is about high-level coordination primitives — the tools Java gives you so you don't have to solve hard synchronization problems from scratch. FAANG interviewers love these because they reveal whether you think in systems, not just code.

The Big Picture — Why These Exist
You already know:
synchronized → mutual exclusion (one thread at a time)
wait/notify  → basic thread signaling

But real systems need richer coordination:

"Start serving traffic only AFTER all 5 services initialize"
→ CountDownLatch

"Allow max 10 concurrent database connections at once"
→ Semaphore

"All 8 worker threads must finish Phase 1 before ANY starts Phase 2"
→ CyclicBarrier

"Pipeline with dynamic worker count across 5 phases"
→ Phaser

"Read result of async computation without blocking forever"
→ Future / CompletableFuture (Section 7)

These aren't just convenience wrappers.
They encode hard-won concurrency wisdom into a clean API.

Part 1: Semaphore — Counting Permits
The Mental Model
Think of a Semaphore as a bouncer at a nightclub:

Club capacity: 10 people (permits = 10)

Person arrives → acquire():
If people inside < 10: enter immediately
If people inside = 10: WAIT outside until someone leaves

Person leaves → release():
One waiting person outside is let in
Available permits increases by 1

Key insight: Semaphore controls HOW MANY threads can access
a resource simultaneously — not WHO or in WHAT ORDER.
Semaphore Internals
java// Semaphore is backed by AbstractQueuedSynchronizer (AQS)
// — the same framework that powers ReentrantLock, CountDownLatch, etc.

// Two modes:
Semaphore unfair = new Semaphore(10);         // Default: unfair (higher throughput)
Semaphore fair   = new Semaphore(10, true);   // Fair: FIFO order — no starvation

// Core methods:
semaphore.acquire();              // Take 1 permit (blocks if none available)
semaphore.acquire(3);             // Take 3 permits at once (blocks until 3 free)
semaphore.release();              // Return 1 permit
semaphore.release(3);             // Return 3 permits at once

semaphore.tryAcquire();           // Try to take 1 permit — return false if none
semaphore.tryAcquire(3);          // Try to take 3 permits — return false if < 3
semaphore.tryAcquire(500, TimeUnit.MILLISECONDS); // Wait max 500ms

semaphore.availablePermits();     // How many permits currently free
semaphore.getQueueLength();       // How many threads waiting
semaphore.drainPermits();         // Take ALL available permits at once (returns count)
Use Case 1: Connection Pool
java// The most classic Semaphore use case
// Database allows max 10 concurrent connections
// Any more → queue up, wait for one to become free

class DatabaseConnectionPool {
private final Semaphore    semaphore;
private final Queue<Connection> pool;
private final int          maxSize;

    DatabaseConnectionPool(int maxConnections) {
        this.maxSize   = maxConnections;
        this.semaphore = new Semaphore(maxConnections, true); // fair!
        this.pool      = new ConcurrentLinkedQueue<>();

        // Pre-create all connections
        for (int i = 0; i < maxConnections; i++) {
            pool.offer(createConnection(i));
        }
    }

    // ── Blocking acquire ──────────────────────────────────────────
    public Connection acquire() throws InterruptedException {
        semaphore.acquire();       // Blocks until a permit (connection) is free
        Connection conn = pool.poll();

        // This should never be null if semaphore is used correctly
        // semaphore count == pool size at all times
        if (conn == null) {
            semaphore.release();   // Safety: return permit if pool is empty
            throw new IllegalStateException("Pool is empty — semaphore bug!");
        }
        return conn;
    }

    // ── Non-blocking acquire (with timeout) ───────────────────────
    public Connection tryAcquire(long timeoutMs) throws InterruptedException {
        if (!semaphore.tryAcquire(timeoutMs, TimeUnit.MILLISECONDS)) {
            return null;           // No connection available within timeout
        }
        Connection conn = pool.poll();
        if (conn == null) {
            semaphore.release();
            return null;
        }
        return conn;
    }

    // ── Release connection back to pool ──────────────────────────
    public void release(Connection conn) {
        if (conn == null) return;
        pool.offer(conn);          // Return connection to pool
        semaphore.release();       // Return permit — wakes one waiting thread
    }

    // ── Usage pattern ─────────────────────────────────────────────
    public <T> T withConnection(java.util.function.Function<Connection, T> work)
            throws InterruptedException {
        Connection conn = acquire();
        try {
            return work.apply(conn);
        } finally {
            release(conn);         // ALWAYS return to pool, even on exception
        }
    }

    int available()  { return semaphore.availablePermits(); }
    int waiting()    { return semaphore.getQueueLength();   }

    private Connection createConnection(int id) {
        // Create real DB connection here
        return new Connection("conn-" + id);
    }
}

// Usage:
DatabaseConnectionPool pool = new DatabaseConnectionPool(10);

// Pattern 1: Manual acquire/release
Connection conn = pool.acquire();
try {
conn.execute("SELECT * FROM users");
} finally {
pool.release(conn);  // ALWAYS in finally
}

// Pattern 2: Lambda (cleaner)
String result = pool.withConnection(c -> c.execute("SELECT COUNT(*) FROM orders"));
Use Case 2: Rate Limiter
java// Allow max 100 API calls per second across all threads
// Classic token bucket pattern

class TokenBucketRateLimiter {
private final Semaphore       tokens;
private final int             maxTokens;
private final ScheduledExecutorService refiller;

    TokenBucketRateLimiter(int requestsPerSecond) {
        this.maxTokens = requestsPerSecond;
        this.tokens    = new Semaphore(requestsPerSecond);

        // Refill tokens every second
        this.refiller  = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "rate-limiter-refiller");
            t.setDaemon(true);
            return t;
        });

        refiller.scheduleAtFixedRate(() -> {
            // How many permits are "used" (missing from semaphore)?
            int used     = maxTokens - tokens.availablePermits();
            // Refill them
            if (used > 0) tokens.release(used);
        }, 1, 1, TimeUnit.SECONDS);
    }

    // Blocking — wait until a token is available
    public void acquire() throws InterruptedException {
        tokens.acquire();
    }

    // Non-blocking — return false if rate limit exceeded right now
    public boolean tryAcquire() {
        return tokens.tryAcquire();
    }

    // Timed — wait up to timeout for a token
    public boolean tryAcquire(long timeoutMs) throws InterruptedException {
        return tokens.tryAcquire(timeoutMs, TimeUnit.MILLISECONDS);
    }

    public void shutdown() {
        refiller.shutdown();
    }
}

// Usage in an API handler:
class ApiHandler {
private final TokenBucketRateLimiter limiter = new TokenBucketRateLimiter(100);

    public Response handleRequest(Request req) throws InterruptedException {
        if (!limiter.tryAcquire()) {
            return Response.status(429).body("Rate limit exceeded: max 100 req/s");
        }
        return processRequest(req);
    }
}
Use Case 3: Bounded Parallelism
java// You have 1000 tasks but want max 5 running simultaneously
// (e.g., 5 concurrent HTTP calls to an external API)

class BoundedParallelExecutor {
private final Semaphore throttle;
private final ExecutorService pool;

    BoundedParallelExecutor(int maxConcurrent) {
        this.throttle = new Semaphore(maxConcurrent);
        this.pool     = Executors.newCachedThreadPool();  // Unlimited threads
    }

    // Submit task — at most maxConcurrent will run simultaneously
    public <T> Future<T> submit(Callable<T> task) {
        return pool.submit(() -> {
            throttle.acquire();      // Block until a slot is free
            try {
                return task.call();
            } finally {
                throttle.release();  // Free the slot ALWAYS
            }
        });
    }

    // Process a list of items with bounded parallelism
    public void processAll(List<String> items) throws InterruptedException {
        List<Future<?>> futures = new ArrayList<>();

        for (String item : items) {
            futures.add(submit(() -> {
                processItem(item);  // At most 5 of these run at once
                return null;
            }));
        }

        // Wait for all to complete
        for (Future<?> f : futures) {
            try {
                f.get();
            } catch (ExecutionException e) {
                log.error("Task failed: {}", e.getCause().getMessage());
            }
        }
    }
}

// Why this matters:
// Without throttle: 1000 tasks → 1000 concurrent HTTP calls
// External service overwhelmed → 503 errors → retry storm
// With Semaphore(5): max 5 concurrent → external service happy
```

---

## Part 2: `CountDownLatch` — One-Time Gate

### The Mental Model
```
CountDownLatch is a one-way gate with a counter.

Initialize with N = 5
Gate is CLOSED

Any thread calls await() → BLOCKS at the gate

Other threads call countDown():
N becomes 4... 3... 2... 1...

When N reaches 0:
Gate OPENS forever
ALL waiting threads released simultaneously
Future await() calls return immediately (gate stays open)

Key: ONE-TIME USE. Cannot reset. Once open, always open.
java// CountDownLatch internals (simplified):
// count = N (atomic integer)
// await() → if count > 0: block on AQS queue
// countDown() → decrement count; if count reaches 0: release ALL waiters
// Once count = 0: all future await() return immediately

CountDownLatch latch = new CountDownLatch(3);

// Blocking methods:
latch.await();                             // Block until count = 0
latch.await(5, TimeUnit.SECONDS);          // Block max 5s, returns false if timeout

// Counting down:
latch.countDown();                         // Decrement count by 1
latch.getCount();                          // Read current count (debugging)
Pattern 1: Service Readiness Gate
java// Classic: "Don't open for traffic until ALL services are ready"

class ApplicationStartup {
private final int            SERVICE_COUNT = 5;
private final CountDownLatch readyLatch    = new CountDownLatch(SERVICE_COUNT);
private final CountDownLatch stopLatch     = new CountDownLatch(1);

    enum Service { DATABASE, CACHE, AUTH, MESSAGE_QUEUE, SEARCH_INDEX }

    class ServiceInitializer implements Runnable {
        private final Service service;

        ServiceInitializer(Service service) { this.service = service; }

        @Override
        public void run() {
            try {
                System.out.println(service + ": initializing...");
                initialize(service);              // May take varying time
                System.out.println(service + ": READY ✅");
                readyLatch.countDown();           // Signal: I'm ready!

                // Keep running until stop signal
                stopLatch.await();

            } catch (Exception e) {
                log.error(service + " failed to start!", e);
                // IMPORTANT: still count down — or main thread waits forever!
                // Consider: fail fast vs degrade gracefully
                readyLatch.countDown();
            }
        }
    }

    public void start() throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(SERVICE_COUNT);

        // Launch all services in parallel
        for (Service svc : Service.values()) {
            executor.submit(new ServiceInitializer(svc));
        }

        System.out.println("Waiting for all services to be ready...");

        // Wait with timeout — don't hang forever if a service fails
        boolean allReady = readyLatch.await(30, TimeUnit.SECONDS);

        if (!allReady) {
            System.err.println("STARTUP FAILED: Not all services ready within 30s");
            System.err.println("Remaining: " + readyLatch.getCount() + " services");
            System.exit(1);
        }

        System.out.println("✅ All " + SERVICE_COUNT + " services ready!");
        System.out.println("🚀 Opening for traffic...");
        startAcceptingRequests();
    }

    public void stop() {
        stopLatch.countDown();   // Signal all services to stop
    }
}
Pattern 2: Starting Gate (Simultaneous Start)
java// Classic: "Start ALL threads at EXACTLY the same moment"
// Used for: stress testing, race condition reproduction, benchmarks

class ConcurrentStressTest {

    public TestResults runConcurrently(int threadCount, Runnable testTask)
            throws InterruptedException {

        CountDownLatch startGun    = new CountDownLatch(1);         // 1 → 0 to release
        CountDownLatch finishLine  = new CountDownLatch(threadCount);// all → 0 when done
        AtomicInteger  errorCount  = new AtomicInteger(0);
        AtomicLong     totalTimeNs = new AtomicLong(0);

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    startGun.await();   // ALL threads block here
                                        // waiting for the "gun" to fire

                    long start = System.nanoTime();
                    try {
                        testTask.run();
                    } catch (Exception e) {
                        errorCount.incrementAndGet();
                    } finally {
                        totalTimeNs.addAndGet(System.nanoTime() - start);
                    }

                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    finishLine.countDown();  // I'm done
                }
            });
        }

        // All threads are now blocked at startGun.await()
        // They're loaded and ready — release them ALL at once
        System.out.println("All " + threadCount + " threads ready. FIRING!");
        long testStart = System.nanoTime();
        startGun.countDown();            // 1 → 0 → releases ALL threads simultaneously!

        // Wait for all threads to complete
        finishLine.await();
        long testDuration = System.nanoTime() - testStart;

        executor.shutdown();

        return new TestResults(
            threadCount,
            errorCount.get(),
            totalTimeNs.get() / threadCount,  // Average latency per thread
            testDuration
        );
    }
}

// Usage:
ConcurrentStressTest test = new ConcurrentStressTest();
TestResults results = test.runConcurrently(100, () -> {
// This exact code runs on 100 threads SIMULTANEOUSLY
accountService.transfer(account1, account2, 100.0);
});
System.out.println("Errors: " + results.errorCount());  // Should be 0
Pattern 3: Fan-Out / Fan-In
java// Fan-out: distribute work to N workers
// Fan-in: collect results when ALL workers finish

class ParallelSearchEngine {
private final ExecutorService searchExecutor =
Executors.newFixedThreadPool(10);

    public List<SearchResult> search(String query) throws InterruptedException {

        // Define search sources
        List<SearchSource> sources = List.of(
            new DatabaseSearch(),
            new CacheSearch(),
            new ExternalApiSearch(),
            new FileSystemSearch()
        );

        CountDownLatch latch   = new CountDownLatch(sources.size());
        List<SearchResult> allResults = Collections.synchronizedList(new ArrayList<>());
        AtomicReference<Exception> firstError = new AtomicReference<>();

        // Fan-OUT: submit all searches in parallel
        for (SearchSource source : sources) {
            searchExecutor.submit(() -> {
                try {
                    List<SearchResult> results = source.search(query);
                    allResults.addAll(results);    // Thread-safe (synchronizedList)
                } catch (Exception e) {
                    log.warn("Search source {} failed: {}", source, e.getMessage());
                    firstError.compareAndSet(null, e);
                } finally {
                    latch.countDown();             // Signal: I'm done (success or failure)
                }
            });
        }

        // Fan-IN: wait for ALL sources to complete
        boolean completed = latch.await(5, TimeUnit.SECONDS);

        if (!completed) {
            log.warn("Search timeout: {} sources didn't respond in 5s",
                latch.getCount());
            // Return partial results — better than nothing
        }

        // Sort, deduplicate, rank
        return allResults.stream()
            .distinct()
            .sorted(Comparator.comparing(SearchResult::getScore).reversed())
            .limit(20)
            .collect(Collectors.toList());
    }
}
```

---

## Part 3: `CyclicBarrier` — Reusable Rendezvous Point

### The Mental Model
```
CyclicBarrier is a meeting point where threads wait for each other.

Initialize with N = 4 threads, optional barrier action

Each thread does its work, then calls await()
→ Thread BLOCKS until ALL 4 have called await()

When the 4th thread calls await():
→ Optional barrier action runs (in the 4th thread)
→ ALL 4 threads are RELEASED simultaneously
→ Barrier RESETS automatically to N=4
→ Can be used again immediately for next phase!

Key difference from CountDownLatch:
CountDownLatch: ONE-TIME, any thread counts down
CyclicBarrier:  REUSABLE, all N threads must arrive AT the barrier
java// CyclicBarrier API
CyclicBarrier barrier = new CyclicBarrier(4);                     // No action
CyclicBarrier barrierWithAction = new CyclicBarrier(4, () -> {    // With action
System.out.println("All 4 arrived! Starting next phase...");
aggregatePhaseResults();   // Runs ONCE when all arrive
});

// In each thread:
barrier.await();                              // Wait for all N threads
barrier.await(5, TimeUnit.SECONDS);           // Wait max 5s (TimeoutException if exceeded)

// Diagnostics:
barrier.getNumberWaiting();                   // How many currently waiting
barrier.getParties();                         // Total N parties
barrier.isBroken();                           // True if any thread timed out or interrupted

barrier.reset();                              // Force-reset: all waiting threads get BrokenBarrierException
Use Case 1: Multi-Phase Parallel Computation
java// Classic: matrix operations, simulations, parallel algorithms
// ALL threads must complete phase N before ANY thread starts phase N+1

class ParallelSimulation {
private final int              threadCount;
private final CyclicBarrier    phaseBarrier;
private final double[][]       grid;
private volatile int           currentPhase = 0;

    ParallelSimulation(int threadCount, int gridSize) {
        this.threadCount  = threadCount;
        this.grid         = new double[gridSize][gridSize];

        // Barrier action: runs after EACH phase completes
        this.phaseBarrier = new CyclicBarrier(threadCount, () -> {
            currentPhase++;
            System.out.printf("Phase %d complete. Max value: %.2f%n",
                currentPhase, findMaxValue(grid));
            // This is safe here — all threads paused at barrier
            // No race conditions during barrier action
        });
    }

    class SimulationWorker implements Runnable {
        private final int workerId;
        private final int rowStart;
        private final int rowEnd;

        SimulationWorker(int id, int rowStart, int rowEnd) {
            this.workerId = id;
            this.rowStart = rowStart;
            this.rowEnd   = rowEnd;
        }

        @Override
        public void run() {
            try {
                // ── PHASE 1: Initialize ───────────────────────────────
                for (int row = rowStart; row < rowEnd; row++) {
                    initializeRow(grid[row]);
                }
                System.out.println("Worker " + workerId + " done with Phase 1");

                phaseBarrier.await();  // ← WAIT: all workers must finish Phase 1

                // ── PHASE 2: First computation step ───────────────────
                for (int row = rowStart; row < rowEnd; row++) {
                    computeStep1(grid[row]);
                }
                System.out.println("Worker " + workerId + " done with Phase 2");

                phaseBarrier.await();  // ← WAIT: all workers must finish Phase 2
                // Barrier RESETS automatically here!

                // ── PHASE 3: Second computation step ──────────────────
                for (int row = rowStart; row < rowEnd; row++) {
                    computeStep2(grid[row], grid); // May need other rows
                }
                System.out.println("Worker " + workerId + " done with Phase 3");

                phaseBarrier.await();  // ← WAIT: all workers must finish Phase 3

                System.out.println("Worker " + workerId + " finished all phases");

            } catch (BrokenBarrierException e) {
                // Another thread timed out or was interrupted
                // Barrier is "broken" — all waiting threads get this exception
                log.error("Barrier broken — another worker failed!", e);
                cancelSimulation();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    public void run() throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        int rowsPerWorker = grid.length / threadCount;

        for (int i = 0; i < threadCount; i++) {
            int rowStart = i * rowsPerWorker;
            int rowEnd   = (i == threadCount - 1) ? grid.length : rowStart + rowsPerWorker;
            executor.submit(new SimulationWorker(i, rowStart, rowEnd));
        }

        executor.shutdown();
        executor.awaitTermination(60, TimeUnit.SECONDS);
    }
}
Use Case 2: Bulk Data Pipeline
java// ETL pipeline: Extract → Transform → Load
// All workers must finish Extract before any starts Transform

class BulkDataPipeline {
private final int                    workerCount = 4;
private final List<List<RawRecord>>  extractedChunks;
private final List<List<CleanRecord>> transformedChunks;
private final CyclicBarrier          barrier;

    BulkDataPipeline(int workerCount) {
        this.extractedChunks  = new ArrayList<>(Collections.nCopies(workerCount, null));
        this.transformedChunks = new ArrayList<>(Collections.nCopies(workerCount, null));

        this.barrier = new CyclicBarrier(workerCount, () -> {
            // Runs once between phases — log stats, check errors
            long totalExtracted = extractedChunks.stream()
                .mapToLong(chunk -> chunk == null ? 0 : chunk.size()).sum();
            System.out.println("Phase complete. Records so far: " + totalExtracted);
        });
    }

    void runWorker(int workerId, List<String> myDataSources)
            throws InterruptedException, BrokenBarrierException {

        // ── Phase 1: EXTRACT ──────────────────────────────────────
        List<RawRecord> raw = new ArrayList<>();
        for (String source : myDataSources) {
            raw.addAll(extractFromSource(source));
        }
        extractedChunks.set(workerId, raw);     // Store this worker's chunk

        barrier.await();  // Wait for ALL workers to finish extracting
        // After barrier: ALL chunks available — can cross-reference if needed

        // ── Phase 2: TRANSFORM ────────────────────────────────────
        List<CleanRecord> clean = raw.stream()
            .map(this::transform)
            .filter(this::isValid)
            .collect(Collectors.toList());
        transformedChunks.set(workerId, clean);

        barrier.await();  // Wait for ALL workers to finish transforming

        // ── Phase 3: LOAD ──────────────────────────────────────────
        List<CleanRecord> myChunk = transformedChunks.get(workerId);
        bulkInsertToDatabase(myChunk);

        barrier.await();  // Wait for ALL workers to finish loading
        // After this barrier: transaction can be committed safely
    }
}
BrokenBarrierException — The Safety Net
java// If ANY thread at a CyclicBarrier:
//   - Times out (await with timeout)
//   - Is interrupted
//   - The barrier action throws an exception
//
// Then the barrier is BROKEN:
//   - All currently waiting threads get BrokenBarrierException
//   - All future await() calls on this barrier get BrokenBarrierException
//   - barrier.isBroken() returns true
//
// WHY: If one worker fails during Phase 1,
//      other workers should NOT proceed to Phase 2 with incomplete data

class SafeBarrierUsage {
void workerWithSafetyNet(CyclicBarrier barrier, int workerId) {
try {
doPhaseWork(workerId);
barrier.await(10, TimeUnit.SECONDS);   // Timeout after 10s

        } catch (TimeoutException e) {
            // THIS thread timed out — other threads will get BrokenBarrierException
            log.error("Worker {} timed out at barrier", workerId);
            // Barrier is now broken — other threads will be notified

        } catch (BrokenBarrierException e) {
            // ANOTHER thread caused the barrier to break
            log.error("Worker {} aborted: another worker failed", workerId);
            cleanupPartialWork(workerId);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Worker {} interrupted", workerId);
        }
    }
}
```

---

## Part 4: `Phaser` — The Most Flexible Synchronizer

### The Mental Model
```
Phaser = CyclicBarrier + CountDownLatch + dynamic participants

CyclicBarrier: fixed N parties, reusable phases
Phaser:        DYNAMIC parties (can join/leave mid-run), reusable phases
+ can be used as one-time latch too
+ supports hierarchical (tree of phasers for large-scale)
+ can override onAdvance() to control termination

Think of it as the Swiss Army knife of synchronizers.
java// Phaser API
Phaser phaser = new Phaser(N);              // N registered parties at start
Phaser phaser2 = new Phaser(parent, N);     // Tiered: sub-phaser with parent

// Registration (can happen at any time):
phaser.register();                          // Register 1 more party
phaser.bulkRegister(5);                     // Register 5 more parties

// Arriving:
phaser.arrive();                            // Signal arrived, DON'T wait for others
phaser.arriveAndAwaitAdvance();             // Signal arrived AND wait for all others
phaser.arriveAndDeregister();              // Signal arrived AND leave the phaser

// Waiting without arriving:
phaser.awaitAdvance(phase);                 // Wait for specific phase to complete
phaser.awaitAdvanceInterruptibly(phase);    // Same but interruptible

// Querying:
phaser.getPhase();                          // Current phase number (0, 1, 2...)
phaser.getRegisteredParties();              // Total registered
phaser.getArrivedParties();                 // How many arrived this phase
phaser.getUnarrivedParties();               // How many haven't arrived yet
phaser.isTerminated();                      // Has phaser terminated?
Use Case 1: Dynamic Worker Pool
java// Workers can join and leave the phaser dynamically at runtime
// Something CyclicBarrier can't do (fixed N)

class DynamicWorkerCoordinator {

    public void run() throws InterruptedException {
        Phaser phaser = new Phaser(1);  // 1 = register main thread as party

        // Initially 3 workers
        for (int i = 0; i < 3; i++) {
            phaser.register();          // Register this worker
            final int id = i;
            new Thread(() -> runWorker(phaser, id, 4)).start();  // 4 phases
        }

        for (int phase = 0; phase < 4; phase++) {
            System.out.println("=== Main: Phase " + phase + " starting ===");

            // Main thread participates in all 4 phases
            phaser.arriveAndAwaitAdvance();

            System.out.println("=== Main: Phase " + phase + " complete ===");

            // Dynamically add a new worker after Phase 1
            if (phase == 1) {
                System.out.println("Adding extra worker mid-run!");
                phaser.register();
                new Thread(() -> runWorker(phaser, 99, 2)).start(); // Only 2 remaining phases
            }
        }

        phaser.arriveAndDeregister();  // Main thread done
        System.out.println("All phases complete!");
    }

    void runWorker(Phaser phaser, int id, int phases) {
        for (int i = 0; i < phases; i++) {
            try {
                System.out.printf("  Worker %d: Phase %d work...%n", id, phaser.getPhase());
                Thread.sleep(100 + (long)(Math.random() * 200));  // Variable work

                phaser.arriveAndAwaitAdvance();   // Done with this phase
                System.out.printf("  Worker %d: Phase %d DONE%n", id, phaser.getPhase() - 1);

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                phaser.arriveAndDeregister();     // Clean exit
                return;
            }
        }
        phaser.arriveAndDeregister();  // Worker leaving phaser — I'm done
        System.out.println("  Worker " + id + " left the phaser");
    }
}
Use Case 2: Timed Iterations (Game Loop / Simulation)
java// Classic: "Run exactly 100 time steps, all workers synchronized each step"
// onAdvance() lets you control when phaser terminates

class GameSimulation {
private final int     TOTAL_STEPS = 100;
private final int     WORKER_COUNT = 8;
private final Phaser  phaser;
private final World   world;

    GameSimulation() {
        this.world = new World();

        // Override onAdvance to terminate after TOTAL_STEPS phases
        this.phaser = new Phaser(WORKER_COUNT) {
            @Override
            protected boolean onAdvance(int phase, int registeredParties) {
                // Called ONCE when each phase completes (in calling thread)
                System.out.printf("Step %d complete. Entities: %d%n",
                    phase + 1, world.getEntityCount());

                // Return true = terminate phaser after this phase
                // Return false = continue to next phase
                boolean shouldTerminate = (phase + 1 >= TOTAL_STEPS)
                    || registeredParties == 0;

                if (shouldTerminate) {
                    System.out.println("Simulation complete!");
                }
                return shouldTerminate;
            }
        };
    }

    class WorldUpdater implements Runnable {
        private final int        workerId;
        private final List<Entity> myEntities;

        WorldUpdater(int id, List<Entity> entities) {
            this.workerId   = id;
            this.myEntities = entities;
        }

        @Override
        public void run() {
            // Keep running until phaser terminates
            while (!phaser.isTerminated()) {
                // Update all my assigned entities for this time step
                for (Entity e : myEntities) {
                    e.update(world);
                    e.resolveCollisions(world);
                }

                // Wait for ALL workers to finish this time step
                // When last worker arrives → onAdvance() is called → phase advances
                int phase = phaser.arriveAndAwaitAdvance();

                if (phase < 0) {
                    // Negative phase = phaser terminated
                    System.out.println("Worker " + workerId + " done (phaser terminated)");
                    return;
                }
            }
        }
    }

    public void run() throws InterruptedException {
        ExecutorService exec = Executors.newFixedThreadPool(WORKER_COUNT);
        List<Entity> allEntities = world.getAllEntities();
        int chunkSize = allEntities.size() / WORKER_COUNT;

        for (int i = 0; i < WORKER_COUNT; i++) {
            List<Entity> chunk = allEntities.subList(
                i * chunkSize,
                i == WORKER_COUNT - 1 ? allEntities.size() : (i + 1) * chunkSize
            );
            exec.submit(new WorldUpdater(i, chunk));
        }

        exec.shutdown();
        exec.awaitTermination(5, TimeUnit.MINUTES);
    }
}
Use Case 3: Phaser as CountDownLatch + CyclicBarrier Combined
java// Phaser can replace EITHER a CountDownLatch OR a CyclicBarrier
// depending on how you use it

class PhaserVersatility {

    // ── As CountDownLatch (one-time, multiple countDowns) ─────────
    void asLatch() throws InterruptedException {
        int taskCount = 5;
        Phaser latch = new Phaser(taskCount + 1); // +1 for main thread

        for (int i = 0; i < taskCount; i++) {
            final int id = i;
            new Thread(() -> {
                doWork(id);
                latch.arrive();  // Count down (don't wait — just signal done)
            }).start();
        }

        latch.arriveAndAwaitAdvance(); // Main waits for all tasks to arrive
        System.out.println("All tasks done!");
        // Equivalent to CountDownLatch(5) + countDown() + await()
    }

    // ── As CyclicBarrier (multiple phases, fixed parties) ─────────
    void asBarrier() throws InterruptedException {
        int workerCount = 4;
        Phaser barrier = new Phaser(workerCount);

        for (int i = 0; i < workerCount; i++) {
            final int id = i;
            new Thread(() -> {
                for (int phase = 0; phase < 3; phase++) {
                    doPhaseWork(id, phase);
                    barrier.arriveAndAwaitAdvance(); // Same as CyclicBarrier.await()
                }
                barrier.arriveAndDeregister();
            }).start();
        }
        // Equivalent to CyclicBarrier(4) with 3 phases
    }
}

Part 5: Comparison — Choosing the Right Tool
java// The decision tree:

class SynchronizerChooser {
/*
┌─────────────────────────────────────────────────────────────────┐
│              WHICH SYNCHRONIZER DO I NEED?                     │
│                                                                 │
│  Q1: Do I need to LIMIT concurrent access (not just coordinate)?│
│      YES → Semaphore                                           │
│                                                                 │
│  Q2: Is this a ONE-TIME event?                                  │
│      YES → CountDownLatch                                       │
│            (can't reset, multiple threads count down)           │
│                                                                 │
│  Q3: Do ALL threads need to reach a point before continuing?    │
│      YES, FIXED party count → CyclicBarrier                    │
│      YES, DYNAMIC parties   → Phaser                           │
│                                                                 │
│  Q4: Do I need multiple phases?                                 │
│      YES, fixed workers      → CyclicBarrier (simpler)         │
│      YES, workers join/leave → Phaser (more powerful)          │
│                                                                 │
│  Q5: Do I need to get a RESULT from async computation?          │
│      → Future / CompletableFuture (Section 7)                  │
└─────────────────────────────────────────────────────────────────┘
*/
}
```
```
┌──────────────────┬──────────────┬──────────────┬──────────────┬──────────────┐
│                  │  Semaphore   │CountDownLatch│CyclicBarrier │    Phaser    │
├──────────────────┼──────────────┼──────────────┼──────────────┼──────────────┤
│ Reusable?        │     ✅        │      ❌       │      ✅       │      ✅       │
│ Dynamic parties? │     N/A      │      N/A     │      ❌       │      ✅       │
│ Controls count   │ Permits (N)  │ Count (N→0)  │ Party count  │ Party count  │
│ Threads blocked  │ acquire()    │ await()      │ await()      │ arriveAndAwait│
│ Who releases?    │ Other thread │ countDown()  │ N-th arrival │ N-th arrival │
│ Termination ctrl?│     ❌        │      ❌       │      ❌       │      ✅       │
│ Use case         │ Resource pool│ One-time gate│ Phase sync   │ Complex pipline│
└──────────────────┴──────────────┴──────────────┴──────────────┴──────────────┘

Part 6: The Interview Q&A Round 🎤

Q1. What is a Semaphore and how is it different from a mutex/lock?

"A Semaphore is a generalization of a lock. A mutex (or binary semaphore) has only two states: locked or unlocked — only ONE thread can hold it. A counting Semaphore starts with N permits and allows UP TO N threads to proceed simultaneously.
The key difference: a lock has ownership — the thread that locked it must unlock it. A Semaphore has no ownership — one thread can acquire() and a DIFFERENT thread can release(). This makes Semaphore ideal for producer-consumer signaling and resource pool management, where acquire and release naturally happen in different threads.
In Java: use ReentrantLock for mutual exclusion (owned), use Semaphore for resource counting and signaling (unowned)."


Q2. CountDownLatch vs CyclicBarrier — when exactly do you use each?

"The core distinction: CountDownLatch is for one thread waiting for MULTIPLE threads to complete work. CyclicBarrier is for MULTIPLE threads waiting for EACH OTHER.
CountDownLatch: main thread awaits(), worker threads countDown(). Workers don't wait for each other — they just report completion. One-time use — cannot reset. Classic example: wait for all services to start before accepting traffic.
CyclicBarrier: ALL N threads call await(). They ALL wait until ALL have arrived. Then ALL are released simultaneously. Automatically resets. Classic example: parallel simulation where all workers must complete Phase 1 before any starts Phase 2.
Memory trick: Latch = main thread waits for others. Barrier = all threads wait for each other."


Q3. What happens if one thread in a CyclicBarrier throws an exception?
java// If any thread's await() throws (timeout, interrupt):
// The barrier enters BROKEN state
// All currently-waiting threads get BrokenBarrierException
// All future await() calls get BrokenBarrierException immediately
// barrier.isBroken() returns true

// Example:
CyclicBarrier barrier = new CyclicBarrier(3);

Thread t1 = new Thread(() -> {
try {
doWork();
barrier.await(1, TimeUnit.SECONDS);  // Timeout after 1 second
} catch (TimeoutException e) {
System.out.println("T1 timed out — barrier now BROKEN");
// T1's timeout BREAKS the barrier for everyone
} catch (BrokenBarrierException | InterruptedException e) {
Thread.currentThread().interrupt();
}
});

Thread t2 = new Thread(() -> {
try {
doWork();
barrier.await();    // This will get BrokenBarrierException
} catch (BrokenBarrierException e) {
System.out.println("T2 aborted — another thread broke the barrier!");
// T2 knows to stop and clean up
} catch (InterruptedException e) {
Thread.currentThread().interrupt();
}
});

// To recover: barrier.reset()
// But reset() also causes BrokenBarrierException in waiting threads
// Best practice: create a new CyclicBarrier instead of resetting

Q4. Implement a bounded blocking counter using Semaphore
java// Counter that blocks incrementers when it reaches max
// and blocks decrementers when it reaches 0
// Classic bounded counter problem

class BoundedCounter {
private final Semaphore increment; // Permits = how much MORE we can increment
private final Semaphore decrement; // Permits = how much we can decrement
private final AtomicInteger value;

    BoundedCounter(int min, int max, int initial) {
        if (initial < min || initial > max)
            throw new IllegalArgumentException("Initial out of bounds");

        this.value     = new AtomicInteger(initial);
        this.increment = new Semaphore(max - initial); // Room to grow
        this.decrement = new Semaphore(initial - min); // Room to shrink
    }

    public void increment() throws InterruptedException {
        increment.acquire();   // Block if at max — no room to grow
        value.incrementAndGet();
        decrement.release();   // Now there's room to decrement
    }

    public void decrement() throws InterruptedException {
        decrement.acquire();   // Block if at min — no room to shrink
        value.decrementAndGet();
        increment.release();   // Now there's room to increment
    }

    public int get() { return value.get(); }
}

// Usage:
BoundedCounter counter = new BoundedCounter(0, 5, 0);
// counter.increment() blocks when value = 5
// counter.decrement() blocks when value = 0

// This elegantly encodes the bounds using permit counts
// No explicit if/while condition checks needed
// Semaphore handles all the blocking and waiting

Q5. Design an initialization barrier for a multi-stage startup
java// Real FAANG scenario:
// Stage 1: Start DB + Cache (can start simultaneously)
// Stage 2: Start Auth service (needs DB + Cache ready)
// Stage 3: Start API servers (need Auth + Cache ready)
// Stage 4: Open load balancer (all services ready)

class MultiStageStartup {

    public void start() throws InterruptedException {
        // Stage 1: DB and Cache start in parallel
        CountDownLatch stage1Done = new CountDownLatch(2);

        ExecutorService exec = Executors.newCachedThreadPool();

        exec.submit(() -> { startDatabase(); stage1Done.countDown(); });
        exec.submit(() -> { startCache();    stage1Done.countDown(); });

        // Stage 2: Auth needs Stage 1 complete
        exec.submit(() -> {
            try {
                stage1Done.await();           // Wait for DB + Cache
                startAuthService();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        // Stage 3: API servers need Auth + Cache (stage 1 guaranteed stage 1 has Cache)
        CountDownLatch authReady = new CountDownLatch(1);

        exec.submit(() -> {
            try {
                stage1Done.await();           // Wait for DB + Cache
                startAuthService();
                authReady.countDown();        // Signal auth is ready
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        exec.submit(() -> {
            try {
                authReady.await();            // Wait for Auth
                stage1Done.await();           // Guaranteed done (auth needed stage1)
                for (int i = 0; i < 3; i++)
                    startApiServer(i);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        // Stage 4: Load balancer needs everything
        CountDownLatch allReady = new CountDownLatch(1);

        exec.submit(() -> {
            try {
                authReady.await();            // Everything above done
                allReady.countDown();
                openLoadBalancer();           // NOW open for traffic
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        allReady.await(60, TimeUnit.SECONDS); // Main thread waits
        System.out.println("System fully started!");
        exec.shutdown();
    }
}
```

---

## Section 4 Master Summary 🧠
```
SEMAPHORE:
Controls HOW MANY threads access a resource simultaneously
acquire() = take permit (block if none)
release() = return permit (wake one waiter)
No ownership — one thread acquires, another can release
Use for: connection pools, rate limiters, bounded parallelism
Fair mode (true): FIFO — no starvation

COUNTDOWNLATCH:
One-time gate: N → 0 → opens forever
await()      = block until count = 0
countDown()  = decrement count by 1
ONE-TIME: cannot reset after reaching 0
Use for: startup gates, fan-in collection, test synchronization
Pattern: use while(!latch.await(timeout)) for resilience

CYCLICBARRIER:
Meeting point: ALL N threads wait for each other
await()      = arrive and wait for all N
REUSABLE: auto-resets after each phase
Barrier action: runs once when all arrive (good for phase transitions)
BrokenBarrierException: if any thread times out → all waiters notified
Use for: multi-phase parallel computation, synchronized pipeline stages

PHASER:
Most powerful: CyclicBarrier + dynamic parties + termination control
register()              = join the phaser
arriveAndAwaitAdvance() = sync point (like CyclicBarrier.await())
arriveAndDeregister()   = leave the phaser
arrive()                = signal done, don't wait (like countDown())
onAdvance() override    = control when phaser terminates
Use for: complex pipelines, dynamic worker pools, game loops

DECISION GUIDE:
"Limit concurrent access to N" → Semaphore
"One-time: main waits for N workers to finish" → CountDownLatch
"Fixed N workers, reusable phases, all wait for each other" → CyclicBarrier
"Dynamic workers, complex phases, need termination control" → Phaser

KEY INSIGHT FOR FAANG:

    All four are built on AQS (AbstractQueuedSynchronizer)
    They're all lock-free in the fast path (CAS-based)
    They all support interruption and timeouts
    They all guarantee memory visibility (happens-before on release→acquire)