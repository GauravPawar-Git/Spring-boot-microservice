package com.practice.discovery;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;

// Starts the real Eureka server and reads its (empty) registry, like a client would
// The parent pom disables the Eureka client for all tests; the Eureka SERVER needs it, so turn it back on here
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = "eureka.client.enabled=true")
class DiscoveryServerApplicationTest {

    @LocalServerPort
    int port;

    @Test
    void registryEndpointAnswers() {
        String apps = RestClient.create("http://localhost:" + port)
                .get().uri("/eureka/apps")
                .header("Accept", "application/json")
                .retrieve().body(String.class);

        assertThat(apps).contains("\"applications\"");
    }
}
