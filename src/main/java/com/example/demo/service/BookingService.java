package com.example.demo.service;

import com.example.demo.domain.Event;
import com.example.demo.domain.Money;
import com.example.demo.domain.Reservation;
import com.example.demo.domain.ReservationSeat;
import com.example.demo.domain.Seat;
import com.example.demo.enums.DiscountType;
import com.example.demo.enums.ReservationStatus;
import com.example.demo.enums.SeatCategory;
import com.example.demo.exception.ConflictException;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.repository.jpa.EventJpaRepository;
import com.example.demo.repository.jpa.ReservationJpaRepository;
import com.example.demo.repository.jpa.SeatJpaRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BookingService {

  private static final Duration HOLD_DURATION = Duration.ofMinutes(5);

  private final EventJpaRepository eventRepo;
  private final SeatJpaRepository seatRepo;
  private final ReservationJpaRepository reservationRepo;

  public BookingService(
    EventJpaRepository eventRepo,
    SeatJpaRepository seatRepo,
    ReservationJpaRepository reservationRepo
  ) {
    this.eventRepo = eventRepo;
    this.seatRepo = seatRepo;
    this.reservationRepo = reservationRepo;
  }

  // ------------------------------------------------------------------
  //  Core booking flow
  // ------------------------------------------------------------------
  @Transactional(isolation = Isolation.SERIALIZABLE)
  public Reservation hold(UUID eventId, String email, List<UUID> seatIds) {
    Event event = eventRepo
      .findById(eventId)
      .orElseThrow(() ->
        new ResourceNotFoundException("Event not found: " + eventId)
      );

    List<Seat> seats = seatRepo.findAllById(seatIds);
    if (seats.size() != seatIds.size()) {
      throw new ResourceNotFoundException("One or more seats not found");
    }

    for (Seat s : seats) {
      if (!s.getVenueId().equals(event.getVenueId())) {
        throw new ConflictException(
          "Seat " + s.getId() + " does not belong to this event's venue"
        );
      }
    }

    Set<UUID> alreadyReserved = reservationRepo
      .findByEventId(eventId)
      .stream()
      .filter(
        r ->
          r.getStatus() == ReservationStatus.HOLD ||
          r.getStatus() == ReservationStatus.CONFIRMED
      )
      .flatMap(r -> r.getSeats().stream())
      .map(ReservationSeat::getSeatId)
      .collect(Collectors.toSet());

    List<UUID> conflicts = seatIds
      .stream()
      .filter(alreadyReserved::contains)
      .toList();
    if (!conflicts.isEmpty()) {
      throw new ConflictException("Seats already reserved: " + conflicts);
    }

    Reservation res = new Reservation(
      UUID.randomUUID(),
      eventId,
      email,
      ReservationStatus.HOLD,
      Instant.now().plus(HOLD_DURATION)
    );

    for (Seat seat : seats) {
      SeatCategory cat = event
        .getPricingRules()
        .getCategoryForSection(seat.getSection());
      Money price = event.getPricingRules().getPriceForCategory(cat);
      res.addSeat(
        new ReservationSeat(seat.getId(), eventId, price, DiscountType.NONE)
      );
    }

    return reservationRepo.save(res);
  }

  @Transactional
  public Reservation confirm(UUID reservationId) {
    Reservation r = reservationRepo
      .findById(reservationId)
      .orElseThrow(() ->
        new ResourceNotFoundException("Reservation not found")
      );
    if (r.getStatus() == ReservationStatus.CONFIRMED) return r; // idempotent
    if (r.getStatus() != ReservationStatus.HOLD) {
      throw new ConflictException(
        "Cannot confirm reservation in status " + r.getStatus()
      );
    }
    if (r.getHoldExpiresAt().isBefore(Instant.now())) {
      throw new ConflictException("Hold has expired");
    }
    r.confirm();
    return reservationRepo.save(r);
  }

  @Transactional
  public Reservation cancel(UUID reservationId) {
    Reservation r = reservationRepo
      .findById(reservationId)
      .orElseThrow(() ->
        new ResourceNotFoundException("Reservation not found")
      );
    if (r.getStatus() == ReservationStatus.CANCELLED) return r; // idempotent
    if (r.getStatus() == ReservationStatus.EXPIRED) {
      throw new ConflictException("Cannot cancel an expired reservation");
    }
    r.cancel();
    return reservationRepo.save(r);
  }

  // ------------------------------------------------------------------
  //  Extra methods for the CLI
  // ------------------------------------------------------------------
  @Transactional(readOnly = true)
  public List<Reservation> getReservationsForEvent(UUID eventId) {
    return reservationRepo.findByEventId(eventId);
  }

  @Transactional(readOnly = true)
  public List<Reservation> getReservationsByEmail(String email) {
    return reservationRepo.findByCustomerEmailIgnoreCase(email);
  }

  @Transactional(readOnly = true)
  public List<Reservation> getAllHolds() {
    return reservationRepo
      .findAll()
      .stream()
      .filter(r -> r.getStatus() == ReservationStatus.HOLD)
      .toList();
  }

  @Transactional
  public Reservation expire(UUID reservationId) {
    Reservation r = reservationRepo
      .findById(reservationId)
      .orElseThrow(() ->
        new ResourceNotFoundException("Reservation not found")
      );
    if (r.getStatus() != ReservationStatus.HOLD) return r;
    r.expire();
    return reservationRepo.save(r);
  }
}
