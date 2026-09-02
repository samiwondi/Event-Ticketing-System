package com.example.demo.repository.memory;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import com.example.demo.domain.Event;
import com.example.demo.repository.EventRepository;

public class InMemoryEventRepository implements EventRepository {
    private final Map<UUID, Event> store = new ConcurrentHashMap<>();

    @Override
    public Event save(Event event) {
        store.put(event.getId(), event);
        return event;
    }

    @Override
    public Optional<Event> findById(UUID id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public List<Event> findAll() {                     // <-- Only ONE definition
        return new ArrayList<>(store.values());
    }

    @Override
    public List<Event> findByVenueId(UUID venueId) {
        return store.values().stream()
                .filter(e -> e.getVenueId().equals(venueId))
                .collect(Collectors.toList());
    }

    @Override
    public List<Event> findBetweenDates(ZonedDateTime from, ZonedDateTime to) {
        return store.values().stream()
                .filter(e -> e.getStartAt().isAfter(from) && e.getEndAt().isBefore(to))
                .collect(Collectors.toList());
    }

    @Override
    public void deleteById(UUID id) {
        store.remove(id);
    }

    @Override
    public boolean existsById(UUID id) {
        return store.containsKey(id);
    }
}