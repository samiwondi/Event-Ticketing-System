package com.example.demo.enums;

import java.math.BigDecimal;
import java.math.RoundingMode;

public enum DiscountType {
  NONE {
    @Override
    public BigDecimal apply(BigDecimal price) {
      return price;
    }
  },
  PERCENTAGE_10 {
    @Override
    public BigDecimal apply(BigDecimal price) {
      return price
        .multiply(BigDecimal.valueOf(0.9))
        .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    }
  },
  FIXED_5 {
    @Override
    public BigDecimal apply(BigDecimal price) {
      return price
        .subtract(BigDecimal.valueOf(5))
        .max(BigDecimal.ZERO)
        .setScale(2, RoundingMode.HALF_UP);
    }
  };

  public abstract BigDecimal apply(BigDecimal price);
}
