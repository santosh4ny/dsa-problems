Section 10: Exception Handling ⚠️
Sections 8-9 covered what the JVM does invisibly. Section 10 is about what happens when things go wrong visibly. Exception handling looks simple — try/catch/finally — but FAANG interviewers use it to probe whether you understand JVM mechanics, API design, performance implications, and production failure patterns. Most candidates know the syntax. Few know the depth.

The Big Picture — Java's Exception Architecture
Throwable
├── Error                          ← JVM-level failures, don't catch
│   ├── OutOfMemoryError
│   ├── StackOverflowError
│   ├── VirtualMachineError
│   └── AssertionError
│
└── Exception                      ← Application-level failures
├── RuntimeException           ← UNCHECKED: compiler doesn't enforce
│   ├── NullPointerException
│   ├── IllegalArgumentException
│   ├── IllegalStateException
│   ├── IndexOutOfBoundsException
│   ├── ClassCastException
│   ├── UnsupportedOperationException
│   ├── ConcurrentModificationException
│   └── ArithmeticException
│
└── (all others)               ← CHECKED: compiler enforces
├── IOException
├── SQLException
├── ClassNotFoundException
├── InterruptedException
└── ParseException

THE FUNDAMENTAL RULE:
Checked   → caller MUST handle or declare (throws)
Unchecked → caller MAY handle but isn't forced to
Error     → don't catch (JVM is broken, can't recover)

Part 1: Checked vs Unchecked — The Design Philosophy
java// ── Checked exceptions: EXPECTED failures (recoverable) ───────────
// The method is saying: "This WILL fail sometimes, and you MUST plan for it"
// Compiler enforces: caller must catch or declare throws

public String readConfig(String path) throws IOException {
// IOException: expected — file may not exist, may lack permissions
// Caller MUST decide: handle it? propagate it?
return Files.readString(Path.of(path));
}

// Caller has no choice:
try {
String config = readConfig("/etc/app.conf");
} catch (IOException e) {
// MUST handle or re-declare 'throws IOException'
useDefaultConfig();
}


// ── Unchecked exceptions: PROGRAMMING ERRORS (bugs) ───────────────
// The method is saying: "This should never happen if you use the API correctly"
// Compiler doesn't enforce: caller decides whether to handle

public User getUser(String userId) {
if (userId == null) {
throw new NullPointerException("userId must not be null");
// Or better: throw new IllegalArgumentException("userId must not be null");
}
return userRepository.find(userId);
}

// Caller can handle but doesn't HAVE to:
User user = getUser(null);   // NPE — this is a bug, not expected behavior
// No try-catch required by compiler


// ── The design debate: when to use which ──────────────────────────

// CHECKED when:
// → Failure is expected and RECOVERABLE (retry, fallback, user feedback)
// → Caller has meaningful options (read from cache if DB fails)
// → External factors cause failure (file missing, network down)

// UNCHECKED when:
// → Failure indicates a BUG in the caller (null parameter, bad state)
// → No reasonable recovery path (just fix the code)
// → Framework/library code: forces every caller to add boilerplate
//   even if they can't do anything with the exception

// The modern industry trend: UNCHECKED for most things
// Rationale:
// → Checked exceptions pollute APIs with throws declarations everywhere
// → Lambda bodies can't throw checked exceptions (functional style breaks)
// → Exception handling often just wraps and rethrows anyway
// → Many frameworks (Spring, Hibernate) wrap all checked in unchecked

// ❌ Checked exceptions break lambdas:
List<String> paths = List.of("/a", "/b", "/c");

// This doesn't compile — forEach lambda can't throw IOException
paths.forEach(path -> {
String content = Files.readString(Path.of(path));  // ← IOException: checked!
process(content);
});

// Ugly workaround: wrap in unchecked
paths.forEach(path -> {
try {
process(Files.readString(Path.of(path)));
} catch (IOException e) {
throw new UncheckedIOException(e);  // Wrap in unchecked
}
});

// Clean utility:
@FunctionalInterface
interface ThrowingFunction<T, R> {
R apply(T t) throws Exception;

