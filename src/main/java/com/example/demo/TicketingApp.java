package com.example.demo;

import java.util.Scanner;
import java.util.UUID;

import com.example.demo.cli.CommandProcessor;
import com.example.demo.cli.ConsoleRenderer;
import com.example.demo.persistence.JsonStorage;
import com.example.demo.repository.EventRepository;
import com.example.demo.repository.ReservationRepository;
import com.example.demo.repository.SeatRepository;
import com.example.demo.repository.VenueRepository;
import com.example.demo.repository.memory.InMemoryEventRepository;
import com.example.demo.repository.memory.InMemoryReservationRepository;
import com.example.demo.repository.memory.InMemorySeatRepository;
import com.example.demo.repository.memory.InMemoryVenueRepository;
import com.example.demo.service.BookingService;
import com.example.demo.service.PricingService;
import com.example.demo.service.ReportService;
import com.example.demo.service.VenueService;
import com.example.demo.util.IdGenerator;

public class TicketingApp {
    private final CommandProcessor processor;
    private final JsonStorage storage;
    private final VenueService venueService;
    private final BookingService bookingService;
    private final ReportService reportService;

    public TicketingApp() {
        VenueRepository venueRepo = new InMemoryVenueRepository();
        EventRepository eventRepo = new InMemoryEventRepository();
        SeatRepository seatRepo = new InMemorySeatRepository();
        ReservationRepository reservationRepo = new InMemoryReservationRepository();

        IdGenerator idGen = UUID::randomUUID;
        var pricingService = new PricingService();
        venueService = new VenueService(venueRepo, eventRepo, seatRepo, idGen);
        bookingService = new BookingService(reservationRepo, eventRepo, seatRepo, pricingService, idGen);
        reportService = new ReportService(reservationRepo, seatRepo, eventRepo);

        var renderer = new ConsoleRenderer();
        processor = new CommandProcessor(venueService, bookingService, reportService, renderer);
        storage = new JsonStorage("ticketing-data.json");
        loadState();
    }

    private void loadState() {
        try {
            var ctx = storage.load();
            // optionally fill repositories
        } catch (Exception e) {
            System.err.println("Warning: Could not load previous data: " + e.getMessage());
        }
    }

    private void saveState() {
        // Collect all data from repositories and save
        var venues = venueService.listVenues();
        var events = venueService.listEvents();
        var seats = venueService.listSeatsByVenue(venues.isEmpty() ? null : venues.get(0).getId()); // placeholder
        // Actually you'd collect all seats from all venues – but for now just stub.
        System.out.println("Saving state... (not fully implemented)");
    }

    public void run() {
        Scanner scanner = new Scanner(System.in);
        System.out.println("Ticketing System CLI. Type 'help' for commands.");
        boolean running = true;
        while (running) {
            System.out.print("> ");
            String line = scanner.nextLine();
            if (line.trim().equalsIgnoreCase("save")) {
                saveState();
                continue;
            } else if (line.trim().equalsIgnoreCase("load")) {
                loadState();
                System.out.println("State loaded.");
                continue;
            }
            running = processor.process(line);
        }
        System.out.println("Goodbye.");
    }

    public static void main(String[] args) {
        new TicketingApp().run();
    }
}