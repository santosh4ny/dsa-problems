
Section 1: OOP Fundamentals

    Let's go deep. I'll teach you exactly how to explain each concept in front of an interviewer — not just what it is, but how to say it confidently.

The 4 Pillars of OOP

    Everything in LLD is built on these four. If your foundation is shaky here, design pattern questions will feel hard. Let's make them feel easy.

Pillar 1 — ENCAPSULATION

    The Concept
    Encapsulation = bundle data + behavior together, and hide internal state from the outside world.
    Think of it like a capsule (pill) — medicine is inside, protected. You swallow it, but you can't directly touch the medicine.
How to say it in an interview:

    "Encapsulation is about protecting the internal state of an object. I make fields private and only expose them through controlled methods — getters and setters — where I can enforce business rules. For example, a BankAccount shouldn't let anyone directly set the balance field. It must go through deposit() or withdraw(), where I can validate the amount."

    javaclass BankAccount {
        private double balance;        // HIDDEN — no direct access
        private String accountNumber;
    
        public void deposit(double amount) {
            if (amount <= 0) throw new IllegalArgumentException("Invalid amount");
            this.balance += amount;    // Rule enforced HERE
        }
    
        public void withdraw(double amount) {
            if (amount > balance) throw new InsufficientFundsException();
            this.balance -= amount;
        }
    
        public double getBalance() { return balance; }  // READ-ONLY — no setter!
    }

Why no setter for balance? Because if someone could call setBalance(99999), the rule "you can't have more than you deposit" breaks. That's exactly what encapsulation prevents.

Pillar 2 — ABSTRACTION

    The Concept
    Abstraction = hide complexity, show only what's necessary.
    You know what something does — not how it does it.
    Analogy: When you drive a car, you use the steering wheel and pedals. You don't know how fuel injection or the combustion cycle works. The car abstracts those details away from you.
How to say it in an interview:

    "Abstraction is about simplifying interfaces. 
    I use abstract classes and interfaces to define WHAT a thing does, 
    hiding HOW it does it. The caller doesn't need to know the implementation — 
    they just call the method. This also lets me swap implementations without changing the caller."

java// ABSTRACTION via Interface

    interface PaymentProcessor {
    void processPayment(double amount);   // WHAT it does
    boolean refund(String transactionId); // HOW — hidden from caller
    }
    
    class StripeProcessor implements PaymentProcessor {
    public void processPayment(double amount) {
    // 200 lines of Stripe API logic — HIDDEN
    stripeApi.charge(amount);
    }
    }
    
    class PayPalProcessor implements PaymentProcessor {
    public void processPayment(double amount) {
    paypalApi.execute(amount);  // Different HOW, same WHAT
    }
    }

// Caller doesn't care which processor — just calls the abstraction

    class CheckoutService {
    private PaymentProcessor processor;  // Just the interface!
    public void checkout(double amount) {
    processor.processPayment(amount);  // Works for ANY processor
    }
    }

Encapsulation vs Abstraction — The KEY difference (interviewers LOVE this):
EncapsulationAbstractionWhat it hidesData (fields)Complexity (implementation)Howprivate fields + getters/settersInterfaces + abstract classesFocusData protectionSimplificationExampleprivate double balanceinterface PaymentProcessor

    "Encapsulation hides DATA. Abstraction hides COMPLEXITY. They work together but solve different problems."


Pillar 3 — INHERITANCE

    The Concept
    Inheritance = child class acquires properties and behavior of parent class. IS-A relationship.
    Dog IS-A Animal. Car IS-A Vehicle. Manager IS-A Employee.

    javaabstract class Vehicle {
        protected String brand;
        protected int speed;
    
        public void accelerate(int amount) {
            this.speed += amount;   // SHARED — every vehicle can accelerate
        }
    
        public abstract int getFuelType();  // MUST be overridden — each type is different
    }
    
    class ElectricCar extends Vehicle {
    private int batteryLevel;
    
        public int getFuelType() { return ELECTRIC; }     // Own implementation
        public void charge() { this.batteryLevel = 100; } // Own behavior
    }
    
    class PetrolCar extends Vehicle {
    public int getFuelType() { return PETROL; }
    }

How to say it + the TRAP interviewers set:

    "Inheritance enables code reuse and polymorphism through IS-A relationships. But I'm careful with it — I always ask: is this TRULY an IS-A? Because inheritance creates tight coupling. If the parent changes, all children can break — that's called the fragile base class problem. When in doubt, I prefer composition."

That last sentence — "when in doubt, I prefer composition" — is gold. It shows seniority.

