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
 * Forwards PostsDeletedEvent to the Kafka topic "posts-deleted" AFTER the transaction commits.
 * If the delete rolls back, nothing is sent; consumers never hear about posts that still exist.
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

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPostsDeleted(PostsDeletedEvent event) {
        if (!enabled) {
            log.info("Events disabled, not publishing {}", event);
            return;
        }
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

    // Create the topic on startup, only when events are enabled (otherwise there is no broker to talk to)
    @Configuration
    @ConditionalOnProperty(name = "app.events.enabled", havingValue = "true")
    static class TopicConfig {
        @Bean
        NewTopic postsDeletedTopic() {
            return TopicBuilder.name(TOPIC).partitions(1).replicas(1).build();
        }
    }
}
