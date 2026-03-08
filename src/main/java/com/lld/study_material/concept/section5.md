
Section 5: Advanced LLD Concepts 🧠
This section separates good engineers from great engineers. These concepts don't have flashy pattern names — but interviewers at FAANG use them to test if you think like a senior engineer or just memorize patterns.

PART 1: DRY, KISS, YAGNI — The Three Laws 📜
These three principles govern every line of code you write. Together they answer one question: "Is this code simple, focused, and non-repetitive?"

DRY — Don't Repeat Yourself

"Every piece of knowledge must have a single, unambiguous, authoritative representation within a system."
— Andrew Hunt & David Thomas, The Pragmatic Programmer

What DRY really means
DRY is NOT just about copy-pasted code. It's about duplicated knowledge — the same business rule expressed in two places. If the rule changes, you must update both. You will forget one. That's a bug.
The 3 Levels of DRY Violation
Level 1: Copy-Pasted Code (obvious, easy to spot)
Level 2: Duplicated Logic (same algorithm, different variable names)
Level 3: Duplicated Knowledge (same business rule in DB schema + code + config)
java// ─── LEVEL 1: Copy-Pasted Code ───────────────────────────
// ❌ VIOLATION
class OrderService {
public boolean isEligibleForDiscount(User user) {
return user.getOrderCount() >= 10
&& user.getTotalSpend() >= 1000.0
&& user.getAccountAge().toYears() >= 1;
}
}

class CartService {
public double applyDiscountIfEligible(User user, double amount) {
// Same 3 conditions — EXACT copy from OrderService
if (user.getOrderCount() >= 10
&& user.getTotalSpend() >= 1000.0
&& user.getAccountAge().toYears() >= 1) {
return amount * 0.9;  // 10% off
}
return amount;
}
}

class RecommendationService {
public List<Product> getPremiumRecommendations(User user) {
// SAME logic AGAIN — 3rd copy
boolean isPremium = user.getOrderCount() >= 10
&& user.getTotalSpend() >= 1000.0
&& user.getAccountAge().toYears() >= 1;
// ...
}
}
// If threshold changes from 10 orders to 15 orders:
// You must find and update ALL 3 places. Miss one = bug.
java// ✅ DRY FIX: Single authoritative source
class UserEligibilityService {
// ONE place. ONE truth. ONE update point.
private static final int    MIN_ORDERS     = 10;
private static final double MIN_SPEND      = 1000.0;
private static final int    MIN_YEARS      = 1;

    public boolean isEligibleForDiscount(User user) {
        return user.getOrderCount()            >= MIN_ORDERS
            && user.getTotalSpend()            >= MIN_SPEND
            && user.getAccountAge().toYears()  >= MIN_YEARS;
    }

    public boolean isPremiumUser(User user) {
        return isEligibleForDiscount(user);  // Reuse — same concept
    }
}

// All services now DELEGATE — no duplication
class OrderService {
private UserEligibilityService eligibility;
public boolean canApplyDiscount(User user) {
return eligibility.isEligibleForDiscount(user);
}
}

class CartService {
private UserEligibilityService eligibility;
public double applyDiscount(User user, double amount) {
return eligibility.isEligibleForDiscount(user) ? amount * 0.9 : amount;
}
}
java// ─── LEVEL 2: Duplicated Logic ───────────────────────────
// ❌ VIOLATION — Same algorithm, different variable names
class TemperatureConverter {
public double fahrenheitToCelsius(double f) {
return (f - 32) * 5.0 / 9.0;         // Algorithm A
}

    public double kelvinToCelsius(double k) {
        return k - 273.15;                     // Algorithm B
    }

    public double fahrenheitToKelvin(double f) {
        double celsius = (f - 32) * 5.0 / 9.0;  // Algorithm A REPEATED
        return celsius + 273.15;                  // Reversal of B
    }
}

// ✅ FIX — Compose existing methods, don't repeat logic
class TemperatureConverter {
public double fahrenheitToCelsius(double f) { return (f - 32) * 5.0 / 9.0; }
public double celsiusToKelvin(double c)     { return c + 273.15; }
public double kelvinToCelsius(double k)     { return k - 273.15; }

    // Compose — no duplication
    public double fahrenheitToKelvin(double f)  {
        return celsiusToKelvin(fahrenheitToCelsius(f));  // Reuse!
    }
    public double kelvinToFahrenheit(double k)  {
        return celsiusToFahrenheit(kelvinToCelsius(k));  // Reuse!
    }
}
java// ─── LEVEL 3: Knowledge Duplication (hardest to spot) ────
// ❌ VIOLATION — Same MAX_RETRIES in 3 different places
class HttpClient {
private int maxRetries = 3;  // Hardcoded
}

class DatabasePool {
private static final int MAX_RECONNECT = 3;  // Same 3, different name
}

// application.properties
// retry.count=3  ← and ALSO in config!

// ✅ FIX — Single source of truth
// application.properties
// system.retry.max=3

class RetryConfig {
private final int maxRetries;
public RetryConfig(@Value("${system.retry.max}") int maxRetries) {
this.maxRetries = maxRetries;
}
public int getMaxRetries() { return maxRetries; }
}

// ALL classes inject RetryConfig — one place to change
class HttpClient {
private RetryConfig retryConfig;
// Uses retryConfig.getMaxRetries() — single source
}
How to say DRY in an interview:

"DRY means every business rule has exactly ONE home in the codebase. When I review PRs, I look for the same condition or algorithm appearing in multiple places. The fix isn't just extracting a method — it's asking 'what KNOWLEDGE is being duplicated?' and creating the right abstraction to own it. DRY violations are why bugs survive after 'fixes' — the developer fixed one copy but not the others."


KISS — Keep It Simple, Stupid

"Simplicity should be a key goal in design, and unnecessary complexity should be avoided."

