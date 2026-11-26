package com.dropgate.order;

import com.dropgate.events.OrderEvent;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class CheckoutService {

    private final PaymentGateway paymentGateway;
    private final OutboxRepository outboxRepository;
    private final InventoryService inventoryService;
    private final Map<String, CheckoutResponse> idempotencyStore = new ConcurrentHashMap<>();

    public CheckoutService(
            PaymentGateway paymentGateway,
            OutboxRepository outboxRepository,
            InventoryService inventoryService) {
        this.paymentGateway = paymentGateway;
        this.outboxRepository = outboxRepository;
        this.inventoryService = inventoryService;
    }

    public CheckoutResponse checkout(String idempotencyKey, CheckoutRequest request) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new IllegalArgumentException("Idempotency-Key header is required");
        }

        return idempotencyStore.computeIfAbsent(idempotencyKey, ignored -> {
            if (request == null) {
                throw new IllegalArgumentException("checkout request is required");
            }
            if (request.quantity() <= 0) {
                throw new IllegalArgumentException("quantity must be positive");
            }
            if (request.holdId() == null || request.holdId().isBlank()) {
                throw new IllegalArgumentException("holdId is required");
            }
            if (!paymentGateway.authorize(request.accountId(), request.totalCents())) {
                throw new IllegalStateException("payment authorization failed");
            }

            inventoryService.confirmHold(
                    request.dropId(),
                    request.holdId(),
                    request.accountId(),
                    request.quantity());

            String orderId = UUID.randomUUID().toString();
            CheckoutResponse response = new CheckoutResponse(
                    orderId,
                    request.dropId(),
                    request.accountId(),
                    request.holdId(),
                    "PAID",
                    request.totalCents());

            OrderEvent event = new OrderEvent(
                    UUID.randomUUID().toString(),
                    orderId,
                    request.dropId(),
                    request.accountId(),
                    request.holdId(),
                    "PAID",
                    request.totalCents(),
                    idempotencyKey,
                    System.currentTimeMillis(),
                    1
            );
            outboxRepository.save(OutboxEvent.orderCreated(orderId, event));

            return response;
        });
    }
}
