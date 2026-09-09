package com.example.demo.domain;

import com.example.demo.enums.ReservationStatus;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public class Reservation {

  private final UUID id;
  private final UUID eventId;
  private final String customerEmail;
  private ReservationStatus status;
  private final Instant createdAt;
  private Instant confirmedAt;
  private final Instant holdExpiresAt;
  private final List<ReservationSeat> seats;

  public Reservation(
    UUID id,
    UUID eventId,
    String customerEmail,
    ReservationStatus status,
    Instant createdAt,
    Instant confirmedAt,
    Instant holdExpiresAt,
    List<ReservationSeat> seats
  ) {
    this.id = Objects.requireNonNull(id);
    this.eventId = Objects.requireNonNull(eventId);
    this.customerEmail = Objects.requireNonNull(customerEmail);
    this.status = (status != null) ? status : ReservationStatus.HOLD;
    this.createdAt = (createdAt != null) ? createdAt : Instant.now();
    this.confirmedAt = confirmedAt;
    this.holdExpiresAt = Objects.requireNonNull(holdExpiresAt);
    this.seats = (seats != null) ? new ArrayList<>(seats) : new ArrayList<>();
  }

  public UUID getId() {
    return id;
  }

  public UUID getEventId() {
    return eventId;
  }

  public String getCustomerEmail() {
    return customerEmail;
  }

  public ReservationStatus getStatus() {
    return status;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Optional<Instant> getConfirmedAt() {
    return Optional.ofNullable(confirmedAt);
  }

  public Instant getHoldExpiresAt() {
    return holdExpiresAt;
  }

  public List<ReservationSeat> getSeats() {
    return Collections.unmodifiableList(seats);
  }

  public void confirm() {
    this.status = this.status.transitionTo(ReservationStatus.CONFIRMED);
    this.confirmedAt = Instant.now();
  }

  public void cancel() {
    this.status = this.status.transitionTo(ReservationStatus.CANCELLED);
  }

  public void expire() {
    if (this.status == ReservationStatus.HOLD) {
      this.status = this.status.transitionTo(ReservationStatus.EXPIRED);
    }
  }

  public boolean isExpired() {
    if (
      status == ReservationStatus.EXPIRED ||
      (status == ReservationStatus.HOLD && Instant.now().isAfter(holdExpiresAt))
    ) return true;
    return false;
  }

  public boolean tryExpireIfHoldExpired() {
    if (
      status == ReservationStatus.HOLD && Instant.now().isAfter(holdExpiresAt)
    ) {
      expire();
      return true;
    }
    return false;
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
    return (
      "Reservation{id=" +
      id +
      ", eventId=" +
      eventId +
      ", status=" +
      status +
      "}"
    );
  }
}
