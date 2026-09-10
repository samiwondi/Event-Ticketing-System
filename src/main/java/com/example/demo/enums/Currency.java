package com.example.demo.enums;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.stream.Stream;

public enum Currency {
  USD("USD", "$", 2, Locale.US),
  EUR("EUR", "€", 2, Locale.GERMANY),
  GBP("GBP", "£", 2, Locale.UK),
  ETB("ETB", "Br", 2, new Locale("am", "ET")),
  CAD("CAD", "C$", 2, Locale.CANADA),
  AED("AED", "د.إ", 2, new Locale("ar", "AE"));

  private final String code;
  private final String symbol;
  private final int decimalDigits;
  private final Locale locale;

  Currency(String code, String symbol, int decimalDigits, Locale locale) {
    this.code = code;
    this.symbol = symbol;
    this.decimalDigits = decimalDigits;
    this.locale = locale;
  }

  public String getCode() {
    return code;
  }

  public String getSymbol() {
    return symbol;
  }

  public int getDecimalDigits() {
    return decimalDigits;
  }

  public Locale getLocale() {
    return locale;
  }

  public static Currency getByCode(String code) {
    if (code == null) return null;
    String upper = code.trim().toUpperCase();
    return Stream.of(values())
      .filter(c -> c.code.equals(upper))
      .findFirst()
      .orElseThrow(() ->
        new IllegalArgumentException("Unknown currency code: " + code)
      );
  }

  public String format(BigDecimal amount) {
    if (amount == null) return symbol + "0.00";
    BigDecimal scaled = amount.setScale(decimalDigits, RoundingMode.HALF_UP);
    NumberFormat nf = NumberFormat.getCurrencyInstance(locale);
    return nf.format(scaled);
  }

  public String formatSimple(BigDecimal amount) {
    if (amount == null) return symbol + "0.00";
    BigDecimal scaled = amount.setScale(decimalDigits, RoundingMode.HALF_UP);
    return symbol + String.format("%." + decimalDigits + "f", scaled);
  }

  @Override
  public String toString() {
    return code;
  }
}
