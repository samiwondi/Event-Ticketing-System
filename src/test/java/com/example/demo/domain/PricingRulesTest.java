package com.example.demo.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.demo.enums.Currency;
import com.example.demo.enums.SeatCategory;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class PricingRulesTest {

  @Test
  void constructor_ShouldCreateWithDefaultMap() {
    PricingRules rules = new PricingRules(Currency.USD, BigDecimal.valueOf(50));
    assertThat(rules.currency()).isEqualTo(Currency.USD);
    assertThat(rules.defaultPrice()).isEqualByComparingTo(
      BigDecimal.valueOf(50)
    );
    assertThat(rules.categoryPrices()).isEmpty();
  }

  @Test
  void constructor_ShouldThrowOnNegativeDefaultPrice() {
    assertThatThrownBy(() ->
      new PricingRules(Currency.USD, BigDecimal.valueOf(-1))
    )
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessageContaining("negative");
  }

  @Test
  void constructor_WithNullCurrency_ShouldThrowNullPointerException() {
    assertThatThrownBy(() -> new PricingRules(null, BigDecimal.valueOf(50)))
      .isInstanceOf(NullPointerException.class)
      .hasMessageContaining("Currency must not be null");
  }

  @Test
  void constructor_WithNullDefaultPrice_ShouldThrowNullPointerException() {
    assertThatThrownBy(() -> new PricingRules(Currency.USD, null))
      .isInstanceOf(NullPointerException.class)
      .hasMessageContaining("Default price must not be null");
  }

  @Test
  void withCategoryPrice_ShouldAddNewCategory() {
    PricingRules rules = new PricingRules(
      Currency.USD,
      BigDecimal.valueOf(50)
    ).withCategoryPrice(SeatCategory.VIP, BigDecimal.valueOf(100));
    assertThat(rules.categoryPrices()).containsEntry(
      SeatCategory.VIP,
      BigDecimal.valueOf(100)
    );
  }

  @Test
  void withCategoryPrice_ShouldOverrideExisting() {
    PricingRules rules = new PricingRules(Currency.USD, BigDecimal.valueOf(50))
      .withCategoryPrice(SeatCategory.VIP, BigDecimal.valueOf(100))
      .withCategoryPrice(SeatCategory.VIP, BigDecimal.valueOf(120));
    assertThat(rules.categoryPrices()).containsEntry(
      SeatCategory.VIP,
      BigDecimal.valueOf(120)
    );
  }

  @Test
  void getPriceForCategory_ShouldReturnDefaultForMissingCategory() {
    PricingRules rules = new PricingRules(Currency.USD, BigDecimal.valueOf(50));
    Money price = rules.getPriceForCategory(SeatCategory.VIP);
    assertThat(price.amount()).isEqualByComparingTo(BigDecimal.valueOf(50));
    assertThat(price.currency()).isEqualTo(Currency.USD);
  }

  @Test
  void getPriceForCategory_ShouldReturnCategoryPrice() {
    PricingRules rules = new PricingRules(
      Currency.USD,
      BigDecimal.valueOf(50)
    ).withCategoryPrice(SeatCategory.VIP, BigDecimal.valueOf(100));
    Money price = rules.getPriceForCategory(SeatCategory.VIP);
    assertThat(price.amount()).isEqualByComparingTo(BigDecimal.valueOf(100));
    assertThat(price.currency()).isEqualTo(Currency.USD);
  }

  @Test
  void hasCategoryPrice_ShouldReturnTrueForExisting() {
    PricingRules rules = new PricingRules(
      Currency.USD,
      BigDecimal.valueOf(50)
    ).withCategoryPrice(SeatCategory.VIP, BigDecimal.valueOf(100));
    assertThat(rules.hasCategoryPrice(SeatCategory.VIP)).isTrue();
    assertThat(rules.hasCategoryPrice(SeatCategory.VVIP)).isFalse();
  }

  @Test
  void defaultRules_ShouldCreateWithStandardPrices() {
    PricingRules rules = PricingRules.defaultRules(Currency.USD);
    assertThat(rules.currency()).isEqualTo(Currency.USD);
    assertThat(rules.defaultPrice()).isEqualByComparingTo(
      BigDecimal.valueOf(50)
    );
    assertThat(
      rules.getPriceForCategory(SeatCategory.VIP).amount()
    ).isEqualByComparingTo(BigDecimal.valueOf(100));
    assertThat(
      rules.getPriceForCategory(SeatCategory.VVIP).amount()
    ).isEqualByComparingTo(BigDecimal.valueOf(150));
  }

  @Test
  void defaultRules_WithBasePrice_ShouldCalculateMultipliers() {
    PricingRules rules = PricingRules.defaultRules(
      Currency.USD,
      BigDecimal.valueOf(60)
    );
    assertThat(rules.defaultPrice()).isEqualByComparingTo(
      BigDecimal.valueOf(60)
    );
    assertThat(
      rules.getPriceForCategory(SeatCategory.VIP).amount()
    ).isEqualByComparingTo(BigDecimal.valueOf(120));
    assertThat(
      rules.getPriceForCategory(SeatCategory.VVIP).amount()
    ).isEqualByComparingTo(BigDecimal.valueOf(180));
  }

  @Test
  void toString_ShouldNotThrow() {
    PricingRules rules = new PricingRules(Currency.USD, BigDecimal.valueOf(50));
    assertThat(rules.toString()).contains("USD", "50");
  }
}
