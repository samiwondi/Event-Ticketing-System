package com.example.demo.domain;

import com.example.demo.enums.DiscountType;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(
  name = "reservation_seats",
  uniqueConstraints = @UniqueConstraint(
    name = "uq_event_seat",
    columnNames = { "event_id", "seat_id" }
  )
)
public class ReservationSeat {

  @Id
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "reservation_id", nullable = false)
  private Reservation reservation;

  @Column(name = "seat_id", nullable = false)
  private UUID seatId;

  @Column(name = "event_id", nullable = false)
  private UUID eventId;

  @Embedded
  private Money price;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 30)
  private DiscountType discount = DiscountType.NONE;

  protected ReservationSeat() {}

  public ReservationSeat(
    UUID seatId,
    UUID eventId,
    Money price,
    DiscountType discount
  ) {
    this.seatId = seatId;
    this.eventId = eventId;
    this.price = price;
    this.discount = discount;
  }

  @PrePersist
  void prePersist() {
    if (id == null) id = UUID.randomUUID();
  }

  void setReservation(Reservation reservation) {
    this.reservation = reservation;
  }

  public UUID getId() {
    return id;
  }

  public UUID getSeatId() {
    return seatId;
  }

  public UUID getEventId() {
    return eventId;
  }

  public Money getPrice() {
    return price;
  }

  public DiscountType getDiscount() {
    return discount;
  }
}
