package com.practice.gateway;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Starts the real gateway with no backend services and no Eureka (disabled for tests in the parent pom).
 * That's enough to check the route table, and that a request for a service with no available instance
 * ends in the fallback's 503 instead of an error.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ApiGatewayApplicationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private RouteLocator routeLocator;

    private WebTestClient client;

    @BeforeEach
    void setUp() {
        client = WebTestClient.bindToServer().baseUrl("http://localhost:" + port).build();
    }

    @Test
    void routesPointAtServiceNamesInEureka() {
        List<Route> routes = routeLocator.getRoutes().collectList().block();

        assertThat(routes).extracting(Route::getId).containsExactlyInAnyOrder("user-service", "comment-service");
        assertThat(routes).extracting(route -> route.getUri().toString())
                .containsExactlyInAnyOrder("lb://user-service", "lb://comment-service");
    }

    @Test
    void unavailableService_getsFallback503_insteadOfAnError() {
        client.get().uri("/jpa/posts")
                .exchange()
                .expectStatus().isEqualTo(503)
                .expectBody()
                .jsonPath("$.message").isEqualTo("user-service is unavailable. Please try again shortly.");
    }

    @Test
    void fallbackKeepsTheMethod_forPostRequests() {
        client.post().uri("/comments")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"postId\":1,\"text\":\"hi\"}")
                .exchange()
                .expectStatus().isEqualTo(503)
                .expectBody()
                .jsonPath("$.message").isEqualTo("comment-service is unavailable. Please try again shortly.");
    }

    @Test
    void healthIsUp() {
        client.get().uri("/actuator/health")
                .exchange()
                .expectStatus().isOk()
                .expectBody().jsonPath("$.status").isEqualTo("UP");
    }
}
