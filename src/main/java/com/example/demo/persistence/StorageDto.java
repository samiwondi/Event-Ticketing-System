package com.example.demo.persistence;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class StorageDto {
    public List<VenueDto> venues;
    public List<EventDto> events;
    public List<SeatDto> seats;
    public List<ReservationDto> reservations;

    public static class VenueDto {
        public UUID id;
        public String name;
        public String address;
        public String timezone;
    }

    public static class EventDto {
        public UUID id;
        public UUID venueId;
        public String title;
        public ZonedDateTime startAt;
        public ZonedDateTime endAt;
        public String status;
    }

    public static class SeatDto {
        public UUID id;
        public UUID venueId;
        public String section;
        public String row;
        public int number;
        public String attributes;
    }

    public static class ReservationDto {
        public UUID id;
        public UUID eventId;
        public String customerEmail;
        public String status;
        public Instant createdAt;
        public Instant confirmedAt;
        public Instant holdExpiresAt;
        public List<ReservationSeatDto> seats;
    }

    public static class ReservationSeatDto {
        public UUID reservationId;
        public UUID seatId;
        public BigDecimal priceAmount;
        public String priceCurrency;
        public String discount;
    }
}