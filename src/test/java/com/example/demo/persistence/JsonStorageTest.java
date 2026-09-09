package com.example.demo.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.demo.domain.Event;
import com.example.demo.domain.Money;
import com.example.demo.domain.PricingRules;
import com.example.demo.domain.Reservation;
import com.example.demo.domain.ReservationSeat;
import com.example.demo.domain.Seat;
import com.example.demo.domain.Venue;
import com.example.demo.enums.Currency;
import com.example.demo.enums.DiscountType;
import com.example.demo.enums.EventStatus;
import com.example.demo.enums.ReservationStatus;
import com.example.demo.enums.SeatAttribute;
import com.example.demo.enums.SeatCategory;
import com.example.demo.exception.StorageException;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class JsonStorageTest {

  @Test
  void shouldRoundTrip(@TempDir Path tempDir) throws Exception {
    var file = tempDir.resolve("test.json").toString();
    var storage = new JsonStorage(file);

    Currency currency = Currency.USD;
    var venue = new Venue(UUID.randomUUID(), "Venue", "Addr", ZoneId.of("UTC"));
    PricingRules rules = PricingRules.defaultRules(currency);
    var event = new Event(
      UUID.randomUUID(),
      venue.getId(),
      "Event",
      ZonedDateTime.now(),
      ZonedDateTime.now().plusHours(1),
      EventStatus.SCHEDULED,
      currency,
      rules
    );
    Set<SeatAttribute> emptyAttrs = Collections.emptySet();
    var seat = new Seat(
      UUID.randomUUID(),
      venue.getId(),
      "A",
      "1",
      1,
      SeatCategory.STANDARD,
      emptyAttrs
    );
    var reservation = new Reservation(
      UUID.randomUUID(),
      event.getId(),
      "test@example.com",
      ReservationStatus.HOLD,
      Instant.now(),
      null,
      Instant.now().plusSeconds(300),
      List.of(
        new ReservationSeat(
          UUID.randomUUID(),
          seat.getId(),
          new Money(BigDecimal.ZERO, currency),
          DiscountType.NONE
        )
      )
    );

    storage.save(
      List.of(venue),
      List.of(event),
      List.of(seat),
      List.of(reservation)
    );
    var ctx = storage.load();

    assertThat(ctx.venues).hasSize(1);
    assertThat(ctx.events).hasSize(1);
    assertThat(ctx.seats).hasSize(1);
    assertThat(ctx.reservations).hasSize(1);

    var loadedRes = ctx.reservations.get(0);
    assertThat(loadedRes.getEventId()).isEqualTo(event.getId());
    assertThat(loadedRes.getStatus()).isEqualTo(ReservationStatus.HOLD);
  }

  @Test
  void loadValidFile_shouldParseCorrectly(@TempDir Path tempDir)
    throws IOException {
    var targetPath = tempDir.resolve("data.json");
    try (
      var in = getClass().getResourceAsStream("/test-data/valid-state.json")
    ) {
      assertThat(in).as("Resource file not found").isNotNull();
      Files.copy(in, targetPath);
    }

    var storage = new JsonStorage(targetPath.toString());
    var ctx = storage.load();

    assertThat(ctx.venues).hasSize(1);
    assertThat(ctx.events).hasSize(1);
    assertThat(ctx.seats).hasSize(2);
    assertThat(ctx.reservations).hasSize(1);

    var venue = ctx.venues.get(0);
    assertThat(venue.getName()).isEqualTo("Main Hall");

    var event = ctx.events.get(0);
    assertThat(event.getTitle()).isEqualTo("Rock Concert");

    var res = ctx.reservations.get(0);
    assertThat(res.getCustomerEmail()).isEqualTo("customer@example.com");
    assertThat(res.getStatus()).isEqualTo(ReservationStatus.HOLD);
    assertThat(res.getSeats()).hasSize(1);
  }

  @Test
  void loadCorruptedFile_shouldThrowStorageException(@TempDir Path tempDir)
    throws IOException {
    var targetPath = tempDir.resolve("corrupt.json");
    try (var in = getClass().getResourceAsStream("/test-data/corrupted.json")) {
      assertThat(in).as("Resource file not found").isNotNull();
      Files.copy(in, targetPath);
    }

    var storage = new JsonStorage(targetPath.toString());
    assertThatThrownBy(storage::load)
      .isInstanceOf(StorageException.class)
      .hasMessageContaining("Failed to load or parse JSON file");
  }

  @Test
  void loadNonExistentFile_shouldReturnEmpty(@TempDir Path tempDir) {
    var file = tempDir.resolve("does-not-exist.json").toString();
    var storage = new JsonStorage(file);
    var ctx = storage.load();
    assertThat(ctx.venues).isEmpty();
    assertThat(ctx.events).isEmpty();
    assertThat(ctx.seats).isEmpty();
    assertThat(ctx.reservations).isEmpty();
  }
}