    static <T, R> Function<T, R> wrap(ThrowingFunction<T, R> f) {
        return t -> {
            try {
                return f.apply(t);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        };
    }
}

// Now works cleanly:
paths.stream()
.map(ThrowingFunction.wrap(p -> Files.readString(Path.of(p))))
.forEach(this::process);

Part 2: try-with-resources — Internals and Gotchas
What the Compiler Actually Generates
java// YOUR CODE:
try (InputStream is  = new FileInputStream("input.txt");
OutputStream os = new FileOutputStream("output.txt")) {
copy(is, os);
}

// WHAT COMPILER GENERATES (simplified):
InputStream is   = new FileInputStream("input.txt");    // Opened first
OutputStream os  = null;
Throwable primaryException = null;

try {
os = new FileOutputStream("output.txt");
copy(is, os);

} catch (Throwable t) {
primaryException = t;
throw t;

} finally {
// CLOSE IN REVERSE ORDER (os first, then is)
if (os != null) {
if (primaryException != null) {
try {
os.close();
} catch (Throwable suppressedEx) {
// SUPPRESSED: don't lose primary exception
primaryException.addSuppressed(suppressedEx);
}
} else {
os.close();  // No primary exception — let close() throw freely
}
}

    if (is != null) {
        if (primaryException != null) {
            try {
                is.close();
            } catch (Throwable suppressedEx) {
                primaryException.addSuppressed(suppressedEx);
            }
        } else {
            is.close();
        }
    }
}

// KEY BEHAVIORS:
// 1. Resources closed in REVERSE DECLARATION ORDER (os before is)
// 2. If body throws AND close() throws: body exception is PRIMARY
//    close() exception is SUPPRESSED (attached to primary, not lost)
// 3. If body succeeds AND close() throws: close() exception propagates
// 4. Resources in initializer: if FileInputStream succeeds but
//    FileOutputStream fails: FileInputStream is NOT yet in the
//    try-with-resources scope → manually need to handle!
Suppressed Exceptions — The Hidden Feature
java// Pre-Java 7 problem: exception in finally REPLACES body exception
// The REAL cause (body exception) is silently lost!

// ❌ Classic Java 6 bug:
InputStream is = null;
try {
is = openStream();
processStream(is);          // Throws IOException (the REAL problem)
} finally {
if (is != null) {
is.close();             // ALSO throws IOException
// This finally exception REPLACES the body exception!
// The original "processStream" exception is GONE
// We see "close() failed" not "processStream() failed"
// Root cause is invisible!
}
}

// ✅ Java 7+ try-with-resources solves this:
try (InputStream is = openStream()) {
processStream(is);          // Throws IOException: "disk read failed"
// close() throws: "connection reset"
}
// Propagates: "disk read failed"
// Attached as suppressed: "connection reset"

// Read suppressed exceptions:
try {
try (InputStream is = openStream()) {
processStream(is);
}
} catch (IOException primary) {
log.error("Primary failure: {}", primary.getMessage());

    for (Throwable suppressed : primary.getSuppressed()) {
        log.error("Suppressed (during close): {}", suppressed.getMessage());
    }
    // Now you see BOTH problems, with correct priority
}

// Manually adding suppressed exceptions (useful in custom resource classes)
Exception primary = new Exception("Main failure");
Exception cleanup = new Exception("Cleanup failure");
primary.addSuppressed(cleanup);
throw primary;
// Caller gets primary, can access cleanup via getSuppressed()


// ── Custom AutoCloseable ──────────────────────────────────────────
class ManagedConnection implements AutoCloseable {
private final Connection     connection;
private final String         connectionId;
private       boolean        closed = false;

    ManagedConnection(String url) throws SQLException {
        this.connection   = DriverManager.getConnection(url);
        this.connectionId = UUID.randomUUID().toString();
        log.debug("Connection opened: {}", connectionId);
    }

    public ResultSet query(String sql) throws SQLException {
        checkOpen();
        return connection.createStatement().executeQuery(sql);
    }

    private void checkOpen() {
        if (closed) throw new IllegalStateException(
            "Connection " + connectionId + " is already closed");
    }

    @Override
    public void close() throws SQLException {
        if (!closed) {
            closed = true;
            connection.close();
            log.debug("Connection closed: {}", connectionId);
        }
        // Idempotent close: calling close() twice is safe
        // This is the CONTRACT of AutoCloseable.close()
    }
}

// Usage:
try (ManagedConnection conn = new ManagedConnection(DB_URL)) {
ResultSet rs = conn.query("SELECT * FROM users");
processResults(rs);
}   // conn.close() guaranteed — even if processResults throws

Part 3: Exception Chaining — Preserving Root Cause
java// ALWAYS preserve the original exception when wrapping
// Without chaining: root cause is INVISIBLE — impossible to debug

// ── Why chaining matters ──────────────────────────────────────────
// ❌ Swallowed cause — loses debugging information
void loadConfig(String path) {
try {
Files.readString(Path.of(path));
} catch (IOException e) {
throw new ConfigException("Failed to load config");
// The original IOException is GONE
// Stack trace shows: ConfigException, but why? What was wrong with the file?
// Impossible to diagnose in production without the cause
}
}

// ✅ Chained — preserves full diagnostic chain
void loadConfigSafe(String path) {
try {
Files.readString(Path.of(path));
} catch (IOException e) {
throw new ConfigException("Failed to load config: " + path, e);  // ← cause!
}
}

// Reading the chain:
try {
loadConfigSafe("/etc/app.conf");
} catch (ConfigException e) {
log.error("Config failed", e);
// Logs full chain:
// ConfigException: Failed to load config: /etc/app.conf
//   at ConfigLoader.loadConfigSafe(...)
// Caused by: java.io.IOException: No such file or directory
//   at java.nio.file.Files.readString(...)
// "Caused by" tells you the ROOT CAUSE — actionable!

    // Programmatically navigate the chain:
    Throwable cause = e;
    while (cause.getCause() != null) {
        cause = cause.getCause();
    }
    // cause is now the ROOT exception (original failure)
}


// ── Exception constructors — know them all ────────────────────────
class AppException extends RuntimeException {
// All 4 forms — provide all in custom exceptions:
AppException(String message)                         { super(message); }
AppException(String message, Throwable cause)        { super(message, cause); }
AppException(Throwable cause)                        { super(cause); }
AppException(String msg, Throwable cause,
boolean enableSuppression,
boolean writableStackTrace)             { super(msg, cause,
enableSuppression,
writableStackTrace); }
// enableSuppression:   false = getSuppressed() always returns empty
// writableStackTrace:  false = no stack trace collected (FAST!)
//   Use writableStackTrace=false for: high-frequency expected exceptions
//   where stack trace is useless noise (e.g., flow-control exceptions)
}

// ── Fast exception for flow control ──────────────────────────────
// Some systems use exceptions for flow control (cache miss, auth reject)
// Problem: stack trace collection is EXPENSIVE (~microseconds)
// For HIGH-FREQUENCY expected exceptions: skip the stack trace

class CacheMissException extends RuntimeException {
static final CacheMissException INSTANCE = new CacheMissException();

