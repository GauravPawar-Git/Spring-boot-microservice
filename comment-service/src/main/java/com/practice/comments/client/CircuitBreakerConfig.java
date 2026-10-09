package com.practice.comments.client;

import io.micrometer.context.ContextExecutorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.circuitbreaker.resilience4j.Resilience4JCircuitBreakerFactory;
import org.springframework.cloud.client.circuitbreaker.Customizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.Executors;

@Configuration
public class CircuitBreakerConfig {

    private static final Logger log = LoggerFactory.getLogger(CircuitBreakerConfig.class);

    // Logs CLOSED -> OPEN -> HALF_OPEN -> CLOSED transitions so you can watch the breaker work
    @Bean
    public Customizer<Resilience4JCircuitBreakerFactory> logStateTransitions() {
        return factory -> factory.addCircuitBreakerCustomizer(
                circuitBreaker -> circuitBreaker.getEventPublisher().onStateTransition(event ->
                        log.warn("Circuit breaker '{}': {}", event.getCircuitBreakerName(), event.getStateTransition())),
                "user-service");
    }

    /**
     * Phase 6 fix: the circuit breaker's time limiter runs each call on a worker thread. The current trace
     * lives in a ThreadLocal, so without this the Feign call started a NEW trace (seen in Zipkin).
     * ContextExecutorService copies ThreadLocal context (trace, observation) onto the worker thread.
     */
    @Bean
    public Customizer<Resilience4JCircuitBreakerFactory> propagateTraceContext() {
        return factory -> {
            factory.configureExecutorService(ContextExecutorService.wrap(Executors.newCachedThreadPool()));
            factory.configureGroupExecutorService(group -> ContextExecutorService.wrap(Executors.newCachedThreadPool()));
        };
    }
}
