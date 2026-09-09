package com.example.demo.cli;

import com.example.demo.domain.Event;
import com.example.demo.domain.PricingRules;
import com.example.demo.domain.Reservation;
import com.example.demo.domain.ReservationSeat;
import com.example.demo.domain.Seat;
import com.example.demo.domain.Venue;
import java.util.List;
import java.util.Scanner;
import java.util.UUID;

public class MenuRenderer {

  private final Scanner scanner = new Scanner(System.in);

  public void printMainMenu() {
    System.out.println("\n========== Ticketing System ==========");
    System.out.println("1. Venue Management");
    System.out.println("2. Event Management");
    System.out.println("3. Seat Management");
    System.out.println("4. Reservation Management");
    System.out.println("5. Reports");
    System.out.println("6. Save / Load");
    System.out.println("7. System Status");
    System.out.println("8. Exit");
    System.out.println("=======================================");
    System.out.print("Select option: ");
  }

  public void printVenueMenu() {
    System.out.println("\n========== Venue Management ==========");
    System.out.println("1. List all venues");
    System.out.println("2. Create new venue");
    System.out.println("3. Back to main menu");
    System.out.println("=======================================");
    System.out.print("Select option: ");
  }

  public void printEventMenu() {
    System.out.println("\n========== Event Management ==========");
    System.out.println("1. List all events");
    System.out.println("2. Create new event");
    System.out.println("3. View event details");
    System.out.println("4. List seats for event");
    System.out.println("5. Back to main menu");
    System.out.println("=======================================");
    System.out.print("Select option: ");
  }

  public void printSeatMenu() {
    System.out.println("\n========== Seat Management ==========");
    System.out.println("1. List seats for venue");
    System.out.println("2. Create new seat");
    System.out.println("3. Bulk import seats from CSV (placeholder)");
    System.out.println("4. Back to main menu");
    System.out.println("=======================================");
    System.out.print("Select option: ");
  }

  public void printReservationMenu() {
    System.out.println("\n========== Reservation Management ==========");
    System.out.println("1. Hold seats");
    System.out.println("2. Confirm reservation");
    System.out.println("3. Cancel reservation");
    System.out.println("4. View reservations for event");
    System.out.println("5. View all active holds");
    System.out.println("6. Back to main menu");
    System.out.println("=============================================");
    System.out.print("Select option: ");
  }

  public void printReportsMenu() {
    System.out.println("\n========== Reports ==========");
    System.out.println("1. Seats per event");
    System.out.println("2. Reservations per event");
    System.out.println("3. Detailed seat report for event");
    System.out.println("4. Back to main menu");
    System.out.println("==============================");
    System.out.print("Select option: ");
  }

  public void printSaveLoadMenu() {
    System.out.println("\n========== Save / Load ==========");
    System.out.println("1. Save state to JSON");
    System.out.println("2. Load state from JSON");
    System.out.println("3. Back to main menu");
    System.out.println("==================================");
    System.out.print("Select option: ");
  }

  public void printStatusMenu() {
    System.out.println("\n========== System Status ==========");
    System.out.println("1. Show full status");
    System.out.println("2. Back to main menu");
    System.out.println("====================================");
    System.out.print("Select option: ");
  }

  public String readLine() {
    return scanner.nextLine().trim();
  }

  public int readInt() {
    try {
      return Integer.parseInt(scanner.nextLine().trim());
    } catch (NumberFormatException e) {
      return -1;
    }
  }

  public UUID readUUID() {
    String input = scanner.nextLine().trim();
    if (input.isEmpty()) return null;
    try {
      return UUID.fromString(input);
    } catch (IllegalArgumentException e) {
      return null;
    }
  }

  // --- Print lists ---

  public void printVenues(List<Venue> venues) {
    if (venues.isEmpty()) {
      System.out.println("No venues found.");
      return;
    }
    int index = 1;
    System.out.println("\n========== Venues ==========");
    for (Venue v : venues) {
      System.out.printf(
        "%d | Name: %s | Address: %s%n",
        index++,
        v.getName(),
        v.getAddress()
      );
    }
    System.out.println("============================");
  }

  public void printEvents(List<Event> events) {
    if (events.isEmpty()) {
      System.out.println("No events found.");
      return;
    }
    System.out.println("\n========== Events ==========");
    for (Event e : events) {
      System.out.printf(
        "Title: %s | Start: %s%n",
        e.getTitle(),
        e.getStartAt()
      );
    }
    System.out.println("============================");
  }

