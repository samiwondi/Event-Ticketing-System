package com.example.demo.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import com.example.demo.domain.Seat;

public class CsvImporter {
    public static List<Seat> importSeats(Path csvPath, UUID venueId) throws IOException {
        try (var lines = Files.lines(csvPath)) {
            return lines.skip(1)
                    .map(line -> line.split(","))
                    .filter(parts -> parts.length >= 3)
                    .map(parts -> {
                        String section = parts[0].trim();
                        String row = parts[1].trim();
                        int number = Integer.parseInt(parts[2].trim());
                        String attributes = parts.length > 3 ? parts[3].trim() : null;
                        return new Seat(UUID.randomUUID(), venueId, section, row, number, attributes);
                    })
                    .collect(Collectors.toList());
        }
    }
}