Section 11: Java 8+ Features 

Sections 1-10 covered the JVM and concurrency foundation. Section 11 is about the language features that changed how Java is written. Java 8 was the biggest evolution since Java 5. FAANG interviewers use these questions to test whether you understand the why and mechanics behind the syntax — not just that lambdas exist.

The Big Picture — What Java 8 Changed
PRE-JAVA 8 JAVA:
Verbose: anonymous inner classes for every callback
Imperative: tell the computer HOW to do things (loops, mutations)
Sequential: single-threaded by default
Null-heavy: null returned everywhere, NPE everywhere

JAVA 8+ JAVA:
Concise: lambda expressions replace anonymous classes
Declarative: tell the computer WHAT you want (streams, pipelines)
Parallel-ready: parallel streams, CompletableFuture
Null-explicit: Optional signals "value may be absent"

The shift: from IMPERATIVE to FUNCTIONAL style
Not pure functional (Java still has mutability, state)
But: functions as values, composable pipelines, immutable transforms

Core features:
→ Lambda expressions        (functions as values)
→ Functional interfaces     (the type system for lambdas)
→ Stream API                (declarative data pipelines)
→ Optional<T>               (explicit null-absence)
→ Method references         (shorthand for lambdas)
→ Default methods           (interface evolution)
→ CompletableFuture         (Section 6)
→ Date/Time API (java.time) (replace broken Date/Calendar)

Part 1: Lambda Expressions — Internals and Capture Rules
What a Lambda Actually Is
java// Pre-Java 8: Runnable via anonymous inner class
Runnable r1 = new Runnable() {
@Override
public void run() {
System.out.println("running");
}
};
// Compiler: creates a .class file (MyClass$1.class)
// JVM: allocates new object each time this line executes
// Memory: captures reference to enclosing instance (hidden this$0 field)

// Java 8: Lambda
Runnable r2 = () -> System.out.println("running");
// Compiler: generates invokedynamic bytecode
// JVM: first call → metafactory generates implementation class (once, cached)
// Subsequent calls: may return SAME instance (JVM decides)
// Memory: no enclosing instance captured (unless lambda captures variables)


// ── What the compiler does with lambdas ───────────────────────────
// Lambda body → private static method in the ENCLOSING class
// invokedynamic → LambdaMetafactory → creates interface implementation
// This is why lambdas are FAST:
//   → No new class file per lambda
//   → JVM can optimize the call site
//   → Stateless lambdas: single instance shared (no allocation after first call)
//   → Capturing lambdas: new instance per capture (like anonymous class)

// Verify:
Runnable stateless1 = () -> System.out.println("hi");
Runnable stateless2 = () -> System.out.println("hi");
System.out.println(stateless1 == stateless2);  // JVM-dependent, often TRUE
// Same singleton instance!

String captured = "hello";
Runnable capturing1 = () -> System.out.println(captured);
Runnable capturing2 = () -> System.out.println(captured);
System.out.println(capturing1 == capturing2);   // FALSE — new instance per lambda
// because captured variable creates
// new closure each time
Variable Capture — The Effectively Final Rule
java// Lambdas can capture: static fields, instance fields, local variables
// Rule for LOCAL variables: must be EFFECTIVELY FINAL

// ── Effectively final: never reassigned after initialization ──────
String message = "hello";          // Effectively final — never reassigned
Runnable r = () -> print(message); // ✅ OK

String message2 = "hello";
message2 = "world";                // Reassigned → NOT effectively final
Runnable r2 = () -> print(message2); // ❌ Compile error!


// WHY this restriction exists:
// Local variables live on the STACK (per thread)
// Lambda may run on a DIFFERENT thread (e.g., in a thread pool)
// Stack frame may be gone by the time lambda runs!
// Solution: lambda COPIES the variable value at capture time
// If value could change after copy: lambda has stale data → inconsistency
// Effectively final guarantee: copy is always valid

// ── Instance fields — NO restriction ─────────────────────────────
class MyClass {
private String mutable = "initial";

    Runnable makeLambda() {
        // No effectively-final restriction on instance fields
        return () -> System.out.println(mutable);
        // Lambda captures 'this' (the MyClass instance)
        // Reads mutable through 'this' reference at execution time
        // Can see changes to mutable after lambda creation
    }

    void demonstrate() {
        Runnable r = makeLambda();
        mutable = "changed";
        r.run();   // Prints "changed" — reads current field value
    }
}

// ── This subtle capture difference ───────────────────────────────
class Handler {
private String prefix = "INFO";

    Runnable logMessage(String msg) {
        String local = "[" + prefix + "]";  // Local var — effectively final
                                             // (value of prefix captured AT THIS POINT)
        return () -> System.out.println(local + " " + msg);
        // local: copy of "[INFO]" — won't change even if prefix changes
        // msg: copy of argument — won't change
        // prefix: NOT captured (we captured local, which USED prefix)
    }
}

// ── Mutation workaround — single-element array ────────────────────
// Common hack (use with caution):
int[] count = {0};  // Array reference is effectively final
// Array CONTENTS can change
Runnable counter = () -> count[0]++;  // ✅ Compiles — mutates array content
// ⚠️ Not thread-safe — if lambda runs on multiple threads: race condition
// ⚠️ Code smell — if you need mutation, reconsider design

// Better: use AtomicInteger
AtomicInteger atomicCount = new AtomicInteger(0);
Runnable safeCounter = () -> atomicCount.incrementAndGet();  // Thread-safe

Part 2: Functional Interfaces — The Type System
java// A functional interface has EXACTLY ONE abstract method
// Java 8: @FunctionalInterface annotation (enforced at compile time)
// This is the type of a lambda expression

// ── The Core 4 ───────────────────────────────────────────────────

// Supplier<T>: () → T  (produce a value)
Supplier<String> greeting = () -> "Hello, World!";
String s = greeting.get();   // "Hello, World!"
// Use for: lazy initialization, factory methods, deferred computation

// Consumer<T>: T → void  (consume a value, side effect)
Consumer<String> printer = msg -> System.out.println(msg);
printer.accept("Hello");    // prints "Hello"
// Use for: forEach, logging, side-effecting operations

// Function<T, R>: T → R  (transform)
Function<String, Integer> length = str -> str.length();
int len = length.apply("hello");  // 5
// Use for: map, transformation, conversion

// Predicate<T>: T → boolean  (test)
Predicate<String> isEmpty = str -> str.isEmpty();
boolean empty = isEmpty.test("");  // true
// Use for: filter, matching, validation


// ── Composition methods — functional programming in Java ──────────

// Predicate composition:
Predicate<Integer> positive = n -> n > 0;
Predicate<Integer> even     = n -> n % 2 == 0;

Predicate<Integer> positiveAndEven = positive.and(even);  // &&
Predicate<Integer> positiveOrEven  = positive.or(even);   // ||
Predicate<Integer> notPositive     = positive.negate();   // !