What KISS really means
The simplest solution that correctly solves the problem is always the best starting point. Every additional layer of complexity must earn its place by solving a real problem.
java// ─── KISS VIOLATION: Over-engineered solution ─────────────
// ❌ Someone read too many design pattern books...
interface StringTransformer {
String transform(String input);
}

abstract class AbstractStringTransformerFactory {
public abstract StringTransformer createTransformer();
}

class UpperCaseTransformerFactory extends AbstractStringTransformerFactory {
public StringTransformer createTransformer() {
return new StringTransformer() {
public String transform(String input) {
return input.toUpperCase();
}
};
}
}

class TransformerStrategy {
private AbstractStringTransformerFactory factory;
public TransformerStrategy(AbstractStringTransformerFactory f) { this.factory = f; }
public String execute(String input) {
return factory.createTransformer().transform(input);
}
}

// Just to... convert a string to uppercase. 5 classes.
String result = new TransformerStrategy(new UpperCaseTransformerFactory())
.execute("hello");
java// ✅ KISS: The simplest thing that works
String result = "hello".toUpperCase();

// When do you add complexity? When you have a REAL reason:
// - Multiple different transformers swapped at runtime? → Strategy pattern
// - Creating objects is expensive? → Factory pattern
// - Need to log every transformation? → Decorator
// But not until that need is REAL.
java// ─── KISS in practice: Validation logic ──────────────────
// ❌ Over-engineered validation framework nobody asked for
class ValidationRule<T> {
private Predicate<T> condition;
private String errorMessage;
private int severity;
private List<ValidationRule<T>> dependsOn;
// ... 200 lines of validation framework
}

class ValidationChain<T> {
private List<ValidationRule<T>> rules;
public ValidationResult validate(T input) { /* ... */ return null; }
}

// vs.

// ✅ KISS — straightforward validation
class UserValidator {
public void validate(User user) {
if (user.getName() == null || user.getName().isBlank())
throw new ValidationException("Name is required");
if (user.getEmail() == null || !user.getEmail().contains("@"))
throw new ValidationException("Valid email is required");
if (user.getAge() < 18)
throw new ValidationException("Must be 18 or older");
// Add complexity ONLY when it's genuinely needed
}
}
KISS and performance — a real interview trap:
java// ❌ "Clever" but unreadable
int result = (n & 1) == 0 ? n >> 1 : (n * 3 + 1);

// ✅ KISS — readable, same performance on modern JVM
int result = (n % 2 == 0) ? n / 2 : (n * 3 + 1);

// Premature micro-optimization violates KISS.
// Profile FIRST. Optimize ONLY proven bottlenecks.
How to say KISS in an interview:

"My default is: what's the simplest correct solution? I don't add abstractions or patterns speculatively. If a problem takes 20 lines to solve simply, I write 20 lines. I add complexity only when I have a concrete reason — a second implementation, a need for testing isolation, a real extensibility requirement. Complexity has a cost: harder to read, harder to debug, harder to onboard new developers. I make complexity pay its way."


YAGNI — You Aren't Gonna Need It

"Always implement things when you actually need them, never when you just foresee that you might need them."
— Ron Jeffries (XP co-founder)

YAGNI + OCP — the tension you must address
java// ❌ YAGNI violation: Building for hypothetical futures
class PaymentService {
// Real requirement: support Credit Card and PayPal
// "Future proofing" nobody asked for:

    public void payByCreditCard()  { /* ✅ needed */ }
    public void payByPayPal()      { /* ✅ needed */ }
    public void payByCrypto()      { /* ❌ no user story exists */ }
    public void payByBarter()      { /* ❌ seriously? */ }
    public void payByMindTransfer(){ /* ❌ Elon asked? */ }

    // Also YAGNI violations:
    private Map<String, Object> futureExtensionHooks;  // "just in case"
    protected abstract void onPaymentProcessedHook();  // "might need"
    // These add complexity WITH ZERO current value
}
java// ✅ YAGNI + OCP working together
// Now: implement exactly what's needed
interface PaymentStrategy {
boolean pay(double amount);
}

class CreditCardPayment implements PaymentStrategy { /* ✅ */ }
class PayPalPayment      implements PaymentStrategy { /* ✅ */ }

// When crypto is ACTUALLY requested as a user story:
class CryptoPayment implements PaymentStrategy { /* Add then, not now */ }

// OCP ensures adding it later is CHEAP — new class, no existing changes.
// YAGNI says: don't write CryptoPayment until it's on the backlog.
// This is the healthy tension: OCP prepares the STRUCTURE,
// YAGNI prevents PREMATURE implementation.
The YAGNI trap in interviews:
java// Interviewer says: "Design a notification system for Email and SMS"
// YAGNI violation: Candidate designs for 15 notification types they imagined
// YAGNI correct: Design for Email + SMS cleanly, with extensible structure

// ✅ Correct approach in interview:
// "I'll design for Email and SMS as required.
//  I'll use the Strategy pattern so adding Push or WhatsApp later
//  requires zero changes to existing code — just new classes.
//  But I won't BUILD those classes now since they're not in scope."
```

---

# PART 2: Coupling and Cohesion 🔗

> *"The goal is LOW coupling and HIGH cohesion. Everything in SOLID is ultimately in service of this goal."*

---

## Coupling — How Dependent Classes Are

**Coupling** = the degree to which one class KNOWS ABOUT and DEPENDS ON another class.

### Types of Coupling (from worst to best)
```
Content Coupling  → One class modifies another's internal data directly   ← WORST
Common Coupling   → Both classes share global state
Control Coupling  → One class controls flow of another via flags/switches
Data Coupling     → Classes share only necessary data via parameters       ← BEST
java// ─── TIGHT COUPLING examples ─────────────────────────────

