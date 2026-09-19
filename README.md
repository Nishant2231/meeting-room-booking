# Meeting Room Booking System

A backend service for booking meeting rooms with correctness guaranteed under concurrent access — built to explore relational data modeling and pessimistic locking as a solution to race conditions.

## The Core Problem

Two people try to book the same room for an overlapping time slot at nearly the same instant. Without protection, both requests can pass their "is this slot free?" check before either one commits, resulting in a double-booked room.

This project solves it with **pessimistic locking**: `SELECT ... FOR UPDATE` on the room row before checking availability, so a second concurrent request is forced to wait until the first transaction fully commits or rolls back. Verified with an automated test that fires two booking requests from separate threads at the same instant — one succeeds, one is correctly rejected as a conflict, every run.

## Tech Stack

- Java 21, Spring Boot
- PostgreSQL + Spring Data JPA / Hibernate
- Docker for local Postgres
- JUnit 5 — including a genuine multithreaded concurrency test, not just sequential request tests

## Design Notes

**Why lock the Room, not the Booking.** The Room is the contended resource — locking it means only one transaction can be mid-way through "check availability, then insert" for that room at a time. There's nothing to lock on the Booking side, since the conflicting booking doesn't exist yet at the moment the race happens.

**Why pessimistic over optimistic locking.** Bookings are relatively rare, low-contention events compared to something like flash-sale inventory. The blocking cost of pessimistic locking is acceptable here, and it guarantees correctness without needing retry logic — a reasonable trade-off to be able to defend, not the only correct answer.

**Custom exceptions, mapped explicitly.** `RoomNotFoundException` → 404, `BookingConflictException` → 409, validation errors → 400. Each extends `RuntimeException` specifically so Spring's default transaction rollback applies without extra configuration — a checked exception would silently commit a failed transaction, which would be a real (and non-obvious) bug.

## API

| Method | Endpoint | Description |
|--------|----------|--------------|
| POST | `/api/v1/rooms` | Create a room |
| GET | `/api/v1/rooms/available?start=&end=` | List rooms free in a time window |
| POST | `/api/v1/bookings` | Create a booking |
| DELETE | `/api/v1/bookings/{id}` | Cancel a booking |
| GET | `/api/v1/bookings?userId=` | List a user's bookings |

**Example**

```bash
curl -X POST http://localhost:8080/api/v1/bookings \
  -H "Content-Type: application/json" \
  -d '{"roomId":1,"userId":1,"startTime":"2026-09-20T14:00:00","endTime":"2026-09-20T15:00:00"}'
```

## Running Locally

```bash
docker-compose up -d
./gradlew bootRun
```

## Testing

```bash
./gradlew test
```

Includes `BookingConcurrencyTest` — spins up two threads that attempt to book the same room and time slot simultaneously, using `CountDownLatch` to synchronize their exact start, and asserts exactly one succeeds while the other is rejected with a conflict. This is the test that actually proves the locking works, rather than just asserting it in a README.

## Known Limitations

- **No authentication yet** — user creation is currently stubbed (inserted directly, no `/users` endpoint). JWT auth is a planned next step.
- **Tests run against a live local Postgres instance** rather than an isolated test database (e.g. via Testcontainers) — acceptable for a portfolio project, but not how a production test suite should be structured.
- **`ddl-auto=update`** is used for convenience during development; a real system would use migration tooling (Flyway/Liquibase) instead of relying on Hibernate to manage schema changes.

## Possible Next Steps

- JWT-based authentication, tying bookings to real logged-in users
- Testcontainers for isolated, reproducible test runs
- Dockerize the application itself (not just the database) for one-command startup
- Recurring bookings