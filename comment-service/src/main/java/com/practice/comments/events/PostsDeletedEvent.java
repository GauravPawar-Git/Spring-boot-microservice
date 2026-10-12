package com.practice.comments.events;

import java.util.List;

/**
 * Message published by user-service on topic "posts-deleted". Each service keeps its own copy of the
 * event class: services share a message FORMAT (the JSON contract), not code.
 *
 *   sent by    : user-service KafkaEventPublisher.onPostsDeleted()
 *                (from user-service's own record com.practice.users.events.PostsDeletedEvent)
 *   read by    : PostsDeletedListener.onPostsDeleted(), which turns the JSON back into this record
 * Field names must match user-service's record, or the JSON won't map.
 */
public record PostsDeletedEvent(List<Integer> postIds, String reason) {
}