// ❌ Content Coupling — directly accessing another's internals
class OrderProcessor {
public void process(Order order) {
// Directly touching Order's private field — horrifying
order.items.clear();        // Accessing private field!
order.status = "PROCESSED"; // Directly setting private field!
}
}

// ❌ Common Coupling — shared mutable global state
class AppState {
public static List<User> loggedInUsers = new ArrayList<>(); // Global!
public static double currentTaxRate = 0.18;                 // Mutable global!
}

class OrderService {
public double calculateTax(double amount) {
return amount * AppState.currentTaxRate; // Depends on global mutable state
// If someone changes AppState.currentTaxRate anywhere, this breaks
}
}

// ❌ Control Coupling — flags controlling behavior
class ReportGenerator {
// The 'type' flag makes caller control internal flow — tight coupling
public String generate(Report report, boolean isPDF, boolean isCSV, boolean isExcel) {
if (isPDF)    return generatePDF(report);
if (isCSV)    return generateCSV(report);
if (isExcel)  return generateExcel(report);
return generateText(report);
}
}
java// ✅ Loose Coupling — depend on abstractions, communicate via interfaces

// Data Coupling — pass only what's needed
class TaxCalculator {
private final double taxRate;  // Injected, not global

    public TaxCalculator(double taxRate) { this.taxRate = taxRate; }
    public double calculate(double amount) { return amount * taxRate; }
    // Only knows about 'amount' — pure data coupling
}

// Decoupled Report Generation — Strategy pattern
interface ReportFormatter {
String format(Report report);
}

class PDFFormatter   implements ReportFormatter { public String format(Report r) { ... } }
class CSVFormatter   implements ReportFormatter { public String format(Report r) { ... } }
class ExcelFormatter implements ReportFormatter { public String format(Report r) { ... } }

class ReportGenerator {
private ReportFormatter formatter;  // Depends on interface, not concrete type

    public ReportGenerator(ReportFormatter formatter) { this.formatter = formatter; }
    public String generate(Report report) { return formatter.format(report); }
    // Zero control coupling — caller picks formatter, this class just calls it
}
Measuring Coupling — what interviewers really want to know:
java// Ask these questions about any class:
// 1. How many OTHER classes does it directly reference?    (fewer = better)
// 2. Does it use concrete classes or interfaces?           (interfaces = better)
// 3. Does it create its dependencies or receive them?      (receive = better)
// 4. If I change class A, how many others break?          (fewer = better)

// Afferent Coupling  (Ca) = How many classes DEPEND ON this class
//   High Ca = risky to change (many dependents will break)
// Efferent Coupling  (Ce) = How many classes THIS class depends on
//   High Ce = fragile (depends on many things that could change)
// Instability = Ce / (Ca + Ce)  → 0 = stable, 1 = unstable
```

---

## Cohesion — How Focused a Class Is

**Cohesion** = the degree to which the elements INSIDE a class belong together and serve the same purpose.

### Types of Cohesion (from worst to best)
```
Coincidental Cohesion → Methods grouped randomly (Utils class with everything)
Logical Cohesion      → Methods grouped by category (all "string" operations)
Temporal Cohesion     → Methods run at the same time (startup initialization)
Functional Cohesion   → All methods work toward ONE well-defined purpose ← BEST
java// ─── LOW COHESION: The God Class ──────────────────────────
// ❌ Coincidental Cohesion — "Utility dumping ground"
class Utils {
public static String formatDate(Date d) { ... }          // Date utility
public static boolean isValidEmail(String e) { ... }     // Validation
public static double calculateTax(double amount) { ... } // Finance
public static void sendEmail(String to, String body) { } // Communication
public static List<User> sortUsers(List<User> users) { } // Collections
public static byte[] compress(byte[] data) { ... }       // Compression
public static String generateUUID() { ... }              // ID generation
// 0 cohesion — these methods share NOTHING in common
// If you have a Utils class, this is almost always what it looks like
}
java// ✅ HIGH COHESION: Each class has one clear purpose
class DateFormatter {
public String format(Date date, String pattern) { ... }
public Date parse(String dateStr, String pattern) { ... }
public String toISO8601(Date date) { ... }
// Every method: date formatting. Pure functional cohesion.
}

class EmailValidator {
public boolean isValid(String email) { ... }
public boolean hasValidDomain(String email) { ... }
public List<String> extractEmails(String text) { ... }
// Every method: email validation. Cohesive.
}

class TaxCalculator {
public double calculateIncome(double income) { ... }
public double calculateGST(double amount) { ... }
public double calculateCapitalGains(double profit) { ... }
// Every method: tax calculation. Cohesive.
}
java// ─── Cohesion in practice: Identifying split points ───────
// ❌ Low cohesion — mixed responsibilities
class UserService {
// Group 1: User CRUD — cohesive within itself
public User createUser(String name, String email) { ... }
public User findById(String id) { ... }
public void updateEmail(String id, String email) { ... }
public void deleteUser(String id) { ... }

    // Group 2: Authentication — DIFFERENT responsibility!
    public String login(String email, String password) { ... }
    public void logout(String sessionId) { ... }
    public boolean validateToken(String jwt) { ... }

    // Group 3: Email sending — YET ANOTHER responsibility!
    public void sendWelcomeEmail(User user) { ... }
    public void sendPasswordResetEmail(User user) { ... }
    public void sendAccountSummary(User user) { ... }
}
// This class has 3 distinct responsibility groups = 3 reasons to change
java// ✅ High cohesion — each class focused
class UserRepository {
public User save(User user) { ... }
public User findById(String id) { ... }
public void delete(String id) { ... }
// Only: data persistence for Users
}

class AuthService {
public String login(String email, String password) { ... }
public void logout(String sessionId) { ... }
public boolean validateToken(String jwt) { ... }
// Only: authentication
}

