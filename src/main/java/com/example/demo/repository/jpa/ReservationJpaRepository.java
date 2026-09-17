package com.example.demo.repository.jpa;

import com.example.demo.domain.Reservation;
import com.example.demo.enums.ReservationStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReservationJpaRepository
  extends JpaRepository<Reservation, UUID>
{
  List<Reservation> findByEventId(UUID eventId);
  List<Reservation> findByCustomerEmailIgnoreCase(String email);
  List<Reservation> findByStatusAndHoldExpiresAtBefore(
    ReservationStatus status,
    Instant cutoff
  );
}
