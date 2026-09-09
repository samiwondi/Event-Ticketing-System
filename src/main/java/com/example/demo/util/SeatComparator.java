package com.example.demo.util;

import com.example.demo.domain.Seat;
import java.util.Comparator;

public class SeatComparator implements Comparator<Seat> {

  @Override
  public int compare(Seat s1, Seat s2) {
    int sectionComp = s1.getSection().compareTo(s2.getSection());
    if (sectionComp != 0) return sectionComp;
    int rowComp = s1.getRow().compareTo(s2.getRow());
    if (rowComp != 0) return rowComp;
    return Integer.compare(s1.getNumber(), s2.getNumber());
  }
}
