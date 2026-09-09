package com.example.demo;

import com.example.demo.cli.MenuController;
import com.example.demo.cli.MenuRenderer;
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
import com.example.demo.service.HoldExpirySweeper;
import com.example.demo.service.PricingService;
import com.example.demo.service.ReportService;
import com.example.demo.service.VenueService;
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
    var bookingService = new BookingService(
      reservationRepo,
      eventRepo,
      seatRepo,
      pricingService,
      idGen
    );
    var reportService = new ReportService(reservationRepo, seatRepo, eventRepo);

    sweeper = new HoldExpirySweeper(bookingService);
    sweeper.start();

    var renderer = new MenuRenderer();
    storage = new JsonStorage("ticketing-data.json");

    menuController = new MenuController(
      venueService,
      bookingService,
      reportService,
      sweeper,
      renderer,
      storage,
      venueRepo,
      eventRepo,
      seatRepo,
      reservationRepo
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
