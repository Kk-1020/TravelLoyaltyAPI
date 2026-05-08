package com.arrivia.loyalty.exception;

public class InsufficientPointsException extends RuntimeException {
    public InsufficientPointsException(int requested, int available) {
        super("Insufficient points: requested " + requested + " but only " + available + " available");
    }
}
