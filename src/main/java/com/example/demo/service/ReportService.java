package com.example.demo.service;

import com.example.demo.domain.Reservation;
import com.example.demo.domain.ReservationSeat;
import com.example.demo.repository.EventRepository;
import com.example.demo.repository.ReservationRepository;
import com.example.demo.repository.SeatRepository;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public class ReportService {

  private final ReservationRepository reservationRepository;
  private final SeatRepository seatRepository;
  private final EventRepository eventRepository;

  public ReportService(
    ReservationRepository reservationRepository,
    SeatRepository seatRepository,
    EventRepository eventRepository
  ) {
    this.reservationRepository = reservationRepository;
    this.seatRepository = seatRepository;
    this.eventRepository = eventRepository;
  }

  public Map<UUID, Long> seatsPerEvent() {
    var events = eventRepository.findAll();
    return events
      .stream()
      .collect(
        Collectors.toMap(
          event -> event.getId(),
          event ->
            (long) seatRepository.findByVenueId(event.getVenueId()).size()
        )
      );
  }

  public Map<UUID, Map<String, Long>> reservationsPerEvent() {
    var allReservations = reservationRepository.findAll();
    return allReservations
      .stream()
      .collect(
        Collectors.groupingBy(
          Reservation::getEventId,
          Collectors.groupingBy(
            r -> r.getStatus().name(),
            Collectors.counting()
          )
        )
      );
  }

  public String detailedSeatReport(UUID eventId) {
    var event = eventRepository.findById(eventId).orElse(null);
    if (event == null) return "Event not found";

    var allSeats = seatRepository.findByVenueId(event.getVenueId());
    var reservations = reservationRepository.findByEventId(eventId);
    var reservedSeatIds = reservations
      .stream()
      .flatMap(r -> r.getSeats().stream().map(ReservationSeat::seatId))
      .collect(Collectors.toSet());

    return (
      "Seat report for event: " +
      event.getTitle() +
      "\n" +
      "Total seats: " +
      allSeats.size() +
      "\n" +
      "Reserved seats: " +
      reservedSeatIds.size() +
      "\n" +
      "Available seats: " +
      (allSeats.size() - reservedSeatIds.size())
    );
  }
}
