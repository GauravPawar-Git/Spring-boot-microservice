package com.practice.first_project.handlers;

public class PostLimitExceededException extends RuntimeException {

    public PostLimitExceededException(String message) {
        super(message);
    }
}
