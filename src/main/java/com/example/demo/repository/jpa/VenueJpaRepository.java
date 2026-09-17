package com.example.demo.repository.jpa;

import com.example.demo.domain.Venue;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VenueJpaRepository extends JpaRepository<Venue, UUID> {}
