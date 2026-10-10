package com.practice.first_project.handlers;

// Thrown by AuthService.register() when UserRepositories.existsByUsername() is true; ErrorResponseHandler -> 409.
public class UsernameTakenException extends RuntimeException {

    public UsernameTakenException(String username) {
        super("Username '" + username + "' is already taken");
    }
}
