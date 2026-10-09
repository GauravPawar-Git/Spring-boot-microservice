package com.practice.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Clients talk only to this gateway (port 8765). Routes are defined in application.properties:
 * the path decides the target service, "lb://name" asks Eureka + the load balancer for a live instance.
 */
@SpringBootApplication
public class ApiGatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(ApiGatewayApplication.class, args);
    }
}
