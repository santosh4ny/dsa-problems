
Section 4: LLD Case Studies 🏗️
This is where everything comes together. Real interview problems, solved step by step, exactly how you'd walk an interviewer through it. For every problem I'll follow the 6-Step Framework from Section 1 of the guide.

The 6-Step Framework (Always follow this)
STEP 1 → Clarify Requirements        (2-3 min)
STEP 2 → Identify Core Entities      (2-3 min)
STEP 3 → Define Relationships        (2-3 min)
STEP 4 → Identify Design Patterns    (2-3 min)
STEP 5 → Class Hierarchy + APIs      (5-7 min)
STEP 6 → Code + Justify Decisions    (15-20 min)

Case Study 1: Parking Lot 🅿️

Most asked LLD question in FAANG. If you can't do this cold, practice until you can.


STEP 1 — Clarify
Say this out loud in the interview:

"Before I start, let me ask a few clarifying questions. Does the parking lot have multiple floors? What types of vehicles does it support — motorcycles, cars, trucks? Do we need multiple entry and exit gates? What payment methods — cash, card, online? Do we need reservation/booking in advance? Should we track real-time availability?"

Assumptions for this design:

Multiple floors, multiple spots per floor
Vehicle types: Motorcycle, Car, Truck
Spot types: Compact (motorcycle), Regular (car), Large (truck)
Multiple entry + exit gates
Cash and card payment
Real-time availability tracking


STEP 2 — Entities
ParkingLot → ParkingFloor → ParkingSpot
Vehicle (Motorcycle, Car, Truck)
Ticket, Payment
EntryGate, ExitGate
ParkingAttendant, Admin

STEP 3 — Relationships
ParkingLot     HAS-A   List<ParkingFloor>
ParkingFloor   HAS-A   List<ParkingSpot>
ParkingSpot    CAN-HOLD  Vehicle
Ticket         LINKS     Vehicle ↔ ParkingSpot
Payment        BELONGS-TO  Ticket

STEP 4 — Patterns
Singleton  → ParkingLot (only one)
Factory    → Create vehicle types
Strategy   → Payment (Cash / Card)
Observer   → Notify when spot becomes available
State      → Spot status (Available / Occupied / Maintenance)

STEP 5 + 6 — Full Code
java// ─── ENUMS ───────────────────────────────────────────────
enum VehicleType  { MOTORCYCLE, CAR, TRUCK }
enum SpotType     { COMPACT, REGULAR, LARGE }
enum SpotStatus   { AVAILABLE, OCCUPIED, MAINTENANCE }
enum PaymentStatus { PENDING, COMPLETED, FAILED, REFUNDED }

// ─── VEHICLE HIERARCHY ───────────────────────────────────
abstract class Vehicle {
protected String licensePlate;
protected VehicleType type;
protected String color;

    public Vehicle(String licensePlate, String color) {
        this.licensePlate = licensePlate;
        this.color = color;
    }

    public abstract VehicleType getType();
    public abstract SpotType getRequiredSpotType();
    public String getLicensePlate() { return licensePlate; }
}

class Motorcycle extends Vehicle {
public Motorcycle(String plate, String color) { super(plate, color); }
public VehicleType getType()        { return VehicleType.MOTORCYCLE; }
public SpotType getRequiredSpotType() { return SpotType.COMPACT; }
}

class Car extends Vehicle {
public Car(String plate, String color) { super(plate, color); }
public VehicleType getType()        { return VehicleType.CAR; }
public SpotType getRequiredSpotType() { return SpotType.REGULAR; }
}

class Truck extends Vehicle {
public Truck(String plate, String color) { super(plate, color); }
public VehicleType getType()        { return VehicleType.TRUCK; }
public SpotType getRequiredSpotType() { return SpotType.LARGE; }
}

// ─── PARKING SPOT ────────────────────────────────────────
class ParkingSpot {
private String spotId;         // e.g., "F1-R-012" (Floor1-Regular-012)
private SpotType type;
private SpotStatus status;
private Vehicle currentVehicle;
private int floorNumber;

    public ParkingSpot(String spotId, SpotType type, int floorNumber) {
        this.spotId      = spotId;
        this.type        = type;
        this.floorNumber = floorNumber;
        this.status      = SpotStatus.AVAILABLE;
    }

    public boolean isAvailable() { return status == SpotStatus.AVAILABLE; }

    public boolean park(Vehicle vehicle) {
        if (!isAvailable()) return false;
        // Validate vehicle fits this spot
        if (!isCompatible(vehicle)) return false;
        this.currentVehicle = vehicle;
        this.status = SpotStatus.OCCUPIED;
        return true;
    }

    public Vehicle unpark() {
        if (status != SpotStatus.OCCUPIED) return null;
        Vehicle v = currentVehicle;
        currentVehicle = null;
        status = SpotStatus.AVAILABLE;
        return v;
    }

    private boolean isCompatible(Vehicle v) {
        // Compact fits Motorcycle
        // Regular fits Car (or Motorcycle in a pinch)
        // Large fits Truck (or Car/Motorcycle)
        return v.getRequiredSpotType() == this.type;
    }

    public String getSpotId()   { return spotId; }
    public SpotType getType()   { return type; }
    public SpotStatus getStatus() { return status; }
    public void setStatus(SpotStatus s) { this.status = s; }
}

// ─── TICKET ──────────────────────────────────────────────
class ParkingTicket {
private static int counter = 1000;

    private String ticketId;
    private Vehicle vehicle;
    private ParkingSpot spot;
    private LocalDateTime entryTime;
    private LocalDateTime exitTime;
    private double amountDue;
    private boolean isPaid;

    public ParkingTicket(Vehicle vehicle, ParkingSpot spot) {
        this.ticketId  = "TKT-" + (++counter);
        this.vehicle   = vehicle;
        this.spot      = spot;
        this.entryTime = LocalDateTime.now();
        this.isPaid    = false;
    }

    public double calculateAmount() {
        LocalDateTime exit = LocalDateTime.now();
        long minutes = ChronoUnit.MINUTES.between(entryTime, exit);
        long hours = (long) Math.ceil(minutes / 60.0); // Round up to nearest hour

        double rate;
        switch (spot.getType()) {
            case COMPACT:  rate = 2.0; break;  // $2/hour for motorcycles
            case REGULAR:  rate = 4.0; break;  // $4/hour for cars
            case LARGE:    rate = 6.0; break;  // $6/hour for trucks
            default:       rate = 4.0;
        }

        this.amountDue = Math.max(hours, 1) * rate; // Minimum 1 hour charge
        this.exitTime  = exit;
        return amountDue;
    }

    public void markPaid()   { this.isPaid = true; }
    public boolean isPaid()  { return isPaid; }
    public String getTicketId() { return ticketId; }
    public ParkingSpot getSpot() { return spot; }
    public Vehicle getVehicle()  { return vehicle; }
    public double getAmountDue() { return amountDue; }

    @Override
    public String toString() {
        return String.format("Ticket[%s | Vehicle: %s | Spot: %s | Entry: %s | Amount: $%.2f]",
            ticketId, vehicle.getLicensePlate(), spot.getSpotId(), entryTime, amountDue);
    }
}

// ─── PAYMENT STRATEGY ────────────────────────────────────
interface PaymentStrategy {
boolean pay(double amount, String ticketId);
String getMethodName();
}

class CashPayment implements PaymentStrategy {
private double cashReceived;

    public CashPayment(double cashReceived) {
        this.cashReceived = cashReceived;
    }

    public boolean pay(double amount, String ticketId) {
        if (cashReceived < amount) {
            System.out.printf("❌ Insufficient cash. Need $%.2f, got $%.2f%n", amount, cashReceived);
            return false;
        }
        double change = cashReceived - amount;
        System.out.printf("💵 Cash payment of $%.2f accepted. Change: $%.2f%n", amount, change);
        return true;
    }
    public String getMethodName() { return "CASH"; }
}

class CardPayment implements PaymentStrategy {
private String cardNumber;
private String cvv;

    public CardPayment(String cardNumber, String cvv) {
        this.cardNumber = cardNumber;
        this.cvv = cvv;
    }

    public boolean pay(double amount, String ticketId) {
        // Simulate card payment gateway call
        System.out.printf("💳 Card payment of $%.2f processing for ticket %s...%n", amount, ticketId);
        System.out.println("✅ Card payment approved!");
        return true;
    }
    public String getMethodName() { return "CARD"; }
}