List<Integer> result = numbers.stream()
.filter(positiveAndEven)
.collect(Collectors.toList());


// Function composition:
Function<String, String> trim      = String::trim;
Function<String, String> uppercase = String::toUpperCase;
Function<String, Integer> length2  = String::length;

// andThen: trim → uppercase → length  (left to right)
Function<String, Integer> pipeline = trim.andThen(uppercase).andThen(length2);
pipeline.apply("  hello  ");   // trim → "hello" → "HELLO" → 5

// compose: reverses order (right to left)
Function<String, String> composed = uppercase.compose(trim);  // trim FIRST, then uppercase
composed.apply("  hello  ");   // trim → "hello" → "HELLO"


// Consumer chaining:
Consumer<String> log    = msg -> logger.info(msg);
Consumer<String> audit  = msg -> auditService.record(msg);
Consumer<String> both   = log.andThen(audit);  // runs log THEN audit
both.accept("User logged in");


// ── Specialized variants — avoid boxing in hot paths ──────────────
// IntFunction<R>:      int → R
// ToIntFunction<T>:    T → int
// IntUnaryOperator:    int → int  (IntFunction<Integer> but no boxing)
// IntBinaryOperator:   (int, int) → int
// IntSupplier:         () → int
// IntConsumer:         int → void
// IntPredicate:        int → boolean

// ❌ Slow: boxing overhead
Function<Integer, Integer> doubleBoxed = n -> n * 2;
List<Integer> doubled = list.stream()
.map(doubleBoxed)      // autoboxes Integer → int → Integer each time
.collect(toList());

// ✅ Fast: no boxing
IntUnaryOperator doublePrim = n -> n * 2;
int[] doubled2 = IntStream.of(1, 2, 3, 4, 5)
.map(doublePrim)       // all primitive operations, no boxing
.toArray();


// ── BiXxx variants — two-argument functions ───────────────────────
BiFunction<String, Integer, String>  repeat  = (s, n) -> s.repeat(n);
BiConsumer<String, Integer>          log2    = (msg, code) -> log(code, msg);
BiPredicate<String, String>          matches = (s, pattern) -> s.matches(pattern);

repeat.apply("ha", 3);   // "hahaha"


// ── UnaryOperator and BinaryOperator — same type in and out ───────
UnaryOperator<String>  shout  = s -> s.toUpperCase() + "!";
BinaryOperator<Integer> sum   = (a, b) -> a + b;

// These are subtype of Function and BiFunction respectively:
UnaryOperator<String> u = String::toUpperCase;  // Also a Function<String, String>
String result2 = u.apply("hello");  // "HELLO"


// ── Custom functional interfaces ──────────────────────────────────
@FunctionalInterface
interface TriFunction<A, B, C, R> {
R apply(A a, B b, C c);

    // Can have default methods (don't count toward abstract method limit)
    default <V> TriFunction<A, B, C, V> andThen(Function<? super R, ? extends V> after) {
        return (a, b, c) -> after.apply(this.apply(a, b, c));
    }
}

TriFunction<String, Integer, Boolean, String> formatter =
(str, repeat, upper) ->
(upper ? str.toUpperCase() : str).repeat(repeat);

formatter.apply("ha", 3, true);  // "HAHAHA"

Part 3: Stream API — Pipeline Mechanics and Internals
How Streams Actually Work
java// Stream is a PIPELINE of operations over a data source
// Three parts: source → intermediate operations → terminal operation

List<String> names = List.of("Alice", "Bob", "Charlie", "Dave", "Eve");

// This is ONE pipeline:
long count = names.stream()          // SOURCE: creates stream from list
.filter(n -> n.length() > 3)     // INTERMEDIATE: lazy, returns new Stream
.map(String::toUpperCase)         // INTERMEDIATE: lazy, returns new Stream
.sorted()                         // INTERMEDIATE: lazy, returns new Stream
.count();                         // TERMINAL: triggers execution, returns result

// KEY: NOTHING EXECUTES until the terminal operation!
// filter/map/sorted are just building a pipeline specification
// count() fires the pipeline

// What happens internally:
// count() asks sorted() for next element
// sorted() needs ALL elements (stateful) → pulls from map
// map() asks filter() for next element
// filter() asks source for next element → "Alice"
// filter("Alice".length() > 3) → true → passes to map
// map("Alice".toUpperCase()) → "ALICE"
// sorted() buffers "ALICE", continues pulling...
// After all elements buffered: sorted() outputs in order
// count() counts: 3 ("ALICE", "CHARLIE", "DAVE") — Bob(3) and Eve(3) filtered

// ── LAZY evaluation — the performance key ─────────────────────────
// Without laziness: each operation scans entire list separately
//   filter: scan all N → build new list
//   map: scan filtered list → build new list
//   sorted: scan mapped list → sort
//   count: scan sorted list

// With laziness: elements flow through pipeline ONE AT A TIME
//   Element 1 → filter → map → (sorted buffers it)
//   Element 2 → filter → map → (sorted buffers it)
//   ...
//   One pass through elements, not three passes!

// ── Short-circuit operations — stop early ────────────────────────
Optional<String> first = names.stream()
.filter(n -> n.startsWith("C"))
.map(String::toLowerCase)
.findFirst();   // Terminal: short-circuits! Stops after FIRST match

// Execution:
// "Alice" → filter (no) → skip
// "Bob"   → filter (no) → skip
// "Charlie" → filter (yes) → map → "charlie" → findFirst() returns it
// "Dave" and "Eve" are NEVER processed!

// Other short-circuit operations: anyMatch, allMatch, noneMatch, limit, findAny
// These can process far fewer elements than the source size

boolean anyLong = names.stream()
.anyMatch(n -> n.length() > 5);
// Stops at "Charlie" — no need to check Dave and Eve
Intermediate Operations — Complete Reference
javaList<Person> people = getPeople();

// ── Filtering ─────────────────────────────────────────────────────
stream.filter(p -> p.getAge() >= 18)          // Keep matching elements
stream.distinct()                              // Remove duplicates (uses equals/hashCode)
stream.limit(10)                               // Take first 10 (short-circuit)
stream.skip(5)                                 // Skip first 5
stream.takeWhile(p -> p.getAge() < 30)        // Java 9: take while predicate holds
stream.dropWhile(p -> p.getAge() < 18)        // Java 9: drop while predicate holds

// ── Transformation ────────────────────────────────────────────────
stream.map(Person::getName)                    // T → R  (String in this case)
stream.mapToInt(Person::getAge)                // T → IntStream (no boxing!)
stream.mapToLong(Person::getId)                // T → LongStream
stream.mapToDouble(Person::getSalary)          // T → DoubleStream
stream.mapToObj(...)                           // Primitive stream → object stream

// ── flatMap — the most misunderstood ─────────────────────────────
// map: one element → one element
// flatMap: one element → ZERO or MORE elements (flattens nested streams)

