package com.dropgate.order;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class HoldExpiryWorker {

    private static final String FIND_EXPIRED_HOLDS_SQL = """
            SELECT id, drop_id, account_id, quantity
            FROM holds
            WHERE status = 'ACTIVE' AND expires_at < NOW()
            ORDER BY expires_at
            LIMIT 100
            FOR UPDATE SKIP LOCKED
            """;

    private static final String MARK_EXPIRED_SQL = "UPDATE holds SET status = 'EXPIRED' WHERE id = ? AND status = 'ACTIVE'";
    private static final String INSERT_OUTBOX_SQL = """
            INSERT INTO outbox (aggregate_type, aggregate_id, event_type, payload, created_at, published_at)
            VALUES (?, ?, ?, ?, NOW(), NULL)
            """;

    private final JdbcTemplate jdbcTemplate;
    private final InventoryPort inventoryPort;
    private final ObjectMapper objectMapper;

    public HoldExpiryWorker(JdbcTemplate jdbcTemplate, InventoryPort inventoryPort, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.inventoryPort = inventoryPort;
        this.objectMapper = objectMapper;
    }

    @Scheduled(fixedDelayString = "${dropgate.hold-expiry-delay-ms:5000}")
    public void expireHolds() {
        List<ExpiredHold> expiredHolds = jdbcTemplate.query(FIND_EXPIRED_HOLDS_SQL,
                (rs, rowNum) -> new ExpiredHold(
                        rs.getLong("id"),
                        rs.getLong("drop_id"),
                        rs.getLong("account_id"),
                        rs.getInt("quantity")));

        for (ExpiredHold expiredHold : expiredHolds) {
            int updated = jdbcTemplate.update(MARK_EXPIRED_SQL, expiredHold.id());
            if (updated == 0) {
                continue;
            }

            try {
                inventoryPort.releaseHold(String.valueOf(expiredHold.dropId()), String.valueOf(expiredHold.id()), expiredHold.quantity());
            } catch (IllegalStateException ignored) {
                // The hold may already have been released by another worker instance.
            }

            String payload = serializeOutboxEvent(expiredHold);
            jdbcTemplate.update(
                    INSERT_OUTBOX_SQL,
                    "Hold",
                    String.valueOf(expiredHold.id()),
                    "HoldExpired",
                    payload);
        }
    }

    private String serializeOutboxEvent(ExpiredHold expiredHold) {
        try {
            return objectMapper.writeValueAsString(Map.of(
                    "dropId", expiredHold.dropId(),
                    "accountId", expiredHold.accountId(),
                    "holdId", expiredHold.id(),
                    "quantity", expiredHold.quantity()));
        } catch (Exception ex) {
            throw new IllegalStateException("failed to serialize hold-expiration event", ex);
        }
    }

    public record ExpiredHold(long id, long dropId, long accountId, int quantity) {
    }
}
