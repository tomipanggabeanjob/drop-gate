package com.dropgate.order;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class ResilientPaymentGatewayTests {

    @Test
    void resilientGatewayRetriesUntilDelegateSucceeds() {
        AtomicInteger attempts = new AtomicInteger();
        PaymentGateway flaky = (accountId, totalCents) -> {
            if (attempts.incrementAndGet() < 3) {
                throw new RuntimeException("temporary payment failure");
            }
            return true;
        };

        ResilientPaymentGateway gateway = new ResilientPaymentGateway(flaky);

        assertThat(gateway.authorize("acct-42", 2500L)).isTrue();
        assertThat(attempts.get()).isEqualTo(3);
    }
}
