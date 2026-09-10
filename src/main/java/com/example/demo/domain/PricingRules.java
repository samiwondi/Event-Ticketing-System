package com.example.demo.domain;

import com.example.demo.enums.Currency;
import com.example.demo.enums.SeatCategory;
import java.math.BigDecimal;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public record PricingRules(
  Currency currency,
  BigDecimal defaultPrice,
  Map<SeatCategory, BigDecimal> categoryPrices,
  Map<String, SeatCategory> sectionCategories
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
    if (sectionCategories == null) {
      sectionCategories = new HashMap<>();
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
    this(
      currency,
      defaultPrice,
      new EnumMap<>(SeatCategory.class),
      new HashMap<>()
    );
  }

  public Money getPriceForCategory(SeatCategory category) {
    if (category == null) category = SeatCategory.STANDARD;
    BigDecimal amount = categoryPrices.getOrDefault(category, defaultPrice);
    return new Money(amount, currency);
  }

  /**
   * Returns the category for a given section name.
   * Defaults to STANDARD if the section isn't mapped.
   */
  public SeatCategory getCategoryForSection(String section) {
    if (section == null) return SeatCategory.STANDARD;
    return sectionCategories.getOrDefault(section, SeatCategory.STANDARD);
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
    return new PricingRules(
      currency,
      defaultPrice,
      newMap,
      new HashMap<>(sectionCategories)
    );
  }

  public PricingRules withSectionCategories(
    Map<String, SeatCategory> sectionCats
  ) {
    return new PricingRules(
      currency,
      defaultPrice,
      new EnumMap<>(categoryPrices),
      sectionCats == null ? new HashMap<>() : new HashMap<>(sectionCats)
    );
  }

  public boolean hasCategoryPrice(SeatCategory category) {
    return categoryPrices.containsKey(category);
  }

  public Set<String> getSectionsWithCategory(SeatCategory category) {
    return sectionCategories
      .entrySet()
      .stream()
      .filter(e -> e.getValue() == category)
      .map(Map.Entry::getKey)
      .collect(Collectors.toSet());
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
      ", sectionCategories=" +
      sectionCategories +
      '}'
    );
  }
}