// ─── PARKING FLOOR ───────────────────────────────────────
class ParkingFloor {
private int floorNumber;
private Map<SpotType, List<ParkingSpot>> spotsByType;
private int totalSpots;
private int occupiedSpots;

    public ParkingFloor(int floorNumber) {
        this.floorNumber  = floorNumber;
        this.spotsByType  = new HashMap<>();
        this.occupiedSpots = 0;
        spotsByType.put(SpotType.COMPACT,  new ArrayList<>());
        spotsByType.put(SpotType.REGULAR,  new ArrayList<>());
        spotsByType.put(SpotType.LARGE,    new ArrayList<>());
    }

    public void addSpot(ParkingSpot spot) {
        spotsByType.get(spot.getType()).add(spot);
        totalSpots++;
    }

    // Find first available spot for this vehicle type
    public ParkingSpot getAvailableSpot(SpotType required) {
        return spotsByType.getOrDefault(required, Collections.emptyList())
                          .stream()
                          .filter(ParkingSpot::isAvailable)
                          .findFirst()
                          .orElse(null);
    }

    public int getAvailableCount(SpotType type) {
        return (int) spotsByType.getOrDefault(type, Collections.emptyList())
                                .stream()
                                .filter(ParkingSpot::isAvailable)
                                .count();
    }

    public boolean hasAvailableSpot(SpotType type) {
        return getAvailableCount(type) > 0;
    }

    public int getFloorNumber() { return floorNumber; }

    public void displayStatus() {
        System.out.printf("  Floor %d: Compact=%d, Regular=%d, Large=%d available%n",
            floorNumber,
            getAvailableCount(SpotType.COMPACT),
            getAvailableCount(SpotType.REGULAR),
            getAvailableCount(SpotType.LARGE));
    }
}

// ─── PARKING LOT — SINGLETON ─────────────────────────────
class ParkingLot {
private static volatile ParkingLot instance;

    private String name;
    private String address;
    private List<ParkingFloor> floors;
    private Map<String, ParkingTicket> activeTickets;   // ticketId → Ticket
    private Map<String, ParkingTicket> vehicleTickets;  // licensePlate → Ticket

    private ParkingLot(String name, String address) {
        this.name           = name;
        this.address        = address;
        this.floors         = new ArrayList<>();
        this.activeTickets  = new HashMap<>();
        this.vehicleTickets = new HashMap<>();
    }

    public static ParkingLot getInstance(String name, String address) {
        if (instance == null) {
            synchronized (ParkingLot.class) {
                if (instance == null) {
                    instance = new ParkingLot(name, address);
                }
            }
        }
        return instance;
    }

    public static ParkingLot getInstance() {
        if (instance == null) throw new IllegalStateException("ParkingLot not initialized!");
        return instance;
    }

    public void addFloor(ParkingFloor floor) { floors.add(floor); }

    // ── ENTRY: Park a vehicle ─────────────────────────────
    public ParkingTicket parkVehicle(Vehicle vehicle) {
        String plate = vehicle.getLicensePlate();

        // Check if already parked
        if (vehicleTickets.containsKey(plate)) {
            throw new IllegalStateException("Vehicle " + plate + " is already parked!");
        }

        SpotType required = vehicle.getRequiredSpotType();

        // Find spot — search floor by floor
        for (ParkingFloor floor : floors) {
            ParkingSpot spot = floor.getAvailableSpot(required);
            if (spot != null) {
                spot.park(vehicle);
                ParkingTicket ticket = new ParkingTicket(vehicle, spot);
                activeTickets.put(ticket.getTicketId(), ticket);
                vehicleTickets.put(plate, ticket);

                System.out.printf("🚗 Vehicle %s parked at Spot %s on Floor %d | Ticket: %s%n",
                    plate, spot.getSpotId(), floor.getFloorNumber(), ticket.getTicketId());
                return ticket;
            }
        }

        throw new RuntimeException("🚫 Parking lot FULL! No " + required + " spot available.");
    }

    // ── EXIT: Process exit + payment ─────────────────────
    public double processExit(String ticketId, PaymentStrategy paymentStrategy) {
        ParkingTicket ticket = activeTickets.get(ticketId);
        if (ticket == null) throw new IllegalArgumentException("Invalid ticket: " + ticketId);
        if (ticket.isPaid())  throw new IllegalStateException("Ticket already paid!");

        double amount = ticket.calculateAmount();
        System.out.printf("%n=== EXIT PROCESSING ===%n%s%n", ticket);

        boolean paid = paymentStrategy.pay(amount, ticketId);
        if (!paid) throw new RuntimeException("Payment failed!");

        ticket.markPaid();
        ticket.getSpot().unpark();  // Free the spot

        // Remove from active tracking
        activeTickets.remove(ticketId);
        vehicleTickets.remove(ticket.getVehicle().getLicensePlate());

        System.out.printf("✅ Exit processed for %s. Amount paid: $%.2f via %s%n",
            ticket.getVehicle().getLicensePlate(), amount, paymentStrategy.getMethodName());
        return amount;
    }

    public void displayAvailability() {
        System.out.println("\n📊 === PARKING LOT STATUS: " + name + " ===");
        floors.forEach(ParkingFloor::displayStatus);
        System.out.println("Active tickets: " + activeTickets.size());
    }

    public int getTotalActiveVehicles() { return vehicleTickets.size(); }
}

// ─── DEMO ────────────────────────────────────────────────
class ParkingLotDemo {
public static void main(String[] args) {
// Setup
ParkingLot lot = ParkingLot.getInstance("Central Plaza Parking", "MG Road");

        // Create floors with spots
        ParkingFloor floor1 = new ParkingFloor(1);
        for (int i = 1; i <= 3; i++)
            floor1.addSpot(new ParkingSpot("F1-C-" + i, SpotType.COMPACT,  1));
        for (int i = 1; i <= 5; i++)
            floor1.addSpot(new ParkingSpot("F1-R-" + i, SpotType.REGULAR,  1));
        for (int i = 1; i <= 2; i++)
            floor1.addSpot(new ParkingSpot("F1-L-" + i, SpotType.LARGE,    1));
        lot.addFloor(floor1);

        lot.displayAvailability();

        // Park vehicles
        Vehicle car1  = new Car("KA-01-AB-1234", "Red");
        Vehicle bike1 = new Motorcycle("KA-02-CD-5678", "Black");
        Vehicle truck = new Truck("KA-03-EF-9012", "White");

        ParkingTicket t1 = lot.parkVehicle(car1);
        ParkingTicket t2 = lot.parkVehicle(bike1);
        ParkingTicket t3 = lot.parkVehicle(truck);

        lot.displayAvailability();

        // Exit with payment
        lot.processExit(t1.getTicketId(), new CardPayment("4111111111111234", "123"));
        lot.processExit(t2.getTicketId(), new CashPayment(10.0));

        lot.displayAvailability();
    }
}
```

---

# Case Study 2: Elevator System 🛗

> *Tests your ability to model state machines and dispatch algorithms.*

---

### STEP 1 — Clarify

> *"How many elevators and floors? Do we have different elevator types — freight, passenger? Is there a priority system — fire service mode? What's the dispatch algorithm — nearest elevator, or directional priority? Do we need real-time floor display?"*

**Assumptions:** Multiple elevators, multiple floors, SCAN dispatch algorithm (serves in one direction, then reverses), no freight elevators.

---

### STEP 2 — Entities
```
ElevatorSystem, Elevator, Floor
ElevatorRequest (from floor button or cabin button)
Door, Display, Button
DispatchAlgorithm (Strategy)
```

---

### STEP 3 — Relationships
```
ElevatorSystem  HAS-A  List<Elevator>
ElevatorSystem  HAS-A  DispatchAlgorithm
Elevator        HAS-A  Door, Display
Elevator        HAS-A  Set<Integer> destinations
ElevatorRequest IS-CREATED-BY  FloorButton or CabinButton
```

---

### STEP 4 — Patterns
```
State     → Elevator states (IDLE, MOVING_UP, MOVING_DOWN, DOOR_OPEN)
Strategy  → Dispatch algorithm (Nearest, SCAN, LOOK)
Observer  → Floors observe elevator arrival
Singleton → ElevatorSystem

