package com.practice.users.repositories;

import com.practice.users.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * JPA slice test: starts only JPA + an in-memory H2 database (no web layer).
 * Each test runs in a transaction that is rolled back afterwards, so tests don't affect each other.
 * Note: data.sql also runs here, so the 4 seed users are present.
 */
@DataJpaTest
class UserRepositoriesTest {

    @Autowired
    private UserRepositories userRepositories;

    @Test
    void save_generatesId() {
        User saved = userRepositories.save(new User("Neo", LocalDate.of(1990, 3, 3)));

        assertThat(saved.getId()).isNotNull();
        assertThat(userRepositories.findById(saved.getId())).isPresent();
    }

    @Test
    void findAll_withPageable_returnsOnePageSortedByName() {
        Page<User> page = userRepositories.findAll(PageRequest.of(0, 2, Sort.by("name")));

        assertThat(page.getContent()).hasSize(2);
        assertThat(page.getTotalElements()).isEqualTo(4);
        assertThat(page.getContent().getFirst().getName()).isEqualTo("Diga");
    }
}
