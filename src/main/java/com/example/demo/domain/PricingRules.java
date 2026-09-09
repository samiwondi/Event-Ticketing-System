package com.example.demo.domain;

import com.example.demo.enums.Currency;
import com.example.demo.enums.SeatCategory;
import java.math.BigDecimal;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

public record PricingRules(
  Currency currency,
  BigDecimal defaultPrice,
  Map<SeatCategory, BigDecimal> categoryPrices
) {
  public PricingRules {
    Objects.requireNonNull(currency, "Currency must not be null");
    Objects.requireNonNull(defaultPrice, "Default price must not be null");
    if (defaultPrice.compareTo(BigDecimal.ZERO) < 0) {
      throw new IllegalArgumentException("Default price cannot be negative");
    }
    if (categoryPrices == null) {
      categoryPrices = new EnumMap<>(SeatCategory.class);
    }
    categoryPrices.forEach((category, price) -> {
      if (price == null) {
        throw new IllegalArgumentException(
          "Price for " + category + " cannot be null"
        );
      }
      if (price.compareTo(BigDecimal.ZERO) < 0) {
        throw new IllegalArgumentException(
          "Price for " + category + " cannot be negative"
        );
      }
    });
  }

  public PricingRules(Currency currency, BigDecimal defaultPrice) {
    this(currency, defaultPrice, new EnumMap<>(SeatCategory.class));
  }

  public Money getPriceForCategory(SeatCategory category) {
    if (category == null) category = SeatCategory.STANDARD;
    BigDecimal amount = categoryPrices.getOrDefault(category, defaultPrice);
    return new Money(amount, currency);
  }

  public PricingRules withCategoryPrice(
    SeatCategory category,
    BigDecimal price
  ) {
    Objects.requireNonNull(category, "Category must not be null");
    Objects.requireNonNull(price, "Price must not be null");
    if (price.compareTo(BigDecimal.ZERO) < 0) {
      throw new IllegalArgumentException("Price cannot be negative");
    }
    EnumMap<SeatCategory, BigDecimal> newMap = new EnumMap<>(categoryPrices);
    newMap.put(category, price);
    return new PricingRules(currency, defaultPrice, newMap);
  }

  public boolean hasCategoryPrice(SeatCategory category) {
    return categoryPrices.containsKey(category);
  }

  public static PricingRules defaultRules(Currency currency) {
    return new PricingRules(currency, BigDecimal.valueOf(50.00))
      .withCategoryPrice(SeatCategory.VIP, BigDecimal.valueOf(100.00))
      .withCategoryPrice(SeatCategory.VVIP, BigDecimal.valueOf(150.00));
  }

  public static PricingRules defaultRules(
    Currency currency,
    BigDecimal basePrice
  ) {
    return new PricingRules(currency, basePrice)
      .withCategoryPrice(
        SeatCategory.VIP,
        basePrice.multiply(BigDecimal.valueOf(2))
      )
      .withCategoryPrice(
        SeatCategory.VVIP,
        basePrice.multiply(BigDecimal.valueOf(3))
      );
  }

  @Override
  public String toString() {
    return (
      "PricingRules{" +
      "currency=" +
      currency +
      ", defaultPrice=" +
      defaultPrice +
      ", categoryPrices=" +
      categoryPrices +
      '}'
    );
  }
}
