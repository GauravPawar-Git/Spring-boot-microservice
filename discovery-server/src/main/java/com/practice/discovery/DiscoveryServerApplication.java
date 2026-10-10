package com.practice.discovery;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;

/**
 * Service registry: every service registers its name + address here on startup and sends a heartbeat
 * every 30 s. Clients ask the registry "where is user-service?" instead of hard-coding hosts and ports.
 * Dashboard: http://localhost:8761
 *
 *   registers here : user-service, comment-service, api-gateway (eureka.client.service-url.defaultZone in each)
 *   looks things up: api-gateway ("lb://user-service", "lb://comment-service" routes),
 *                    comment-service (UserServiceClient, @FeignClient(name = "user-service"))
 * No code of ours calls this class: @EnableEurekaServer turns the app into the registry.
 */
@SpringBootApplication
@EnableEurekaServer
public class DiscoveryServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(DiscoveryServerApplication.class, args);
    }
}
