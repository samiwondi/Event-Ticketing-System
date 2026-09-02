package com.example.demo.persistence;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.example.demo.domain.*;
import com.example.demo.exception.StorageException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class JsonStorage {
    private final ObjectMapper mapper;
    private final Path storagePath;

    public JsonStorage(String filePath) {
        this.mapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        this.storagePath = Paths.get(filePath);
    }

    public void save(List<Venue> venues, List<Event> events, List<Seat> seats, List<Reservation> reservations) {
        var dto = DomainMapper.toDto(venues, events, seats, reservations);
        try {
            var json = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(dto);
            Files.writeString(storagePath, json);
        } catch (IOException e) {
            throw new StorageException("Failed to save data", e);
        }
    }

    public DomainMapper.StorageContext load() {
        if (!Files.exists(storagePath)) {
            return new DomainMapper.StorageContext(List.of(), List.of(), List.of(), List.of());
        }
        try {
            var json = Files.readString(storagePath);
            var dto = mapper.readValue(json, StorageDto.class);
            return DomainMapper.fromDto(dto);
        } catch (IOException e) {
            throw new StorageException("Failed to load or parse JSON file", e);
        }
    }
}