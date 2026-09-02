package com.example.demo.domain;

import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.*;

public class Reservation {
    private final UUID id;
    private final UUID eventId;
    private final String customerEmail;
    private ReservationStatus status;
    private final Instant createdAt;
    private Instant confirmedAt;
    private final Instant holdExpiresAt;
    private final List<ReservationSeat> seats;

    public Reservation(UUID id, UUID eventId, String customerEmail, ReservationStatus status,
        Instant createdAt, Instant confirmedAt, Instant holdExpiresAt,
        List<ReservationSeat> seats) {
        this.id = Objects.requireNonNull(id);
        this.eventId = Objects.requireNonNull(eventId);
        this.customerEmail = Objects.requireNonNull(customerEmail);
        this.status = status != null ? status : ReservationStatus.HOLD;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
        this.confirmedAt = confirmedAt;
        this.holdExpiresAt = Objects.requireNonNull(holdExpiresAt);
        this.seats = seats != null ? new ArrayList<>(seats) : new ArrayList<>();
    }

    // Getters
    public UUID getId() { return id; }
    public UUID getEventId() { return eventId; }
    public String getCustomerEmail() { return customerEmail; }
    public ReservationStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public Optional<Instant> getConfirmedAt() { return Optional.ofNullable(confirmedAt); }
    public Instant getHoldExpiresAt() { return holdExpiresAt; }
    public List<ReservationSeat> getSeats() { return Collections.unmodifiableList(seats); }

    // Business logic methods
    public void confirm() {
        if (status == ReservationStatus.CONFIRMED) {
            throw new IllegalStateException("Reservation already confirmed");
        }
        if (status == ReservationStatus.CANCELLED) {
            throw new IllegalStateException("Cannot confirm a cancelled reservation");
        }
        if (Instant.now().isAfter(holdExpiresAt)) {
            throw new IllegalStateException("Hold expired");
        }
        this.status = ReservationStatus.CONFIRMED;
        this.confirmedAt = Instant.now();
    }

    public void cancel() {
        if (status == ReservationStatus.CANCELLED) {
            throw new IllegalStateException("Already cancelled");
        }
        if (status == ReservationStatus.CONFIRMED) {
            // In real system, might have a cancellation window; we allow it per spec
            // but we could add logic later.
        }
        this.status = ReservationStatus.CANCELLED;
    }

    public boolean isExpired() {
        return status == ReservationStatus.HOLD && Instant.now().isAfter(holdExpiresAt);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Reservation that)) return false;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Reservation{id=" + id + ", eventId=" + eventId + ", status=" + status + "}";
    }
}