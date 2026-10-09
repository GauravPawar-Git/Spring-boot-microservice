package com.practice.first_project;

import com.practice.first_project.dto.LoginRequest;
import com.practice.first_project.dto.TokenResponse;
import com.practice.first_project.events.KafkaEventPublisher;
import com.practice.first_project.service.AuthService;
import com.practice.first_project.service.UserService;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The prod profile against REAL infrastructure: PostgreSQL 17 and Apache Kafka, each started in a throwaway
 * Docker container by Testcontainers. @ServiceConnection hands their addresses to Spring Boot, so no
 * datasource URL or bootstrap-servers property is needed.
 *
 * disabledWithoutDocker = true: skipped (not failed) on machines without Docker, runs in CI.
 */
@SpringBootTest(properties = {
        "app.events.enabled=true",
        "DB_PASSWORD=unused-by-service-connection",     // prod profile placeholders must resolve
        "JWT_SECRET=fOdhuXCFjqBH0/XJj72/vvAFRr11A622jbndlv8cSu8="
})
@ActiveProfiles("prod")
@Testcontainers(disabledWithoutDocker = true)
class RealInfrastructureContainersTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer(
            // official image via the ECR mirror, declared compatible with "postgres"
            DockerImageName.parse("public.ecr.aws/docker/library/postgres:17-alpine").asCompatibleSubstituteFor("postgres"));

    @Container
    @ServiceConnection
    static KafkaContainer kafka = new KafkaContainer("apache/kafka:4.3.1");

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private AuthService authService;

    @Autowired
    private UserService userService;

    @Test
    void runsOnRealPostgres_andPublishesToRealKafka() {
        // Flyway ran all migrations on PostgreSQL 17
        String version = jdbc.queryForObject("select version()", String.class);
        assertThat(version).startsWith("PostgreSQL 17");
        assertThat(jdbc.queryForObject("select count(*) from flyway_schema_history where success", Integer.class)).isEqualTo(4);

        // BCrypt login with a Flyway seed user
        TokenResponse token = authService.login(new LoginRequest("rambo", "password123"));
        assertThat(token.accessToken()).isNotBlank();

        // Deleting Diga publishes her post ids to the real broker after commit
        try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(Map.of(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers(),
                ConsumerConfig.GROUP_ID_CONFIG, "containers-test",
                ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest",
                ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class,
                ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class))) {
            consumer.subscribe(List.of(KafkaEventPublisher.TOPIC));

            userService.deleteUser(4);

            List<String> received = new ArrayList<>();
            long deadline = System.currentTimeMillis() + 20_000;
            while (received.isEmpty() && System.currentTimeMillis() < deadline) {
                ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(500));
                for (ConsumerRecord<String, String> record : records) {
                    received.add(record.value());
                }
            }
            assertThat(received).containsExactly("{\"postIds\":[5,6],\"reason\":\"USER_DELETED\"}");
        }
    }
}
