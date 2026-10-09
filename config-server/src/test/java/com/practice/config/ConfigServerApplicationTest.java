package com.practice.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;

// Starts the real server and asks it for comment-service's configuration, like a client would
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ConfigServerApplicationTest {

    @LocalServerPort
    int port;

    @Test
    void servesCommentServiceConfig_fromConfigRepo() {
        String body = RestClient.create("http://localhost:" + port)
                .get().uri("/comment-service/default")
                .retrieve().body(String.class);

        assertThat(body).contains("app.comments.banned-words");      // from config-repo/comment-service.properties
        assertThat(body).contains("management.tracing.sampling.probability"); // from config-repo/application.properties
    }
}
