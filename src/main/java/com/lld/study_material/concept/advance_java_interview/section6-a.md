Section 6-a: CompletableFuture & Async Programming 
Section 5 gave you thread pools — the engine. Section 6 gives you CompletableFuture — 
the steering wheel. This is Java's most powerful concurrency feature and the one 
FAANG interviewers probe deepest because it tests async thinking, functional composition, 
error handling, and system design simultaneously.

The Problem With Future<T> — Why CompletableFuture Exists
Java 5 gave us Future<T>. It had one fatal flaw: blocking.

Future<String> future = executor.submit(() -> fetchUser("u1"));

// To get the result, you MUST block:
String user = future.get();   // ← Thread sits here doing NOTHING

// You cannot:
// ✅ "When this finishes, automatically run the next step"
// ✅ "If this fails, run a fallback automatically"
// ✅ "Combine two futures when BOTH complete"
// ✅ "Complete it manually from outside"
// ✅ "Chain multiple async operations"

// Future is: submit → block → get
// CompletableFuture is: submit → transform → transform → combine → handle errors
//                       ALL without blocking a thread
java// The difference in one example:

// ── Old Future way — thread blocked at each step ──────────────────
String userId   = userFuture.get();          // Block 1: wait for user
String orderId  = orderFuture.get();         // Block 2: wait for order
String result   = combine(userId, orderId);  // Block 3: now combine
// Total time: time(user) + time(order) — SEQUENTIAL

// ── CompletableFuture way — no blocking ───────────────────────────
CompletableFuture<String> result = CompletableFuture
.supplyAsync(() -> fetchUser("u1"))
.thenCombine(
CompletableFuture.supplyAsync(() -> fetchOrder("o1")),
(user, order) -> combine(user, order)
);
// Total time: max(time(user), time(order)) — PARALLEL
// No thread blocked while waiting — they're doing other work

Part 1: Creation — The Four Entry Points
java// ── supplyAsync — async task WITH result ──────────────────────────
CompletableFuture<String> cf1 = CompletableFuture.supplyAsync(
() -> fetchFromDatabase("user:1")   // Supplier<T>
);
// Runs on ForkJoinPool.commonPool() — AVOID in production!

// ✅ Always provide your own executor
ExecutorService ioPool = Executors.newFixedThreadPool(10);
CompletableFuture<String> cf2 = CompletableFuture.supplyAsync(
() -> fetchFromDatabase("user:1"),
ioPool                              // Your controlled executor
);
// Why: commonPool is shared JVM-wide
// A slow DB call can starve parallel streams, other CompletableFutures
// Your own pool = isolated, sized correctly, named threads


// ── runAsync — async task WITHOUT result (side effects) ───────────
CompletableFuture<Void> cf3 = CompletableFuture.runAsync(
() -> sendEmail(user),              // Runnable
emailExecutor
);


// ── completedFuture — already done (useful for testing/caching) ───
CompletableFuture<String> cached = CompletableFuture.completedFuture("cached-value");
// Returns immediately — no async execution
// Use for: cache hits, default values, mocking in tests


// ── failedFuture — already failed (Java 9+) ───────────────────────
CompletableFuture<String> failed = CompletableFuture.failedFuture(
new ServiceUnavailableException("DB is down")
);
// Immediately failed — any .get() throws ExecutionException
// Use for: testing error handling paths


// ── new CompletableFuture<>() — manual completion ─────────────────
CompletableFuture<String> promise = new CompletableFuture<>();

// Complete it from ANYWHERE — any thread, any time
promise.complete("result");                // Complete with value
promise.completeExceptionally(new RuntimeException("failed")); // Complete with error
promise.cancel(true);                      // Cancel

// Classic use: bridge callback-based APIs to CompletableFuture
void fetchWithCallback(String url, CompletableFuture<String> promise) {
httpClient.get(url, new Callback() {
@Override
public void onSuccess(String body) {
promise.complete(body);         // Bridge: callback → CF
}
@Override
public void onError(Exception e) {
promise.completeExceptionally(e);
}
});
}

CompletableFuture<String> result = new CompletableFuture<>();
fetchWithCallback("https://api.example.com/users", result);
result.thenApply(body -> parseJson(body))
.thenAccept(users -> display(users));

Part 2: Transformation — The Core Pipeline Operations
thenApply — Synchronous Map
java// Transform the result when it's available
// The mapping function runs on the COMPLETING thread (or calling thread if already done)
// Like Stream.map() — same thread, no new async task

CompletableFuture<Integer> length = CompletableFuture
.supplyAsync(() -> fetchName("u1"), pool)   // Future<String>
.thenApply(name -> name.length());           // Future<Integer>
// name.length() is FAST — fine to run inline

// Chain multiple transforms
CompletableFuture<String> pipeline = CompletableFuture
.supplyAsync(() -> fetchRawJson("u1"), pool) // Future<String>    raw JSON
.thenApply(json -> parseJson(json))          // Future<User>      parsed
.thenApply(user -> user.getEmail())          // Future<String>    email
.thenApply(email -> email.toLowerCase());    // Future<String>    normalized


// thenApplyAsync — run transform on a new thread
CompletableFuture<User> cf = CompletableFuture
.supplyAsync(() -> fetchRawData(), ioPool)
.thenApplyAsync(raw -> heavyTransform(raw), cpuPool); // Heavy work → CPU pool
// thenApply:      runs on completing thread — good for FAST transforms
// thenApplyAsync: runs on executor thread   — good for SLOW/CPU-intensive transforms
thenAccept and thenRun — Terminal Consumers
java// thenAccept — consume result, return Void
CompletableFuture<Void> cf = CompletableFuture
.supplyAsync(() -> fetchOrder("o1"), pool)
.thenAccept(order -> {
saveToCache(order);        // Side effect — no return value needed
log.info("Order cached: {}", order.getId());
});
// Returns CompletableFuture<Void> — can still chain after it

