package com.example.demo.service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import com.example.demo.domain.DiscountType;
import com.example.demo.domain.Money;
import com.example.demo.domain.Reservation;
import com.example.demo.domain.ReservationSeat;
import com.example.demo.domain.ReservationStatus;
import com.example.demo.domain.Seat;
import com.example.demo.exception.InvalidSeatException;
import com.example.demo.exception.ReservationException;
import com.example.demo.exception.ValidationException;
import com.example.demo.repository.EventRepository;
import com.example.demo.repository.ReservationRepository;
import com.example.demo.repository.SeatRepository;
import com.example.demo.util.IdGenerator;

public class BookingService {
    private final ReservationRepository reservationRepository;
    private final EventRepository eventRepository;
    private final SeatRepository seatRepository;
    private final PricingService pricingService;
    private final IdGenerator idGenerator;
    private final Duration holdDuration = Duration.ofMinutes(5);

    public BookingService(ReservationRepository reservationRepository,
                        EventRepository eventRepository,
                        SeatRepository seatRepository,
                        PricingService pricingService,
                        IdGenerator idGenerator) {
        this.reservationRepository = reservationRepository;
        this.eventRepository = eventRepository;
        this.seatRepository = seatRepository;
        this.pricingService = pricingService;
        this.idGenerator = idGenerator;
    }

    public Reservation holdSeats(UUID eventId, String customerEmail, List<UUID> seatIds) {
        var event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ValidationException("Event not found"));

        var seats = seatIds.stream()
                .map(id -> seatRepository.findById(id)
                        .orElseThrow(() -> new InvalidSeatException("Seat " + id + " not found")))
                .collect(Collectors.toList());

        var venueId = event.getVenueId();
        for (Seat seat : seats) {
            if (!seat.getVenueId().equals(venueId)) {
                throw new ValidationException("Seat " + seat.getId() + " does not belong to this event's venue");
            }
        }

        var reservationsForEvent = reservationRepository.findByEventId(eventId);
        var reservedSeatIds = reservationsForEvent.stream()
                .flatMap(r -> r.getSeats().stream().map(ReservationSeat::seatId))
                .collect(Collectors.toSet());

        var conflictingSeats = seatIds.stream()
                .filter(reservedSeatIds::contains)
                .collect(Collectors.toList());
        if (!conflictingSeats.isEmpty()) {
            throw new ReservationException("Seats already reserved: " + conflictingSeats);
        }

        var reservationId = idGenerator.generateId();
        var now = Instant.now();
        var holdExpires = now.plus(holdDuration);

        var reservationSeats = seats.stream()
                .map(seat -> {
                    var price = Money.zero("USD");
                    return new ReservationSeat(reservationId, seat.getId(), price, DiscountType.NONE);
                })
                .collect(Collectors.toList());

        var reservation = new Reservation(
                reservationId,
                eventId,
                customerEmail,
                ReservationStatus.HOLD,
                now,
                null,
                holdExpires,
                reservationSeats
        );

        return reservationRepository.save(reservation);
    }

    public void confirmReservation(UUID reservationId) {
        var reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new ValidationException("Reservation not found"));
        if (reservation.getStatus() == ReservationStatus.CONFIRMED) return;
        reservation.confirm();
        reservationRepository.save(reservation);
    }

    public void cancelReservation(UUID reservationId) {
        var reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new ValidationException("Reservation not found"));
        if (reservation.getStatus() == ReservationStatus.CANCELLED) return;
        reservation.cancel();
        reservationRepository.save(reservation);
    }

    public List<Reservation> getReservationsForEvent(UUID eventId) {
        return reservationRepository.findByEventId(eventId);
    }
}