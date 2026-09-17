package com.example.demo.service;

import com.example.demo.domain.Event;
import com.example.demo.domain.PricingRules;
import com.example.demo.domain.Seat;
import com.example.demo.domain.Venue;
import com.example.demo.enums.Currency;
import com.example.demo.enums.EventStatus;
import com.example.demo.enums.SeatAttribute;
import com.example.demo.enums.SeatCategory;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.repository.jpa.EventJpaRepository;
import com.example.demo.repository.jpa.SeatJpaRepository;
import com.example.demo.repository.jpa.VenueJpaRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class VenueService {

  private final VenueJpaRepository venueRepository;
  private final EventJpaRepository eventRepository;
  private final SeatJpaRepository seatRepository;

  public VenueService(
    VenueJpaRepository venueRepository,
    EventJpaRepository eventRepository,
    SeatJpaRepository seatRepository
  ) {
    this.venueRepository = venueRepository;
    this.eventRepository = eventRepository;
    this.seatRepository = seatRepository;
  }

  public record SectionLayout(String name, int rows, int seatsPerRow) {}

  // ------------------------------------------------------------------
  //  Venue
  // ------------------------------------------------------------------
  @Transactional
  public Venue createVenue(String name, String address, String timezone) {
    Venue venue = new Venue(UUID.randomUUID(), name, address, timezone);
    return venueRepository.save(venue);
  }

  @Transactional
  public Venue createVenueWithSections(
    String name,
    String address,
    String timezone,
    List<SectionLayout> sections
  ) {
    Venue venue = new Venue(UUID.randomUUID(), name, address, timezone);
    venueRepository.save(venue);

    // Build all seats in memory first
    List<Seat> allSeats = new ArrayList<>();
    for (SectionLayout section : sections) {
      for (int r = 1; r <= section.rows(); r++) {
        for (int n = 1; n <= section.seatsPerRow(); n++) {
          allSeats.add(
            new Seat(
              UUID.randomUUID(),
              venue.getId(),
              section.name(),
              String.valueOf(r),
              n,
              SeatCategory.STANDARD,
              Collections.emptySet()
            )
          );
        }
      }
    }

    // Save in chunks of 500 so the transaction stays fast
    int chunkSize = 500;
    for (int i = 0; i < allSeats.size(); i += chunkSize) {
      int end = Math.min(i + chunkSize, allSeats.size());
      seatRepository.saveAll(allSeats.subList(i, end));
      seatRepository.flush();
    }

    return venue;
  }

  @Transactional
  public Venue updateVenue(
    UUID venueId,
    String name,
    String address,
    String timezone
  ) {
    Venue venue = venueRepository
      .findById(venueId)
      .orElseThrow(() ->
        new ResourceNotFoundException("Venue not found: " + venueId)
      );
    Venue updated = new Venue(venue.getId(), name, address, timezone);
    return venueRepository.save(updated);
  }

  @Transactional
  public void deleteVenue(UUID venueId) {
    if (!venueRepository.existsById(venueId)) {
      throw new ResourceNotFoundException("Venue not found: " + venueId);
    }
    venueRepository.deleteById(venueId);
  }

  @Transactional(readOnly = true)
  public List<Venue> listVenues() {
    return venueRepository.findAll();
  }

  // ------------------------------------------------------------------
  //  Event
  // ------------------------------------------------------------------
  @Transactional
  public Event createEvent(
    UUID venueId,
    String title,
    Instant start,
    Instant end,
    EventStatus status,
    Currency currency,
    PricingRules pricingRules
  ) {
    Venue venue = venueRepository
      .findById(venueId)
      .orElseThrow(() ->
        new ResourceNotFoundException("Venue not found: " + venueId)
      );
    if (start.isBefore(Instant.now())) {
      throw new IllegalArgumentException(
        "Start date/time cannot be in the past."
      );
    }
    Event event = new Event(
      UUID.randomUUID(),
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

  @Transactional
  public Event updateEvent(
    UUID eventId,
    String title,
    Instant start,
    Instant end,
    EventStatus status,
    PricingRules pricingRules
  ) {
    Event event = eventRepository
      .findById(eventId)
      .orElseThrow(() ->
        new ResourceNotFoundException("Event not found: " + eventId)
      );
    if (start.isBefore(Instant.now())) {
      throw new IllegalArgumentException(
        "Start date/time cannot be in the past."
      );
    }
    Event updated = new Event(
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

  @Transactional
  public void deleteEvent(UUID eventId) {
    if (!eventRepository.existsById(eventId)) {
      throw new ResourceNotFoundException("Event not found: " + eventId);
    }
    eventRepository.deleteById(eventId);
  }

  @Transactional(readOnly = true)
  public List<Event> listEvents() {
    return eventRepository.findAll();
  }

  // ------------------------------------------------------------------
  //  Seat
  // ------------------------------------------------------------------
  @Transactional
  public Seat createSeat(
    UUID venueId,
    String section,
    String row,
    int number,
    SeatCategory category,
    Set<SeatAttribute> attributes
  ) {
    if (!venueRepository.existsById(venueId)) {
      throw new ResourceNotFoundException("Venue not found: " + venueId);
    }
    Seat seat = new Seat(
      UUID.randomUUID(),
      venueId,
      section,
      row,
      number,
      category,
      attributes
    );
    return seatRepository.save(seat);
  }

  @Transactional
  public Seat updateSeat(
    UUID seatId,
    String section,
    String row,
    int number,
    SeatCategory category,
    Set<SeatAttribute> attributes
  ) {
    Seat seat = seatRepository
      .findById(seatId)
      .orElseThrow(() ->
        new ResourceNotFoundException("Seat not found: " + seatId)
      );
    Seat updated = new Seat(
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

  @Transactional
  public void deleteSeat(UUID seatId) {
    if (!seatRepository.existsById(seatId)) {
      throw new ResourceNotFoundException("Seat not found: " + seatId);
    }
    seatRepository.deleteById(seatId);
  }

  @Transactional(readOnly = true)
  public List<Seat> listSeatsByVenue(UUID venueId) {
    return seatRepository.findByVenueIdOrderBySectionAscRowAscNumberAsc(
      venueId
    );
  }
}
