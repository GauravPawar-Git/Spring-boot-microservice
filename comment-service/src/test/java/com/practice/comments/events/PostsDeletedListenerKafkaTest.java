package com.practice.comments.events;

import com.practice.comments.entity.Comment;
import com.practice.comments.repository.CommentRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.context.EmbeddedKafka;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A real Kafka broker started inside the test JVM (@EmbeddedKafka): we publish the same JSON that
 * user-service publishes and check that the listener deletes the right comments.
 */
@SpringBootTest(properties = {
        "app.events.enabled=true",
        "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}",
        "spring.datasource.url=jdbc:h2:mem:kafkatest"
})
@EmbeddedKafka(partitions = 1, topics = PostsDeletedListener.TOPIC)
class PostsDeletedListenerKafkaTest {

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    private CommentRepository commentRepository;

    @Test
    void deletesCommentsOfDeletedPosts_only() throws Exception {
        commentRepository.save(new Comment(5, 3, "vigo", "on post 5"));
        commentRepository.save(new Comment(6, 3, "vigo", "on post 6"));
        commentRepository.save(new Comment(7, 3, "vigo", "on post 7 - must survive"));

        kafkaTemplate.send(PostsDeletedListener.TOPIC, "{\"postIds\":[5,6],\"reason\":\"USER_DELETED\"}").get();

        // asynchronous: poll until the listener has processed the message (max 15 s)
        Instant deadline = Instant.now().plus(Duration.ofSeconds(15));
        while (commentRepository.count() != 1 && Instant.now().isBefore(deadline)) {
            Thread.sleep(200);
        }
        assertThat(commentRepository.findAll()).extracting(Comment::getPostId).containsExactly(7);
    }
}
