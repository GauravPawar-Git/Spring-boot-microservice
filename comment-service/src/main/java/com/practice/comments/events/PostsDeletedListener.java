package com.practice.comments.events;

import com.practice.comments.service.CommentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

/**
 * KAFKA CONSUMER. Asynchronous clean-up: when user-service deletes posts, remove their comments here.
 * user-service doesn't call us and doesn't wait for us; if this service is down, Kafka keeps the
 * message and we process it when we're back (the consumer group remembers its offset).
 *
 * Why it exists: a comment only stores postId (a plain number, see Comment). The post itself lives in
 * user-service's database, so no foreign key / cascade can delete our comments. This listener is the
 * "cascade" across services.
 *
 * Where the message comes from (user-service module, package com.practice.first_project):
 *   UserJPAResources.deleteUserById()  (DELETE /jpa/users/{id}, ADMIN only)
 *     -> UserService.deleteUser()           publishes PostsDeletedEvent inside user-service
 *     -> KafkaEventPublisher.onPostsDeleted()   after commit: kafkaTemplate.send("posts-deleted", json)
 *     -> Kafka broker (docker-compose service "kafka" / helm templates/kafka.yaml)
 *     -> THIS class's onPostsDeleted(String)
 *     -> CommentService.deleteCommentsOfPosts() -> CommentRepository.deleteByPostIdIn()
 *
 * Nobody in this service calls onPostsDeleted(): Spring Kafka creates a consumer for every @KafkaListener
 * method, polls the topic, and calls the method once per message.
 *
 * Switches (comment-service application.properties):
 *   app.events.enabled=${EVENTS_ENABLED:false}       autoStartup below: without a broker the listener doesn't start
 *   spring.kafka.bootstrap-servers=${KAFKA_SERVERS:...}
 *   spring.kafka.consumer.group-id=comment-service    instances of this service share the work and the offset
 *   spring.kafka.consumer.auto-offset-reset=earliest  a brand-new group starts at the oldest message
 * Test: PostsDeletedListenerKafkaTest (embedded broker).
 */
@Component
public class PostsDeletedListener {

    // must match KafkaEventPublisher.TOPIC in user-service
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
        // message = the JSON written by user-service, e.g. {"postIds":[7,8],"reason":"USER_DELETED"}
        PostsDeletedEvent event = jsonMapper.readValue(message, PostsDeletedEvent.class);
        long removed = commentService.deleteCommentsOfPosts(event.postIds());
        // Deleting by post id is idempotent: if Kafka redelivers this message, the second run removes 0 rows
        log.info("posts-deleted {} ({}): removed {} comments", event.postIds(), event.reason(), removed);
    }
}