// thenRun — run action, no access to result
CompletableFuture<Void> cf2 = CompletableFuture
.supplyAsync(() -> processPayment(paymentRequest), pool)
.thenRun(() -> {
metrics.increment("payments.processed");  // Don't need the result
log.info("Payment processing complete");
});

// Practical: fire-and-forget with logging
CompletableFuture.supplyAsync(() -> sendNotification(userId), notifPool)
.thenAccept(result -> log.info("Notification sent: {}", result))
.exceptionally(ex -> { log.error("Notification failed", ex); return null; });
thenCompose — Async FlatMap (The Most Important One)
java// PROBLEM: what if your transform function ITSELF returns a CompletableFuture?

// ❌ thenApply with async function → nested Future
CompletableFuture<CompletableFuture<Profile>> nested = CompletableFuture
.supplyAsync(() -> findUser("u1"), pool)        // CF<User>
.thenApply(user ->
CompletableFuture.supplyAsync(              // Returns CF<Profile>
() -> loadProfile(user.getId()), pool)
);
// Type: CF<CF<Profile>> — useless! You'd have to call .get().get()


// ✅ thenCompose FLATTENS the nesting — like flatMap for streams
CompletableFuture<Profile> flat = CompletableFuture
.supplyAsync(() -> findUser("u1"), pool)        // CF<User>
.thenCompose(user ->
CompletableFuture.supplyAsync(             // Returns CF<Profile>
() -> loadProfile(user.getId()), pool)
);
// Type: CF<Profile> — flat! One level, clean.


// Real example: sequential dependent async calls
CompletableFuture<OrderConfirmation> processOrder(String userId, Cart cart) {
return CompletableFuture
.supplyAsync(() -> validateUser(userId), ioPool)         // Step 1: validate
.thenCompose(user ->
CompletableFuture.supplyAsync(                       // Step 2: check inventory
() -> checkInventory(cart), ioPool)
)
.thenCompose(inventoryOk ->
CompletableFuture.supplyAsync(                       // Step 3: charge payment
() -> chargePayment(userId, cart.getTotal()), payPool)
)
.thenCompose(paymentResult ->
CompletableFuture.supplyAsync(                       // Step 4: create order
() -> createOrder(userId, cart, paymentResult), ioPool)
);
// Each step DEPENDS on the previous step's result
// Sequential chain — each starts only after previous completes
// But none block a thread while waiting
}


// thenCompose vs thenApply decision rule:
// Does your transform return a plain value?          → thenApply
// Does your transform return a CompletableFuture<T>? → thenCompose
//
// Memory trick:
// thenApply   = map    (Stream.map)
// thenCompose = flatMap (Stream.flatMap)

Part 3: Combining Multiple Futures
thenCombine — Two Independent Parallel Futures
java// When you have TWO independent async tasks and need BOTH results

// ❌ Sequential — wastes time
String user    = fetchUser("u1").get();      // Wait 100ms
List<Order> orders = fetchOrders("u1").get(); // Wait 100ms
// Total: 200ms

// ✅ Parallel with thenCombine
CompletableFuture<String>       userFuture   = CompletableFuture
.supplyAsync(() -> fetchUser("u1"), pool);

CompletableFuture<List<Order>>  ordersFuture = CompletableFuture
.supplyAsync(() -> fetchOrders("u1"), pool);

CompletableFuture<UserDashboard> dashboard = userFuture
.thenCombine(
ordersFuture,
(user, orders) -> buildDashboard(user, orders)  // BiFunction
);
// Total: max(100ms, 100ms) = 100ms — 2× faster!
// BiFunction called ONLY when BOTH futures complete


// Chain of combines — three parallel then combine
CompletableFuture<String>    nameFuture    = supplyAsync(() -> fetchName("u1"),    pool);
CompletableFuture<Address>   addressFuture = supplyAsync(() -> fetchAddress("u1"), pool);
CompletableFuture<List<Tag>> tagsFuture    = supplyAsync(() -> fetchTags("u1"),    pool);

CompletableFuture<UserProfile> profile = nameFuture
.thenCombine(addressFuture, (name, addr) -> new PartialProfile(name, addr))
.thenCombine(tagsFuture, (partial, tags) -> partial.withTags(tags));
// All three run in parallel, combine step-by-step as they complete


// thenCombineAsync — combine on specific thread pool
userFuture.thenCombineAsync(ordersFuture,
(user, orders) -> heavyCombine(user, orders),  // Heavy → own thread
cpuPool
);
allOf — Wait for ALL Futures
java// allOf: returns CF<Void> that completes when ALL given futures complete
// ⚠️ Returns Void — you must manually collect results from original futures

CompletableFuture<String>   cf1 = supplyAsync(() -> fetchFromDB(),    pool);
CompletableFuture<String>   cf2 = supplyAsync(() -> fetchFromCache(), pool);
CompletableFuture<String>   cf3 = supplyAsync(() -> fetchFromAPI(),   pool);

// Wait for ALL, then collect results
CompletableFuture<List<String>> allResults = CompletableFuture
.allOf(cf1, cf2, cf3)
.thenApply(v -> {                          // v is Void — ignore it
// All three are DONE by the time thenApply runs
return List.of(
cf1.join(),                        // join() = get() without checked exception
cf2.join(),                        // Safe here — we know it's done (allOf)
cf3.join()
);
});

