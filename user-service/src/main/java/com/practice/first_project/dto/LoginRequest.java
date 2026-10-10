package com.practice.first_project.dto;

import jakarta.validation.constraints.NotBlank;

// Body of POST /auth/login ({"username":"rambo","password":"password123"}): AuthController.login() -> AuthService.login()
public record LoginRequest(
        @NotBlank(message = "Username is required") String username,
        @NotBlank(message = "Password is required") String password
) {
}
