package com.dropgate.queue;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class RateLimitAndCacheTests {

    @Test
    void tokenBucketRejectsThirdRequestWhenCapacityExceeded() {
        RateLimitService service = new RateLimitService(new RateLimitProperties("token-bucket", 2, 1, 1));

        assertThat(service.allow("fp-1")).isTrue();
        assertThat(service.allow("fp-1")).isTrue();
        assertThat(service.allow("fp-1")).isFalse();
    }

    @Test
    void slidingWindowRejectsOverCapacityRequests() {
        RateLimitService service = new RateLimitService(new RateLimitProperties("sliding-window", 2, 1, 1));

        assertThat(service.allow("fp-2")).isTrue();
        assertThat(service.allow("fp-2")).isTrue();
        assertThat(service.allow("fp-2")).isFalse();
    }

    @Test
    void cacheStoresAndReturnsMetadataOnceLoaded() {
        DropMetadataCacheService cache = new DropMetadataCacheService();

        Optional<DropMetadata> first = cache.get("drop-9", () -> Optional.of(new DropMetadata("drop-9", "Test Drop", 50, 1999L, true)));
        Optional<DropMetadata> second = cache.get("drop-9", () -> Optional.of(new DropMetadata("drop-9", "Other Name", 80, 2999L, true)));

        assertThat(first).isPresent();
        assertThat(second).isPresent();
        assertThat(first.get().name()).isEqualTo("Test Drop");
        assertThat(second.get().name()).isEqualTo("Test Drop");
    }
}
