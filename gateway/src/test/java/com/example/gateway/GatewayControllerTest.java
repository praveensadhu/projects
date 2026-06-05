package com.example.gateway;

import com.example.gateway.repository.EventRepository;
import com.example.gateway.service.AccountClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import reactor.core.publisher.Mono;

import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class GatewayControllerTest {
    @Autowired MockMvc mvc;
    @Autowired EventRepository repository;
    @MockBean AccountClient accountClient;

    @BeforeEach
    void clean() {
        repository.deleteAll();
        when(accountClient.applyTransaction(anyString(), anyString(), anyDouble(), anyString(), anyString()))
                .thenReturn(Mono.empty());
    }

    @Test
    void submitsAndDuplicatesEvent() throws Exception {
        String payload = "{\"eventId\":\"evt-1\",\"accountId\":\"acct-1\",\"type\":\"CREDIT\",\"amount\":10.0,\"currency\":\"USD\",\"eventTimestamp\":\"2026-06-05T10:00:00Z\"}";
        mvc.perform(post("/events").contentType(MediaType.APPLICATION_JSON).content(payload))
                .andExpect(status().isCreated())
                .andExpect(header().exists("X-Trace-Id"));

        mvc.perform(post("/events").contentType(MediaType.APPLICATION_JSON).content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eventId").value("evt-1"));
    }

    @Test
    void validatesInput() throws Exception {
        String bad = "{\"eventId\":\"bad\",\"accountId\":\"acct\",\"type\":\"UNKNOWN\",\"amount\":0,\"currency\":\"USD\",\"eventTimestamp\":\"2026-06-05T10:00:00Z\"}";
        mvc.perform(post("/events").contentType(MediaType.APPLICATION_JSON).content(bad))
                .andExpect(status().isBadRequest());
    }

    @Test
    void returnsEventsInChronologicalOrder() throws Exception {
        repository.save(new com.example.gateway.model.EventRecord("e2", "acct-1", "DEBIT", 5.0, "USD", java.time.Instant.parse("2026-06-05T12:00:00Z"), "", java.time.Instant.now(), "SUBMITTED"));
        repository.save(new com.example.gateway.model.EventRecord("e1", "acct-1", "CREDIT", 10.0, "USD", java.time.Instant.parse("2026-06-05T11:00:00Z"), "", java.time.Instant.now(), "SUBMITTED"));
        mvc.perform(get("/events").param("account", "acct-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].eventId").value("e1"))
                .andExpect(jsonPath("$[1].eventId").value("e2"));
    }

    @Test
    void returnsServiceUnavailableWhenAccountServiceFails() throws Exception {
        when(accountClient.applyTransaction(anyString(), anyString(), anyDouble(), anyString(), anyString()))
                .thenReturn(Mono.error(new RuntimeException("boom")));
        String payload = "{\"eventId\":\"evt-fail\",\"accountId\":\"acct-1\",\"type\":\"CREDIT\",\"amount\":10.0,\"currency\":\"USD\",\"eventTimestamp\":\"2026-06-05T10:00:00Z\"}";
        mvc.perform(post("/events").contentType(MediaType.APPLICATION_JSON).content(payload))
                .andExpect(status().isServiceUnavailable());
    }
}
