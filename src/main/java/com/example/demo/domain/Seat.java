package com.example.demo.domain;

import com.example.demo.enums.SeatAttribute;
import com.example.demo.enums.SeatCategory;
import jakarta.persistence.*;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(
  name = "seats",
  uniqueConstraints = @UniqueConstraint(
    name = "uq_seat_position",
    columnNames = { "venue_id", "section", "row_label", "number" }
  )
)
public class Seat {

  @Id
  private UUID id;

  @Column(name = "venue_id", nullable = false)
  private UUID venueId;

  @Column(nullable = false)
  private String section;

  @Column(name = "row_label", nullable = false)
  private String row;

  @Column(nullable = false)
  private int number;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private SeatCategory category;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(columnDefinition = "jsonb", nullable = false)
  private Set<SeatAttribute> attributes = new HashSet<>();

  protected Seat() {}

  public Seat(
    UUID id,
    UUID venueId,
    String section,
    String row,
    int number,
    SeatCategory category,
    Set<SeatAttribute> attributes
  ) {
    this.id = id;
    this.venueId = venueId;
    this.section = section;
    this.row = row;
    this.number = number;
    this.category = category;
    this.attributes =
      attributes == null ? new HashSet<>() : new HashSet<>(attributes);
  }

  @PrePersist
  void prePersist() {
    if (id == null) id = UUID.randomUUID();
  }

  public UUID getId() {
    return id;
  }

  public UUID getVenueId() {
    return venueId;
  }

  public String getSection() {
    return section;
  }

  public String getRow() {
    return row;
  }

  public int getNumber() {
    return number;
  }

  public SeatCategory getCategory() {
    return category;
  }

  public Set<SeatAttribute> getAttributes() {
    return attributes;
  }
}
