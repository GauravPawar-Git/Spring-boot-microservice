package com.practice.first_project.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Type-safe binding of the app.jwt.* properties:
 *   app.jwt.secret=base64 key (at least 256 bits for HS256)
 *   app.jwt.expiration=1h
 */
@ConfigurationProperties(prefix = "app.jwt")
public record JwtProperties(String secret, Duration expiration) {
}
