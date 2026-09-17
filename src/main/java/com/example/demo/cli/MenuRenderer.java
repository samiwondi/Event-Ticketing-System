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

  // -------- Menus --------
  public void printMainMenu() {
    System.out.println(
      "\n========== Ticketing System (CLI on PostgreSQL) =========="
    );
    System.out.println("1. Venue Management");
    System.out.println("2. Event Management");
    System.out.println("3. Reservation Management");
    System.out.println("4. Reports");
    System.out.println("5. System Status");
    System.out.println("6. Exit");
    System.out.println(
      "=========================================================="
    );
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

  // -------- Input helpers (retry on invalid, skip blank lines) --------
  public String readLine() {
    return scanner.nextLine().trim();
  }

  public int readInt(String prompt) {
    while (true) {
      System.out.print(prompt);
      String in = scanner.nextLine().trim();
      if (in.isEmpty()) continue; // skip stray Enters silently
      try {
        return Integer.parseInt(in);
      } catch (NumberFormatException e) {
        System.out.println("Invalid input. Please enter a number.");
      }
    }
  }

  public int readInt(String prompt, int min, int max) {
    while (true) {
      int v = readInt(prompt);
      if (v >= min && v <= max) return v;
      System.out.println(
        "Please enter a number between " + min + " and " + max + "."
      );
    }
  }

  public LocalDate readLocalDate(String prompt) {
    while (true) {
      System.out.print(prompt);
      String in = scanner.nextLine().trim();
      if (in.isEmpty()) continue;
      try {
        return LocalDate.parse(in);
      } catch (DateTimeParseException e) {
        System.out.println("Invalid date. Use yyyy-MM-dd (e.g., 2026-09-15).");
      }
    }
  }

  public LocalTime readLocalTime(String prompt) {
    while (true) {
      System.out.print(prompt);
      String in = scanner.nextLine().trim();
      if (in.isEmpty()) continue;
      try {
        return LocalTime.parse(in);
      } catch (DateTimeParseException e) {
        System.out.println("Invalid time. Use HH:mm (e.g., 20:00).");
      }
    }
  }

  public boolean readYesNo(String prompt) {
    while (true) {
      System.out.print(prompt + " (y/n): ");
      String in = scanner.nextLine().trim().toLowerCase();
      if (in.isEmpty()) continue;
      if (in.equals("y") || in.equals("yes")) return true;
      if (in.equals("n") || in.equals("no")) return false;
      System.out.println("Please enter 'y' or 'n'.");
    }
  }

  public String readEmail(String prompt) {
    while (true) {
      System.out.print(prompt);
      String in = scanner.nextLine().trim();
      if (
        in.matches("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
      ) return in;
      System.out.println("Invalid email. Example: user@example.com");
    }
  }

  public UUID readUUID(String prompt) {
    while (true) {
      System.out.print(prompt);
      String in = scanner.nextLine().trim();
      if (in.isEmpty()) {
        System.out.println("Input cannot be empty.");
        continue;
      }
      try {
        return UUID.fromString(in);
      } catch (IllegalArgumentException e) {
        System.out.println("Invalid UUID format.");
      }
    }
  }

  public void pressEnterToContinue() {
    System.out.print("Press Enter to continue...");
    scanner.nextLine();
  }

  // -------- Selection lists (with 0. Cancel) --------
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
    DateTimeFormatter d = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    DateTimeFormatter t = DateTimeFormatter.ofPattern("HH:mm");
    for (int i = 0; i < events.size(); i++) {
      Event e = events.get(i);
      System.out.printf(
        "%d. %s | %s %s – %s%n",
        i + 1,
        e.getTitle(),
        e.getStartAt().atZone(java.time.ZoneId.systemDefault()).format(d),
        e.getStartAt().atZone(java.time.ZoneId.systemDefault()).format(t),
        e.getEndAt().atZone(java.time.ZoneId.systemDefault()).format(t)
      );
    }
    return readInt("Enter choice: ", 0, events.size());
  }

  // -------- Listing methods --------
  public void printVenues(List<Venue> venues) {
    if (venues.isEmpty()) {
      System.out.println("No venues found.");
      return;
    }
    System.out.println("\n========== Venues ==========");
    for (int i = 0; i < venues.size(); i++) {
      Venue v = venues.get(i);
      System.out.printf(
        "%d. %s (%s) [%s]%n",
        i + 1,
        v.getName(),
        v.getAddress(),
        v.getTimezone()
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
    DateTimeFormatter d = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    DateTimeFormatter t = DateTimeFormatter.ofPattern("HH:mm");
    for (int i = 0; i < events.size(); i++) {
      Event e = events.get(i);
      System.out.printf(
        "%d. %s | %s %s – %s%n",
        i + 1,
        e.getTitle(),
        e.getStartAt().atZone(java.time.ZoneId.systemDefault()).format(d),
        e.getStartAt().atZone(java.time.ZoneId.systemDefault()).format(t),
        e.getEndAt().atZone(java.time.ZoneId.systemDefault()).format(t)
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
      java.math.BigDecimal total = java.math.BigDecimal.ZERO;
      String symbol = "";
      for (var rs : r.getSeats()) {
        total = total.add(rs.getPrice().getAmount());
        if (symbol.isEmpty()) symbol = rs.getPrice().getCurrency().getSymbol();
      }
      System.out.printf(
        "%d. ID: %s | Event: %s | Email: %s | Status: %s | Seats: %d | Total: %s %s%n",
        i + 1,
        r.getId(),
        r.getEventId(),
        r.getCustomerEmail(),
        r.getStatus(),
        r.getSeats().size(),
        symbol,
        total
      );
    }
    System.out.println("==================================");
  }

  public void printStatus(
    long venues,
    long events,
    long seats,
    long reservations,
    long holds
  ) {
    System.out.println("\n========== System Status ==========");
    System.out.println("Database:         PostgreSQL (running)");
    System.out.println("Venues:           " + venues);
    System.out.println("Events:           " + events);
    System.out.println("Seats:            " + seats);
    System.out.println("Reservations:     " + reservations);
    System.out.println("Active HOLDs:     " + holds);
    System.out.println("====================================");
  }

  // -------- Generic messages --------
  public void printMessage(String m) {
    System.out.println(m);
  }

  public void printError(String e) {
    System.err.println("Error: " + e);
  }

  public void printSuccess(String s) {
    System.out.println("Success: " + s);
  }

  public void printReport(String r) {
    System.out.println(r);
  }
}
