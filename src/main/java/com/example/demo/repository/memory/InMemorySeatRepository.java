package com.example.demo.repository.memory;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import com.example.demo.domain.Seat;
import com.example.demo.repository.SeatRepository;

public class InMemorySeatRepository implements SeatRepository {
    private final Map<UUID, Seat> store = new ConcurrentHashMap<>();

    @Override
    public Seat save(Seat seat) {
        store.put(seat.getId(), seat);
        return seat;
    }

    @Override
    public Optional<Seat> findById(UUID id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public List<Seat> findByVenueId(UUID venueId) {
        return store.values().stream()
                .filter(s -> s.getVenueId().equals(venueId))
                .collect(Collectors.toList());
    }

    @Override
    public List<Seat> findByVenueIdAndSection(UUID venueId, String section) {
        return store.values().stream()
                .filter(s -> s.getVenueId().equals(venueId) && s.getSection().equals(section))
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