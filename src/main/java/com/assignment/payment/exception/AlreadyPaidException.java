package com.assignment.payment.exception;

public class AlreadyPaidException extends RuntimeException {

    public AlreadyPaidException(Long orderId) {
        super("Order already paid: " + orderId);
    }
}