class UserEmailService {
public void sendWelcome(User user) { ... }
public void sendPasswordReset(User user) { ... }
public void sendSummary(User user) { ... }
// Only: user-related emails
}
```

### The Rule of Thumb:
```
LOW coupling  + HIGH cohesion = Easy to change, easy to test, easy to understand
HIGH coupling + LOW cohesion  = The legacy codebase everyone is afraid to touch

PART 3: Immutability 🔒

"Immutable objects are simpler to construct, test, and use. They are thread-safe by nature. They make better map keys and set elements."


Why Immutability Matters in Interviews
Interviewers ask about immutability because it touches:

Thread safety (no synchronization needed)
Object identity vs equality
Defensive copying
Value Objects in Domain-Driven Design

java// ─── The Mutability Bug ───────────────────────────────────
class Schedule {
private List<LocalDate> holidays;

    public Schedule(List<LocalDate> holidays) {
        this.holidays = holidays;  // ← DANGER: storing reference!
    }

    public List<LocalDate> getHolidays() {
        return holidays;  // ← DANGER: exposing reference!
    }

    public boolean isHoliday(LocalDate date) {
        return holidays.contains(date);
    }
}

// Usage reveals the bug:
List<LocalDate> dates = new ArrayList<>();
dates.add(LocalDate.of(2024, 12, 25));

Schedule schedule = new Schedule(dates);

// Caller mutates the list AFTER passing it in
dates.add(LocalDate.of(2024, 1, 1));  // Now schedule sees NEW YEAR too!
// schedule.isHoliday(Jan 1) = TRUE — but Schedule never agreed to this!

// Or:
List<LocalDate> h = schedule.getHolidays();
h.clear();  // Cleared the schedule's internal state!
// schedule.isHoliday(Dec 25) = FALSE now — object corrupted!
java// ─── Rules for Immutable Class ────────────────────────────
public final class ImmutableSchedule {        // Rule 1: final class
private final Set<LocalDate> holidays;    // Rule 2: final fields

    public ImmutableSchedule(Set<LocalDate> holidays) {
        // Rule 3: Defensive copy in constructor
        this.holidays = Collections.unmodifiableSet(new HashSet<>(holidays));
    }

    // Rule 4: No setters
    // Rule 5: Defensive copy in getters for mutable fields
    public Set<LocalDate> getHolidays() {
        return holidays;  // Already unmodifiable — safe to return
    }

    public boolean isHoliday(LocalDate date) {
        return holidays.contains(date);
    }

    // Rule 6: "Mutation" creates a NEW object
    public ImmutableSchedule addHoliday(LocalDate date) {
        Set<LocalDate> newHolidays = new HashSet<>(holidays);
        newHolidays.add(date);
        return new ImmutableSchedule(newHolidays);  // New object — original unchanged
    }

    public ImmutableSchedule removeHoliday(LocalDate date) {
        Set<LocalDate> newHolidays = new HashSet<>(holidays);
        newHolidays.remove(date);
        return new ImmutableSchedule(newHolidays);
    }
}

// Now safe:
Set<LocalDate> dates = new HashSet<>();
dates.add(LocalDate.of(2024, 12, 25));

ImmutableSchedule schedule = new ImmutableSchedule(dates);
dates.add(LocalDate.of(2024, 1, 1));  // Doesn't affect schedule!
// schedule.isHoliday(Jan 1) = FALSE — protected!

ImmutableSchedule withNewYear = schedule.addHoliday(LocalDate.of(2025, 1, 1));
// schedule unchanged, withNewYear is new object
java// ─── Value Objects — DDD concept interviewers love ─────
// A Value Object is defined by its VALUE, not its identity.
// Two Money objects with same amount+currency ARE equal.
// Money is a perfect immutable Value Object.

public final class Money {
private final BigDecimal amount;   // BigDecimal, not double (precision!)
private final String currency;

    public Money(BigDecimal amount, String currency) {
        if (amount == null)    throw new IllegalArgumentException("Amount required");
        if (amount.signum() < 0) throw new IllegalArgumentException("Amount cannot be negative");
        if (currency == null || currency.length() != 3)
            throw new IllegalArgumentException("Valid 3-letter currency code required");

        this.amount   = amount.setScale(2, RoundingMode.HALF_UP);  // Normalize
        this.currency = currency.toUpperCase();
    }

    // Operations return NEW Money objects
    public Money add(Money other) {
        assertSameCurrency(other);
        return new Money(this.amount.add(other.amount), this.currency);
    }

    public Money subtract(Money other) {
        assertSameCurrency(other);
        BigDecimal result = this.amount.subtract(other.amount);
        if (result.signum() < 0) throw new ArithmeticException("Result would be negative");
        return new Money(result, this.currency);
    }

    public Money multiply(double factor) {
        return new Money(this.amount.multiply(BigDecimal.valueOf(factor)), this.currency);
    }

    public boolean isGreaterThan(Money other) {
        assertSameCurrency(other);
        return this.amount.compareTo(other.amount) > 0;
    }

    private void assertSameCurrency(Money other) {
        if (!this.currency.equals(other.currency))
            throw new IllegalArgumentException(
                "Currency mismatch: " + this.currency + " vs " + other.currency);
    }

    // Value equality — not reference equality
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Money)) return false;
        Money money = (Money) o;
        return amount.compareTo(money.amount) == 0  // compareTo for BigDecimal!
            && currency.equals(money.currency);
    }

    @Override
    public int hashCode() { return Objects.hash(amount, currency); }

    @Override
    public String toString() { return currency + " " + amount.toPlainString(); }
}

// Usage
Money price    = new Money(new BigDecimal("99.99"), "USD");
Money discount = new Money(new BigDecimal("10.00"), "USD");
Money total    = price.subtract(discount);  // USD 89.99 — new object!

