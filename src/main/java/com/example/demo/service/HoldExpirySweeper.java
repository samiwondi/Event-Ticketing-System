package com.example.demo.service;

import com.example.demo.domain.Reservation;
import com.example.demo.enums.ReservationStatus;
import com.example.demo.repository.jpa.ReservationJpaRepository;
import java.time.Instant;
import java.util.List;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Profile("!cli")
public class HoldExpirySweeper {

  private final ReservationJpaRepository reservationRepo;
  private final BookingService bookingService;

  public HoldExpirySweeper(
    ReservationJpaRepository reservationRepo,
    BookingService bookingService
  ) {
    this.reservationRepo = reservationRepo;
    this.bookingService = bookingService;
  }

  @Scheduled(fixedRate = 5000)
  @Transactional
  public void sweep() {
    try {
      List<Reservation> expired =
        reservationRepo.findByStatusAndHoldExpiresAtBefore(
          ReservationStatus.HOLD,
          Instant.now()
        );
      for (Reservation r : expired) {
        bookingService.expire(r.getId());
        System.out.println("Expired hold cancelled: " + r.getId());
      }
    } catch (Exception e) {
      System.err.println("Error during sweep: " + e.getMessage());
    }
  }
}
