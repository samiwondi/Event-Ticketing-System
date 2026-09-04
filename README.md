# Ticketing System – Phase 1

A pure Java CLI ticketing system for venues and events.  
This is the **foundational phase** of a multi‑phase project that will later evolve into a Spring Boot REST API with PostgreSQL, security, observability, and performance tuning.

## Features (Phase 1)

- **Domain‑Driven Design** – clear entities: `Venue`, `Event`, `Seat`, `Reservation`, `ReservationSeat`.
- **In‑memory repositories** – with concurrent collections (`ConcurrentHashMap`).
- **JSON persistence** – save/load the entire system state using Jackson.
- **Command‑Line Interface** – create venues/events/seats, hold/confirm/cancel reservations, and run basic reports.
- **Unit tests** – JUnit 5 + AssertJ, covering domain logic, services, and JSON round‑trip.
- **Error handling** – custom exceptions, friendly error messages.
- **Java Time API** – proper handling of time zones and durations.

### Core Commands
| Command | Description |
|---------|-------------|
| `create venue <name> <address> <timezone>` | Add a new venue |
| `create event <venueId> <title> <start> <end>` | Schedule an event |
| `create seat <venueId> <section> <row> <number>` | Add a seat to a venue |
| `list venues`, `list events`, `list seats <venueId>` | List data |
| `hold <eventId> <email> <seatId1,seatId2,...>` | Place a hold on seats |
| `confirm <reservationId>` | Confirm a hold |
| `cancel <reservationId>` | Cancel a reservation |
| `reports seats-per-event`, `reports reservations-per-event` | Generate reports |
| `save`, `load` | Persist / restore state (to `ticketing-data.json`) |
| `exit` | Quit the application |

## Technology Stack

- **Java 17** (compatible with JDK 17+)
- **Maven** – build and dependency management
- **Jackson** – JSON serialisation/deserialisation
- **JUnit 5** – unit testing
- **AssertJ** – fluent assertions

## How to Build and Run

### Prerequisites
- JDK 17 or later (tested with JDK 20)
- Maven (or use the included Maven wrapper)

### Build
```bash
mvn clean package