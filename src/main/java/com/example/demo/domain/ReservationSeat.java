package com.example.demo.domain;

import java.util.Objects;
import java.util.UUID;

public record ReservationSeat(UUID reservationId, UUID seatId, Money price, DiscountType discount) {
    public ReservationSeat {
        Objects.requireNonNull(reservationId);
        Objects.requireNonNull(seatId);
        Objects.requireNonNull(price);
        if (discount == null) discount = DiscountType.NONE;
    }

    public Money finalPrice() {
        return new Money(discount.apply(price.amount()), price.currency());
    }
}