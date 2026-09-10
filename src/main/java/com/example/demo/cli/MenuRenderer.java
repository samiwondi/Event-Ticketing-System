package com.example.demo.cli;

import com.example.demo.domain.Event;
import com.example.demo.domain.Reservation;
import com.example.demo.domain.Seat;
import com.example.demo.domain.Venue;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Scanner;
import java.util.UUID;

public class MenuRenderer {

  private final Scanner scanner = new Scanner(System.in);

  public void printMainMenu() {
    System.out.println("\n========== Ticketing System ==========");
    System.out.println("1. Venue Management");
    System.out.println("2. Event Management");
    System.out.println("3. Reservation Management");
    System.out.println("4. Reports");
    System.out.println("5. System Status");
    System.out.println("6. Exit");
    System.out.println("=======================================");
    System.out.print("Select option: ");
  }

  public void printVenueMenu() {
    System.out.println("\n========== Venue Management ==========");
    System.out.println("1. List all venues");
    System.out.println("2. Create new venue");
    System.out.println("3. Update venue");
    System.out.println("4. Delete venue");
    System.out.println("5. Back to main menu");
    System.out.println("=======================================");
    System.out.print("Select option: ");
  }

  public void printEventMenu() {
    System.out.println("\n========== Event Management ==========");
    System.out.println("1. List all events");
    System.out.println("2. Create new event");
    System.out.println("3. View event details");
    System.out.println("4. List seats for event");
    System.out.println("5. Update event");
    System.out.println("6. Delete event");
    System.out.println("7. Back to main menu");
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

  public void printStatusMenu() {
    System.out.println("\n========== System Status ==========");
    System.out.println("1. Show full status");
    System.out.println("2. Back to main menu");
    System.out.println("====================================");
    System.out.print("Select option: ");
  }

  // --------------------------------------------------------------
  //  INPUT HELPERS
  // --------------------------------------------------------------
  public String readLine() {
    return scanner.nextLine().trim();
  }

  public int readInt(String prompt) {
    while (true) {
      System.out.print(prompt);
      String input = scanner.nextLine().trim();
      try {
        return Integer.parseInt(input);
      } catch (NumberFormatException e) {
        System.out.println("Invalid input. Please enter a number.");
      }
    }
  }

  public int readInt(String prompt, int min, int max) {
    while (true) {
      int value = readInt(prompt);
      if (value >= min && value <= max) return value;
      System.out.println(
        "Please enter a number between " + min + " and " + max + "."
      );
    }
  }

  public LocalDate readLocalDate(String prompt) {
    while (true) {
      System.out.print(prompt);
      String input = scanner.nextLine().trim();
      try {
        return LocalDate.parse(input);
      } catch (DateTimeParseException e) {
        System.out.println(
          "Invalid date format. Please use yyyy-MM-dd (e.g., 2026-09-15)."
        );
      }
    }
  }

  public LocalTime readLocalTime(String prompt) {
    while (true) {
      System.out.print(prompt);
      String input = scanner.nextLine().trim();
      try {
        return LocalTime.parse(input);
      } catch (DateTimeParseException e) {
        System.out.println(
          "Invalid time format. Please use HH:mm (e.g., 20:00)."
        );
      }
    }
  }

  public boolean readYesNo(String prompt) {
    while (true) {
      System.out.print(prompt + " (y/n): ");
      String input = scanner.nextLine().trim().toLowerCase();
      if (input.equals("y") || input.equals("yes")) return true;
      if (input.equals("n") || input.equals("no")) return false;
      System.out.println("Please enter 'y' or 'n'.");
    }
  }

  public UUID readUUID(String prompt) {
    while (true) {
      System.out.print(prompt);
      String input = scanner.nextLine().trim();
      if (input.isEmpty()) {
        System.out.println("Input cannot be empty.");
        continue;
      }
      try {
        return UUID.fromString(input);
      } catch (IllegalArgumentException e) {
        System.out.println("Invalid UUID format. Please enter a valid UUID.");
      }
    }
  }

  /**
   * Reads and validates an email address using a simple regex.
   */
  public String readEmail(String prompt) {
    while (true) {
      System.out.print(prompt);
      String input = scanner.nextLine().trim();
      if (input.matches("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) {
        return input;
      }
      System.out.println(
        "Invalid email format. Please enter a valid email (e.g., user@example.com)."
      );
    }
  }

  public void pressEnterToContinue() {
    System.out.print("Press Enter to continue...");
    scanner.nextLine();
  }

  // --------------------------------------------------------------
  //  SELECTION PRINT METHODS
  // --------------------------------------------------------------
  public int printAndSelectVenues(List<Venue> venues) {
    System.out.println("\nSelect a venue:");
    System.out.println("0. Cancel");
    for (int i = 0; i < venues.size(); i++) {
      Venue v = venues.get(i);
      System.out.println(
        (i + 1) + ". " + v.getName() + " (" + v.getAddress() + ")"
      );
    }
    return readInt("Enter choice: ", 0, venues.size());
  }

  public int printAndSelectEvents(List<Event> events) {
    System.out.println("\nSelect an event:");
    System.out.println("0. Cancel");
    DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");
    for (int i = 0; i < events.size(); i++) {
      Event e = events.get(i);
      String startDate = e.getStartAt().format(dateFormatter);
      String startTime = e.getStartAt().format(timeFormatter);
      String endTime = e.getEndAt().format(timeFormatter);
      System.out.printf(
        "%d. %s | %s %s – %s%n",
        i + 1,
        e.getTitle(),
        startDate,
        startTime,
        endTime
      );
    }
    return readInt("Enter choice: ", 0, events.size());
  }

  public int printAndSelectSeats(List<Seat> seats) {
    System.out.println("\nSelect a seat:");
    System.out.println("0. Cancel");
    for (int i = 0; i < seats.size(); i++) {
      Seat s = seats.get(i);
      System.out.printf(
        "%d. Section: %s, Row: %s, Number: %d (Category: %s)%n",
        i + 1,
        s.getSection(),
        s.getRow(),
        s.getNumber(),
        s.getCategory()
      );
    }
    return readInt("Enter choice: ", 0, seats.size());
  }

  // --------------------------------------------------------------
  //  PRINT METHODS
  // --------------------------------------------------------------
  public void printVenues(List<Venue> venues) {
    if (venues.isEmpty()) {
      System.out.println("No venues found.");
      return;
    }
    System.out.println("\n========== Venues ==========");
    for (int i = 0; i < venues.size(); i++) {
      Venue v = venues.get(i);
      System.out.printf("%d. %s (%s)%n", i + 1, v.getName(), v.getAddress());
    }
    System.out.println("============================");
  }

  public void printEvents(List<Event> events) {
    if (events.isEmpty()) {
      System.out.println("No events found.");
      return;
    }
    System.out.println("\n========== Events ==========");
    DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");
    for (int i = 0; i < events.size(); i++) {
      Event e = events.get(i);
      System.out.printf(
        "%d. %s | %s %s – %s%n",
        i + 1,
        e.getTitle(),
        e.getStartAt().format(dateFormatter),
        e.getStartAt().format(timeFormatter),
        e.getEndAt().format(timeFormatter)
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
    for (int i = 0; i < seats.size(); i++) {
      Seat s = seats.get(i);
      System.out.printf(
        "%d. Section: %s, Row: %s, Number: %d (Category: %s)%n",
        i + 1,
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
    for (int i = 0; i < reservations.size(); i++) {
      Reservation r = reservations.get(i);

      // Compute total price from all seats in this reservation
      var seats = r.getSeats();
      java.math.BigDecimal total = java.math.BigDecimal.ZERO;
      String currencySymbol = "";
      for (var rs : seats) {
        total = total.add(rs.price().amount());
        if (currencySymbol.isEmpty()) {
          currencySymbol = rs.price().currency().getSymbol();
        }
      }

      System.out.printf(
        "%d. Event: %s | Email: %s | Status: %s | Seats: %d | Total: %s %s%n",
        i + 1,
        r.getEventId(),
        r.getCustomerEmail(),
        r.getStatus(),
        seats.size(),
        currencySymbol,
        total
      );
    }
    System.out.println("==================================");
  }

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
}
