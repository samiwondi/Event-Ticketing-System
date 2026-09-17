package com.example.demo.controller;

import com.example.demo.dto.HoldRequest;
import com.example.demo.dto.ReservationResponse;
import com.example.demo.service.BookingService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reservations")
public class ReservationController {

  private final BookingService bookingService;

  public ReservationController(BookingService bookingService) {
    this.bookingService = bookingService;
  }

  @PostMapping("/hold")
  public ResponseEntity<ReservationResponse> hold(
    @Valid @RequestBody HoldRequest req
  ) {
    var r = bookingService.hold(
      req.eventId(),
      req.customerEmail(),
      req.seatIds()
    );
    return ResponseEntity.created(
      URI.create("/api/reservations/" + r.getId())
    ).body(ReservationResponse.from(r));
  }

  @PostMapping("/{id}/confirm")
  public ResponseEntity<ReservationResponse> confirm(@PathVariable UUID id) {
    return ResponseEntity.ok(
      ReservationResponse.from(bookingService.confirm(id))
    );
  }

  @PostMapping("/{id}/cancel")
  public ResponseEntity<ReservationResponse> cancel(@PathVariable UUID id) {
    return ResponseEntity.ok(
      ReservationResponse.from(bookingService.cancel(id))
    );
  }
}
