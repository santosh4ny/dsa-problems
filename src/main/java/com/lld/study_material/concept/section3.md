Section 3: Design Patterns

    Design patterns are where FAANG interviews truly separate candidates.
    You need to know not just what each pattern is — but when to use it, when NOT to use it, 
    and how to spot which pattern a problem is asking for.

The Big Picture First

    "Design patterns are reusable solutions to commonly occurring design problems. 
    They're not code you copy-paste — they're blueprints. 
    The Gang of Four book (Gamma, Helm, Johnson, Vlissides) defined 23 patterns in 3 categories. 
    In interviews, I don't just recite patterns — I identify which problem calls for which pattern."

CREATIONAL — How objects are CREATED

    Singleton, Factory Method, Abstract Factory, Builder, Prototype

STRUCTURAL — How objects are COMPOSED

    Adapter, Decorator, Facade, Proxy, Composite, Bridge, Flyweight

BEHAVIORAL — How objects COMMUNICATE

    Observer, Strategy, Command, Template Method, Iterator,
    Chain of Responsibility, State, Mediator, Memento
The Pattern Recognition Cheat Sheet

    Before diving in, memorize this. When a problem description has these keywords → think this pattern:
     
    -------------------------------------------------------------------------------------------------   
    Keyword in Problem                                                  Think Pattern
    -------------------------------------------------------------------------------------------------
    "only one instance", "global access point"                          Singleton
    -------------------------------------------------------------------------------------------------
    "create objects without specifying class","based on type"           Factory
    -------------------------------------------------------------------------------------------------
    "families of related objects", "cross-platform UI"                  Abstract Factory
    -------------------------------------------------------------------------------------------------
    "many optional parameters", "step by step construction"             Builder
    -------------------------------------------------------------------------------------------------
    "add behavior dynamically", "wrap an object"                        Decorator
    -------------------------------------------------------------------------------------------------
    "notify multiple objects when state changes", "event system"        Observer
    -------------------------------------------------------------------------------------------------
    "interchangeable algorithms", "swap behavior at runtime"            Strategy
    -------------------------------------------------------------------------------------------------
    "undo/redo", "queue requests", "log operations"                     Command
    -------------------------------------------------------------------------------------------------
    "simplify complex subsystem", "one unified interface"               Facade
    -------------------------------------------------------------------------------------------------
    "incompatible interfaces work together"                             Adapter
    -------------------------------------------------------------------------------------------------
    "object behavior changes based on state"                            State
    -------------------------------------------------------------------------------------------------
    "tree structure", "treat individual and group same way"             Composite
    -------------------------------------------------------------------------------------------------
    "control access to object", "lazy loading"                          Proxy
    -------------------------------------------------------------------------------------------------
    "algorithm skeleton", "steps overridable by subclass"               Template Method
    -------------------------------------------------------------------------------------------------

CREATIONAL PATTERNS

Pattern 1: Singleton

    The Concept
    Ensure a class has only ONE instance and provide a global access point to it.
    The Analogy
    A country has ONE president. No matter how many times you ask "who's the president?" — you always get the same person. You can't create a second president by calling new President().
When to use

    Logger (one logger for whole app)
    Configuration manager
    Database connection pool
    Thread pool
    Cache manager

4 Ways to implement — Know ALL of them

java// WAY 1: Eager Initialization

    // ✅ Simple  ✅ Thread-safe  ❌ Always creates instance even if never used
    class EagerSingleton {
        private static final EagerSingleton INSTANCE = new EagerSingleton(); // Created at class load
        private EagerSingleton() {}  // Private constructor — no one else can call new
        public static EagerSingleton getInstance() { return INSTANCE; }
    }

java// WAY 2: Lazy + Synchronized (Thread-safe but slow)

    // ✅ Lazy   ✅ Thread-safe   ❌ Every call acquires lock — performance hit
    class SynchronizedSingleton {
        private static SynchronizedSingleton instance;
        private SynchronizedSingleton() {}
    
        public static synchronized SynchronizedSingleton getInstance() {
            if (instance == null) {
                instance = new SynchronizedSingleton();
            }
            return instance;
        }
    }

java// WAY 3: Double-Checked Locking (BEST for most cases)

    // ✅ Lazy   ✅ Thread-safe   ✅ Fast (lock only on first creation)
    // ⚠️  volatile is NON-NEGOTIABLE — without it, broken on multi-core CPUs
    class Logger {
        private static volatile Logger instance;  // volatile prevents CPU caching issues
        private List<String> logs = new ArrayList<>();
    
        private Logger() {}
    
        public static Logger getInstance() {
            if (instance == null) {              // First check — no lock (fast path)
                synchronized (Logger.class) {    // Lock only when might be null
                    if (instance == null) {      // Second check — with lock (safe)
                        instance = new Logger(); // Only one thread creates it
                    }
                }
            }
            return instance;
        }
    
        public void log(String message) {
            logs.add(LocalDateTime.now() + " : " + message);
        }
    }

java// WAY 4: Bill Pugh / Initialization-on-demand Holder (CLEANEST)

    // ✅ Lazy   ✅ Thread-safe   ✅ No synchronization needed   ✅ Simple
    // JVM guarantees class loading is thread-safe — we exploit that here
    class ConfigManager {
        private Map<String, String> config;
    
        private ConfigManager() {
            config = new HashMap<>();
            config.put("db.host", "localhost");
            config.put("db.port", "5432");
        }
    
        // Inner class is NOT loaded until getInstance() is called
        private static class Holder {
            static final ConfigManager INSTANCE = new ConfigManager();
        }
    
        public static ConfigManager getInstance() {
            return Holder.INSTANCE; // Class loads here — JVM handles thread safety
        }
    
        public String get(String key) { return config.get(key); }
    }

java// WAY 5: Enum Singleton (Handles serialization + reflection attacks)

    // ✅ Simplest   ✅ Thread-safe   ✅ Serialization-safe   ✅ Reflection-proof
    enum AppCache {
        INSTANCE;
    
        private Map<String, Object> cache = new HashMap<>();
    
        public void put(String key, Object value) { cache.put(key, value); }
        public Object get(String key) { return cache.get(key); }
        public void evict(String key) { cache.remove(key); }
    }
    
// Usage

    AppCache.INSTANCE.put("user:123", userObject);
    AppCache.INSTANCE.get("user:123");
    Why volatile matters — explain this in interviews:
    java// Without volatile, this can happen on multi-core CPUs:
    // Thread A: instance = new Singleton()
    //   Step 1: Allocate memory
    //   Step 2: Assign reference to 'instance'  ← CPU might reorder these two!
    //   Step 3: Initialize object
    // Thread B sees instance != null (step 2 done) but object not initialized (step 3 pending)
    // Thread B returns a BROKEN, half-constructed object!
    // volatile prevents this reordering — guarantees visibility across threads

