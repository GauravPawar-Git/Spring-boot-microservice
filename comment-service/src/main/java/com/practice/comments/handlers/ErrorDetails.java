package com.practice.comments.handlers;

import java.time.LocalDateTime;
import java.util.List;

// Same error shape as user-service, so clients see one format across services
public record ErrorDetails(LocalDateTime timeStamp, String message, String details, List<String> errors) {

    public ErrorDetails(String message, String details) {
        this(LocalDateTime.now(), message, details, List.of());
    }
}
