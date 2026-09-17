package com.example.demo.domain;

import com.example.demo.enums.ReservationStatus;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "reservations")
public class Reservation {

  @Id
  private UUID id;

  @Column(name = "event_id", nullable = false)
  private UUID eventId;

  @Column(name = "customer_email", nullable = false)
  private String customerEmail;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private ReservationStatus status;

  @Column(name = "hold_expires_at", nullable = false)
  private Instant holdExpiresAt;

  @CreationTimestamp
  @Column(name = "created_at", updatable = false)
  private Instant createdAt;

  @Column(name = "confirmed_at")
  private Instant confirmedAt;

  @Version
  private int version;

  @OneToMany(
    mappedBy = "reservation",
    cascade = CascadeType.ALL,
    orphanRemoval = true,
    fetch = FetchType.EAGER
  )
  private List<ReservationSeat> seats = new ArrayList<>();

  protected Reservation() {}

  public Reservation(
    UUID id,
    UUID eventId,
    String customerEmail,
    ReservationStatus status,
    Instant holdExpiresAt
  ) {
    this.id = id;
    this.eventId = eventId;
    this.customerEmail = customerEmail;
    this.status = status;
    this.holdExpiresAt = holdExpiresAt;
  }

  @PrePersist
  void prePersist() {
    if (id == null) id = UUID.randomUUID();
  }

  public void addSeat(ReservationSeat seat) {
    seats.add(seat);
    seat.setReservation(this);
  }

  public void confirm() {
    this.status = ReservationStatus.CONFIRMED;
    this.confirmedAt = Instant.now();
  }

  public void cancel() {
    this.status = ReservationStatus.CANCELLED;
  }

  public void expire() {
    this.status = ReservationStatus.EXPIRED;
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

  public Instant getHoldExpiresAt() {
    return holdExpiresAt;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getConfirmedAt() {
    return confirmedAt;
  }

  public List<ReservationSeat> getSeats() {
    return seats;
  }
}