=======================================================================================
Pattern 2: Factory Method

The Concept

    Define an interface for creating an object, but let subclasses or a factory method 
    decide which class to instantiate. The client doesn't need to know the concrete class.
    
The Analogy

    When you order food at a restaurant, you say "I want pasta." 
    The kitchen decides whether to make spaghetti, penne, or fettuccine — you just get pasta. 
    You don't go into the kitchen and cook it yourself.

    java// The Product — what gets created
    interface Notification {
        void send(String recipient, String message);
        String getType();
    }

// Concrete Products

    class EmailNotification implements Notification {
        public void send(String recipient, String message) {
        System.out.println("📧 Sending EMAIL to " + recipient + ": " + message);
        // emailService.send(recipient, subject, message);
    }
    public String getType() { return "EMAIL"; }
    }
    
    class SMSNotification implements Notification {
        public void send(String recipient, String message) {
            System.out.println("📱 Sending SMS to " + recipient + ": " + message);
            // twilioApi.sendSMS(recipient, message);
        }
        public String getType() { return "SMS"; }
    }

    class PushNotification implements Notification {
        public void send(String recipient, String message) {
            System.out.println("🔔 Sending PUSH to device " + recipient + ": " + message);
            // fcmApi.push(recipient, message);
        }
        public String getType() { return "PUSH"; }
    }

    class WhatsAppNotification implements Notification {
        public void send(String recipient, String message) {
            System.out.println("💬 Sending WhatsApp to " + recipient + ": " + message);
        }
        public String getType() { return "WHATSAPP"; }
    }

// The Factory — centralized creation logic

    class NotificationFactory {
        // Static factory method
        public static Notification create(String type) {
            switch (type.toUpperCase()) {
                case "EMAIL":     return new EmailNotification();
                case "SMS":       return new SMSNotification();
                case "PUSH":      return new PushNotification();
                case "WHATSAPP":  return new WhatsAppNotification();
                default: throw new IllegalArgumentException("Unknown type: " + type);
            }
        }
    }

// Client — completely decoupled from concrete classes

    class AlertService {
        public void sendAlert(String type, String userId, String message) {
            Notification notification = NotificationFactory.create(type); // Don't know/care what class
            notification.send(userId, message);
            System.out.println("Alert sent via: " + notification.getType());
        }
    }

// Usage

    AlertService alertService = new AlertService();
    alertService.sendAlert("EMAIL", "user@example.com", "Your order is ready!");
    alertService.sendAlert("SMS", "+1234567890", "OTP: 4821");
    alertService.sendAlert("PUSH", "device_token_xyz", "Flash sale starts now!");

========================================================================================
Pattern 3: Abstract Factory
========================================================================================

The Concept

    Creates families of related objects without specifying their concrete classes. 
    A factory of factories.

Factory Method vs Abstract Factory — the key difference:

                Factory Method                                          Abstract Factory
    ========================================================================================
    Creates     ONE type of product                                     FAMILIES of related products
    ========================================================================================
    Focus       One product, multiple implementations                   Multiple products that belong together
    ========================================================================================
    Example     Create a Notification                                   Create a full UI (Button + TextField + Checkbox)
    ========================================================================================


java// Products — a family: Button AND Checkbox (must work together)

    interface Button   { void render(); void onClick(); }
    interface Checkbox { void render(); boolean isChecked(); }

// Windows Family

    class WindowsButton implements Button {
        public void render()   { System.out.println("Rendering Windows-style button"); }
        public void onClick()  { System.out.println("Windows button clicked"); }
    }

    class WindowsCheckbox implements Checkbox {
        private boolean checked = false;
        public void render()          { System.out.println("Rendering Windows checkbox"); }
        public boolean isChecked()    { return checked; }
    }

// Mac Family

    class MacButton implements Button {
        public void render()   { System.out.println("Rendering macOS-style button"); }
        public void onClick()  { System.out.println("Mac button clicked"); }
    }
    class MacCheckbox implements Checkbox {
        private boolean checked = false;
        public void render()       { System.out.println("Rendering macOS checkbox"); }
        public boolean isChecked() { return checked; }
    }

========================================================================================
// Abstract Factory — creates a FAMILY
========================================================================================

    interface UIFactory {
        Button createButton();
        Checkbox createCheckbox();
        // All products in the family are created together — they MATCH each other
    }

// Concrete Factories — each creates one consistent family

    class WindowsUIFactory implements UIFactory {
        public Button   createButton()   { return new WindowsButton(); }
        public Checkbox createCheckbox() { return new WindowsCheckbox(); }
    }

    class MacUIFactory implements UIFactory {
        public Button   createButton()   { return new MacButton(); }
        public Checkbox createCheckbox() { return new MacCheckbox(); }
    }

// Application — knows NOTHING about Windows or Mac

    class Application {
        private Button button;
        private Checkbox checkbox;
    
        public Application(UIFactory factory) {
            // Factory creates matching components — guaranteed to work together
            button   = factory.createButton();
            checkbox = factory.createCheckbox();
        }
    
        public void render() {
            button.render();
            checkbox.render();
        }
    }

// Wiring — only ONE place in the entire app knows the OS

    class AppRunner {
        public static void main(String[] args) {
            String os = System.getProperty("os.name");
            UIFactory factory = os.contains("Windows")
            ? new WindowsUIFactory()
            : new MacUIFactory();
    
            Application app = new Application(factory); // Rest of app is platform-agnostic
            app.render();
        }
    }

========================================================================================
Pattern 4: Builder
========================================================================================

The Concept

    Construct a complex object step by step. 
    Same construction process can create different representations.

    The Problem it Solves: Telescoping Constructors

java// WITHOUT Builder — "Telescoping Constructor" anti-pattern
    // Which boolean is which?? Impossible to read!

    Pizza p = new Pizza("Large", "Thin", true, false, true, false, true, "Extra Mozzarella");
    //                   size    crust  extraCheese? pepperoni? mushrooms? onion? olives? notes
java// WITH Builder — readable, self-documenting, flexible

    Pizza p = new Pizza.Builder("Large", "Thin Crust")
        .extraCheese()
        .mushrooms()
        .olives()
        .specialNotes("Extra crispy please")
        .build();

