package com.dropgate.order;

public interface PaymentGateway {
    boolean authorize(String accountId, long totalCents);
}
