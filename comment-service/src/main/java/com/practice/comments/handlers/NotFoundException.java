package com.practice.comments.handlers;

// Thrown by CommentService: post doesn't exist (addComment) or comment doesn't exist (deleteComment).
// ErrorResponseHandler.handleNotFound() -> 404
public class NotFoundException extends RuntimeException {

    public NotFoundException(String message) {
        super(message);
    }
}