Full Implementation

    javaclass Pizza {
        // All final — immutable once built!
        private final String size;
        private final String crust;
        private final boolean extraCheese;
        private final boolean pepperoni;
        private final boolean mushrooms;
        private final boolean onions;
        private final boolean olives;
        private final String specialNotes;
    
        // Private constructor — ONLY Builder can create Pizza
        private Pizza(Builder builder) {
            this.size         = builder.size;
            this.crust        = builder.crust;
            this.extraCheese  = builder.extraCheese;
            this.pepperoni    = builder.pepperoni;
            this.mushrooms    = builder.mushrooms;
            this.onions       = builder.onions;
            this.olives       = builder.olives;
            this.specialNotes = builder.specialNotes;
        }
    
        @Override
        public String toString() {
            return String.format("Pizza[%s, %s crust, cheese=%b, pepperoni=%b, mushrooms=%b, notes='%s']",
                size, crust, extraCheese, pepperoni, mushrooms, specialNotes);
        }
    
        // Static inner Builder class
        public static class Builder {
            // Required parameters — set in constructor
            private final String size;
            private final String crust;
    
            // Optional parameters — default values set here
            private boolean extraCheese  = false;
            private boolean pepperoni    = false;
            private boolean mushrooms    = false;
            private boolean onions       = false;
            private boolean olives       = false;
            private String specialNotes  = "";
    
            public Builder(String size, String crust) {
                if (size == null || size.isBlank())  throw new IllegalArgumentException("Size required");
                if (crust == null || crust.isBlank()) throw new IllegalArgumentException("Crust required");
                this.size  = size;
                this.crust = crust;
            }
    
            // Each optional returns 'this' — enables chaining
            public Builder extraCheese()                  { this.extraCheese = true;   return this; }
            public Builder pepperoni()                    { this.pepperoni   = true;   return this; }
            public Builder mushrooms()                    { this.mushrooms   = true;   return this; }
            public Builder onions()                       { this.onions      = true;   return this; }
            public Builder olives()                       { this.olives      = true;   return this; }
            public Builder specialNotes(String notes)     { this.specialNotes = notes; return this; }
    
            // Validation + construction happens HERE
            public Pizza build() {
                // Add any cross-field validation
                return new Pizza(this);
            }
        }
    }

// Usage
    Pizza margherita = new Pizza.Builder("Medium", "Thin")
    .extraCheese()
    .build();
    
    Pizza supreme = new Pizza.Builder("Large", "Thick")
    .extraCheese()
    .pepperoni()
    .mushrooms()
    .onions()
    .olives()
    .specialNotes("Well done please")
    .build();
    
    System.out.println(margherita);
    System.out.println(supreme);
    Builder in real frameworks:
    java// StringBuilder — classic Builder
    String result = new StringBuilder()
    .append("Hello")
    .append(", ")
    .append("World")
    .toString();

// HTTP Request (OkHttp)

    Request request = new Request.Builder()
    .url("https://api.example.com/users")
    .addHeader("Authorization", "Bearer token")
    .post(body)
    .build();
    
    // AlertDialog (Android)
    AlertDialog dialog = new AlertDialog.Builder(context)
    .setTitle("Confirm")
    .setMessage("Are you sure?")
    .setPositiveButton("Yes", listener)
    .setNegativeButton("No", null)
    .create();

========================================================================================
STRUCTURAL PATTERNS
========================================================================================

========================================================================================
Pattern 5: Decorator
========================================================================================

The Concept
    
    Attach additional responsibilities to an object dynamically — as a runtime alternative to subclassing.
The Analogy
    
    A coffee shop. Start with simple coffee. 
    Add milk → costs more. Add sugar → costs more. Add vanilla → costs more. 
    You're not creating CoffeeWithMilkAndSugarAndVanilla class — you're wrapping coffee with behaviors.
The key insight:

    Decorator has the same interface as what it wraps. 
    So it can wrap multiple times. Stack decorators like layers.

java// Component interface

    interface Coffee {
        String getDescription();
        double getCost();
    }

// Concrete Component — the base

    class SimpleCoffee implements Coffee {
        public String getDescription() { return "Simple Coffee"; }
        public double getCost()        { return 1.00; }
    }

// Abstract Decorator — SAME interface as Coffee, WRAPS a Coffee

    abstract class CoffeeDecorator implements Coffee {
        protected final Coffee coffee;  // Wrapped object
    
        public CoffeeDecorator(Coffee coffee) {
            this.coffee = coffee;
        }
        // Delegates to wrapped object by default
        public String getDescription() { return coffee.getDescription(); }
        public double getCost()        { return coffee.getCost(); }
    }

// Concrete Decorators — each adds ONE behavior

    class MilkDecorator extends CoffeeDecorator {
        public MilkDecorator(Coffee coffee) { super(coffee); }
    
        @Override
        public String getDescription() { return coffee.getDescription() + ", Milk"; }
        @Override
        public double getCost()        { return coffee.getCost() + 0.25; }
    }

    class SugarDecorator extends CoffeeDecorator {
        public SugarDecorator(Coffee coffee) { super(coffee); }
    
        @Override
        public String getDescription() { return coffee.getDescription() + ", Sugar"; }
        @Override
        public double getCost()        { return coffee.getCost() + 0.10; }
    }

    class VanillaDecorator extends CoffeeDecorator {
        public VanillaDecorator(Coffee coffee) { super(coffee); }
    
        @Override
        public String getDescription() { return coffee.getDescription() + ", Vanilla"; }
        @Override
        public double getCost()        { return coffee.getCost() + 0.50; }
    }

    class WhipDecorator extends CoffeeDecorator {
        public WhipDecorator(Coffee coffee) { super(coffee); }
    
        @Override
        public String getDescription() { return coffee.getDescription() + ", Whip"; }
        @Override
        public double getCost()        { return coffee.getCost() + 0.35; }
    }

// Usage — stack decorators dynamically!

    Coffee order1 = new SimpleCoffee();
    System.out.println(order1.getDescription() + " = $" + order1.getCost());
    // Simple Coffee = $1.00

    Coffee order2 = new MilkDecorator(new SugarDecorator(new SimpleCoffee()));
    System.out.println(order2.getDescription() + " = $" + order2.getCost());
    // Simple Coffee, Sugar, Milk = $1.35

    Coffee order3 = new WhipDecorator(
    new VanillaDecorator(
    new MilkDecorator(
    new MilkDecorator(  // Double milk!
    new SimpleCoffee()))));
    System.out.println(order3.getDescription() + " = $" + order3.getCost());
    // Simple Coffee, Milk, Milk, Vanilla, Whip = $2.10
    Decorator in Java's own codebase:
    java// Java I/O is pure Decorator pattern
    InputStream raw      = new FileInputStream("file.txt");          // Base
    InputStream buffered = new BufferedInputStream(raw);             // Decorator 1
    DataInputStream data = new DataInputStream(buffered);            // Decorator 2
    
    // Each wraps the previous — same InputStream interface all the way down
    data.readInt();   // Buffered AND file reading — both active simultaneously


========================================================================================
Pattern 6: Facade
========================================================================================

The Concept

    Provide a simplified, unified interface to a complex subsystem. 
    Hide complexity behind one clean door.
The Analogy

    A hotel concierge. You don't call the restaurant, the taxi company, the theater, 
    and the spa separately. You call the concierge — one call, they coordinate everything.

