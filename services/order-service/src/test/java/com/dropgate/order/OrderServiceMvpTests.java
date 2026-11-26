package com.dropgate.order;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = "dropgate.max-per-account=2")
class OrderServiceMvpTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AllocationService allocationService;

    @Test
    void inventoryIsAvailableAndCanBeHeld() throws Exception {
        mockMvc.perform(get("/api/inventory/drop-42"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dropId").value("drop-42"))
                .andExpect(jsonPath("$.available").value(10));

        String request = """
                {
                  "dropId": "drop-42",
                  "accountId": "acct-1",
                  "quantity": 2
                }
                """;

        mockMvc.perform(post("/api/inventory/holds")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dropId").value("drop-42"))
                .andExpect(jsonPath("$.accountId").value("acct-1"))
                .andExpect(jsonPath("$.quantity").value(2))
                .andExpect(jsonPath("$.remaining").value(8));
    }

    @Test
    void flywayMigrationDefinesCoreSchemaAndIndexes() throws IOException {
        Path migrationFile = Path.of("src", "main", "resources", "db", "migration", "V1__init.sql");

        assertThat(migrationFile).exists();

        String sql = Files.readString(migrationFile);

        assertThat(sql)
                .contains("CREATE TABLE IF NOT EXISTS drops")
                .contains("CREATE TABLE IF NOT EXISTS accounts")
                .contains("CREATE TABLE IF NOT EXISTS holds")
                .contains("CREATE TABLE IF NOT EXISTS orders")
                .contains("CREATE TABLE IF NOT EXISTS outbox")
                .contains("CREATE INDEX IF NOT EXISTS idx_holds_active_expires_at")
                .contains("CREATE INDEX IF NOT EXISTS idx_orders_drop_created_at")
                .contains("CREATE INDEX IF NOT EXISTS idx_orders_attempt_meta_gin")
                .contains("CREATE UNIQUE INDEX IF NOT EXISTS idx_orders_idempotency_key")
                .contains("CREATE INDEX IF NOT EXISTS idx_drops_config_gin");
    }

    @Test
    void allocationServiceSelectsConfiguredStrategy() {
        AllocationDecision decision = allocationService.decide("fifo", new AllocationRequest("drop-42", "acct-1", 2, 10, "fifo"));

        assertThat(decision.strategyName()).isEqualTo("fifo");
        assertThat(decision.approved()).isTrue();
        assertThat(decision.allocatedQuantity()).isEqualTo(2);
    }

    @Test
    void accountHoldLimitIsEnforced() {
        DropgateProperties properties = new DropgateProperties();
        properties.setMaxPerAccount(2);

        InventoryService service = new InventoryService(new InMemoryInventory(properties));

        service.placeHold(new InventoryHoldRequest("drop-42", "acct-limit", 1));
        service.placeHold(new InventoryHoldRequest("drop-42", "acct-limit", 1));

        assertThatThrownBy(() -> service.placeHold(new InventoryHoldRequest("drop-42", "acct-limit", 1)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("max holds");
    }

    @Test
    void createDropHoldEndpointUsesDropIdFromPath() throws Exception {
        String request = """
                {
                  "accountId": "acct-path-hold",
                  "quantity": 1
                }
                """;

        mockMvc.perform(post("/api/drops/drop-42/holds")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dropId").value("drop-42"))
                .andExpect(jsonPath("$.accountId").value("acct-path-hold"))
                .andExpect(jsonPath("$.quantity").value(1));
    }

    @Test
    void checkoutUsesIdempotencyKeyAndReplaysSameResponse() throws Exception {
        String holdRequest = """
                {
                  "accountId": "acct-checkout",
                  "quantity": 1
                }
                """;

        String holdResponse = mockMvc.perform(post("/api/drops/drop-42/holds")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(holdRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.holdId").isNotEmpty())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String holdId = objectMapper.readTree(holdResponse).get("holdId").asText();
        String request = """
                {
                  "dropId": "drop-42",
                  "accountId": "acct-checkout",
                  "holdId": "%s",
                  "quantity": 1,
                  "totalCents": 2500
                }
                """.formatted(holdId);

        String first = mockMvc.perform(post("/api/checkout")
                        .header("Idempotency-Key", "idem-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String second = mockMvc.perform(post("/api/checkout")
                        .header("Idempotency-Key", "idem-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(first).isEqualTo(second);
        assertThat(first).contains("\"status\":\"PAID\"");
    }

    @Test
    void holdExpiryWorkerMarksExpiredAndReleasesStock() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        InventoryPort inventoryPort = mock(InventoryPort.class);
        ObjectMapper mapper = new ObjectMapper();

        when(jdbcTemplate.query(anyString(), org.mockito.ArgumentMatchers.<RowMapper<HoldExpiryWorker.ExpiredHold>>any()))
                .thenReturn(java.util.List.of(new HoldExpiryWorker.ExpiredHold(7L, 42L, 5L, 2)));
        when(jdbcTemplate.update(anyString(), eq(7L))).thenReturn(1);
        when(jdbcTemplate.update(anyString(), eq("Hold"), eq("7"), eq("HoldExpired"), anyString()))
                .thenReturn(1);

        new HoldExpiryWorker(jdbcTemplate, inventoryPort, mapper).expireHolds();

        verify(inventoryPort).releaseHold("42", "7", 2);
    }

    @Test
    void auditedBeanPostProcessorPublishesAuditEvent() {
        AuditEventSink sink = mock(AuditEventSink.class);
        AuditedBeanPostProcessor processor = new AuditedBeanPostProcessor(sink, "operator-user");

        ExampleAuditedService bean = (ExampleAuditedService) processor.postProcessAfterInitialization(new ExampleAuditedService(), "example");
        String outcome = bean.ping("hello");

        assertThat(outcome).isEqualTo("pong:hello");
        verify(sink).publish(any(AuditEvent.class));
    }

    static class ExampleAuditedService {
        @Audited
        public String ping(String value) {
            return "pong:" + value;
        }
    }
}