// Can safely use as HashMap key — equals + hashCode consistent
Map<Money, Product> priceMap = new HashMap<>();
priceMap.put(price, product);
priceMap.get(new Money(new BigDecimal("99.99"), "USD")); // Works! Value equality.

PART 4: Clean Code Principles 🧹

"Clean code is code that a developer can read, understand, and modify quickly. It expresses intent clearly."
— Robert C. Martin, Clean Code


Naming — The Most Underrated Skill
java// ❌ Terrible naming — reads like assembly code
int d; // days elapsed since last backup
List<int[]> q = new ArrayList<>();
for (int[] x : q) { if (x[0] == 4) { ... } }
public void p(User u) { ... }
boolean flag = true;

// ✅ Clean naming — reads like English prose
int daysSinceLastBackup;
List<int[]> urgentTickets = new ArrayList<>();
for (int[] ticket : urgentTickets) {
if (ticket[STATUS_INDEX] == STATUS_CRITICAL) { ... }
}
public void processPaymentForUser(User customer) { ... }
boolean isEmailVerified = true;
java// ─── Naming Rules ────────────────────────────────────────
// Classes: Noun or Noun phrase
class CustomerInvoiceGenerator { }   // ✅
class ProcessData { }                // ❌ verb

// Methods: Verb or Verb phrase
public void sendConfirmationEmail() { }  // ✅
public void email() { }                  // ❌ too vague

// Booleans: is/has/can/should prefix
boolean isActive        = true;   // ✅
boolean active          = true;   // ❌ ambiguous
boolean hasPermission   = false;  // ✅
boolean canDelete       = true;   // ✅

// Constants: UPPER_SNAKE_CASE with meaning
static final int MAX_LOGIN_ATTEMPTS     = 5;   // ✅
static final int N                      = 5;   // ❌
static final int MAX                    = 5;   // ❌ max what?

// Collections: plural nouns
List<User>   users;         // ✅
List<User>   userList;      // ❌ redundant 'List'
List<User>   data;          // ❌ too generic

// Avoid encodings / Hungarian notation
String strName;             // ❌ type in name (from 1980s)
int    iCount;              // ❌
String name;                // ✅
int    count;               // ✅

Methods — Small, Focused, One Level of Abstraction
java// ❌ The "Wall of Code" method — does everything
public void processOrder(Order order) {
// Step 1: validate
if (order == null) throw new IllegalArgumentException();
if (order.getItems().isEmpty()) throw new IllegalStateException();
if (order.getCustomer() == null) throw new IllegalArgumentException();
for (OrderItem item : order.getItems()) {
if (item.getQuantity() <= 0) throw new IllegalStateException();
if (item.getProduct() == null) throw new IllegalArgumentException();
}

    // Step 2: calculate
    double subtotal = 0;
    for (OrderItem item : order.getItems()) {
        subtotal += item.getProduct().getPrice() * item.getQuantity();
    }
    double tax = subtotal * 0.18;
    double shipping = subtotal > 500 ? 0 : 50;
    double total = subtotal + tax + shipping;
    order.setTotal(total);

    // Step 3: check inventory
    for (OrderItem item : order.getItems()) {
        if (inventory.getStock(item.getProduct().getId()) < item.getQuantity()) {
            throw new InsufficientStockException(item.getProduct().getName());
        }
        inventory.reserve(item.getProduct().getId(), item.getQuantity());
    }

    // Step 4: payment
    boolean paid = paymentGateway.charge(order.getCustomer().getPaymentMethod(), total);
    if (!paid) throw new PaymentFailedException();

    // Step 5: notifications
    emailService.send(order.getCustomer().getEmail(), "Order confirmed", buildEmailBody(order));
    smsService.send(order.getCustomer().getPhone(), "Order #" + order.getId() + " confirmed!");

    // ... 80 more lines
}
java// ✅ Clean — one level of abstraction per method
public Order processOrder(Order order) {
validateOrder(order);
calculateOrderTotals(order);
reserveInventory(order);
processPayment(order);
sendOrderConfirmations(order);
return order;
}

// Each method is at the SAME level of abstraction
// processOrder reads like a high-level summary — each step self-explanatory
// Drill down only when you need detail

private void validateOrder(Order order) {
validateNotNull(order);
validateCustomer(order.getCustomer());
validateItems(order.getItems());
}

private void validateItems(List<OrderItem> items) {
if (items.isEmpty()) throw new EmptyOrderException();
items.forEach(this::validateItem);
}

private void validateItem(OrderItem item) {
if (item.getProduct() == null) throw new InvalidProductException();
if (item.getQuantity() <= 0)   throw new InvalidQuantityException(item);
}

private void calculateOrderTotals(Order order) {
double subtotal = calculateSubtotal(order.getItems());
double tax      = taxCalculator.calculate(subtotal);
double shipping = shippingCalculator.calculate(subtotal, order.getAddress());
order.setTotals(subtotal, tax, shipping);
}

private void processPayment(Order order) {
boolean success = paymentGateway.charge(
order.getCustomer().getPaymentMethod(),
order.getTotal()
);
if (!success) throw new PaymentFailedException(order.getId());
order.markPaymentComplete();
}

Comments — When to Write, When Not To
java// ─── BAD COMMENTS — noise, lies, or explaining bad code ─────

// ❌ Redundant — code says exactly this
// Get user by id
User user = userRepository.findById(userId);

// ❌ Explaining bad variable names (fix the NAME, not add a comment)
int d; // days since last login  ← just name it daysSinceLastLogin!

// ❌ Outdated comment — code changed but comment didn't (a LIE)
// Calculates 10% discount
double discount = amount * 0.15;  // Now 15%, comment says 10% — which is right??

// ❌ "TODO" graveyard
// TODO: fix this someday  ← 4 years old, nobody did it