Full Code
java// ─── ENUMS ───────────────────────────────────────────────
enum Direction     { UP, DOWN, IDLE }
enum ElevatorState { IDLE, MOVING_UP, MOVING_DOWN, DOOR_OPEN, DOOR_CLOSED, MAINTENANCE }

// ─── ELEVATOR REQUEST ─────────────────────────────────────
class ElevatorRequest {
private int floor;
private Direction direction;   // For external (floor button) requests
private LocalDateTime timestamp;
private boolean isInternal;    // true = from inside cabin, false = from floor button

    // External request (floor button)
    public ElevatorRequest(int floor, Direction direction) {
        this.floor      = floor;
        this.direction  = direction;
        this.timestamp  = LocalDateTime.now();
        this.isInternal = false;
    }

    // Internal request (cabin button — just a destination)
    public ElevatorRequest(int floor) {
        this.floor      = floor;
        this.timestamp  = LocalDateTime.now();
        this.isInternal = true;
        this.direction  = Direction.IDLE;
    }

    public int getFloor()           { return floor; }
    public Direction getDirection() { return direction; }
    public boolean isInternal()     { return isInternal; }
}

// ─── DISPATCH STRATEGY ───────────────────────────────────
interface DispatchStrategy {
Elevator selectElevator(List<Elevator> elevators, ElevatorRequest request);
}

// Nearest elevator — simplest algorithm
class NearestElevatorStrategy implements DispatchStrategy {
public Elevator selectElevator(List<Elevator> elevators, ElevatorRequest request) {
return elevators.stream()
.filter(e -> e.getState() != ElevatorState.MAINTENANCE)
.min(Comparator.comparingInt(e ->
Math.abs(e.getCurrentFloor() - request.getFloor())))
.orElseThrow(() -> new RuntimeException("No elevator available!"));
}
}

// SCAN algorithm — better for high traffic
class SCANStrategy implements DispatchStrategy {
public Elevator selectElevator(List<Elevator> elevators, ElevatorRequest request) {
int targetFloor = request.getFloor();

        // Priority 1: Elevator already moving toward request in same direction
        Optional<Elevator> sameDirElevator = elevators.stream()
            .filter(e -> e.getState() != ElevatorState.MAINTENANCE)
            .filter(e -> isMovingToward(e, targetFloor))
            .min(Comparator.comparingInt(e -> Math.abs(e.getCurrentFloor() - targetFloor)));

        if (sameDirElevator.isPresent()) return sameDirElevator.get();

        // Priority 2: Idle elevator nearest to request
        return elevators.stream()
            .filter(e -> e.getState() != ElevatorState.MAINTENANCE)
            .min(Comparator.comparingInt(e -> Math.abs(e.getCurrentFloor() - targetFloor)))
            .orElseThrow(() -> new RuntimeException("No elevator available!"));
    }

    private boolean isMovingToward(Elevator e, int targetFloor) {
        if (e.getState() == ElevatorState.MOVING_UP   && targetFloor > e.getCurrentFloor()) return true;
        if (e.getState() == ElevatorState.MOVING_DOWN && targetFloor < e.getCurrentFloor()) return true;
        return false;
    }
}

// ─── DOOR ────────────────────────────────────────────────
class Door {
private boolean isOpen = false;
private int floor;

    public void open(int floor) {
        this.floor = floor;
        isOpen = true;
        System.out.println("🚪 Door OPEN at floor " + floor);
    }

    public void close() {
        isOpen = false;
        System.out.println("🚪 Door CLOSED at floor " + floor);
    }

    public boolean isOpen() { return isOpen; }
}

// ─── ELEVATOR ────────────────────────────────────────────
class Elevator {
private int id;
private int currentFloor;
private int totalFloors;
private ElevatorState state;
private Direction direction;
private TreeSet<Integer> destinationsUp;    // Floors to serve going UP (sorted asc)
private TreeSet<Integer> destinationsDown;  // Floors to serve going DOWN (sorted desc)
private Door door;
private int capacity;        // Max persons
private int currentLoad;

    public Elevator(int id, int totalFloors, int capacity) {
        this.id               = id;
        this.totalFloors      = totalFloors;
        this.capacity         = capacity;
        this.currentFloor     = 1;  // Start at ground floor
        this.state            = ElevatorState.IDLE;
        this.direction        = Direction.IDLE;
        this.destinationsUp   = new TreeSet<>();  // Natural order = ascending
        this.destinationsDown = new TreeSet<>(Comparator.reverseOrder()); // Descending
        this.door             = new Door();
        this.currentLoad      = 0;
    }

    // Add a destination floor
    public void addDestination(int floor) {
        if (floor == currentFloor) {
            openDoor();
            return;
        }
        if (floor > currentFloor) destinationsUp.add(floor);
        else                      destinationsDown.add(floor);

        // Update direction if currently idle
        if (state == ElevatorState.IDLE) {
            direction = (floor > currentFloor) ? Direction.UP : Direction.DOWN;
        }

        System.out.printf("  [Elevator %d] Added destination: Floor %d | Queue UP=%s DOWN=%s%n",
            id, floor, destinationsUp, destinationsDown);
    }

    // Move one step — called by scheduler tick
    public void move() {
        if (door.isOpen()) {
            door.close();
            state = (direction == Direction.UP) ? ElevatorState.MOVING_UP
                  : (direction == Direction.DOWN) ? ElevatorState.MOVING_DOWN
                  : ElevatorState.IDLE;
            return;
        }

        if (direction == Direction.UP) {
            moveUp();
        } else if (direction == Direction.DOWN) {
            moveDown();
        }
        // If IDLE — nothing to do
    }

    private void moveUp() {
        if (!destinationsUp.isEmpty()) {
            currentFloor = destinationsUp.first();  // Next floor going up
            destinationsUp.remove(currentFloor);
            System.out.printf("⬆️  [Elevator %d] Arrived at Floor %d%n", id, currentFloor);
            openDoor();

            // If no more UP destinations, switch to DOWN or IDLE
            if (destinationsUp.isEmpty()) {
                direction = destinationsDown.isEmpty() ? Direction.IDLE : Direction.DOWN;
            }
        }
    }

    private void moveDown() {
        if (!destinationsDown.isEmpty()) {
            currentFloor = destinationsDown.first();  // Next floor going down
            destinationsDown.remove(currentFloor);
            System.out.printf("⬇️  [Elevator %d] Arrived at Floor %d%n", id, currentFloor);
            openDoor();

            if (destinationsDown.isEmpty()) {
                direction = destinationsUp.isEmpty() ? Direction.IDLE : Direction.UP;
            }
        }
    }

    private void openDoor() {
        state = ElevatorState.DOOR_OPEN;
        door.open(currentFloor);
        // In real system: schedule door close after 3 seconds
    }

    public boolean isAvailable()    { return state != ElevatorState.MAINTENANCE; }
    public int getCurrentFloor()    { return currentFloor; }
    public ElevatorState getState() { return state; }
    public Direction getDirection() { return direction; }
    public int getId()              { return id; }

    public void displayStatus() {
        System.out.printf("  [Elevator %d] Floor=%d | State=%s | Direction=%s | DestUp=%s | DestDown=%s%n",
            id, currentFloor, state, direction, destinationsUp, destinationsDown);
    }
}

// ─── ELEVATOR SYSTEM — SINGLETON ─────────────────────────
class ElevatorSystem {
private static ElevatorSystem instance;
private List<Elevator> elevators;
private DispatchStrategy strategy;
private int totalFloors;
private Queue<ElevatorRequest> pendingRequests;

    private ElevatorSystem(int totalFloors, int numElevators, DispatchStrategy strategy) {
        this.totalFloors     = totalFloors;
        this.strategy        = strategy;
        this.elevators       = new ArrayList<>();
        this.pendingRequests = new LinkedList<>();

        for (int i = 1; i <= numElevators; i++) {
            elevators.add(new Elevator(i, totalFloors, 10));
        }
        System.out.printf("🏢 Elevator System: %d floors, %d elevators%n", totalFloors, numElevators);
    }

    public static ElevatorSystem getInstance(int floors, int elevators, DispatchStrategy strategy) {
        if (instance == null) instance = new ElevatorSystem(floors, elevators, strategy);
        return instance;
    }

