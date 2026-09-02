package com.example.demo.service;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

import com.example.demo.domain.Event;
import com.example.demo.domain.Seat;
import com.example.demo.domain.Venue;
import com.example.demo.exception.ValidationException;
import com.example.demo.repository.EventRepository;
import com.example.demo.repository.SeatRepository;
import com.example.demo.repository.VenueRepository;
import com.example.demo.util.IdGenerator;

public class VenueService {
    private final VenueRepository venueRepository;
    private final EventRepository eventRepository;
    private final SeatRepository seatRepository;
    private final IdGenerator idGenerator;

    public VenueService(VenueRepository venueRepository, EventRepository eventRepository,
                        SeatRepository seatRepository, IdGenerator idGenerator) {
        this.venueRepository = venueRepository;
        this.eventRepository = eventRepository;
        this.seatRepository = seatRepository;
        this.idGenerator = idGenerator;
    }

    public Venue createVenue(String name, String address, String timezone) {
        if (name == null || name.isBlank()) throw new ValidationException("Venue name required");
        var venue = new Venue(idGenerator.generateId(), name, address, ZoneId.of(timezone));
        return venueRepository.save(venue);
    }

    public Event createEvent(UUID venueId, String title, ZonedDateTime start, ZonedDateTime end) {
        if (!venueRepository.existsById(venueId)) {
            throw new ValidationException("Venue not found");
        }
        var event = new Event(idGenerator.generateId(), venueId, title, start, end, "SCHEDULED");
        return eventRepository.save(event);
    }

    public Seat createSeat(UUID venueId, String section, String row, int number, String attributes) {
        if (!venueRepository.existsById(venueId)) {
            throw new ValidationException("Venue not found");
        }
        var seat = new Seat(idGenerator.generateId(), venueId, section, row, number, attributes);
        return seatRepository.save(seat);
    }

    public List<Venue> listVenues() { return venueRepository.findAll(); }
    public List<Event> listEvents() { return eventRepository.findAll(); }
    public List<Seat> listSeatsByVenue(UUID venueId) { return seatRepository.findByVenueId(venueId); }
}