package com.example.demo.repository;

import com.example.demo.domain.Venue;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface VenueRepository {
    Venue save(Venue venue);
    Optional<Venue> findById(UUID id);
    List<Venue> findAll();
    void deleteById(UUID id);
    boolean existsById(UUID id);
}