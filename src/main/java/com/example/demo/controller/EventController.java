package com.example.demo.controller;

import com.example.demo.dto.*;
import com.example.demo.service.EventService;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/events")
public class EventController {

  private final EventService eventService;

  public EventController(EventService eventService) {
    this.eventService = eventService;
  }

  @GetMapping
  public ResponseEntity<Page<EventSummaryResponse>> list(
    @RequestParam(required = false) @DateTimeFormat(
      iso = DateTimeFormat.ISO.DATE_TIME
    ) Instant from,
    @RequestParam(required = false) @DateTimeFormat(
      iso = DateTimeFormat.ISO.DATE_TIME
    ) Instant to,
    @RequestParam(required = false) String q,
    @RequestParam(defaultValue = "0") int page,
    @RequestParam(defaultValue = "20") int size
  ) {
    var pageable = PageRequest.of(
      page,
      Math.min(size, 100),
      Sort.by("startAt").ascending()
    );
    return ResponseEntity.ok(
      eventService.search(from, to, q, pageable).map(EventSummaryResponse::from)
    );
  }

  @GetMapping("/{id}")
  public ResponseEntity<EventResponse> get(@PathVariable UUID id) {
    return ResponseEntity.ok(EventResponse.from(eventService.findById(id)));
  }

  @GetMapping("/{id}/seats")
  public ResponseEntity<List<SeatResponse>> seats(
    @PathVariable UUID id,
    @RequestParam(defaultValue = "false") boolean onlyAvailable
  ) {
    return ResponseEntity.ok(
      eventService
        .seatsForEvent(id, onlyAvailable)
        .stream()
        .map(SeatResponse::from)
        .toList()
    );
  }
}