// Example: List<List<String>> → List<String>
List<List<String>> nested = List.of(
List.of("a", "b", "c"),
List.of("d", "e"),
List.of("f")
);

List<String> flat = nested.stream()
.flatMap(List::stream)          // Each List → Stream<String> → flattened
.collect(Collectors.toList());
// ["a", "b", "c", "d", "e", "f"]

// Real use case: get all order items across all orders
List<OrderItem> allItems = orders.stream()
.flatMap(order -> order.getItems().stream())   // Order → Stream<OrderItem>
.collect(Collectors.toList());

// flatMap with Optional (Java 9+):
Optional<String> result = Optional.of("user123")
.flatMap(id -> userRepository.findById(id))    // findById returns Optional<User>
.map(User::getEmail);                           // map only if present


// ── Sorting ───────────────────────────────────────────────────────
stream.sorted()                                    // Natural order (Comparable)
stream.sorted(Comparator.comparing(Person::getName)) // By name
stream.sorted(Comparator.comparing(Person::getAge)
.reversed()
.thenComparing(Person::getName))       // Multi-level sort


// ── Peek — debugging ──────────────────────────────────────────────
// Intermediate: passes each element through, calls action as side effect
stream.filter(p -> p.getAge() > 18)
.peek(p -> log.debug("Passed filter: {}", p.getName()))  // Inspect mid-pipeline
.map(Person::getName)
.peek(name -> log.debug("Mapped to: {}", name))
.collect(Collectors.toList());
// ⚠️ peek is for DEBUGGING ONLY — don't use for real side effects
// peek may not execute if downstream short-circuits!
Terminal Operations — Complete Reference
java// ── Reduction ─────────────────────────────────────────────────────
// reduce: combine all elements into one result
int sum = IntStream.rangeClosed(1, 100)
.reduce(0, Integer::sum);           // identity=0, accumulator=sum
// 0 + 1 + 2 + ... + 100 = 5050

Optional<Integer> max = Stream.of(3, 1, 4, 1, 5, 9, 2, 6)
.reduce(Integer::max);              // No identity → returns Optional
// Optional[9]

// Three-arg reduce for parallel (combiner joins partial results):
int parallelSum = Stream.of(1, 2, 3, 4, 5)
.parallel()
.reduce(0,
(partialSum, element) -> partialSum + element,  // accumulator
Integer::sum);                                   // combiner (merges partial sums)


// ── Matching ──────────────────────────────────────────────────────
boolean anyAdult  = people.stream().anyMatch(p -> p.getAge() >= 18);   // Short-circuit OR
boolean allAdults = people.stream().allMatch(p -> p.getAge() >= 18);   // Short-circuit AND
boolean noneChild = people.stream().noneMatch(p -> p.getAge() < 0);   // Short-circuit NOR

// anyMatch on empty stream: false
// allMatch on empty stream: TRUE (vacuous truth!)
// noneMatch on empty stream: true


// ── Finding ───────────────────────────────────────────────────────
Optional<Person> first  = stream.findFirst();   // First in encounter order (deterministic)
Optional<Person> any    = stream.findAny();     // Any (faster in parallel, non-deterministic)


// ── Counting and statistics ───────────────────────────────────────
long count        = stream.count();
OptionalDouble avg = IntStream.of(1,2,3,4,5).average();       // OptionalDouble[3.0]
int  sumI         = IntStream.of(1,2,3,4,5).sum();            // 15
OptionalInt maxI  = IntStream.of(1,2,3,4,5).max();            // OptionalInt[5]
IntSummaryStatistics stats = IntStream.of(1,2,3,4,5).summaryStatistics();
// count=5, sum=15, min=1, average=3.0, max=5


// ── forEach ───────────────────────────────────────────────────────
stream.forEach(System.out::println);        // Unordered (parallel-friendly)
stream.forEachOrdered(System.out::println); // Maintains encounter order


// ── toArray ───────────────────────────────────────────────────────
Object[]    arr1 = stream.toArray();
String[]    arr2 = stream.toArray(String[]::new);   // Pass constructor reference
Collectors — The Real Power
javaimport static java.util.stream.Collectors.*;

// ── Basic collection ──────────────────────────────────────────────
List<String>       list  = stream.collect(toList());
Set<String>        set   = stream.collect(toSet());
LinkedList<String> llist = stream.collect(toCollection(LinkedList::new));


// ── Joining ───────────────────────────────────────────────────────
String csv     = names.stream().collect(joining(", "));           // "Alice, Bob, Charlie"
String fmtd    = names.stream().collect(joining(", ", "[", "]")); // "[Alice, Bob, Charlie]"


// ── Grouping — the most powerful collector ────────────────────────
// groupingBy: Map<K, List<V>>
Map<String, List<Person>> byCity = people.stream()
.collect(groupingBy(Person::getCity));
// {"New York" → [Alice, Bob], "LA" → [Charlie], ...}

// groupingBy with downstream collector
Map<String, Long> countByCity = people.stream()
.collect(groupingBy(Person::getCity, counting()));
// {"New York" → 2, "LA" → 1}

Map<String, Double> avgAgeByCity = people.stream()
.collect(groupingBy(Person::getCity, averagingInt(Person::getAge)));

Map<String, Optional<Person>> oldestByCity = people.stream()
.collect(groupingBy(Person::getCity,
maxBy(Comparator.comparing(Person::getAge))));

// Multi-level grouping
Map<String, Map<String, List<Person>>> byStateAndCity = people.stream()
.collect(groupingBy(Person::getState,
groupingBy(Person::getCity)));


// ── Partitioning — split into two groups ─────────────────────────
Map<Boolean, List<Person>> partitioned = people.stream()
.collect(partitioningBy(p -> p.getAge() >= 18));
// {true → [adults], false → [minors]}

Map<Boolean, Long> partitionCount = people.stream()
.collect(partitioningBy(
p -> p.getAge() >= 18,
counting()            // downstream: count each partition
));


// ── toMap ─────────────────────────────────────────────────────────
Map<Integer, String> idToName = people.stream()
.collect(toMap(Person::getId, Person::getName));
// ⚠️ Throws IllegalStateException on DUPLICATE keys!

// Handle duplicates with merge function:
Map<String, String> nameToCity = people.stream()
.collect(toMap(
Person::getName,
Person::getCity,
(existing, newer) -> existing  // Keep first on duplicate key
));

// Control map type:
Map<Integer, String> sorted = people.stream()
.collect(toMap(
Person::getId,
Person::getName,
(a, b) -> a,              // merge function
TreeMap::new              // map supplier: use TreeMap (sorted)
));


// ── summarizingInt / summarizingDouble ────────────────────────────
IntSummaryStatistics ageStats = people.stream()
.collect(summarizingInt(Person::getAge));
// count, sum, min, max, average — all in one pass


// ── mapping — transform before collecting ─────────────────────────
Map<String, Set<String>> cityToNames = people.stream()
.collect(groupingBy(
Person::getCity,
mapping(Person::getName, toSet())   // transform Person→name, collect to Set
));


