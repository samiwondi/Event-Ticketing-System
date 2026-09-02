package com.example.demo.service;

import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.example.demo.domain.DiscountType;
import com.example.demo.domain.Event;
import com.example.demo.domain.Money;
import com.example.demo.domain.Reservation;
import com.example.demo.domain.ReservationSeat;
import com.example.demo.domain.ReservationStatus;
import com.example.demo.domain.Seat;
import com.example.demo.repository.memory.InMemoryEventRepository;
import com.example.demo.repository.memory.InMemoryReservationRepository;
import com.example.demo.repository.memory.InMemorySeatRepository;

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
        var event1 = new Event(
                eventId1,
                venueId1,
                "Concert A",
                ZonedDateTime.now().plusDays(1),
                ZonedDateTime.now().plusDays(1).plusHours(2),
                "SCHEDULED"
        );
        eventRepository.save(event1);

        // Event 2 (venue 2)
        eventId2 = UUID.randomUUID();
        var event2 = new Event(
                eventId2,
                venueId2,
                "Concert B",
                ZonedDateTime.now().plusDays(2),
                ZonedDateTime.now().plusDays(2).plusHours(2),
                "SCHEDULED"
        );
        eventRepository.save(event2);

        // Seats for venue 1 (2 seats)
        var seat1v1 = new Seat(UUID.randomUUID(), venueId1, "A", "1", 1, null);
        var seat2v1 = new Seat(UUID.randomUUID(), venueId1, "A", "1", 2, null);
        seatRepository.save(seat1v1);
        seatRepository.save(seat2v1);

        // Seats for venue 2 (3 seats)
        var seat1v2 = new Seat(UUID.randomUUID(), venueId2, "B", "1", 1, null);
        var seat2v2 = new Seat(UUID.randomUUID(), venueId2, "B", "1", 2, null);
        var seat3v2 = new Seat(UUID.randomUUID(), venueId2, "B", "1", 3, null);
        seatRepository.save(seat1v2);
        seatRepository.save(seat2v2);
        seatRepository.save(seat3v2);

        // ----------------------------
        // ONLY TWO RESERVATIONS, both using seats from venue 1
        // Reservation 1 (HOLD)
        var res1 = new Reservation(
                UUID.randomUUID(),
                eventId1,
                "alice@example.com",
                ReservationStatus.HOLD,
                Instant.now(),
                null,
                Instant.now().plusSeconds(300),
                List.of(new ReservationSeat(UUID.randomUUID(), seat1v1.getId(), Money.zero("USD"), DiscountType.NONE))
        );
        reservationRepository.save(res1);

        // Reservation 2 (CONFIRMED)
        var res2 = new Reservation(
                UUID.randomUUID(),
                eventId1,
                "bob@example.com",
                ReservationStatus.CONFIRMED,
                Instant.now(),
                Instant.now(),
                Instant.now().plusSeconds(300),
                List.of(new ReservationSeat(UUID.randomUUID(), seat2v1.getId(), Money.zero("USD"), DiscountType.NONE))
        );
        reservationRepository.save(res2);
        // (NO third reservation)
    }

    @Test
    void seatsPerEvent_shouldCountSeatsCorrectly() {
        var result = reportService.seatsPerEvent();
        assertThat(result)
                .containsEntry(eventId1, 2L)
                .containsEntry(eventId2, 3L);
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
        eventRepository.findAll().forEach(e -> eventRepository.deleteById(e.getId()));
        var result = reportService.seatsPerEvent();
        assertThat(result).isEmpty();
    }

    @Test
    void reservationsPerEvent_withNoReservations_shouldReturnEmptyMap() {
        reservationRepository.findAll().forEach(r -> reservationRepository.deleteById(r.getId()));
        var result = reportService.reservationsPerEvent();
        assertThat(result).isEmpty();
    }
}