package com.example.demo.service;

import com.example.demo.domain.*;
import com.example.demo.exception.ReservationException;
import com.example.demo.repository.*;
import com.example.demo.repository.memory.*;
import com.example.demo.util.IdGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

class BookingServiceTest {
    private BookingService bookingService;
    private VenueService venueService;
    private UUID venueId;
    private UUID eventId;
    private UUID seat1Id, seat2Id;

    @BeforeEach
    void setUp() {
        var venueRepo = new InMemoryVenueRepository();
        var eventRepo = new InMemoryEventRepository();
        var seatRepo = new InMemorySeatRepository();
        var reservationRepo = new InMemoryReservationRepository();
        var idGen = new IdGenerator() {
            @Override
            public UUID generateId() {
                return UUID.randomUUID();
            }
        };
        var pricingService = new PricingService();
        venueService = new VenueService(venueRepo, eventRepo, seatRepo, idGen);
        bookingService = new BookingService(reservationRepo, eventRepo, seatRepo, pricingService, idGen);

        // Create venue and event
        var venue = venueService.createVenue("Stadium", "123 Main", "America/New_York");
        venueId = venue.getId();
        var start = ZonedDateTime.now().plusDays(1);
        var end = start.plusHours(2);
        var event = venueService.createEvent(venueId, "Concert", start, end);
        eventId = event.getId();

        // Create seats
        var seat1 = venueService.createSeat(venueId, "A", "1", 1, null);
        var seat2 = venueService.createSeat(venueId, "A", "1", 2, null);
        seat1Id = seat1.getId();
        seat2Id = seat2.getId();
    }

    @Test
    void shouldHoldSeats() {
        var reservation = bookingService.holdSeats(eventId, "test@example.com", List.of(seat1Id, seat2Id));
        assertThat(reservation).isNotNull();
        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.HOLD);
        assertThat(reservation.getSeats()).hasSize(2);
    }

    @Test
    void shouldNotAllowDoubleBookingSameSeat() {
        bookingService.holdSeats(eventId, "test@example.com", List.of(seat1Id));
        assertThatThrownBy(() -> bookingService.holdSeats(eventId, "test2@example.com", List.of(seat1Id)))
                .isInstanceOf(ReservationException.class)
                .hasMessageContaining("already reserved");
    }

    @Test
    void shouldConfirmReservation() {
        var reservation = bookingService.holdSeats(eventId, "test@example.com", List.of(seat1Id));
        bookingService.confirmReservation(reservation.getId());
        var confirmed = bookingService.getReservationsForEvent(eventId).get(0);
        assertThat(confirmed.getStatus()).isEqualTo(ReservationStatus.CONFIRMED);
    }

    @Test
    void confirmShouldBeIdempotent() {
        var reservation = bookingService.holdSeats(eventId, "test@example.com", List.of(seat1Id));
        bookingService.confirmReservation(reservation.getId());
        bookingService.confirmReservation(reservation.getId()); // second time
        assertThat(bookingService.getReservationsForEvent(eventId).get(0).getStatus())
                .isEqualTo(ReservationStatus.CONFIRMED);
    }
}