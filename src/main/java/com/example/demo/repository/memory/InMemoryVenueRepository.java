package com.example.demo.repository.memory;

import com.example.demo.domain.Venue;
import com.example.demo.repository.VenueRepository;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryVenueRepository implements VenueRepository {
    private final Map<UUID, Venue> store = new ConcurrentHashMap<>();

    @Override
    public Venue save(Venue venue) {
        store.put(venue.getId(), venue);
        return venue;
    }

    @Override
    public Optional<Venue> findById(UUID id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public List<Venue> findAll() {
        return new ArrayList<>(store.values());
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