    // External request — someone on floor X pressed UP or DOWN
    public void requestElevator(int floor, Direction direction) {
        System.out.printf("%n🔔 External request: Floor %d, Direction %s%n", floor, direction);
        ElevatorRequest request = new ElevatorRequest(floor, direction);
        Elevator selected = strategy.selectElevator(elevators, request);
        System.out.printf("  → Dispatching Elevator %d%n", selected.getId());
        selected.addDestination(floor);
    }

    // Internal request — person inside elevator pressed floor button
    public void selectFloor(int elevatorId, int floor) {
        System.out.printf("%n🔘 Internal request: Elevator %d → Floor %d%n", elevatorId, floor);
        Elevator elevator = elevators.stream()
            .filter(e -> e.getId() == elevatorId)
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Elevator " + elevatorId + " not found"));
        elevator.addDestination(floor);
    }

    // Simulate one tick of the system
    public void tick() {
        elevators.forEach(Elevator::move);
    }

    // Simulate multiple ticks
    public void simulate(int ticks) {
        for (int i = 0; i < ticks; i++) {
            System.out.println("\n--- Tick " + (i+1) + " ---");
            tick();
        }
    }

    public void displayStatus() {
        System.out.println("\n📊 === ELEVATOR SYSTEM STATUS ===");
        elevators.forEach(Elevator::displayStatus);
    }
}

// ─── DEMO ────────────────────────────────────────────────
class ElevatorDemo {
public static void main(String[] args) {
ElevatorSystem system = ElevatorSystem.getInstance(20, 3, new SCANStrategy());

        // Simulate rush hour
        system.requestElevator(5,  Direction.UP);
        system.requestElevator(12, Direction.DOWN);
        system.requestElevator(1,  Direction.UP);

        system.selectFloor(1, 15);  // Person in elevator 1 going to 15
        system.selectFloor(2, 3);   // Person in elevator 2 going to 3

        system.displayStatus();
        system.simulate(5);
        system.displayStatus();
    }
}
```

---

# Case Study 3: Ride Sharing App (Uber/Ola) 🚖

> *Tests matching, state machines, and real-time tracking design.*

---

### STEP 1 — Clarify

> *"Do we need surge pricing? Multiple vehicle categories — Auto, Mini, Prime? Real-time tracking? Rating system? Multiple payment methods? Trip history? Driver and Rider both? Cancellation policy?"*

**Assumptions:** Rider requests ride, system matches nearest driver, trip progresses through states, multiple vehicle categories, rating system.

---

### STEP 2 — Entities
```
Rider, Driver, Vehicle
Trip, TripRequest
Location
PaymentMethod, Payment
Rating
TripMatcher (matching algorithm)
```

---

### STEP 3 — Patterns
```
State    → Trip states (REQUESTED → ACCEPTED → DRIVER_ARRIVING
→ TRIP_STARTED → COMPLETED → CANCELLED)
Strategy → Pricing (Normal, Surge, Flat rate)
Strategy → Matching algorithm (Nearest, Rating-based)
Observer → Notify rider when driver accepts/arrives
Factory  → Create vehicle categories

Full Code
java// ─── ENUMS ───────────────────────────────────────────────
enum VehicleCategory { AUTO, MINI, PRIME, SUV, BIKE }
enum TripStatus {
REQUESTED, DRIVER_ASSIGNED, DRIVER_ARRIVING,
TRIP_STARTED, COMPLETED, CANCELLED
}
enum DriverStatus { AVAILABLE, ON_TRIP, OFFLINE }

// ─── LOCATION ────────────────────────────────────────────
class Location {
private double latitude;
private double longitude;
private String address;

    public Location(double lat, double lng, String address) {
        this.latitude  = lat;
        this.longitude = lng;
        this.address   = address;
    }

    // Haversine approximation (simplified)
    public double distanceTo(Location other) {
        double latDiff  = Math.abs(this.latitude  - other.latitude);
        double lngDiff  = Math.abs(this.longitude - other.longitude);
        return Math.sqrt(latDiff * latDiff + lngDiff * lngDiff) * 111; // ~km
    }

    public double getLatitude()  { return latitude;  }
    public double getLongitude() { return longitude; }
    public String getAddress()   { return address;   }

    @Override
    public String toString() { return address + " (" + latitude + "," + longitude + ")"; }
}

// ─── VEHICLE ─────────────────────────────────────────────
class Vehicle {
private String vehicleId;
private String plateNumber;
private String model;
private VehicleCategory category;
private int capacity;

    public Vehicle(String id, String plate, String model, VehicleCategory category, int capacity) {
        this.vehicleId  = id;
        this.plateNumber = plate;
        this.model      = model;
        this.category   = category;
        this.capacity   = capacity;
    }

    public VehicleCategory getCategory() { return category; }
    public String getModel()     { return model; }
    public String getPlateNumber() { return plateNumber; }
}

// ─── DRIVER ──────────────────────────────────────────────
class Driver {
private String driverId;
private String name;
private String phone;
private double rating;
private int totalTrips;
private Vehicle vehicle;
private Location currentLocation;
private DriverStatus status;

    public Driver(String id, String name, String phone, Vehicle vehicle, Location location) {
        this.driverId        = id;
        this.name            = name;
        this.phone           = phone;
        this.vehicle         = vehicle;
        this.currentLocation = location;
        this.rating          = 5.0;
        this.totalTrips      = 0;
        this.status          = DriverStatus.AVAILABLE;
    }

    public boolean isAvailable() { return status == DriverStatus.AVAILABLE; }

    public void setAvailable()   {
        status = DriverStatus.AVAILABLE;
        System.out.printf("  ✅ Driver %s is now AVAILABLE%n", name);
    }

    public void assignTrip() {
        status = DriverStatus.ON_TRIP;
        System.out.printf("  🚗 Driver %s assigned to trip%n", name);
    }

    public void updateLocation(Location loc) { this.currentLocation = loc; }

    public void updateRating(double newRating) {
        this.rating = (rating * totalTrips + newRating) / (totalTrips + 1);
        totalTrips++;
    }

    public void completeTrip() {
        totalTrips++;
        status = DriverStatus.AVAILABLE;
    }

    public double distanceTo(Location loc) { return currentLocation.distanceTo(loc); }

    public String getDriverId()       { return driverId; }
    public String getName()           { return name; }
    public double getRating()         { return rating; }
    public Location getCurrentLocation() { return currentLocation; }
    public Vehicle getVehicle()       { return vehicle; }
    public DriverStatus getStatus()   { return status; }
}

// ─── RIDER ───────────────────────────────────────────────
class Rider {
private String riderId;
private String name;
private String phone;
private double rating;
private int totalTrips;
private List<Trip> tripHistory;
private Location savedHome;
private Location savedWork;

    public Rider(String id, String name, String phone) {
        this.riderId    = id;
        this.name       = name;
        this.phone      = phone;
        this.rating     = 5.0;
        this.totalTrips = 0;
        this.tripHistory = new ArrayList<>();
    }

    public void addTrip(Trip trip) {
        tripHistory.add(trip);
        totalTrips++;
    }

    public String getRiderId() { return riderId; }
    public String getName()    { return name; }
    public double getRating()  { return rating; }
    public List<Trip> getTripHistory() { return Collections.unmodifiableList(tripHistory); }
}

// ─── PRICING STRATEGY ────────────────────────────────────
interface PricingStrategy {
double calculateFare(double distanceKm, long durationMinutes, VehicleCategory category);
String getStrategyName();
}

class NormalPricing implements PricingStrategy {
private Map<VehicleCategory, Double> ratePerKm = Map.of(
VehicleCategory.AUTO,  12.0,
VehicleCategory.MINI,  15.0,
VehicleCategory.PRIME, 22.0,
VehicleCategory.SUV,   30.0,
VehicleCategory.BIKE,   8.0
);

    public double calculateFare(double distanceKm, long durationMin, VehicleCategory category) {
        double basefare   = 30.0;
        double distFare   = distanceKm * ratePerKm.getOrDefault(category, 15.0);
        double timeFare   = durationMin * 1.5; // Rs 1.5 per minute
        return Math.max(basefare, distFare + timeFare);
    }

    public String getStrategyName() { return "NORMAL"; }
}

class SurgePricing implements PricingStrategy {
private double surgeMultiplier;
private NormalPricing basePricing = new NormalPricing();

    public SurgePricing(double multiplier) { this.surgeMultiplier = multiplier; }

