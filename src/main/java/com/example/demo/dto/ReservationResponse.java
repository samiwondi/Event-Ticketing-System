package com.example.demo.dto;

import com.example.demo.domain.Reservation;
import com.example.demo.domain.ReservationSeat;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ReservationResponse(
  UUID id,
  UUID eventId,
  String customerEmail,
  String status,
  Instant holdExpiresAt,
  Instant confirmedAt,
  BigDecimal total,
  String currency,
  List<SeatLine> seats
) {
  public record SeatLine(UUID seatId, BigDecimal amount, String currency) {}

  public static ReservationResponse from(Reservation r) {
    BigDecimal total = BigDecimal.ZERO;
    String cur = null;
    List<SeatLine> lines = new java.util.ArrayList<>();
    for (ReservationSeat rs : r.getSeats()) {
      total = total.add(rs.getPrice().getAmount());
      if (cur == null) cur = rs.getPrice().getCurrency().name();
      lines.add(
        new SeatLine(
          rs.getSeatId(),
          rs.getPrice().getAmount(),
          rs.getPrice().getCurrency().name()
        )
      );
    }
    return new ReservationResponse(
      r.getId(),
      r.getEventId(),
      r.getCustomerEmail(),
      r.getStatus().name(),
      r.getHoldExpiresAt(),
      r.getConfirmedAt(),
      total,
      cur,
      lines
    );
  }
}
