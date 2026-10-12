package com.practice.users.service;

import com.practice.users.dto.UserRequest;
import com.practice.users.repositories.PostRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * The repository is a Mockito SPY: the real repository runs, but we can count its calls.
 * Own H2 database (cachetest) because the test renames a seed user.
 */
@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:cachetest")
class PostCachingTest {

    @Autowired
    private PostService postService;

    @Autowired
    private UserService userService;

    @MockitoSpyBean
    private PostRepository postRepository;

    @Test
    void secondReadComesFromCache_untilAUserChanges() {
        postService.getPost(1);
        postService.getPost(1);
        verify(postRepository, times(1)).findById(1); // second call never reached the repository

        // Rambo (author of post 1) is renamed: @CacheEvict clears "posts", so the next read is fresh
        userService.updateUser(1, new UserRequest("Rambo Jr", LocalDate.of(1985, 7, 6)));

        assertThat(postService.getPost(1).authorName()).isEqualTo("Rambo Jr");
        verify(postRepository, times(2)).findById(1);
    }
}