java// Complex subsystem — many interacting classes

    class VideoDecoder {
        public byte[] decode(String filePath) {
            System.out.println("Decoding video: " + filePath);
            return new byte[0]; // decoded video data
        }
    }

    class AudioDecoder {
        public byte[] decode(String filePath) {
            System.out.println("Decoding audio: " + filePath);
            return new byte[0];
        }
    }

    class SubtitleParser {
        public List<String> parse(String filePath) {
            System.out.println("Parsing subtitles: " + filePath);
            return new ArrayList<>();
        }
    }
    
    class VideoEncoder {
        public void encode(byte[] data, String format, String output) {
            System.out.println("Encoding video to " + format + " -> " + output);
        }
    }
    
    class AudioEncoder {
        public void encode(byte[] data, String format, String output) {
            System.out.println("Encoding audio to " + format + " -> " + output);
        }
    }
    
    class FileMuxer {
        public void mux(String video, String audio, String subtitle, String output) {
            System.out.println("Muxing all streams -> " + output);
        }
    }

// FACADE — one simple interface hiding all complexity

    class VideoConverterFacade {
        private VideoDecoder   videoDecoder   = new VideoDecoder();
        private AudioDecoder   audioDecoder   = new AudioDecoder();
        private SubtitleParser subtitleParser = new SubtitleParser();
        private VideoEncoder   videoEncoder   = new VideoEncoder();
        private AudioEncoder   audioEncoder   = new AudioEncoder();
        private FileMuxer      muxer          = new FileMuxer();
    
        // Client calls ONE method — all complexity hidden
        public String convert(String inputFile, String targetFormat) {
            System.out.println("=== Starting conversion: " + inputFile + " -> " + targetFormat);
    
            byte[] rawVideo = videoDecoder.decode(inputFile);
            byte[] rawAudio = audioDecoder.decode(inputFile);
            List<String> subtitles = subtitleParser.parse(inputFile);
    
            String outputVideo = "temp_video." + targetFormat;
            String outputAudio = "temp_audio." + targetFormat;
            videoEncoder.encode(rawVideo, targetFormat, outputVideo);
            audioEncoder.encode(rawAudio, targetFormat, outputAudio);
    
            String output = inputFile.split("\\.")[0] + "." + targetFormat;
            muxer.mux(outputVideo, outputAudio, subtitles.toString(), output);
    
            System.out.println("=== Done: " + output);
            return output;
        }
    }

// Client code — clean and simple!
 
    VideoConverterFacade converter = new VideoConverterFacade();
    String result = converter.convert("movie.avi", "mp4");
    // One line vs managing 6 objects manually


========================================================================================
Pattern 7: Adapter
========================================================================================

The Concept

    Makes incompatible interfaces work together without changing either side.
The Analogy

    A power plug adapter. Your laptop has a UK 3-pin plug. 
    The Indian socket is different. The adapter sits between them — converts 
    one interface to another without changing the laptop or the wall socket.

java// Existing interface — what our system expects

    interface JsonDataProcessor {
        Map<String, Object> processData(String jsonData);
    }

// Third-party library — incompatible interface (can't modify it)

    class XMLProcessor {
        public Document parseXML(String xmlData) {
            System.out.println("Third-party XML processing...");
            return new Document(xmlData); // Returns XML Document, not Map
        }
    
        public String extractValue(Document doc, String tag) {
            return doc.getValueByTag(tag);
        }
    }

// Adapter — bridges the gap
    
    class XMLToJsonAdapter implements JsonDataProcessor {
        private XMLProcessor xmlProcessor; // Holds the adaptee
    
        public XMLToJsonAdapter(XMLProcessor xmlProcessor) {
            this.xmlProcessor = xmlProcessor;
        }
    
        @Override
        public Map<String, Object> processData(String data) {
            // Convert data if it's XML-formatted
            String xmlData = convertToXmlIfNeeded(data);
    
            // Use the third-party library
            Document doc = xmlProcessor.parseXML(xmlData);
    
            // Convert result to what our system expects
            return convertDocumentToMap(doc);
        }
    
        private String convertToXmlIfNeeded(String data) {
            // Conversion logic
            return "<root>" + data + "</root>";
        }
    
        private Map<String, Object> convertDocumentToMap(Document doc) {
            Map<String, Object> result = new HashMap<>();
            result.put("name",  xmlProcessor.extractValue(doc, "name"));
            result.put("value", xmlProcessor.extractValue(doc, "value"));
            return result;
        }
    }

// Our system only knows JsonDataProcessor — no idea XML is involved

    class DataService {
        private JsonDataProcessor processor;
    
        public DataService(JsonDataProcessor processor) {
            this.processor = processor; // Could be JSON or XML via adapter
        }
    
        public void process(String data) {
            Map<String, Object> result = processor.processData(data);
            System.out.println("Processed: " + result);
        }
    }

// Usage

    XMLProcessor xmlLib = new XMLProcessor(); // Third-party
    DataService service = new DataService(new XMLToJsonAdapter(xmlLib));
    service.process("name=Alice,value=100"); // Works!

========================================================================================
BEHAVIORAL PATTERNS
========================================================================================

========================================================================================
Pattern 8: Observer
========================================================================================


The Concept

    Defines a one-to-many dependency. When the Subject changes, 
    ALL registered Observers are notified automatically.
The Analogy

    YouTube subscriptions. The channel (Subject) doesn't call each subscriber individually. 
    Subscribers register interest. When a video is uploaded, 
    ALL subscribers get notified simultaneously. Subscribers can unsubscribe anytime.

java// Observer interface

    interface Observer {
        void update(String eventType, Object data);
    }

// Subject interface


    interface Subject {
        void subscribe(String event, Observer observer);
        void unsubscribe(String event, Observer observer);
        void notifyObservers(String event, Object data);
    }

// Concrete Subject — Stock Market

    class StockMarket implements Subject {
        private Map<String, List<Observer>> subscribers = new HashMap<>();
        private Map<String, Double> stockPrices = new HashMap<>();
    
        public void subscribe(String stock, Observer observer) {
            subscribers.computeIfAbsent(stock, k -> new ArrayList<>()).add(observer);
            System.out.println("New subscriber for " + stock);
        }
    
        public void unsubscribe(String stock, Observer observer) {
            List<Observer> observers = subscribers.get(stock);
            if (observers != null) observers.remove(observer);
        }
    
        public void notifyObservers(String stock, Object price) {
            List<Observer> observers = subscribers.getOrDefault(stock, new ArrayList<>());
            System.out.println("Notifying " + observers.size() + " observers for " + stock);
            observers.forEach(o -> o.update(stock, price));
        }
    
        // Business method — triggers notifications
        public void updatePrice(String stock, double newPrice) {
            double oldPrice = stockPrices.getOrDefault(stock, 0.0);
            stockPrices.put(stock, newPrice);
            System.out.printf("%n=== %s price changed: $%.2f -> $%.2f%n", stock, oldPrice, newPrice);
            notifyObservers(stock, newPrice);  // All subscribers notified!
        }
    }

