package com.practice.first_project.events;

import org.apache.kafka.clients.admin.NewTopic;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import tools.jackson.databind.json.JsonMapper;

/**
 * KAFKA PRODUCER: forwards PostsDeletedEvent to the Kafka topic "posts-deleted" AFTER the transaction commits.
 * If the delete rolls back, nothing is sent; consumers never hear about posts that still exist.
 *
 * Why it exists: comment-service stores comments with a postId but has no foreign key to user-service's
 * post table (separate databases). When posts disappear, comment-service must be told, or its comments
 * become orphans. A Kafka message tells it without user-service calling (or waiting for) comment-service.
 *
 * The whole chain, step by step:
 *   1. ADMIN sends DELETE /jpa/users/{id}  -> api-gateway routes it to user-service
 *   2. UserJPAResources.deleteUserById()   -> UserService.deleteUser()
 *   3. UserService.deleteUser() deletes the user (+ posts by cascade) and calls
 *      events.publishEvent(new PostsDeletedEvent(postIds, "USER_DELETED"))   (in-process, not Kafka yet)
 *   4. The transaction commits -> Spring calls THIS class's onPostsDeleted()
 *   5. onPostsDeleted() -> kafkaTemplate.send("posts-deleted", json)  -> the Kafka broker stores the message
 *   6. comment-service: PostsDeletedListener.onPostsDeleted(String) (@KafkaListener on "posts-deleted")
 *      -> CommentService.deleteCommentsOfPosts() -> CommentRepository.deleteByPostIdIn()
 *
 * Switches (user-service application.properties):
 *   app.events.enabled=${EVENTS_ENABLED:false}            false locally (no broker): onPostsDeleted only logs
 *   spring.kafka.bootstrap-servers=${KAFKA_SERVERS:...}   where the broker is
 * docker-compose.yml and helm/microservices/values.yaml set EVENTS_ENABLED=true and KAFKA_SERVERS=kafka:9092.
 * KafkaTemplate itself is auto-configured by Spring Boot (String key/value serializers by default).
 * Test: PostsDeletedKafkaTest (embedded broker).
 *
 * Remaining gap: if the app crashes between commit and send, the event is lost. The fix is the
 * transactional outbox pattern (write the event to an outbox table in the same transaction).
 */
@Component
public class KafkaEventPublisher {

    public static final String TOPIC = "posts-deleted";
    private static final Logger log = LoggerFactory.getLogger(KafkaEventPublisher.class);

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final JsonMapper jsonMapper;
    private final boolean enabled;

    public KafkaEventPublisher(KafkaTemplate<String, String> kafkaTemplate, JsonMapper jsonMapper,
                               @Value("${app.events.enabled:false}") boolean enabled) {
        this.kafkaTemplate = kafkaTemplate;
        this.jsonMapper = jsonMapper;
        this.enabled = enabled;
    }

    // Nobody calls this method directly. Spring calls it because of the annotation: it receives every
    // PostsDeletedEvent published with ApplicationEventPublisher.publishEvent() (by UserService.deleteUser()),
    // but holds it until that transaction has committed (AFTER_COMMIT). On rollback it is never called.
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPostsDeleted(PostsDeletedEvent event) {
        if (!enabled) {
            log.info("Events disabled, not publishing {}", event);
            return;
        }
        // The message body is JSON text, e.g. {"postIds":[7,8],"reason":"USER_DELETED"}.
        // comment-service's PostsDeletedListener reads exactly this format back into its own PostsDeletedEvent.
        // send() is asynchronous; the callback reports the outcome without blocking the HTTP request
        kafkaTemplate.send(TOPIC, jsonMapper.writeValueAsString(event))
                .whenComplete((result, error) -> {
                    if (error != null) {
                        log.error("Failed to publish {}", event, error);
                    } else {
                        log.info("Published {} to {} offset {}", event, TOPIC, result.getRecordMetadata().offset());
                    }
                });
    }

    // Create the topic on startup, only when events are enabled (otherwise there is no broker to talk to).
    // Spring Boot's KafkaAdmin sees this NewTopic bean and creates "posts-deleted" on the broker if it's missing.
    @Configuration
    @ConditionalOnProperty(name = "app.events.enabled", havingValue = "true")
    static class TopicConfig {
        @Bean
        NewTopic postsDeletedTopic() {
            return TopicBuilder.name(TOPIC).partitions(1).replicas(1).build();
        }
    }
}
