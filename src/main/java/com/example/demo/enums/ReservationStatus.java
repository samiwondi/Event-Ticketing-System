package com.example.demo.enums;

import java.util.EnumSet;
import java.util.Set;

public enum ReservationStatus {
  HOLD,
  CONFIRMED,
  CANCELLED,
  EXPIRED;

  private Set<ReservationStatus> allowedTransitions;

  static {
    HOLD.allowedTransitions = EnumSet.of(CONFIRMED, CANCELLED, EXPIRED);
    CONFIRMED.allowedTransitions = EnumSet.noneOf(ReservationStatus.class);
    CANCELLED.allowedTransitions = EnumSet.noneOf(ReservationStatus.class);
    EXPIRED.allowedTransitions = EnumSet.noneOf(ReservationStatus.class);
  }

  public boolean canTransitionTo(ReservationStatus target) {
    return allowedTransitions.contains(target);
  }

  public ReservationStatus transitionTo(ReservationStatus target) {
    if (!canTransitionTo(target)) {
      throw new IllegalStateException(
        String.format(
          "Cannot transition from %s to %s. Allowed: %s",
          this,
          target,
          allowedTransitions
        )
      );
    }
    return target;
  }
}