// ✅ Generic utility — collect N futures into List<T>
static <T> CompletableFuture<List<T>> allOfList(List<CompletableFuture<T>> futures) {
CompletableFuture<Void> allDone = CompletableFuture.allOf(
futures.toArray(new CompletableFuture[0])
);
return allDone.thenApply(v ->
futures.stream()
.map(CompletableFuture::join)   // All done — no blocking
.collect(Collectors.toList())
);
}

// Usage:
List<CompletableFuture<UserScore>> scoreFutures = userIds.stream()
.map(id -> supplyAsync(() -> computeScore(id), pool))
.collect(Collectors.toList());

List<UserScore> allScores = allOfList(scoreFutures).get();


// allOf with timeout — don't wait forever
CompletableFuture<List<String>> withTimeout = CompletableFuture
.allOf(cf1, cf2, cf3)
.orTimeout(3, TimeUnit.SECONDS)            // Java 9+: fail if > 3s
.thenApply(v -> List.of(cf1.join(), cf2.join(), cf3.join()))
.exceptionally(ex -> {
log.warn("Some futures timed out: {}", ex.getMessage());
// Return partial results — collect whichever are done
return List.of(cf1, cf2, cf3).stream()
.filter(CompletableFuture::isDone)
.filter(f -> !f.isCompletedExceptionally())
.map(CompletableFuture::join)
.collect(Collectors.toList());
});
anyOf — First to Complete Wins
java// anyOf: completes when the FIRST future completes (success or failure)
// Returns CF<Object> — need to cast (type safety lost — known limitation)

CompletableFuture<Object> fastest = CompletableFuture.anyOf(
supplyAsync(() -> queryPrimaryDB("u1"),    pool),
supplyAsync(() -> queryReplicaUS("u1"),    pool),
supplyAsync(() -> queryReplicaEU("u1"),    pool)
);
// Whichever replica responds first → use that result
// Others are CANCELLED (or run to completion — JVM doesn't cancel them!)

String result = (String) fastest.get();  // Cast needed — type erasure


// ✅ Type-safe anyOf for same-type futures
static <T> CompletableFuture<T> anyOfTyped(List<CompletableFuture<T>> futures) {
CompletableFuture<T> result = new CompletableFuture<>();

    for (CompletableFuture<T> future : futures) {
        future.whenComplete((value, ex) -> {
            if (ex == null) {
                result.complete(value);    // First success completes the result
            }
            // Failures ignored — another future may still succeed
        });
    }

    // If ALL fail, complete with the last exception
    CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
        .exceptionally(ex -> { result.completeExceptionally(ex); return null; });

    return result;
}

// Usage:
List<CompletableFuture<String>> replicas = List.of(
supplyAsync(() -> primaryDB.query("u1"),  pool),
supplyAsync(() -> replicaUS.query("u1"),  pool),
supplyAsync(() -> replicaEU.query("u1"),  pool)
);
String fastestResult = anyOfTyped(replicas).get(2, TimeUnit.SECONDS);

Part 4: Error Handling — Complete Guide
java// Three ways to handle errors — each for different scenarios

// ── exceptionally — fallback on failure ───────────────────────────
CompletableFuture<User> withFallback = CompletableFuture
.supplyAsync(() -> fetchFromPrimaryDB("u1"), pool)
.exceptionally(ex -> {
// ONLY called if previous stage FAILED
log.warn("Primary DB failed: {}, using fallback", ex.getMessage());
return fetchFromCache("u1");  // Return fallback value — same type T
// ⚠️ This fallback is SYNCHRONOUS — runs on completing thread
// For async fallback: use handle() + thenCompose()
});
// If fetch succeeds: exceptionally is SKIPPED
// If fetch fails: exceptionally provides fallback value


// ── handle — called for BOTH success and failure ──────────────────
CompletableFuture<Response> handled = CompletableFuture
.supplyAsync(() -> callExternalService(), pool)
.handle((result, ex) -> {             // BiFunction<T, Throwable, U>
// result is non-null on SUCCESS
// ex is non-null on FAILURE
// Exactly one of them is non-null

        if (ex != null) {
            log.error("Service call failed: {}", ex.getMessage());
            return Response.error(503, "Service temporarily unavailable");
        }
        return Response.success(result);
    });
// Good for: transforming result AND handling errors in one place
// Good for: always returning a Response regardless of success/failure


// ── whenComplete — observe without transforming ───────────────────
CompletableFuture<String> observed = CompletableFuture
.supplyAsync(() -> processOrder(order), pool)
.whenComplete((result, ex) -> {
// ALWAYS called — success or failure
// Cannot change the result — just observe

        if (ex != null) {
            metrics.increment("orders.failed");
            alertOpsTeam("Order processing failed: " + ex.getMessage());
        } else {
            metrics.increment("orders.processed");
            auditLog.record("Order processed: " + result);
        }
        // Return value IGNORED — original result/exception passes through
    });
// Good for: logging, metrics, audit — side effects without changing flow


// ── Comparison table ──────────────────────────────────────────────
// Method         Called when    Can change result?  Returns
// exceptionally  Failure only   YES (T)             CF<T>
// handle         Always         YES (U, can be ≠T)  CF<U>
// whenComplete   Always         NO                  CF<T> (same)


// ── Chaining error handlers ───────────────────────────────────────
CompletableFuture<String> resilient = CompletableFuture
.supplyAsync(() -> fetchFromPrimary(), ioPool)

    // First fallback: try secondary
    .exceptionally(ex -> {
        log.warn("Primary failed, trying secondary");
        return fetchFromSecondary();        // Synchronous fallback
    })

    // Transform result (regardless of which source it came from)
    .thenApply(data -> enrichData(data))

    // Final safety net: if EVERYTHING failed, return empty
    .exceptionally(ex -> {
        log.error("All sources failed!", ex);
        return "";                          // Empty string as last resort
    })

    // Always log final outcome
    .whenComplete((result, ex) ->
        log.info("Final result: {}", result != null ? result : "ERROR")
    );
