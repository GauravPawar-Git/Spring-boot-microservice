package com.practice.users.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Body of POST /jpa/users/{id}/posts  ({"description":"..."}), and each item of the /bulk array.
 * Checked by @Valid in PostJPAResources (fail -> 400 from ErrorResponseHandler); used by PostService.createPost(s)().
 */
public record PostRequest(

        @NotBlank(message = "Description is required")
        @Size(max = 500, message = "Description must be at most 500 characters")
        String description
) {
}
