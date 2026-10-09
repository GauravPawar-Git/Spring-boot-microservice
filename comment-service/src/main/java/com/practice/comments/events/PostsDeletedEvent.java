package com.practice.comments.events;

import java.util.List;

/**
 * Message published by user-service on topic "posts-deleted". Each service keeps its own copy of the
 * event class: services share a message FORMAT (the JSON contract), not code.
 */
public record PostsDeletedEvent(List<Integer> postIds, String reason) {
}
