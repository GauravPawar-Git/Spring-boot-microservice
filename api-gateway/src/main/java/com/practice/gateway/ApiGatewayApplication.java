package com.practice.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Clients talk only to this gateway (port 8765). Routes are defined in application.properties:
 * the path decides the target service, "lb://name" asks Eureka + the load balancer for a live instance.
 *
 *   /auth/**, /jpa/**  -> user-service    (AuthController, UserJPAResources, PostJPAResources)
 *   /comments/**       -> comment-service (CommentController)
 *   service down       -> CircuitBreaker filter forwards to FallbackController (503)
 * No Java routing code: Spring Cloud Gateway builds the routes from the properties at startup.
 * The gateway doesn't check tokens; it passes the Authorization header on and each service verifies it.
 */
@SpringBootApplication
public class ApiGatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(ApiGatewayApplication.class, args);
    }
}
