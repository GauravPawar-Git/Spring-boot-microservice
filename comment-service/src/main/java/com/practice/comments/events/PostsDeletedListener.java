package com.practice.comments.events;

import com.practice.comments.service.CommentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

/**
 * Asynchronous clean-up: when user-service deletes posts, remove their comments here.
 * user-service doesn't call us and doesn't wait for us; if this service is down, Kafka keeps the
 * message and we process it when we're back (the consumer group remembers its offset).
 *
 * autoStartup is driven by app.events.enabled, so without a broker the listener simply doesn't start.
 */
@Component
public class PostsDeletedListener {

    static final String TOPIC = "posts-deleted";
    private static final Logger log = LoggerFactory.getLogger(PostsDeletedListener.class);

    private final CommentService commentService;
    private final JsonMapper jsonMapper;

    public PostsDeletedListener(CommentService commentService, JsonMapper jsonMapper) {
        this.commentService = commentService;
        this.jsonMapper = jsonMapper;
    }

    @KafkaListener(topics = TOPIC, autoStartup = "${app.events.enabled:false}")
    public void onPostsDeleted(String message) {
        PostsDeletedEvent event = jsonMapper.readValue(message, PostsDeletedEvent.class);
        long removed = commentService.deleteCommentsOfPosts(event.postIds());
        // Deleting by post id is idempotent: if Kafka redelivers this message, the second run removes 0 rows
        log.info("posts-deleted {} ({}): removed {} comments", event.postIds(), event.reason(), removed);
    }
}