    private CacheMissException() {
        super("Cache miss", null, true, false);  // No stack trace!
    }
}

// Usage: throw pre-allocated singleton — zero allocation, no stack walk
Object get(String key) {
Object value = cache.get(key);
if (value == null) throw CacheMissException.INSTANCE;  // Fast!
return value;
}
// ⚠️ ONLY for: truly high-frequency, truly expected, where stack trace adds nothing
// ⚠️ Makes debugging harder if misused — use sparingly

Part 4: The finally Block — Guaranteed Execution and Gotchas
java// finally runs in ALL cases:
// → Normal completion
// → Exception thrown
// → return statement in try
// → break/continue in try

// ── Gotcha 1: return in finally OVERRIDES return in try ────────────
int riskyReturn() {
try {
return 1;        // "Would" return 1...
} finally {
return 2;        // ...but finally OVERRIDES it → returns 2!
}
}
// System.out.println(riskyReturn()); → prints 2
// ❌ NEVER return from finally — it swallows exceptions too!

int exceptionSwallowed() {
try {
throw new RuntimeException("Real problem");   // Would propagate...
} finally {
return 42;       // ...but finally return SWALLOWS the exception!
}
}
// No exception! Returns 42. Exception silently vanished. Production nightmare.
// Rule: NEVER return from finally


// ── Gotcha 2: exception in finally replaces try exception ─────────
void exceptionInFinally() throws IOException {
try {
throw new RuntimeException("ORIGINAL cause");   // Exception A
} finally {
throw new IOException("finally problem");        // Exception B
// Exception B REPLACES Exception A!
// Original cause is lost!
}
}
// Caller catches IOException — has no idea RuntimeException was the root cause
// Use try-with-resources to avoid this (suppressed exceptions)


// ── Gotcha 3: finally still runs after System.exit() ── WRONG! ───
void finallyAndExit() {
try {
System.exit(0);   // JVM exits HERE
} finally {
System.out.println("Does this print?");  // NO! System.exit halts JVM
// Shutdown hooks run, but finally blocks do NOT
}
}
// Thread.stop() also skips finally — another reason it's deprecated


// ── finally for guaranteed cleanup ───────────────────────────────
class LockManager {
private final Lock lock = new ReentrantLock();

    void withLock(Runnable action) {
        lock.lock();
        try {
            action.run();          // May throw anything
        } finally {
            lock.unlock();         // ALWAYS releases, even on exception
        }
        // If action throws: lock released before exception propagates
        // If action returns: lock released normally
    }
}

// ── finally vs try-with-resources ────────────────────────────────
// ❌ Manual finally for resource cleanup:
InputStream is = null;
try {
is = new FileInputStream(file);
process(is);
} catch (IOException e) {
handleError(e);
} finally {
if (is != null) {
try { is.close(); } catch (IOException e) { /* suppress */ }
// ^ This suppression is WRONG — you're silently losing close errors
}
}

// ✅ try-with-resources (always prefer for AutoCloseable):
try (InputStream is = new FileInputStream(file)) {
process(is);
} catch (IOException e) {
handleError(e);
}
// Cleaner, correct suppression semantics, less code, harder to get wrong

Part 5: Multi-catch and Exception Handling Patterns
java// ── Multi-catch (Java 7+) — handle multiple exceptions the same way
try {
riskyOperation();
} catch (IOException | SQLException | ParseException e) {
// e is effectively final — can't reassign
log.error("Operation failed: {}", e.getMessage());
throw new ServiceException("Failed", e);
}
// Eliminates code duplication from:
// catch (IOException e) { handle(e); }
// catch (SQLException e) { handle(e); }  // same code!
// catch (ParseException e) { handle(e); }  // same code!


// ── Catch ordering — specific before general ──────────────────────
try {
operation();
} catch (FileNotFoundException e) {  // Specific first!
handleMissingFile(e);
} catch (IOException e) {            // General second (FileNotFoundException IS IOException)
handleIOError(e);
} catch (Exception e) {              // Most general last
handleUnexpected(e);
}
// ❌ Wrong order: catch (Exception e) first — FileNotFoundException never reached
// Compiler catches this: "Exception has already been caught"


// ── Re-throwing — when and how ────────────────────────────────────
// Pattern 1: Add context, preserve type
void loadUser(String id) throws UserLoadException {
try {
return db.query("SELECT * FROM users WHERE id = ?", id);
} catch (SQLException e) {
throw new UserLoadException("Failed to load user: " + id, e);
}
}

// Pattern 2: Re-throw same exception after logging
void process(String data) throws ProcessingException {
try {
return parser.parse(data);
} catch (ProcessingException e) {
log.error("Processing failed for data: {}", summarize(data), e);
throw e;  // Re-throw original (not wrapped) — type preserved
}
}

// Pattern 3: Unwrap checked to unchecked (crossing API boundary)
User findUser(String id) {
try {
return db.query(id);
} catch (SQLException e) {
// Don't want callers to handle SQLException —
// they shouldn't know about our DB layer
throw new RuntimeException("Database error finding user: " + id, e);
// Or better: throw new DataAccessException("...", e);
}
}

// ── catch (Exception e) — when it's acceptable ────────────────────
// ❌ Don't catch Exception and ignore:
try {
process();
} catch (Exception e) {
// Empty — NEVER DO THIS. Hides bugs completely.
}

// ❌ Don't catch Exception just to log:
try {
process();
} catch (Exception e) {
log.error("Error", e);
// Where does the exception go? What does the caller get? Nothing!
// Caller thinks it succeeded. It didn't.
}

// ✅ Acceptable: catch Exception to re-throw as specific typed exception
void executeTask(Task task) {
try {
task.run();
} catch (Exception e) {
// Convert any exception to our domain exception
// Gives callers a clean API, all exceptions handled uniformly
throw new TaskExecutionException("Task " + task.getId() + " failed", e);
}
}

// ✅ Acceptable: top-level handler in framework/server
// (catches anything that slipped through, logs it, returns 500)
@ExceptionHandler(Exception.class)
ResponseEntity<ErrorResponse> handleAll(Exception e, HttpServletRequest req) {
log.error("Unhandled exception for request {}: ", req.getRequestURI(), e);
return ResponseEntity.internalServerError()
.body(new ErrorResponse("An unexpected error occurred"));
}

Part 6: Custom Exception Design — FAANG Standards
java// ── Hierarchy design for a domain ────────────────────────────────
// Good exception hierarchy: one root + domain-specific subclasses

// Root: all application exceptions
public class AppException extends RuntimeException {
private final String errorCode;       // Machine-readable code
private final boolean retryable;      // Should client retry?

