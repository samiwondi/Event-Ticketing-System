package com.example.demo.repository.jpa;

import com.example.demo.domain.ReservationSeat;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReservationSeatJpaRepository
  extends JpaRepository<ReservationSeat, UUID> {}
