package com.example.demo.repository;

import com.example.demo.domain.Reservation;
import com.example.demo.enums.ReservationStatus;
import java.util.List;
import java.util.Optional; // fixed import
import java.util.UUID;

public interface ReservationRepository {
  Reservation save(Reservation reservation);
  Optional<Reservation> findById(UUID id);
  List<Reservation> findByEventId(UUID eventId);
  List<Reservation> findByCustomerEmail(String email);
  List<Reservation> findByEventIdAndStatus(
    UUID eventId,
    ReservationStatus status
  );
  List<Reservation> findAll();
  void deleteById(UUID id);
  boolean existsById(UUID id);
}
