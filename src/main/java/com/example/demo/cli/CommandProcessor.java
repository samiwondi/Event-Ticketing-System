package com.example.demo.cli;

import com.example.demo.exception.ReservationException;
import com.example.demo.exception.ValidationException;
import com.example.demo.service.BookingService;
import com.example.demo.service.ReportService;
import com.example.demo.service.VenueService;
import com.example.demo.util.SeatComparator;

import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.UUID;
import java.util.stream.Collectors;

public class CommandProcessor {
    private final VenueService venueService;
    private final BookingService bookingService;
    private final ReportService reportService;
    private final ConsoleRenderer renderer;

    public CommandProcessor(VenueService venueService, BookingService bookingService,
                            ReportService reportService, ConsoleRenderer renderer) {
        this.venueService = venueService;
        this.bookingService = bookingService;
        this.reportService = reportService;
        this.renderer = renderer;
    }

    public boolean process(String input) {
        if (input == null || input.isBlank()) return true;
        String[] parts = input.trim().split("\\s+");
        if (parts.length == 0) return true;
        String command = parts[0].toLowerCase();

        try {
            switch (command) {
                case "help" -> renderer.printHelp();
                case "create" -> handleCreate(parts);
                case "list" -> handleList(parts);
                case "hold" -> handleHold(parts);
                case "confirm" -> handleConfirm(parts);
                case "cancel" -> handleCancel(parts);
                case "reports" -> handleReports(parts);
                case "report" -> handleReport(parts);
                case "save" -> handleSave(parts);
                case "load" -> handleLoad(parts);
                case "exit" -> { return false; }
                default -> renderer.printError("Unknown command. Type 'help'.");
            }
        } catch (Exception e) {
            renderer.printError(e.getMessage());
        }
        return true;
    }

    private void handleCreate(String[] parts) {
        if (parts.length < 2) {
            renderer.printError("Usage: create venue|event|seat ...");
            return;
        }
        String type = parts[1].toLowerCase();
        switch (type) {
            case "venue" -> {
                if (parts.length < 5) { renderer.printError("create venue <name> <address> <timezone>"); return; }
                String name = parts[2];
                String address = parts[3];
                String tz = parts[4];
                var venue = venueService.createVenue(name, address, tz);
                renderer.printSuccess("Venue created: " + venue.getId());
            }
            case "event" -> {
                if (parts.length < 6) { renderer.printError("create event <venueId> <title> <start> <end>"); return; }
                UUID venueId = UUID.fromString(parts[2]);
                String title = parts[3];
                ZonedDateTime start = ZonedDateTime.parse(parts[4]);
                ZonedDateTime end = ZonedDateTime.parse(parts[5]);
                var event = venueService.createEvent(venueId, title, start, end);
                renderer.printSuccess("Event created: " + event.getId());
            }
            case "seat" -> {
                if (parts.length < 6) { renderer.printError("create seat <venueId> <section> <row> <number> [attributes]"); return; }
                UUID venueId = UUID.fromString(parts[2]);
                String section = parts[3];
                String row = parts[4];
                int number = Integer.parseInt(parts[5]);
                String attributes = parts.length > 6 ? parts[6] : null;
                var seat = venueService.createSeat(venueId, section, row, number, attributes);
                renderer.printSuccess("Seat created: " + seat.getId());
            }
            default -> renderer.printError("Unknown create type.");
        }
    }

    private void handleList(String[] parts) {
        if (parts.length < 2) {
            renderer.printError("list venues|events|seats <venueId>");
            return;
        }
        String type = parts[1].toLowerCase();
        switch (type) {
            case "venues" -> renderer.printVenues(venueService.listVenues());
            case "events" -> renderer.printEvents(venueService.listEvents());
            case "seats" -> {
                if (parts.length < 3) { renderer.printError("list seats <venueId>"); return; }
                UUID venueId = UUID.fromString(parts[2]);
                renderer.printSeats(venueService.listSeatsByVenue(venueId));
            }
            default -> renderer.printError("Unknown list type.");
        }
    }

    private void handleHold(String[] parts) {
        if (parts.length < 4) {
            renderer.printError("hold <eventId> <email> <seatId1,seatId2,...>");
            return;
        }
        UUID eventId = UUID.fromString(parts[1]);
        String email = parts[2];
        var seatIds = Arrays.stream(parts[3].split(","))
                .map(UUID::fromString)
                .collect(Collectors.toList());
        var reservation = bookingService.holdSeats(eventId, email, seatIds);
        renderer.printReservation(reservation);
        renderer.printSuccess("Hold created. Reservation ID: " + reservation.getId());
    }

    private void handleConfirm(String[] parts) {
        if (parts.length < 2) { renderer.printError("confirm <reservationId>"); return; }
        UUID reservationId = UUID.fromString(parts[1]);
        bookingService.confirmReservation(reservationId);
        renderer.printSuccess("Reservation confirmed.");
    }

    private void handleCancel(String[] parts) {
        if (parts.length < 2) { renderer.printError("cancel <reservationId>"); return; }
        UUID reservationId = UUID.fromString(parts[1]);
        bookingService.cancelReservation(reservationId);
        renderer.printSuccess("Reservation cancelled.");
    }

    private void handleReports(String[] parts) {
        if (parts.length < 2) { renderer.printError("reports seats-per-event | reservations-per-event"); return; }
        String report = parts[1].toLowerCase();
        if ("seats-per-event".equals(report)) {
            var map = reportService.seatsPerEvent();
            map.forEach((id, count) -> renderer.printReport("Event " + id + ": " + count + " seats"));
        } else if ("reservations-per-event".equals(report)) {
            var map = reportService.reservationsPerEvent();
            map.forEach((id, statusCount) -> {
                renderer.printReport("Event " + id + ": " + statusCount);
            });
        } else {
            renderer.printError("Unknown report type.");
        }
    }

    private void handleReport(String[] parts) {
        if (parts.length < 3 || !"event".equals(parts[1])) {
            renderer.printError("report event <eventId>");
            return;
        }
        UUID eventId = UUID.fromString(parts[2]);
        String report = reportService.detailedSeatReport(eventId);
        renderer.printReport(report);
    }

    private void handleSave(String[] parts) {
        // The save/load is handled by the main app, so we just call the app's method.
        // To avoid circular dependency, we'll let the main app handle these commands.
        // For simplicity, we'll just print a message.
        renderer.printError("Save/load commands are handled by the main application.");
    }

    private void handleLoad(String[] parts) {
        renderer.printError("Save/load commands are handled by the main application.");
    }
}