// Concrete Observers — each reacts differently to same event

    class PriceAlertObserver implements Observer {
        private String name;
        private double threshold;
    
        public PriceAlertObserver(String name, double threshold) {
            this.name = name;
            this.threshold = threshold;
        }
    
        @Override
        public void update(String stock, Object data) {
            double price = (Double) data;
            if (price > threshold) {
                System.out.printf("🚨 ALERT [%s]: %s crossed $%.2f threshold! Now at $%.2f%n",
                    name, stock, threshold, price);
            }
        }
    }

    class PortfolioTracker implements Observer {
        private String investorName;
        private Map<String, Integer> holdings = new HashMap<>();
    
        public PortfolioTracker(String name) { this.investorName = name; }
    
        public void addHolding(String stock, int shares) { holdings.put(stock, shares); }
    
        @Override
        public void update(String stock, Object data) {
            double price = (Double) data;
            int shares = holdings.getOrDefault(stock, 0);
            double value = price * shares;
            System.out.printf("📊 Portfolio [%s]: %s × %d shares = $%.2f%n",
                investorName, stock, shares, value);
        }
    }

    class TradingBot implements Observer {
        private double buyBelow;
        private double sellAbove;
    
        public TradingBot(double buyBelow, double sellAbove) {
            this.buyBelow = buyBelow;
            this.sellAbove = sellAbove;
        }
    
        @Override
        public void update(String stock, Object data) {
            double price = (Double) data;
            if      (price < buyBelow)  System.out.printf("🤖 BOT: AUTO-BUY  %s at $%.2f%n", stock, price);
            else if (price > sellAbove) System.out.printf("🤖 BOT: AUTO-SELL %s at $%.2f%n", stock, price);
        }
    }

// Usage

    StockMarket market = new StockMarket();
    
    Observer alert     = new PriceAlertObserver("Alice", 150.0);
    Observer portfolio = new PortfolioTracker("Bob");
    Observer bot       = new TradingBot(140.0, 160.0);
    
    ((PortfolioTracker)portfolio).addHolding("AAPL", 100);
    
    market.subscribe("AAPL", alert);
    market.subscribe("AAPL", portfolio);
    market.subscribe("AAPL", bot);
    
    market.updatePrice("AAPL", 135.0);  // Bot buys
    market.updatePrice("AAPL", 152.0);  // Alert fires, bot sells
    market.unsubscribe("AAPL", bot);    // Bot unsubscribes
    market.updatePrice("AAPL", 145.0);  // Only alert + portfolio notified

========================================================================================
Pattern 9: Strategy
========================================================================================

The Concept
    
    Define a family of algorithms, encapsulate each, make them interchangeable at runtime.

The core insight
    
    When you see a class with a big if-else or switch based on TYPE — 
    that's Strategy pattern waiting to happen.

java// Strategy interface — the algorithm contract

    interface SortStrategy {
        void sort(int[] array);
        String getName();
    }

// Concrete Strategies — each is its own class

    class BubbleSort implements SortStrategy {
        public void sort(int[] arr) {
            System.out.println("BubbleSort: O(n²) — ok for tiny arrays");
            for (int i = 0; i < arr.length - 1; i++)
            for (int j = 0; j < arr.length - 1 - i; j++)
            if (arr[j] > arr[j+1]) { int t = arr[j]; arr[j] = arr[j+1]; arr[j+1] = t; }
        }
        public String getName() { return "BubbleSort"; }
    }

    class QuickSort implements SortStrategy {
        public void sort(int[] arr) {
            System.out.println("QuickSort: O(n log n) avg — great for general use");
            // ... quicksort implementation
        }
        public String getName() { return "QuickSort"; }
    }

    class MergeSort implements SortStrategy {
        public void sort(int[] arr) {
            System.out.println("MergeSort: O(n log n) stable — best for linked lists");
            // ... mergesort implementation
        }
        public String getName() { return "MergeSort"; }
    }

// Context — uses strategy, doesn't know which one

    class DataSorter {
        private SortStrategy strategy;
    
        public DataSorter(SortStrategy strategy) {
            this.strategy = strategy;
        }
    
        // Swap strategy at RUNTIME — key power of Strategy pattern
        public void setStrategy(SortStrategy strategy) {
            System.out.println("Switching to: " + strategy.getName());
            this.strategy = strategy;
        }
    
        public void sort(int[] data) {
            System.out.print("Sorting " + data.length + " elements using ");
            strategy.sort(data);
        }
    }

// Real-world Strategy: Payment

    interface PaymentStrategy {
        boolean pay(double amount);
        String getPaymentMethod();
    }

    class CreditCardPayment implements PaymentStrategy {
        private String cardNumber;
        private String cvv;
    
        public CreditCardPayment(String cardNumber, String cvv) {
            this.cardNumber = cardNumber;
            this.cvv = cvv;
        }
    
        public boolean pay(double amount) {
            System.out.printf("💳 Charging $%.2f to card ending %s%n",
                amount, cardNumber.substring(cardNumber.length() - 4));
            return true; // Payment gateway call
        }
        public String getPaymentMethod() { return "CREDIT_CARD"; }
    }

    class UPIPayment implements PaymentStrategy {
        private String upiId;
    
        public UPIPayment(String upiId) { this.upiId = upiId; }
    
        public boolean pay(double amount) {
            System.out.printf("📱 UPI payment of $%.2f to/from %s%n", amount, upiId);
            return true;
        }
        public String getPaymentMethod() { return "UPI"; }
    }

    class CryptoPayment implements PaymentStrategy {
        private String walletAddress;
    
        public CryptoPayment(String walletAddress) { this.walletAddress = walletAddress; }
    
        public boolean pay(double amount) {
            System.out.printf("₿ Crypto payment of $%.2f to wallet %s%n", amount, walletAddress);
            return true;
        }
        public String getPaymentMethod() { return "CRYPTO"; }
    }

    class ShoppingCart {
        private List<String> items = new ArrayList<>();
        private double total = 0;
        private PaymentStrategy paymentStrategy;
    
        public void addItem(String item, double price) {
            items.add(item);
            total += price;
        }
    
        public void setPaymentStrategy(PaymentStrategy strategy) {
            this.paymentStrategy = strategy;
        }
    
        public void checkout() {
            System.out.println("=== Checkout ===");
            items.forEach(i -> System.out.println("  - " + i));
            System.out.printf("Total: $%.2f%n", total);
            boolean success = paymentStrategy.pay(total);
            System.out.println(success ? "✅ Payment successful!" : "❌ Payment failed!");
        }
    }

// Usage


    ShoppingCart cart = new ShoppingCart();
    cart.addItem("Laptop", 999.99);
    cart.addItem("Mouse", 29.99);
    
    cart.setPaymentStrategy(new CreditCardPayment("4111111111111234", "123"));
    cart.checkout();
    
    // User switches to UPI — no cart code changes!
    cart.setPaymentStrategy(new UPIPayment("alice@paytm"));
    cart.checkout();

