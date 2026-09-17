package com.example.demo.service;

import com.example.demo.domain.Event;
import com.example.demo.domain.Seat;
import com.example.demo.enums.ReservationStatus;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.repository.jpa.*;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EventService {

  private final EventJpaRepository eventRepo;
  private final SeatJpaRepository seatRepo;
  private final ReservationJpaRepository reservationRepo;

  public EventService(
    EventJpaRepository eventRepo,
    SeatJpaRepository seatRepo,
    ReservationJpaRepository reservationRepo
  ) {
    this.eventRepo = eventRepo;
    this.seatRepo = seatRepo;
    this.reservationRepo = reservationRepo;
  }

  @Transactional(readOnly = true)
  public Page<Event> search(
    Instant from,
    Instant to,
    String q,
    Pageable pageable
  ) {
    return eventRepo.search(from, to, q, pageable);
  }

  @Transactional(readOnly = true)
  public Event findById(UUID id) {
    return eventRepo
      .findById(id)
      .orElseThrow(() ->
        new ResourceNotFoundException("Event not found: " + id)
      );
  }

  /**
   * Returns seats for an event. If onlyAvailable, excludes seats that are on HOLD or CONFIRMED.
   */
  @Transactional(readOnly = true)
  public List<Seat> seatsForEvent(UUID eventId, boolean onlyAvailable) {
    Event event = findById(eventId);
    List<Seat> all = seatRepo.findByVenueIdOrderBySectionAscRowAscNumberAsc(
      event.getVenueId()
    );
    if (!onlyAvailable) return all;

    Set<UUID> reserved = reservationRepo
      .findByEventId(eventId)
      .stream()
      .filter(
        r ->
          r.getStatus() == ReservationStatus.HOLD ||
          r.getStatus() == ReservationStatus.CONFIRMED
      )
      .flatMap(r -> r.getSeats().stream())
      .map(rs -> rs.getSeatId())
      .collect(Collectors.toSet());

    return all
      .stream()
      .filter(s -> !reserved.contains(s.getId()))
      .toList();
  }
}