Async Fallback — The Right Pattern
java// Problem: exceptionally() is synchronous — the fallback runs on the completing thread
// If your fallback is also async (another DB call), you need handle + thenCompose

// ❌ Wrong: synchronous fallback inside exceptionally
.exceptionally(ex ->
fetchFromFallbackDB()  // This BLOCKS the completing thread if it's slow!
)

// ✅ Correct: async fallback via handle + thenCompose
CompletableFuture<User> asyncFallback = CompletableFuture
.supplyAsync(() -> fetchFromPrimary("u1"), pool)

    .handle((result, ex) -> {
        if (ex != null) {
            // Return a new CompletableFuture as the fallback
            return CompletableFuture.supplyAsync(
                () -> fetchFromFallback("u1"), pool  // Async fallback
            );
        }
        // Success: wrap result in completed future for uniform type
        return CompletableFuture.completedFuture(result);
    })

    // Flatten CF<CF<User>> → CF<User>
    .thenCompose(cf -> cf);

// Cleaner with Java 12+ exceptionallyCompose:
CompletableFuture<User> cleaner = CompletableFuture
.supplyAsync(() -> fetchFromPrimary("u1"), pool)
.exceptionallyCompose(ex ->                       // Java 12+
CompletableFuture.supplyAsync(() -> fetchFromFallback("u1"), pool)
);

Part 5: Timeouts — Never Wait Forever
java// Java 9+ added two timeout methods

// ── orTimeout — complete exceptionally after timeout ──────────────
CompletableFuture<String> withTimeout = CompletableFuture
.supplyAsync(() -> slowDatabaseCall(), pool)
.orTimeout(3, TimeUnit.SECONDS);
// If not done in 3 seconds: completes with TimeoutException
// Can chain exceptionally to handle it:

withTimeout.exceptionally(ex -> {
if (ex instanceof TimeoutException) {
return "DEFAULT_VALUE";    // Return default on timeout
}
throw new RuntimeException(ex);  // Re-throw other exceptions
});


// ── completeOnTimeout — complete with VALUE after timeout ─────────
CompletableFuture<String> withDefault = CompletableFuture
.supplyAsync(() -> slowDatabaseCall(), pool)
.completeOnTimeout("cached-fallback", 3, TimeUnit.SECONDS);
// If not done in 3 seconds: completes SUCCESSFULLY with "cached-fallback"
// No exception — cleaner when you have a good default


// ── Manual timeout (Java 8 compatible) ───────────────────────────
static <T> CompletableFuture<T> withTimeout(
CompletableFuture<T> future,
long timeout, TimeUnit unit) {

    CompletableFuture<T> timeoutFuture = new CompletableFuture<>();

    ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    scheduler.schedule(
        () -> timeoutFuture.completeExceptionally(
            new TimeoutException("Operation timed out after " + timeout + " " + unit)
        ),
        timeout, unit
    );

    // Whichever completes first (real result or timeout) wins
    return future.applyToEither(timeoutFuture, t -> t);
}

// Usage:
CompletableFuture<User> timedUser = withTimeout(
supplyAsync(() -> fetchUser("u1"), pool),
2, TimeUnit.SECONDS
);


// ── Production timeout pattern ─────────────────────────────────────
CompletableFuture<Response> productionCall(String userId) {
return CompletableFuture
.supplyAsync(() -> expensiveOperation(userId), pool)

        // Hard timeout: fail fast if too slow
        .orTimeout(5, TimeUnit.SECONDS)

        // Handle timeout specifically
        .exceptionally(ex -> {
            if (ex.getCause() instanceof TimeoutException) {
                metrics.increment("api.timeouts");
                log.warn("Operation timed out for user: {}", userId);
                return Response.timeout();
            }
            metrics.increment("api.errors");
            log.error("Operation failed for user: {}", userId, ex.getCause());
            return Response.error(ex.getCause().getMessage());
        })

        // Always record latency
        .whenComplete((resp, ex) ->
            metrics.timer("api.duration").record(/* duration */));
}

Part 6: The FAANG Microservice Aggregation Pattern
This is the most commonly asked CompletableFuture design scenario at FAANG.
java// Scenario: Product detail page
// Needs data from 5 independent microservices
// Sequential: 5 × 100ms = 500ms latency
// Parallel:   max(100ms) = 100ms latency  → 5× improvement

class ProductPageService {

    private final ExecutorService ioPool =
        new ThreadPoolExecutor(20, 50, 60, TimeUnit.SECONDS,
            new ArrayBlockingQueue<>(500), new NamedThreadFactory("product-io"));

    // ── Simple version: all parallel, fail fast ────────────────────
    public ProductPage buildPage(String productId) throws Exception {

        // All 5 calls start SIMULTANEOUSLY
        CompletableFuture<Product>       productFuture   =
            supplyAsync(() -> productSvc.get(productId),    ioPool);

        CompletableFuture<List<Review>>  reviewsFuture   =
            supplyAsync(() -> reviewSvc.list(productId),    ioPool);

        CompletableFuture<Inventory>     inventoryFuture =
            supplyAsync(() -> inventorySvc.get(productId),  ioPool);

        CompletableFuture<List<Product>> relatedFuture   =
            supplyAsync(() -> relatedSvc.list(productId),   ioPool);

        CompletableFuture<PricingInfo>   pricingFuture   =
            supplyAsync(() -> pricingSvc.get(productId),    ioPool);

        // Wait for ALL — fail if ANY fails
        return CompletableFuture
            .allOf(productFuture, reviewsFuture, inventoryFuture,
                   relatedFuture, pricingFuture)
            .thenApply(v -> ProductPage.builder()
                .product(productFuture.join())
                .reviews(reviewsFuture.join())
                .inventory(inventoryFuture.join())
                .related(relatedFuture.join())
                .pricing(pricingFuture.join())
                .build())
            .get(3, TimeUnit.SECONDS);  // Hard SLA timeout
    }


