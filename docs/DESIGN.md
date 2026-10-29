# Design Notes

## 1. Problem interpretation

The core use case is:

> Reserve one car of a requested type at a requested local date/time for a positive number of days, provided that at least one physical car of that type has no overlapping reservation.

The solution also exposes availability, lookup, and cancellation because these make the reservation rule observable and testable without introducing unrelated features.

## 2. Object-oriented model

### `CarType`

An enum defines the complete supported category set: `SEDAN`, `SUV`, and `VAN`. This gives compile-time safety and prevents arbitrary category strings. Subclasses are unnecessary because the categories have no different behavior in the assessment.

### `Car`

An immutable entity value containing a stable car ID and type. Inventory is made of physical cars (`SEDAN-1`, `SEDAN-2`, etc.), which prevents the subtle errors that can arise from tracking only aggregate counts.

### `RentalPeriod`

An immutable value object that owns duration validation, end calculation, and overlap semantics. Keeping overlap logic here avoids duplicating date comparisons in the service and gives the rule a small, directly testable unit.

For periods A and B:

```text
overlaps = A.start < B.endExclusive AND B.start < A.endExclusive
```

Using exclusive ends makes adjacent reservations valid and avoids ambiguous boundary conditions.

### `Reservation`

An immutable confirmation containing a UUID, the selected car, and the rental period. A reference to the car ensures that availability is evaluated per physical vehicle.

### `CarRentalService`

The application-facing contract. It isolates callers from storage choices and defines reservation, cancellation, lookup, availability, and snapshot operations.

### `InMemoryCarRentalService`

The assessment implementation. It encapsulates the fleet and current reservations. Callers cannot mutate internal collections directly.

## 3. Reservation algorithm

```text
reserve(requested type, requested period):
    validate arguments
    enter atomic critical section
    for each car of requested type, in stable ID order:
        if the car has no reservation overlapping the requested period:
            create a reservation with a new UUID
            store it by reservation ID
            return it
    throw NoCarAvailableException
```

Selecting and saving occur in the same critical section. Checking availability in one method and saving later in another would create a check-then-act race.

## 4. Thread safety

All methods that access mutable reservation state are `synchronized` on the service instance. This provides:

- Atomic selection and insertion for reservation requests
- Atomic cancellation
- Consistent availability and lookup snapshots
- No overselling when multiple threads compete for the last car

The concurrency test starts twelve requests together against one sedan and verifies exactly one reservation and eleven capacity failures.

A single lock is intentionally simple and appropriate for this small, in-memory exercise. Per-car-type locks could improve contention but would complicate operations that report global reservation state. A production, multi-instance deployment cannot rely on a JVM lock and must enforce consistency in a shared datastore.

## 5. Complexity

Let **C** be the number of cars of the requested type and **R** the number of current reservations.

- Reservation: worst-case **O(C × R)** with the straightforward per-car scan
- Availability count: **O(C × R)**
- Cancellation and lookup: average **O(1)** by UUID
- Reservation snapshot: **O(R log R)** because results are sorted
- Fleet storage: **O(total cars + R)**

This is clear and sufficient for the assessment. An obvious optimization is to index active reservations by car ID, reducing each availability check to the reservations for that car.

## 6. Validation and failure behavior

- Null required values are rejected with `NullPointerException`.
- Zero or negative rental days and negative inventory counts are rejected with `IllegalArgumentException`.
- A missing inventory count is rejected during construction rather than silently interpreted.
- Capacity exhaustion uses `NoCarAvailableException`, distinct from invalid input.
- Unknown cancellation uses `ReservationNotFoundException`.
- Failed requests do not change reservation state.

The implementation does not reject dates in the past because that policy was not specified. If required, a future-date policy should be added at the application boundary using an injected `Clock`, which keeps tests deterministic.

## 7. Time model

`LocalDateTime` matches a rental location's local pickup date and time. Adding days preserves the requested local time. A production system with multiple locations should also store the location time zone and define explicitly how daylight-saving transitions are handled.

## 8. Persistence evolution

A database implementation should preserve the same service and domain semantics. One possible design:

1. Store cars and reservations in relational tables.
2. Start a transaction and lock the candidate cars or existing overlapping reservations.
3. Select an available car and insert the reservation in that transaction.
4. Use a database exclusion constraint where supported, or another invariant that prevents overlapping reservations for the same car.
5. Translate constraint violations to `NoCarAvailableException` and add retry behavior where appropriate.

The Java lock alone would not be sufficient once more than one application instance can write reservations.

## 9. Scope decisions

The assessment asks for object-oriented reservation logic and unit tests, not a complete rental product. The project therefore avoids unrelated frameworks and infrastructure. The runnable demo, documentation, CI, validation, and concurrency test support review without obscuring the core model.
