package com.dropgate.order;

public record CheckoutResponse(
        String orderId,
        String dropId,
        String accountId,
        String holdId,
        String status,
        long totalCents) {
}
