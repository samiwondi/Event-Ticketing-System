package com.example.demo.domain;

import com.example.demo.enums.Currency;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import java.math.BigDecimal;
import java.util.Objects;

@Embeddable
public class Money {

  @Column(name = "price_amount", nullable = false, precision = 12, scale = 2)
  private BigDecimal amount;

  @Enumerated(EnumType.STRING)
  @Column(name = "price_currency", nullable = false, length = 10)
  private Currency currency;

  protected Money() {} // JPA

  public Money(BigDecimal amount, Currency currency) {
    if (amount == null) throw new IllegalArgumentException(
      "Amount must not be null"
    );
    if (
      amount.compareTo(BigDecimal.ZERO) < 0
    ) throw new IllegalArgumentException("Amount cannot be negative");
    this.amount = amount;
    this.currency = Objects.requireNonNull(currency);
  }

  public BigDecimal getAmount() {
    return amount;
  }

  public Currency getCurrency() {
    return currency;
  }

  public Money add(Money other) {
    if (currency != other.currency) throw new IllegalArgumentException(
      "Currency mismatch"
    );
    return new Money(amount.add(other.amount), currency);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof Money m)) return false;
    return amount.compareTo(m.amount) == 0 && currency == m.currency;
  }

  @Override
  public int hashCode() {
    return Objects.hash(amount, currency);
  }
}