// ─── GOOD COMMENTS — intent, warnings, clarifications ───────

// ✅ Explains WHY — something non-obvious
// Using LinkedHashMap to preserve insertion order for predictable API responses
private Map<String, Object> responseFields = new LinkedHashMap<>();

// ✅ Warning about consequence
// WARNING: Do NOT cache this result. The exchange rate MUST be fetched
// fresh every time — regulatory compliance requirement (RBI circular 2023-04)
public double getLiveExchangeRate(String currency) { ... }

// ✅ Clarifies a complex algorithm
// Phase 1: Collect all items with their weighted scores
// Phase 2: Normalize scores to 0-1 range
// Phase 3: Apply time-decay function (recent items score higher)
// Phase 4: Sort by final score descending
public List<Item> rankItems(List<Item> items) { ... }

// ✅ Explains non-obvious business rule
// Accounts dormant for 6+ months are frozen per Section 26A of Banking Act
if (daysSinceLastTransaction > 180) account.freeze();

PART 5: Anti-Patterns 🚫

"An anti-pattern is a common response to a recurring problem that is usually ineffective and risks being counterproductive."

These are what interviewers show you in "what's wrong with this code?" questions.

Anti-Pattern 1: God Class / God Object
java// ❌ The God Class — knows everything, does everything
class ApplicationController {
// User management
private List<User> users;
public User createUser(String name) { ... }
public void deleteUser(String id) { ... }

    // Database
    private Connection dbConnection;
    public void executeQuery(String sql) { ... }
    public void commitTransaction() { ... }

    // Business logic
    public Order processOrder(Order o) { ... }
    public double calculateDiscount(User u) { ... }

    // Email
    public void sendEmail(String to, String body) { ... }
    public void sendBulkEmail(List<String> to) { ... }

    // File operations
    public void exportToCSV(List<?> data) { ... }
    public List<?> importFromCSV(String path) { ... }

    // Reporting
    public Report generateSalesReport() { ... }
    public Report generateUserReport() { ... }

    // 500+ lines. Nobody dares touch it.
    // Change anything → break everything
}

// ✅ FIX: Decompose into focused classes (already covered in SRP)
// Extract: UserService, UserRepository, OrderService,
//          EmailService, FileService, ReportService

Anti-Pattern 2: Primitive Obsession
java// ❌ Using primitives for domain concepts
class User {
private String phone;     // Is "123" a valid phone?
private String email;     // Is "notanemail" valid?
private double latitude;  // Is 999.0 a valid latitude?
private double longitude;
private String status;    // Is "blah" a valid status? Magic strings!
}

public void createTrip(String pickupLat,  String pickupLng,
String destLat,    String destLng,
String userId,     String driverId) {
// All strings — compiler can't catch if you swap pickupLat with userId!
}
java// ✅ FIX: Wrap primitives in domain Value Objects
final class PhoneNumber {
private final String number;
public PhoneNumber(String number) {
if (!number.matches("\\+?[1-9]\\d{9,14}"))
throw new IllegalArgumentException("Invalid phone: " + number);
this.number = number;
}
public String getValue() { return number; }
}

final class EmailAddress {
private final String email;
public EmailAddress(String email) {
if (!email.matches("^[^@]+@[^@]+\\.[^@]+$"))
throw new IllegalArgumentException("Invalid email: " + email);
this.email = email.toLowerCase();
}
}

final class Coordinates {
private final double latitude;
private final double longitude;
public Coordinates(double lat, double lng) {
if (lat < -90  || lat > 90)  throw new IllegalArgumentException("Invalid lat: " + lat);
if (lng < -180 || lng > 180) throw new IllegalArgumentException("Invalid lng: " + lng);
this.latitude  = lat;
this.longitude = lng;
}
}

enum UserStatus { ACTIVE, INACTIVE, SUSPENDED, PENDING_VERIFICATION }

class User {
private PhoneNumber phone;       // Type-safe — only valid phones
private EmailAddress email;      // Type-safe — only valid emails
private UserStatus status;       // Type-safe — only valid statuses
}

// Now compiler HELPS you — can't accidentally swap a phone and email
public void createTrip(Coordinates pickup, Coordinates destination,
UserId riderId, DriverId driverId) {
// Distinct types — impossible to confuse!
}

Anti-Pattern 3: Shotgun Surgery
java// ❌ Shotgun Surgery — ONE change requires touching MANY classes
// Scenario: Change the date format from "dd/MM/yyyy" to "yyyy-MM-dd"

// You must change:
class OrderController {
String date = order.getDate().format("dd/MM/yyyy");  // Change here
}
class InvoicePDF {
String date = invoice.getDate().format("dd/MM/yyyy"); // And here
}
class EmailTemplate {
String date = booking.getDate().format("dd/MM/yyyy"); // And here
}
class ReportGenerator {
String date = record.getDate().format("dd/MM/yyyy");  // And here
}
class AuditLogger {
String date = event.getDate().format("dd/MM/yyyy");   // And here
}
// 5 files for ONE logical change — high risk of missing one

// ✅ FIX: Centralize the knowledge (DRY applied)
class DateFormatter {
private static final DateTimeFormatter FORMAT =
DateTimeFormatter.ofPattern("yyyy-MM-dd");  // ONE place

    public static String format(LocalDate date) { return date.format(FORMAT); }
    public static String format(LocalDateTime dt) { return dt.format(FORMAT); }
}

// Now every class uses DateFormatter.format() — change format in ONE place

