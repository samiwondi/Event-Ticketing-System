package com.example.demo;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.demo.domain.Event;
import com.example.demo.domain.PricingRules;
import com.example.demo.domain.Seat;
import com.example.demo.domain.Venue;
import com.example.demo.dto.HoldRequest;
import com.example.demo.enums.Currency;
import com.example.demo.enums.EventStatus;
import com.example.demo.enums.SeatCategory;
import com.example.demo.repository.jpa.EventJpaRepository;
import com.example.demo.repository.jpa.SeatJpaRepository;
import com.example.demo.repository.jpa.VenueJpaRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class ReservationControllerIT extends AbstractIntegrationTest {

  @Autowired
  TestRestTemplate rest;

  @Autowired
  VenueJpaRepository venueRepo;

  @Autowired
  EventJpaRepository eventRepo;

  @Autowired
  SeatJpaRepository seatRepo;

  UUID eventId;
  UUID seat1Id;
  UUID seat2Id;

  @BeforeEach
  void setUp() {
    venueRepo.deleteAll();
    eventRepo.deleteAll();
    seatRepo.deleteAll();

    Venue venue = new Venue(
      UUID.randomUUID(),
      "Stadium",
      "Addis Ababa",
      "Africa/Addis_Ababa"
    );
    venueRepo.save(venue);

    Instant start = Instant.now().plus(2, ChronoUnit.DAYS);
    Instant end = start.plus(2, ChronoUnit.HOURS);
    PricingRules rules = new PricingRules(
      Currency.USD,
      BigDecimal.valueOf(50),
      Map.of(
        SeatCategory.VIP,
        BigDecimal.valueOf(100),
        SeatCategory.VVIP,
        BigDecimal.valueOf(150)
      ),
      Map.of("A", SeatCategory.VIP)
    );
    Event event = new Event(
      UUID.randomUUID(),
      venue.getId(),
      "Concert",
      start,
      end,
      EventStatus.SCHEDULED,
      Currency.USD,
      rules
    );
    eventRepo.save(event);
    eventId = event.getId();

    Seat s1 = new Seat(
      UUID.randomUUID(),
      venue.getId(),
      "A",
      "1",
      1,
      SeatCategory.STANDARD,
      Set.of()
    );
    Seat s2 = new Seat(
      UUID.randomUUID(),
      venue.getId(),
      "A",
      "1",
      2,
      SeatCategory.STANDARD,
      Set.of()
    );
    seatRepo.save(s1);
    seatRepo.save(s2);
    seat1Id = s1.getId();
    seat2Id = s2.getId();
  }

  @Test
  void holdSeats_thenConfirm_succeeds() {
    HoldRequest req = new HoldRequest(
      eventId,
      "alice@example.com",
      List.of(seat1Id, seat2Id)
    );
    ResponseEntity<Map> holdRes = rest.postForEntity(
      "/api/reservations/hold",
      req,
      Map.class
    );
    assertThat(holdRes.getStatusCode()).isEqualTo(HttpStatus.CREATED);

    Map<?, ?> body = holdRes.getBody();
    assertThat(body).isNotNull();
    assertThat(body.get("status")).isEqualTo("HOLD");
    assertThat(body.get("customerEmail")).isEqualTo("alice@example.com");

    UUID reservationId = UUID.fromString(body.get("id").toString());

    ResponseEntity<Map> confirmRes = rest.postForEntity(
      "/api/reservations/" + reservationId + "/confirm",
      null,
      Map.class
    );
    assertThat(confirmRes.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(confirmRes.getBody().get("status")).isEqualTo("CONFIRMED");
  }

  @Test
  void holdSameSeatTwice_returns409() {
    HoldRequest first = new HoldRequest(
      eventId,
      "alice@example.com",
      List.of(seat1Id)
    );
    ResponseEntity<Map> firstRes = rest.postForEntity(
      "/api/reservations/hold",
      first,
      Map.class
    );
    assertThat(firstRes.getStatusCode()).isEqualTo(HttpStatus.CREATED);

    HoldRequest second = new HoldRequest(
      eventId,
      "bob@example.com",
      List.of(seat1Id)
    );
    ResponseEntity<Map> secondRes = rest.postForEntity(
      "/api/reservations/hold",
      second,
      Map.class
    );
    assertThat(secondRes.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
  }

  @Test
  void confirmTwice_isIdempotent() {
    HoldRequest req = new HoldRequest(
      eventId,
      "alice@example.com",
      List.of(seat1Id)
    );
    ResponseEntity<Map> holdRes = rest.postForEntity(
      "/api/reservations/hold",
      req,
      Map.class
    );
    UUID reservationId = UUID.fromString(
      holdRes.getBody().get("id").toString()
    );

    ResponseEntity<Map> first = rest.postForEntity(
      "/api/reservations/" + reservationId + "/confirm",
      null,
      Map.class
    );
    ResponseEntity<Map> second = rest.postForEntity(
      "/api/reservations/" + reservationId + "/confirm",
      null,
      Map.class
    );

    assertThat(first.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(second.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(second.getBody().get("status")).isEqualTo("CONFIRMED");
  }

  @Test
  void cancelHold_setsStatusCancelled() {
    HoldRequest req = new HoldRequest(
      eventId,
      "alice@example.com",
      List.of(seat1Id)
    );
    ResponseEntity<Map> holdRes = rest.postForEntity(
      "/api/reservations/hold",
      req,
      Map.class
    );
    UUID reservationId = UUID.fromString(
      holdRes.getBody().get("id").toString()
    );

    ResponseEntity<Map> cancelRes = rest.postForEntity(
      "/api/reservations/" + reservationId + "/cancel",
      null,
      Map.class
    );

    assertThat(cancelRes.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(cancelRes.getBody().get("status")).isEqualTo("CANCELLED");
  }

  @Test
  void holdWithNoSeats_returns400() {
    HoldRequest req = new HoldRequest(eventId, "alice@example.com", List.of());
    ResponseEntity<Map> res = rest.postForEntity(
      "/api/reservations/hold",
      req,
      Map.class
    );
    assertThat(res.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
  }

  @Test
  void holdWithBadEmail_returns400() {
    HoldRequest req = new HoldRequest(
      eventId,
      "not-an-email",
      List.of(seat1Id)
    );
    ResponseEntity<Map> res = rest.postForEntity(
      "/api/reservations/hold",
      req,
      Map.class
    );
    assertThat(res.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
  }

  @Test
  void holdOnUnknownEvent_returns404() {
    HoldRequest req = new HoldRequest(
      UUID.randomUUID(),
      "alice@example.com",
      List.of(seat1Id)
    );
    ResponseEntity<Map> res = rest.postForEntity(
      "/api/reservations/hold",
      req,
      Map.class
    );
    assertThat(res.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
  }

  @Test
  void confirmUnknownReservation_returns404() {
    ResponseEntity<Map> res = rest.postForEntity(
      "/api/reservations/" + UUID.randomUUID() + "/confirm",
      null,
      Map.class
    );
    assertThat(res.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
  }

  @Test
  void availableSeats_excludesHeldSeats() {
    ResponseEntity<List> initial = rest.getForEntity(
      "/api/events/" + eventId + "/seats?onlyAvailable=true",
      List.class
    );
    assertThat(initial.getBody()).hasSize(2);

    HoldRequest req = new HoldRequest(
      eventId,
      "alice@example.com",
      List.of(seat1Id)
    );
    rest.postForEntity("/api/reservations/hold", req, Map.class);

    ResponseEntity<List> after = rest.getForEntity(
      "/api/events/" + eventId + "/seats?onlyAvailable=true",
      List.class
    );
    assertThat(after.getBody()).hasSize(1);

    ResponseEntity<List> all = rest.getForEntity(
      "/api/events/" + eventId + "/seats",
      List.class
    );
    assertThat(all.getBody()).hasSize(2);
  }

  @Test
  void holdComputesVipPriceFromSectionMapping() {
    HoldRequest req = new HoldRequest(
      eventId,
      "alice@example.com",
      List.of(seat1Id)
    );
    ResponseEntity<Map> res = rest.postForEntity(
      "/api/reservations/hold",
      req,
      Map.class
    );

    assertThat(res.getStatusCode()).isEqualTo(HttpStatus.CREATED);

    Map<?, ?> body = res.getBody();
    assertThat(body).isNotNull();
    assertThat(body.get("total").toString()).isEqualTo("100.00");
    assertThat(body.get("currency")).isEqualTo("USD");

    var seats = (List<?>) body.get("seats");
    assertThat(seats).hasSize(1);
  }
}
