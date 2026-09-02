package com.example.demo.cli;

import java.util.List;

import com.example.demo.domain.Event;
import com.example.demo.domain.Reservation;
import com.example.demo.domain.Seat;
import com.example.demo.domain.Venue;

public class ConsoleRenderer {
    public void printHelp() {
        System.out.println("""
                Available commands:
                  create venue <name> <address> <timezone>
                  create event <venueId> <title> <start> <end>   (dates in ISO-8601)
                  create seat <venueId> <section> <row> <number> [attributes]
                  list venues
                  list events
                  list seats <venueId>
                  hold <eventId> <email> <seatId1,seatId2,...>
                  confirm <reservationId>
                  cancel <reservationId>
                  reports seats-per-event
                  reports reservations-per-event
                  report event <eventId>
                  save [file]
                  load [file]
                  exit
                """);
    }

    public void printVenues(List<Venue> venues) {
        if (venues.isEmpty()) System.out.println("No venues.");
        else venues.forEach(v -> System.out.println(v.getId() + " | " + v.getName() + " | " + v.getAddress()));
    }

    public void printEvents(List<Event> events) {
        if (events.isEmpty()) System.out.println("No events.");
        else events.forEach(e -> System.out.println(e.getId() + " | " + e.getTitle() + " | " + e.getStartAt()));
    }

    public void printSeats(List<Seat> seats) {
        if (seats.isEmpty()) System.out.println("No seats.");
        else seats.forEach(s -> System.out.println(s.getId() + " | " + s.getSection() + "-" + s.getRow() + "-" + s.getNumber()));
    }

    public void printReservation(Reservation reservation) {
        System.out.println("Reservation: " + reservation.getId());
        System.out.println("  Event: " + reservation.getEventId());
        System.out.println("  Email: " + reservation.getCustomerEmail());
        System.out.println("  Status: " + reservation.getStatus());
        System.out.println("  Hold expires: " + reservation.getHoldExpiresAt());
        System.out.println("  Seats: " + reservation.getSeats().size());
    }

    public void printReport(String report) { System.out.println(report); }
    public void printError(String error) { System.err.println("Error: " + error); }
    public void printSuccess(String message) { System.out.println("Success: " + message); }
}