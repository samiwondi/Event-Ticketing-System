package com.example.demo.domain;

import com.example.demo.util.SeatComparator;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.assertj.core.api.Assertions.*;

class SeatTest {
    @Test
    void comparatorShouldOrderBySectionRowNumber() {
        var venueId = UUID.randomUUID();
        var s1 = new Seat(UUID.randomUUID(), venueId, "A", "1", 2, null);
        var s2 = new Seat(UUID.randomUUID(), venueId, "A", "1", 1, null);
        var s3 = new Seat(UUID.randomUUID(), venueId, "B", "1", 1, null);
        List<Seat> seats = Arrays.asList(s1, s2, s3);
        Collections.sort(seats, new SeatComparator());
        assertThat(seats).containsExactly(s2, s1, s3);
    }
}