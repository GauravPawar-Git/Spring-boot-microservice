package com.practice.first_project.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record RegisterRequest(

        @NotBlank(message = "Name is required")
        @Size(min = 2, max = 50, message = "Name must be 2-50 characters")
        String name,

        @NotNull(message = "Birth date is required")
        @Past(message = "Birth date must be in the past")
        LocalDate birthDate,

        @Pattern(regexp = "[a-z0-9_]{3,30}", message = "Username must be 3-30 lowercase letters, digits or _")
        String username,

        // BCrypt only uses the first 72 bytes of a password, so longer ones are rejected rather than silently cut
        @NotBlank(message = "Password is required")
        @Size(min = 8, max = 72, message = "Password must be 8-72 characters")
        String password
) {
}
