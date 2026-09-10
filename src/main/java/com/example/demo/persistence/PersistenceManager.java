package com.example.demo.persistence;

import com.example.demo.repository.EventRepository;
import com.example.demo.repository.ReservationRepository;
import com.example.demo.repository.SeatRepository;
import com.example.demo.repository.VenueRepository;

/**
 * Manages automatic persistence: loads on startup, saves after every change.
 */
public class PersistenceManager {

  private final JsonStorage storage;
  private final VenueRepository venueRepository;
  private final EventRepository eventRepository;
  private final SeatRepository seatRepository;
  private final ReservationRepository reservationRepository;

  public PersistenceManager(
    JsonStorage storage,
    VenueRepository venueRepository,
    EventRepository eventRepository,
    SeatRepository seatRepository,
    ReservationRepository reservationRepository
  ) {
    this.storage = storage;
    this.venueRepository = venueRepository;
    this.eventRepository = eventRepository;
    this.seatRepository = seatRepository;
    this.reservationRepository = reservationRepository;
  }

  /**
   * Loads data from JSON file into repositories (if file exists).
   * Called at application startup.
   */
  public void load() {
    try {
      var ctx = storage.load();
      // Clear existing data
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

      // Populate
      ctx.venues.forEach(venueRepository::save);
      ctx.events.forEach(eventRepository::save);
      ctx.seats.forEach(seatRepository::save);
      ctx.reservations.forEach(reservationRepository::save);
      System.out.println("Data loaded from ticketing-data.json");
    } catch (Exception e) {
      // File does not exist or is corrupted – start fresh
      System.out.println("No existing data found. Starting fresh.");
    }
  }

  /**
   * Saves all current data to JSON file.
   * Called after every create/update/delete operation.
   */
  public void save() {
    try {
      var venues = venueRepository.findAll();
      var events = eventRepository.findAll();
      var seats = seatRepository.findAll();
      var reservations = reservationRepository.findAll();
      storage.save(venues, events, seats, reservations);
    } catch (Exception e) {
      System.err.println("Failed to auto-save: " + e.getMessage());
    }
  }
}
