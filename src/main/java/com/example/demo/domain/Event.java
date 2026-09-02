package com.example.demo.domain;

import java.time.ZonedDateTime;
import java.util.Objects;
import java.util.UUID;

public class Event {
    private final UUID id;
    private final UUID venueId;
    private final String title;
    private final ZonedDateTime startAt;
    private final ZonedDateTime endAt;
    private final String status; // e.g., "SCHEDULED", "CANCELLED"

    public Event(UUID id, UUID venueId, String title, ZonedDateTime startAt, ZonedDateTime endAt, String status) {
        this.id = Objects.requireNonNull(id);
        this.venueId = Objects.requireNonNull(venueId);
        this.title = Objects.requireNonNull(title);
        this.startAt = Objects.requireNonNull(startAt);
        this.endAt = Objects.requireNonNull(endAt);
        this.status = status != null ? status : "SCHEDULED";
        if (startAt.isAfter(endAt)) {
            throw new IllegalArgumentException("Start must be before end");
        }
    }

    // Getters
    public UUID getId() { return id; }
    public UUID getVenueId() { return venueId; }
    public String getTitle() { return title; }
    public ZonedDateTime getStartAt() { return startAt; }
    public ZonedDateTime getEndAt() { return endAt; }
    public String getStatus() { return status; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Event event)) return false;
        return Objects.equals(id, event.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Event{id=" + id + ", title='" + title + "'}";
    }
}