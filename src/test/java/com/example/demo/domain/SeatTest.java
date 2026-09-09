package com.example.demo.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.demo.enums.SeatAttribute;
import com.example.demo.enums.SeatCategory;
import com.example.demo.util.SeatComparator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class SeatTest {

  @Test
  void comparatorShouldOrderBySectionRowNumber() {
    var venueId = UUID.randomUUID();
    Set<SeatAttribute> emptyAttrs = Collections.emptySet();
    var s1 = new Seat(
      UUID.randomUUID(),
      venueId,
      "A",
      "1",
      2,
      SeatCategory.STANDARD,
      emptyAttrs
    );
    var s2 = new Seat(
      UUID.randomUUID(),
      venueId,
      "A",
      "1",
      1,
      SeatCategory.STANDARD,
      emptyAttrs
    );
    var s3 = new Seat(
      UUID.randomUUID(),
      venueId,
      "B",
      "1",
      1,
      SeatCategory.VIP,
      emptyAttrs
    );
    // Use mutable list
    List<Seat> seats = new ArrayList<>(List.of(s1, s2, s3));
    seats.sort(new SeatComparator());
    assertThat(seats).containsExactly(s2, s1, s3);
  }
}
