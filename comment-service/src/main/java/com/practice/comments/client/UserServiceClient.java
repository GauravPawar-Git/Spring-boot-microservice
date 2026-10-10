package com.practice.comments.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * Declarative HTTP client: Spring Cloud OpenFeign generates the implementation.
 * name = "user-service" is a Eureka service name, not a host: the load balancer picks a live instance.
 *
 *   enabled by : @EnableFeignClients on CommentServiceApplication
 *   called by  : PostLookup.fetch() only (always inside the circuit breaker; never call it directly)
 *   lands in   : user-service PostJPAResources.getPost() -> PostService.getPost()
 *   timeouts   : spring.cloud.openfeign.client.config.user-service.* in application.properties
 * No token is sent: GET /jpa/posts/{id} is public in user-service's SecurityConfig.
 */
@FeignClient(name = "user-service")
public interface UserServiceClient {

    // calls GET http://<a user-service instance>/jpa/posts/{id}
    @GetMapping("/jpa/posts/{id}")
    PostSummary getPost(@PathVariable("id") int id);
}