Pillar 4 — POLYMORPHISM

        The Concept
        Polymorphism = many forms. Same interface, different behavior.
        Two types — and you need to know the difference cold:
        Compile-timeRuntimeAlso calledStatic polymorphismDynamic polymorphismMechanismMethod OverloadingMethod OverridingResolvedAt compile timeAt runtimeFlexibilityLessMoreUsed in patternsRarelyEverywhere
        java// COMPILE-TIME: Same name, different parameters
    class Calculator {
    public int add(int a, int b)       { return a + b; }
    public double add(double a, double b) { return a + b; }  // Overloaded
    public int add(int a, int b, int c) { return a + b + c; } // Overloaded
    }
    
    // RUNTIME: Same reference type, different actual object
    class Shape {
    public double area() { return 0; }
    }
    class Circle extends Shape {
    private double r;
    public double area() { return Math.PI * r * r; }  // Overrides
    }
    class Rectangle extends Shape {
    private double w, h;
    public double area() { return w * h; }  // Overrides
    }
    
    // THIS is runtime polymorphism — the magic line:
    Shape s = new Circle();   // Reference: Shape. Object: Circle.
    s.area();                 // Calls Circle.area() — decided at RUNTIME
    
    // This is why you can do this:
    List<Shape> shapes = List.of(new Circle(), new Rectangle(), new Circle());
    for (Shape shape : shapes) {
    System.out.println(shape.area());  // Each calls its OWN area()!
    }
How to say it:

    "Runtime polymorphism is what makes design patterns like Strategy and Observer possible. I can write code that operates on an interface, and the actual behavior is decided at runtime based on what object is passed in. This means I can add new types — a Triangle, a Pentagon — without changing the loop at all. That's OCP in action."


Now the Q&A Round

Treat each one like you're in the hot seat. Read the question, think for 5 seconds, then read the answer.

Q1. What is the difference between an Abstract Class and an Interface?

    "An abstract class can have both abstract and concrete methods, can hold state (instance variables), and only allows single inheritance. An interface defines a pure contract — no state, multiple implementation allowed. My rule: if classes share COMMON CODE and state, use abstract class. If you're defining a CAPABILITY that unrelated classes can share, use interface. For example, Animal is an abstract class — all animals share eat() behavior. But Flyable is an interface — a Bird, a Plane, and a Superman can all fly, but they're not related to each other."
    
    javaabstract class Animal {
    protected String name;
    public void eat() { System.out.println("Eating"); }  // Shared code
    public abstract void makeSound();                     // Must override
    }
    
    interface Flyable  { void fly(); }
    interface Swimmable { void swim(); }
    
    // Duck IS-A Animal, CAN fly and swim
    class Duck extends Animal implements Flyable, Swimmable {
    public void makeSound() { System.out.println("Quack!"); }
    public void fly()  { System.out.println("Flap flap"); }
    public void swim() { System.out.println("Splash"); }
    }

Q2. What is method overriding and what are its rules?

    "Overriding is when a subclass provides its own implementation of a method already defined in the parent. The rules are: same method name, same parameter list, return type must be same or covariant, access modifier can only be same or MORE permissive — never more restrictive. And I always use @Override annotation. It's not just convention — it's a compile-time safety net. If I misspell the method name, the compiler catches it."
    
    javaclass Animal {
    protected Animal clone() throws CloneNotSupportedException { ... }
    public final void breathe() { }   // final — CANNOT override
    private void digest() { }         // private — CANNOT override
    }
    
    class Dog extends Animal {
    @Override  // ALWAYS use this
    public Dog clone() {   // ✅ public is MORE permissive than protected
    return new Dog();  // ✅ Dog is covariant return type of Animal
    }
    
        // @Override
        // private void breathe() {}  // ❌ Can't override final
        // private void breathe() {}  // ❌ Can't reduce access (public→private)
    }

Q3. What is the difference between == and .equals()?

    "== compares references — it checks if both variables point to the SAME object in memory. .equals() compares content — it checks if two objects are logically equal. For primitives, == works fine. For objects, especially Strings, always use .equals(). A classic bug is str == "hello" — it might work sometimes due to String interning, but it's unreliable and wrong. I override equals() whenever I define a value object."
    
    javaString a = new String("hello");
    String b = new String("hello");
    
    System.out.println(a == b);       // FALSE — different objects in memory
    System.out.println(a.equals(b));  // TRUE  — same content
    
    // Always override equals() AND hashCode() together!
    class Point {
    int x, y;
    @Override
    public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof Point)) return false;
    Point p = (Point) o;
    return x == p.x && y == p.y;
    }
    @Override
    public int hashCode() {
    return Objects.hash(x, y);  // Must override when equals() is overridden
    }
    }

