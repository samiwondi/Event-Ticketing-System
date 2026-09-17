package com.example.demo.dto;

import com.example.demo.domain.Event;
import java.time.Instant;
import java.util.UUID;

public record EventResponse(
  UUID id,
  UUID venueId,
  String title,
  Instant startAt,
  Instant endAt,
  String status,
  String currency
) {
  public static EventResponse from(Event e) {
    return new EventResponse(
      e.getId(),
      e.getVenueId(),
      e.getTitle(),
      e.getStartAt(),
      e.getEndAt(),
      e.getStatus().name(),
      e.getCurrency().name()
    );
  }
}
