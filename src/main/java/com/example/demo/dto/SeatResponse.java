package com.example.demo.dto;

import com.example.demo.domain.Seat;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public record SeatResponse(
  UUID id,
  String section,
  String row,
  int number,
  String category,
  Set<String> attributes
) {
  public static SeatResponse from(Seat s) {
    return new SeatResponse(
      s.getId(),
      s.getSection(),
      s.getRow(),
      s.getNumber(),
      s.getCategory().name(),
      s.getAttributes().stream().map(Enum::name).collect(Collectors.toSet())
    );
  }
}
