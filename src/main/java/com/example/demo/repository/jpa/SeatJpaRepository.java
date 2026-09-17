package com.example.demo.repository.jpa;

import com.example.demo.domain.Seat;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SeatJpaRepository extends JpaRepository<Seat, UUID> {
  List<Seat> findByVenueIdOrderBySectionAscRowAscNumberAsc(UUID venueId);
}
