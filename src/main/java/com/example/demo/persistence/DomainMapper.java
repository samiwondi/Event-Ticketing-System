package com.example.demo.persistence;

import com.example.demo.domain.*;
import com.example.demo.enums.Currency;
import com.example.demo.enums.DiscountType;
import com.example.demo.enums.EventStatus;
import com.example.demo.enums.ReservationStatus;
import com.example.demo.enums.SeatCategory;
import java.time.ZoneId;
import java.util.EnumMap;
import java.util.List;
import java.util.stream.Collectors;

public class DomainMapper {

  public static StorageDto toDto(
    List<Venue> venues,
    List<Event> events,
    List<Seat> seats,
    List<Reservation> reservations
  ) {
    StorageDto dto = new StorageDto();

    dto.venues = venues
      .stream()
      .map(v -> {
        StorageDto.VenueDto vd = new StorageDto.VenueDto();
        vd.id = v.getId();
        vd.name = v.getName();
        vd.address = v.getAddress();
        vd.timezone = v.getTimezone().getId();
        return vd;
      })
      .collect(Collectors.toList());

    dto.events = events
      .stream()
      .map(e -> {
        StorageDto.EventDto ed = new StorageDto.EventDto();
        ed.id = e.getId();
        ed.venueId = e.getVenueId();
        ed.title = e.getTitle();
        ed.startAt = e.getStartAt();
        ed.endAt = e.getEndAt();
        ed.status = e.getStatus();
        PricingRules rules = e.getPricingRules();
        ed.currency = rules.currency();
        ed.pricingRules = rules; // direct assignment
        return ed;
      })
      .collect(Collectors.toList());

    dto.seats = seats
      .stream()
      .map(s -> {
        StorageDto.SeatDto sd = new StorageDto.SeatDto();
        sd.id = s.getId();
        sd.venueId = s.getVenueId();
        sd.section = s.getSection();
        sd.row = s.getRow();
        sd.number = s.getNumber();
        sd.category = s.getCategory();
        sd.attributes = s.getAttributes();
        return sd;
      })
      .collect(Collectors.toList());

    dto.reservations = reservations
      .stream()
      .map(r -> {
        StorageDto.ReservationDto rd = new StorageDto.ReservationDto();
        rd.id = r.getId();
        rd.eventId = r.getEventId();
        rd.customerEmail = r.getCustomerEmail();
        rd.status = r.getStatus().name();
        rd.createdAt = r.getCreatedAt();
        rd.confirmedAt = r.getConfirmedAt().orElse(null);
        rd.holdExpiresAt = r.getHoldExpiresAt();
        rd.seats = r
          .getSeats()
          .stream()
          .map(rs -> {
            StorageDto.ReservationSeatDto rsd =
              new StorageDto.ReservationSeatDto();
            rsd.reservationId = rs.reservationId();
            rsd.seatId = rs.seatId();
            rsd.priceAmount = rs.price().amount();
            rsd.priceCurrency = rs.price().currency().name();
            rsd.discount = rs.discount() != null ? rs.discount().name() : null;
            return rsd;
          })
          .collect(Collectors.toList());
        return rd;
      })
      .collect(Collectors.toList());

    return dto;
  }

  public static StorageContext fromDto(StorageDto dto) {
    List<Venue> venues = dto.venues
      .stream()
      .map(vd -> new Venue(vd.id, vd.name, vd.address, ZoneId.of(vd.timezone)))
      .collect(Collectors.toList());

    List<Event> events = dto.events
      .stream()
      .map(ed ->
        new Event(
          ed.id,
          ed.venueId,
          ed.title,
          ed.startAt,
          ed.endAt,
          ed.status,
          ed.currency,
          ed.pricingRules
        )
      )
      .collect(Collectors.toList());

    List<Seat> seats = dto.seats
      .stream()
      .map(sd ->
        new Seat(
          sd.id,
          sd.venueId,
          sd.section,
          sd.row,
          sd.number,
          sd.category,
          sd.attributes
        )
      )
      .collect(Collectors.toList());

    List<Reservation> reservations = dto.reservations
      .stream()
      .map(rd -> {
        List<ReservationSeat> reservationSeats = rd.seats
          .stream()
          .map(rsd ->
            new ReservationSeat(
              rsd.reservationId,
              rsd.seatId,
              new Money(rsd.priceAmount, Currency.valueOf(rsd.priceCurrency)),
              rsd.discount != null ? DiscountType.valueOf(rsd.discount) : null
            )
          )
          .collect(Collectors.toList());
        return new Reservation(
          rd.id,
          rd.eventId,
          rd.customerEmail,
          ReservationStatus.valueOf(rd.status),
          rd.createdAt,
          rd.confirmedAt,
          rd.holdExpiresAt,
          reservationSeats
        );
      })
      .collect(Collectors.toList());

    return new StorageContext(venues, events, seats, reservations);
  }

  public static class StorageContext {

    public final List<Venue> venues;
    public final List<Event> events;
    public final List<Seat> seats;
    public final List<Reservation> reservations;

    public StorageContext(
      List<Venue> venues,
      List<Event> events,
      List<Seat> seats,
      List<Reservation> reservations
    ) {
      this.venues = venues;
      this.events = events;
      this.seats = seats;
      this.reservations = reservations;
    }
  }
}
