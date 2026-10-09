package com.practice.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.config.server.EnableConfigServer;

/**
 * Hands out configuration over HTTP: GET /{application}/{profile}, e.g. /comment-service/default.
 * Files come from config-repo/ (native mode). In production this is usually a Git repository,
 * so every config change is versioned and reviewed like code.
 */
@SpringBootApplication
@EnableConfigServer
public class ConfigServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(ConfigServerApplication.class, args);
    }
}
