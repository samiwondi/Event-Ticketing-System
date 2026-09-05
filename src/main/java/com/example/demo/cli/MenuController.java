package com.example.demo.cli;

import com.example.demo.domain.Reservation;
import com.example.demo.exception.ReservationException;
import com.example.demo.service.BookingService;
import com.example.demo.service.HoldExpirySweeper;
import com.example.demo.service.ReportService;
import com.example.demo.service.VenueService;

import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class MenuController {
    private final VenueService venueService;
    private final BookingService bookingService;
    private final ReportService reportService;
    private final HoldExpirySweeper sweeper;
    private final MenuRenderer renderer;
    private boolean running = true;

    public MenuController(VenueService venueService, BookingService bookingService,
                          ReportService reportService, HoldExpirySweeper sweeper,
                          MenuRenderer renderer) {
        this.venueService = venueService;
        this.bookingService = bookingService;
        this.reportService = reportService;
        this.sweeper = sweeper;
        this.renderer = renderer;
    }

    public void run() {
        while (running) {
            renderer.printMainMenu();
            int choice = renderer.readInt();
            switch (choice) {
                case 1 -> venueManagement();
                case 2 -> eventManagement();
                case 3 -> seatManagement();
                case 4 -> reservationManagement();
                case 5 -> reportsMenu();
                case 6 -> statusMenu();
                case 7 -> saveLoadMenu();
                case 8 -> {
                    running = false;
                    renderer.printMessage("Goodbye!");
                }
                default -> renderer.printError("Invalid option. Please try again.");
            }
        }
    }

    private void venueManagement() {
        boolean back = false;
        while (!back) {
            renderer.printVenueMenu();
            int choice = renderer.readInt();
            switch (choice) {
                case 1 -> {
                    var venues = venueService.listVenues();
                    renderer.printVenues(venues);
                    renderer.pressEnterToContinue();
                }
                case 2 -> {
                    renderer.printMessage("Enter venue name: ");
                    String name = renderer.readLine();
                    renderer.printMessage("Enter address: ");
                    String address = renderer.readLine();
                    renderer.printMessage("Enter timezone (e.g., America/New_York): ");
                    String timezone = renderer.readLine();
                    try {
                        var venue = venueService.createVenue(name, address, timezone);
                        renderer.printSuccess("Venue created: " + venue.getId());
                    } catch (Exception e) {
                        renderer.printError(e.getMessage());
                    }
                    renderer.pressEnterToContinue();
                }
                case 3 -> back = true;
                default -> renderer.printError("Invalid option.");
            }
        }
    }

    private void eventManagement() {
        boolean back = false;
        while (!back) {
            renderer.printEventMenu();
            int choice = renderer.readInt();
            switch (choice) {
                case 1 -> {
                    var events = venueService.listEvents();
                    renderer.printEvents(events);
                    renderer.pressEnterToContinue();
                }
                case 2 -> {
                    renderer.printMessage("Enter venue ID: ");
                    UUID venueId = renderer.readUUID();
                    if (venueId == null) {
                        renderer.printError("Invalid UUID format.");
                        renderer.pressEnterToContinue();
                        break;
                    }
                    renderer.printMessage("Enter event title: ");
                    String title = renderer.readLine();
                    renderer.printMessage("Enter start time (ISO-8601, e.g., 2026-09-05T20:00:00-04:00): ");
                    String startStr = renderer.readLine();
                    renderer.printMessage("Enter end time (ISO-8601): ");
                    String endStr = renderer.readLine();
                    try {
                        ZonedDateTime start = ZonedDateTime.parse(startStr);
                        ZonedDateTime end = ZonedDateTime.parse(endStr);
                        var event = venueService.createEvent(venueId, title, start, end);
                        renderer.printSuccess("Event created: " + event.getId());
                    } catch (Exception e) {
                        renderer.printError("Invalid date format: " + e.getMessage());
                    }
                    renderer.pressEnterToContinue();
                }
                case 3 -> {
                    renderer.printMessage("Enter event ID: ");
                    UUID eventId = renderer.readUUID();
                    if (eventId == null) {
                        renderer.printError("Invalid UUID.");
                        renderer.pressEnterToContinue();
                        break;
                    }
                    var events = venueService.listEvents();
                    events.stream()
                            .filter(e -> e.getId().equals(eventId))
                            .findFirst()
                            .ifPresentOrElse(
                                    e -> renderer.printReport("Event: " + e.getTitle() + "\nVenue: " + e.getVenueId() + "\nStart: " + e.getStartAt() + "\nEnd: " + e.getEndAt()),
                                    () -> renderer.printError("Event not found.")
                            );
                    renderer.pressEnterToContinue();
                }
                case 4 -> {
                    renderer.printMessage("Enter event ID: ");
                    UUID eventId = renderer.readUUID();
                    if (eventId == null) {
                        renderer.printError("Invalid UUID.");
                        renderer.pressEnterToContinue();
                        break;
                    }
                    var event = venueService.listEvents().stream()
                            .filter(e -> e.getId().equals(eventId))
                            .findFirst()
                            .orElse(null);
                    if (event == null) {
                        renderer.printError("Event not found.");
                        renderer.pressEnterToContinue();
                        break;
                    }
                    var seats = venueService.listSeatsByVenue(event.getVenueId());
                    renderer.printSeats(seats);
                    renderer.pressEnterToContinue();
                }
                case 5 -> back = true;
                default -> renderer.printError("Invalid option.");
            }
        }
    }

    private void seatManagement() {
        boolean back = false;
        while (!back) {
            renderer.printSeatMenu();
            int choice = renderer.readInt();
            switch (choice) {
                case 1 -> {
                    renderer.printMessage("Enter venue ID: ");
                    UUID venueId = renderer.readUUID();
                    if (venueId == null) {
                        renderer.printError("Invalid UUID.");
                        renderer.pressEnterToContinue();
                        break;
                    }
                    var seats = venueService.listSeatsByVenue(venueId);
                    renderer.printSeats(seats);
                    renderer.pressEnterToContinue();
                }
                case 2 -> {
                    renderer.printMessage("Enter venue ID: ");
                    UUID venueId = renderer.readUUID();
                    if (venueId == null) {
                        renderer.printError("Invalid UUID.");
                        renderer.pressEnterToContinue();
                        break;
                    }
                    renderer.printMessage("Enter section: ");
                    String section = renderer.readLine();
                    renderer.printMessage("Enter row: ");
                    String row = renderer.readLine();
                    renderer.printMessage("Enter seat number: ");
                    int number = renderer.readInt();
                    if (number <= 0) {
                        renderer.printError("Invalid seat number.");
                        renderer.pressEnterToContinue();
                        break;
                    }
                    renderer.printMessage("Enter attributes (optional, press Enter to skip): ");
                    String attributes = renderer.readLine();
                    if (attributes.isBlank()) attributes = null;
                    try {
                        var seat = venueService.createSeat(venueId, section, row, number, attributes);
                        renderer.printSuccess("Seat created: " + seat.getId());
                    } catch (Exception e) {
                        renderer.printError(e.getMessage());
                    }
                    renderer.pressEnterToContinue();
                }
                case 3 -> {
                    renderer.printMessage("CSV import not fully implemented. (Phase 1 placeholder)");
                    renderer.pressEnterToContinue();
                }
                case 4 -> back = true;
                default -> renderer.printError("Invalid option.");
            }
        }
    }

    private void reservationManagement() {
        boolean back = false;
        while (!back) {
            renderer.printReservationMenu();
            int choice = renderer.readInt();
            switch (choice) {
                case 1 -> {
                    renderer.printMessage("Enter event ID: ");
                    UUID eventId = renderer.readUUID();
                    if (eventId == null) {
                        renderer.printError("Invalid UUID.");
                        renderer.pressEnterToContinue();
                        break;
                    }
                    renderer.printMessage("Enter customer email: ");
                    String email = renderer.readLine();
                    renderer.printMessage("Enter seat IDs (comma-separated): ");
                    String seatIdsStr = renderer.readLine();
                    try {
                        List<UUID> seatIds = Arrays.stream(seatIdsStr.split(","))
                                .map(String::trim)
                                .map(UUID::fromString)
                                .collect(Collectors.toList());
                        var reservation = bookingService.holdSeats(eventId, email, seatIds);
                        renderer.printSuccess("Hold created! Reservation ID: " + reservation.getId());
                        renderer.printReservations(List.of(reservation));
                    } catch (Exception e) {
                        renderer.printError(e.getMessage());
                    }
                    renderer.pressEnterToContinue();
                }
                case 2 -> {
                    renderer.printMessage("Enter reservation ID to confirm: ");
                    UUID reservationId = renderer.readUUID();
                    if (reservationId == null) {
                        renderer.printError("Invalid UUID.");
                        renderer.pressEnterToContinue();
                        break;
                    }
                    try {
                        bookingService.confirmReservation(reservationId);
                        renderer.printSuccess("Reservation confirmed.");
                    } catch (Exception e) {
                        renderer.printError(e.getMessage());
                    }
                    renderer.pressEnterToContinue();
                }
                case 3 -> {
                    renderer.printMessage("Enter reservation ID to cancel: ");
                    UUID reservationId = renderer.readUUID();
                    if (reservationId == null) {
                        renderer.printError("Invalid UUID.");
                        renderer.pressEnterToContinue();
                        break;
                    }
                    try {
                        bookingService.cancelReservation(reservationId);
                        renderer.printSuccess("Reservation cancelled.");
                    } catch (Exception e) {
                        renderer.printError(e.getMessage());
                    }
                    renderer.pressEnterToContinue();
                }
                case 4 -> {
                    renderer.printMessage("Enter event ID: ");
                    UUID eventId = renderer.readUUID();
                    if (eventId == null) {
                        renderer.printError("Invalid UUID.");
                        renderer.pressEnterToContinue();
                        break;
                    }
                    var reservations = bookingService.getReservationsForEvent(eventId);
                    renderer.printReservations(reservations);
                    renderer.pressEnterToContinue();
                }
                case 5 -> {
                    var holds = bookingService.getAllHolds();
                    renderer.printReservations(holds);
                    renderer.pressEnterToContinue();
                }
                case 6 -> back = true;
                default -> renderer.printError("Invalid option.");
            }
        }
    }

    private void reportsMenu() {
        boolean back = false;
        while (!back) {
            renderer.printReportsMenu();
            int choice = renderer.readInt();
            switch (choice) {
                case 1 -> {
                    var map = reportService.seatsPerEvent();
                    if (map.isEmpty()) {
                        renderer.printMessage("No events found.");
                    } else {
                        map.forEach((id, count) -> renderer.printMessage("Event " + id + ": " + count + " seats"));
                    }
                    renderer.pressEnterToContinue();
                }
                case 2 -> {
                    var map = reportService.reservationsPerEvent();
                    if (map.isEmpty()) {
                        renderer.printMessage("No reservations found.");
                    } else {
                        map.forEach((id, statusCount) -> {
                            renderer.printMessage("Event " + id + ": " + statusCount);
                        });
                    }
                    renderer.pressEnterToContinue();
                }
                case 3 -> {
                    renderer.printMessage("Enter event ID: ");
                    UUID eventId = renderer.readUUID();
                    if (eventId == null) {
                        renderer.printError("Invalid UUID.");
                        renderer.pressEnterToContinue();
                        break;
                    }
                    String report = reportService.detailedSeatReport(eventId);
                    renderer.printReport(report);
                    renderer.pressEnterToContinue();
                }
                case 4 -> back = true;
                default -> renderer.printError("Invalid option.");
            }
        }
    }

    private void statusMenu() {
        boolean back = false;
        while (!back) {
            renderer.printStatusMenu();
            int choice = renderer.readInt();
            switch (choice) {
                case 1 -> {
                    boolean sweeperRunning = sweeper.isRunning();
                    int activeHolds = bookingService.getAllHolds().size();
                    int cacheSize = bookingService.getCacheSize();
                    int lockedEvents = bookingService.getLockedEventCount();
                    renderer.printStatus(sweeperRunning, activeHolds, cacheSize, lockedEvents);
                    renderer.pressEnterToContinue();
                }
                case 2 -> back = true;
                default -> renderer.printError("Invalid option.");
            }
        }
    }

    private void saveLoadMenu() {
        boolean back = false;
        while (!back) {
            renderer.printSaveLoadMenu();
            int choice = renderer.readInt();
            switch (choice) {
                case 1 -> {
                    renderer.printMessage("Saving state... (not fully implemented)");
                    renderer.pressEnterToContinue();
                }
                case 2 -> {
                    renderer.printMessage("Loading state... (not fully implemented)");
                    renderer.pressEnterToContinue();
                }
                case 3 -> back = true;
                default -> renderer.printError("Invalid option.");
            }
        }
    }
}