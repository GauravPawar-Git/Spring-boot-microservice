package com.practice.comments.client;

import com.practice.comments.handlers.UserServiceUnavailableException;
import feign.FeignException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.client.circuitbreaker.CircuitBreaker;
import org.springframework.cloud.client.circuitbreaker.CircuitBreakerFactory;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Every call to user-service goes through the "user-service" circuit breaker.
 * - Post found        -> Optional.of(post)
 * - 404 from upstream -> Optional.empty()      (a normal answer, NOT counted as a failure)
 * - down / slow / 5xx -> fallback runs          (counted; enough of them OPEN the circuit)
 * - circuit OPEN      -> fallback runs at once  (no network call at all)
 *
 *   called by : CommentService.addComment() -> findPost();  CommentService.getCommentsOfPost() -> findPostOrEmpty()
 *   calls     : UserServiceClient.getPost() (Feign -> user-service GET /jpa/posts/{id})
 *   breaker   : "user-service", settings resilience4j.circuitbreaker.instances.user-service.* in application.properties,
 *               state changes logged by CircuitBreakerConfig.logStateTransitions()
 *   test      : PostLookupCircuitBreakerTest
 */
@Component
public class PostLookup {

    private static final Logger log = LoggerFactory.getLogger(PostLookup.class);

    private final UserServiceClient client;
    private final CircuitBreaker circuitBreaker;

    public PostLookup(UserServiceClient client, CircuitBreakerFactory<?, ?> circuitBreakerFactory) {
        this.client = client;
        this.circuitBreaker = circuitBreakerFactory.create("user-service");
    }

    /** For writes: we must know the post exists, so if user-service can't answer, the request fails (503). */
    public Optional<PostSummary> findPost(int postId) {
        return circuitBreaker.run(() -> fetch(postId), throwable -> {
            log.warn("user-service call failed for post {}: {}", postId, throwable.toString());
            // ErrorResponseHandler.handleUnavailable() turns this into a 503 for the client
            throw new UserServiceUnavailableException();
        });
    }

    /** For reads: post details are nice to have, so degrade gracefully and return empty instead of failing. */
    public Optional<PostSummary> findPostOrEmpty(int postId) {
        return circuitBreaker.run(() -> fetch(postId), throwable -> {
            log.warn("user-service unavailable, serving comments without post details: {}", throwable.toString());
            return Optional.empty();
        });
    }

    private Optional<PostSummary> fetch(int postId) {
        try {
            return Optional.of(client.getPost(postId));
        } catch (FeignException.NotFound notFound) {
            // user-service answered 404 (PostNotFoundException there): the post doesn't exist, the service is fine
            return Optional.empty();
        }
    }
}