    // ── Production version: partial results on failure ─────────────
    public ProductPage buildPageResilient(String productId) throws Exception {

        // Core data (required — fail if unavailable)
        CompletableFuture<Product> productFuture = supplyAsync(
            () -> productSvc.get(productId), ioPool)
            .orTimeout(2, TimeUnit.SECONDS);

        // Enhanced data (optional — show page without these)
        CompletableFuture<List<Review>> reviewsFuture = supplyAsync(
            () -> reviewSvc.list(productId), ioPool)
            .orTimeout(1, TimeUnit.SECONDS)
            .exceptionally(ex -> {
                log.warn("Reviews unavailable for {}: {}", productId, ex.getMessage());
                return List.of();                // Empty list — page still shows
            });

        CompletableFuture<Inventory> inventoryFuture = supplyAsync(
            () -> inventorySvc.get(productId), ioPool)
            .orTimeout(1, TimeUnit.SECONDS)
            .exceptionally(ex -> Inventory.unknown());  // "Stock unknown" fallback

        CompletableFuture<List<Product>> relatedFuture = supplyAsync(
            () -> relatedSvc.list(productId), ioPool)
            .orTimeout(1, TimeUnit.SECONDS)
            .exceptionally(ex -> List.of());    // No related items — acceptable

        CompletableFuture<PricingInfo> pricingFuture = supplyAsync(
            () -> pricingSvc.get(productId), ioPool)
            .orTimeout(1, TimeUnit.SECONDS)
            .exceptionally(ex -> PricingInfo.fromProduct(productFuture.join()));
            // Fallback: use base price from product data

        // Product is required — propagate failure if it fails
        // Others are optional — already have fallbacks above
        return CompletableFuture
            .allOf(productFuture, reviewsFuture, inventoryFuture,
                   relatedFuture, pricingFuture)
            .thenApply(v -> ProductPage.builder()
                .product(productFuture.join())       // May throw if timed out
                .reviews(reviewsFuture.join())       // Always succeeds (has fallback)
                .inventory(inventoryFuture.join())   // Always succeeds (has fallback)
                .related(relatedFuture.join())       // Always succeeds (has fallback)
                .pricing(pricingFuture.join())       // Always succeeds (has fallback)
                .build())
            .get(3, TimeUnit.SECONDS);
    }


    // ── Advanced: dependent + independent combined ─────────────────
    public ProductPage buildPageWithDependencies(String productId, String userId) {

        // Independent: product + user (no dependency between them)
        CompletableFuture<Product> productFuture =
            supplyAsync(() -> productSvc.get(productId), ioPool);

        CompletableFuture<User> userFuture =
            supplyAsync(() -> userSvc.get(userId), ioPool);

        // Dependent: personalizedPrice NEEDS both product AND user
        CompletableFuture<Price> personalizedPriceFuture =
            productFuture.thenCombine(
                userFuture,
                (product, user) -> pricingSvc.getPersonalized(product, user)
                // thenCombine: runs when BOTH product and user are ready
            );

        // Dependent: recommendations NEED user (sequential on user)
        CompletableFuture<List<Product>> recommendationsFuture =
            userFuture.thenCompose(
                user -> supplyAsync(() -> recSvc.getFor(user, productId), ioPool)
                // thenCompose: async step after user is fetched
            );

        // Independent: reviews (no dependency on product or user)
        CompletableFuture<List<Review>> reviewsFuture =
            supplyAsync(() -> reviewSvc.list(productId), ioPool);

        // Wait for all required data
        return CompletableFuture
            .allOf(personalizedPriceFuture, recommendationsFuture, reviewsFuture)
            .thenApply(v -> ProductPage.builder()
                .product(productFuture.join())
                .price(personalizedPriceFuture.join())
                .recommendations(recommendationsFuture.join())
                .reviews(reviewsFuture.join())
                .build())
            .orTimeout(3, TimeUnit.SECONDS)
            .exceptionally(ex -> ProductPage.degraded(productId))
            .join();
    }
}

Part 7: The Execution Thread Model
java// Critical: which thread runs each stage?
// This is where most CompletableFuture bugs come from

class ThreadModel {

    // ── thenApply — runs on completing thread ────────────────────
    CompletableFuture.supplyAsync(() -> fetchData(), pool)
        .thenApply(data -> transform(data));
        // transform() runs on the thread that completed fetchData()
        // Usually: pool's worker thread
        // If already done when thenApply is called: runs on CALLING thread!

    // ── thenApplyAsync — runs on executor thread ─────────────────
    CompletableFuture.supplyAsync(() -> fetchData(), pool)
        .thenApplyAsync(data -> transform(data), cpuPool);
        // ALWAYS runs on cpuPool — no ambiguity

    // ── The gotcha: already-completed futures ─────────────────────
    CompletableFuture<String> alreadyDone = CompletableFuture.completedFuture("result");

    alreadyDone.thenApply(s -> {
        System.out.println(Thread.currentThread().getName());
        return s.toUpperCase();
    });
    // Prints: "main" — runs on current thread, synchronously!
    // Because: future is already done → no async needed → runs inline

    // This can cause subtle issues if you expect async execution
    // Fix: always use thenApplyAsync with an explicit executor
    // when you need guaranteed async execution