========================================================================================
Pattern 10: Command
========================================================================================

The Concept
    
    Encapsulate a request as an object. This lets you parameterize methods, queue requests, 
    log them, and support undo/redo.

The 4 components

    Command interface: execute() + undo()
    Concrete Command: wraps receiver + action
    Invoker: holds and fires commands
    Receiver: does the actual work

java// Command interface

    interface Command {
        void execute();
        void undo();
        String getDescription();
    }

// Receiver — does the actual work

    class TextDocument {
        private StringBuilder content = new StringBuilder();
    
        public void insertText(String text, int position) {
            content.insert(position, text);
            System.out.println("Document: \"" + content + "\"");
        }
    
        public void deleteText(int start, int length) {
            content.delete(start, start + length);
            System.out.println("Document: \"" + content + "\"");
        }
    
        public void applyFormatting(int start, int end, String format) {
            System.out.println("Applied " + format + " from " + start + " to " + end);
        }
    
        public String getContent() { return content.toString(); }
    }

// Concrete Commands

    class InsertCommand implements Command {
        private TextDocument doc;
        private String text;
        private int position;
    
        public InsertCommand(TextDocument doc, String text, int position) {
            this.doc = doc; this.text = text; this.position = position;
        }
    
        public void execute() { doc.insertText(text, position); }
        public void undo()    { doc.deleteText(position, text.length()); }
        public String getDescription() { return "Insert '" + text + "' at " + position; }
    }

    class DeleteCommand implements Command {
        private TextDocument doc;
        private int start, length;
        private String deletedText; // Save for undo!
    
        public DeleteCommand(TextDocument doc, int start, int length) {
            this.doc = doc; this.start = start; this.length = length;
        }
    
        public void execute() {
            deletedText = doc.getContent().substring(start, start + length); // Save first!
            doc.deleteText(start, length);
        }
    
        public void undo() { doc.insertText(deletedText, start); } // Restore saved text
        public String getDescription() { return "Delete " + length + " chars at " + start; }
    }

// Invoker — manages command history

    class CommandHistory {
        private Deque<Command> undoStack = new ArrayDeque<>();
        private Deque<Command> redoStack = new ArrayDeque<>();
    
        public void execute(Command command) {
            command.execute();
            undoStack.push(command);
            redoStack.clear(); // New command clears redo history
            System.out.println("✅ Executed: " + command.getDescription());
        }
    
        public void undo() {
            if (undoStack.isEmpty()) {
                System.out.println("❌ Nothing to undo");
                return;
            }
            Command command = undoStack.pop();
            command.undo();
            redoStack.push(command);
            System.out.println("↩️  Undid: " + command.getDescription());
        }
    
        public void redo() {
            if (redoStack.isEmpty()) {
                System.out.println("❌ Nothing to redo");
                return;
            }
            Command command = redoStack.pop();
            command.execute();
            undoStack.push(command);
            System.out.println("↪️  Redid: " + command.getDescription());
        }
    
        public void printHistory() {
            System.out.println("History: " + undoStack.size() + " commands");
            undoStack.forEach(c -> System.out.println("  - " + c.getDescription()));
        }
    }

// Usage — just like a real text editor

    TextDocument doc = new TextDocument();
    CommandHistory history = new CommandHistory();
    
    history.execute(new InsertCommand(doc, "Hello", 0));
    history.execute(new InsertCommand(doc, " World", 5));
    history.execute(new InsertCommand(doc, "!", 11));
    
    history.undo();  // Removes "!"
    history.undo();  // Removes " World"
    history.redo();  // Brings back " World"
    
    history.printHistory();


========================================================================================
Pattern 11: Template Method
========================================================================================

The Concept

    Define the skeleton of an algorithm in a base class. 
    Let subclasses override specific steps without changing the overall structure.
The Analogy
    
    A recipe template: Prepare ingredients → Cook → Plate → Serve. 
    Every dish follows this skeleton. But HOW you cook differs by dish.

java// Abstract class with template method

    abstract class DataMiner {
    
        // TEMPLATE METHOD — the algorithm skeleton (final = no override!)
        public final void mine(String path) {
            System.out.println("=== Mining: " + path + " ===");
            String rawData  = extractData(path);         // Step 1
            String parsed   = parseData(rawData);         // Step 2
            String analyzed = analyzeData(parsed);        // Step 3
            String report   = formatReport(analyzed);     // Step 4 — has default!
            sendReport(report);                           // Step 5 — has default!
        }
    
        // Abstract steps — MUST be implemented by subclasses
        protected abstract String extractData(String path);
        protected abstract String parseData(String rawData);
        protected abstract String analyzeData(String data);
    
        // Hook methods — CAN be overridden (have default behavior)
        protected String formatReport(String data) {
            return "=== Report ===\n" + data;
        }
    
        protected void sendReport(String report) {
            System.out.println("📧 Sending default report:");
            System.out.println(report);
        }
    }

// Subclass overrides ONLY what's different

    class PDFDataMiner extends DataMiner {
        protected String extractData(String path) {
            return "Extracted PDF content from: " + path;
        }
        protected String parseData(String raw) {
            return "Parsed PDF tables and text: " + raw;
        }
        protected String analyzeData(String data) {
            return "PDF Analysis: Found 5 key metrics in " + data;
        }
        // formatReport and sendReport use defaults
    }
    
    class CSVDataMiner extends DataMiner {
        protected String extractData(String path) {
        return "Read CSV rows from: " + path;
        }
        protected String parseData(String raw) {
        return "Parsed " + raw.split(",").length + " columns";
        }
        protected String analyzeData(String data) {
        return "CSV Analysis: Calculated averages for " + data;
        }
    
        // Override hook — CSV sends to Slack instead
        @Override
        protected void sendReport(String report) {
            System.out.println("💬 Sending to Slack channel: " + report);
        }
    }

// Usage — same skeleton, different implementations

    DataMiner pdfMiner = new PDFDataMiner();
    pdfMiner.mine("report.pdf");
    
    DataMiner csvMiner = new CSVDataMiner();
    csvMiner.mine("data.csv");

========================================================================================
Pattern 12: State
========================================================================================

The Concept

    Allow an object to change its behavior when its internal state changes. 
    The object will appear to change its class.
The Analogy
    
    A traffic light. RED → YELLOW → GREEN → RED. Same light object, 
    completely different behavior based on current state.

java// State interface

    interface TrafficLightState {
        void handle(TrafficLight light);
        String getColor();
    }

