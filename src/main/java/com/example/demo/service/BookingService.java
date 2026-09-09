package com.example.demo.service;

import com.example.demo.cache.SeatAvailabilityCache;
import com.example.demo.domain.Money;
import com.example.demo.domain.Reservation;
import com.example.demo.domain.ReservationSeat;
import com.example.demo.enums.DiscountType;
import com.example.demo.enums.ReservationStatus;
import com.example.demo.exception.InvalidSeatException;
import com.example.demo.exception.ReservationException;
import com.example.demo.exception.ValidationException;
import com.example.demo.repository.EventRepository;
import com.example.demo.repository.ReservationRepository;
import com.example.demo.repository.SeatRepository;
import com.example.demo.util.IdGenerator;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

public class BookingService {

  private final ReservationRepository reservationRepository;
  private final EventRepository eventRepository;
  private final SeatRepository seatRepository;
  private final PricingService pricingService;
  private final IdGenerator idGenerator;
  private final Duration holdDuration = Duration.ofMinutes(5);

  private final ConcurrentHashMap<UUID, ReentrantLock> eventLocks =
    new ConcurrentHashMap<>();
  private final SeatAvailabilityCache seatCache;

  public BookingService(
    ReservationRepository reservationRepository,
    EventRepository eventRepository,
    SeatRepository seatRepository,
    PricingService pricingService,
    IdGenerator idGenerator
  ) {
    this.reservationRepository = reservationRepository;
    this.eventRepository = eventRepository;
    this.seatRepository = seatRepository;
    this.pricingService = pricingService;
    this.idGenerator = idGenerator;
    this.seatCache = new SeatAvailabilityCache(seatRepository, 100);
  }

  public Reservation holdSeats(
    UUID eventId,
    String customerEmail,
    List<UUID> seatIds
  ) {
    ReentrantLock lock = eventLocks.computeIfAbsent(eventId, k ->
      new ReentrantLock(true)
    );
    lock.lock();
    try {
      var event = eventRepository
        .findById(eventId)
        .orElseThrow(() -> new ValidationException("Event not found"));

      var allSeats = seatCache.getSeatsForEvent(eventId, event.getVenueId());
      var seats = seatIds
        .stream()
        .map(id ->
          allSeats
            .stream()
            .filter(s -> s.getId().equals(id))
            .findFirst()
            .orElseThrow(() ->
              new InvalidSeatException(
                "Seat " + id + " not found in this venue"
              )
            )
        )
        .collect(Collectors.toList());

      var reservationsForEvent = reservationRepository.findByEventId(eventId);
      var reservedSeatIds = reservationsForEvent
        .stream()
        .flatMap(r -> r.getSeats().stream().map(ReservationSeat::seatId))
        .collect(Collectors.toSet());

      var conflictingSeats = seatIds
        .stream()
        .filter(reservedSeatIds::contains)
        .collect(Collectors.toList());
      if (!conflictingSeats.isEmpty()) {
        throw new ReservationException(
          "Seats already reserved: " + conflictingSeats
        );
      }

      var reservationId = idGenerator.generateId();
      var now = Instant.now();
      var holdExpires = now.plus(holdDuration);

      var reservationSeats = seats
        .stream()
        .map(seat -> {
          Money price = pricingService.calculatePrice(event, seat);
          return new ReservationSeat(
            reservationId,
            seat.getId(),
            price,
            DiscountType.NONE
          );
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

      Reservation saved = reservationRepository.save(reservation);
      seatCache.invalidate(eventId);
      return saved;
    } finally {
      lock.unlock();
    }
  }

  public void confirmReservation(UUID reservationId) {
    var reservation = reservationRepository
      .findById(reservationId)
      .orElseThrow(() -> new ValidationException("Reservation not found"));
    if (reservation.getStatus() == ReservationStatus.CONFIRMED) return;
    UUID eventId = reservation.getEventId();
    ReentrantLock lock = eventLocks.computeIfAbsent(eventId, k ->
      new ReentrantLock(true)
    );
    lock.lock();
    try {
      var res = reservationRepository
        .findById(reservationId)
        .orElseThrow(() -> new ValidationException("Reservation not found"));
      if (res.getStatus() == ReservationStatus.CONFIRMED) return;
      res.confirm();
      reservationRepository.save(res);
      seatCache.invalidate(eventId);
    } finally {
      lock.unlock();
    }
  }

  public void cancelReservation(UUID reservationId) {
    var reservation = reservationRepository
      .findById(reservationId)
      .orElseThrow(() -> new ValidationException("Reservation not found"));
    if (reservation.getStatus() == ReservationStatus.CANCELLED) return;
    UUID eventId = reservation.getEventId();
    ReentrantLock lock = eventLocks.computeIfAbsent(eventId, k ->
      new ReentrantLock(true)
    );
    lock.lock();
    try {
      var res = reservationRepository
        .findById(reservationId)
        .orElseThrow(() -> new ValidationException("Reservation not found"));
      if (res.getStatus() == ReservationStatus.CANCELLED) return;
      res.cancel();
      reservationRepository.save(res);
      seatCache.invalidate(eventId);
    } finally {
      lock.unlock();
    }
  }

  public void expireReservation(UUID reservationId) {
    var reservation = reservationRepository
      .findById(reservationId)
      .orElseThrow(() -> new ValidationException("Reservation not found"));
    if (reservation.getStatus() != ReservationStatus.HOLD) return; // only HOLD can expire
    UUID eventId = reservation.getEventId();
    ReentrantLock lock = eventLocks.computeIfAbsent(eventId, k ->
      new ReentrantLock(true)
    );
    lock.lock();
    try {
      var res = reservationRepository
        .findById(reservationId)
        .orElseThrow(() -> new ValidationException("Reservation not found"));
      if (res.getStatus() == ReservationStatus.HOLD) {
        res.expire();
        reservationRepository.save(res);
        seatCache.invalidate(eventId);
      }
    } finally {
      lock.unlock();
    }
  }

  public List<Reservation> getAllHolds() {
    return reservationRepository
      .findAll()
      .stream()
      .filter(r -> r.getStatus() == ReservationStatus.HOLD)
      .collect(Collectors.toList());
  }

  public List<Reservation> getReservationsForEvent(UUID eventId) {
    return reservationRepository.findByEventId(eventId);
  }

  public int getCacheSize() {
    return seatCache.size();
  }

  public int getLockedEventCount() {
    int count = 0;
    for (ReentrantLock lock : eventLocks.values()) {
      if (lock.isLocked()) count++;
    }
    return count;
  }

  public void clearCache() {
    seatCache.clear();
  }
}