Q4. Why must hashCode() be overridden when equals() is?

    "This is a Java contract: if two objects are equal by equals(), they MUST have the same hashCode(). HashMap and HashSet use hashCode first to find the bucket, then equals to confirm. If I override equals but not hashCode, two 'equal' Point objects will go into DIFFERENT buckets in a HashMap, so map.get(new Point(1,2)) will return null even after I put with new Point(1,2). It's a subtle but devastating bug."


Q5. What is the difference between Composition and Inheritance? Why prefer composition?

    "Inheritance is IS-A, composition is HAS-A. Inheritance creates tight coupling — child is dependent on parent's implementation details. If the parent changes, all children can break. Composition is more flexible: you hold a reference to another object and delegate to it. You can even swap it at runtime. The Gang of Four book says 'favor composition over inheritance', and I follow that. A classic example: instead of LoggingService extending FileWriter — which makes no semantic sense — it should HAVE a FileOutput interface and delegate to it."
    
    java// BAD: Inheritance for code reuse (Logger is NOT a FileWriter)
    class Logger extends FileWriter { }
    
    // GOOD: Composition
    interface LogOutput { void write(String msg); }
    
    class Logger {
    private LogOutput output;  // HAS-A — can swap at runtime!
    
        public Logger(LogOutput output) { this.output = output; }
        public void log(String msg) { output.write("[LOG] " + msg); }
    }
    
    // Usage — swap behavior without changing Logger
    Logger fileLogger    = new Logger(new FileOutput());
    Logger consoleLogger = new Logger(new ConsoleOutput());

Q6. What is a Constructor? Explain constructor chaining.

    "A constructor initializes a new object's state. It has the same name as the class and no return type. Constructor chaining is calling one constructor from another to avoid duplicating initialization logic. this() chains within the same class, super() calls the parent. The rule is it must be the FIRST statement. I use this heavily in value objects with optional fields — instead of duplicating null checks across 4 overloaded constructors, I chain them all into one master constructor."
    
    javaclass User {
    private String name;
    private String email;
    private String role;
    
        public User(String name) {
            this(name, "unknown@email.com");  // ← chains down
        }
    
        public User(String name, String email) {
            this(name, email, "USER");  // ← chains down
        }
    
        public User(String name, String email, String role) {
            // ALL validation in ONE place
            if (name == null || name.isBlank()) throw new IllegalArgumentException();
            this.name  = name;
            this.email = email;
            this.role  = role;
        }
    }

Q7. What is static? When should and shouldn't you use it?

    "Static members belong to the CLASS, not any instance. All instances share the same static field. Static methods can be called without creating an object. I use static for: utility methods like Math.sqrt(), constants, counters shared across instances, Singleton getInstance(), and Factory methods. I avoid static for anything that depends on object state — because static methods can't access instance fields, and they're hard to mock in unit tests."
    
    javaclass Employee {
    private static int count = 0;    // Shared by ALL employees
    private int id;
    private String name;
    
        public Employee(String name) {
            count++;               // Every new employee increments shared counter
            this.id = count;
            this.name = name;
        }
    
        public static int getCount() { return count; }  // No instance needed
    
        // Static factory method — gives meaningful name to creation
        public static Employee createManager(String name) {
            Employee e = new Employee(name);
            e.setRole("MANAGER");
            return e;
        }
    }

Q8. What is final? Explain final variable, method, and class.

    "final has three uses. A final VARIABLE cannot be reassigned after initialization — it's a constant. A final METHOD cannot be overridden in any subclass — useful when the behavior must stay consistent, like a security check. A final CLASS cannot be extended at all — String is final in Java, which is why String is safely immutable. Combined with private fields, final is the tool for building immutable objects."
    
    javafinal class ImmutableMoney {          // Can't be extended
    private final String currency;    // Can't be reassigned
    private final double amount;
    
        public ImmutableMoney(String currency, double amount) {
            this.currency = currency;
            this.amount   = amount;
        }
    
        // 'Addition' creates NEW object — original untouched
        public ImmutableMoney add(double more) {
            return new ImmutableMoney(currency, amount + more);
        }
    
        public final double getAmount() { return amount; }  // Can't be overridden
    }

