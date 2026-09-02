package com.example.demo.repository;

import java.util.List;
import java.util.Optional;   // <-- ADD import
import java.util.UUID;

import com.example.demo.domain.Reservation;
import com.example.demo.domain.ReservationStatus;

public interface ReservationRepository {
    Reservation save(Reservation reservation);
    Optional<Reservation> findById(UUID id);
    List<Reservation> findByEventId(UUID eventId);
    List<Reservation> findByCustomerEmail(String email);
    List<Reservation> findByEventIdAndStatus(UUID eventId, ReservationStatus status);
    void deleteById(UUID id);
    boolean existsById(UUID id);
    List<Reservation> findAll();    // <-- ADD this method
}