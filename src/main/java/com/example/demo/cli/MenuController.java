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
import com.example.demo.persistence.PersistenceManager;
import com.example.demo.service.BookingService;
import com.example.demo.service.HoldExpirySweeper;
import com.example.demo.service.ReportService;
import com.example.demo.service.VenueService;
import com.example.demo.service.VenueService.SectionLayout;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public class MenuController {

  private final VenueService venueService;
  private final BookingService bookingService;
  private final ReportService reportService;
  private final HoldExpirySweeper sweeper;
  private final MenuRenderer renderer;
  private final PersistenceManager persistenceManager;
  private boolean running = true;

  public MenuController(
    VenueService venueService,
    BookingService bookingService,
    ReportService reportService,
    HoldExpirySweeper sweeper,
    MenuRenderer renderer,
    PersistenceManager persistenceManager
  ) {
    this.venueService = venueService;
    this.bookingService = bookingService;
    this.reportService = reportService;
    this.sweeper = sweeper;
    this.renderer = renderer;
    this.persistenceManager = persistenceManager;
  }

  public void run() {
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
          default -> renderer.printError("Invalid option.");
        }
      } catch (Exception e) {
        renderer.printError("Unexpected error: " + e.getMessage());
        renderer.pressEnterToContinue();
      }
    }
  }

  // -----------------------------------------------------------------
  //  SELECTION HELPERS
  // -----------------------------------------------------------------
  private Venue selectVenue() {
    var venues = venueService.listVenues();
    if (venues.isEmpty()) {
      renderer.printError("No venues available.");
      return null;
    }
    int choice = renderer.printAndSelectVenues(venues);
    if (choice == 0) return null;
    return venues.get(choice - 1);
  }

  private Event selectEvent() {
    var events = venueService.listEvents();
    if (events.isEmpty()) {
      renderer.printError("No events available.");
      return null;
    }
    int choice = renderer.printAndSelectEvents(events);
    if (choice == 0) return null;
    return events.get(choice - 1);
  }

  // -----------------------------------------------------------------
  //  HELPERS
  // -----------------------------------------------------------------
  private ZoneId selectTimezone() {
    var timezones = TimeZoneEnum.getAll();
    while (true) {
      renderer.printMessage("\nSelect timezone:");
      for (TimeZoneEnum tz : timezones) {
        System.out.println(tz.getId() + ". " + tz.getDisplayName());
      }
      int choice = renderer.readInt("Enter choice: ", 1, timezones.size());
      return TimeZoneEnum.fromId(choice).getZoneId();
    }
  }

  private Currency selectCurrency() {
    var currencies = Currency.values();
    while (true) {
      renderer.printMessage("\nSelect currency:");
      for (int i = 0; i < currencies.length; i++) {
        System.out.println((i + 1) + ". " + currencies[i]);
      }
      int choice = renderer.readInt("Enter choice: ", 1, currencies.length);
      return currencies[choice - 1];
    }
  }

  private BigDecimal askPrice(String prompt, BigDecimal defaultPrice) {
    while (true) {
      renderer.printMessage(
        prompt + " (press Enter for default " + defaultPrice + "): "
      );
      String input = renderer.readLine();
      if (input.isBlank()) return defaultPrice;
      try {
        return new BigDecimal(input);
      } catch (NumberFormatException e) {
        renderer.printError("Invalid number. Please enter a valid amount.");
      }
    }
  }

  private boolean confirm(String message) {
    return renderer.readYesNo(message);
  }

  /**
   * Multi-select sections by index. Returns set of selected section names.
   */
  private Set<String> multiSelectSections(List<String> sections) {
    for (int i = 0; i < sections.size(); i++) {
      System.out.println((i + 1) + ". " + sections.get(i));
    }
    System.out.print("Enter comma-separated numbers (Enter for none): ");
    String input = renderer.readLine();
    Set<String> selected = new HashSet<>();
    if (input == null || input.isBlank()) return selected;
    for (String part : input.split(",")) {
      try {
        int idx = Integer.parseInt(part.trim()) - 1;
        if (idx >= 0 && idx < sections.size()) {
          selected.add(sections.get(idx));
        }
      } catch (NumberFormatException ignored) {}
    }
    return selected;
  }

  /**
   * Show a list of reservations for a customer (with total price) and let them pick one.
   * Returns the selected reservation, or null if cancelled or none.
   */
  private Reservation selectReservationByEmail(String prompt) {
    String email = renderer.readEmail("Enter customer email: ");
    var userReservations = bookingService.getReservationsByEmail(email);
    if (userReservations.isEmpty()) {
      renderer.printError("No reservations found for this email.");
      return null;
    }

    renderer.printMessage("\nReservations for " + email + ":");
    System.out.println("0. Cancel");
    for (int i = 0; i < userReservations.size(); i++) {
      Reservation r = userReservations.get(i);
      BigDecimal total = BigDecimal.ZERO;
      String symbol = "";
      for (ReservationSeat rs : r.getSeats()) {
        total = total.add(rs.price().amount());
        if (symbol.isEmpty()) symbol = rs.price().currency().getSymbol();
      }
      System.out.printf(
        "%d. ID: %s | Event: %s | Status: %s | Seats: %d | Total: %s %s%n",
        i + 1,
        r.getId(),
        r.getEventId(),
        r.getStatus(),
        r.getSeats().size(),
        symbol,
        total
      );
    }
    int choice = renderer.readInt("Enter choice: ", 0, userReservations.size());
    if (choice == 0) return null;
    return userReservations.get(choice - 1);
  }

  // -----------------------------------------------------------------
  //  VENUE MANAGEMENT
  // -----------------------------------------------------------------
  private void venueManagement() {
    boolean back = false;
    while (!back) {
      renderer.printVenueMenu();
      int choice = renderer.readInt("", 1, 5);
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

          int sectionCount = renderer.readInt("How many sections? ", 1, 26);
          int rowsPerSection = renderer.readInt(
            "How many rows per section? ",
            1,
            500
          );
          int seatsPerRow = renderer.readInt(
            "How many seats per row? ",
            1,
            500
          );

          try {
            List<SectionLayout> sections = new ArrayList<>();
            for (int i = 0; i < sectionCount; i++) {
              String sectionName = String.valueOf((char) ('A' + i));
              sections.add(
                new SectionLayout(sectionName, rowsPerSection, seatsPerRow)
              );
            }
            var venue = venueService.createVenueWithSections(
              name,
              address,
              zoneId.getId(),
              sections
            );
            int totalSeats = sectionCount * rowsPerSection * seatsPerRow;
            renderer.printSuccess(
              "Venue created: " +
                venue.getId() +
                " (" +
                sectionCount +
                " sections, " +
                rowsPerSection +
                " rows × " +
                seatsPerRow +
                " seats = " +
                totalSeats +
                " seats)"
            );
            persistenceManager.save();
          } catch (Exception e) {
            renderer.printError(e.getMessage());
          }
          renderer.pressEnterToContinue();
        }
        case 3 -> {
          Venue venue = selectVenue();
          if (venue == null) {
            renderer.pressEnterToContinue();
            break;
          }
          renderer.printMessage("\nWhat do you want to update?");
          System.out.println("0. Cancel");
          System.out.println("1. Name (current: " + venue.getName() + ")");
          System.out.println(
            "2. Address (current: " + venue.getAddress() + ")"
          );
          System.out.println(
            "3. Timezone (current: " + venue.getTimezone() + ")"
          );
          System.out.println("4. All fields");
          int fieldChoice = renderer.readInt("Enter choice: ", 0, 4);
          if (fieldChoice == 0) {
            renderer.pressEnterToContinue();
            break;
          }
          String newName = venue.getName();
          String newAddress = venue.getAddress();
          String newTimezone = venue.getTimezone().getId();
          boolean changed = false;
          try {
            switch (fieldChoice) {
              case 1 -> {
                renderer.printMessage("Enter new name: ");
                newName = renderer.readLine();
                changed = true;
              }
              case 2 -> {
                renderer.printMessage("Enter new address: ");
                newAddress = renderer.readLine();
                changed = true;
              }
              case 3 -> {
                ZoneId tz = selectTimezone();
                newTimezone = tz.getId();
                changed = true;
              }
              case 4 -> {
                renderer.printMessage(
                  "Enter new name (current: " + venue.getName() + "): "
                );
                newName = renderer.readLine();
                renderer.printMessage(
                  "Enter new address (current: " + venue.getAddress() + "): "
                );
                newAddress = renderer.readLine();
                ZoneId tz = selectTimezone();
                newTimezone = tz.getId();
                changed = true;
              }
            }
            if (changed) {
              var updated = venueService.updateVenue(
                venue.getId(),
                newName,
                newAddress,
                newTimezone
              );
              renderer.printSuccess("Venue updated.");
              persistenceManager.save();
            }
          } catch (Exception e) {
            renderer.printError(e.getMessage());
          }
          renderer.pressEnterToContinue();
        }
        case 4 -> {
          Venue venue = selectVenue();
          if (venue == null) {
            renderer.pressEnterToContinue();
            break;
          }
          if (
            !confirm(
              "Are you sure you want to delete this venue? All associated events and seats will also be removed."
            )
          ) {
            renderer.pressEnterToContinue();
            break;
          }
          try {
            var seats = venueService.listSeatsByVenue(venue.getId());
            for (Seat s : seats) {
              venueService.deleteSeat(s.getId());
            }
            var events = venueService
              .listEvents()
              .stream()
              .filter(e -> e.getVenueId().equals(venue.getId()))
              .collect(Collectors.toList());
            for (Event e : events) {
              venueService.deleteEvent(e.getId());
            }
            venueService.deleteVenue(venue.getId());
            renderer.printSuccess("Venue and all associated data deleted.");
            persistenceManager.save();
          } catch (Exception e) {
            renderer.printError(e.getMessage());
          }
          renderer.pressEnterToContinue();
        }
        case 5 -> back = true;
        default -> renderer.printError("Invalid option.");
      }
    }
  }

  // -----------------------------------------------------------------
  //  EVENT MANAGEMENT
  // -----------------------------------------------------------------
  private void eventManagement() {
    boolean back = false;
    while (!back) {
      renderer.printEventMenu();
      int choice = renderer.readInt("", 1, 7);
      switch (choice) {
        case 1 -> {
          var events = venueService.listEvents();
          renderer.printEvents(events);
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
          ZoneId venueZone = venue.getTimezone();

          LocalDate startDate = renderer.readLocalDate(
            "Enter start date (yyyy-MM-dd): "
          );
          int days = renderer.readInt(
            "Enter duration in days (0 if same day): ",
            0,
            365
          );
          LocalTime startTime = renderer.readLocalTime(
            "Enter start time (HH:mm, e.g., 20:00): "
          );
          int hours = renderer.readInt("Enter duration in hours: ", 0, 24);
          int minutes = renderer.readInt("Enter duration in minutes: ", 0, 59);

          try {
            ZonedDateTime start = startDate.atTime(startTime).atZone(venueZone);
            ZonedDateTime end = start
              .plusDays(days)
              .plusHours(hours)
              .plusMinutes(minutes);
            if (!end.isAfter(start)) {
              renderer.printError("Total duration must be > 0.");
              renderer.pressEnterToContinue();
              break;
            }
            ZonedDateTime now = ZonedDateTime.now(venueZone);
            if (start.isBefore(now)) {
              renderer.printError("Start date/time cannot be in the past.");
              renderer.pressEnterToContinue();
              break;
            }

            List<String> sections = venueService
              .listSeatsByVenue(venue.getId())
              .stream()
              .map(Seat::getSection)
              .distinct()
              .sorted()
              .collect(Collectors.toList());

            if (sections.isEmpty()) {
              renderer.printError("This venue has no seats.");
              renderer.pressEnterToContinue();
              break;
            }

            Currency currency = selectCurrency();
            BigDecimal defaultPrice = BigDecimal.valueOf(50.00);
            BigDecimal standardPrice = askPrice(
              "Enter standard seat price",
              defaultPrice
            );

            Map<String, SeatCategory> sectionCategories = new HashMap<>();
            for (String s : sections)
              sectionCategories.put(s, SeatCategory.STANDARD);

            PricingRules rules = new PricingRules(
              currency,
              standardPrice
            ).withSectionCategories(sectionCategories);

            if (renderer.readYesNo("Does this event have VIP sections?")) {
              renderer.printMessage("\nWhich sections are VIP?");
              Set<String> vipSections = multiSelectSections(sections);
              if (!vipSections.isEmpty()) {
                BigDecimal vipPrice = askPrice(
                  "Enter VIP seat price",
                  standardPrice.multiply(BigDecimal.valueOf(2))
                );
                rules = rules.withCategoryPrice(SeatCategory.VIP, vipPrice);
                Map<String, SeatCategory> updated = new HashMap<>(
                  rules.sectionCategories()
                );
                for (String s : vipSections) updated.put(s, SeatCategory.VIP);
                rules = rules.withSectionCategories(updated);
              }
            }

            if (renderer.readYesNo("Does this event have VVIP sections?")) {
              renderer.printMessage("\nWhich sections are VVIP?");
              Set<String> vvipSections = multiSelectSections(sections);
              if (!vvipSections.isEmpty()) {
                BigDecimal vvipPrice = askPrice(
                  "Enter VVIP seat price",
                  standardPrice.multiply(BigDecimal.valueOf(3))
                );
                rules = rules.withCategoryPrice(SeatCategory.VVIP, vvipPrice);
                Map<String, SeatCategory> updated = new HashMap<>(
                  rules.sectionCategories()
                );
                for (String s : vvipSections) updated.put(s, SeatCategory.VVIP);
                rules = rules.withSectionCategories(updated);
              }
            }

            var event = venueService.createEvent(
              venue.getId(),
              title,
              start,
              end,
              EventStatus.SCHEDULED,
              currency,
              rules
            );
            renderer.printSuccess("Event created: " + event.getId());
            persistenceManager.save();
          } catch (Exception e) {
            renderer.printError("Error: " + e.getMessage());
          }
          renderer.pressEnterToContinue();
        }
        case 3 -> {
          Event event = selectEvent();
          if (event == null) {
            renderer.pressEnterToContinue();
            break;
          }
          renderer.printReport(
            "Event: " +
              event.getTitle() +
              "\nVenue: " +
              event.getVenueId() +
              "\nStart: " +
              event.getStartAt() +
              "\nEnd: " +
              event.getEndAt() +
              "\nCurrency: " +
              event.getCurrency() +
              "\nPricing: " +
              event.getPricingRules()
          );
          renderer.pressEnterToContinue();
        }
        case 4 -> {
          Event event = selectEvent();
          if (event == null) {
            renderer.pressEnterToContinue();
            break;
          }
          var seats = venueService.listSeatsByVenue(event.getVenueId());
          renderer.printSeats(seats);
          renderer.pressEnterToContinue();
        }
        case 5 -> {
          Event event = selectEvent();
          if (event == null) {
            renderer.pressEnterToContinue();
            break;
          }
          renderer.printMessage("\nWhat do you want to update?");
          System.out.println("0. Cancel");
          System.out.println("1. Title (current: " + event.getTitle() + ")");
          System.out.println(
            "2. Start date (current: " +
              event.getStartAt().format(DateTimeFormatter.ISO_LOCAL_DATE) +
              ")"
          );
          System.out.println(
            "3. Duration in days (current: " +
              java.time.Duration.between(
                event.getStartAt().toLocalDate().atStartOfDay(),
                event.getEndAt().toLocalDate().atStartOfDay()
              ).toDays() +
              " days)"
          );
          System.out.println(
            "4. Start time (current: " +
              event.getStartAt().format(DateTimeFormatter.ofPattern("HH:mm")) +
              ")"
          );
          System.out.println(
            "5. Duration in hours/minutes (current end time: " +
              event.getEndAt().format(DateTimeFormatter.ofPattern("HH:mm")) +
              ")"
          );
          System.out.println("6. All fields");
          int fieldChoice = renderer.readInt("Enter choice: ", 0, 6);
          if (fieldChoice == 0) {
            renderer.pressEnterToContinue();
            break;
          }
          String newTitle = event.getTitle();
          LocalDate newStartDate = event.getStartAt().toLocalDate();
          int newDays = (int) java.time.Duration.between(
            event.getStartAt().toLocalDate().atStartOfDay(),
            event.getEndAt().toLocalDate().atStartOfDay()
          ).toDays();
          LocalTime newStartTime = event.getStartAt().toLocalTime();
          long totalMinutes = java.time.Duration.between(
            event.getStartAt(),
            event.getEndAt()
          ).toMinutes();
          int currentHours = (int) (totalMinutes / 60);
          int currentMinutes = (int) (totalMinutes % 60);
          int newHours = currentHours,
            newMinutes = currentMinutes;
          boolean changed = false;
          try {
            ZoneId venueZone = event.getStartAt().getZone();
            switch (fieldChoice) {
              case 1 -> {
                renderer.printMessage("Enter new title: ");
                newTitle = renderer.readLine();
                changed = true;
              }
              case 2 -> {
                newStartDate = renderer.readLocalDate(
                  "Enter new start date (yyyy-MM-dd): "
                );
                changed = true;
              }
              case 3 -> {
                newDays = renderer.readInt(
                  "Enter new duration in days: ",
                  0,
                  365
                );
                changed = true;
              }
              case 4 -> {
                newStartTime = renderer.readLocalTime(
                  "Enter new start time (HH:mm): "
                );
                changed = true;
              }
              case 5 -> {
                newHours = renderer.readInt(
                  "Enter new duration in hours: ",
                  0,
                  24
                );
                newMinutes = renderer.readInt(
                  "Enter new duration in minutes: ",
                  0,
                  59
                );
                changed = true;
              }
              case 6 -> {
                renderer.printMessage("Enter new title: ");
                newTitle = renderer.readLine();
                newStartDate = renderer.readLocalDate(
                  "Enter new start date (yyyy-MM-dd): "
                );
                newDays = renderer.readInt(
                  "Enter new duration in days: ",
                  0,
                  365
                );
                newStartTime = renderer.readLocalTime(
                  "Enter new start time (HH:mm): "
                );
                newHours = renderer.readInt(
                  "Enter new duration in hours: ",
                  0,
                  24
                );
                newMinutes = renderer.readInt(
                  "Enter new duration in minutes: ",
                  0,
                  59
                );
                changed = true;
              }
            }
            if (changed) {
              ZonedDateTime newStart = newStartDate
                .atTime(newStartTime)
                .atZone(venueZone);
              ZonedDateTime newEnd = newStart
                .plusDays(newDays)
                .plusHours(newHours)
                .plusMinutes(newMinutes);
              if (!newEnd.isAfter(newStart)) {
                renderer.printError("Total duration must be > 0.");
                renderer.pressEnterToContinue();
                break;
              }
              ZonedDateTime now = ZonedDateTime.now(venueZone);
              if (newStart.isBefore(now)) {
                renderer.printError("Start date/time cannot be in the past.");
                renderer.pressEnterToContinue();
                break;
              }
              var updated = venueService.updateEvent(
                event.getId(),
                newTitle,
                newStart,
                newEnd,
                event.getStatus(),
                event.getPricingRules()
              );
              renderer.printSuccess("Event updated.");
              persistenceManager.save();
            }
          } catch (Exception e) {
            renderer.printError("Error: " + e.getMessage());
          }
          renderer.pressEnterToContinue();
        }
        case 6 -> {
          Event event = selectEvent();
          if (event == null) {
            renderer.pressEnterToContinue();
            break;
          }
          if (!confirm("Are you sure you want to delete this event?")) {
            renderer.pressEnterToContinue();
            break;
          }
          try {
            var reservations = bookingService.getReservationsForEvent(
              event.getId()
            );
            for (Reservation r : reservations) {
              bookingService.cancelReservation(r.getId());
            }
            venueService.deleteEvent(event.getId());
            renderer.printSuccess("Event deleted.");
            persistenceManager.save();
          } catch (Exception e) {
            renderer.printError(e.getMessage());
          }
          renderer.pressEnterToContinue();
        }
        case 7 -> back = true;
        default -> renderer.printError("Invalid option.");
      }
    }
  }

  // -----------------------------------------------------------------
  //  RESERVATION MANAGEMENT
  // -----------------------------------------------------------------
  private void reservationManagement() {
    boolean back = false;
    while (!back) {
      renderer.printReservationMenu();
      int choice = renderer.readInt("", 1, 6);
      switch (choice) {
        case 1 -> {
          // ---------- 1. Select event ----------
          Event event = selectEvent();
          if (event == null) {
            renderer.pressEnterToContinue();
            break;
          }

          // ---------- 2. Email verification ----------
          String email = renderer.readEmail("Enter customer email: ");

          // ---------- 3. Available seats for this event ----------
          var allSeats = venueService.listSeatsByVenue(event.getVenueId());
          if (allSeats.isEmpty()) {
            renderer.printError("No seats available for this event.");
            renderer.pressEnterToContinue();
            break;
          }
          var reservations = bookingService.getReservationsForEvent(
            event.getId()
          );
          Set<UUID> reservedSeatIds = reservations
            .stream()
            .filter(
              r ->
                r.getStatus() == ReservationStatus.HOLD ||
                r.getStatus() == ReservationStatus.CONFIRMED
            )
            .flatMap(r -> r.getSeats().stream().map(ReservationSeat::seatId))
            .collect(Collectors.toSet());

          var availableSeats = allSeats
            .stream()
            .filter(s -> !reservedSeatIds.contains(s.getId()))
            .collect(Collectors.toList());

          if (availableSeats.isEmpty()) {
            renderer.printError("All seats are reserved for this event.");
            renderer.pressEnterToContinue();
            break;
          }

          // ---------- 4. Select section ----------
          List<String> sections = availableSeats
            .stream()
            .map(Seat::getSection)
            .distinct()
            .sorted()
            .collect(Collectors.toList());

          PricingRules rules = event.getPricingRules();

          renderer.printMessage("\nSelect a section:");
          for (int i = 0; i < sections.size(); i++) {
            String section = sections.get(i);
            SeatCategory category = rules.getCategoryForSection(section);
            String label = (category == SeatCategory.STANDARD)
              ? section
              : section + " (" + category + ")";
            System.out.println((i + 1) + ". " + label);
          }
          int sectionChoice = renderer.readInt(
            "Enter choice: ",
            1,
            sections.size()
          );
          String selectedSection = sections.get(sectionChoice - 1);

          // ---------- 5. Select row ----------
          List<String> rows = availableSeats
            .stream()
            .filter(s -> s.getSection().equals(selectedSection))
            .map(Seat::getRow)
            .distinct()
            .sorted()
            .collect(Collectors.toList());

          renderer.printMessage("\nSelect a row:");
          for (int i = 0; i < rows.size(); i++) {
            System.out.println((i + 1) + ". Row " + rows.get(i));
          }
          int rowChoice = renderer.readInt("Enter choice: ", 1, rows.size());
          String selectedRow = rows.get(rowChoice - 1);

          // ---------- 6. Select seat number(s) ----------
          List<Seat> seatsInRow = availableSeats
            .stream()
            .filter(
              s ->
                s.getSection().equals(selectedSection) &&
                s.getRow().equals(selectedRow)
            )
            .sorted(Comparator.comparingInt(Seat::getNumber))
            .collect(Collectors.toList());

          renderer.printMessage("\nSelect seat number(s):");
          for (int i = 0; i < seatsInRow.size(); i++) {
            System.out.println(
              (i + 1) + ". Seat " + seatsInRow.get(i).getNumber()
            );
          }
          renderer.printMessage(
            "Enter comma-separated numbers (e.g., 1,3,5): "
          );
          String seatIdxStr = renderer.readLine();

          List<Seat> selectedSeats = new ArrayList<>();
          for (String part : seatIdxStr.split(",")) {
            try {
              int idx = Integer.parseInt(part.trim()) - 1;
              if (idx >= 0 && idx < seatsInRow.size()) {
                selectedSeats.add(seatsInRow.get(idx));
              }
            } catch (NumberFormatException ignored) {}
          }
          if (selectedSeats.isEmpty()) {
            renderer.printError("No valid seats selected.");
            renderer.pressEnterToContinue();
            break;
          }

          // ---------- 7. Show total price ----------
          BigDecimal total = BigDecimal.ZERO;
          renderer.printMessage("\n========== Booking Summary ==========");
          renderer.printMessage("Event: " + event.getTitle());
          renderer.printMessage("Email: " + email);
          renderer.printMessage("-------------------------------------");
          for (Seat s : selectedSeats) {
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
                "]" +
                " = " +
                price.currency().getSymbol() +
                " " +
                price.amount()
            );
            total = total.add(price.amount());
          }
          renderer.printMessage("-------------------------------------");
          renderer.printMessage(
            "Total: " + rules.currency().getSymbol() + " " + total
          );
          renderer.printMessage("=====================================");

          // ---------- 8. Confirmation ----------
          if (!renderer.readYesNo("Confirm this reservation?")) {
            renderer.printMessage("Reservation cancelled.");
            renderer.pressEnterToContinue();
            break;
          }

          // ---------- 9. Create hold ----------
          try {
            List<UUID> seatIds = selectedSeats
              .stream()
              .map(Seat::getId)
              .collect(Collectors.toList());
            var reservation = bookingService.holdSeats(
              event.getId(),
              email,
              seatIds
            );
            renderer.printSuccess(
              "Hold created! Reservation ID: " + reservation.getId()
            );
            renderer.printReservations(List.of(reservation));
            persistenceManager.save();
          } catch (Exception e) {
            renderer.printError(e.getMessage());
          }
          renderer.pressEnterToContinue();
        }
        case 2 -> {
          // ---------- Confirm reservation by email ----------
          Reservation selected = selectReservationByEmail("confirm");
          if (selected == null) {
            renderer.pressEnterToContinue();
            break;
          }
          if (selected.getStatus() != ReservationStatus.HOLD) {
            renderer.printError(
              "Only HOLD reservations can be confirmed. Current status: " +
                selected.getStatus()
            );
            renderer.pressEnterToContinue();
            break;
          }
          if (!renderer.readYesNo("Confirm this reservation?")) {
            renderer.pressEnterToContinue();
            break;
          }
          try {
            bookingService.confirmReservation(selected.getId());
            renderer.printSuccess("Reservation confirmed.");
            persistenceManager.save();
          } catch (Exception e) {
            renderer.printError(e.getMessage());
          }
          renderer.pressEnterToContinue();
        }
        case 3 -> {
          // ---------- Cancel reservation by email ----------
          Reservation selected = selectReservationByEmail("cancel");
          if (selected == null) {
            renderer.pressEnterToContinue();
            break;
          }
          if (
            selected.getStatus() == ReservationStatus.CANCELLED ||
            selected.getStatus() == ReservationStatus.EXPIRED
          ) {
            renderer.printError(
              "This reservation is already " + selected.getStatus() + "."
            );
            renderer.pressEnterToContinue();
            break;
          }
          if (!renderer.readYesNo("Cancel this reservation?")) {
            renderer.pressEnterToContinue();
            break;
          }
          try {
            bookingService.cancelReservation(selected.getId());
            renderer.printSuccess("Reservation cancelled.");
            persistenceManager.save();
          } catch (Exception e) {
            renderer.printError(e.getMessage());
          }
          renderer.pressEnterToContinue();
        }
        case 4 -> {
          Event event = selectEvent();
          if (event == null) {
            renderer.pressEnterToContinue();
            break;
          }
          var reservations = bookingService.getReservationsForEvent(
            event.getId()
          );
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

  // -----------------------------------------------------------------
  //  REPORTS
  // -----------------------------------------------------------------
  private void reportsMenu() {
    boolean back = false;
    while (!back) {
      renderer.printReportsMenu();
      int choice = renderer.readInt("", 1, 4);
      switch (choice) {
        case 1 -> {
          var map = reportService.seatsPerEvent();
          if (map.isEmpty()) renderer.printMessage("No events found.");
          else map.forEach((id, count) ->
            renderer.printMessage("Event " + id + ": " + count + " seats")
          );
          renderer.pressEnterToContinue();
        }
        case 2 -> {
          var map = reportService.reservationsPerEvent();
          if (map.isEmpty()) renderer.printMessage("No reservations found.");
          else map.forEach((id, statusCount) ->
            renderer.printMessage("Event " + id + ": " + statusCount)
          );
          renderer.pressEnterToContinue();
        }
        case 3 -> {
          Event event = selectEvent();
          if (event == null) {
            renderer.pressEnterToContinue();
            break;
          }
          String report = reportService.detailedSeatReport(event.getId());
          renderer.printReport(report);
          renderer.pressEnterToContinue();
        }
        case 4 -> back = true;
        default -> renderer.printError("Invalid option.");
      }
    }
  }

  // -----------------------------------------------------------------
  //  SYSTEM STATUS
  // -----------------------------------------------------------------
  private void statusMenu() {
    boolean back = false;
    while (!back) {
      renderer.printStatusMenu();
      int choice = renderer.readInt("", 1, 2);
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
}
