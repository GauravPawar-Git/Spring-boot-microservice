package com.practice.first_project.service;

import com.practice.first_project.dto.PostRequest;
import com.practice.first_project.handlers.PostLimitExceededException;
import com.practice.first_project.repositories.PostRepository;
import com.practice.first_project.repositories.UserRepositories;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Full-context test: the real PostService with its real @Transactional proxy and the H2 database.
 * Deliberately NOT annotated @Transactional itself, so we observe the service's own commit/rollback.
 */
@SpringBootTest
class PostServiceTransactionTest {

    @Autowired
    private PostService postService;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private UserRepositories userRepositories;

    @Test
    void createPosts_rollsBackEveryInsert_whenLimitIsCrossedPartWay() {
        // Rambo has 2 seeded posts; 9 more crosses the limit of 10 on the 9th insert
        int rambo = userRepositories.findAll().stream()
                .filter(u -> u.getName().equals("Rambo")).findFirst().orElseThrow().getId();
        long before = postRepository.countByUserId(rambo);
        List<PostRequest> nine = IntStream.rangeClosed(1, 9)
                .mapToObj(i -> new PostRequest("bulk post " + i))
                .toList();

        assertThatThrownBy(() -> postService.createPosts(rambo, nine))
                .isInstanceOf(PostLimitExceededException.class);

        // all 9 INSERTs really ran (the 9th made it 11), but the rollback undid every one
        assertThat(postRepository.countByUserId(rambo)).isEqualTo(before);
    }
}
