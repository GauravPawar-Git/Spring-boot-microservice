package com.practice.users.handlers;

// Thrown by PostService.createPost() / createPosts() at more than 10 posts; ErrorResponseHandler -> 409.
// A RuntimeException, so @Transactional rolls back the bulk insert.
public class PostLimitExceededException extends RuntimeException {

    public PostLimitExceededException(String message) {
        super(message);
    }
}
