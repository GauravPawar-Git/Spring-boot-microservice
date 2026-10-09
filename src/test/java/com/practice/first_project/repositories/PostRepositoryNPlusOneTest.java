package com.practice.first_project.repositories;

import com.practice.first_project.bean.Post;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.data.domain.Sort;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Proves the N+1 problem by COUNTING the SQL statements Hibernate sends.
 * Seed data (V3__seed_data.sql): 6 posts written by 4 different users.
 */
@DataJpaTest(properties = "spring.jpa.properties.hibernate.generate_statistics=true")
class PostRepositoryNPlusOneTest {

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    private Statistics statistics;

    @BeforeEach
    void setUp() {
        entityManager.clear(); // empty the first-level cache so every entity must come from the database
        statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        statistics.clear();
    }

    @Test
    void findAll_thenReadingAuthors_runsOnePlusNQueries() {
        List<Post> posts = postRepository.findAll(Sort.by("createdAt"));
        posts.forEach(post -> post.getUser().getName()); // each new author = one more SELECT

        assertThat(posts).hasSize(6);
        assertThat(statistics.getPrepareStatementCount()).isEqualTo(1 + 4);
    }

    @Test
    void joinFetch_loadsPostsAndAuthorsInOneQuery() {
        List<Post> posts = postRepository.findAllWithUser();
        posts.forEach(post -> post.getUser().getName());

        assertThat(posts).hasSize(6);
        assertThat(statistics.getPrepareStatementCount()).isEqualTo(1);
    }

    @Test
    void entityGraph_loadsPostsAndAuthorsInOneQuery() {
        List<Post> posts = postRepository.findAllBy(Sort.by("createdAt"));
        posts.forEach(post -> post.getUser().getName());

        assertThat(posts).hasSize(6);
        assertThat(statistics.getPrepareStatementCount()).isEqualTo(1);
    }
}
