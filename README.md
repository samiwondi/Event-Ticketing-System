# Ticketing System – Phase 2

**Concurrency, Caching & Hold Expiry**

This is the second phase of the ticketing system project. Building on the Core Java CLI from Phase 1, we've added:

- **Automatic hold expiry** (background sweeper)
- **Per-event locking** to prevent double-booking
- **LRU cache** for seat availability
- **System Status** menu to monitor runtime behaviour
- **Concurrency tests** to verify thread-safety

---

## ✨ Features

### Core Ticketing (from Phase 1)
- Domain model: `Venue`, `Event`, `Seat`, `Reservation`, `ReservationSeat`, `Money`
- In-memory repositories with `ConcurrentHashMap`
- Menu‑driven CLI (no more flat commands)
- JSON persistence (manual save/load via menu)
- Reports: seats per event, reservations per event, detailed seat report

### Phase 2 – New Features

#### 🔁 Hold Expiry Sweeper
- Runs in the background every **2 seconds**
- Automatically cancels `HOLD` reservations that have passed their expiry time
- Logs cancellation events to the console

#### 🔒 Per-Event Locking
- Each event has its own `ReentrantLock`
- Prevents double-booking in concurrent scenarios
- Locks are acquired automatically when holding, confirming, or cancelling reservations

#### 📦 LRU Cache (Seat Availability)
- Caches seat lists per event to reduce repository calls
- Max size: **100 entries** (configurable)
- Uses `LinkedHashMap` with `removeEldestEntry`
- Cache is invalidated after any reservation change

#### 📊 System Status Menu
- New option in the main menu: `7. System Status`
- Shows:
  - Whether the sweeper is running
  - Number of active HOLD reservations
  - Current cache size
  - Number of event locks currently held

#### 🧪 Concurrency Tests
- `BookingServiceConcurrencyTest`
- Uses `CountDownLatch` to simulate simultaneous requests
- Verifies exactly one success among 10 concurrent holds on the same seat

---

## 🛠️ Technology Stack

| Component | Technology |
|-----------|------------|
| Language | Java 17+ (tested with JDK 20) |
| Build Tool | Maven |
| JSON | Jackson 2.15.2 |
| Testing | JUnit 5 + AssertJ |
| Concurrency | `ReentrantLock`, `ConcurrentHashMap`, `ScheduledExecutorService` |
| Cache | `LinkedHashMap` with LRU eviction |

---

### Prerequisites
- JDK 17 or later (tested with JDK 20)
- Maven (or use the Maven wrapper)

### Build
```bash
mvn clean package
