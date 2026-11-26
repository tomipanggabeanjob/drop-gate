package com.dropgate.order;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
@Primary
@ConditionalOnProperty(name = "dropgate.inventory", havingValue = "memory", matchIfMissing = true)
public class InMemoryInventory implements InventoryPort {
    private final Map<String, AtomicInteger> stockByDrop = new ConcurrentHashMap<>();
    private final Map<String, AtomicInteger> holdsByAccount = new ConcurrentHashMap<>();
    private final Map<String, String> holdOwners = new ConcurrentHashMap<>();
    private final DropgateProperties properties;

    public InMemoryInventory(DropgateProperties properties) {
        this.properties = properties;
        stockByDrop.put("drop-42", new AtomicInteger(10));
    }

    @Override
    public InventorySnapshot getInventory(String dropId) {
        return new InventorySnapshot(dropId, stockByDrop.computeIfAbsent(dropId, ignored -> new AtomicInteger(0)).get());
    }

    @Override
    public InventoryHoldResponse placeHold(InventoryHoldRequest request) {
        if (request.quantity() <= 0) {
            throw new IllegalArgumentException("quantity must be positive");
        }

        AtomicInteger stock = stockByDrop.computeIfAbsent(request.dropId(), ignored -> new AtomicInteger(0));
        AtomicInteger accountHolds = holdsByAccount.computeIfAbsent(request.accountId(), ignored -> new AtomicInteger(0));

        while (true) {
            int currentStock = stock.get();
            if (currentStock < request.quantity()) {
                throw new IllegalStateException("insufficient stock for drop " + request.dropId());
            }

            int currentHolds = accountHolds.get();
            if (currentHolds >= properties.getMaxPerAccount()) {
                throw new IllegalStateException("max holds reached for account " + request.accountId());
            }

            if (accountHolds.compareAndSet(currentHolds, currentHolds + 1)) {
                if (stock.compareAndSet(currentStock, currentStock - request.quantity())) {
                    String holdId = "hold-" + request.accountId() + "-" + System.nanoTime();
                    holdOwners.put(holdId, request.accountId());
                    return new InventoryHoldResponse(request.dropId(), request.accountId(), request.quantity(), currentStock - request.quantity());
                }
                accountHolds.decrementAndGet();
            }
        }
    }

    @Override
    public void releaseHold(String dropId, String holdId, int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("quantity must be positive");
        }

        AtomicInteger stock = stockByDrop.computeIfAbsent(dropId, ignored -> new AtomicInteger(0));
        stock.addAndGet(quantity);

        String owner = holdOwners.remove(holdId);
        if (owner != null) {
            AtomicInteger accountHolds = holdsByAccount.get(owner);
            if (accountHolds != null) {
                accountHolds.updateAndGet(current -> Math.max(0, current - 1));
            }
        }
    }
}