Anti-Pattern 4: Feature Envy
java// ❌ Feature Envy — a method more interested in another class's data than its own
class OrderCalculator {
public double calculateTotal(Order order) {
// This method is OBSESSED with Order's internals
double subtotal = 0;
for (OrderItem item : order.getItems()) {
double price    = item.getProduct().getPrice();    // Order's data
int    quantity = item.getQuantity();              // Order's data
double discount = item.getProduct().getDiscount(); // Order's data
subtotal += price * quantity * (1 - discount);
}
double tax      = order.getCustomer().getTaxRate();    // Order's data
double shipping = order.getShippingAddress().getZone().getRate(); // Order's data
return subtotal + (subtotal * tax) + shipping;
// OrderCalculator uses Order's data more than its own — it envies Order
}
}

// ✅ FIX: Move behavior to the class that owns the data
class Order {
// The logic BELONGS here — this class owns the data
public double calculateSubtotal() {
return items.stream()
.mapToDouble(item -> item.getPrice() * item.getQuantity())
.sum();
}

    public double calculateTax() {
        return calculateSubtotal() * customer.getTaxRate();
    }

    public double getTotal() {
        return calculateSubtotal() + calculateTax() + shippingAddress.getShippingRate();
    }
}

class OrderCalculator {
public double calculateTotal(Order order) {
return order.getTotal();  // Delegate — no envy
}
}

Anti-Pattern 5: Magic Numbers and Strings
java// ❌ Magic numbers — what does 86400 mean? 7? 3?
if (account.getDaysSinceLastLogin() > 86400) account.expire();
if (password.length() < 8) throw new Exception("Too short");
if (user.getRole().equals("ADMIN_L2")) grantAccess();
if (retryCount > 3) throw new TimeoutException();

// ✅ Named constants explain intent
class AccountPolicy {
private static final int  ACCOUNT_EXPIRY_DAYS    = 86400;
private static final int  MIN_PASSWORD_LENGTH     = 8;
private static final int  MAX_RETRY_ATTEMPTS      = 3;
}

enum UserRole {
USER, MODERATOR, ADMIN_L1, ADMIN_L2, SUPER_ADMIN;
public boolean canAccessAdminPanel() {
return this == ADMIN_L2 || this == SUPER_ADMIN;
}
}

if (account.getDaysSinceLastLogin() > AccountPolicy.ACCOUNT_EXPIRY_DAYS) account.expire();
if (password.length() < AccountPolicy.MIN_PASSWORD_LENGTH) throw new WeakPasswordException();
if (user.getRole().canAccessAdminPanel()) grantAccess();
if (retryCount > AccountPolicy.MAX_RETRY_ATTEMPTS) throw new MaxRetriesExceededException();

Anti-Pattern 6: The Lava Flow
java// ❌ Lava Flow — dead code nobody dares delete
class PaymentProcessor {

    // TODO: remove this after migration (written 2018, it's 2024)
    @Deprecated
    public void processPaymentV1(String cardNumber) { ... }

    // Old method kept "just in case"
    private double calculateOldTax(double amount) { ... }

    // No one knows if this is still used — afraid to remove
    private void legacyRefundFlow() { ... }

    // Commented-out code "for reference"
    // public void sendConfirmation(Order o) {
    //     emailService.send(o.getEmail(), buildOldTemplate(o));
    // }

    // This field was for a feature that got cancelled
    private boolean experimentalFastCheckout = false;
}

// ✅ FIX: Use version control as your safety net — delete dead code!
// Git has history. If you need it back, checkout the commit.
// Dead code is worse than no code — it confuses, misleads, and decays.

PART 6: The Q&A Round 🎤

Q1. What's the difference between a design principle and a design pattern?

"A design principle is a guideline — a rule of thumb about HOW to design good software. DRY, SOLID, KISS, YAGNI, low coupling, high cohesion — these are principles. A design pattern is a concrete, named solution to a recurring problem. Strategy, Observer, Factory — these are patterns. Principles tell you WHAT to aim for. Patterns are proven IMPLEMENTATIONS that often achieve those principles. Strategy pattern implements OCP. Observer pattern achieves low coupling between publishers and subscribers. Singleton enforces encapsulation of instance creation."


Q2. How do you balance DRY with readability? Is DRY always right?

"No — DRY can be over-applied. Two pieces of code that look similar but represent DIFFERENT concepts should NOT be merged just because they're syntactically similar. Over-DRYing creates the wrong abstraction. Kent Beck calls this the 'wrong abstraction' problem — it's worse than duplication. My test: are these two things the SAME concept that will always change together? Then DRY it. Or are they accidentally similar but conceptually different? Then leave them separate. The pain of duplication is concrete — you forget to update both. The pain of wrong abstraction is worse — your abstraction breaks down when the two concepts inevitably diverge."


Q3. How do you identify that a class has low cohesion?

"Four signals I look for: First, the class name is vague — 'Manager', 'Handler', 'Utils', 'Helper' — these often mean 'we didn't know where else to put this.' Second, the class has many unrelated fields — some methods use fields A and B, other methods use C and D — those two groups never interact. Third, when writing unit tests, I need radically different test setups for different methods — signal they're testing different responsibilities. Fourth, team members describe the class differently — 'it handles orders' vs 'it does billing' vs 'it manages inventory.' If teammates disagree on what it does, it's doing too many things."


Q4. When is tight coupling acceptable?

"Tight coupling isn't always wrong — it's a trade-off. It's acceptable when: the coupled classes form a stable, cohesive unit unlikely to change independently — like a LinkedList and its Node class. When performance is critical and the indirection of interfaces is genuinely costly — though this is rare in modern JVMs. In simple scripts or throwaway code where the overhead of abstractions isn't worth it. And between classes in the SAME bounded context — within a package or module, tighter coupling is often fine. The key question is: will these two things EVER need to change independently? If yes, decouple. If no, coupling is acceptable."


Q5. What is the 'wrong abstraction' and how do you avoid it?