    public double calculateFare(double distanceKm, long durationMin, VehicleCategory category) {
        double baseFare = basePricing.calculateFare(distanceKm, durationMin, category);
        double surgedFare = baseFare * surgeMultiplier;
        System.out.printf("  ⚡ Surge pricing %.1fx applied! Base: $%.2f → Surge: $%.2f%n",
            surgeMultiplier, baseFare, surgedFare);
        return surgedFare;
    }

    public String getStrategyName() { return "SURGE_" + surgeMultiplier + "x"; }
}

// ─── MATCHING STRATEGY ───────────────────────────────────
interface MatchingStrategy {
Driver findBestDriver(List<Driver> drivers, Location pickup, VehicleCategory category);
}

class NearestDriverStrategy implements MatchingStrategy {
public Driver findBestDriver(List<Driver> drivers, Location pickup, VehicleCategory category) {
return drivers.stream()
.filter(Driver::isAvailable)
.filter(d -> d.getVehicle().getCategory() == category)
.min(Comparator.comparingDouble(d -> d.distanceTo(pickup)))
.orElse(null);
}
}

class RatingWeightedStrategy implements MatchingStrategy {
// Score = 60% proximity + 40% rating
public Driver findBestDriver(List<Driver> drivers, Location pickup, VehicleCategory category) {
double maxDistance = 10.0; // Max search radius in km

        return drivers.stream()
            .filter(Driver::isAvailable)
            .filter(d -> d.getVehicle().getCategory() == category)
            .filter(d -> d.distanceTo(pickup) <= maxDistance)
            .min(Comparator.comparingDouble(d -> {
                double proximityScore = d.distanceTo(pickup) / maxDistance;  // 0=close, 1=far
                double ratingScore    = 1.0 - (d.getRating() / 5.0);          // 0=best, 1=worst
                return 0.6 * proximityScore + 0.4 * ratingScore;
            }))
            .orElse(null);
    }
}

// ─── TRIP ────────────────────────────────────────────────
class Trip {
private static int counter = 5000;

    private String tripId;
    private Rider rider;
    private Driver driver;
    private Location pickup;
    private Location destination;
    private TripStatus status;
    private LocalDateTime requestTime;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private double distanceKm;
    private long durationMinutes;
    private double fare;
    private VehicleCategory category;
    private PricingStrategy pricingStrategy;
    private Double riderRating;
    private Double driverRating;

    public Trip(Rider rider, Location pickup, Location destination,
                VehicleCategory category, PricingStrategy pricingStrategy) {
        this.tripId          = "TRIP-" + (++counter);
        this.rider           = rider;
        this.pickup          = pickup;
        this.destination     = destination;
        this.category        = category;
        this.pricingStrategy = pricingStrategy;
        this.status          = TripStatus.REQUESTED;
        this.requestTime     = LocalDateTime.now();
    }

    public void assignDriver(Driver driver) {
        this.driver = driver;
        this.status = TripStatus.DRIVER_ASSIGNED;
        driver.assignTrip();
        System.out.printf("  👤 Driver %s (%.1f⭐, %.2f km away) assigned to %s%n",
            driver.getName(), driver.getRating(),
            driver.distanceTo(pickup), tripId);
    }

    public void driverArrived() {
        status = TripStatus.DRIVER_ARRIVING;
        System.out.printf("  🚗 Driver %s arrived at pickup: %s%n",
            driver.getName(), pickup.getAddress());
    }

    public void startTrip() {
        status    = TripStatus.TRIP_STARTED;
        startTime = LocalDateTime.now();
        System.out.printf("  🏁 Trip %s STARTED | %s → %s%n",
            tripId, pickup.getAddress(), destination.getAddress());
    }

    public double completeTrip() {
        endTime         = LocalDateTime.now();
        status          = TripStatus.COMPLETED;
        distanceKm      = pickup.distanceTo(destination);
        durationMinutes = ChronoUnit.MINUTES.between(startTime, endTime);
        durationMinutes = Math.max(durationMinutes, 10); // Min 10 min for demo

        fare = pricingStrategy.calculateFare(distanceKm, durationMinutes, category);

        driver.completeTrip();
        rider.addTrip(this);

        System.out.printf("  🏆 Trip %s COMPLETED!%n", tripId);
        System.out.printf("     Distance: %.2f km | Duration: %d min | Fare: $%.2f | Pricing: %s%n",
            distanceKm, durationMinutes, fare, pricingStrategy.getStrategyName());
        return fare;
    }

    public void cancelTrip(String reason) {
        status = TripStatus.CANCELLED;
        System.out.printf("  ❌ Trip %s CANCELLED: %s%n", tripId, reason);
        if (driver != null) driver.setAvailable();
    }

    public void rateDriver(double rating) {
        if (status != TripStatus.COMPLETED)
            throw new IllegalStateException("Can only rate after trip completion");
        this.driverRating = rating;
        driver.updateRating(rating);
        System.out.printf("  ⭐ Rider rated Driver %s: %.1f%n", driver.getName(), rating);
    }

    public String getTripId()    { return tripId; }
    public TripStatus getStatus() { return status; }
    public double getFare()      { return fare; }
    public Driver getDriver()    { return driver; }
    public Rider getRider()      { return rider; }
}

// ─── RIDE SHARING SYSTEM ─────────────────────────────────
class RideSharingSystem {
private static RideSharingSystem instance;
private List<Driver>  registeredDrivers;
private List<Rider>   registeredRiders;
private List<Trip>    activeTrips;
private MatchingStrategy matchingStrategy;
private PricingStrategy  pricingStrategy;

    private RideSharingSystem() {
        registeredDrivers = new ArrayList<>();
        registeredRiders  = new ArrayList<>();
        activeTrips       = new ArrayList<>();
        matchingStrategy  = new NearestDriverStrategy();
        pricingStrategy   = new NormalPricing();
    }

    public static RideSharingSystem getInstance() {
        if (instance == null) instance = new RideSharingSystem();
        return instance;
    }

    public void registerDriver(Driver driver) {
        registeredDrivers.add(driver);
        System.out.println("✅ Driver registered: " + driver.getName());
    }

    public void registerRider(Rider rider) {
        registeredRiders.add(rider);
        System.out.println("✅ Rider registered: " + rider.getName());
    }

    // Enable surge pricing during peak hours
    public void enableSurgePricing(double multiplier) {
        pricingStrategy = new SurgePricing(multiplier);
        System.out.printf("⚡ Surge pricing enabled: %.1fx%n", multiplier);
    }

    public void disableSurgePricing() {
        pricingStrategy = new NormalPricing();
        System.out.println("✅ Normal pricing restored");
    }

    // Core: Rider requests a trip
    public Trip requestTrip(String riderId, Location pickup,
                            Location destination, VehicleCategory category) {
        Rider rider = findRider(riderId);
        System.out.printf("%n🔔 Trip requested by %s: %s → %s [%s]%n",
            rider.getName(), pickup.getAddress(), destination.getAddress(), category);

        // Create trip
        Trip trip = new Trip(rider, pickup, destination, category, pricingStrategy);

        // Find best driver using matching strategy
        Driver driver = matchingStrategy.findBestDriver(registeredDrivers, pickup, category);

        if (driver == null) {
            System.out.println("  😔 No drivers available nearby. Try again later.");
            trip.cancelTrip("No drivers available");
            return trip;
        }

        trip.assignDriver(driver);
        activeTrips.add(trip);
        return trip;
    }

    private Rider findRider(String riderId) {
        return registeredRiders.stream()
            .filter(r -> r.getRiderId().equals(riderId))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Rider not found: " + riderId));
    }

    public void displayStats() {
        System.out.println("\n📊 === SYSTEM STATS ===");
        System.out.println("Registered Drivers: " + registeredDrivers.size());
        System.out.println("Registered Riders: "  + registeredRiders.size());
        System.out.println("Active Trips: " + activeTrips.stream()
            .filter(t -> t.getStatus() == TripStatus.TRIP_STARTED).count());
        System.out.println("Available Drivers: " + registeredDrivers.stream()
            .filter(Driver::isAvailable).count());
    }
}

