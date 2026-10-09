package com.practice.first_project.service;

import com.practice.first_project.bean.User;
import com.practice.first_project.dto.UserRequest;
import com.practice.first_project.dto.UserResponse;
import com.practice.first_project.handlers.UserNotFoundException;
import com.practice.first_project.repositories.UserRepositories;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Business logic lives here, not in the controller.
 * The controller handles HTTP; the repository handles the database; the service connects them.
 */
@Service
@Transactional(readOnly = true) // default for every method; write methods override it below
public class UserService {

    // final + constructor injection: the dependency is required and can't be changed later
    private final UserRepositories userRepositories;

    // only one constructor, so Spring uses it automatically (no @Autowired needed)
    public UserService(UserRepositories userRepositories) {
        this.userRepositories = userRepositories;
    }

    public Page<UserResponse> getAllUsers(Pageable pageable) {
        return userRepositories.findAll(pageable).map(UserResponse::from);
    }

    public UserResponse getUserById(int id) {
        return UserResponse.from(findUserOrThrow(id));
    }

    @Transactional
    public UserResponse createUser(UserRequest request) {
        User saved = userRepositories.save(new User(request.name(), request.birthDate()));
        return UserResponse.from(saved);
    }

    @Transactional
    public UserResponse updateUser(int id, UserRequest request) {
        User user = findUserOrThrow(id);
        user.setName(request.name());
        user.setBirthDate(request.birthDate());
        // no save() call needed: inside a transaction, JPA "dirty checking" writes the changes on commit
        return UserResponse.from(user);
    }

    @Transactional
    public void deleteUser(int id) {
        if (!userRepositories.existsById(id)) {
            throw new UserNotFoundException("User not found with id: " + id);
        }
        userRepositories.deleteById(id);
    }

    private User findUserOrThrow(int id) {
        return userRepositories.findById(id)
                .orElseThrow(() -> new UserNotFoundException("User not found with id: " + id));
    }
}
