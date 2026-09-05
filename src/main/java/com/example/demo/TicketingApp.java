package com.example.demo;

import com.example.demo.cli.MenuController;
import com.example.demo.cli.MenuRenderer;
import com.example.demo.persistence.JsonStorage;
import com.example.demo.repository.*;
import com.example.demo.repository.memory.*;
import com.example.demo.service.*;
import com.example.demo.util.IdGenerator;

import java.util.UUID;

public class TicketingApp {
    private final JsonStorage storage;
    private final HoldExpirySweeper sweeper;
    private final MenuController menuController;

    public TicketingApp() {
        VenueRepository venueRepo = new InMemoryVenueRepository();
        EventRepository eventRepo = new InMemoryEventRepository();
        SeatRepository seatRepo = new InMemorySeatRepository();
        ReservationRepository reservationRepo = new InMemoryReservationRepository();

        IdGenerator idGen = UUID::randomUUID;
        var pricingService = new PricingService();
        var venueService = new VenueService(venueRepo, eventRepo, seatRepo, idGen);
        var bookingService = new BookingService(reservationRepo, eventRepo, seatRepo, pricingService, idGen);
        var reportService = new ReportService(reservationRepo, seatRepo, eventRepo);

        // Phase 2: sweeper
        sweeper = new HoldExpirySweeper(bookingService);
        sweeper.start();

        var renderer = new MenuRenderer();
        storage = new JsonStorage("ticketing-data.json");

        menuController = new MenuController(
                venueService, bookingService, reportService, sweeper,
                renderer, storage,
                venueRepo, eventRepo, seatRepo, reservationRepo
        );
    }

    public void run() {
        menuController.run();
        sweeper.stop();
        System.out.println("Goodbye!");
    }

    public static void main(String[] args) {
        new TicketingApp().run();
    }
}