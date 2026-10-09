package com.practice.comments.client;

/**
 * The parts of user-service's PostResponse this service needs. Extra JSON fields are ignored,
 * so user-service can add fields without breaking us (tolerant reader).
 */
public record PostSummary(Integer id, String description, String authorName) {
}
