package com.example.demo.repository.memory;

import com.example.demo.domain.Reservation;
import com.example.demo.enums.ReservationStatus;
import com.example.demo.repository.ReservationRepository;
import java.util.ArrayList;
import java.util.List; // fixed import
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class InMemoryReservationRepository implements ReservationRepository {

  private final Map<UUID, Reservation> store = new ConcurrentHashMap<>();

  @Override
  public Reservation save(Reservation reservation) {
    store.put(reservation.getId(), reservation);
    return reservation;
  }

  @Override
  public Optional<Reservation> findById(UUID id) {
    return Optional.ofNullable(store.get(id));
  }

  @Override
  public List<Reservation> findByEventId(UUID eventId) {
    return store
      .values()
      .stream()
      .filter(r -> r.getEventId().equals(eventId))
      .collect(Collectors.toList());
  }

  @Override
  public List<Reservation> findByCustomerEmail(String email) {
    return store
      .values()
      .stream()
      .filter(r -> r.getCustomerEmail().equalsIgnoreCase(email))
      .collect(Collectors.toList());
  }

  @Override
  public List<Reservation> findByEventIdAndStatus(
    UUID eventId,
    ReservationStatus status
  ) {
    return store
      .values()
      .stream()
      .filter(r -> r.getEventId().equals(eventId) && r.getStatus() == status)
      .collect(Collectors.toList());
  }

  @Override
  public List<Reservation> findAll() {
    return new ArrayList<>(store.values());
  }

  @Override
  public void deleteById(UUID id) {
    store.remove(id);
  }

  @Override
  public boolean existsById(UUID id) {
    return store.containsKey(id);
  }
}
