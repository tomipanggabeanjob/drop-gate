package com.dropgate.queue;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class QueueServiceTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void queueJoinReturnsTicketAndPosition() throws Exception {
        String body = "{\"accountId\":\"acct-100\"}";

        mockMvc.perform(post("/api/drops/drop-42/queue/join")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dropId").value("drop-42"))
                .andExpect(jsonPath("$.accountId").value("acct-100"))
                .andExpect(jsonPath("$.queuePosition").value(1));
    }

    @Test
    void queuePositionEndpointReportsCurrentRank() throws Exception {
        mockMvc.perform(post("/api/drops/drop-88/queue/join")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"accountId\":\"acct-88\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/drops/drop-88/queue/position").param("accountId", "acct-88"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.queuePosition").value(1));
    }

    @Test
    void queueStreamEndpointAcceptsSseConnection() throws Exception {
        var result = mockMvc.perform(get("/api/drops/drop-77/queue/stream").param("accountId", "acct-77"))
                .andExpect(status().isOk())
                .andReturn();

        assertThat(result.getResponse().getContentType()).contains("text/event-stream");
    }

    @Test
    void lotteryQueueReturnsPositivePositionsForJoinedAccounts() throws Exception {
        mockMvc.perform(post("/api/drops/drop-lottery/queue/join")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"accountId\":\"acct-a\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/drops/drop-lottery/queue/join")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"accountId\":\"acct-b\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/drops/drop-lottery/queue/position").param("accountId", "acct-a"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.queuePosition").isNumber());

        mockMvc.perform(get("/api/drops/drop-lottery/queue/position").param("accountId", "acct-b"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.queuePosition").isNumber());
    }
}