// ─── DEMO ────────────────────────────────────────────────
class RideSharingDemo {
public static void main(String[] args) {
RideSharingSystem system = RideSharingSystem.getInstance();

        // Register drivers
        Location driverLoc1 = new Location(12.9716, 77.5946, "Brigade Road");
        Location driverLoc2 = new Location(12.9352, 77.6245, "Koramangala");

        Vehicle car1 = new Vehicle("V1", "KA-01-AB-1234", "Swift Dzire", VehicleCategory.MINI, 4);
        Vehicle car2 = new Vehicle("V2", "KA-02-CD-5678", "Honda City",  VehicleCategory.PRIME, 4);

        Driver driver1 = new Driver("D1", "Ravi Kumar",  "9876543210", car1, driverLoc1);
        Driver driver2 = new Driver("D2", "Suresh Babu", "9876543211", car2, driverLoc2);

        system.registerDriver(driver1);
        system.registerDriver(driver2);

        // Register rider
        Rider rider = new Rider("R1", "Alice", "9999999999");
        system.registerRider(rider);

        // Request trip
        Location pickup = new Location(12.9698, 77.5986, "MG Road Metro");
        Location dest   = new Location(12.9352, 77.6245, "Koramangala 5th Block");

        Trip trip = system.requestTrip("R1", pickup, dest, VehicleCategory.MINI);

        // Simulate trip progression
        trip.driverArrived();
        trip.startTrip();
        double fare = trip.completeTrip();

        // Rate the driver
        trip.rateDriver(4.8);

        // Now test surge pricing
        system.enableSurgePricing(2.0);
        Rider rider2 = new Rider("R2", "Bob", "8888888888");
        system.registerRider(rider2);
        Trip surgeTrip = system.requestTrip("R2", pickup, dest, VehicleCategory.PRIME);
        if (surgeTrip.getStatus() == TripStatus.DRIVER_ASSIGNED) {
            surgeTrip.startTrip();
            surgeTrip.completeTrip();
        }

        system.displayStats();
    }
}
```

---

# Case Study 4: Snake and Ladder Game 🎲

> *Tests game loop design, turn-based state management, and object modeling.*

---

### The Key Entities
```
Game, Board, Player, Dice
Snake (head → tail: moves player backward)
Ladder (bottom → top: moves player forward)
Cell

Full Code
java// ─── JUMP ENTITY (Snake or Ladder) ───────────────────────
class Jump {
private int start;
private int end;
private String type;  // "SNAKE" or "LADDER"

    public Jump(int start, int end) {
        this.start = start;
        this.end   = end;
        this.type  = (end > start) ? "LADDER" : "SNAKE";
    }

    public int getStart()  { return start; }
    public int getEnd()    { return end;   }
    public String getType(){ return type;  }
}

// ─── CELL ────────────────────────────────────────────────
class Cell {
private int number;
private Jump jump;  // null if no snake/ladder here

    public Cell(int number) { this.number = number; }

    public void setJump(Jump jump) { this.jump = jump; }
    public Jump getJump()          { return jump; }
    public int getNumber()         { return number; }
    public boolean hasJump()       { return jump != null; }
}

// ─── DICE ────────────────────────────────────────────────
class Dice {
private int numberOfDice;
private Random random;

    public Dice(int numberOfDice) {
        this.numberOfDice = numberOfDice;
        this.random       = new Random();
    }

    public int roll() {
        int total = 0;
        for (int i = 0; i < numberOfDice; i++) {
            total += random.nextInt(6) + 1;  // 1 to 6
        }
        return total;
    }
}

// ─── PLAYER ──────────────────────────────────────────────
class Player {
private String playerId;
private String name;
private int currentPosition;
private int totalMoves;
private List<Integer> moveHistory;

    public Player(String id, String name) {
        this.playerId     = id;
        this.name         = name;
        this.currentPosition = 0;  // Start before board
        this.totalMoves   = 0;
        this.moveHistory  = new ArrayList<>();
    }

    public void moveTo(int position) {
        currentPosition = position;
        totalMoves++;
        moveHistory.add(position);
    }

    public int getCurrentPosition() { return currentPosition; }
    public String getName()         { return name; }
    public String getPlayerId()     { return playerId; }
    public int getTotalMoves()      { return totalMoves; }
}

// ─── BOARD ───────────────────────────────────────────────
class Board {
private int size;
private Cell[] cells;

    public Board(int size) {
        this.size  = size;
        this.cells = new Cell[size + 1];  // 1-indexed
        for (int i = 1; i <= size; i++) cells[i] = new Cell(i);
    }

    public void addSnake(int head, int tail) {
        if (head <= tail) throw new IllegalArgumentException("Snake head must be > tail");
        cells[head].setJump(new Jump(head, tail));
        System.out.printf("🐍 Snake added: %d → %d%n", head, tail);
    }

    public void addLadder(int bottom, int top) {
        if (bottom >= top) throw new IllegalArgumentException("Ladder bottom must be < top");
        cells[bottom].setJump(new Jump(bottom, top));
        System.out.printf("🪜 Ladder added: %d → %d%n", bottom, top);
    }

    public Cell getCell(int position) {
        if (position < 1 || position > size) return null;
        return cells[position];
    }

    public int getSize()  { return size; }
    public boolean isWinningPosition(int pos) { return pos == size; }
}

// ─── GAME ────────────────────────────────────────────────
class SnakeLadderGame {
private Board board;
private Dice dice;
private Queue<Player> players;  // Circular queue for turns
private Player winner;
private boolean gameOver;
private int turnNumber;

    public SnakeLadderGame(int boardSize, int numberOfDice) {
        this.board   = new Board(boardSize);
        this.dice    = new Dice(numberOfDice);
        this.players = new LinkedList<>();
        this.gameOver = false;
        this.turnNumber = 0;
        System.out.printf("🎲 Snake & Ladder Game: %d×%d board, %d dice%n",
            (int)Math.sqrt(boardSize), (int)Math.sqrt(boardSize), numberOfDice);
    }

    public void addSnake(int head, int tail)   { board.addSnake(head, tail); }
    public void addLadder(int bottom, int top) { board.addLadder(bottom, top); }
    public void addPlayer(Player player)       { players.add(player); }

    // Play one turn for current player
    public boolean playTurn() {
        if (gameOver || players.isEmpty()) return false;

        Player current = players.poll();  // Take from front
        int roll = dice.roll();
        turnNumber++;

        System.out.printf("%n--- Turn %d: %s (pos=%d) rolled %d ---%n",
            turnNumber, current.getName(), current.getCurrentPosition(), roll);

        int newPosition = current.getCurrentPosition() + roll;

        // Overshoot — can't move beyond board
        if (newPosition > board.getSize()) {
            System.out.printf("  📍 %s stays at %d (overshoot — need exact roll)%n",
                current.getName(), current.getCurrentPosition());
            players.add(current);  // Add back for next turn
            return false;
        }

        // Move to new position
        current.moveTo(newPosition);
        System.out.printf("  ➡️  %s moves to %d%n", current.getName(), newPosition);

        // Check for snake or ladder
        Cell cell = board.getCell(newPosition);
        if (cell != null && cell.hasJump()) {
            Jump jump = cell.getJump();
            System.out.printf("  %s %s at %d! %s → %d%n",
                jump.getType().equals("LADDER") ? "🪜 Ladder" : "🐍 Snake",
                jump.getType().equals("LADDER") ? "up"        : "bite",
                newPosition,
                jump.getType().equals("LADDER") ? "Climb"     : "Slide",
                jump.getEnd());
            current.moveTo(jump.getEnd());
            System.out.printf("  📍 %s now at %d%n", current.getName(), jump.getEnd());
        }

        // Check win
        if (board.isWinningPosition(current.getCurrentPosition())) {
            winner = current;
            gameOver = true;
            System.out.printf("%n🏆🎉 %s WINS in %d moves! Total turns: %d 🎉🏆%n",
                current.getName(), current.getTotalMoves(), turnNumber);
            return true;
        }

        // Add player back for next turn
        players.add(current);
        return false;
    }

    // Play full game
    public Player play() {
        System.out.println("\n🎮 === GAME START ===");
        while (!gameOver) {
            playTurn();
            if (turnNumber > 1000) {  // Safety limit
                System.out.println("Game exceeded 1000 turns — ending.");
                break;
            }
        }
        return winner;
    }

    public boolean isGameOver() { return gameOver; }
    public Player getWinner()   { return winner;   }
}

