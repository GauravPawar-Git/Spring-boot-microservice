package com.practice.users.events;

import java.util.List;

/**
 * "These posts no longer exist." Published inside the application first (ApplicationEventPublisher),
 * then forwarded to Kafka by KafkaEventPublisher once the database transaction has committed.
 *
 *   created in : UserService.deleteUser()                 events.publishEvent(new PostsDeletedEvent(...))
 *   received by: KafkaEventPublisher.onPostsDeleted()      sends it as JSON to topic "posts-deleted"
 *   consumed by: comment-service PostsDeletedListener.onPostsDeleted(), which has its own copy of this record
 *                (com.practice.comments.events.PostsDeletedEvent). Keep the two field names in sync.
 */
public record PostsDeletedEvent(List<Integer> postIds, String reason) {
}
