package com.example.demo.domain;

import com.example.demo.enums.Currency;
import java.math.BigDecimal;
import java.util.Objects;

public record Money(BigDecimal amount, Currency currency) {
  public Money {
    if (amount == null) {
      throw new IllegalArgumentException("Amount must not be null/blank");
    }
    if (amount.compareTo(BigDecimal.ZERO) < 0) {
      throw new IllegalArgumentException("Amount cannot be negative");
    }
    if (currency == null) {
      throw new IllegalArgumentException("Currency must not be null");
    }
  }

  public Money add(Money other) {
    if (!currency.equals(other.currency)) {
      throw new IllegalArgumentException("Currencies must match");
    }
    return new Money(amount.add(other.amount), currency);
  }

  public Money subtract(Money other) {
    if (!currency.equals(other.currency)) {
      throw new IllegalArgumentException("Currencies must match");
    }
    return new Money(amount.subtract(other.amount), currency);
  }

  public Money multiply(int multiplier) {
    return new Money(amount.multiply(BigDecimal.valueOf(multiplier)), currency);
  }

  public static Money zero(Currency currency) {
    return new Money(BigDecimal.ZERO, currency);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof Money money)) return false;
    return (
      amount.compareTo(money.amount) == 0 && currency.equals(money.currency)
    );
  }

  @Override
  public int hashCode() {
    return Objects.hash(amount, currency);
  }

  @Override
  public String toString() {
    return currency.getSymbol() + " " + amount;
  }
}
