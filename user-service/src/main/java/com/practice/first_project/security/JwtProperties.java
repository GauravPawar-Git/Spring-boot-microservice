package com.practice.first_project.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Type-safe binding of the app.jwt.* properties:
 *   app.jwt.secret=base64 key (at least 256 bits for HS256)
 *   app.jwt.expiration=1h
 * Values: application.properties (expiration), application-dev.properties / JWT_SECRET env var (secret).
 * Registered by @EnableConfigurationProperties in SecurityConfig; used by JwtService's constructor.
 * comment-service must use the SAME secret (its app.jwt.secret), or it rejects these tokens.
 */
@ConfigurationProperties(prefix = "app.jwt")
public record JwtProperties(String secret, Duration expiration) {
}
