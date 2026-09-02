package com.example.demo.domain;

import java.time.ZoneId;
import java.util.Objects;
import java.util.UUID;

public class Venue {
    private final UUID id;
    private final String name;
    private final String address;
    private final ZoneId timezone;

    public Venue(UUID id, String name, String address, ZoneId timezone) {
        this.id = Objects.requireNonNull(id);
        this.name = Objects.requireNonNull(name);
        this.address = Objects.requireNonNull(address);
        this.timezone = Objects.requireNonNull(timezone);
    }

    // Getters
    public UUID getId() { return id; }
    public String getName() { return name; }
    public String getAddress() { return address; }
    public ZoneId getTimezone() { return timezone; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Venue venue)) return false;
        return Objects.equals(id, venue.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Venue{id=" + id + ", name='" + name + "'}";
    }
}