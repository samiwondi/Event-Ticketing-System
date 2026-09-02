package com.example.demo.domain;

import java.util.Objects;
import java.util.UUID;

public class Seat {
    private final UUID id;
    private final UUID venueId;
    private final String section;
    private final String row;
    private final int number;
    private final String attributes;

    public Seat(UUID id, UUID venueId, String section, String row, int number, String attributes) {
        this.id = Objects.requireNonNull(id);
        this.venueId = Objects.requireNonNull(venueId);
        this.section = Objects.requireNonNull(section);
        this.row = Objects.requireNonNull(row);
        this.number = number;
        this.attributes = attributes;
    }

    public UUID getId() { return id; }
    public UUID getVenueId() { return venueId; }
    public String getSection() { return section; }
    public String getRow() { return row; }
    public int getNumber() { return number; }
    public String getAttributes() { return attributes; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Seat seat)) return false;
        return Objects.equals(id, seat.id);
    }

    @Override
    public int hashCode() { return Objects.hash(id); }

    @Override
    public String toString() {
        return "Seat{id=" + id + ", section='" + section + "', row='" + row + "', number=" + number + "}";
    }
}