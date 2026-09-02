package com.example.demo.service;

import com.example.demo.domain.Money;
import com.example.demo.domain.ReservationSeat;

public class PricingService {
    // Basic pricing: could be extended with strategies per event.
    public Money calculatePrice(ReservationSeat seat) {
        return seat.finalPrice();
    }
}