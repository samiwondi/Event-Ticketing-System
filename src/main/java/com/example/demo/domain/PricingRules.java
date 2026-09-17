package com.example.demo.domain;

import com.example.demo.enums.Currency;
import com.example.demo.enums.SeatCategory;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Embeddable
public class PricingRules implements Serializable {

  @Column(name = "pricing_rules", columnDefinition = "jsonb", nullable = false)
  @JdbcTypeCode(SqlTypes.JSON)
  private Map<String, Object> data = new HashMap<>();

  protected PricingRules() {
    // JPA
  }

  public PricingRules(
    Currency currency,
    BigDecimal defaultPrice,
    Map<SeatCategory, BigDecimal> categoryPrices,
    Map<String, SeatCategory> sectionCategories
  ) {
    Objects.requireNonNull(currency, "Currency must not be null");
    Objects.requireNonNull(defaultPrice, "Default price must not be null");
    if (defaultPrice.compareTo(BigDecimal.ZERO) < 0) {
      throw new IllegalArgumentException("Default price cannot be negative");
    }

    data.put("currency", currency.name());
    data.put("defaultPrice", defaultPrice);

    Map<String, BigDecimal> cp = new HashMap<>();
    if (categoryPrices != null) {
      categoryPrices.forEach((k, v) -> {
        if (v == null) throw new IllegalArgumentException(
          "Price for " + k + " cannot be null"
        );
        if (
          v.compareTo(BigDecimal.ZERO) < 0
        ) throw new IllegalArgumentException(
          "Price for " + k + " cannot be negative"
        );
        cp.put(k.name(), v);
      });
    }
    data.put("categoryPrices", cp);

    Map<String, String> sc = new HashMap<>();
    if (sectionCategories != null) {
      sectionCategories.forEach((k, v) -> sc.put(k, v.name()));
    }
    data.put("sectionCategories", sc);
  }

  /** Convenience ctor: default maps. */
  public PricingRules(Currency currency, BigDecimal defaultPrice) {
    this(
      currency,
      defaultPrice,
      new EnumMap<>(SeatCategory.class),
      new HashMap<>()
    );
  }

  // ------------------------------------------------------------------
  //  Phase 2-style API used by the CLI
  // ------------------------------------------------------------------
  public Currency currency() {
    return Currency.valueOf((String) data.get("currency"));
  }

  public BigDecimal defaultPrice() {
    Object v = data.get("defaultPrice");
    return v == null ? BigDecimal.ZERO : new BigDecimal(v.toString());
  }

  public Map<SeatCategory, BigDecimal> categoryPrices() {
    Map<SeatCategory, BigDecimal> result = new EnumMap<>(SeatCategory.class);
    Object raw = data.get("categoryPrices");
    if (raw instanceof Map<?, ?> m) {
      for (var e : m.entrySet()) {
        result.put(
          SeatCategory.valueOf(e.getKey().toString()),
          new BigDecimal(e.getValue().toString())
        );
      }
    }
    return result;
  }

  public Map<String, SeatCategory> sectionCategories() {
    Map<String, SeatCategory> result = new HashMap<>();
    Object raw = data.get("sectionCategories");
    if (raw instanceof Map<?, ?> m) {
      for (var e : m.entrySet()) {
        result.put(
          e.getKey().toString(),
          SeatCategory.valueOf(e.getValue().toString())
        );
      }
    }
    return result;
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

    Map<SeatCategory, BigDecimal> cp = categoryPrices();
    cp.put(category, price);
    return new PricingRules(
      currency(),
      defaultPrice(),
      cp,
      sectionCategories()
    );
  }

  public PricingRules withSectionCategories(
    Map<String, SeatCategory> sectionCats
  ) {
    return new PricingRules(
      currency(),
      defaultPrice(),
      categoryPrices(),
      sectionCats == null ? new HashMap<>() : new HashMap<>(sectionCats)
    );
  }

  // ------------------------------------------------------------------
  //  Phase 3-style getters used by BookingService, PricingService
  // ------------------------------------------------------------------
  public Money getPriceForCategory(SeatCategory category) {
    if (category == null) category = SeatCategory.STANDARD;
    BigDecimal amount = categoryPrices().getOrDefault(category, defaultPrice());
    return new Money(amount, currency());
  }

  public SeatCategory getCategoryForSection(String section) {
    if (section == null) return SeatCategory.STANDARD;
    return sectionCategories().getOrDefault(section, SeatCategory.STANDARD);
  }

  public boolean hasCategoryPrice(SeatCategory category) {
    return categoryPrices().containsKey(category);
  }

  public Set<String> getSectionsWithCategory(SeatCategory category) {
    return sectionCategories()
      .entrySet()
      .stream()
      .filter(e -> e.getValue() == category)
      .map(Map.Entry::getKey)
      .collect(Collectors.toSet());
  }

  public Currency getCurrency() {
    return currency();
  }

  public BigDecimal getDefaultPrice() {
    return defaultPrice();
  }

  // ------------------------------------------------------------------
  //  Factories
  // ------------------------------------------------------------------
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
      "PricingRules{currency=" +
      currency() +
      ", defaultPrice=" +
      defaultPrice() +
      ", categoryPrices=" +
      categoryPrices() +
      ", sectionCategories=" +
      sectionCategories() +
      '}'
    );
  }
}
