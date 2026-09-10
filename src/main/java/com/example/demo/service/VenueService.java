package com.example.demo.service;

import com.example.demo.domain.Event;
import com.example.demo.domain.PricingRules;
import com.example.demo.domain.Seat;
import com.example.demo.domain.Venue;
import com.example.demo.enums.Currency;
import com.example.demo.enums.EventStatus;
import com.example.demo.enums.SeatAttribute;
import com.example.demo.enums.SeatCategory;
import com.example.demo.exception.ValidationException;
import com.example.demo.repository.EventRepository;
import com.example.demo.repository.SeatRepository;
import com.example.demo.repository.VenueRepository;
import com.example.demo.util.IdGenerator;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class VenueService {

  private final VenueRepository venueRepository;
  private final EventRepository eventRepository;
  private final SeatRepository seatRepository;
  private final IdGenerator idGenerator;

  public VenueService(
    VenueRepository venueRepository,
    EventRepository eventRepository,
    SeatRepository seatRepository,
    IdGenerator idGenerator
  ) {
    this.venueRepository = venueRepository;
    this.eventRepository = eventRepository;
    this.seatRepository = seatRepository;
    this.idGenerator = idGenerator;
  }

  // -----------------------------------------------------------------
  //  Nested record for section layout
  // -----------------------------------------------------------------
  public record SectionLayout(String name, int rows, int seatsPerRow) {}

  // -----------------------------------------------------------------
  //  Venue
  // -----------------------------------------------------------------
  public Venue createVenue(String name, String address, String timezone) {
    if (name == null || name.isBlank()) {
      throw new ValidationException("Venue name required");
    }
    var venue = new Venue(
      idGenerator.generateId(),
      name,
      address,
      ZoneId.of(timezone)
    );
    return venueRepository.save(venue);
  }

  /**
   * Creates a venue and auto-generates all seats from the given layout.
   */
  public Venue createVenueWithSections(
    String name,
    String address,
    String timezone,
    List<SectionLayout> sections
  ) {
    Venue venue = createVenue(name, address, timezone);
    for (SectionLayout section : sections) {
      for (int r = 1; r <= section.rows(); r++) {
        for (int n = 1; n <= section.seatsPerRow(); n++) {
          Seat seat = new Seat(
            idGenerator.generateId(),
            venue.getId(),
            section.name(),
            String.valueOf(r),
            n,
            SeatCategory.STANDARD,
            Collections.emptySet()
          );
          seatRepository.save(seat);
        }
      }
    }
    return venue;
  }

  public Venue updateVenue(
    UUID venueId,
    String name,
    String address,
    String timezone
  ) {
    var venue = venueRepository
      .findById(venueId)
      .orElseThrow(() -> new ValidationException("Venue not found"));
    var updated = new Venue(venue.getId(), name, address, ZoneId.of(timezone));
    return venueRepository.save(updated);
  }

  public void deleteVenue(UUID venueId) {
    if (!venueRepository.existsById(venueId)) {
      throw new ValidationException("Venue not found");
    }
    venueRepository.deleteById(venueId);
  }

  public List<Venue> listVenues() {
    return venueRepository.findAll();
  }

  // -----------------------------------------------------------------
  //  Event
  // -----------------------------------------------------------------
  public Event createEvent(
    UUID venueId,
    String title,
    ZonedDateTime start,
    ZonedDateTime end,
    EventStatus status,
    Currency currency,
    PricingRules pricingRules
  ) {
    Venue venue = venueRepository
      .findById(venueId)
      .orElseThrow(() -> new ValidationException("Venue not found"));
    ZonedDateTime now = ZonedDateTime.now(venue.getTimezone());
    if (start.isBefore(now)) {
      throw new ValidationException("Start date/time cannot be in the past.");
    }
    var event = new Event(
      idGenerator.generateId(),
      venueId,
      title,
      start,
      end,
      status,
      currency,
      pricingRules
    );
    return eventRepository.save(event);
  }

  public Event updateEvent(
    UUID eventId,
    String title,
    ZonedDateTime start,
    ZonedDateTime end,
    EventStatus status,
    PricingRules pricingRules
  ) {
    var event = eventRepository
      .findById(eventId)
      .orElseThrow(() -> new ValidationException("Event not found"));
    Venue venue = venueRepository
      .findById(event.getVenueId())
      .orElseThrow(() -> new ValidationException("Venue not found"));
    ZonedDateTime now = ZonedDateTime.now(venue.getTimezone());
    if (start.isBefore(now)) {
      throw new ValidationException("Start date/time cannot be in the past.");
    }
    var updated = new Event(
      event.getId(),
      event.getVenueId(),
      title,
      start,
      end,
      status,
      event.getCurrency(),
      pricingRules
    );
    return eventRepository.save(updated);
  }

  public void deleteEvent(UUID eventId) {
    if (!eventRepository.existsById(eventId)) {
      throw new ValidationException("Event not found");
    }
    eventRepository.deleteById(eventId);
  }

  public List<Event> listEvents() {
    return eventRepository.findAll();
  }

  // -----------------------------------------------------------------
  //  Seat
  // -----------------------------------------------------------------
  public Seat createSeat(
    UUID venueId,
    String section,
    String row,
    int number,
    SeatCategory category,
    Set<SeatAttribute> attributes
  ) {
    if (!venueRepository.existsById(venueId)) {
      throw new ValidationException("Venue not found");
    }
    var seat = new Seat(
      idGenerator.generateId(),
      venueId,
      section,
      row,
      number,
      category,
      attributes
    );
    return seatRepository.save(seat);
  }

  public Seat updateSeat(
    UUID seatId,
    String section,
    String row,
    int number,
    SeatCategory category,
    Set<SeatAttribute> attributes
  ) {
    var seat = seatRepository
      .findById(seatId)
      .orElseThrow(() -> new ValidationException("Seat not found"));
    var updated = new Seat(
      seat.getId(),
      seat.getVenueId(),
      section,
      row,
      number,
      category,
      attributes
    );
    return seatRepository.save(updated);
  }

  public void deleteSeat(UUID seatId) {
    if (!seatRepository.existsById(seatId)) {
      throw new ValidationException("Seat not found");
    }
    seatRepository.deleteById(seatId);
  }

  public List<Seat> listSeatsByVenue(UUID venueId) {
    return seatRepository.findByVenueId(venueId);
  }
}
