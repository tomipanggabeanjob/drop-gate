package com.dropgate.order;

public record CheckoutRequest(
        String dropId,
        String accountId,
        String holdId,
        int quantity,
        long totalCents) {
}