    public AppException(String errorCode, String message, boolean retryable) {
        super(message);
        this.errorCode = errorCode;
        this.retryable = retryable;
    }

    public AppException(String errorCode, String message,
                        boolean retryable, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.retryable = retryable;
    }

    public String getErrorCode()  { return errorCode; }
    public boolean isRetryable()  { return retryable; }
}

// Domain-specific subclasses
public class UserNotFoundException extends AppException {
public UserNotFoundException(String userId) {
super("USER_NOT_FOUND",
"User not found: " + userId,
false);   // Not retryable — user doesn't exist
}
}

public class ServiceUnavailableException extends AppException {
private final Duration retryAfter;

    public ServiceUnavailableException(String service, Duration retryAfter, Throwable cause) {
        super("SERVICE_UNAVAILABLE",
              "Service temporarily unavailable: " + service,
              true,     // Retryable!
              cause);
        this.retryAfter = retryAfter;
    }

    public Duration getRetryAfter() { return retryAfter; }
}

public class ValidationException extends AppException {
private final List<FieldError> fieldErrors;

    public ValidationException(List<FieldError> errors) {
        super("VALIDATION_FAILED",
              "Request validation failed: " + errors.size() + " error(s)",
              false);
        this.fieldErrors = List.copyOf(errors);
    }

