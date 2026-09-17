package com.example.demo.dto;

import com.example.demo.domain.Event;
import java.time.Instant;
import java.util.UUID;

public record EventSummaryResponse(
  UUID id,
  String title,
  Instant startAt,
  Instant endAt,
  String status
) {
  public static EventSummaryResponse from(Event e) {
    return new EventSummaryResponse(
      e.getId(),
      e.getTitle(),
      e.getStartAt(),
      e.getEndAt(),
      e.getStatus().name()
    );
  }
}
