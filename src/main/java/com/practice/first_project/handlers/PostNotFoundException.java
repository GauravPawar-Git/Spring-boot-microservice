package com.practice.first_project.handlers;

public class PostNotFoundException extends RuntimeException {

    public PostNotFoundException(int id) {
        super("Post not found with id: " + id);
    }
}
