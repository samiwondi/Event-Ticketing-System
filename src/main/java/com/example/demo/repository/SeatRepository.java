package com.example.demo.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.example.demo.domain.Seat;

public interface SeatRepository {
    Seat save(Seat seat);
    Optional<Seat> findById(UUID id);
    List<Seat> findByVenueId(UUID venueId);
    List<Seat> findByVenueIdAndSection(UUID venueId, String section);
    void deleteById(UUID id);
    boolean existsById(UUID id);
    List<Seat> findAll();   // <-- ADD THIS
}