    public List<FieldError> getFieldErrors() { return fieldErrors; }
}

// Usage: callers can catch specific OR general
try {
userService.getUser(userId);
} catch (UserNotFoundException e) {
return Response.notFound(e.getMessage());
} catch (ServiceUnavailableException e) {
return Response.serviceUnavailable()
.header("Retry-After", e.getRetryAfter().toString());
} catch (AppException e) {
// All other app exceptions
if (e.isRetryable()) {
enqueueForRetry(request);
}
return Response.error(e.getErrorCode(), e.getMessage());
}


// ── Exception naming conventions ─────────────────────────────────
// ✅ End with "Exception": UserNotFoundException, ValidationException
// ✅ Name describes WHAT WENT WRONG: OrderNotFoundException (not "BadOrder")
// ✅ Include relevant context in message: "Order not found: orderId=abc123"
// ❌ Too broad: ApplicationException (what went wrong?)
// ❌ Too vague: ProcessingException (processing of what?)


// ── What to put in the exception message ─────────────────────────
// ✅ Include the key that was looked up: "User not found: userId=" + userId
// ✅ Include the constraint violated: "Amount exceeds limit: requested=" + amount + ", limit=" + limit
// ✅ Include the operation that failed: "Failed to update order " + orderId
// ❌ Don't include sensitive data: passwords, SSNs, credit card numbers
// ❌ Don't include internal implementation details in user-visible messages

public class InsufficientFundsException extends AppException {
public InsufficientFundsException(long accountId, BigDecimal requested, BigDecimal available) {
super("INSUFFICIENT_FUNDS",
String.format("Insufficient funds for account %d: requested=%.2f, available=%.2f",
accountId, requested, available),
false);
// Message is diagnostic (internal) — map to user-friendly message at API layer
}
}

Part 7: Exception Anti-Patterns — What FAANG Code Reviews Reject
java// ── Anti-Pattern 1: Swallowing exceptions ─────────────────────────
// ❌ THE worst anti-pattern in Java
try {
importantOperation();
} catch (Exception e) {
// Nothing here. Exception silently disappears.
// importantOperation() failed. Caller doesn't know. System is in unknown state.
}

// Why it's catastrophic: system appears to work, actually broken
// Debugging: impossible — no evidence anything went wrong
// Fix: ALWAYS at minimum log; better: re-throw or handle meaningfully

// If you genuinely want to ignore (extremely rare):
try {
cache.preload();  // Optional optimization — failure is genuinely OK
} catch (CacheUnavailableException e) {
// Intentionally ignored: cache is optional, system works without it
log.debug("Cache preload skipped: {}", e.getMessage());  // At least log at debug
}


// ── Anti-Pattern 2: Log and throw (double logging) ────────────────
// ❌ Causes same exception to appear in logs multiple times
void serviceLayer(String id) throws ServiceException {
try {
return repositoryLayer(id);
} catch (DataException e) {
log.error("Service failed", e);     // Logged here
throw new ServiceException(e);      // AND logged again by caller!
// Stack trace appears 2-3 times in logs — confusing
}
}

void controllerLayer(String id) {
try {
return serviceLayer(id);
} catch (ServiceException e) {
log.error("Controller failed", e);  // SAME exception logged AGAIN
return Response.error();
}
}
// Fix: log ONCE at the boundary where you stop propagating
// All other layers: just re-throw (or wrap and throw), no logging


// ── Anti-Pattern 3: Using exceptions for flow control ─────────────
// ❌ Using exceptions to indicate normal expected conditions
// Exceptions are expensive: stack trace collection, JVM overhead

// Bad: checking login with exception
boolean isLoggedIn(String token) {
try {
sessionService.validate(token);
return true;
} catch (InvalidTokenException e) {
return false;   // Exception used as boolean — wrong!
}
}

// ✅ Use Optional or boolean return instead:
boolean isLoggedIn(String token) {
return sessionService.isValid(token);   // Returns boolean directly
}

Optional<Session> getSession(String token) {
return sessionService.findByToken(token);  // Returns Optional
}

// Legitimate exception-as-flow-control (rare):
// Parser combinators: fail() throws BacktrackException to try another rule
// But even there: use a specialized, no-stack-trace exception
// And it's contained within the parser — never crosses API boundaries


// ── Anti-Pattern 4: Catching Throwable or Error ───────────────────
// ❌ Never catch Error or Throwable in application code
try {
process();
} catch (Throwable t) {   // ❌ Catches OutOfMemoryError, StackOverflowError!
log.error("Error", t);
// JVM is in a broken state. Continuing after OOME is WRONG.
// Heap is corrupted. Data structures may be incomplete.
// Worst thing: the application appears to keep running, silently broken.
}

// ✅ Let Errors propagate — JVM will crash, system will restart
// That's BETTER than limping along in a broken state
// With -XX:+ExitOnOutOfMemoryError: clean exit + restart is correct

// Acceptable: health-check or monitoring that catches Throwable to report it
// and then RETHROWS:
void executeWithMonitoring(Runnable task) {
try {
task.run();
} catch (Throwable t) {
metrics.increment("task.failures", "error", t.getClass().getSimpleName());
throw t;  // MUST rethrow — don't swallow Errors
}
}


// ── Anti-Pattern 5: Exception message with no context ─────────────
// ❌ Message too vague to diagnose
throw new ServiceException("Database error");
// Which database? Which operation? Which record? Unknown.

// ✅ Include all relevant context
throw new ServiceException(
String.format("Failed to load order %s for user %s from DB [%s]: %s",
orderId, userId, dbUrl, cause.getMessage()),
cause);
// Now you can diagnose without guessing


// ── Anti-Pattern 6: Catching and re-wrapping loses stack trace ────
// ❌ Creates new exception without cause — loses original stack trace
try {
dbOperation();
} catch (SQLException e) {
throw new DataException("Database error");  // ← cause not passed!
// Original SQLException's stack trace is GONE
// You see DataException at your code, but not WHICH SQL line failed
}

// ✅ Always pass cause
throw new DataException("Database error", e);  // ← e is the cause


// ── Anti-Pattern 7: Using printStackTrace() in production ─────────
// ❌ Prints to stderr — not captured by logging framework
try {
operation();
} catch (Exception e) {
e.printStackTrace();  // Goes to System.err, not to your log aggregator
// In Kubernetes/Docker: stderr might go somewhere completely different
// In production: this is invisible to your monitoring tools
}

// ✅ Always use your logging framework
try {
operation();
} catch (Exception e) {
log.error("Operation failed", e);  // Goes to log aggregator, searchable, alertable
}


// ── Anti-Pattern 8: Overly broad catch hiding different causes ─────
// ❌ Catches everything the same way — can't distinguish causes
void process(String input) {
try {
String validated = validate(input);     // May throw ValidationException
Result result    = compute(validated);  // May throw ArithmeticException
save(result);                           // May throw IOException
} catch (Exception e) {
throw new ProcessingException("Failed", e);
// All three failures get the same treatment
// ValidationException is a client error (400)
// IOException is a server error (500)
// ArithmeticException is a programming bug
// Treating them the same is wrong
}
}

// ✅ Handle each appropriately
void processBetter(String input) {
try {
String validated = validate(input);
} catch (ValidationException e) {
throw new ClientException("Invalid input: " + e.getMessage(), e);  // 400
}

    Result result;
    try {
        result = compute(validated);
    } catch (ArithmeticException e) {
        // This is a bug — rethrow as-is or wrap in unchecked
        throw new IllegalStateException("Computation failed for: " + validated, e);
    }

    try {
        save(result);
    } catch (IOException e) {
        throw new ServerException("Failed to save result", e);  // 500
    }
}

Part 8: InterruptedException — The Most Mishandled Exception
java// InterruptedException deserves its own section
// It is THE most commonly mishandled exception in Java concurrent code

// WHAT interruption means:
// Thread.interrupt() sets a flag on the target thread
// Blocking methods (sleep, wait, take, get...) CHECK this flag
// If flag is set → they throw InterruptedException

// The flag is CLEARED when InterruptedException is thrown
// The exception IS the notification: "someone wants you to stop"

// ── WHY the flag matters ──────────────────────────────────────────
// ❌ Anti-pattern 1: Catch and ignore (NEVER DO THIS)
try {
Thread.sleep(1000);
} catch (InterruptedException e) {
// Ignore. Flag was cleared when exception was thrown.
// Whoever wanted this thread to stop: gets no response
// Thread keeps running. Shutdown blocked. System hangs.
}

// ❌ Anti-pattern 2: Log and continue
try {
Thread.sleep(1000);
} catch (InterruptedException e) {
log.warn("Interrupted");
// Still doesn't restore the flag — caller can't check it
}


// ── CORRECT PATTERN 1: Restore flag and exit ─────────────────────
// Use when: you ARE a worker thread and should stop when interrupted

class WorkerThread implements Runnable {
@Override
public void run() {
while (!Thread.currentThread().isInterrupted()) {
try {
Task task = queue.take();   // Blocking — throws on interrupt
process(task);
} catch (InterruptedException e) {
Thread.currentThread().interrupt();  // ← RESTORE THE FLAG
break;                               // ← Exit the loop
}
}
// Clean shutdown: finish current work, exit
log.info("Worker thread shutting down cleanly");
}
}

// WHY restore the flag?
// After InterruptedException: flag is CLEARED
// If you don't restore it: isInterrupted() returns false
// Any caller checking isInterrupted() thinks thread is fine
// Graceful shutdown logic breaks
// restore = Thread.currentThread().interrupt() sets flag again


// ── CORRECT PATTERN 2: Re-throw or declare throws ────────────────
// Use when: you're a library/utility, caller decides how to handle

class TaskExecutor {
void executeTask(Callable<?> task) throws InterruptedException {
// Don't catch InterruptedException — let it propagate
// Caller is responsible for handling interruption
Object result = task.call();
// If task.call() throws InterruptedException → propagates to caller
}
}

// Or: wrap in unchecked to cross functional API boundary
void executeNoThrow(Callable<?> task) {
try {
task.call();
} catch (InterruptedException e) {
Thread.currentThread().interrupt();  // ← Always restore
throw new TaskInterruptedException("Task interrupted", e);  // Unchecked
} catch (Exception e) {
throw new TaskExecutionException("Task failed", e);
}
}


// ── CORRECT PATTERN 3: Clean up then restore ────────────────────
// Use when: you hold resources that must be released before exiting

void processWithResource() {
Resource resource = acquireResource();
try {
while (true) {
try {
doWork(resource);               // May block and throw IE
} catch (InterruptedException e) {
log.info("Processing interrupted — cleaning up");
releaseResource(resource);      // Clean up FIRST
Thread.currentThread().interrupt(); // THEN restore flag
return;                         // Then exit
}
}
} finally {
releaseResource(resource);  // Also clean up on normal exit
}
}


// ── RULE SUMMARY ─────────────────────────────────────────────────
// NEVER silently swallow InterruptedException
// ALWAYS do one of:
//   A) Restore flag + stop (worker thread, shutdown handler)
//   B) Re-throw / declare throws (library/utility code)
//   C) Restore flag + wrap in unchecked (functional API boundary)
// Restoring the flag lets callers detect the interrupt
// Not restoring = lying about thread state = subtle, hard-to-find bugs


// ── Checking and resetting interruption ──────────────────────────
// Check if interrupted (does NOT clear flag):
if (Thread.currentThread().isInterrupted()) {
// Thread has been interrupted
}

// Check and CLEAR the flag:
if (Thread.interrupted()) {  // static method — clears flag!
// Thread WAS interrupted — flag is now cleared
// Use only when you're "consuming" the interrupt signal
}

// Distinguishing:
// isInterrupted(): "is my thread interrupted?" — non-destructive check
// Thread.interrupted(): "was I interrupted? (and clear it)" — destructive
// Use isInterrupted() in loop conditions: while (!Thread.currentThread().isInterrupted())
// Use Thread.interrupted() only when you explicitly want to consume and handle

Part 9: Exception Handling in Concurrent Code
java// Exceptions behave differently in multithreaded contexts

// ── Exceptions in thread pool tasks ──────────────────────────────

// execute() — uncaught exception goes to UncaughtExceptionHandler
ExecutorService pool = Executors.newFixedThreadPool(4);

pool.execute(() -> {
throw new RuntimeException("Task failed!");
// Goes to thread's UncaughtExceptionHandler
// Default: prints to stderr (invisible in production!)
});

// ✅ Set UncaughtExceptionHandler on ThreadFactory
ThreadFactory factory = r -> {
Thread t = new Thread(r);
t.setUncaughtExceptionHandler((thread, ex) -> {
log.error("Uncaught exception in thread {}", thread.getName(), ex);
metrics.increment("thread.uncaught.exception");
// Don't rethrow — thread is already terminating
// A new thread will be created for the next task
});
return t;
};


// submit() — exception captured in Future (silent trap!)
Future<?> future = pool.submit(() -> {
throw new RuntimeException("Silent killer!");
// Exception is CAPTURED inside the Future
// NOT thrown until you call future.get()!
});

// If you never call future.get(): exception SILENTLY DISAPPEARS
future.isDone(); // true — but succeeded? failed? can't tell without get()

// ✅ Always call get() or use whenComplete:
try {
future.get();
} catch (ExecutionException e) {
// e.getCause() = the original RuntimeException
log.error("Task failed: {}", e.getCause().getMessage(), e.getCause());
}


// ── afterExecute hook — catch all pool exceptions ────────────────
class SafeThreadPool extends ThreadPoolExecutor {
SafeThreadPool(int core, int max, long keepAlive, TimeUnit unit,
BlockingQueue<Runnable> queue) {
super(core, max, keepAlive, unit, queue);
}

