package com.practice.users;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Starts user-service (port 8080): users, posts, login/JWT. Spring scans this package and everything below it.
 *
 * Request path: client -> api-gateway :8765 -> (Eureka "user-service") -> controller -> service -> repository -> DB
 *   controller/   HTTP endpoints          service/   business rules + transactions
 *   repositories/ Spring Data JPA         entity/    database tables
 *   dto/          JSON in and out         security/  JWT login and checks
 *   handlers/     exceptions -> JSON      events/    Kafka producer (posts-deleted -> comment-service)
 * Startup also: Flyway runs db/migration, registers with Eureka (discovery-server), and reads
 * shared settings from config-server (spring.config.import in application.properties).
 */
@SpringBootApplication
public class UserServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(UserServiceApplication.class, args);
	}

}