    // ── Thread pool confusion — the commonPool trap ───────────────
    CompletableFuture.supplyAsync(() -> fetchFromDB())
        // ⚠️ No executor → uses ForkJoinPool.commonPool()
        .thenApply(data -> process(data));
        // ⚠️ No executor → ALSO uses commonPool (or completing thread)
        // Multiple CF chains all sharing commonPool → starvation

    // ✅ Always explicit executors in production
    CompletableFuture.supplyAsync(() -> fetchFromDB(), ioPool)
        .thenApplyAsync(data -> process(data), cpuPool)
        .thenAcceptAsync(result -> store(result), ioPool);
    // Each stage on appropriate pool — I/O on ioPool, CPU on cpuPool
}

Part 8: join() vs get() — And When to Use Neither
java// Both block the calling thread until the future completes

// ── get() — checked exceptions ────────────────────────────────────
try {
String result = future.get();
String timed  = future.get(5, TimeUnit.SECONDS);
} catch (InterruptedException e) {
Thread.currentThread().interrupt();   // Restore interrupt status!
throw new RuntimeException(e);
} catch (ExecutionException e) {
throw new RuntimeException(e.getCause()); // Unwrap real exception
} catch (TimeoutException e) {
future.cancel(true);
throw new RuntimeException("Timed out", e);
}
// Verbose but explicit — forces you to handle all failure modes


// ── join() — unchecked exceptions ────────────────────────────────
String result = future.join();
// Throws CompletionException (unchecked) wrapping the real cause
// No checked exception boilerplate
// Use inside lambda/stream where checked exceptions are awkward:

List<String> results = futures.stream()
.map(CompletableFuture::join)    // ✅ Works in stream — unchecked
.collect(Collectors.toList());
// join() is safe here because: called AFTER allOf().get()
// All futures are done — join() returns immediately, no real blocking


// ── When to use each ──────────────────────────────────────────────
// get()  → top-level code where you handle all exceptions explicitly
// join() → inside thenApply/thenCompose after allOf() ensures completion
// NEVER → inside thenApply/thenCompose BEFORE you know it's done (deadlock!)


// ── The deadlock trap — blocking inside a CF stage ────────────────

// ❌ DEADLOCK: waiting for a future inside another future's stage
CompletableFuture<String> outer = CompletableFuture
.supplyAsync(() -> {
CompletableFuture<String> inner = supplyAsync(() -> fetchData(), pool);

        return inner.join();  // ❌ DEADLOCK if pool has only 1 thread!
        // outer task is using pool's only thread
        // inner task is queued in same pool — can never run
        // outer.join() waits for inner — inner waits for a thread — DEADLOCK
    }, pool);

// ✅ FIX: use thenCompose instead — no blocking
CompletableFuture<String> correct = CompletableFuture
.supplyAsync(() -> prepareRequest(), pool)
.thenCompose(req ->
supplyAsync(() -> fetchData(req), pool)  // No blocking!
);

Part 9: Real Patterns Used at FAANG
Pattern 1: Scatter-Gather
java// Send request to N workers, gather ALL results

class ScatterGather {

    // Scatter: fan out to all workers
    public List<Result> scatterGather(List<String> workerUrls, Request request) {

        // Scatter — launch all simultaneously
        List<CompletableFuture<Result>> futures = workerUrls.stream()
            .map(url -> CompletableFuture.supplyAsync(
                () -> callWorker(url, request),
                ioPool
            ).orTimeout(5, TimeUnit.SECONDS)
             .exceptionally(ex -> Result.failed(url, ex.getMessage())))
            .collect(Collectors.toList());

        // Gather — collect all results
        return CompletableFuture
            .allOf(futures.toArray(new CompletableFuture[0]))
            .thenApply(v -> futures.stream()
                .map(CompletableFuture::join)
                .collect(Collectors.toList()))
            .join();
    }
}
Pattern 2: Circuit Breaker with CompletableFuture
java// Automatically stop calling a failing service
// Open circuit → return fallback immediately without calling service

class CircuitBreakerCF {
private final AtomicInteger  failureCount = new AtomicInteger(0);
private volatile boolean     open         = false;
private volatile long        openedAt     = 0;
private static final int     THRESHOLD    = 5;     // 5 failures → open
private static final long    TIMEOUT_MS   = 30000; // 30s before retry

    public CompletableFuture<String> call(Supplier<String> operation) {
        // Circuit is open — fail fast
        if (open) {
            if (System.currentTimeMillis() - openedAt > TIMEOUT_MS) {
                open = false;  // Try again (half-open)
                log.info("Circuit half-open — trying again");
            } else {
                return CompletableFuture.failedFuture(
                    new CircuitOpenException("Circuit breaker is open")
                );
            }
        }

        return CompletableFuture
            .supplyAsync(operation, ioPool)
            .whenComplete((result, ex) -> {
                if (ex != null) {
                    int failures = failureCount.incrementAndGet();
                    if (failures >= THRESHOLD) {
                        open     = true;
                        openedAt = System.currentTimeMillis();
                        log.error("Circuit OPENED after {} failures", failures);
                    }
                } else {
                    failureCount.set(0);  // Reset on success
                }
            });
    }
}
Pattern 3: Retry with Backoff
java// Retry failed async operation with exponential backoff

class RetryableFuture {
private final ScheduledExecutorService scheduler =
Executors.newSingleThreadScheduledExecutor();

    public <T> CompletableFuture<T> withRetry(
            Supplier<CompletableFuture<T>> operation,
            int maxRetries,
            long initialDelayMs) {

        CompletableFuture<T> result = new CompletableFuture<>();
        attempt(operation, maxRetries, initialDelayMs, result, 0);
        return result;
    }

