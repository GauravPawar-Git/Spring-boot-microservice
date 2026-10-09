package com.practice.first_project.service;

import com.practice.first_project.bean.User;
import com.practice.first_project.dto.UserRequest;
import com.practice.first_project.dto.UserResponse;
import com.practice.first_project.handlers.UserNotFoundException;
import com.practice.first_project.repositories.UserRepositories;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pure unit test: no Spring context, no database. The repository is a Mockito mock,
 * so we test only UserService's own logic. Runs in milliseconds.
 */
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepositories userRepositories;

    @InjectMocks
    private UserService userService;

    @Test
    void getUserById_returnsUser_whenFound() {
        User user = new User(1, "Adam", LocalDate.of(1995, 5, 20));
        when(userRepositories.findById(1)).thenReturn(Optional.of(user));

        UserResponse response = userService.getUserById(1);

        assertThat(response.id()).isEqualTo(1);
        assertThat(response.name()).isEqualTo("Adam");
    }

    @Test
    void getUserById_throwsNotFound_whenMissing() {
        when(userRepositories.findById(99)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getUserById(99))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    void createUser_savesAndReturnsGeneratedId() {
        UserRequest request = new UserRequest("Eve", LocalDate.of(2000, 1, 1));
        when(userRepositories.save(any(User.class)))
                .thenReturn(new User(42, "Eve", LocalDate.of(2000, 1, 1)));

        UserResponse response = userService.createUser(request);

        assertThat(response.id()).isEqualTo(42);
        assertThat(response.name()).isEqualTo("Eve");
    }

    @Test
    void deleteUser_throwsNotFound_andDeletesNothing_whenMissing() {
        when(userRepositories.existsById(7)).thenReturn(false);

        assertThatThrownBy(() -> userService.deleteUser(7)).isInstanceOf(UserNotFoundException.class);
        verify(userRepositories, never()).deleteById(any());
    }
}
