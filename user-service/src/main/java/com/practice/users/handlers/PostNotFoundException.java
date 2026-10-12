package com.practice.users.handlers;

// Thrown by PostService.getPost(); ErrorResponseHandler.handlePostNotFound() -> 404.
// comment-service relies on this 404: PostLookup.fetch() turns it into "post doesn't exist".
public class PostNotFoundException extends RuntimeException {

    public PostNotFoundException(int id) {
        super("Post not found with id: " + id);
    }
}