Q9. What is this and super? How are they used in the Builder pattern?

    "this refers to the current object. I use it to distinguish field from parameter, to call another constructor via this(), and crucially in the Builder pattern — every setter returns this, enabling fluent method chaining. super accesses the parent class — to call parent constructor, parent's overridden method, or parent's hidden field."
    
    java// Builder pattern — 'this' enables chaining
    class HttpRequest {
    private final String url;
    private final String method;
    private final Map<String, String> headers;
    private final String body;
    
        private HttpRequest(Builder b) {
            this.url = b.url; this.method = b.method;
            this.headers = b.headers; this.body = b.body;
        }
    
        public static class Builder {
            private String url, method = "GET", body;
            private Map<String, String> headers = new HashMap<>();
    
            public Builder(String url) { this.url = url; }
            public Builder method(String m) { this.method = m; return this; }  // ← 'this' enables chaining
            public Builder header(String k, String v) { headers.put(k,v); return this; }
            public Builder body(String b) { this.body = b; return this; }
            public HttpRequest build() { return new HttpRequest(this); }
        }
    }
    
    // Beautiful, readable
    HttpRequest req = new HttpRequest.Builder("https://api.example.com/users")
    .method("POST")
    .header("Content-Type", "application/json")
    .header("Authorization", "Bearer token123")
    .body("{\"name\": \"Alice\"}")
    .build();

Q10. What is the difference between throw and throws?

    "throw is the action — it's a statement that actually throws an exception object at runtime. throws is a declaration — it's part of the method signature, warning callers that this method might throw a checked exception and they need to handle it. Unchecked exceptions (RuntimeException subclasses) don't need throws in the signature, but I sometimes add them anyway for documentation clarity."
    
    java// throws = declaration (warning to callers)
    public User findUser(String id) throws UserNotFoundException {
    User user = db.find(id);
    if (user == null) {
    throw new UserNotFoundException("User not found: " + id);  // throw = action
    }
    return user;
    }
    
    // Caller MUST handle it
    try {
    User u = findUser("abc123");
    } catch (UserNotFoundException e) {
    log.error("User missing", e);
    }

Q11. What are checked vs unchecked exceptions?

    "Checked exceptions are checked at compile time — you MUST either catch them or declare throws. They represent recoverable conditions like file not found or network timeout. Unchecked exceptions (RuntimeException subclasses) represent programming errors — null pointer, array index out of bounds. You don't have to declare them. My design philosophy: use checked exceptions for things the CALLER can reasonably recover from. Use unchecked for bugs or invalid states that shouldn't happen."
    
    java// Checked — caller must handle
    class FileParser {
    public String parse(String path) throws IOException { ... }  // Must declare
    }
    
    // Unchecked — programming error
    class OrderService {
    public Order create(String customerId) {
    if (customerId == null)
    throw new IllegalArgumentException("customerId cannot be null");  // No throws needed
    // ...
    }
    }

Q12. What is the difference between deep copy and shallow copy?

    "Shallow copy creates a new object but copies references — both the original and copy point to the SAME nested objects. Deep copy creates a completely independent copy — new objects all the way down the object graph. This matters a lot for immutability and preventing bugs. If I give out a shallow copy of a list and the caller modifies it, they've just mutated my internal state — encapsulation broken."
    
    javaclass Team {
    private String name;
    private List<String> members;
    
        // SHALLOW COPY — dangerous!
        public List<String> getMembersShallow() {
            return members;  // Caller can modify your internal list!
        }
    
        // DEEP COPY — safe
        public List<String> getMembersSafe() {
            return new ArrayList<>(members);  // New list, same strings (ok for immutable Strings)
        }
    
        // For mutable objects, must go deeper
        public Team shallowCopy() {
            Team t = new Team();
            t.name = this.name;
            t.members = this.members;  // SAME list reference — shared!
            return t;
        }
    
        public Team deepCopy() {
            Team t = new Team();
            t.name = this.name;
            t.members = new ArrayList<>(this.members);  // NEW list — independent!
            return t;
        }
    }

Section 1 Summary

    ---------------------------------------------------------------------------------------
    Concept                    | One-liner to remember
    ---------------------------|------------------------------------------------------------
    Encapsulation              | Private fields + controlled access = data protection
    ---------------------------|------------------------------------------------------------
    Abstraction                | Interface = WHAT, implementation = HOW (hidden)
    ---------------------------|------------------------------------------------------------
    Inheritance                | IS-A relationship, enables reuse + polymorphism
    ---------------------------|------------------------------------------------------------
    PolymorphismSame           | interface, different runtime behavior
    ---------------------------|------------------------------------------------------------
    Abstract vs Interface      | State + code → abstract class. Contract → interface
    ---------------------------|------------------------------------------------------------
    Composition vs Inheritance | HAS-A > IS-A when in doubt
    ---------------------------|------------------------------------------------------------
    equals + hashCode          | Override BOTH or override NEITHER
    ---------------------------|------------------------------------------------------------
    final                      | Variable=constant, method=no override, class=no extend
    ---------------------------|------------------------------------------------------------
    Checked vs Unchecked       | Recoverable=checked, Bug=unchecked
    ---------------------------|------------------------------------------------------------
    Deep vs Shallow            | copyShallow shares refs, Deep is fully independent
    ---------------------------------------------------------------------------------------
