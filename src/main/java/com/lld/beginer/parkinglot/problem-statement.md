
# Design a parking lot system that can:
    - Support multiple floors
    - Support different vehicle types (Bike, Car, Truck)
    - Assign parking spot
    - Vacate parking spot
    - Calculate parking fees
    - Be extensible for future vahicle types.


# Clarifying questions:  i will ask is:
    - is it multi-floor? YES
    - Different vehicle type? YES
    - Payment mode required? NO
    - Concurrency needed? Not now

# Identify Core Entities:
    How we are clarifying: we are finding nouns into the problem statements.
    - ParkingLot
    - Floor
    - ParkingSpot
    - Vehicle
    - Ticket
    - FeeCalculator


# Design Principles: Here what principles should i apply?
    - Opne/closed Principle:
        - Tomorrow new vehicle type may come(ElectricCar)
        - I shoud not modify old code.
    - I use Abstraction and Strategy pattern (for fee calculation)

# Few followups questions might ask 
    1. How will you make it thread-safe?(why needed, if 100 cars try to park at same time. and 2 thread 
    pick the same spot)
        Solution: Option - 1:
        - Synchronization on method level in Ticket class
            public syncronized Ticket parkVehicle(Vehicle vehicle)
        Why? 
            - It ensures only one thread enters method at a time.
            - but still problem is there 
            1. Not scalable 
            2. Entire method is locked.
        Option - 2:
        Add a lock at ParkingSpot level:
        private final Reentrantlock lock = new ReentrantLock();
        public boolean assignVehicle(Vehicle vehicle){
            lock.lock();
            try{
                if(isFree && vehicle.getType() == supportedType){
                    isFree = false;
                    this.vehicle = vehicle;
                    return true;
                }
                return false;
            }finally{
                lock.unclock();
why option - 2 is better?
    Because Only that spot is locked and
    other spots can be used concurrently.
    
    Option -3 :
    Use AtomicBoolean for isFree.
    why?
    Lock free design, and better performance.

How will you handle 1 million parking requests?

    Current problem is in my solution:
    - We loop through all floors and spots it will take O(n).
    if 100 floors * 1000 spots = 100000 scans per request
    so this solution is not scalable.
solution: I will use Map 
    Map<VehicleType, Queue<ParkingSpot>> availableSpot

    why: Direct loockup
        O(1) time i can assess the spot
    when spot becoms free add back to queue.
    this approach is going to removes scanning.

How will you persist tickets?
why needed?
if system crashes: all active parking data lost.
solution: use TicketRepository{
    void save(Ticket ticket);
    Ticket findById(String id);
}

How will you support dynamic pricing?
like: weekend pricing, Peak hour pricing, Surge pricing

i will do the update of strategy pattern:
Instead of FeeStrategy i will use 
PricingPolicy{
    double calculate(Ticket ticket)
}

and create :
HourlyPricing
WeekendPricing
SurgePricing
CompositePricing(which will use Decorator Pattern)

why above all ? Because Pricing rules can combine.

How will you design electric charging spot?
there are two ways 
1. class ElectricSpot extends ParkingSpot{
    private boolean chargerAvailable;
}
2. or its better that add attribute:
   private SpotFeature feature;

why? it will avoid deep inheritance. and use composition

How to make this microservice ready?

Split into services:
    - Parking Service
    - Ticket Service
    - Billing Service
    - Payment Service
Why?
    - Single Responsibility at system level
    - Independent scaling
use
    - REST APIs
    - kafka for event
    - Redis for caching
What if spot allocation must be nearest?
    - we need distance awareness.
    i am going to change PriorityQueue<ParkingSpot>
    and sort by distance from entry gate
Why? Atutomaitically gives nearest spot in O(long n)

How to reduce search time?
    I have already solved via:
    - Map<VehicleType,Queue>
    - PriorityQueue
Complexity: From O(n) To O(1) or O(long n)

What data structure improves performance?
Quick lookup by type - HashMap
Nearest spot - PriorityQueue
Fast concurrent access - ConcurrentHashMap
Fixed size capacity - Array