// Concrete States

    class RedState implements TrafficLightState {
        public void handle(TrafficLight light) {
            System.out.println("🔴 RED — Stop! Waiting 60 seconds...");
            // After timeout, transition to Green
            light.setState(new GreenState());
        }
        public String getColor() { return "RED"; }
    }
    
    class GreenState implements TrafficLightState {
        public void handle(TrafficLight light) {
            System.out.println("🟢 GREEN — Go! Waiting 45 seconds...");
            light.setState(new YellowState());
        }
        public String getColor() { return "GREEN"; }
    }
    
    class YellowState implements TrafficLightState {
        public void handle(TrafficLight light) {
            System.out.println("🟡 YELLOW — Caution! Waiting 5 seconds...");
            light.setState(new RedState());
        }
        public String getColor() { return "YELLOW"; }
    }

// Context

    class TrafficLight {
        private TrafficLightState state;
    
        public TrafficLight() {
            state = new RedState(); // Initial state
        }
    
        public void setState(TrafficLightState state) {
            this.state = state;
        }
    
        public void change() {
            state.handle(this); // Delegate to current state
        }
    
        public String getColor() { return state.getColor(); }
    }

// Real-world: Vending Machine states

    interface VendingMachineState {
        void insertCoin(VendingMachine vm);
        void selectProduct(VendingMachine vm, String product);
        void dispense(VendingMachine vm);
        void refund(VendingMachine vm);
    }

    class IdleState implements VendingMachineState {
        public void insertCoin(VendingMachine vm) {
            System.out.println("💰 Coin inserted. Select product.");
            vm.setState(new HasMoneyState());
        }
        public void selectProduct(VendingMachine vm, String product) {
            System.out.println("❌ Please insert coin first.");
        }
        public void dispense(VendingMachine vm) {
            System.out.println("❌ Insert coin and select product first.");
        }
        public void refund(VendingMachine vm) {
            System.out.println("❌ No coin to refund.");
        }
    }

    class HasMoneyState implements VendingMachineState {
        public void insertCoin(VendingMachine vm) {
            System.out.println("💰 Additional coin added.");
        }
        public void selectProduct(VendingMachine vm, String product) {
            System.out.println("✅ " + product + " selected. Dispensing...");
            vm.setState(new DispensingState());
            vm.dispense();
        }
        public void dispense(VendingMachine vm) {
            System.out.println("❌ Select a product first.");
        }
        public void refund(VendingMachine vm) {
            System.out.println("💵 Coin refunded.");
            vm.setState(new IdleState());
        }
    }

    class DispensingState implements VendingMachineState {
        public void insertCoin(VendingMachine vm)             { System.out.println("⏳ Please wait, dispensing..."); }
        public void selectProduct(VendingMachine vm, String p){ System.out.println("⏳ Please wait, dispensing..."); }
        public void dispense(VendingMachine vm) {
        System.out.println("🎁 Product dispensed! Enjoy!");
        vm.setState(new IdleState());
        }
        public void refund(VendingMachine vm)                 { System.out.println("❌ Cannot refund, dispensing."); }
        }

========================================================================================
The Q&A Round
========================================================================================

Q1. When would you choose Decorator over Inheritance for adding behavior?

    "I choose Decorator when behavior needs to be composed dynamically at runtime, 
    or when I need multiple combinations without a class explosion. 
    If I have 5 coffee add-ons, inheritance would need 2⁵ = 32 subclasses to cover all combinations.
    Decorator needs just 5 decorator classes — and they stack freely. 
    Inheritance is static at compile time. Decorator is dynamic at runtime. 
    When the combinations are open-ended, Decorator wins every time."


Q2. What's the difference between Observer and Event Bus/Pub-Sub?

    "In classic Observer, subjects hold direct references to observers — 
    the subject knows who's listening. In Pub-Sub / Event Bus, publishers and 
    subscribers are completely decoupled — they don't know each other. 
    A message broker sits between them. Observer is synchronous and direct. 
    Event Bus can be asynchronous and distributed. In a monolith, Observer works great. 
    In a microservices system, you'd use Kafka or RabbitMQ — that's Pub-Sub at scale."


Q3. Can you combine multiple design patterns?

    "Absolutely — and that's how real systems work. 
    In the Parking Lot system: Singleton for ParkingLot, 
    Factory for creating vehicle types, Strategy for payment methods, 
    Observer for notifying when spots open up, and State for managing spot status. 
    Patterns complement each other. Factory + Strategy is very common — 
    factory creates the right strategy. Observer + Command is common too — 
    commands trigger observer notifications."


Q4. When would you NOT use Singleton?

    "When the class has state that might need to be different in different contexts — 
    like in tests. Singletons make unit testing harder because you can't easily substitute 
    them with mocks. They also create hidden global state. I prefer dependency injection — 
    pass the object in rather than calling getInstance(). 
    That way tests can inject their own instance. I use Singleton sparingly — 
    Logger and Config are legitimate. For services and repositories, 
    I prefer DI containers to manage single instances."


Q5. What's the difference between Strategy and State?

    "Both use an interface with multiple implementations that the context delegates to —
    structurally similar. The difference is INTENT and WHO changes the strategy. 
    In Strategy, the CLIENT chooses and sets the algorithm — the context doesn't change it. 
    It's about swapping algorithms. In State, the STATE ITSELF changes the context's state — 
    the transitions are internal. The context doesn't pick the next state; the current state does. 
    Strategy = external algorithm selection. State = self-managing state machine."


Q6. How does the Builder pattern differ from the Factory pattern?

    "Factory focuses on WHAT to create — it returns a complete object, 
    hiding which concrete class was instantiated. Builder focuses on HOW to create — 
    it constructs a complex object step by step. Factory is for type selection. 
    Builder is for complex construction with many optional parameters. 
    You can combine them: a factory that returns the appropriate builder, 
    or a builder that internally uses factories."


Q7. What is the Prototype pattern and when would you use it?

    "Prototype creates new objects by CLONING an existing object rather than calling a constructor. 
    Useful when object creation is expensive — like loading configuration from disk, 
    or when you need many similar objects with small variations. 
    Instead of creating 1000 bullet objects from scratch, create one prototype and clone it. 
    Java's Cloneable interface is the classic example, though I prefer copy constructors or 
    serialization-based cloning for clarity."

    javainterface Prototype { Prototype clone(); }

    class GameCharacter implements Prototype {
        private String name;
        private int health, attack, defense;
        private List<String> inventory;
    
        public GameCharacter(String name, int hp, int atk, int def) {
            this.name = name; this.health = hp;
            this.attack = atk; this.defense = def;
            this.inventory = new ArrayList<>();
            // Imagine expensive initialization here
        }
    
        // Copy constructor — clean alternative to Cloneable
        private GameCharacter(GameCharacter source) {
            this.name    = source.name;
            this.health  = source.health;
            this.attack  = source.attack;
            this.defense = source.defense;
            this.inventory = new ArrayList<>(source.inventory); // Deep copy!
        }
    
        @Override
        public GameCharacter clone() { return new GameCharacter(this); }
    }