"Wrong abstraction happens when you DRY up code that's accidentally similar but conceptually different. Sandi Metz calls it 'duplication is far cheaper than the wrong abstraction.' Example: two report generators that both loop through items and format strings — they LOOK similar. You extract a shared base class. Then one needs to add discount logic, the other needs pagination. Now you're hacking your abstraction to support both, adding flags and special cases. The abstraction is now MORE complex than the duplication was. To avoid it: before abstracting, wait for the THIRD occurrence. And ask: will these things ALWAYS change for the same reason? Only then abstract."


Q6. Explain thread safety in the context of immutability.

"Immutable objects are inherently thread-safe. The core concurrency problems — race conditions, visibility issues, data corruption — arise from shared mutable state. Thread A reads a value, Thread B writes it, Thread A uses stale data. Immutable objects have NO mutable state after construction — nothing to race on. You can share them freely across threads with zero synchronization. String is immutable in Java — you can pass it to 1000 threads simultaneously, no locks needed. This is why Java's LocalDate, LocalTime, BigDecimal are immutable. In contrast, java.util.Date is mutable — it's not thread-safe and caused countless concurrency bugs, which is why it's been replaced."


Q7. What is defensive programming and when should you use it?

"Defensive programming means assuming inputs can be wrong and protecting against it explicitly. At PUBLIC API boundaries — method parameters, constructor arguments, user input — I always validate. Assert invariants. Fail fast with meaningful exceptions rather than propagating invalid state. Inside a well-tested private method in a cohesive class, I'm less defensive — I trust the internal contract. The rule: be defensive at BOUNDARIES, trust within a well-defined unit. Also: defensive copying for mutable objects entering or leaving a class — so callers can't corrupt your internal state. But don't over-defensively check things the type system already guarantees."


Q8. How does Clean Code relate to LLD interviews?

"Directly. The interviewer sees how you NAME variables, whether your methods are focused, whether your code expresses intent clearly. An interviewer watching you code in real time is evaluating your habits. If you write int x = u.getA() * 0.1 they see sloppy thinking. If you write double bonusAmount = employee.getBaseSalary() * BONUS_RATE they see engineering maturity. Clean Code habits signal that you write maintainable production code, not just code that works in isolation. At FAANG, you're working in codebases with thousands of engineers — readability is a professional responsibility, not a preference."


Q9. What is the Law of Demeter? How does it reduce coupling?

"The Law of Demeter says: a method should only call methods on objects it directly owns — its own fields, method parameters, objects it creates, or objects returned by its own methods. Also called 'Don't talk to strangers.' The telltale violation is a long chain: order.getCustomer().getAddress().getCity().getPostalCode(). This means OrderService knows about Customer, Address, City, AND PostalCode — any change in that chain breaks OrderService. Fix: add getCustomerPostalCode() to Order — it navigates internally, hiding the chain. The caller only talks to Order. This reduces coupling by limiting what classes each class needs to know about."

java// ❌ Law of Demeter violation — "train wreck"
String postalCode = order.getCustomer()
.getAddress()
.getCity()
.getPostalCode();

// ✅ Fix — delegate navigation into the class that owns the data
class Order {
public String getCustomerPostalCode() {
return customer.getPostalCode();  // Customer handles its own traversal
}
}

class Customer {
public String getPostalCode() {
return address.getPostalCode();  // Address handles its own traversal
}
}

// Caller
String postalCode = order.getCustomerPostalCode();  // Only talks to Order
```

---

**Q10. What makes code "clean" beyond just working correctly?**

> *"Code that works is the minimum. Clean code is code your teammate can understand without asking you questions. I judge code by these measures: Can I read this like prose — does it tell a story? Are methods short enough to fit on one screen? Are names specific enough that comments are rarely needed? Is each class focused enough that I can describe it in one sentence without using 'and'? Are tests independent and fast? Is there minimal repetition? And critically — is complexity proportional to the problem? A simple CRUD endpoint should have simple code. A complex pricing engine warrants more sophistication. Unnecessary complexity in simple code is just as bad as missing complexity in hard problems."*

---

## Section 5 Master Summary 🧠
```
DRY        → Every business rule has ONE home. Duplicate knowledge = bugs.
KISS       → Simplest correct solution first. Complexity must earn its place.
YAGNI      → Build what's needed NOW. OCP prepares structure; YAGNI stops premature build.

Coupling   → LOW = depend on interfaces, not concretions. Inject dependencies.
Cohesion   → HIGH = one class, one purpose. Name it clearly or split it.

Immutability→ final class + final fields + defensive copies = thread-safe value objects.

Clean Code → Name intent. Short methods. One abstraction level. No magic numbers.

Anti-Patterns to name-drop:
→ God Class      (SRP violation at scale)
→ Primitive Obsession (use Value Objects)
→ Shotgun Surgery (DRY violation — knowledge in many places)
→ Feature Envy   (move method to the class with the data)
→ Lava Flow      (delete dead code — trust git history)
→ Magic Numbers  (use named constants)
```

---

## The Complete LLD Interview Mindset 🎯
```
When you hear a problem:
STOP  → Don't code immediately
ASK   → Clarify scope, scale, constraints
DRAW  → Entities, relationships, rough class diagram
NAME  → Which patterns apply and WHY
CODE  → Start with interfaces, then implementations
TALK  → Narrate every decision: "I'm using X because..."
REVIEW→ "I would also add thread safety here in production..."

Phrases that make interviewers nod:
→ "I'm favoring composition over inheritance here because..."
→ "This is an OCP violation — adding this type would require modifying existing code"
→ "I'd make this immutable to avoid defensive copying overhead and get thread safety for free"
→ "This is heading toward a God Class — let me extract the payment logic"
→ "The Law of Demeter tells me this chain is too long — I should delegate"
→ "This feels like duplicated knowledge — I'd centralize it in..."
→ "I won't build that yet — YAGNI — but the Strategy pattern means adding it later is a new class, zero existing changes"

You've now covered all 5 sections! 🏆