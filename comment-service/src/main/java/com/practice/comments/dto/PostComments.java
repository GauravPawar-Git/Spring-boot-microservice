package com.practice.comments.dto;

import java.util.List;

/**
 * Comments of one post plus the post's details from user-service.
 * postDetailsAvailable=false means user-service couldn't be reached: the comments (our own data) are still served.
 */
public record PostComments(Integer postId, boolean postDetailsAvailable, String postDescription,
                           String postAuthor, List<CommentResponse> comments) {
}