// Usage — clone is cheaper than new

    GameCharacter warrior = new GameCharacter("Warrior", 100, 50, 30);
    warrior.addItem("Sword");
    
    GameCharacter clone1 = warrior.clone(); // Fast clone
    GameCharacter clone2 = warrior.clone(); // Another clone
    clone1.setName("Warrior-Alpha");
    clone2.setName("Warrior-Beta");
    // warrior is unchanged — deep copy worked!

Q8. When does the Facade pattern become an anti-pattern?

    "Facade becomes an anti-pattern when it becomes a God object — too many methods,
    wrapping too many subsystems. It can also be overused to hide bad design rather than fix it. 
    The subsystem SHOULD be refactored; the Facade just papers over it. A good Facade is thin — 
    it simplifies access to a well-designed subsystem. A bad Facade is thick — 50 methods 
    masking a poorly designed one. I also watch for Facade becoming a bottleneck — 
    if everything goes through it, it can become a single point of failure."


Q9. How would you implement undo/redo in a drawing application?

    "Command pattern with two stacks — undo stack and redo stack. 
    Every user action (draw line, change color, delete shape) is a Command object with execute() 
    and undo(). When executed, push to undo stack. When user hits Ctrl+Z, pop from undo stack, 
    call undo(), push to redo stack. Ctrl+Y pops from redo stack, calls execute() again, 
    pushes back to undo stack. New action clears redo stack. For a drawing app, each command saves
    the previous state of affected pixels or shapes for restoration."


Q10. What's the Chain of Responsibility pattern? Give a real example.

    "Chain of Responsibility passes a request along a CHAIN of handlers. 
    Each handler decides: can I handle this? If yes, handle it. If no, pass to next. 
    Classic example: ATM cash dispensing — request for $530: $500 handler takes $500, passes 
    remaining $30 → $20 handler takes $20, passes $10 → $10 handler takes $10, done. Another: 
    HTTP middleware pipeline — auth middleware → logging middleware → rate limiting → actual handler. 
    Each middleware either handles and stops, or passes along the chain."

    javaabstract class CashHandler {
        protected CashHandler next;
        protected int denomination;
    
        public CashHandler setNext(CashHandler next) {
            this.next = next;
            return next; // Enables chaining setNext calls
        }
    
        public abstract void dispense(int amount);
    }

    class HundredHandler extends CashHandler {
        public HundredHandler() { denomination = 100; }
        public void dispense(int amount) {
            int notes = amount / denomination;
            int remainder = amount % denomination;
            if (notes > 0) System.out.println("Dispensing " + notes + " × $100");
            if (remainder > 0 && next != null) next.dispense(remainder);
        }
    }
    
    class TwentyHandler extends CashHandler {
        public TwentyHandler() { denomination = 20; }
        public void dispense(int amount) {
            int notes = amount / denomination;
            int remainder = amount % denomination;
            if (notes > 0) System.out.println("Dispensing " + notes + " × $20");
            if (remainder > 0 && next != null) next.dispense(remainder);
        }
    }

    class TenHandler extends CashHandler {
        public TenHandler() { denomination = 10; }
        public void dispense(int amount) {
            int notes = amount / denomination;
            if (notes > 0) System.out.println("Dispensing " + notes + " × $10");
        }
    }

// Chain setup

    CashHandler hundreds = new HundredHandler();
    CashHandler twenties = new TwentyHandler();
    CashHandler tens     = new TenHandler();
    hundreds.setNext(twenties).setNext(tens); // Chain!
    
    hundreds.dispense(530);
    // Dispensing 5 × $100
    // Dispensing 1 × $20
    // Dispensing 1 × $10



## Pattern Quick-Reference Table

    | Pattern                       | Category    | One Line                            | Real-World |
    --------------------------------------------------------------------------------------------------------
    | **Singleton**                 | Creational | One instance, global access          | Logger, Config |
    --------------------------------------------------------------------------------------------------------
    | **Factory**                   | Creational | Create without specifying class      | Notification factory |
    --------------------------------------------------------------------------------------------------------
    | **Abstract Factory**          | Creational | Families of related objects          | Cross-platform UI |
    --------------------------------------------------------------------------------------------------------
    | **Builder**                   | Creational | Complex object, step by step         | Pizza, HTTP Request |
    --------------------------------------------------------------------------------------------------------
    | **Prototype**                 | Creational | Clone existing object                | Game characters |
    --------------------------------------------------------------------------------------------------------
    | **Decorator**                 | Structural | Add behavior dynamically             | Coffee add-ons, IO Streams |
    --------------------------------------------------------------------------------------------------------
    | **Facade**                    | Structural | Simplify complex subsystem           | Video converter API |
    --------------------------------------------------------------------------------------------------------
    | **Adapter**                   | Structural | Make incompatible interfaces work    | Power plug adapter |
    --------------------------------------------------------------------------------------------------------
    | **Composite**                 | Structural | Tree of objects, same interface      | File system |
    --------------------------------------------------------------------------------------------------------
    | **Proxy**                     | Structural | Control access to object             | Lazy loading, cache |
    --------------------------------------------------------------------------------------------------------
    | **Observer**                  | Behavioral | One-to-many notification             | Stock alerts, YouTube |
    --------------------------------------------------------------------------------------------------------
    | **Strategy**                  | Behavioral | Swap algorithms at runtime           | Payment methods |
    --------------------------------------------------------------------------------------------------------
    | **Command**                   | Behavioral | Encapsulate request as object        | Undo/Redo, Queue |
    --------------------------------------------------------------------------------------------------------
    | **Template Method**           | Behavioral | Algorithm skeleton, steps vary       | Data miners |
    --------------------------------------------------------------------------------------------------------
    | **State**                     | Behavioral | Behavior changes with state          | ATM, Traffic light |
    --------------------------------------------------------------------------------------------------------
    | **Chain of Responsibility**   | Behavioral | Pass request along chain             | ATM cash, Middleware |
    --------------------------------------------------------------------------------------------------------


## The Golden Decision Tree
```
Problem involves object CREATION?
→ Only one instance needed?           → Singleton
→ Create based on type?               → Factory Method
→ Create families of objects?         → Abstract Factory
→ Many optional parameters?           → Builder
→ Clone expensive objects?            → Prototype

Problem involves object COMPOSITION?
→ Add behavior dynamically?           → Decorator
→ Simplify complex subsystem?         → Facade
→ Incompatible interfaces?            → Adapter
→ Control access / lazy load?         → Proxy
→ Tree structure?                     → Composite

Problem involves object COMMUNICATION?
→ One-to-many notifications?          → Observer
→ Swappable algorithms?               → Strategy
→ Undo/Redo / Queue operations?       → Command
→ Object behavior changes by state?   → State
→ Algorithm skeleton, steps vary?     → Template Method
→ Pass request along handlers?        → Chain of Responsibility
```
