package com.practice.first_project.handlers;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Error body returned to clients.
 * A record: immutable, and field order is fixed by the declaration (no mixed-up constructor arguments).
 */
public record ErrorDetails(LocalDateTime timeStamp, String message, String details, List<String> errors) {

    public ErrorDetails(String message, String details) {
        this(LocalDateTime.now(), message, details, List.of());
    }
}
