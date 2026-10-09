package com.practice.comments.client;

import com.practice.comments.handlers.UserServiceUnavailableException;
import feign.FeignException;
import feign.Request;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.circuitbreaker.resilience4j.Resilience4JCircuitBreakerFactory;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Real Spring context with the real Resilience4j circuit breaker and the settings from application.properties;
 * only the Feign client is a mock, so we can make user-service "fail" on demand.
 */
@SpringBootTest
class PostLookupCircuitBreakerTest {

    @MockitoBean
    private UserServiceClient client;

    @Autowired
    private PostLookup postLookup;

    @Autowired
    private Resilience4JCircuitBreakerFactory circuitBreakerFactory;

    private CircuitBreaker breaker;

    @BeforeEach
    void resetBreaker() {
        breaker = circuitBreakerFactory.getCircuitBreakerRegistry().circuitBreaker("user-service");
        breaker.reset();
    }

    @Test
    void opensAfterThreeFailures_thenStopsCallingUserService() {
        when(client.getPost(1)).thenThrow(new RuntimeException("Connection refused"));

        for (int i = 0; i < 3; i++) {
            assertThatThrownBy(() -> postLookup.findPost(1)).isInstanceOf(UserServiceUnavailableException.class);
        }
        assertThat(breaker.getState()).isEqualTo(CircuitBreaker.State.OPEN);

        // OPEN: fails immediately, user-service is not called a 4th time
        assertThatThrownBy(() -> postLookup.findPost(1)).isInstanceOf(UserServiceUnavailableException.class);
        verify(client, times(3)).getPost(1);
    }

    @Test
    void notFound_isAnAnswer_notAFailure() {
        FeignException.NotFound notFound = new FeignException.NotFound("Not Found",
                Request.create(Request.HttpMethod.GET, "/jpa/posts/9", Map.of(), null, null, null), null, Map.of());
        when(client.getPost(9)).thenThrow(notFound);

        for (int i = 0; i < 5; i++) {
            assertThat(postLookup.findPost(9)).isEmpty();
        }
        assertThat(breaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
    }

    @Test
    void readFallback_returnsEmpty_insteadOfFailing() {
        when(client.getPost(1)).thenThrow(new RuntimeException("Connection refused"));

        assertThat(postLookup.findPostOrEmpty(1)).isEmpty();
    }

    @Test
    void success_returnsThePost() {
        when(client.getPost(1)).thenReturn(new PostSummary(1, "Learning Spring Boot", "Rambo"));

        assertThat(postLookup.findPost(1)).contains(new PostSummary(1, "Learning Spring Boot", "Rambo"));
    }
}
