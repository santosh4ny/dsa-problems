Concurrency & Threading
```
Java Memory Model — visibility bugs, happens-before, instruction reordering (the root of every concurrency bug)
Thread lifecycle (all 6 states), 4 ways to create threads, critical methods (sleep/wait/interrupt/join)
Synchronization — synchronized, volatile, AtomicInteger/LongAdder with CAS internals
Deadlock (Coffman conditions + 2 fixes), Livelock, Starvation
Semaphore, CountDownLatch, CyclicBarrier, Phaser — when to use each
Thread pools — ThreadPoolExecutor internals, sizing formula, rejection policies
CompletableFuture — thenApply vs thenCompose vs thenCombine, allOf, anyOf, error handling
```
Java Core
```
Java 8+ Streams (full API — groupingBy, partitioningBy, reduce, parallel), Lambdas, Optional
Collections deep dive — HashMap internal working (how put/get/resize works, why keys must be immutable), LRU cache with LinkedHashMap, PriorityQueue Top-K trick
JVM internals — Heap/Stack/Metaspace, ClassLoader delegation, JIT tiered compilation
Garbage Collection — G1 GC, ZGC, memory leak patterns (ThreadLocal, static collections, inner classes)
Exception handling — hierarchy, custom exceptions, InterruptedException rules
```