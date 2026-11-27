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
    static final String SEEDED_DROP_ID = "drop-42";
    static final int SEEDED_AVAILABLE = 10;

    private final Map<String, AtomicInteger> stockByDrop = new ConcurrentHashMap<>();
    private final Map<String, AtomicInteger> holdsByAccount = new ConcurrentHashMap<>();
    private final Map<String, HoldRecord> holdsById = new ConcurrentHashMap<>();
    private final DropgateProperties properties;

    public InMemoryInventory(DropgateProperties properties) {
        this.properties = properties;
        reset();
    }

    /**
     * Restores seeded stock and clears holds. Spring MVC tests share one bean,
     * so each case resets before it runs and does not depend on test order.
     */
    void reset() {
        stockByDrop.clear();
        holdsByAccount.clear();
        holdsById.clear();
        stockByDrop.put(SEEDED_DROP_ID, new AtomicInteger(SEEDED_AVAILABLE));
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
                    holdsById.put(holdId, new HoldRecord(request.dropId(), request.accountId(), request.quantity()));
                    return new InventoryHoldResponse(
                            holdId,
                            request.dropId(),
                            request.accountId(),
                            request.quantity(),
                            currentStock - request.quantity());
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

        HoldRecord hold = holdsById.remove(holdId);
        AtomicInteger stock = stockByDrop.computeIfAbsent(dropId, ignored -> new AtomicInteger(0));
        stock.addAndGet(quantity);

        String owner = hold == null ? null : hold.accountId();
        if (owner != null) {
            AtomicInteger accountHolds = holdsByAccount.get(owner);
            if (accountHolds != null) {
                accountHolds.updateAndGet(current -> Math.max(0, current - 1));
            }
        }
    }

    @Override
    public void confirmHold(String dropId, String holdId, String accountId, int quantity) {
        if (holdId == null || holdId.isBlank()) {
            throw new IllegalArgumentException("holdId is required");
        }
        HoldRecord hold = holdsById.get(holdId);
        if (hold == null) {
            throw new IllegalStateException("hold not found: " + holdId);
        }
        if (!hold.dropId().equals(dropId)) {
            throw new IllegalStateException("hold drop mismatch for " + holdId);
        }
        if (!hold.accountId().equals(accountId)) {
            throw new IllegalStateException("hold account mismatch for " + holdId);
        }
        if (hold.quantity() != quantity) {
            throw new IllegalStateException("hold quantity mismatch for " + holdId);
        }
        holdsById.remove(holdId);
        AtomicInteger accountHolds = holdsByAccount.get(accountId);
        if (accountHolds != null) {
            accountHolds.updateAndGet(current -> Math.max(0, current - 1));
        }
    }

    private record HoldRecord(String dropId, String accountId, int quantity) {
    }
}
