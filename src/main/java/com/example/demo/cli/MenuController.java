package com.example.demo.cli;

import com.example.demo.domain.PricingRules;
import com.example.demo.enums.Currency;
import com.example.demo.enums.EventStatus;
import com.example.demo.enums.SeatAttribute;
import com.example.demo.enums.SeatCategory;
import com.example.demo.enums.TimeZoneEnum;
import com.example.demo.persistence.JsonStorage;
import com.example.demo.repository.EventRepository;
import com.example.demo.repository.ReservationRepository;
import com.example.demo.repository.SeatRepository;
import com.example.demo.repository.VenueRepository;
import com.example.demo.service.BookingService;
import com.example.demo.service.HoldExpirySweeper;
import com.example.demo.service.ReportService;
import com.example.demo.service.VenueService;
import java.math.BigDecimal;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public class MenuController {

  private final VenueService venueService;
  private final BookingService bookingService;
  private final ReportService reportService;
  private final HoldExpirySweeper sweeper;
  private final MenuRenderer renderer;
  private final JsonStorage storage;
  private final VenueRepository venueRepository;
  private final EventRepository eventRepository;
  private final SeatRepository seatRepository;
  private final ReservationRepository reservationRepository;
  private boolean running = true;

  public MenuController(
    VenueService venueService,
    BookingService bookingService,
    ReportService reportService,
    HoldExpirySweeper sweeper,
    MenuRenderer renderer,
    JsonStorage storage,
    VenueRepository venueRepository,
    EventRepository eventRepository,
    SeatRepository seatRepository,
    ReservationRepository reservationRepository
  ) {
    this.venueService = venueService;
    this.bookingService = bookingService;
    this.reportService = reportService;
    this.sweeper = sweeper;
    this.renderer = renderer;
    this.storage = storage;
    this.venueRepository = venueRepository;
    this.eventRepository = eventRepository;
    this.seatRepository = seatRepository;
    this.reservationRepository = reservationRepository;
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
        case 6 -> saveLoadMenu();
        case 7 -> statusMenu();
        case 8 -> {
          running = false;
          renderer.printMessage("Goodbye!");
        }
        default -> renderer.printError("Invalid option.");
      }
    }
  }

  private ZoneId selectTimezone() {
    renderer.printMessage("\nSelect timezone:");
    var timezones = TimeZoneEnum.getAll();
    for (TimeZoneEnum tz : timezones) {
      System.out.println(tz.getId() + ". " + tz.getDisplayName());
    }
    int choice = renderer.readInt();
    if (choice < 1 || choice > timezones.size()) {
      renderer.printError("Invalid selection.");
      return selectTimezone();
    }
    return TimeZoneEnum.fromId(choice).getZoneId();
  }

  private Currency selectCurrency() {
    renderer.printMessage("\nSelect currency:");
    var currencies = Currency.values();
    for (int i = 0; i < currencies.length; i++) {
      System.out.println((i + 1) + ". " + currencies[i]);
    }
    int choice = renderer.readInt();
    if (choice < 1 || choice > currencies.length) {
      renderer.printError("Invalid selection.");
      return selectCurrency();
    }
    return currencies[choice - 1];
  }

  private boolean askYesNo(String question) {
    renderer.printMessage(question + " (y/n): ");
    String input = renderer.readLine().toLowerCase();
    return input.equals("y") || input.equals("yes");
  }

  private BigDecimal askPrice(String prompt, BigDecimal defaultPrice) {
    renderer.printMessage(
      prompt + " (press Enter for default " + defaultPrice + "): "
    );
    String input = renderer.readLine();
    if (input.isBlank()) return defaultPrice;
    try {
      return new BigDecimal(input);
    } catch (NumberFormatException e) {
      renderer.printError("Invalid number, using default: " + defaultPrice);
      return defaultPrice;
    }
  }

  // ========== Venue Management ==========
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
          ZoneId zoneId = selectTimezone();
          try {
            var venue = venueService.createVenue(name, address, zoneId.getId());
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

  // ========== Event Management ==========
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
          renderer.printMessage(
            "Enter start time (ISO-8601, e.g., 2026-09-07T20:00:00-04:00): "
          );
          String startStr = renderer.readLine();
          renderer.printMessage("Enter end time (ISO-8601): ");
          String endStr = renderer.readLine();

          Currency currency = selectCurrency();

          BigDecimal defaultPrice = BigDecimal.valueOf(50.00);
          BigDecimal standardPrice = askPrice(
            "Enter base price for Standard seats",
            defaultPrice
          );

          PricingRules rules = new PricingRules(currency, standardPrice);

          if (askYesNo("Does this event have VIP seats?")) {
            BigDecimal vipPrice = askPrice(
              "Enter VIP seat price",
              standardPrice.multiply(BigDecimal.valueOf(2))
            );
            rules = rules.withCategoryPrice(SeatCategory.VIP, vipPrice);
          }
          if (askYesNo("Does this event have VVIP seats?")) {
            BigDecimal vvipPrice = askPrice(
              "Enter VVIP seat price",
              standardPrice.multiply(BigDecimal.valueOf(3))
            );
            rules = rules.withCategoryPrice(SeatCategory.VVIP, vvipPrice);
          }

          try {
            ZonedDateTime start = ZonedDateTime.parse(startStr);
            ZonedDateTime end = ZonedDateTime.parse(endStr);
            var event = venueService.createEvent(
              venueId,
              title,
              start,
              end,
              EventStatus.SCHEDULED,
              currency,
              rules
            );
            renderer.printSuccess("Event created: " + event.getId());
          } catch (Exception e) {
            renderer.printError("Error: " + e.getMessage());
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
          events
            .stream()
            .filter(e -> e.getId().equals(eventId))
            .findFirst()
            .ifPresentOrElse(
              e -> renderer.printEventDetails(e),
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
          var event = venueService
            .listEvents()
            .stream()
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

  // ========== Seat Management ==========
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
          renderer.printMessage(
            "Select category (1: Standard, 2: VIP, 3: VVIP): "
          );
          int catChoice = renderer.readInt();
          SeatCategory category = switch (catChoice) {
            case 2 -> SeatCategory.VIP;
            case 3 -> SeatCategory.VVIP;
            default -> SeatCategory.STANDARD;
          };
          renderer.printMessage(
            "Enter attributes (optional, semicolon-separated e.g. ACCESSIBLE;HAS_POWER): "
          );
          String attributesInput = renderer.readLine();
          Set<SeatAttribute> attributes = parseAttributes(attributesInput);
          try {
            var seat = venueService.createSeat(
              venueId,
              section,
              row,
              number,
              category,
              attributes
            );
            renderer.printSuccess("Seat created: " + seat.getId());
          } catch (Exception e) {
            renderer.printError(e.getMessage());
          }
          renderer.pressEnterToContinue();
        }
        case 3 -> {
          renderer.printMessage(
            "CSV import not fully implemented. (Placeholder)"
          );
          renderer.pressEnterToContinue();
        }
        case 4 -> back = true;
        default -> renderer.printError("Invalid option.");
      }
    }
  }

  // ========== Reservation Management ==========
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
            renderer.printSuccess(
              "Hold created! Reservation ID: " + reservation.getId()
            );
            renderer.printReservationDetails(reservation);
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

  // ========== Reports ==========
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
            map.forEach((id, count) ->
              renderer.printMessage("Event " + id + ": " + count + " seats")
            );
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

  // ========== Save / Load ==========
  private void saveLoadMenu() {
    boolean back = false;
    while (!back) {
      renderer.printSaveLoadMenu();
      int choice = renderer.readInt();
      switch (choice) {
        case 1 -> saveState();
        case 2 -> loadState();
        case 3 -> back = true;
        default -> renderer.printError("Invalid option.");
      }
      if (choice != 3) renderer.pressEnterToContinue();
    }
  }

  private void saveState() {
    try {
      var venues = venueRepository.findAll();
      var events = eventRepository.findAll();
      var seats = seatRepository.findAll();
      var reservations = reservationRepository.findAll();
      storage.save(venues, events, seats, reservations);
      renderer.printSuccess("State saved successfully to ticketing-data.json");
    } catch (Exception e) {
      renderer.printError("Failed to save state: " + e.getMessage());
    }
  }

  private void loadState() {
    try {
      var context = storage.load();

      venueRepository
        .findAll()
        .forEach(v -> venueRepository.deleteById(v.getId()));
      eventRepository
        .findAll()
        .forEach(e -> eventRepository.deleteById(e.getId()));
      seatRepository
        .findAll()
        .forEach(s -> seatRepository.deleteById(s.getId()));
      reservationRepository
        .findAll()
        .forEach(r -> reservationRepository.deleteById(r.getId()));

      context.venues.forEach(venueRepository::save);
      context.events.forEach(eventRepository::save);
      context.seats.forEach(seatRepository::save);
      context.reservations.forEach(reservationRepository::save);

      renderer.printSuccess(
        "State loaded successfully from ticketing-data.json"
      );
    } catch (Exception e) {
      renderer.printError("Failed to load state: " + e.getMessage());
    }
  }

  // ========== Status ==========
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
          renderer.printStatus(
            sweeperRunning,
            activeHolds,
            cacheSize,
            lockedEvents
          );
          renderer.pressEnterToContinue();
        }
        case 2 -> back = true;
        default -> renderer.printError("Invalid option.");
      }
    }
  }

  // ========== Utility ==========
  private Set<SeatAttribute> parseAttributes(String input) {
    if (input == null || input.isBlank()) {
      return Collections.emptySet();
    }
    Set<SeatAttribute> result = new HashSet<>();
    for (String token : input.split(";")) {
      String trimmed = token.trim();
      if (!trimmed.isEmpty()) {
        try {
          result.add(SeatAttribute.valueOf(trimmed.toUpperCase()));
        } catch (IllegalArgumentException e) {
          renderer.printError("Unknown seat attribute: " + trimmed);
        }
      }
    }
    return result;
  }
}
