package com.example.demo.exception;

public class InvalidSeatException extends RuntimeException {
    public InvalidSeatException(String message) {
        super(message);
    }
}