  public void printSeats(List<Seat> seats) {
    if (seats.isEmpty()) {
      System.out.println("No seats found.");
      return;
    }
    System.out.println("\n========== Seats ==========");
    for (Seat s : seats) {
      System.out.printf(
        "ID: %s | Section: %s | Row: %s | Number: %d | Category: %s%n",
        s.getId(),
        s.getSection(),
        s.getRow(),
        s.getNumber(),
        s.getCategory()
      );
    }
    System.out.println("============================");
  }

  public void printReservations(List<Reservation> reservations) {
    if (reservations.isEmpty()) {
      System.out.println("No reservations found.");
      return;
    }
    System.out.println("\n========== Reservations ==========");
    for (Reservation r : reservations) {
      System.out.printf(
        "ID: %s | Event: %s | Email: %s | Status: %s | Seats: %d%n",
        r.getId(),
        r.getEventId(),
        r.getCustomerEmail(),
        r.getStatus(),
        r.getSeats().size()
      );
    }
    System.out.println("==================================");
  }

  // --- Print detailed objects ---

  public void printEventDetails(Event event) {
    System.out.println("\n========== Event Details ==========");
    System.out.println("ID: " + event.getId());
    System.out.println("Venue ID: " + event.getVenueId());
    System.out.println("Title: " + event.getTitle());
    System.out.println("Start: " + event.getStartAt());
    System.out.println("End: " + event.getEndAt());
    System.out.println("Status: " + event.getStatus());
    PricingRules rules = event.getPricingRules();
    System.out.println("Currency: " + rules.currency());
    System.out.println("Default price: " + rules.defaultPrice());
    System.out.println("Category prices: " + rules.categoryPrices());
    System.out.println("====================================");
  }

  public void printSeatDetails(Seat seat) {
    System.out.println("\n========== Seat Details ==========");
    System.out.println("ID: " + seat.getId());
    System.out.println("Venue ID: " + seat.getVenueId());
    System.out.println("Section: " + seat.getSection());
    System.out.println("Row: " + seat.getRow());
    System.out.println("Number: " + seat.getNumber());
    System.out.println("Category: " + seat.getCategory());
    System.out.println("Attributes: " + seat.getAttributes());
    System.out.println("==================================");
  }

  public void printReservationDetails(Reservation reservation) {
    System.out.println("\n========== Reservation Details ==========");
    System.out.println("ID: " + reservation.getId());
    System.out.println("Event ID: " + reservation.getEventId());
    System.out.println("Customer Email: " + reservation.getCustomerEmail());
    System.out.println("Status: " + reservation.getStatus());
    System.out.println("Created At: " + reservation.getCreatedAt());
    System.out.println(
      "Confirmed At: " + reservation.getConfirmedAt().orElse(null)
    );
    System.out.println("Hold Expires At: " + reservation.getHoldExpiresAt());
    System.out.println("Seats (" + reservation.getSeats().size() + "):");
    for (ReservationSeat rs : reservation.getSeats()) {
      System.out.printf(
        "  - Seat ID: %s | Price: %s | Discount: %s%n",
        rs.seatId(),
        rs.price(),
        rs.discount()
      );
    }
    System.out.println("===========================================");
  }

  // --- Utilities ---

  public void printStatus(
    boolean sweeperRunning,
    int activeHolds,
    int cacheSize,
    int lockedEvents
  ) {
    System.out.println("\n========== System Status ==========");
    System.out.println("Sweeper running: " + (sweeperRunning ? "YES" : "NO"));
    System.out.println("Active HOLD reservations: " + activeHolds);
    System.out.println("Seat availability cache size: " + cacheSize);
    System.out.println("Event locks currently held: " + lockedEvents);
    System.out.println("====================================");
  }

  public void printMessage(String message) {
    System.out.println(message);
  }

  public void printError(String error) {
    System.err.println("Error: " + error);
  }

  public void printSuccess(String success) {
    System.out.println("Success: " + success);
  }

  public void printReport(String report) {
    System.out.println(report);
  }

  public void pressEnterToContinue() {
    System.out.print("Press Enter to continue...");
    scanner.nextLine();
  }
}