    @Override
    protected void afterExecute(Runnable r, Throwable t) {
        super.afterExecute(r, t);

        // For execute(): t contains the exception directly
        if (t != null) {
            log.error("Task threw uncaught exception", t);
            metrics.increment("pool.task.error");
        }

        // For submit(): t is null but Future has the exception
        if (t == null && r instanceof Future<?>) {
            try {
                ((Future<?>) r).get(0, TimeUnit.MILLISECONDS);
            } catch (ExecutionException e) {
                log.error("Future task threw exception", e.getCause());
                metrics.increment("pool.task.error");
            } catch (TimeoutException | CancellationException | InterruptedException e) {
                // Task still running, cancelled, or interrupted — not an error
            }
        }
    }
}


// ── CompletableFuture exception handling ─────────────────────────
CompletableFuture<String> cf = CompletableFuture
.supplyAsync(() -> fetchData(), pool)
.thenApply(data -> transform(data))
.exceptionally(ex -> {
// ex is wrapped: ex instanceof CompletionException
Throwable cause = ex.getCause();  // The REAL exception
log.error("Pipeline failed: {}", cause.getMessage(), cause);
return "DEFAULT_VALUE";
})
.whenComplete((result, ex) -> {
if (ex != null) {
metrics.increment("cf.failures");
} else {
metrics.increment("cf.successes");
}
});


// ── Thread.UncaughtExceptionHandler for all threads ───────────────
// Global handler for any thread without specific handler
Thread.setDefaultUncaughtExceptionHandler((thread, ex) -> {
log.error("UNCAUGHT EXCEPTION in thread [{}]: {}",
thread.getName(), ex.getMessage(), ex);
// For Errors: consider killing the JVM (OOM, SOE = bad state)
if (ex instanceof Error) {
log.error("FATAL: JVM Error — initiating shutdown");
Runtime.getRuntime().halt(1);  // Hard exit
}
});

Part 10: The Interview Q&A Round 🎤

Q1. What is the difference between checked and unchecked exceptions? When do you use each?

"Checked exceptions are subclasses of Exception that are NOT RuntimeException. The compiler enforces handling them — the caller must either catch or declare throws. They represent EXPECTED failures where the caller has meaningful recovery options: file not found, network connection refused, SQL constraint violated. The idea is: forcing callers to acknowledge these failures at compile time produces more robust code.
Unchecked exceptions are RuntimeException and its subclasses. No compiler enforcement. They typically represent programming errors — null where non-null expected, invalid argument, violated precondition. The assumption is: if you use the API correctly, these never happen. Forcing callers to handle programming bugs in try-catch would just add noise.
In modern practice, most libraries prefer unchecked. Checked exceptions break lambdas, pollute API signatures with throws chains, and the handling is often just catching and re-throwing anyway. Spring, Hibernate, and most modern frameworks wrap everything in unchecked. The key principle I apply: checked when there's a specific, recoverable response the caller can take; unchecked for bugs and for any exception that crosses many layers where most callers can't handle it meaningfully."


Q2. What are suppressed exceptions and when do they occur?
java// Suppressed exceptions: secondary exceptions that occur during cleanup
// when a primary exception has already been thrown

// Classic scenario:
try (Connection conn = getConnection();  // Might throw on close
Statement stmt = conn.createStatement()) {

    stmt.execute("DELETE FROM users WHERE id = 1");  // ← Primary exception here
    // Suppose this throws: SQLTimeoutException

    // try-with-resources then closes stmt and conn
    // Suppose conn.close() throws: ConnectionResetException
}
// What propagates? SQLTimeoutException (primary)
// What's suppressed? ConnectionResetException (attached to primary)

// Reading suppressed exceptions:
try {
// try-with-resources block
} catch (SQLException primary) {
System.out.println("Primary: " + primary.getMessage());

    Throwable[] suppressed = primary.getSuppressed();
    for (Throwable s : suppressed) {
        System.out.println("Suppressed: " + s.getMessage());
    }
}

// WHY suppressed exceptions matter:
// Pre-Java 7: exception in finally REPLACED try exception
// The original cause was LOST — debugging production issues was guesswork
// Java 7 try-with-resources: keeps BOTH — primary cause + cleanup failures
// Now you see the full picture: "query failed AND connection close failed"

// Manual use:
Exception primary = new ServiceException("Main operation failed");
try {
cleanup();
} catch (CleanupException e) {
primary.addSuppressed(e);  // Attach without replacing
}
throw primary;

Q3. What happens when an exception is thrown in finally?

"When an exception is thrown in finally, it REPLACES any exception that was propagating from the try block. The try-block exception is silently discarded — it vanishes with no trace. The caller only sees the finally exception. This is one of the most dangerous Java gotchas because it means the root cause of your problem disappears.
This is exactly the problem that try-with-resources and suppressed exceptions were designed to solve. With try-with-resources, if the body throws exception A and close() throws exception B, exception A propagates as the primary and exception B is added as a suppressed exception via addSuppressed(). Nothing is lost.
The rules for finally: NEVER return from a finally block (swallows exceptions), NEVER throw from a finally block manually (use try-with-resources instead), and always use try-with-resources for AutoCloseable resources rather than manual finally cleanup. This makes the right behavior automatic."


Q4. How do you handle InterruptedException correctly?

"InterruptedException is the JVM's cooperative cancellation mechanism. When Thread.interrupt() is called on a thread, any blocking operation (sleep, wait, take, get) throws InterruptedException AND CLEARS the interrupt flag. The critical mistake is catching it and ignoring it — that clears the flag without acting on it, so any code higher up that checks isInterrupted() thinks the thread is fine and shutdown logic breaks.
There are three correct patterns. First: restore the flag and exit. This is for worker threads — Thread.currentThread().interrupt() to restore the flag, then break out of the loop. The thread terminates cleanly. Second: re-throw or declare throws. Library code shouldn't decide what to do with interruption — let the caller decide by letting the exception propagate. Third: restore the flag and wrap in unchecked. For functional APIs where checked exceptions aren't allowed in lambdas, catch, restore the flag, then throw a RuntimeException wrapping the InterruptedException.
The invariant: interrupted status must always be preserved for callers who need it. Swallowing it silently is a contract violation."


Q5. Design an exception hierarchy for a payment service
java// Root: all payment exceptions (unchecked — modern style)
public class PaymentException extends RuntimeException {
private final String errorCode;
private final boolean retryable;

