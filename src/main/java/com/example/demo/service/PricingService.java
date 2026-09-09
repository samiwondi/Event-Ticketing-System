package com.example.demo.service;

import com.example.demo.domain.Event;
import com.example.demo.domain.Money;
import com.example.demo.domain.Seat;

public class PricingService {
    public Money calculatePrice(Event event, Seat seat) {
        return event.getPricingRules().getPriceForCategory(seat.getCategory());
    }
}