package com.example.demo.service;

import com.example.demo.domain.Event;
import com.example.demo.domain.Reservation;
import com.example.demo.domain.ReservationSeat;
import com.example.demo.repository.jpa.EventJpaRepository;
import com.example.demo.repository.jpa.ReservationJpaRepository;
import com.example.demo.repository.jpa.SeatJpaRepository;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReportService {

  private final ReservationJpaRepository reservationRepo;
  private final SeatJpaRepository seatRepo;
  private final EventJpaRepository eventRepo;

  public ReportService(
    ReservationJpaRepository reservationRepo,
    SeatJpaRepository seatRepo,
    EventJpaRepository eventRepo
  ) {
    this.reservationRepo = reservationRepo;
    this.seatRepo = seatRepo;
    this.eventRepo = eventRepo;
  }

  @Transactional(readOnly = true)
  public Map<UUID, Long> seatsPerEvent() {
    Map<UUID, Long> result = new LinkedHashMap<>();
    for (Event e : eventRepo.findAll()) {
      long count = seatRepo
        .findByVenueIdOrderBySectionAscRowAscNumberAsc(e.getVenueId())
        .size();
      result.put(e.getId(), count);
    }
    return result;
  }

  @Transactional(readOnly = true)
  public Map<UUID, Map<String, Long>> reservationsPerEvent() {
    Map<UUID, Map<String, Long>> result = new LinkedHashMap<>();
    for (Event e : eventRepo.findAll()) {
      Map<String, Long> byStatus = reservationRepo
        .findByEventId(e.getId())
        .stream()
        .collect(
          Collectors.groupingBy(
            r -> r.getStatus().name(),
            Collectors.counting()
          )
        );
      if (!byStatus.isEmpty()) result.put(e.getId(), byStatus);
    }
    return result;
  }

  @Transactional(readOnly = true)
  public String detailedSeatReport(UUID eventId) {
    Event event = eventRepo.findById(eventId).orElse(null);
    if (event == null) return "Event not found";

    int totalSeats = seatRepo
      .findByVenueIdOrderBySectionAscRowAscNumberAsc(event.getVenueId())
      .size();
    long reserved = reservationRepo
      .findByEventId(eventId)
      .stream()
      .flatMap(r -> r.getSeats().stream())
      .map(ReservationSeat::getSeatId)
      .distinct()
      .count();

    return (
      "Seat report for event: " +
      event.getTitle() +
      "\n" +
      "Total seats: " +
      totalSeats +
      "\n" +
      "Reserved seats: " +
      reserved +
      "\n" +
      "Available seats: " +
      (totalSeats - reserved)
    );
  }
}
