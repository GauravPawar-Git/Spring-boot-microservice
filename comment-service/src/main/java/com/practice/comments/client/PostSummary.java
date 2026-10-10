package com.practice.comments.client;

/**
 * The parts of user-service's PostResponse this service needs. Extra JSON fields are ignored,
 * so user-service can add fields without breaking us (tolerant reader).
 * Filled from the JSON of user-service's PostResponse by UserServiceClient.getPost();
 * read in CommentService.getCommentsOfPost() (description, authorName -> PostComments).
 */
public record PostSummary(Integer id, String description, String authorName) {
}
