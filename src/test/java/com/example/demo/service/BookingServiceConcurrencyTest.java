package com.example.demo.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.demo.domain.PricingRules;
import com.example.demo.domain.Reservation;
import com.example.demo.enums.Currency;
import com.example.demo.enums.EventStatus;
import com.example.demo.enums.ReservationStatus;
import com.example.demo.enums.SeatAttribute;
import com.example.demo.enums.SeatCategory;
import com.example.demo.repository.memory.InMemoryEventRepository;
import com.example.demo.repository.memory.InMemoryReservationRepository;
import com.example.demo.repository.memory.InMemorySeatRepository;
import com.example.demo.repository.memory.InMemoryVenueRepository;
import com.example.demo.util.IdGenerator;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class BookingServiceConcurrencyTest {

  private BookingService bookingService;
  private UUID eventId;
  private UUID seatId1, seatId2;

  @BeforeEach
  void setUp() {
    var venueRepo = new InMemoryVenueRepository();
    var eventRepo = new InMemoryEventRepository();
    var seatRepo = new InMemorySeatRepository();
    var reservationRepo = new InMemoryReservationRepository();
    IdGenerator idGen = UUID::randomUUID;
    var pricingService = new PricingService();
    var venueService = new VenueService(venueRepo, eventRepo, seatRepo, idGen);

    var venue = venueService.createVenue(
      "Stadium",
      "123 Main",
      ZoneId.systemDefault().getId()
    );
    var start = ZonedDateTime.now().plusDays(1);
    var end = start.plusHours(2);
    PricingRules rules = PricingRules.defaultRules(Currency.USD);
    var event = venueService.createEvent(
      venue.getId(),
      "Concert",
      start,
      end,
      EventStatus.SCHEDULED,
      Currency.USD,
      rules
    );
    eventId = event.getId();

    Set<SeatAttribute> emptyAttrs = Collections.emptySet();
    var seat1 = venueService.createSeat(
      venue.getId(),
      "A",
      "1",
      1,
      SeatCategory.STANDARD,
      emptyAttrs
    );
    var seat2 = venueService.createSeat(
      venue.getId(),
      "A",
      "1",
      2,
      SeatCategory.VIP,
      emptyAttrs
    );
    seatId1 = seat1.getId();
    seatId2 = seat2.getId();

    bookingService = new BookingService(
      reservationRepo,
      eventRepo,
      seatRepo,
      pricingService,
      idGen
    );
  }

  @Test
  void concurrentHoldSameSeat_shouldOnlyOneSucceed()
    throws InterruptedException {
    int threadCount = 10;
    ExecutorService executor = Executors.newFixedThreadPool(threadCount);
    CountDownLatch startLatch = new CountDownLatch(1);
    CountDownLatch doneLatch = new CountDownLatch(threadCount);
    AtomicInteger successCount = new AtomicInteger(0);
    AtomicInteger failureCount = new AtomicInteger(0);

    for (int i = 0; i < threadCount; i++) {
      final int index = i;
      executor.submit(() -> {
        try {
          startLatch.await();
          bookingService.holdSeats(
            eventId,
            "user" + index + "@test.com",
            List.of(seatId1)
          );
          successCount.incrementAndGet();
        } catch (Exception e) {
          failureCount.incrementAndGet();
        } finally {
          doneLatch.countDown();
        }
      });
    }

    startLatch.countDown();
    doneLatch.await(5, TimeUnit.SECONDS);
    executor.shutdownNow();

    assertThat(successCount.get()).isEqualTo(1);
    assertThat(failureCount.get()).isEqualTo(threadCount - 1);

    var reservations = bookingService.getReservationsForEvent(eventId);
    assertThat(reservations).hasSize(1);
    assertThat(reservations.get(0).getStatus()).isEqualTo(
      ReservationStatus.HOLD
    );
  }

  @Test
  void concurrentHoldDifferentSeats_shouldBothSucceed()
    throws InterruptedException {
    int threadCount = 10;
    ExecutorService executor = Executors.newFixedThreadPool(threadCount);
    CountDownLatch startLatch = new CountDownLatch(1);
    CountDownLatch doneLatch = new CountDownLatch(threadCount);
    AtomicInteger successCount = new AtomicInteger(0);

    List<UUID> seatIdChoices = List.of(seatId1, seatId2);

    for (int i = 0; i < threadCount; i++) {
      final int index = i;
      UUID seatToHold = seatIdChoices.get(i % 2);
      executor.submit(() -> {
        try {
          startLatch.await();
          bookingService.holdSeats(
            eventId,
            "user" + index + "@test.com",
            List.of(seatToHold)
          );
          successCount.incrementAndGet();
        } catch (Exception e) {
          // expected if seat already taken
        } finally {
          doneLatch.countDown();
        }
      });
    }

    startLatch.countDown();
    doneLatch.await(5, TimeUnit.SECONDS);
    executor.shutdownNow();

    assertThat(successCount.get()).isEqualTo(2);

    var reservations = bookingService.getReservationsForEvent(eventId);
    assertThat(reservations).hasSize(2);
    assertThat(
      reservations
        .stream()
        .map(Reservation::getStatus)
        .collect(Collectors.toSet())
    ).containsOnly(ReservationStatus.HOLD);
  }

  @Test
  void sweeperShouldStartAndStopCleanly() {
    var sweeper = new HoldExpirySweeper(bookingService);
    sweeper.start();
    assertThat(sweeper).isNotNull();
    sweeper.stop();
  }
}
