package com.example.demo.domain;

import java.math.BigDecimal;

public enum DiscountType {
    NONE {
        @Override
        public BigDecimal apply(BigDecimal price) {
            return price;
        }
    },
    PERCENTAGE_10 {
        @Override
        public BigDecimal apply(BigDecimal price) {
            return price.multiply(BigDecimal.valueOf(0.9));
        }
    },
    FIXED_5 {
        @Override
        public BigDecimal apply(BigDecimal price) {
            return price.subtract(BigDecimal.valueOf(5)).max(BigDecimal.ZERO);
        }
    };

    public abstract BigDecimal apply(BigDecimal price);
}