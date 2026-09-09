package com.example.demo.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.demo.enums.Currency;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class MoneyTest {

  @Test
  void shouldCreateMoney() {
    Money m = new Money(BigDecimal.TEN, Currency.USD);
    assertThat(m.amount()).isEqualByComparingTo(BigDecimal.TEN);
    assertThat(m.currency()).isEqualTo(Currency.USD); // compare enum, not string
  }

  @Test
  void shouldThrowOnNegativeAmount() {
    assertThatThrownBy(() ->
      new Money(BigDecimal.valueOf(-1), Currency.USD)
    ).isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void shouldAddMoney() {
    Money m1 = new Money(BigDecimal.valueOf(10), Currency.USD);
    Money m2 = new Money(BigDecimal.valueOf(20), Currency.USD);
    Money sum = m1.add(m2);
    assertThat(sum.amount()).isEqualByComparingTo(BigDecimal.valueOf(30));
  }

  @Test
  void shouldNotAddDifferentCurrencies() {
    Money m1 = new Money(BigDecimal.TEN, Currency.USD);
    Money m2 = new Money(BigDecimal.TEN, Currency.EUR);
    assertThatThrownBy(() -> m1.add(m2)).isInstanceOf(
      IllegalArgumentException.class
    );
  }
}
