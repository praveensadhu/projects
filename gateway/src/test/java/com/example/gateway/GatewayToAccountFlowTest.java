package com.example.gateway;

import com.example.account.AccountServiceApplication;
import org.junit.jupiter.api.Test;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.MapPropertySource;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GatewayToAccountFlowTest {
    @Test
    void gatewayCallsAccountServiceAndUpdatesBalance() {
        int accountPort;
        int gatewayPort;

        ConfigurableApplicationContext accountContext = null;
        ConfigurableApplicationContext gatewayContext = null;

        try {
            accountContext = new SpringApplicationBuilder(AccountServiceApplication.class)
                    .properties("spring.profiles.active=test")
                    .run();

            accountPort = accountContext.getEnvironment().getProperty("local.server.port", Integer.class);

            gatewayContext = new SpringApplicationBuilder(GatewayApplication.class)
                    .properties("spring.profiles.active=test")
                    .initializers(context -> context.getEnvironment().getPropertySources().addFirst(
                            new MapPropertySource("test-account-url", Map.of("account.service.base-url", "http://localhost:" + accountPort))
                    ))
                    .run();

            gatewayPort = gatewayContext.getEnvironment().getProperty("local.server.port", Integer.class);

            TestRestTemplate client = new TestRestTemplate();
            Map<String, Object> event = Map.of(
                    "eventId", "flow-1",
                    "accountId", "acct-flow",
                    "type", "CREDIT",
                    "amount", 25.0,
                    "currency", "USD",
                    "eventTimestamp", "2026-06-05T10:00:00Z"
            );

            var response = client.postForEntity("http://localhost:" + gatewayPort + "/events", event, Map.class);
            assertEquals(201, response.getStatusCodeValue());

            var balance = client.getForEntity("http://localhost:" + accountPort + "/accounts/acct-flow/balance", Map.class);
            assertEquals(200, balance.getStatusCodeValue());
            assertEquals(25.0, ((Number) balance.getBody().get("balance")).doubleValue(), 0.01);
            assertTrue(response.getHeaders().containsKey("X-Trace-Id"));
        } finally {
            if (gatewayContext != null) gatewayContext.close();
            if (accountContext != null) accountContext.close();
        }
    }
}