// ─── DEMO ────────────────────────────────────────────────
class SnakeLadderDemo {
public static void main(String[] args) {
SnakeLadderGame game = new SnakeLadderGame(100, 1);

        // Add snakes (head → tail)
        game.addSnake(98, 78);
        game.addSnake(95, 60);
        game.addSnake(88, 24);
        game.addSnake(62, 18);
        game.addSnake(36,  6);

        // Add ladders (bottom → top)
        game.addLadder( 1, 38);
        game.addLadder( 4, 14);
        game.addLadder( 9, 31);
        game.addLadder(20, 42);
        game.addLadder(71, 91);

        // Add players
        game.addPlayer(new Player("P1", "Alice"));
        game.addPlayer(new Player("P2", "Bob"));
        game.addPlayer(new Player("P3", "Charlie"));

        // Play!
        Player winner = game.play();
        if (winner != null) {
            System.out.printf("Final: %s won in %d moves!%n",
                winner.getName(), winner.getTotalMoves());
        }
    }
}
```

---

# Case Study 5: Online Food Ordering (Zomato/Swiggy) 🍕

> *Tests complex relationships, state machines, and strategy combinations.*

---

### Entities & Patterns
```
User, Restaurant, MenuItem, Cart, Order, Delivery, Payment
Review, Coupon

State   → Order states
Strategy → Payment, Sorting (by rating/distance/price)
Observer → Notify user when order status changes
Factory  → Create order based on restaurant type

Core Design
java// ─── ORDER STATE MACHINE ─────────────────────────────────
// States: PLACED → RESTAURANT_CONFIRMED → PREPARING
//         → READY_FOR_PICKUP → PICKED_UP → DELIVERED → CANCELLED

enum OrderStatus {
PLACED, RESTAURANT_CONFIRMED, PREPARING,
READY_FOR_PICKUP, PICKED_UP, DELIVERED, CANCELLED
}

// ─── MENU ITEM ───────────────────────────────────────────
class MenuItem {
private String itemId;
private String name;
private String description;
private double price;
private boolean isVeg;
private boolean isAvailable;
private String category;  // Starter, Main, Dessert, Drink

    public MenuItem(String id, String name, double price, boolean isVeg, String category) {
        this.itemId      = id;
        this.name        = name;
        this.price       = price;
        this.isVeg       = isVeg;
        this.category    = category;
        this.isAvailable = true;
    }

    public double getPrice()        { return price;       }
    public String getName()         { return name;        }
    public String getItemId()       { return itemId;      }
    public boolean isAvailable()    { return isAvailable; }
    public void setAvailable(boolean b) { isAvailable = b; }

    @Override
    public String toString() {
        return String.format("%s %s - $%.2f", isVeg ? "🟢" : "🔴", name, price);
    }
}

// ─── CART ────────────────────────────────────────────────
class Cart {
private String userId;
private String restaurantId;  // Cart is tied to ONE restaurant
private Map<MenuItem, Integer> items;  // item → quantity
private String appliedCoupon;

    public Cart(String userId, String restaurantId) {
        this.userId       = userId;
        this.restaurantId = restaurantId;
        this.items        = new LinkedHashMap<>();
    }

    public void addItem(MenuItem item, int quantity) {
        if (!item.isAvailable())
            throw new IllegalStateException(item.getName() + " is not available");
        items.merge(item, quantity, Integer::sum);
        System.out.printf("  🛒 Added %dx %s to cart%n", quantity, item.getName());
    }

    public void removeItem(MenuItem item) {
        items.remove(item);
    }

    public void updateQuantity(MenuItem item, int qty) {
        if (qty <= 0) removeItem(item);
        else items.put(item, qty);
    }

    public double getSubtotal() {
        return items.entrySet().stream()
            .mapToDouble(e -> e.getKey().getPrice() * e.getValue())
            .sum();
    }

    public double getTotal(double deliveryFee, double taxRate) {
        double subtotal = getSubtotal();
        double tax      = subtotal * taxRate;
        return subtotal + tax + deliveryFee;
    }

    public void applyCoupon(String couponCode) { this.appliedCoupon = couponCode; }

    public Map<MenuItem, Integer> getItems()   { return Collections.unmodifiableMap(items); }
    public boolean isEmpty()                   { return items.isEmpty(); }
    public String getRestaurantId()            { return restaurantId; }

    public void display() {
        System.out.println("  🛒 === CART ===");
        items.forEach((item, qty) ->
            System.out.printf("  %s × %d = $%.2f%n", item.getName(), qty, item.getPrice() * qty));
        System.out.printf("  Subtotal: $%.2f%n", getSubtotal());
    }
}

// ─── ORDER ───────────────────────────────────────────────
class Order {
private static int counter = 10000;

    private String orderId;
    private String userId;
    private String restaurantId;
    private Map<MenuItem, Integer> items;
    private OrderStatus status;
    private double subtotal;
    private double deliveryFee;
    private double tax;
    private double totalAmount;
    private LocalDateTime placedAt;
    private LocalDateTime estimatedDelivery;
    private String deliveryAddress;
    private List<String> statusHistory;

    // Observer: notify user on status changes
    private List<OrderObserver> observers;

    public Order(String userId, String restaurantId, Cart cart, String deliveryAddress) {
        this.orderId         = "ORD-" + (++counter);
        this.userId          = userId;
        this.restaurantId    = restaurantId;
        this.items           = new HashMap<>(cart.getItems());
        this.status          = OrderStatus.PLACED;
        this.subtotal        = cart.getSubtotal();
        this.deliveryFee     = 30.0;
        this.tax             = subtotal * 0.05;  // 5% tax
        this.totalAmount     = subtotal + deliveryFee + tax;
        this.placedAt        = LocalDateTime.now();
        this.deliveryAddress = deliveryAddress;
        this.statusHistory   = new ArrayList<>();
        this.observers       = new ArrayList<>();
        statusHistory.add("PLACED at " + placedAt);
    }

    public void addObserver(OrderObserver observer) { observers.add(observer); }

    public void updateStatus(OrderStatus newStatus) {
        OrderStatus oldStatus = this.status;
        this.status = newStatus;
        String entry = newStatus + " at " + LocalDateTime.now();
        statusHistory.add(entry);

        System.out.printf("  📦 Order %s: %s → %s%n", orderId, oldStatus, newStatus);

        // Notify all observers
        observers.forEach(obs -> obs.onStatusChange(this, oldStatus, newStatus));
    }

    public String getOrderId()      { return orderId; }
    public OrderStatus getStatus()  { return status; }
    public double getTotalAmount()  { return totalAmount; }

    public void displayReceipt() {
        System.out.println("\n🧾 === ORDER RECEIPT ===");
        System.out.println("Order ID: " + orderId);
        System.out.println("Status:   " + status);
        items.forEach((item, qty) ->
            System.out.printf("  %-20s × %d  $%.2f%n", item.getName(), qty, item.getPrice() * qty));
        System.out.printf("Subtotal:     $%.2f%n", subtotal);
        System.out.printf("Delivery Fee: $%.2f%n", deliveryFee);
        System.out.printf("Tax (5%%):     $%.2f%n", tax);
        System.out.printf("TOTAL:        $%.2f%n", totalAmount);
        System.out.println("Deliver to:   " + deliveryAddress);
    }
}

// ─── OBSERVER ────────────────────────────────────────────
interface OrderObserver {
void onStatusChange(Order order, OrderStatus from, OrderStatus to);
}

class UserNotificationObserver implements OrderObserver {
private String userName;

    public UserNotificationObserver(String userName) { this.userName = userName; }

    public void onStatusChange(Order order, OrderStatus from, OrderStatus to) {
        String message = switch (to) {
            case RESTAURANT_CONFIRMED -> "✅ Restaurant accepted your order!";
            case PREPARING            -> "👨‍🍳 Chef is preparing your food!";
            case READY_FOR_PICKUP     -> "📦 Order ready, waiting for delivery partner!";
            case PICKED_UP            -> "🚴 Your order is on the way!";
            case DELIVERED            -> "🎉 Order delivered! Enjoy your meal!";
            case CANCELLED            -> "❌ Your order was cancelled.";
            default -> "Order status updated to " + to;
        };
        System.out.printf("  🔔 Notification to %s: %s%n", userName, message);
    }
}

// ─── RESTAURANT SEARCH STRATEGY ──────────────────────────
interface RestaurantSortStrategy {
List<Restaurant> sort(List<Restaurant> restaurants, Location userLocation);
}

