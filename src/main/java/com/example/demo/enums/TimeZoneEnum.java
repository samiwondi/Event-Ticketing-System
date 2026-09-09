package com.example.demo.enums;

import java.time.ZoneId;
import java.util.Arrays;
import java.util.List;

public enum TimeZoneEnum {
  AFRICA_ADDIS_ABABA(1, "Africa/Addis_Ababa", ZoneId.of("Africa/Addis_Ababa")),
  AMERICA_NEW_YORK(2, "America/New_York", ZoneId.of("America/New_York")),
  AMERICA_LOS_ANGELES(
    3,
    "America/Los_Angeles",
    ZoneId.of("America/Los_Angeles")
  ),
  AMERICA_CHICAGO(4, "America/Chicago", ZoneId.of("America/Chicago")),
  EUROPE_LONDON(5, "Europe/London", ZoneId.of("Europe/London")),
  EUROPE_BERLIN(6, "Europe/Berlin", ZoneId.of("Europe/Berlin")),
  ASIA_DUBAI(7, "Asia/Dubai", ZoneId.of("Asia/Dubai")),
  ASIA_RIYADH(8, "Asia/Riyadh", ZoneId.of("Asia/Riyadh")),
  EUROPE_ROME(9, "Europe/Rome", ZoneId.of("Europe/Rome")),
  AUSTRALIA_SYDNEY(10, "Australia/Sydney", ZoneId.of("Australia/Sydney")),
  AFRICA_JOHANNESBURG(
    11,
    "Africa/Johannesburg",
    ZoneId.of("Africa/Johannesburg")
  );

  private final int id;
  private final String displayName;
  private final ZoneId zoneId;

  TimeZoneEnum(int id, String displayName, ZoneId zoneId) {
    this.id = id;
    this.displayName = displayName;
    this.zoneId = zoneId;
  }

  public int getId() {
    return id;
  }

  public String getDisplayName() {
    return displayName;
  }

  public ZoneId getZoneId() {
    return zoneId;
  }

  public static TimeZoneEnum fromId(int id) {
    return Arrays.stream(values())
      .filter(tz -> tz.id == id)
      .findFirst()
      .orElseThrow(() ->
        new IllegalArgumentException("Invalid timezone ID: " + id)
      );
  }

  public static List<TimeZoneEnum> getAll() {
    return Arrays.asList(values());
  }
}