    private <T> void attempt(
            Supplier<CompletableFuture<T>> operation,
            int maxRetries,
            long delayMs,
            CompletableFuture<T> result,
            int attemptNumber) {

        operation.get().whenComplete((value, ex) -> {
            if (ex == null) {
                result.complete(value);     // Success!
                return;
            }

            if (attemptNumber >= maxRetries) {
                result.completeExceptionally(
                    new RetryExhaustedException(
                        "Failed after " + maxRetries + " attempts", ex)
                );
                return;
            }

            // Calculate backoff: 100ms → 200ms → 400ms → 800ms...
            long nextDelay = delayMs * (long)Math.pow(2, attemptNumber);
            long jitter    = (long)(Math.random() * nextDelay * 0.1); // 10% jitter

            log.warn("Attempt {} failed, retrying in {}ms: {}",
                attemptNumber + 1, nextDelay + jitter, ex.getMessage());

            // Schedule retry after backoff
            scheduler.schedule(
                () -> attempt(operation, maxRetries, delayMs, result, attemptNumber + 1),
                nextDelay + jitter,
                TimeUnit.MILLISECONDS
            );
        });
    }
}

// Usage:
RetryableFuture retrier = new RetryableFuture();
CompletableFuture<User> user = retrier.withRetry(
() -> supplyAsync(() -> userService.fetch("u1"), pool),
3,    // max 3 retries
100   // start with 100ms delay
);
Pattern 4: Bulkhead — Isolate Failure Domains
java// Different services get different pools — one overloaded service
// doesn't starve threads for other services

class BulkheadPattern {
// Each service has its own isolated thread pool
private final ExecutorService userServicePool =
new ThreadPoolExecutor(5, 10, 60, TimeUnit.SECONDS,
new ArrayBlockingQueue<>(50), new NamedThreadFactory("user-svc"),
new ThreadPoolExecutor.AbortPolicy());  // Fail fast if user-svc overwhelmed

    private final ExecutorService orderServicePool =
        new ThreadPoolExecutor(5, 20, 60, TimeUnit.SECONDS,
            new ArrayBlockingQueue<>(200), new NamedThreadFactory("order-svc"),
            new ThreadPoolExecutor.CallerRunsPolicy());

    private final ExecutorService paymentServicePool =
        new ThreadPoolExecutor(3, 5, 60, TimeUnit.SECONDS,
            new ArrayBlockingQueue<>(20), new NamedThreadFactory("payment-svc"),
            new ThreadPoolExecutor.AbortPolicy());  // Payments: strict limit

    public CompletableFuture<OrderSummary> getOrderSummary(String userId) {
        // Each call isolated — payment pool being overwhelmed
        // doesn't affect user service calls
        CompletableFuture<User>  userFuture  = supplyAsync(
            () -> userService.get(userId),    userServicePool);

        CompletableFuture<List<Order>> ordersFuture = supplyAsync(
            () -> orderService.list(userId),  orderServicePool);

        return userFuture.thenCombine(
            ordersFuture,
            (user, orders) -> new OrderSummary(user, orders)
        );
    }
}

Part 10: The Interview Q&A Round 🎤

Q1. What is the difference between thenApply, thenCompose, and thenCombine?

"thenApply is a synchronous map — it transforms the result of one future into another value. The function takes T and returns R (not a future). Like Stream.map().
thenCompose is async flatMap — used when the transformation function itself returns a CompletableFuture<R>. Without thenCompose you'd get CompletableFuture<CompletableFuture<R>> — nested and useless. thenCompose flattens it to CompletableFuture<R>. Use it for sequential dependent async steps: 'get user ID, THEN fetch user profile by that ID.'
thenCombine merges TWO independent futures when both complete. Both start simultaneously, the BiFunction is called only when BOTH are done. Use it for parallel independent calls: 'fetch user AND fetch orders simultaneously, combine when both arrive.'
Decision rule: one future, sync transform → thenApply. One future, next step is also async → thenCompose. Two independent futures, merge results → thenCombine."


Q2. What is the difference between exceptionally, handle, and whenComplete?
java// exceptionally — ONLY on failure, CAN change result to fallback value
cf.exceptionally(ex -> fallback);
// Called: failure only
// Can change: YES (to same type T)
// Returns:    CF<T>
// Use when:   you have a meaningful fallback value

// handle — ALWAYS called, CAN change result (to any type U)
cf.handle((result, ex) -> {
if (ex != null) return errorResponse;
return successResponse;
});
// Called: always (both success and failure)
// Can change: YES (can return different type U)
// Returns:    CF<U>
// Use when:   you want one place to handle both paths
//             or need to transform result AND handle errors

// whenComplete — ALWAYS called, CANNOT change result
cf.whenComplete((result, ex) -> logMetrics(result, ex));
// Called: always
// Can change: NO (result passes through unchanged)
// Returns:    CF<T> (same)
// Use when:   side effects only — logging, metrics, audit
//             original result/exception must be preserved

Q3. What happens to other futures when anyOf completes?

"CompletableFuture.anyOf() completes as soon as the FIRST future completes — success or failure. But critically, it does NOT cancel the other futures. They continue running to completion in their thread pools, using resources, but their results are simply ignored by the anyOf future.
This is a resource concern in high-throughput systems. If you do anyOf on 10 slow futures and one fast one completes, the other 9 still run. For true cancellation you need to track each future individually and call cancel(true) on the losers when the winner is known. The type-safe anyOf utility I implement manually handles this by using whenComplete on each future — first success wins, and I can cancel the rest.
This is why invokeAny() on ExecutorService is often preferable for same-type tasks — it handles cancellation of the losers automatically."