// ── filtering (Java 9+) — filter inside collector ────────────────
Map<String, List<Person>> adults = people.stream()
.collect(groupingBy(
Person::getCity,
filtering(p -> p.getAge() >= 18, toList())  // Filter within each group
));
// Difference from stream.filter(): stream.filter() removes groups entirely
// Collectors.filtering: keeps all groups, filters WITHIN groups


// ── teeing (Java 12+) — two collectors, one pass ─────────────────
// Process stream ONCE, collect into TWO results simultaneously
var result = people.stream()
.collect(teeing(
counting(),                              // Collector 1: count
averagingInt(Person::getAge),            // Collector 2: average age
(count, avg) -> Map.of("count", count, "avgAge", avg)  // Combine
));
// {"count" → 5, "avgAge" → 32.4}
// Only ONE pass over the stream!


// ── Custom Collector ──────────────────────────────────────────────
// Collector<T, A, R>: T=input, A=accumulator, R=result
Collector<Person, ?, Map<String, Integer>> nameToAgeCollector =
Collector.of(
HashMap::new,                                    // supplier: create accumulator
(map, p) -> map.put(p.getName(), p.getAge()),   // accumulator: fold in element
(map1, map2) -> { map1.putAll(map2); return map1; }, // combiner: merge (parallel)
Collector.Characteristics.UNORDERED              // characteristics
);

Map<String, Integer> nameAgeMap = people.stream()
.collect(nameToAgeCollector);
Parallel Streams — When to Use and When NOT to
java// Parallel stream: splits source, processes in parallel, merges results
// Powered by ForkJoinPool.commonPool()

List<Integer> numbers = IntStream.rangeClosed(1, 1_000_000)
.boxed()
.collect(Collectors.toList());

// Sequential:
long seqSum = numbers.stream()
.mapToLong(Integer::longValue)
.sum();   // ~10ms on modern hardware

// Parallel:
long parSum = numbers.parallelStream()
.mapToLong(Integer::longValue)
.sum();   // ~3ms on 4 cores → 3× faster


// ── When parallel IS faster ───────────────────────────────────────
// ✅ Large data (10,000+ elements to justify split/merge overhead)
// ✅ CPU-intensive per-element work (not I/O)
// ✅ No shared mutable state
// ✅ Order doesn't matter (or you use forEachOrdered explicitly)
// ✅ Source splits easily (ArrayList: yes, LinkedList: no)

// ── When parallel is SLOWER or WRONG ─────────────────────────────
// ❌ Small data: split/merge overhead > computation gain
List<Integer> tiny = List.of(1, 2, 3, 4, 5);
tiny.parallelStream().sum();  // SLOWER than sequential! Fork overhead > work

// ❌ I/O-bound work: threads block, not CPU-bound
paths.parallelStream().forEach(path -> {
Files.readString(path);  // Blocking I/O — threads idle, not faster
// Fix: use CompletableFuture with an I/O thread pool instead
});

// ❌ Shared mutable state — race condition
List<Integer> results = new ArrayList<>();  // NOT thread-safe!
numbers.parallelStream().forEach(n -> {
results.add(n * 2);  // ❌ Race condition — ArrayList is not thread-safe
});
// Fix: use collect() instead
List<Integer> safe = numbers.parallelStream()
.map(n -> n * 2)
.collect(Collectors.toList());  // Collectors handle concurrency internally

// ❌ Stateful lambdas
int[] runningTotal = {0};
numbers.parallelStream().forEach(n -> {
runningTotal[0] += n;  // ❌ Race condition on shared array
});
// Fix: use reduce or collect

// ❌ Encounter order matters for findFirst (but findAny is fine)
Optional<Integer> first = numbers.parallelStream()
.filter(n -> n > 500_000)
.findFirst();    // Correct result but SLOW — must find first in order
// Use findAny() if you don't care which one:
Optional<Integer> any = numbers.parallelStream()
.filter(n -> n > 500_000)
.findAny();     // Much faster — any thread's result is fine


// ── Spliterator — how streams split ───────────────────────────────
// ArrayList: O(1) split → excellent parallel performance
// LinkedList: must traverse to split → poor parallel performance
// HashSet/HashMap: good split (backed by array)
// ConcurrentHashMap.keySet(): excellent parallel performance

// Rule: parallel streams work best with random-access sources (arrays, ArrayList)
// Profile before using parallel — measure with JMH, don't assume

