package com.practice.comments.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.circuitbreaker.resilience4j.Resilience4JCircuitBreakerFactory;
import org.springframework.cloud.client.circuitbreaker.Customizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// Logs CLOSED -> OPEN -> HALF_OPEN -> CLOSED transitions so you can watch the breaker work
@Configuration
public class CircuitBreakerLogging {

    private static final Logger log = LoggerFactory.getLogger(CircuitBreakerLogging.class);

    @Bean
    public Customizer<Resilience4JCircuitBreakerFactory> logStateTransitions() {
        return factory -> factory.addCircuitBreakerCustomizer(
                circuitBreaker -> circuitBreaker.getEventPublisher().onStateTransition(event ->
                        log.warn("Circuit breaker '{}': {}", event.getCircuitBreakerName(), event.getStateTransition())),
                "user-service");
    }
}
