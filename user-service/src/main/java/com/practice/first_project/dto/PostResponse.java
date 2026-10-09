package com.practice.first_project.dto;

import com.practice.first_project.entity.Post;

import java.time.LocalDateTime;

public record PostResponse(Integer id, String description, LocalDateTime createdAt, Integer authorId, String authorName) {

    // Note: post.getUser().getName() touches the LAZY user. If the user wasn't fetched with the post,
    // this line runs one extra SELECT per user -> the N+1 problem.
    public static PostResponse from(Post post) {
        return new PostResponse(post.getId(), post.getDescription(), post.getCreatedAt(),
                post.getUser().getId(), post.getUser().getName());
    }
}