Q4. How do you run 100 async tasks but limit concurrency to 10 at a time?
java// Problem: 100 tasks, but only 10 run simultaneously
// (rate-limiting external API, protecting a downstream service)

// ── Solution 1: Semaphore (simplest) ──────────────────────────────
Semaphore semaphore = new Semaphore(10);   // Max 10 concurrent

List<CompletableFuture<Result>> futures = tasks.stream()
.map(task -> CompletableFuture.supplyAsync(() -> {
try {
semaphore.acquire();            // Block if 10 already running
try {
return processTask(task);
} finally {
semaphore.release();        // ALWAYS release in finally
}
} catch (InterruptedException e) {
Thread.currentThread().interrupt();
throw new RuntimeException(e);
}
}, pool))
.collect(Collectors.toList());

// ── Solution 2: Batching with allOf ───────────────────────────────
int batchSize = 10;
List<List<Task>> batches = partition(tasks, batchSize);

CompletableFuture<Void> chain = CompletableFuture.completedFuture(null);
List<Result> allResults = new ArrayList<>();

for (List<Task> batch : batches) {
chain = chain.thenCompose(v -> {
// Start all tasks in this batch simultaneously
List<CompletableFuture<Result>> batchFutures = batch.stream()
.map(t -> supplyAsync(() -> processTask(t), pool))
.collect(Collectors.toList());

        // Wait for entire batch before starting next batch
        return CompletableFuture.allOf(
                batchFutures.toArray(new CompletableFuture[0]))
            .thenApply(ignore -> {
                batchFutures.forEach(f -> allResults.add(f.join()));
                return null;
            });
    });
}

chain.get();  // Wait for all batches

Q5. You have a method that takes 500ms. 10 threads call it. What's the total time?
java// This question probes: sequential vs parallel understanding

// Scenario:
CompletableFuture<String> slowMethod() {
return supplyAsync(() -> {
Thread.sleep(500);     // Simulates 500ms work
return "result";
}, pool);
}

// ── Sequential calls (naive) ──────────────────────────────────────
// 10 calls, each waits for previous
long start = System.currentTimeMillis();
for (int i = 0; i < 10; i++) {
slowMethod().get();        // Blocks for 500ms each
}
// Total: ~5000ms  (10 × 500ms)


// ── Parallel calls with allOf ─────────────────────────────────────
long start2 = System.currentTimeMillis();
List<CompletableFuture<String>> futures = IntStream.range(0, 10)
.mapToObj(i -> slowMethod())
.collect(Collectors.toList());

CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).get();
// Total: ~500ms  (all 10 run simultaneously)
// 10× faster! (assuming pool has ≥10 threads)


// The follow-up FAANG question:
// "What if the pool only has 4 threads?"
// → At most 4 run simultaneously
// → 10 tasks on 4 threads: ceil(10/4) = 3 waves
// → Wave 1: 4 tasks = 500ms
// → Wave 2: 4 tasks = 500ms
// → Wave 3: 2 tasks = 500ms
// → Total: ~1500ms
// Lesson: pool size is the REAL constraint on parallelism
```

---

## Section 6 Master Summary 🧠
```
CREATION:
supplyAsync(supplier, executor)  → async with result
runAsync(runnable, executor)     → async, void
completedFuture(value)           → already done
new CompletableFuture<>()        → manual completion (bridge pattern)
⚠️ Always provide your OWN executor — never rely on commonPool

TRANSFORMATION:
thenApply(fn)       → sync map:     T → R
thenApplyAsync(fn)  → async map:    T → R on new thread
thenCompose(fn)     → flatMap:      T → CF<R>  (fn returns CF)
thenAccept(fn)      → consume:      T → void
thenRun(fn)         → side effect:  () → void, no result access

COMBINING:
thenCombine(other, fn)  → two parallel CFs → merge when BOTH done
allOf(cf1, cf2, ...)    → wait for ALL, returns CF<Void> (collect manually)
anyOf(cf1, cf2, ...)    → first to complete wins, returns CF<Object>

ERROR HANDLING:
exceptionally(fn)   → failure only, same type T
handle(fn)          → always, can change type
whenComplete(fn)    → always, side effect, cannot change result

TIMEOUTS:
orTimeout(n, unit)           → TimeoutException after n units
completeOnTimeout(val, n, u) → complete with value after n units

KEY RULES:
thenApply vs thenCompose:
fn returns T?      → thenApply
fn returns CF<T>?  → thenCompose  (avoids CF<CF<T>>)

allOf vs anyOf:
need ALL results?  → allOf + manual collect with join()
need FIRST result? → anyOf (or type-safe custom version)

join() vs get():
get()  → checked exceptions, top-level code
join() → unchecked, inside lambdas/streams after allOf

Thread assignment:
thenApply      → completing thread (may be calling thread if done!)
thenApplyAsync → always on executor thread (predictable)

PRODUCTION PATTERNS:
Microservice aggregation → allOf + parallel supplyAsync per service
Fallback on failure      → exceptionally or handle
Async fallback           → handle + thenCompose / exceptionallyCompose
Retry with backoff       → recursive withRetry + ScheduledExecutor
Scatter-gather           → allOf with per-future error handling
Circuit breaker          → failure counter + open/half-open states
Bulkhead                 → separate ExecutorService per service

COMMON MISTAKES:
❌ Using commonPool (no explicit executor)
❌ join()/get() inside thenApply → deadlock if same pool
❌ Ignoring CF return from exceptionally → errors silently lost
❌ Not handling ExecutionException → getCause() for real error
❌ Forgetting allOf returns Void → must collect from original CFs
❌ Assuming anyOf cancels losers → it does NOT