package com.dropgate.order;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
@ConditionalOnProperty(name = "dropgate.inventory", havingValue = "redis")
public class RedisInventory implements InventoryPort {

    private final RedisTemplate<String, String> redisTemplate;
    private final RedisScript<Long> createHoldScript;
    private final RedisScript<Long> releaseHoldScript;
    private final RedisScript<Long> confirmHoldScript;
    private final DropgateProperties properties;

    public RedisInventory(
            RedisTemplate<String, String> redisTemplate,
            @Qualifier("createHoldScript") RedisScript<Long> createHoldScript,
            @Qualifier("releaseHoldScript") RedisScript<Long> releaseHoldScript,
            @Qualifier("confirmHoldScript") RedisScript<Long> confirmHoldScript,
            DropgateProperties properties) {
        this.redisTemplate = redisTemplate;
        this.createHoldScript = createHoldScript;
        this.releaseHoldScript = releaseHoldScript;
        this.confirmHoldScript = confirmHoldScript;
        this.properties = properties;
    }

    @Override
    public InventorySnapshot getInventory(String dropId) {
        String value = redisTemplate.opsForValue().get(stockKey(dropId));
        int available = value == null ? 0 : Integer.parseInt(value);
        return new InventorySnapshot(dropId, available);
    }

    @Override
    public InventoryHoldResponse placeHold(InventoryHoldRequest request) {
        String holdId = UUID.randomUUID().toString();
        String stockKey = stockKey(request.dropId());
        String holdsKey = holdsKey(request.dropId());
        String accountHoldsKey = accountHoldsKey(request.dropId());
        Long remaining = redisTemplate.execute(
                createHoldScript,
                List.of(stockKey, holdsKey, accountHoldsKey),
                holdId,
                request.accountId(),
                String.valueOf(request.quantity()),
                String.valueOf(properties.getHoldTtlSeconds() * 1000L),
                String.valueOf(properties.getMaxPerAccount())
        );

        if (remaining == null) {
            throw new IllegalStateException("inventory reservation failed for drop " + request.dropId());
        }
        if (remaining == -2) {
            throw new IllegalStateException("max holds reached for account " + request.accountId());
        }
        if (remaining < 0) {
            throw new IllegalStateException("insufficient stock for drop " + request.dropId());
        }

        return new InventoryHoldResponse(
                holdId,
                request.dropId(),
                request.accountId(),
                request.quantity(),
                remaining.intValue());
    }

    @Override
    public void releaseHold(String dropId, String holdId, int quantity) {
        Long remaining = redisTemplate.execute(
                releaseHoldScript,
                List.of(stockKey(dropId), holdsKey(dropId), accountHoldsKey(dropId)),
                holdId,
                String.valueOf(quantity));
        if (remaining == null || remaining < 0) {
            throw new IllegalStateException("hold not found for release");
        }
    }

    @Override
    public void confirmHold(String dropId, String holdId, String accountId, int quantity) {
        Long result = redisTemplate.execute(
                confirmHoldScript,
                List.of(holdsKey(dropId), accountHoldsKey(dropId)),
                holdId,
                accountId == null ? "" : accountId);
        if (result == null || result == -1) {
            throw new IllegalStateException("hold not found: " + holdId);
        }
        if (result == -2) {
            throw new IllegalStateException("hold account mismatch for " + holdId);
        }
    }

    private String stockKey(String dropId) {
        return "drop:" + dropId + ":stock";
    }

    private String holdsKey(String dropId) {
        return "drop:" + dropId + ":holds";
    }

    private String accountHoldsKey(String dropId) {
        return "drop:" + dropId + ":account-holds";
    }
}
