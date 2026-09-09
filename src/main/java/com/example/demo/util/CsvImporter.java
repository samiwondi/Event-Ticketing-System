package com.example.demo.util;

import com.example.demo.domain.Seat;
import com.example.demo.enums.SeatAttribute;
import com.example.demo.enums.SeatCategory;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public class CsvImporter {

  public static List<Seat> importSeats(Path csvPath, UUID venueId)
    throws IOException {
    try (var lines = Files.lines(csvPath)) {
      return lines
        .skip(1)
        .map(line -> line.split(","))
        .filter(parts -> parts.length >= 3)
        .map(parts -> {
          String section = parts[0].trim();
          String row = parts[1].trim();
          int number = Integer.parseInt(parts[2].trim());

          SeatCategory category = SeatCategory.STANDARD;
          Set<SeatAttribute> attributes = Collections.emptySet();

          if (parts.length >= 4) {
            String categoryRaw = parts[3].trim();
            if (!categoryRaw.isEmpty()) {
              try {
                category = SeatCategory.valueOf(categoryRaw.toUpperCase());
              } catch (IllegalArgumentException e) {}
            }
          }

          if (parts.length >= 5) {
            String attributesRaw = parts[4].trim();
            attributes = parseAttributes(attributesRaw);
          } else if (parts.length == 4) {
            String fourth = parts[3].trim();
            if (!fourth.isEmpty()) {
              try {
                category = SeatCategory.valueOf(fourth.toUpperCase());
              } catch (IllegalArgumentException e) {
                attributes = parseAttributes(fourth);
              }
            }
          }

          return new Seat(
            UUID.randomUUID(),
            venueId,
            section,
            row,
            number,
            category,
            attributes
          );
        })
        .collect(Collectors.toList());
    }
  }

  private static Set<SeatAttribute> parseAttributes(String raw) {
    if (raw == null || raw.isBlank()) {
      return Collections.emptySet();
    }
    Set<SeatAttribute> result = new HashSet<>();
    for (String token : raw.split(";")) {
      String trimmed = token.trim();
      if (!trimmed.isEmpty()) {
        try {
          result.add(SeatAttribute.valueOf(trimmed.toUpperCase()));
        } catch (IllegalArgumentException ignored) {}
      }
    }
    return result;
  }
}
