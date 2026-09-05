package com.example.demo.cli;

import com.example.demo.domain.Event;
import com.example.demo.domain.Reservation;
import com.example.demo.domain.Seat;
import com.example.demo.domain.Venue;

import java.util.List;
import java.util.Scanner;

public class MenuRenderer {
    private final Scanner scanner = new Scanner(System.in);

    public void printMainMenu() {
        System.out.println("\n========== Ticketing System ==========");
        System.out.println("1. Venue Management");
        System.out.println("2. Event Management");
        System.out.println("3. Seat Management");
        System.out.println("4. Reservation Management");
        System.out.println("5. Reports");
        System.out.println("6. Exit");
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
        try {
            return UUID.fromString(scanner.nextLine().trim());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public void printVenues(List<Venue> venues) {
        if (venues.isEmpty()) {
            System.out.println("No venues found.");
            return;
        }
        System.out.println("\n========== Venues ==========");
        for (Venue v : venues) {
            System.out.printf("ID: %s | Name: %s | Address: %s%n", v.getId(), v.getName(), v.getAddress());
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
            System.out.printf("ID: %s | Title: %s | Start: %s%n", e.getId(), e.getTitle(), e.getStartAt());
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
            System.out.printf("ID: %s | Section: %s | Row: %s | Number: %d%n",
                    s.getId(), s.getSection(), s.getRow(), s.getNumber());
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
            System.out.printf("ID: %s | Event: %s | Email: %s | Status: %s | Seats: %d%n",
                    r.getId(), r.getEventId(), r.getCustomerEmail(), r.getStatus(), r.getSeats().size());
        }
        System.out.println("==================================");
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