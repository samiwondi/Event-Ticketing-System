package com.example.demo.domain;

import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import static org.assertj.core.api.Assertions.*;

class MoneyTest {
    @Test
    void shouldCreateMoney() {
        Money m = new Money(BigDecimal.TEN, "USD");
        assertThat(m.amount()).isEqualByComparingTo(BigDecimal.TEN);
        assertThat(m.currency()).isEqualTo("USD");
    }

    @Test
    void shouldThrowOnNegativeAmount() {
        assertThatThrownBy(() -> new Money(BigDecimal.valueOf(-1), "USD"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldAddMoney() {
        Money m1 = new Money(BigDecimal.valueOf(10), "USD");
        Money m2 = new Money(BigDecimal.valueOf(20), "USD");
        Money sum = m1.add(m2);
        assertThat(sum.amount()).isEqualByComparingTo(BigDecimal.valueOf(30));
    }

    @Test
    void shouldNotAddDifferentCurrencies() {
        Money m1 = new Money(BigDecimal.TEN, "USD");
        Money m2 = new Money(BigDecimal.TEN, "EUR");
        assertThatThrownBy(() -> m1.add(m2)).isInstanceOf(IllegalArgumentException.class);
    }
}