Part 4: Optional<T> — Design Philosophy and Correct Usage
What Optional Is (and Isn't)
java// Optional is NOT:
// → A replacement for null everywhere
// → A Maybe monad (it's close but lacks flatMap on the result value in some forms)
// → Something to put in fields, method parameters, or collections
// → A way to avoid NullPointerException at all costs

// Optional IS:
// → A RETURN TYPE that signals "this method might not return a value"
// → A way to make absence EXPLICIT in the API contract
// → A tool for composing null-safe pipelines

// The fundamental design question:
// Does the absence of a value have DOMAIN MEANING?
// → findUserById(): user might not exist → Optional<User> ✅
// → getUserName():  if you have a User, it has a name → String ✅
// → getMiddleName(): some people have none → Optional<String> ✅


// ── Creation ──────────────────────────────────────────────────────
Optional<String> present = Optional.of("hello");         // Non-null — throws NPE if null
Optional<String> maybe   = Optional.ofNullable(getValue()); // null-safe
Optional<String> empty   = Optional.empty();              // Explicitly absent


// ── The WRONG way to use Optional ────────────────────────────────

// ❌ Anti-pattern: using Optional as nullable check replacement
Optional<User> optUser = findUser(id);
if (optUser.isPresent()) {
User user = optUser.get();   // isPresent() + get() = just use orElse!
return user.getName();
}
return "Unknown";
// This is Java-6-with-extra-steps — no benefit over null check

// ✅ Correct: use the Optional pipeline API
return findUser(id)
.map(User::getName)
.orElse("Unknown");

// ❌ Anti-pattern: get() without isPresent() check
String name = findUser(id).get();   // Throws NoSuchElementException if empty!
// Worse than NullPointerException — at least
// NPE tells you the variable was null

// ❌ Anti-pattern: Optional.get() used anywhere (always use orElse variants)
// If you call get(), you're missing the point of Optional

// ❌ Anti-pattern: Optional in fields
class User {
private Optional<String> middleName;  // ❌ Not serializable
// Unclear lifecycle
// Use @Nullable annotation instead
}

// ❌ Anti-pattern: Optional as method parameter
void process(Optional<String> name) { ... }  // ❌ Forces callers to wrap
// Caller must write: process(Optional.of("Alice")) instead of process("Alice")
// Just use overloading or @Nullable instead:
void process(String name) { ... }             // May be null — document it
void process() { process(null); }             // Or: overload


// ❌ Anti-pattern: Optional in collections
List<Optional<User>> users = ...;  // ❌ Just use List<User> and filter nulls
// Or better: never put null in collections
The Optional Pipeline API — Master It
javaOptional<User> userOpt = findUser(userId);

// ── Transforming with map ─────────────────────────────────────────
Optional<String> nameOpt = userOpt.map(User::getName);
// If present: apply User::getName → Optional<String>
// If empty:   stay empty → Optional.empty()

Optional<Integer> ageOpt = userOpt.map(User::getAge);


// ── Chaining optionals with flatMap ──────────────────────────────
// Like thenCompose for CompletableFuture — prevents Optional<Optional<T>>

// map when function returns Optional:
Optional<Optional<Address>> wrong = userOpt.map(u -> findAddress(u.getId()));
//                           ↑ Nested Optional — useless

Optional<Address> flat = userOpt.flatMap(u -> findAddress(u.getId()));
//                ↑ Flat — correct

// Chain of nullable lookups — without Optional:
String country = null;
User user = findUser(id);
if (user != null) {
Address addr = findAddress(user.getId());
if (addr != null) {
City city = addr.getCity();
if (city != null) {
country = city.getCountry();
}
}
}

// With Optional:
String country2 = findUser(id)                // Optional<User>
.flatMap(u -> findAddress(u.getId()))      // Optional<Address>
.map(Address::getCity)                     // Optional<City>
.map(City::getCountry)                     // Optional<String>
.orElse("Unknown");                        // String


// ── Terminal operations — extracting the value ────────────────────
user.orElse(User.ANONYMOUS);              // Default value if empty
user.orElseGet(() -> createDefaultUser()); // Lazy default — computed only if empty
user.orElseThrow();                        // Throws NoSuchElementException if empty (Java 10+)
user.orElseThrow(() ->
new UserNotFoundException(userId));    // Custom exception if empty

// ⚠️ orElse vs orElseGet:
// orElse(expr):      expr is ALWAYS evaluated (even if Optional is present)
// orElseGet(lambda): lambda is ONLY evaluated if Optional is empty (lazy)

User defaultUser = expensiveDefaultUser();  // ← ALWAYS called with orElse
User u1 = findUser(id).orElse(defaultUser);

User u2 = findUser(id).orElseGet(() -> expensiveDefaultUser());
//                                   ↑ ONLY called if findUser returns empty


// ── Conditional actions ───────────────────────────────────────────
userOpt.ifPresent(u -> sendWelcomeEmail(u));          // Run if present, ignore if empty
userOpt.ifPresentOrElse(                               // Java 9+
u -> sendWelcomeEmail(u),                          // Run if present
() -> log.warn("User not found: {}", userId)       // Run if empty
);


// ── Filtering within Optional ─────────────────────────────────────
Optional<User> adult = userOpt
.filter(u -> u.getAge() >= 18);
// If present AND passes filter: returns the Optional
// If present AND fails filter: returns empty
// If already empty: stays empty


// ── or() — alternative Optional (Java 9+) ────────────────────────
Optional<User> fromDbOrCache = findInDb(id)
.or(() -> findInCache(id))      // If DB empty: try cache
.or(() -> Optional.of(User.ANONYMOUS));  // If cache empty: fallback
// Chain of fallbacks — none execute until needed


// ── stream() — Optional to Stream bridge (Java 9+) ───────────────
// Converts Optional to Stream of 0 or 1 elements
// Powerful for flatMapping a stream of Optionals

List<String> names = ids.stream()
.map(id -> findUser(id))         // Stream<Optional<User>>
.flatMap(Optional::stream)       // Stream<User> — empty Optionals removed!
.map(User::getName)
.collect(Collectors.toList());
// Elegantly filters out absent users


// ── Practical complete example ────────────────────────────────────
class UserService {
Optional<User> findUser(String id) {
return userRepository.findById(id);
}

    String getDisplayName(String userId) {
        return findUser(userId)
            .filter(User::isActive)                    // Skip inactive users
            .map(u -> u.getFirstName() + " " + u.getLastName()) // Format name
            .map(String::trim)
            .filter(s -> !s.isBlank())                 // Skip blank names
            .orElse("Anonymous User");                 // Default
    }

    void sendNotification(String userId, String message) {
        findUser(userId)
            .filter(User::hasEmailEnabled)
            .map(User::getEmail)
            .ifPresent(email -> emailService.send(email, message));
        // Does nothing if: user not found, notifications disabled, no email
    }
}

Part 5: Method References — All Four Types
java// Method references are shorthand for specific lambda patterns
// Lambda: x -> SomeClass.someMethod(x)
// Method ref: SomeClass::someMethod
// SAME semantics, more readable when method name expresses intent

// ── Type 1: Static method reference ──────────────────────────────
// Lambda:        x -> Integer.parseInt(x)
// Method ref:    Integer::parseInt
Function<String, Integer> parse = Integer::parseInt;
parse.apply("42");   // 42

// More examples:
Function<String, String>  toUpper  = String::valueOf;     // Object → String
Predicate<Object>         nonNull  = Objects::nonNull;    // null check
Consumer<Object>          printer  = System.out::println; // print


// ── Type 2: Instance method of a particular instance ──────────────
// Lambda:        x -> someObject.someMethod(x)
// Method ref:    someObject::someMethod
String prefix = "Hello, ";
Function<String, String> greet = prefix::concat;
greet.apply("World");  // "Hello, World"

// Common pattern: pass a specific object's method
Logger logger = LoggerFactory.getLogger(MyClass.class);
Consumer<String> log = logger::info;          // logger.info(x) for each x
list.forEach(logger::info);                   // Print each element


// ── Type 3: Instance method of an arbitrary instance ──────────────
// Lambda:        (x) -> x.someMethod()
// Method ref:    ClassName::someMethod
// The RECEIVER of the method is the PARAMETER
Function<String, String>    upper    = String::toUpperCase;    // s -> s.toUpperCase()
Function<String, Integer>   length   = String::length;         // s -> s.length()
Predicate<String>           isEmpty2 = String::isEmpty;        // s -> s.isEmpty()
Function<List<Integer>, Integer> size = List::size;            // list -> list.size()

upper.apply("hello");   // "HELLO"

// Two-argument: first arg is receiver, second is parameter
BiFunction<String, String, Boolean> startsWith = String::startsWith;
startsWith.apply("hello", "he");  // "hello".startsWith("he") → true

BiFunction<List<Integer>, Integer, Boolean> contains = List::contains;
contains.apply(List.of(1,2,3), 2);  // true


// ── Type 4: Constructor reference ────────────────────────────────
// Lambda:        x -> new SomeClass(x)
// Method ref:    SomeClass::new
Supplier<ArrayList<String>>      listFactory  = ArrayList::new;  // new ArrayList<>()
Function<Integer, ArrayList<?>  > sized        = ArrayList::new;  // new ArrayList<>(size)
BiFunction<String, Integer, Point> pointMaker  = Point::new;      // new Point(x, y)

ArrayList<String> list = listFactory.get();   // new ArrayList<>()
ArrayList<?> sized5 = sized.apply(500);       // new ArrayList<>(500)
Point p = pointMaker.apply(3, 4);             // new Point(3, 4)

// Most common: stream.collect with constructor reference
List<String> names2 = stream.collect(Collectors.toCollection(LinkedList::new));
String[] array = stream.toArray(String[]::new);


// ── When to use method refs vs lambdas ───────────────────────────
// Method ref: when the lambda JUST calls a method with its parameters
list.stream().map(s -> s.toUpperCase())     // Lambda — verbose
list.stream().map(String::toUpperCase)      // Method ref — cleaner ✅

// Lambda: when there's any transformation or logic
list.stream().map(s -> s.trim().toUpperCase())  // Lambda — needed (chain)
list.stream().filter(s -> s.length() > 3)       // Lambda — needed (expression)

// Don't force method refs when they obscure intent:
list.stream().filter(s -> !s.isEmpty())     // Lambda — clearly "not empty" ✅
list.stream().filter(Predicate.not(String::isEmpty))  // Less readable ❌

Part 6: Default Methods — Interface Evolution
java// Java 8 problem: how to add methods to interfaces without
// breaking ALL existing implementations?
// Solution: default methods — interface methods with implementation

interface Collection<E> {
// ...existing methods...

    // New in Java 8: default implementation
    default Stream<E> stream() {
        return StreamSupport.stream(spliterator(), false);
    }

    default void forEach(Consumer<? super E> action) {
        Objects.requireNonNull(action);
        for (E t : this) {
            action.accept(t);
        }
    }
}
// Every class implementing Collection (ArrayList, LinkedList, etc.)
// gets stream() and forEach() for FREE — no code changes needed
// This is how Java 8 added streams to existing collections


// ── Default method rules ──────────────────────────────────────────

interface A {
default String hello() { return "Hello from A"; }
}

interface B {
default String hello() { return "Hello from B"; }
}

// ❌ Conflict: class must override to resolve
class C implements A, B {
@Override
public String hello() {
return A.super.hello();  // Explicitly choose A's implementation
// OR: B.super.hello()
// OR: return "Hello from C"; (own implementation)
}
}

// Class implementation always wins over default:
class D implements A {
@Override
public String hello() { return "Hello from D"; }
// D's hello() overrides A's default — D wins
}

// More specific interface wins over less specific:
interface E extends A {
@Override
default String hello() { return "Hello from E"; }
}

class F implements E, A { }   // No conflict — E is more specific than A
new F().hello();               // "Hello from E" — E wins


// ── Static methods in interfaces (Java 8+) ────────────────────────
interface Validator<T> {
boolean validate(T value);

    // Static factory methods in interface
    static Validator<String> nonEmpty() {
        return s -> s != null && !s.isEmpty();
    }

    static Validator<Integer> positive() {
        return n -> n != null && n > 0;
    }

    // Default composition method
    default Validator<T> and(Validator<T> other) {
        return value -> this.validate(value) && other.validate(value);
    }

    default Validator<T> or(Validator<T> other) {
        return value -> this.validate(value) || other.validate(value);
    }

    default Validator<T> negate() {
        return value -> !this.validate(value);
    }
}

// Usage:
Validator<String> notEmpty    = Validator.nonEmpty();
Validator<String> notTooLong  = s -> s.length() <= 100;
Validator<String> usernameValid = notEmpty.and(notTooLong);

usernameValid.validate("alice");      // true
usernameValid.validate("");           // false (empty)
usernameValid.validate("a".repeat(101));  // false (too long)


// ── Private methods in interfaces (Java 9+) ───────────────────────
interface Logger {
void log(String message);

    default void info(String msg)  { logWithLevel("INFO",  msg); }
    default void warn(String msg)  { logWithLevel("WARN",  msg); }
    default void error(String msg) { logWithLevel("ERROR", msg); }

    // Private: shared logic for default methods (not part of public API)
    private void logWithLevel(String level, String msg) {
        log("[" + level + "] " + LocalDateTime.now() + " - " + msg);
    }
    // Without private methods: would have to either duplicate the formatting logic
    // or make it a default method (exposing it publicly — bad design)
}

Part 7: Java 9-21 Features — The Modern Additions
var — Local Variable Type Inference (Java 10)
java// var: compiler infers type from right-hand side
// ONLY for local variables (not fields, not parameters, not return types)

// ── Before var ────────────────────────────────────────────────────
Map<String, List<Integer>> groupedData = new HashMap<String, List<Integer>>();
Entry<String, List<Integer>> entry = groupedData.entrySet().iterator().next();

// ── With var ──────────────────────────────────────────────────────
var groupedData2 = new HashMap<String, List<Integer>>();  // Still HashMap<String, List<Integer>>
var entry2 = groupedData2.entrySet().iterator().next();   // Entry<String, List<Integer>>

// ✅ Good use of var: type obvious from right-hand side
var users = new ArrayList<User>();           // Clearly ArrayList<User>
var response = httpClient.send(request);    // Return type is obvious
var conn = dataSource.getConnection();      // Connection — clear from method name

// ❌ Bad use: type not obvious
var result = process(data);     // What is result? What type does process return?
var x = compute();              // x is what exactly?

// var doesn't change type safety — it's compile-time inference:
var number = 42;        // int — not Object, not Number — EXACTLY int
number = "hello";       // ❌ Compile error: int ≠ String

// var with diamond operator:
var list = new ArrayList<>();   // ❌ ArrayList<Object> — infers Object! Useless.
var list2 = new ArrayList<String>();  // ✅ ArrayList<String>

// var in enhanced for loops (Java 10):
for (var user : userList) {    // user is User — same as explicit type
process(user);
}

// var with lambda (NOT ALLOWED):
var fn = (String s) -> s.length();  // ❌ Compile error
// Lambdas need a target type — var can't provide it
Records (Java 16)
java// Records: concise immutable data classes
// Automatically generates: constructor, getters, equals, hashCode, toString

// Before records:
class PersonOld {
private final String name;
private final int    age;

    PersonOld(String name, int age) {
        this.name = name;
        this.age  = age;
    }

    String getName() { return name; }
    int    getAge()  { return age;  }

    @Override public boolean equals(Object o) { ... }  // 10 lines
    @Override public int     hashCode()       { ... }  // 5 lines
    @Override public String  toString()       { ... }  // 3 lines
}
// Total: ~30 lines

// With record:
record Person(String name, int age) { }   // ONE LINE — same behavior!

// Using the record:
Person alice = new Person("Alice", 30);
alice.name();          // "Alice" — accessor (note: name(), not getName()!)
alice.age();           // 30
alice.equals(new Person("Alice", 30));  // true — value-based equality
alice.toString();      // "Person[name=Alice, age=30]"


// ── Compact canonical constructor ────────────────────────────────
record Range(int min, int max) {
// Compact constructor: validation without repeating field assignments
Range {
if (min > max) throw new IllegalArgumentException(
"min " + min + " > max " + max);
// No need to write: this.min = min; this.max = max;
// Happens automatically AFTER compact constructor body
}
}

// ── Custom methods in records ─────────────────────────────────────
record Money(BigDecimal amount, String currency) {
Money {
Objects.requireNonNull(amount, "amount");
Objects.requireNonNull(currency, "currency");
if (amount.compareTo(BigDecimal.ZERO) < 0)
throw new IllegalArgumentException("Amount cannot be negative");
}

    Money add(Money other) {
        if (!this.currency.equals(other.currency))
            throw new IllegalArgumentException("Currency mismatch");
        return new Money(this.amount.add(other.amount), this.currency);
    }

    boolean isZero() { return amount.compareTo(BigDecimal.ZERO) == 0; }

    static Money of(String amount, String currency) {
        return new Money(new BigDecimal(amount), currency);
    }
}

// Records can implement interfaces
record Point(double x, double y) implements Comparable<Point> {
@Override
public int compareTo(Point other) {
return Double.compare(
Math.hypot(this.x, this.y),
Math.hypot(other.x, other.y)
);
}
}
Sealed Classes (Java 17)
java// Sealed classes: restrict which classes can extend/implement
// Works with pattern matching for exhaustive handling

// Before sealed: anyone could subclass Shape
// After sealed: ONLY listed classes can extend Shape

public sealed interface Shape
permits Circle, Rectangle, Triangle {
double area();
}

public record Circle(double radius) implements Shape {
public double area() { return Math.PI * radius * radius; }
}

public record Rectangle(double width, double height) implements Shape {
public double area() { return width * height; }
}

public record Triangle(double base, double height) implements Shape {
public double area() { return 0.5 * base * height; }
}

// The power: EXHAUSTIVE pattern matching (Java 21)
String describe(Shape shape) {
return switch (shape) {
case Circle c    -> "Circle with radius " + c.radius();
case Rectangle r -> "Rectangle " + r.width() + "×" + r.height();
case Triangle t  -> "Triangle base=" + t.base();
// NO default needed! Compiler KNOWS these are all possibilities
// If you add a new subclass to permits: compiler forces you to handle it
};
}

// ── non-sealed: opt out of sealing downstream ────────────────────
public sealed interface Animal permits Dog, Cat, OtherAnimal { }
public record Dog(String name) implements Animal { }
public record Cat(String name) implements Animal { }
public non-sealed class OtherAnimal implements Animal { }
// OtherAnimal: anyone can subclass (breaks sealing for that branch)
Pattern Matching (Java 16-21)
java// ── instanceof pattern matching (Java 16) ────────────────────────
// Before:
Object obj = "hello";
if (obj instanceof String) {
String s = (String) obj;   // Redundant cast
System.out.println(s.length());
}

// After:
if (obj instanceof String s) {     // Pattern variable 's' — no cast needed
System.out.println(s.length()); // s is String, scoped to if-block
}

// Guards (Java 21):
if (obj instanceof String s && s.length() > 3) {
System.out.println(s.toUpperCase());
}


// ── Switch pattern matching (Java 21) ─────────────────────────────
// The most powerful form — replaces instanceof chains

Object value = getValue();
String result = switch (value) {
case Integer i when i < 0  -> "negative: " + i;
case Integer i             -> "positive int: " + i;
case String s when s.isBlank() -> "blank string";
case String s              -> "string: " + s;
case null                  -> "null value";
default                    -> "other: " + value;
};

// With sealed classes — exhaustive (no default needed):
double area = switch (shape) {
case Circle c    -> Math.PI * c.radius() * c.radius();
case Rectangle r -> r.width() * r.height();
case Triangle t  -> 0.5 * t.base() * t.height();
};

// Deconstruction patterns (Java 21):
if (point instanceof Point(double x, double y) && x > 0 && y > 0) {
System.out.println("First quadrant: " + x + ", " + y);
}

Part 8: The Interview Q&A Round 🎤

Q1. What is the difference between map and flatMap in streams?

"map is a one-to-one transformation — each input element produces exactly one output element. You give it a Function<T, R> and get back a Stream<R>. The stream size stays the same.
flatMap is a one-to-many transformation — each input element produces ZERO OR MORE output elements. You give it a Function<T, Stream<R>> — the function returns a Stream for each element — and flatMap flattens all those streams into one. The resulting stream can be smaller or larger than the input.
Classic example: you have List<Order> and each Order has List<OrderItem>. map(Order::getItems) gives you Stream<List<OrderItem>> — nested. flatMap(o -> o.getItems().stream()) gives you Stream<OrderItem> — flat. You use flatMap whenever your mapping function returns a collection or stream and you want to work on the elements, not the collections."


Q2. What is lazy evaluation in streams and why does it matter?
java// Lazy evaluation: intermediate operations don't execute until
// a terminal operation is called

// Example that proves laziness:
Stream<String> stream = Stream.of("a", "bb", "ccc", "dddd")
.filter(s -> {
System.out.println("filter: " + s);   // When does this print?
return s.length() > 1;
})
.map(s -> {
System.out.println("map: " + s);       // When does this print?
return s.toUpperCase();
});

System.out.println("Stream created — nothing printed yet!");
// Output so far: just "Stream created" — filter and map haven't run!

String first = stream.findFirst().get();  // Terminal operation fires pipeline
// Output:
// filter: a     ← checks "a", length 1 → filtered out
// filter: bb    ← checks "bb", length 2 → passes
// map: bb        ← maps to "BB"
// (findFirst short-circuits — "ccc" and "dddd" never processed!)

// WHY IT MATTERS:
// Without laziness: each operation creates intermediate collections
//   filter all N → new list → map all filtered → new list → findFirst
//   Even if you only need 1 element: processed ALL N elements!

// With laziness:
//   Elements flow through ONE AT A TIME
//   Short-circuit terminals (findFirst, anyMatch, limit) stop early
//   Can process infinite streams: Stream.iterate(0, n -> n + 1)
//                                      .filter(n -> n % 2 == 0)
//                                      .findFirst()
//   Without laziness: infinite stream → infinite loop!

Q3. When should you use Optional and when should you not?

"Optional should be used as a return type when a method might legitimately not return a value — findById(), findByEmail(), getMiddleName(). It makes the absence explicit in the API contract, forces callers to consider the absent case, and enables a clean pipeline style with map, flatMap, orElse.
Don't use Optional as a field type — it's not Serializable, signals unclear lifecycle, and @Nullable annotation is more appropriate for fields. Don't use it as a method parameter — just use overloading or @Nullable. Don't put Optional in collections — just filter out absent values instead.
The biggest anti-pattern: if (opt.isPresent()) { opt.get(); } — that's just null checking with extra steps. Use opt.map(...).orElse(...) to get the benefit. Also, orElse(expr) always evaluates expr even if Optional is present — use orElseGet(lambda) for expensive defaults."


Q4. What is the difference between Comparator.comparing and implementing Comparable?
java// Comparable: natural ordering (ONE ordering, baked into the class)
// Comparator: external ordering (MANY orderings, defined outside)

// Comparable: class implements it — defines natural order
class Employee implements Comparable<Employee> {
private String name;
private int    salary;

    @Override
    public int compareTo(Employee other) {
        return this.name.compareTo(other.name);  // Natural order = by name
    }
}

// Sort uses natural order automatically:
List<Employee> employees = getEmployees();
Collections.sort(employees);         // Uses Comparable → sorts by name
employees.sort(null);                // null Comparator → natural order


// Comparator: external — define any number of orderings
Comparator<Employee> bySalary    = Comparator.comparing(Employee::getSalary);
Comparator<Employee> byName      = Comparator.comparing(Employee::getName);
Comparator<Employee> bySalaryDesc = bySalary.reversed();
Comparator<Employee> byNameThenSalary = byName.thenComparing(bySalary);

employees.sort(bySalary);           // Sort by salary
employees.sort(byNameThenSalary);   // Sort by name, then by salary for ties

// Comparator.comparing with key extractor:
// employees.sorted(Comparator.comparing(Employee::getDepartment)
//                            .thenComparing(Employee::getLevel)
//                            .thenComparing(Comparator.comparing(Employee::getSalary).reversed()))

// When to use each:
// Comparable: when there's ONE obvious natural ordering (String, Integer, LocalDate)
// Comparator: when you need MULTIPLE orderings, or can't modify the class,
//             or the ordering is external/contextual
```

---

**Q5. What is the difference between `stream()` and `parallelStream()`? When is parallel actually faster?**

> *"Both return a `Stream<T>` and support the same operations. `stream()` is sequential — elements processed one at a time in a single thread. `parallelStream()` splits the source, processes splits in parallel using `ForkJoinPool.commonPool()`, and merges results.*
>
> *Parallel is actually faster when: the data source is large (10,000+ elements to amortize split/merge overhead), the per-element work is CPU-intensive (not I/O bound), there's no shared mutable state, and the source supports efficient splitting (ArrayList, arrays — good; LinkedList, Streams generated from iterators — poor).*
>
> *Parallel is slower or wrong when: data is small (fork overhead exceeds savings), work is I/O bound (threads block anyway, not a parallelism problem — use CompletableFuture instead), there's shared mutable state (race conditions), or you use `findFirst` on unordered data (forces sequential-style ordering overhead).*
>
> *The practical rule: profile first. Most people assume parallel is faster and get surprised. `parallelStream()` on a list of 10 elements is measurably slower than `stream()`. JMH benchmarks before committing to parallel."*

---

**Q6. Explain how records differ from regular classes and when to use them**

> *"Records are a special class form for pure data carriers — their purpose is to hold a fixed set of fields and nothing else. The compiler auto-generates the canonical constructor (sets all fields), accessor methods named after the fields (not `getX()`, just `x()`), and value-based `equals`, `hashCode`, and `toString` based on all components.*
>
> *Key differences from regular classes: records are implicitly final (can't be subclassed), all fields are implicitly `final` (immutable), and you can't add instance fields beyond the record components. You CAN add methods, implement interfaces, define static fields, and customize the canonical constructor with validation logic using the compact form.*
>
> *Use records for: DTOs between layers, value objects (`Money`, `Point`, `Range`), return values carrying multiple pieces of data, and anywhere you'd otherwise write a simple immutable class with just fields and accessors. Don't use records when: you need inheritance, mutable state, different field names than accessors suggest, or the object has complex behavior beyond its data.*
>
> *Records also work perfectly with sealed classes and switch pattern matching — a sealed interface with record implementors gives you exhaustive, type-safe dispatch with zero boilerplate."*

---

## Section 11 Master Summary 🧠
```
LAMBDA EXPRESSIONS:
invokedynamic → LambdaMetafactory at first call (not class per lambda)
Stateless lambdas: may share single instance
Capturing lambdas: new instance per captured context
Local var capture: must be effectively final (copied at capture time)
Instance fields: captured via 'this' — can see later changes

FUNCTIONAL INTERFACES (core 4):
Supplier<T>:         () → T         (produce)
Consumer<T>:         T → void       (consume)
Function<T,R>:       T → R          (transform)
Predicate<T>:        T → boolean    (test)
Composition: and/or/negate (Predicate), andThen/compose (Function)
Primitive variants: IntFunction, ToIntFunction, IntUnaryOperator (avoid boxing!)

STREAMS:
Pipeline: source → [intermediate ops] → terminal op
LAZY: nothing executes until terminal operation
Short-circuit: findFirst/anyMatch/limit stop early
Stateless: filter, map, flatMap (process element-by-element)
Stateful: sorted, distinct, limit (may need all elements)

map vs flatMap:
map:     T → R       (one to one)
flatMap: T → Stream<R> (one to many, flattens)

Parallel: ✅ large data, CPU-intensive, no shared state, splittable source
❌ small data, I/O-bound, shared mutable state, LinkedList

COLLECTORS:
groupingBy → Map<K, List<V>>
partitioningBy → Map<Boolean, List<V>>
toMap → Map (throws on duplicate keys — provide merge fn!)
joining → String
counting/averagingInt/summaryStatistics → aggregate
teeing (Java 12) → two collectors, one pass

OPTIONAL:
Return type for "value may be absent" — NOT field/parameter type
Pipeline API: map, flatMap, filter, or, stream()
orElse(val) → ALWAYS evaluates val (eager)
orElseGet(fn) → evaluates fn ONLY if empty (lazy)
Anti-patterns: isPresent()+get(), get() alone, Optional in fields/collections

METHOD REFERENCES (4 types):
1. Static:                  Integer::parseInt       (x → Class.method(x))
2. Specific instance:       logger::info            (x → obj.method(x))
3. Arbitrary instance:      String::toUpperCase     (x → x.method())
4. Constructor:             ArrayList::new          (x → new Class(x))

DEFAULT METHODS:
Allow interface evolution without breaking implementors
Class implementation wins over default
More specific interface wins over less specific
Conflict (two defaults): must override explicitly, use A.super.method()
Private methods (Java 9): shared logic for default methods

MODERN JAVA (9-21):
var (10):      local variable type inference — use when type is obvious
records (16):  immutable data classes — auto constructor/equals/hashCode/toString
sealed (17):   restrict permitted subclasses — enables exhaustive switches
pattern (21):  switch on types with guards — instanceof without cast

KEY STREAM COLLECTORS INTERVIEW POINTS:
"groupingBy(key, downstream)" for nested aggregation
"toMap with merge fn" to handle duplicate keys
"flatMap" to flatten nested collections
"Collectors.teeing" for two aggregations in one pass
"Comparator.comparing().thenComparing()" for multi-level sort