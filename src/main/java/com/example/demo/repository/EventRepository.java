package com.example.demo.repository;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.example.demo.domain.Event;

public interface EventRepository {
    Event save(Event event);
    Optional<Event> findById(UUID id);
    List<Event> findAll();      // <-- ADD if missing
    List<Event> findByVenueId(UUID venueId);
    List<Event> findBetweenDates(ZonedDateTime from, ZonedDateTime to);
    void deleteById(UUID id);
    boolean existsById(UUID id);
}