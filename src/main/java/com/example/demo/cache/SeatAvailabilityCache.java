package com.example.demo.cache;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import com.example.demo.domain.Seat;
import com.example.demo.repository.SeatRepository;

/**
 * LRU cache for seat lists per event.
 * Uses {@link LinkedHashMap} with removeEldestEntry to evict oldest entries.
 */
public class SeatAvailabilityCache {
    private final SeatRepository seatRepository;
    private final int maxSize;
    private final Map<UUID, List<Seat>> cache;
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    public SeatAvailabilityCache(SeatRepository seatRepository, int maxSize) {
        this.seatRepository = seatRepository;
        this.maxSize = maxSize;
        this.cache = new LinkedHashMap<>(maxSize, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<UUID, List<Seat>> eldest) {
                return size() > maxSize;
            }
        };
    }

    public List<Seat> getSeatsForEvent(UUID eventId, UUID venueId) {
        lock.readLock().lock();
        try {
            if (cache.containsKey(eventId)) {
                return Collections.unmodifiableList(cache.get(eventId));
            }
        } finally {
            lock.readLock().unlock();
        }

        lock.writeLock().lock();
        try {
            if (cache.containsKey(eventId)) {
                return Collections.unmodifiableList(cache.get(eventId));
            }
            List<Seat> seats = seatRepository.findByVenueId(venueId);
            cache.put(eventId, seats);
            return Collections.unmodifiableList(seats);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void invalidate(UUID eventId) {
        lock.writeLock().lock();
        try {
            cache.remove(eventId);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void clear() {
        lock.writeLock().lock();
        try {
            cache.clear();
        } finally {
            lock.writeLock().unlock();
        }
    }

    public int size() {
        lock.readLock().lock();
        try {
            return cache.size();
        } finally {
            lock.readLock().unlock();
        }
    }
}