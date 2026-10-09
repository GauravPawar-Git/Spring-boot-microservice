package com.practice.first_project.events;

import com.practice.first_project.service.UserService;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Real Kafka broker inside the test JVM. Deleting a user must publish ONE "posts-deleted" message
 * with that user's post ids, after the transaction commits.
 * Own H2 database (kafkatest) so deleting a seed user doesn't affect other test classes.
 */
@SpringBootTest(properties = {
        "app.events.enabled=true",
        "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}",
        "spring.datasource.url=jdbc:h2:mem:kafkatest"
})
@EmbeddedKafka(partitions = 1, topics = KafkaEventPublisher.TOPIC)
class PostsDeletedKafkaTest {

    @Autowired
    private UserService userService;

    @Autowired
    private EmbeddedKafkaBroker broker;

    @Test
    void deletingAUser_publishesTheirPostIds() {
        Map<String, Object> props = KafkaTestUtils.consumerProps(broker, "test-reader", false);
        props.put("auto.offset.reset", "earliest");
        try (Consumer<String, String> consumer =
                     new DefaultKafkaConsumerFactory<String, String>(props).createConsumer()) {
            broker.consumeFromAnEmbeddedTopic(consumer, KafkaEventPublisher.TOPIC);

            userService.deleteUser(4); // Diga: seed posts 5 and 6

            ConsumerRecord<String, String> record = KafkaTestUtils.getSingleRecord(consumer, KafkaEventPublisher.TOPIC);
            assertThat(record.value()).isEqualTo("{\"postIds\":[5,6],\"reason\":\"USER_DELETED\"}");
        }
    }
}
