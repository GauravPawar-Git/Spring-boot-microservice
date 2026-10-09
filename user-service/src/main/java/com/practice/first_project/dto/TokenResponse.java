package com.practice.first_project.dto;

// "Bearer" tells the client how to send it back: Authorization: Bearer <accessToken>
public record TokenResponse(String accessToken, String tokenType, long expiresInSeconds) {

    public TokenResponse(String accessToken, long expiresInSeconds) {
        this(accessToken, "Bearer", expiresInSeconds);
    }
}
