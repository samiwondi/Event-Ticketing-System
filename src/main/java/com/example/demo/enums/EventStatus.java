package com.example.demo.enums;

import java.time.ZonedDateTime;
import java.util.EnumSet;
import java.util.Set;

public enum EventStatus {
  SCHEDULED,
  IN_PROGRESS,
  COMPLETED,
  CANCELLED,
  POSTPONED;

  private Set<EventStatus> allowedTransitions;

  static {
    SCHEDULED.allowedTransitions = EnumSet.of(
      IN_PROGRESS,
      CANCELLED,
      POSTPONED
    );
    IN_PROGRESS.allowedTransitions = EnumSet.of(COMPLETED);
    POSTPONED.allowedTransitions = EnumSet.of(SCHEDULED, CANCELLED);
    COMPLETED.allowedTransitions = EnumSet.noneOf(EventStatus.class);
    CANCELLED.allowedTransitions = EnumSet.noneOf(EventStatus.class);
  }

  public EventStatus transitionTo(EventStatus target) {
    if (!allowedTransitions.contains(target)) {
      String message = String.format(
        "Cannot move from %s to %s. Allowed transitions: %s",
        this,
        target,
        allowedTransitions
      );
      throw new IllegalStateException(message);
    }
    return target;
  }

  public EventStatus transitionTo(
    EventStatus target,
    ZonedDateTime newStartDate
  ) {
    if (target != POSTPONED) {
      String message = String.format(
        "Cannot move from %s to %s. Allowed transitions: %s",
        this,
        target,
        allowedTransitions
      );
      throw new IllegalStateException(message);
    }

    if (
      target == POSTPONED &&
      (newStartDate == null || newStartDate.isBefore(ZonedDateTime.now()))
    ) {
      throw new IllegalStateException(
        String.format(
          "Cannot reschedule because the new date (%s) must be in the future.",
          newStartDate
        )
      );
    }

    return target;
  }
}
