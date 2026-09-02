package com.example.demo.persistence;

import java.time.ZoneId;
import java.util.List;
import java.util.stream.Collectors;

import com.example.demo.domain.DiscountType;          // <-- ADD
import com.example.demo.domain.Event;
import com.example.demo.domain.Money;
import com.example.demo.domain.Reservation;
import com.example.demo.domain.ReservationSeat;
import com.example.demo.domain.ReservationStatus;
import com.example.demo.domain.Seat;
import com.example.demo.domain.Venue;

public class DomainMapper {

    public static StorageDto toDto(List<Venue> venues, List<Event> events, List<Seat> seats, List<Reservation> reservations) {
        var dto = new StorageDto();
        dto.venues = venues.stream().map(v -> {
            var vd = new StorageDto.VenueDto();
            vd.id = v.getId();
            vd.name = v.getName();
            vd.address = v.getAddress();
            vd.timezone = v.getTimezone().getId();
            return vd;
        }).collect(Collectors.toList());

        dto.events = events.stream().map(e -> {
            var ed = new StorageDto.EventDto();
            ed.id = e.getId();
            ed.venueId = e.getVenueId();
            ed.title = e.getTitle();
            ed.startAt = e.getStartAt();
            ed.endAt = e.getEndAt();
            ed.status = e.getStatus();
            return ed;
        }).collect(Collectors.toList());

        dto.seats = seats.stream().map(s -> {
            var sd = new StorageDto.SeatDto();
            sd.id = s.getId();
            sd.venueId = s.getVenueId();
            sd.section = s.getSection();
            sd.row = s.getRow();
            sd.number = s.getNumber();
            sd.attributes = s.getAttributes();
            return sd;
        }).collect(Collectors.toList());

        dto.reservations = reservations.stream().map(r -> {
            var rd = new StorageDto.ReservationDto();
            rd.id = r.getId();
            rd.eventId = r.getEventId();
            rd.customerEmail = r.getCustomerEmail();
            rd.status = r.getStatus().name();
            rd.createdAt = r.getCreatedAt();
            rd.confirmedAt = r.getConfirmedAt().orElse(null);
            rd.holdExpiresAt = r.getHoldExpiresAt();
            rd.seats = r.getSeats().stream().map(rs -> {
                var rsd = new StorageDto.ReservationSeatDto();
                rsd.reservationId = rs.reservationId();
                rsd.seatId = rs.seatId();
                rsd.priceAmount = rs.price().amount();
                rsd.priceCurrency = rs.price().currency();
                rsd.discount = rs.discount().name();
                return rsd;
            }).collect(Collectors.toList());
            return rd;
        }).collect(Collectors.toList());

        return dto;
    }

    public static StorageContext fromDto(StorageDto dto) {
        var venues = dto.venues.stream()
                .map(vd -> new Venue(vd.id, vd.name, vd.address, ZoneId.of(vd.timezone)))
                .collect(Collectors.toList());

        var events = dto.events.stream()
                .map(ed -> new Event(ed.id, ed.venueId, ed.title, ed.startAt, ed.endAt, ed.status))
                .collect(Collectors.toList());

        var seats = dto.seats.stream()
                .map(sd -> new Seat(sd.id, sd.venueId, sd.section, sd.row, sd.number, sd.attributes))
                .collect(Collectors.toList());

        var reservations = dto.reservations.stream().map(rd -> {
            var seatsList = rd.seats.stream()
                    .map(rsd -> new ReservationSeat(
                            rsd.reservationId,
                            rsd.seatId,
                            new Money(rsd.priceAmount, rsd.priceCurrency),
                            DiscountType.valueOf(rsd.discount)
                    ))
                    .collect(Collectors.toList());
            return new Reservation(
                    rd.id,
                    rd.eventId,
                    rd.customerEmail,
                    ReservationStatus.valueOf(rd.status),
                    rd.createdAt,
                    rd.confirmedAt,
                    rd.holdExpiresAt,
                    seatsList
            );
        }).collect(Collectors.toList());

        return new StorageContext(venues, events, seats, reservations);
    }

    public static class StorageContext {
        public final List<Venue> venues;
        public final List<Event> events;
        public final List<Seat> seats;
        public final List<Reservation> reservations;

        public StorageContext(List<Venue> venues, List<Event> events, List<Seat> seats, List<Reservation> reservations) {
            this.venues = venues;
            this.events = events;
            this.seats = seats;
            this.reservations = reservations;
        }
    }
}