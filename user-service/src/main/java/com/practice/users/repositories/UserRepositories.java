package com.practice.users.repositories;

import com.practice.users.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Database access for User (table "user_details", Flyway V1 + V4). Spring Data JPA generates the implementation.
 *
 *   used by: UserService, PostService (find the author), AuthService (register), AppUserDetailsService (login)
 */
public interface UserRepositories extends JpaRepository<User, Integer> {

    // login: AppUserDetailsService.loadUserByUsername()
    Optional<User> findByUsername(String username);

    // registration: AuthService.register() -> 409 UsernameTakenException if true
    boolean existsByUsername(String username);
}
