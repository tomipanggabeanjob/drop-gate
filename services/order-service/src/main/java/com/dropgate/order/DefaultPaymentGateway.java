package com.dropgate.order;

import org.springframework.stereotype.Component;

@Component
public class DefaultPaymentGateway implements PaymentGateway {

    @Override
    public boolean authorize(String accountId, long totalCents) {
        return totalCents > 0 && accountId != null && !accountId.isBlank();
    }
}
