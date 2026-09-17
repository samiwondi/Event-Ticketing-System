package com.example.demo.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "venues")
public class Venue {

  @Id
  @Column(name = "id", updatable = false, nullable = false)
  private UUID id;

  @Column(nullable = false)
  private String name;

  @Column(nullable = false)
  private String address;

  @Column(nullable = false)
  private String timezone;

  @CreationTimestamp
  @Column(name = "created_at", updatable = false)
  private Instant createdAt;

  protected Venue() {}

  public Venue(UUID id, String name, String address, String timezone) {
    this.id = id;
    this.name = name;
    this.address = address;
    this.timezone = timezone;
  }

  @PrePersist
  void prePersist() {
    if (id == null) id = UUID.randomUUID();
  }

  public UUID getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public String getAddress() {
    return address;
  }

  public String getTimezone() {
    return timezone;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }
}
