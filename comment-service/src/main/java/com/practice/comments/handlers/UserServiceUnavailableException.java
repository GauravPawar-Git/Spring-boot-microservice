package com.practice.comments.handlers;

// Thrown by the circuit-breaker fallback in PostLookup.findPost() (user-service down, slow, or circuit OPEN).
// ErrorResponseHandler.handleUnavailable() -> 503
public class UserServiceUnavailableException extends RuntimeException {

    public UserServiceUnavailableException() {
        super("user-service is unavailable, so the post can't be checked. Please try again shortly.");
    }
}