    public PaymentException(String errorCode, String message,
                            boolean retryable, Throwable cause) {
        super(message, cause);
        this.errorCode  = errorCode;
        this.retryable  = retryable;
    }

    public String  getErrorCode() { return errorCode; }
    public boolean isRetryable()  { return retryable; }
}

// Client errors (4xx equivalent — don't retry)
public class InsufficientFundsException extends PaymentException {
public InsufficientFundsException(String accountId,
BigDecimal requested, BigDecimal available) {
super("INSUFFICIENT_FUNDS",
String.format("Account %s: requested=%.2f available=%.2f",
accountId, requested, available),
false, null);
}
}

public class CardDeclinedException extends PaymentException {
private final String declineCode;  // Processor-specific code

    public CardDeclinedException(String declineCode, String message) {
        super("CARD_DECLINED", message, false, null);
        this.declineCode = declineCode;
    }
    public String getDeclineCode() { return declineCode; }
}

public class InvalidPaymentDetailsException extends PaymentException {
public InvalidPaymentDetailsException(String field, String reason) {
super("INVALID_PAYMENT_DETAILS",
"Invalid payment details - " + field + ": " + reason,
false, null);
}
}

// Server/transient errors (5xx equivalent — may retry)
public class PaymentProcessorUnavailableException extends PaymentException {
public PaymentProcessorUnavailableException(String processor, Throwable cause) {
super("PROCESSOR_UNAVAILABLE",
"Payment processor unavailable: " + processor,
true, cause);   // Retryable!
}
}

public class PaymentTimeoutException extends PaymentException {
private final Duration timeout;

