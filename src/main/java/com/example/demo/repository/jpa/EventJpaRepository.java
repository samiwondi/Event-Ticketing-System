package com.example.demo.repository.jpa;

import com.example.demo.domain.Event;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EventJpaRepository extends JpaRepository<Event, UUID> {
  @Query(
    """
    SELECT e FROM Event e
    WHERE (CAST(:from AS timestamp) IS NULL OR e.startAt >= :from)
      AND (CAST(:to   AS timestamp) IS NULL OR e.endAt   <= :to)
      AND (CAST(:q    AS string)    IS NULL
           OR LOWER(e.title) LIKE LOWER(CONCAT('%', CAST(:q AS string), '%')))
    """
  )
  Page<Event> search(
    @Param("from") Instant from,
    @Param("to") Instant to,
    @Param("q") String q,
    Pageable pageable
  );
}