class SortByRating implements RestaurantSortStrategy {
public List<Restaurant> sort(List<Restaurant> restaurants, Location loc) {
return restaurants.stream()
.sorted(Comparator.comparingDouble(Restaurant::getRating).reversed())
.collect(Collectors.toList());
}
}

class SortByDistance implements RestaurantSortStrategy {
public List<Restaurant> sort(List<Restaurant> restaurants, Location userLocation) {
return restaurants.stream()
.sorted(Comparator.comparingDouble(r -> r.getLocation().distanceTo(userLocation)))
.collect(Collectors.toList());
}
}

class SortByDeliveryTime implements RestaurantSortStrategy {
public List<Restaurant> sort(List<Restaurant> restaurants, Location loc) {
return restaurants.stream()
.sorted(Comparator.comparingInt(Restaurant::getEstimatedDeliveryMinutes))
.collect(Collectors.toList());
}
}

// ─── RESTAURANT ──────────────────────────────────────────
class Restaurant {
private String restaurantId;
private String name;
private String cuisine;
private double rating;
private Location location;
private int estimatedDeliveryMinutes;
private boolean isOpen;
private List<MenuItem> menu;

    public Restaurant(String id, String name, String cuisine,
                      double rating, Location location, int deliveryMin) {
        this.restaurantId = id;
        this.name         = name;
        this.cuisine      = cuisine;
        this.rating       = rating;
        this.location     = location;
        this.estimatedDeliveryMinutes = deliveryMin;
        this.isOpen       = true;
        this.menu         = new ArrayList<>();
    }

    public void addMenuItem(MenuItem item) { menu.add(item); }

    public List<MenuItem> getMenu()      { return Collections.unmodifiableList(menu); }
    public String getRestaurantId()      { return restaurantId; }
    public String getName()              { return name; }
    public double getRating()            { return rating; }
    public Location getLocation()        { return location; }
    public int getEstimatedDeliveryMinutes() { return estimatedDeliveryMinutes; }
    public boolean isOpen()              { return isOpen; }

    @Override
    public String toString() {
        return String.format("%s | %s | ⭐%.1f | 🕐%d min | %s",
            name, cuisine, rating, estimatedDeliveryMinutes,
            isOpen ? "Open" : "Closed");
    }
}
```

---

## The Q&A Round 🎤

---

**Q1. In the Parking Lot, how do you handle concurrent requests for the same spot?**

> *"Great question — this is a thread-safety concern. The parkVehicle() method in ParkingLot must be synchronized, or we use a concurrent data structure. When two cars arrive simultaneously and only one spot remains, without synchronization both could see the spot as 'available' and try to park there. I'd add synchronized to the parkVehicle method, or better — use optimistic locking with a CAS (compare-and-swap) operation on the spot's status. In a distributed system, I'd use Redis distributed locks with TTL to prevent the same spot being double-booked across multiple servers."*

---

**Q2. How would you extend the Parking Lot to support EV charging spots?**

> *"OCP-friendly extension — no existing code changes needed. I add EVSpot extends ParkingSpot with a chargingRate field. Add ELECTRIC to SpotType enum. Add ELECTRIC to VehicleType for EVCar. Override the park() method in EVSpot to also start a charging session. The ParkingFloor.getAvailableSpot() already works generically — it'll find EVSpots when requested. The charging session creates a ChargingTicket in addition to the ParkingTicket. Payment now includes parking + charging. I've added a new feature without touching a single existing class."*

---

**Q3. In the Elevator System, how does the SCAN algorithm prevent starvation?**

> *"Starvation means a floor never gets served. Pure SCAN can starve floors at the extremes if there's always demand in the middle. LOOK algorithm helps — it reverses direction before reaching the physical top/bottom if no more requests exist in that direction. For anti-starvation: every pending request gets a timestamp. If a request has been waiting more than N seconds, it gets elevated priority — the nearest idle elevator is forced to serve it regardless of direction. This is similar to how OS schedulers handle process starvation with aging."*

---

**Q4. How would you add real-time location tracking to the Ride Sharing system?**

> *"Driver sends location updates every 5 seconds via WebSocket — persistent bidirectional connection, much more efficient than polling. On the server side, I store driver locations in Redis geospatial data structure (GEOADD, GEORADIUS commands) — optimized for proximity queries. The matching algorithm queries Redis for drivers within N km of pickup in O(log N). For the rider's app, server pushes updates via WebSocket when driver location changes beyond a threshold (100m). I wouldn't push every single update — that's too much traffic. Only meaningful position changes trigger push."*

---

**Q5. How would you design the rating system to prevent fake ratings?**

> *"Multiple safeguards. First, you can only rate after a COMPLETED trip — system verifies the trip exists and belongs to this user. One rating per trip — composite primary key (tripId, raterId) in the DB. Rate limiting — you can't rate more than once per trip (backend enforced, not just UI). For fake positive ratings: detect collusion patterns — if Driver A always gets 5 stars from the same 10 riders repeatedly, flag for review. Driver's rating uses a rolling window of last 500 trips — old ratings expire. Anomaly detection flags sudden rating spikes. Verified reviews require trip completion proof."*

---

**Q6. In the Snake and Ladder game, how do you prevent infinite loops?**

> *"Two scenarios. First: a snake's tail landing on a ladder's bottom that goes back up to the snake's head — a cycle. I validate the board configuration at setup time using DFS/BFS to detect cycles. If adding a snake/ladder would create a cycle, throw a configuration exception. Second: gameplay taking too long — I add a MAX_TURNS limit (say 1000 turns). If exceeded, declare no winner or the player currently in the lead wins. In the code, the while(!gameOver) loop checks turnNumber > MAX_TURNS."*

---

**Q7. How would you make the Food Ordering system handle restaurant going offline mid-order?**

> *"Observer pattern helps here — Restaurant is also an observable. When restaurant goes offline (status change), the OrderSystem observer triggers: find orders in PLACED or RESTAURANT_CONFIRMED state for this restaurant, auto-cancel them, initiate refunds, notify users. For PREPARING orders — those can't be cancelled easily. I'd notify users about potential delay and give them the option to cancel with full refund. For the restaurant going offline detection: restaurants send heartbeat every 30 seconds. If 3 consecutive heartbeats missed, mark as offline. This is the same pattern used in distributed systems for health checks."*

---

**Q8. How would you design the system to support split payments (2 people splitting a ride)?**

> *"Trip gets a SplitPaymentRequest with a list of payerIds and split amounts (equal or custom). Each payer gets a PaymentLink. Trip only proceeds when ALL payments confirmed. Timeout: if one payer doesn't pay in 5 minutes, initiator is charged full amount. State machine for payment: AWAITING_SPLIT_PAYMENT → PARTIAL_PAID → FULLY_PAID. Store each individual PaymentRecord linked to the trip. Refunds must also be split proportionally. The PaymentStrategy pattern extends naturally — SplitPaymentStrategy wraps multiple individual PaymentStrategy objects."*

---

## Quick Design Summary Table

| Problem | Key Pattern | Hardest Part | Interviewer Focus |
|---|---|---|---|
| **Parking Lot** | Singleton + Strategy | Concurrency on spot booking | Spot allocation algorithm |
| **Elevator** | State + Strategy | Dispatch algorithm | Starvation prevention |
| **Ride Sharing** | State + Observer + Strategy | Real-time matching | Surge pricing + driver states |
| **Snake & Ladder** | Game loop + Queue | Cycle detection | Board validation |
| **Food Ordering** | Observer + State + Strategy | Order status transitions | Failure handling |

---

## How to Approach ANY New LLD Problem 🧭
```
HEAR the problem → Immediately ask:
1. "How many users / scale?"  → Singleton? Thread safety?
2. "Can X change at runtime?" → Strategy pattern
3. "Do multiple things react to one event?" → Observer pattern
4. "Does behavior change based on status?" → State pattern
5. "Complex object creation?" → Factory / Builder
6. "Simplify a messy system?" → Facade

DRAW entities on paper:
→ Nouns = Classes
→ Verbs = Methods
→ "Has a" = Composition
→ "Is a" = Inheritance (use sparingly)

NARRATE out loud:
"I'm choosing Strategy here because..."
"This should be a Singleton because..."
"I notice a state machine emerging here..."

Type "Section 5" for Advanced Concepts — Coupling, Cohesion, DRY/KISS/YAGNI,