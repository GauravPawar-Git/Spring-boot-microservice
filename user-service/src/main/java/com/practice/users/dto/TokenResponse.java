package com.practice.users.dto;

// Answer of POST /auth/login, built in AuthService.login() with a token from JwtService.generateToken().
// "Bearer" tells the client how to send it back: Authorization: Bearer <accessToken>
public record TokenResponse(String accessToken, String tokenType, long expiresInSeconds) {

    public TokenResponse(String accessToken, long expiresInSeconds) {
        this(accessToken, "Bearer", expiresInSeconds);
    }
}
