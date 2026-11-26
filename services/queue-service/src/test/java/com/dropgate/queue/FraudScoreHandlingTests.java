package com.dropgate.queue;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class FraudScoreHandlingTests {

    @Test
    void flaggedFingerprintIsRejectedByRateLimiter() {
        RateLimitService service = new RateLimitService(new RateLimitProperties("token-bucket", 10, 5, 1));

        service.flagFingerprint("fp-bot", 30_000L);

        assertThat(service.allow("fp-bot")).isFalse();
    }
}
