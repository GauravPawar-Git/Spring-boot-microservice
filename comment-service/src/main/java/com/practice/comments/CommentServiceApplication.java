package com.practice.comments;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * Starts comment-service (port 8082): comments on posts, in its own database.
 *
 * It talks to user-service in two ways:
 *   synchronous  (we ask, we wait): UserServiceClient (Feign) -> GET /jpa/posts/{id}, wrapped by PostLookup
 *                                    in a circuit breaker. Used by CommentService before saving / when reading.
 *   asynchronous (they tell us)    : Kafka topic "posts-deleted", sent by user-service's KafkaEventPublisher,
 *                                    received by PostsDeletedListener -> CommentService.deleteCommentsOfPosts()
 * Login tokens are issued by user-service (AuthService.login()) and only verified here (SecurityConfig).
 * Banned words come from config-server (config-repo/comment-service.properties -> CommentRules).
 */
@SpringBootApplication
@EnableFeignClients // scans for @FeignClient interfaces and generates their HTTP implementations
@ConfigurationPropertiesScan // registers @ConfigurationProperties classes such as CommentRules
public class CommentServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(CommentServiceApplication.class, args);
    }
}
