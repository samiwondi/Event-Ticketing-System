package com.example.demo.domain;

import com.example.demo.enums.Currency;
import com.example.demo.enums.EventStatus;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(
  name = "events",
  uniqueConstraints = @UniqueConstraint(
    name = "uq_event_venue_title_start",
    columnNames = { "venue_id", "title", "start_at" }
  )
)
public class Event {

  @Id
  private UUID id;

  @Column(name = "venue_id", nullable = false)
  private UUID venueId;

  @Column(nullable = false)
  private String title;

  @Column(name = "start_at", nullable = false)
  private Instant startAt;

  @Column(name = "end_at", nullable = false)
  private Instant endAt;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 30)
  private EventStatus status;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 10)
  private Currency currency;

  @Embedded
  private PricingRules pricingRules;

  @Version
  private int version;

  @CreationTimestamp
  @Column(name = "created_at", updatable = false)
  private Instant createdAt;

  protected Event() {}

  public Event(
    UUID id,
    UUID venueId,
    String title,
    Instant startAt,
    Instant endAt,
    EventStatus status,
    Currency currency,
    PricingRules pricingRules
  ) {
    this.id = id;
    this.venueId = venueId;
    this.title = title;
    this.startAt = startAt;
    this.endAt = endAt;
    this.status = status;
    this.currency = currency;
    this.pricingRules = pricingRules;
  }

  @PrePersist
  void prePersist() {
    if (id == null) id = UUID.randomUUID();
  }

  public UUID getId() {
    return id;
  }

  public UUID getVenueId() {
    return venueId;
  }

  public String getTitle() {
    return title;
  }

  public Instant getStartAt() {
    return startAt;
  }

  public Instant getEndAt() {
    return endAt;
  }

  public EventStatus getStatus() {
    return status;
  }

  public Currency getCurrency() {
    return currency;
  }

  public PricingRules getPricingRules() {
    return pricingRules;
  }
}
