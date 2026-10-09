package com.practice.first_project.events;

import java.util.List;

/**
 * "These posts no longer exist." Published inside the application first (ApplicationEventPublisher),
 * then forwarded to Kafka by KafkaEventPublisher once the database transaction has committed.
 */
public record PostsDeletedEvent(List<Integer> postIds, String reason) {
}
