package com.practice.comments.dto;

import com.practice.comments.entity.Comment;

import java.time.LocalDateTime;

public record CommentResponse(Integer id, Integer postId, Integer authorId, String authorUsername,
                              String text, LocalDateTime createdAt) {

    public static CommentResponse from(Comment comment) {
        return new CommentResponse(comment.getId(), comment.getPostId(), comment.getAuthorId(),
                comment.getAuthorUsername(), comment.getText(), comment.getCreatedAt());
    }
}
