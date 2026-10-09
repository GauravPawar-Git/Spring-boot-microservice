package com.practice.comments.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CommentRequest(

        @NotNull(message = "postId is required")
        Integer postId,

        @NotBlank(message = "Text is required")
        @Size(max = 1000, message = "Text must be at most 1000 characters")
        String text
) {
}
