package com.example.demo.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.demo.domain.Event;
import com.example.demo.domain.Money;
import com.example.demo.domain.PricingRules;
import com.example.demo.domain.Reservation;
import com.example.demo.domain.ReservationSeat;
import com.example.demo.domain.Seat;
import com.example.demo.enums.Currency;
import com.example.demo.enums.DiscountType;
import com.example.demo.enums.EventStatus;
import com.example.demo.enums.ReservationStatus;
import com.example.demo.enums.SeatAttribute;
import com.example.demo.enums.SeatCategory;
import com.example.demo.repository.memory.InMemoryEventRepository;
import com.example.demo.repository.memory.InMemoryReservationRepository;
import com.example.demo.repository.memory.InMemorySeatRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ReportServiceTest {

  private ReportService reportService;
  private InMemoryEventRepository eventRepository;
  private InMemorySeatRepository seatRepository;
  private InMemoryReservationRepository reservationRepository;

  private UUID venueId1;
  private UUID venueId2;
  private UUID eventId1;
  private UUID eventId2;

  @BeforeEach
  void setUp() {
    eventRepository = new InMemoryEventRepository();
    seatRepository = new InMemorySeatRepository();
    reservationRepository = new InMemoryReservationRepository();

    reportService = new ReportService(
      reservationRepository,
      seatRepository,
      eventRepository
    );

    venueId1 = UUID.randomUUID();
    venueId2 = UUID.randomUUID();

    // Event 1 (venue 1)
    eventId1 = UUID.randomUUID();
    PricingRules rules1 = PricingRules.defaultRules(Currency.USD);
    var event1 = new Event(
      eventId1,
      venueId1,
      "Concert A",
      ZonedDateTime.now().plusDays(1),
      ZonedDateTime.now().plusDays(1).plusHours(2),
      EventStatus.SCHEDULED,
      Currency.USD,
      rules1
    );
    eventRepository.save(event1);

    // Event 2 (venue 2)
    eventId2 = UUID.randomUUID();
    PricingRules rules2 = PricingRules.defaultRules(Currency.EUR);
    var event2 = new Event(
      eventId2,
      venueId2,
      "Concert B",
      ZonedDateTime.now().plusDays(2),
      ZonedDateTime.now().plusDays(2).plusHours(2),
      EventStatus.SCHEDULED,
      Currency.EUR,
      rules2
    );
    eventRepository.save(event2);

    Set<SeatAttribute> emptyAttrs = Collections.emptySet();

    // Seats for venue 1 (2 seats)
    var seat1v1 = new Seat(
      UUID.randomUUID(),
      venueId1,
      "A",
      "1",
      1,
      SeatCategory.STANDARD,
      emptyAttrs
    );
    var seat2v1 = new Seat(
      UUID.randomUUID(),
      venueId1,
      "A",
      "1",
      2,
      SeatCategory.VIP,
      emptyAttrs
    );
    seatRepository.save(seat1v1);
    seatRepository.save(seat2v1);

    // Seats for venue 2 (3 seats)
    var seat1v2 = new Seat(
      UUID.randomUUID(),
      venueId2,
      "B",
      "1",
      1,
      SeatCategory.STANDARD,
      emptyAttrs
    );
    var seat2v2 = new Seat(
      UUID.randomUUID(),
      venueId2,
      "B",
      "1",
      2,
      SeatCategory.VIP,
      emptyAttrs
    );
    var seat3v2 = new Seat(
      UUID.randomUUID(),
      venueId2,
      "B",
      "1",
      3,
      SeatCategory.VVIP,
      emptyAttrs
    );
    seatRepository.save(seat1v2);
    seatRepository.save(seat2v2);
    seatRepository.save(seat3v2);

    // Reservations for Event 1 (both using seats from venue1)
    var res1 = new Reservation(
      UUID.randomUUID(),
      eventId1,
      "alice@example.com",
      ReservationStatus.HOLD,
      Instant.now(),
      null,
      Instant.now().plusSeconds(300),
      List.of(
        new ReservationSeat(
          UUID.randomUUID(),
          seat1v1.getId(),
          new Money(BigDecimal.valueOf(50.00), Currency.USD),
          DiscountType.NONE
        )
      )
    );
    reservationRepository.save(res1);

    var res2 = new Reservation(
      UUID.randomUUID(),
      eventId1,
      "bob@example.com",
      ReservationStatus.CONFIRMED,
      Instant.now(),
      Instant.now(),
      Instant.now().plusSeconds(300),
      List.of(
        new ReservationSeat(
          UUID.randomUUID(),
          seat2v1.getId(),
          new Money(BigDecimal.valueOf(100.00), Currency.USD),
          DiscountType.PERCENTAGE_10
        )
      )
    );
    reservationRepository.save(res2);
  }

  @Test
  void seatsPerEvent_shouldCountSeatsCorrectly() {
    var result = reportService.seatsPerEvent();
    assertThat(result).containsEntry(eventId1, 2L).containsEntry(eventId2, 3L);
  }

  @Test
  void reservationsPerEvent_shouldGroupByEventAndStatus() {
    var result = reportService.reservationsPerEvent();
    var event1Stats = result.get(eventId1);
    assertThat(event1Stats)
      .containsEntry("HOLD", 1L)
      .containsEntry("CONFIRMED", 1L);
    assertThat(result).doesNotContainKey(eventId2);
  }

  @Test
  void detailedSeatReport_shouldGenerateCorrectSummary() {
    String report = reportService.detailedSeatReport(eventId1);
    assertThat(report)
      .contains("Total seats: 2")
      .contains("Reserved seats: 2")
      .contains("Available seats: 0")
      .contains("Concert A");
  }

  @Test
  void detailedSeatReport_withNonExistentEvent_shouldReturnNotFound() {
    String report = reportService.detailedSeatReport(UUID.randomUUID());
    assertThat(report).contains("Event not found");
  }

  @Test
  void seatsPerEvent_withNoEvents_shouldReturnEmptyMap() {
    eventRepository
      .findAll()
      .forEach(e -> eventRepository.deleteById(e.getId()));
    var result = reportService.seatsPerEvent();
    assertThat(result).isEmpty();
  }

  @Test
  void reservationsPerEvent_withNoReservations_shouldReturnEmptyMap() {
    reservationRepository
      .findAll()
      .forEach(r -> reservationRepository.deleteById(r.getId()));
    var result = reportService.reservationsPerEvent();
    assertThat(result).isEmpty();
  }
}
