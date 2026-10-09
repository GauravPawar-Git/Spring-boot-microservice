package com.practice.comments.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * Declarative HTTP client: Spring Cloud OpenFeign generates the implementation.
 * name = "user-service" is a Eureka service name, not a host: the load balancer picks a live instance.
 */
@FeignClient(name = "user-service")
public interface UserServiceClient {

    // calls GET http://<a user-service instance>/jpa/posts/{id}
    @GetMapping("/jpa/posts/{id}")
    PostSummary getPost(@PathVariable("id") int id);
}
