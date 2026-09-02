package com.example.demo.persistence;

import com.example.demo.domain.*;
import com.example.demo.exception.StorageException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

class JsonStorageTest {

    @Test
    void shouldRoundTrip(@TempDir Path tempDir) throws Exception {
        var file = tempDir.resolve("test.json").toString();
        var storage = new JsonStorage(file);

        // Build test data
        var venue = new Venue(UUID.randomUUID(), "Venue", "Addr", ZoneId.of("UTC"));
        var event = new Event(UUID.randomUUID(), venue.getId(), "Event",
                ZonedDateTime.now(), ZonedDateTime.now().plusHours(1), "SCHEDULED");
        var seat = new Seat(UUID.randomUUID(), venue.getId(), "A", "1", 1, null);
        var reservation = new Reservation(
                UUID.randomUUID(),
                event.getId(),
                "test@example.com",
                ReservationStatus.HOLD,
                Instant.now(),
                null,
                Instant.now().plusSeconds(300),
                List.of(new ReservationSeat(UUID.randomUUID(), seat.getId(), Money.zero("USD"), DiscountType.NONE))
        );

        // Save
        storage.save(List.of(venue), List.of(event), List.of(seat), List.of(reservation));

        // Load back
        var ctx = storage.load();

        // Assert
        assertThat(ctx.venues).hasSize(1);
        assertThat(ctx.events).hasSize(1);
        assertThat(ctx.seats).hasSize(1);
        assertThat(ctx.reservations).hasSize(1);

        var loadedRes = ctx.reservations.get(0);
        assertThat(loadedRes.getEventId()).isEqualTo(event.getId());
        assertThat(loadedRes.getStatus()).isEqualTo(ReservationStatus.HOLD);
    }

    // ---------- NEW: Load valid file from resources ----------
    @Test
    void loadValidFile_shouldParseCorrectly(@TempDir Path tempDir) throws IOException {
        // Copy the valid-state.json from classpath to a temp file
        var resourcePath = Path.of("src/test/resources/test-data/valid-state.json");
        var targetPath = tempDir.resolve("data.json");

        // If running from JAR, use getResourceAsStream. For maven test, this works.
        try (var in = getClass().getResourceAsStream("/test-data/valid-state.json")) {
            assertThat(in).as("Resource file not found").isNotNull();
            Files.copy(in, targetPath);
        }

        var storage = new JsonStorage(targetPath.toString());
        var ctx = storage.load();

        // Validate loaded data
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

    // ---------- NEW: Load corrupted file should throw StorageException ----------
    @Test
    void loadCorruptedFile_shouldThrowStorageException(@TempDir Path tempDir) throws IOException {
        // Copy the corrupted.json from classpath to a temp file
        var targetPath = tempDir.resolve("corrupt.json");
        try (var in = getClass().getResourceAsStream("/test-data/corrupted.json")) {
            assertThat(in).as("Resource file not found").isNotNull();
            Files.copy(in, targetPath);
        }

        var storage = new JsonStorage(targetPath.toString());

        // When we load, a StorageException should be thrown (wrapping the Jackson parse error)
        assertThatThrownBy(storage::load)
                .isInstanceOf(StorageException.class)
                .hasMessageContaining("Failed to load or parse JSON file");
    }

    // ---------- Load non-existent file should return empty state ----------
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