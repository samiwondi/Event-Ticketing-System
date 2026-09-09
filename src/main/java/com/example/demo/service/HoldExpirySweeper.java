package com.example.demo.service;

import com.example.demo.domain.Reservation;
import java.time.Instant;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class HoldExpirySweeper {

  private final BookingService bookingService;
  private final ScheduledExecutorService scheduler =
    Executors.newSingleThreadScheduledExecutor();
  private final AtomicBoolean running = new AtomicBoolean(false);

  public HoldExpirySweeper(BookingService bookingService) {
    this.bookingService = bookingService;
  }

  public void start() {
    if (running.compareAndSet(false, true)) {
      scheduler.scheduleAtFixedRate(this::sweep, 0, 2, TimeUnit.SECONDS);
      System.out.println("Hold expiry sweeper started.");
    }
  }

  public void stop() {
    if (running.compareAndSet(true, false)) {
      scheduler.shutdown();
      try {
        if (!scheduler.awaitTermination(2, TimeUnit.SECONDS)) {
          scheduler.shutdownNow();
        }
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
      System.out.println("Hold expiry sweeper stopped.");
    }
  }

  public boolean isRunning() {
    return running.get();
  }

  private void sweep() {
    try {
      var holds = bookingService.getAllHolds();
      Instant now = Instant.now();
      for (Reservation res : holds) {
        if (res.getHoldExpiresAt().isBefore(now)) {
          bookingService.expireReservation(res.getId());
          System.out.println("Expired hold cancelled: " + res.getId());
        }
      }
    } catch (Exception e) {
      System.err.println("Error during sweep: " + e.getMessage());
    }
  }
}
