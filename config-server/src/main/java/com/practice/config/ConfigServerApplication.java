package com.practice.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.config.server.EnableConfigServer;

/**
 * Hands out configuration over HTTP: GET /{application}/{profile}, e.g. /comment-service/default.
 * Files come from config-repo/ (native mode). In production this is usually a Git repository,
 * so every config change is versioned and reviewed like code.
 *
 *   clients: user-service, comment-service, api-gateway, via spring.config.import=optional:configserver:...
 *            in their application.properties (read once at startup)
 *   files  : config-repo/application.properties     shared by all (tracing settings)
 *            config-repo/comment-service.properties banned words -> comment-service's CommentRules
 *   live change: edit the file, then POST /actuator/refresh on comment-service (ADMIN token)
 */
@SpringBootApplication
@EnableConfigServer
public class ConfigServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(ConfigServerApplication.class, args);
    }
}
