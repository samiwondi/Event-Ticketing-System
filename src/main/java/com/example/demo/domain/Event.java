package com.example.demo.domain;

import com.example.demo.enums.Currency;
import com.example.demo.enums.EventStatus;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.Objects;
import java.util.UUID;

public class Event {

  private final UUID id;
  private final UUID venueId;
  private final String title;
  private ZonedDateTime startAt;
  private ZonedDateTime endAt;
  private EventStatus status;
  private final Currency currency;
  private final PricingRules pricingRules;

  public Event(
    UUID id,
    UUID venueId,
    String title,
    ZonedDateTime startAt,
    ZonedDateTime endAt,
    EventStatus status,
    Currency currency,
    PricingRules pricingRules
  ) {
    this.id = Objects.requireNonNull(id);
    this.venueId = Objects.requireNonNull(venueId);
    this.title = Objects.requireNonNull(title);
    setTimes(startAt, endAt);
    this.status = status != null ? status : EventStatus.SCHEDULED;

    if (currency == null) {
      throw new IllegalArgumentException("Currency must not be null");
    }
    if (pricingRules == null) {
      this.pricingRules = PricingRules.defaultRules(currency);
    } else {
      if (pricingRules.currency() != currency) {
        throw new IllegalArgumentException(
          "Currency mismatch: event currency does not match pricing rules currency"
        );
      }
      this.pricingRules = pricingRules;
    }
    this.currency = currency;
    autoStatus();
  }

  private void setTimes(ZonedDateTime startAt, ZonedDateTime endAt) {
    if (startAt == null || endAt == null) {
      throw new IllegalArgumentException(
        "Start and end times must not be null"
      );
    }
    if (startAt.isAfter(endAt)) {
      throw new IllegalArgumentException("Start must be before end");
    }
    this.startAt = startAt;
    this.endAt = endAt;
  }

  public void autoStatus() {
    ZonedDateTime now = ZonedDateTime.now();
    if (status == EventStatus.CANCELLED || status == EventStatus.COMPLETED) {
      return;
    }
    if (startAt.isAfter(now)) {
      this.status = EventStatus.SCHEDULED;
    } else if (startAt.isBefore(now) && endAt.isAfter(now)) {
      this.status = EventStatus.IN_PROGRESS;
    } else if (endAt.isBefore(now)) {
      this.status = EventStatus.COMPLETED;
    }
  }

  public void manualStatus(EventStatus target, ZonedDateTime newStartTime) {
    if (target == null) {
      throw new IllegalArgumentException("Target status cannot be null");
    }
    if (target == EventStatus.POSTPONED) {
      if (newStartTime == null) {
        throw new IllegalArgumentException(
          "New start time required for postponement"
        );
      }
      if (newStartTime.isBefore(ZonedDateTime.now())) {
        throw new IllegalArgumentException(
          "Postponed Event must be in the future."
        );
      }
      Duration duration = Duration.between(this.startAt, this.endAt);
      this.status = target;
      setTimes(newStartTime, newStartTime.plus(duration));
      return;
    }
    // For other transitions (CANCELLED, etc.) use enum transition logic
    this.status = this.status.transitionTo(target);
    // Don't override CANCELLED or COMPLETED
    if (target != EventStatus.CANCELLED && target != EventStatus.COMPLETED) {
      autoStatus();
    }
  }

  public boolean isSoldOut(int totalSeats, int reservedSeats) {
    return reservedSeats >= totalSeats;
  }

  // ------ Getters ------
  public UUID getId() {
    return id;
  }

  public UUID getVenueId() {
    return venueId;
  }

  public String getTitle() {
    return title;
  }

  public ZonedDateTime getStartAt() {
    return startAt;
  }

  public ZonedDateTime getEndAt() {
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

  // ------ equals / hashCode / toString ------
  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof Event event)) return false;
    return Objects.equals(id, event.id);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id);
  }

  @Override
  public String toString() {
    return "Event{id=" + id + ", title='" + title + "', status=" + status + "}";
  }
}