    public PaymentTimeoutException(Duration timeout, Throwable cause) {
        super("PAYMENT_TIMEOUT",
              "Payment timed out after " + timeout.toMillis() + "ms",
              true, cause);   // Retryable!
        this.timeout = timeout;
    }
}

// Duplicate payment detection (idempotency)
public class DuplicatePaymentException extends PaymentException {
private final String originalPaymentId;

    public DuplicatePaymentException(String idempotencyKey, String originalId) {
        super("DUPLICATE_PAYMENT",
              "Duplicate payment detected: key=" + idempotencyKey +
              " original=" + originalId,
              false, null);
        this.originalPaymentId = originalId;
    }
    public String getOriginalPaymentId() { return originalPaymentId; }
}

// Service layer usage:
PaymentResult process(PaymentRequest request) {
try {
validateRequest(request);           // throws InvalidPaymentDetailsException
checkFunds(request);                // throws InsufficientFundsException
return processor.charge(request);  // throws CardDeclined/Unavailable/Timeout
} catch (PaymentException e) {
throw e;   // All are already correct type — just re-throw
} catch (Exception e) {
// Unexpected exception — wrap with context
throw new PaymentException("UNEXPECTED_ERROR",
"Unexpected error processing payment " + request.getId(),
false, e);
}
}

// Controller/API layer:
try {
return process(request);
} catch (InsufficientFundsException | CardDeclinedException e) {
return Response.unprocessable(e.getErrorCode(), e.getMessage());  // 422
} catch (InvalidPaymentDetailsException e) {
return Response.badRequest(e.getErrorCode(), e.getMessage());     // 400
} catch (DuplicatePaymentException e) {
return Response.conflict(e.getOriginalPaymentId());                // 409
} catch (PaymentException e) {
if (e.isRetryable()) {
enqueueForRetry(request);
return Response.accepted("Payment queued for retry");          // 202
}
return Response.internalError(e.getErrorCode());                   // 500
}
```

---

## Section 10 Master Summary 🧠
```
EXCEPTION HIERARCHY:
Throwable → Error (don't catch) | Exception
Exception → RuntimeException (unchecked) | others (checked)
Checked:   compiler enforces handling, expected/recoverable failures
Unchecked: no enforcement, programming bugs or common framework style

CHECKED vs UNCHECKED:
Checked:   file I/O, network, DB — external, recoverable
Unchecked: null params, illegal args, bad state — bugs
Modern trend: unchecked preferred (lambdas, clean APIs, Spring)

TRY-WITH-RESOURCES:
Closes in REVERSE declaration order
Exception in body → primary; exception in close() → SUPPRESSED
primary.getSuppressed() to read all cleanup failures
ALWAYS prefer over manual finally for AutoCloseable

SUPPRESSED EXCEPTIONS:
Java 7+ solves "finally overwrites try exception" problem
primary.addSuppressed(secondary) — nothing lost
Both exceptions visible in logs and via getSuppressed()

EXCEPTION CHAINING:
ALWAYS pass cause: throw new WrapperException("msg", cause)
Without cause: root cause invisible in production logs
getCause() navigates chain; "Caused by:" in stack trace

INTERRUPTEDEXCEPTION RULES:
NEVER swallow silently (clears flag, breaks shutdown)
Pattern A: restore + exit    (Thread.currentThread().interrupt(); return)
Pattern B: re-throw          (declare throws or let propagate)
Pattern C: restore + wrap    (interrupt(); throw new RuntimeException(e))
Flag cleared by IE throw — must manually restore with interrupt()

ANTI-PATTERNS (code review rejections):
❌ Empty catch block (swallowing)
❌ Log AND throw (double logging)
❌ Exception for flow control (use Optional/boolean)
❌ Catching Throwable/Error (JVM broken state)
❌ Exception message with no context
❌ Wrapping without cause (root cause lost)
❌ printStackTrace() in production (use logging framework)
❌ return from finally (swallows exceptions)

CUSTOM EXCEPTION DESIGN:
One domain root extends RuntimeException
Specific subclasses for distinct failures
Include: errorCode (machine-readable), retryable flag
Message: include all relevant IDs and values
Omit: passwords, SSNs, credit cards from messages
Provide all 4 constructor forms

CONCURRENT EXCEPTION HANDLING:
execute(): goes to UncaughtExceptionHandler (set it explicitly!)
submit():  captured in Future — silent unless get() is called
afterExecute() hook: catch all pool exceptions in one place
CompletableFuture: exceptionally/handle/whenComplete for pipelines
Default UncaughtExceptionHandler: set globally for safety net

KEY INTERVIEW POINTS:
"Suppressed exceptions prevent root cause loss in cleanup"
"InterruptedException flag must be restored — it's a contract"
"Checked breaks lambdas — modern code prefers unchecked"
"Log ONCE at the boundary where you stop propagating"
"Exception message must include context: IDs, values, constraints"