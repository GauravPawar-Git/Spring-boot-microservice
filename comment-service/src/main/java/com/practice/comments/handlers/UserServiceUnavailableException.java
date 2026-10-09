package com.practice.comments.handlers;

public class UserServiceUnavailableException extends RuntimeException {

    public UserServiceUnavailableException() {
        super("user-service is unavailable, so the post can't be checked. Please try again shortly.");
    }
}
