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

  // ----- Venue -----
  public Venue createVenue(String name, String address, String timezone) {
    if (name == null || name.isBlank()) throw new ValidationException(
      "Venue name required"
    );
    var venue = new Venue(
      idGenerator.generateId(),
      name,
      address,
      ZoneId.of(timezone)
    );
    return venueRepository.save(venue);
  }

  public List<Venue> listVenues() {
    return venueRepository.findAll();
  }

  // ----- Event -----
  public Event createEvent(
    UUID venueId,
    String title,
    ZonedDateTime start,
    ZonedDateTime end,
    EventStatus status,
    Currency currency,
    PricingRules pricingRules
  ) {
    if (!venueRepository.existsById(venueId)) {
      throw new ValidationException("Venue not found");
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

  public Event createEvent(
    UUID venueId,
    String title,
    ZonedDateTime start,
    ZonedDateTime end,
    Currency currency,
    PricingRules pricingRules
  ) {
    return createEvent(
      venueId,
      title,
      start,
      end,
      EventStatus.SCHEDULED,
      currency,
      pricingRules
    );
  }

  public List<Event> listEvents() {
    return eventRepository.findAll();
  }

  // ----- Seat -----
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

  public List<Seat> listSeatsByVenue(UUID venueId) {
    return seatRepository.findByVenueId(venueId);
  }
}
