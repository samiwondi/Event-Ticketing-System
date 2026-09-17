package com.example.demo.cli;

import com.example.demo.domain.Event;
import com.example.demo.domain.Money;
import com.example.demo.domain.PricingRules;
import com.example.demo.domain.Reservation;
import com.example.demo.domain.ReservationSeat;
import com.example.demo.domain.Seat;
import com.example.demo.domain.Venue;
import com.example.demo.enums.Currency;
import com.example.demo.enums.EventStatus;
import com.example.demo.enums.ReservationStatus;
import com.example.demo.enums.SeatCategory;
import com.example.demo.enums.TimeZoneEnum;
import com.example.demo.exception.ConflictException;
import com.example.demo.repository.jpa.EventJpaRepository;
import com.example.demo.repository.jpa.ReservationJpaRepository;
import com.example.demo.repository.jpa.SeatJpaRepository;
import com.example.demo.repository.jpa.VenueJpaRepository;
import com.example.demo.service.BookingService;
import com.example.demo.service.ReportService;
import com.example.demo.service.VenueService;
import com.example.demo.service.VenueService.SectionLayout;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class MenuController {

  private final VenueService venueService;
  private final BookingService bookingService;
  private final ReportService reportService;
  private final VenueJpaRepository venueRepo;
  private final EventJpaRepository eventRepo;
  private final SeatJpaRepository seatRepo;
  private final ReservationJpaRepository reservationRepo;
  private final MenuRenderer renderer = new MenuRenderer();
  private boolean running = true;

  public MenuController(
    VenueService venueService,
    BookingService bookingService,
    ReportService reportService,
    VenueJpaRepository venueRepo,
    EventJpaRepository eventRepo,
    SeatJpaRepository seatRepo,
    ReservationJpaRepository reservationRepo
  ) {
    this.venueService = venueService;
    this.bookingService = bookingService;
    this.reportService = reportService;
    this.venueRepo = venueRepo;
    this.eventRepo = eventRepo;
    this.seatRepo = seatRepo;
    this.reservationRepo = reservationRepo;
  }

  public void run() {
    System.out.println("\nTicketing CLI started. Connected to PostgreSQL.\n");
    while (running) {
      try {
        renderer.printMainMenu();
        int choice = renderer.readInt("", 1, 6);
        switch (choice) {
          case 1 -> venueManagement();
          case 2 -> eventManagement();
          case 3 -> reservationManagement();
          case 4 -> reportsMenu();
          case 5 -> statusMenu();
          case 6 -> {
            running = false;
            renderer.printMessage("Goodbye!");
          }
        }
      } catch (Exception e) {
        renderer.printError("Unexpected error: " + e.getMessage());
        renderer.pressEnterToContinue();
      }
    }
  }

  // ------------------------------------------------------------------
  //  Selection helpers
  // ------------------------------------------------------------------
  private Venue selectVenue() {
    var venues = venueService.listVenues();
    if (venues.isEmpty()) {
      renderer.printError("No venues available.");
      return null;
    }
    int c = renderer.printAndSelectVenues(venues);
    return c == 0 ? null : venues.get(c - 1);
  }

  private Event selectEvent() {
    var events = venueService.listEvents();
    if (events.isEmpty()) {
      renderer.printError("No events available.");
      return null;
    }
    int c = renderer.printAndSelectEvents(events);
    return c == 0 ? null : events.get(c - 1);
  }

  private ZoneId selectTimezone() {
    var list = TimeZoneEnum.getAll();
    renderer.printMessage("\nSelect timezone:");
    for (TimeZoneEnum tz : list)
      System.out.println(tz.getId() + ". " + tz.getDisplayName());
    int c = renderer.readInt("Enter choice: ", 1, list.size());
    return TimeZoneEnum.fromId(c).getZoneId();
  }

  private Currency selectCurrency() {
    var currencies = Currency.values();
    renderer.printMessage("\nSelect currency:");
    for (int i = 0; i < currencies.length; i++) System.out.println(
      (i + 1) + ". " + currencies[i]
    );
    int c = renderer.readInt("Enter choice: ", 1, currencies.length);
    return currencies[c - 1];
  }

  private BigDecimal askPrice(String prompt, BigDecimal defaultPrice) {
    while (true) {
      renderer.printMessage(
        prompt + " (press Enter for default " + defaultPrice + "): "
      );
      String in = renderer.readLine();
      if (in.isBlank()) return defaultPrice;
      try {
        return new BigDecimal(in);
      } catch (NumberFormatException e) {
        renderer.printError("Invalid number.");
      }
    }
  }

  private Set<String> multiSelectSections(List<String> sections) {
    for (int i = 0; i < sections.size(); i++) System.out.println(
      (i + 1) + ". " + sections.get(i)
    );
    System.out.print("Enter comma-separated numbers (Enter for none): ");
    String in = renderer.readLine();
    Set<String> sel = new HashSet<>();
    if (in == null || in.isBlank()) return sel;
    for (String p : in.split(",")) {
      try {
        int i = Integer.parseInt(p.trim()) - 1;
        if (i >= 0 && i < sections.size()) sel.add(sections.get(i));
      } catch (NumberFormatException ignored) {}
    }
    return sel;
  }

  // ------------------------------------------------------------------
  //  Venue Management
  // ------------------------------------------------------------------
  private void venueManagement() {
    boolean back = false;
    while (!back) {
      renderer.printVenueMenu();
      int choice = renderer.readInt("", 1, 5);
      switch (choice) {
        case 1 -> {
          renderer.printVenues(venueService.listVenues());
          renderer.pressEnterToContinue();
        }
        case 2 -> {
          renderer.printMessage("Enter venue name: ");
          String name = renderer.readLine();
          renderer.printMessage("Enter address: ");
          String address = renderer.readLine();
          ZoneId zone = selectTimezone();

          int sectionCount = renderer.readInt("How many sections? ", 1, 26);
          int rows = renderer.readInt("How many rows per section? ", 1, 500);
          int seatsPerRow = renderer.readInt(
            "How many seats per row? ",
            1,
            500
          );

          int total = sectionCount * rows * seatsPerRow;
          renderer.printMessage("Creating " + total + " seats... please wait.");

          try {
            List<SectionLayout> layouts = new ArrayList<>();
            for (int i = 0; i < sectionCount; i++) {
              String sname = String.valueOf((char) ('A' + i));
              layouts.add(new SectionLayout(sname, rows, seatsPerRow));
            }
            Venue v = venueService.createVenueWithSections(
              name,
              address,
              zone.getId(),
              layouts
            );
            renderer.printSuccess(
              "Venue created: " + v.getId() + " (" + total + " seats)"
            );
          } catch (Exception e) {
            renderer.printError(e.getMessage());
          }
          renderer.pressEnterToContinue();
        }
        case 3 -> {
          Venue v = selectVenue();
          if (v == null) {
            renderer.pressEnterToContinue();
            break;
          }
          renderer.printMessage("\nWhat do you want to update?");
          System.out.println("0. Cancel");
          System.out.println("1. Name (current: " + v.getName() + ")");
          System.out.println("2. Address (current: " + v.getAddress() + ")");
          System.out.println("3. Timezone (current: " + v.getTimezone() + ")");
          System.out.println("4. All fields");
          int f = renderer.readInt("Enter choice: ", 0, 4);
          if (f == 0) {
            renderer.pressEnterToContinue();
            break;
          }

          String nn = v.getName(),
            na = v.getAddress(),
            ntz = v.getTimezone();
          boolean changed = false;
          if (f == 1 || f == 4) {
            renderer.printMessage("Enter new name: ");
            nn = renderer.readLine();
            changed = true;
          }
          if (f == 2 || f == 4) {
            renderer.printMessage("Enter new address: ");
            na = renderer.readLine();
            changed = true;
          }
          if (f == 3 || f == 4) {
            ntz = selectTimezone().getId();
            changed = true;
          }
          if (changed) {
            try {
              venueService.updateVenue(v.getId(), nn, na, ntz);
              renderer.printSuccess("Venue updated.");
            } catch (Exception e) {
              renderer.printError(e.getMessage());
            }
          }
          renderer.pressEnterToContinue();
        }
        case 4 -> {
          Venue v = selectVenue();
          if (v == null) {
            renderer.pressEnterToContinue();
            break;
          }
          if (
            !renderer.readYesNo(
              "Delete this venue? All events/reservations will be removed."
            )
          ) {
            renderer.pressEnterToContinue();
            break;
          }
          try {
            for (Event e : eventRepo
              .findAll()
              .stream()
              .filter(ev -> ev.getVenueId().equals(v.getId()))
              .toList()) {
              venueService.deleteEvent(e.getId());
            }
            venueService.deleteVenue(v.getId());
            renderer.printSuccess("Venue and all related data deleted.");
          } catch (Exception e) {
            renderer.printError(e.getMessage());
          }
          renderer.pressEnterToContinue();
        }
        case 5 -> back = true;
      }
    }
  }

  // ------------------------------------------------------------------
  //  Event Management
  // ------------------------------------------------------------------
  private void eventManagement() {
    boolean back = false;
    while (!back) {
      renderer.printEventMenu();
      int choice = renderer.readInt("", 1, 7);
      switch (choice) {
        case 1 -> {
          renderer.printEvents(venueService.listEvents());
          renderer.pressEnterToContinue();
        }
        case 2 -> {
          Venue venue = selectVenue();
          if (venue == null) {
            renderer.pressEnterToContinue();
            break;
          }
          renderer.printMessage("Enter event title: ");
          String title = renderer.readLine();
          ZoneId zone = ZoneId.of(venue.getTimezone());

          LocalDate sDate = renderer.readLocalDate(
            "Enter start date (yyyy-MM-dd): "
          );
          int days = renderer.readInt(
            "Enter duration in days (0 if same day): ",
            0,
            365
          );
          LocalTime sTime = renderer.readLocalTime(
            "Enter start time (HH:mm): "
          );
          int hours = renderer.readInt("Enter duration in hours: ", 0, 24);
          int minutes = renderer.readInt("Enter duration in minutes: ", 0, 59);

          ZonedDateTime zStart = sDate.atTime(sTime).atZone(zone);
          ZonedDateTime zEnd = zStart
            .plusDays(days)
            .plusHours(hours)
            .plusMinutes(minutes);
          Instant start = zStart.toInstant();
          Instant end = zEnd.toInstant();

          if (!end.isAfter(start)) {
            renderer.printError("Duration must be > 0.");
            renderer.pressEnterToContinue();
            break;
          }
          if (start.isBefore(Instant.now())) {
            renderer.printError("Start cannot be in the past.");
            renderer.pressEnterToContinue();
            break;
          }

          List<String> sections = seatRepo
            .findByVenueIdOrderBySectionAscRowAscNumberAsc(venue.getId())
            .stream()
            .map(Seat::getSection)
            .distinct()
            .sorted()
            .toList();
          if (sections.isEmpty()) {
            renderer.printError("This venue has no seats.");
            renderer.pressEnterToContinue();
            break;
          }

          Currency currency = selectCurrency();
          BigDecimal standardPrice = askPrice(
            "Enter standard seat price",
            BigDecimal.valueOf(50)
          );

          Map<String, SeatCategory> sectionCats = new HashMap<>();
          for (String s : sections) sectionCats.put(s, SeatCategory.STANDARD);

          PricingRules rules = new PricingRules(
            currency,
            standardPrice
          ).withSectionCategories(sectionCats);

          if (renderer.readYesNo("Does this event have VIP sections?")) {
            renderer.printMessage("\nWhich sections are VIP?");
            Set<String> vip = multiSelectSections(sections);
            if (!vip.isEmpty()) {
              BigDecimal vipPrice = askPrice(
                "Enter VIP seat price",
                standardPrice.multiply(BigDecimal.valueOf(2))
              );
              rules = rules.withCategoryPrice(SeatCategory.VIP, vipPrice);
              Map<String, SeatCategory> updated = new HashMap<>(
                rules.sectionCategories()
              );
              for (String s : vip) updated.put(s, SeatCategory.VIP);
              rules = rules.withSectionCategories(updated);
            }
          }
          if (renderer.readYesNo("Does this event have VVIP sections?")) {
            renderer.printMessage("\nWhich sections are VVIP?");
            Set<String> vvip = multiSelectSections(sections);
            if (!vvip.isEmpty()) {
              BigDecimal vvipPrice = askPrice(
                "Enter VVIP seat price",
                standardPrice.multiply(BigDecimal.valueOf(3))
              );
              rules = rules.withCategoryPrice(SeatCategory.VVIP, vvipPrice);
              Map<String, SeatCategory> updated = new HashMap<>(
                rules.sectionCategories()
              );
              for (String s : vvip) updated.put(s, SeatCategory.VVIP);
              rules = rules.withSectionCategories(updated);
            }
          }

          try {
            Event e = venueService.createEvent(
              venue.getId(),
              title,
              start,
              end,
              EventStatus.SCHEDULED,
              currency,
              rules
            );
            renderer.printSuccess("Event created: " + e.getId());
          } catch (Exception ex) {
            renderer.printError(ex.getMessage());
          }
          renderer.pressEnterToContinue();
        }
        case 3 -> {
          Event e = selectEvent();
          if (e == null) {
            renderer.pressEnterToContinue();
            break;
          }
          renderer.printReport(
            "Event: " +
              e.getTitle() +
              "\nVenue: " +
              e.getVenueId() +
              "\nStart: " +
              e.getStartAt() +
              "\nEnd: " +
              e.getEndAt() +
              "\nCurrency: " +
              e.getCurrency() +
              "\nPricing: " +
              e.getPricingRules()
          );
          renderer.pressEnterToContinue();
        }
        case 4 -> {
          Event e = selectEvent();
          if (e == null) {
            renderer.pressEnterToContinue();
            break;
          }
          renderer.printSeats(venueService.listSeatsByVenue(e.getVenueId()));
          renderer.pressEnterToContinue();
        }
        case 5 -> {
          Event e = selectEvent();
          if (e == null) {
            renderer.pressEnterToContinue();
            break;
          }
          renderer.printMessage("\nWhat do you want to update?");
          System.out.println("0. Cancel");
          System.out.println("1. Title (current: " + e.getTitle() + ")");
          System.out.println("2. Start time (current: " + e.getStartAt() + ")");
          System.out.println("3. End time (current: " + e.getEndAt() + ")");
          System.out.println("4. All fields");
          int f = renderer.readInt("Enter choice: ", 0, 4);
          if (f == 0) {
            renderer.pressEnterToContinue();
            break;
          }

          ZoneId zone = ZoneId.of(
            venueRepo.findById(e.getVenueId()).get().getTimezone()
          );

          String nt = e.getTitle();
          Instant ns = e.getStartAt(),
            ne = e.getEndAt();

          if (f == 1 || f == 4) {
            renderer.printMessage("Enter new title: ");
            nt = renderer.readLine();
          }
          if (f == 2 || f == 4) {
            LocalDate d = renderer.readLocalDate(
              "Enter new start date (yyyy-MM-dd): "
            );
            LocalTime t = renderer.readLocalTime(
              "Enter new start time (HH:mm): "
            );
            ns = d.atTime(t).atZone(zone).toInstant();
          }
          if (f == 3 || f == 4) {
            LocalDate d = renderer.readLocalDate(
              "Enter new end date (yyyy-MM-dd): "
            );
            LocalTime t = renderer.readLocalTime(
              "Enter new end time (HH:mm): "
            );
            ne = d.atTime(t).atZone(zone).toInstant();
          }
          if (!ne.isAfter(ns)) {
            renderer.printError("End must be after start.");
            renderer.pressEnterToContinue();
            break;
          }

          try {
            venueService.updateEvent(
              e.getId(),
              nt,
              ns,
              ne,
              e.getStatus(),
              e.getPricingRules()
            );
            renderer.printSuccess("Event updated.");
          } catch (Exception ex) {
            renderer.printError(ex.getMessage());
          }
          renderer.pressEnterToContinue();
        }
        case 6 -> {
          Event e = selectEvent();
          if (e == null) {
            renderer.pressEnterToContinue();
            break;
          }
          if (
            !renderer.readYesNo(
              "Delete this event? Reservations will also be deleted."
            )
          ) {
            renderer.pressEnterToContinue();
            break;
          }
          try {
            venueService.deleteEvent(e.getId());
            renderer.printSuccess("Event deleted.");
          } catch (Exception ex) {
            renderer.printError(ex.getMessage());
          }
          renderer.pressEnterToContinue();
        }
        case 7 -> back = true;
      }
    }
  }

  // ------------------------------------------------------------------
  //  Reservation Management
  // ------------------------------------------------------------------
  private void reservationManagement() {
    boolean back = false;
    while (!back) {
      renderer.printReservationMenu();
      int choice = renderer.readInt("", 1, 6);
      switch (choice) {
        case 1 -> holdFlow();
        case 2 -> confirmFlow();
        case 3 -> cancelFlow();
        case 4 -> {
          Event e = selectEvent();
          if (e == null) {
            renderer.pressEnterToContinue();
            break;
          }
          renderer.printReservations(
            bookingService.getReservationsForEvent(e.getId())
          );
          renderer.pressEnterToContinue();
        }
        case 5 -> {
          renderer.printReservations(bookingService.getAllHolds());
          renderer.pressEnterToContinue();
        }
        case 6 -> back = true;
      }
    }
  }

  private void holdFlow() {
    Event event = selectEvent();
    if (event == null) {
      renderer.pressEnterToContinue();
      return;
    }

    String email = renderer.readEmail("Enter customer email: ");

    List<Seat> all = venueService.listSeatsByVenue(event.getVenueId());
    Set<UUID> reserved = bookingService
      .getReservationsForEvent(event.getId())
      .stream()
      .filter(
        r ->
          r.getStatus() == ReservationStatus.HOLD ||
          r.getStatus() == ReservationStatus.CONFIRMED
      )
      .flatMap(r -> r.getSeats().stream())
      .map(ReservationSeat::getSeatId)
      .collect(Collectors.toSet());
    List<Seat> available = all
      .stream()
      .filter(s -> !reserved.contains(s.getId()))
      .toList();
    if (available.isEmpty()) {
      renderer.printError("All seats are reserved.");
      renderer.pressEnterToContinue();
      return;
    }

    PricingRules rules = event.getPricingRules();

    // Section
    List<String> sections = available
      .stream()
      .map(Seat::getSection)
      .distinct()
      .sorted()
      .toList();
    renderer.printMessage("\nSelect a section:");
    for (int i = 0; i < sections.size(); i++) {
      String s = sections.get(i);
      SeatCategory cat = rules.getCategoryForSection(s);
      String label = (cat == SeatCategory.STANDARD) ? s : s + " (" + cat + ")";
      System.out.println((i + 1) + ". " + label);
    }
    int sc = renderer.readInt("Enter choice: ", 1, sections.size());
    String selectedSection = sections.get(sc - 1);

    // Row
    List<String> rows = available
      .stream()
      .filter(s -> s.getSection().equals(selectedSection))
      .map(Seat::getRow)
      .distinct()
      .sorted()
      .toList();
    renderer.printMessage("\nSelect a row:");
    for (int i = 0; i < rows.size(); i++) System.out.println(
      (i + 1) + ". Row " + rows.get(i)
    );
    int rc = renderer.readInt("Enter choice: ", 1, rows.size());
    String selectedRow = rows.get(rc - 1);

    // Seats in row
    List<Seat> inRow = available
      .stream()
      .filter(
        s ->
          s.getSection().equals(selectedSection) &&
          s.getRow().equals(selectedRow)
      )
      .sorted(Comparator.comparingInt(Seat::getNumber))
      .toList();
    renderer.printMessage("\nSelect seat number(s):");
    for (int i = 0; i < inRow.size(); i++) System.out.println(
      (i + 1) + ". Seat " + inRow.get(i).getNumber()
    );
    renderer.printMessage("Enter comma-separated numbers (e.g., 1,3,5): ");
    String seatStr = renderer.readLine();

    List<Seat> selected = new ArrayList<>();
    for (String p : seatStr.split(",")) {
      try {
        int i = Integer.parseInt(p.trim()) - 1;
        if (i >= 0 && i < inRow.size()) selected.add(inRow.get(i));
      } catch (NumberFormatException ignored) {}
    }
    if (selected.isEmpty()) {
      renderer.printError("No valid seats selected.");
      renderer.pressEnterToContinue();
      return;
    }

    // Summary
    BigDecimal total = BigDecimal.ZERO;
    renderer.printMessage("\n========== Booking Summary ==========");
    renderer.printMessage("Event: " + event.getTitle());
    renderer.printMessage("Email: " + email);
    renderer.printMessage("-------------------------------------");
    for (Seat s : selected) {
      SeatCategory cat = rules.getCategoryForSection(s.getSection());
      Money price = rules.getPriceForCategory(cat);
      renderer.printMessage(
        "  Section " +
          s.getSection() +
          ", Row " +
          s.getRow() +
          ", Seat " +
          s.getNumber() +
          " [" +
          cat +
          "] – " +
          price.getCurrency().getSymbol() +
          " " +
          price.getAmount()
      );
      total = total.add(price.getAmount());
    }
    renderer.printMessage("-------------------------------------");
    renderer.printMessage(
      "Total: " + rules.currency().getSymbol() + " " + total
    );
    renderer.printMessage("=====================================");

    if (!renderer.readYesNo("Confirm this reservation?")) {
      renderer.printMessage("Reservation cancelled.");
      renderer.pressEnterToContinue();
      return;
    }

    try {
      List<UUID> seatIds = selected.stream().map(Seat::getId).toList();
      Reservation r = bookingService.hold(event.getId(), email, seatIds);
      renderer.printSuccess("Hold created! Reservation ID: " + r.getId());
      renderer.printReservations(List.of(r));
    } catch (ConflictException ce) {
      renderer.printError("Conflict: " + ce.getMessage());
    } catch (Exception e) {
      renderer.printError(e.getMessage());
    }
    renderer.pressEnterToContinue();
  }

  private void confirmFlow() {
    String email = renderer.readEmail("Enter customer email: ");
    List<Reservation> list = bookingService.getReservationsByEmail(email);
    if (list.isEmpty()) {
      renderer.printError("No reservations for this email.");
      renderer.pressEnterToContinue();
      return;
    }
    renderer.printReservations(list);
    int c = renderer.readInt("Enter choice (0 to cancel): ", 0, list.size());
    if (c == 0) {
      renderer.pressEnterToContinue();
      return;
    }

    Reservation r = list.get(c - 1);
    if (r.getStatus() != ReservationStatus.HOLD) {
      renderer.printError(
        "Only HOLD reservations can be confirmed. Current: " + r.getStatus()
      );
      renderer.pressEnterToContinue();
      return;
    }
    if (!renderer.readYesNo("Confirm this reservation?")) {
      renderer.pressEnterToContinue();
      return;
    }
    try {
      bookingService.confirm(r.getId());
      renderer.printSuccess("Reservation confirmed.");
    } catch (Exception e) {
      renderer.printError(e.getMessage());
    }
    renderer.pressEnterToContinue();
  }

  private void cancelFlow() {
    String email = renderer.readEmail("Enter customer email: ");
    List<Reservation> list = bookingService.getReservationsByEmail(email);
    if (list.isEmpty()) {
      renderer.printError("No reservations for this email.");
      renderer.pressEnterToContinue();
      return;
    }
    renderer.printReservations(list);
    int c = renderer.readInt("Enter choice (0 to cancel): ", 0, list.size());
    if (c == 0) {
      renderer.pressEnterToContinue();
      return;
    }

    Reservation r = list.get(c - 1);
    if (
      r.getStatus() == ReservationStatus.CANCELLED ||
      r.getStatus() == ReservationStatus.EXPIRED
    ) {
      renderer.printError("Already " + r.getStatus());
      renderer.pressEnterToContinue();
      return;
    }
    if (!renderer.readYesNo("Cancel this reservation?")) {
      renderer.pressEnterToContinue();
      return;
    }
    try {
      bookingService.cancel(r.getId());
      renderer.printSuccess("Reservation cancelled.");
    } catch (Exception e) {
      renderer.printError(e.getMessage());
    }
    renderer.pressEnterToContinue();
  }

  // ------------------------------------------------------------------
  //  Reports
  // ------------------------------------------------------------------
  private void reportsMenu() {
    boolean back = false;
    while (!back) {
      renderer.printReportsMenu();
      int c = renderer.readInt("", 1, 4);
      switch (c) {
        case 1 -> {
          var m = reportService.seatsPerEvent();
          if (m.isEmpty()) renderer.printMessage("No events.");
          else m.forEach((id, n) ->
            renderer.printMessage("Event " + id + ": " + n + " seats")
          );
          renderer.pressEnterToContinue();
        }
        case 2 -> {
          var m = reportService.reservationsPerEvent();
          if (m.isEmpty()) renderer.printMessage("No reservations.");
          else m.forEach((id, sc) ->
            renderer.printMessage("Event " + id + ": " + sc)
          );
          renderer.pressEnterToContinue();
        }
        case 3 -> {
          Event e = selectEvent();
          if (e == null) {
            renderer.pressEnterToContinue();
            break;
          }
          renderer.printReport(reportService.detailedSeatReport(e.getId()));
          renderer.pressEnterToContinue();
        }
        case 4 -> back = true;
      }
    }
  }

  // ------------------------------------------------------------------
  //  System Status
  // ------------------------------------------------------------------
  private void statusMenu() {
    boolean back = false;
    while (!back) {
      renderer.printStatusMenu();
      int c = renderer.readInt("", 1, 2);
      if (c == 1) {
        renderer.printStatus(
          venueRepo.count(),
          eventRepo.count(),
          seatRepo.count(),
          reservationRepo.count(),
          bookingService.getAllHolds().size()
        );
        renderer.pressEnterToContinue();
      } else back = true;
    }
  }
}
