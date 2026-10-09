package com.practice.first_project.security;

import com.practice.first_project.entity.Role;

/**
 * Builds real "Bearer <jwt>" header values for tests, so requests pass through the actual JwtAuthenticationFilter.
 */
public final class TestTokens {

    private TestTokens() {
    }

    public static String bearer(JwtService jwtService, int userId, String username, Role role) {
        return "Bearer " + jwtService.generateToken(new AppUserPrincipal(userId, username, null, role));
    }

    public static String user(JwtService jwtService, int userId) {
        return bearer(jwtService, userId, "user" + userId, Role.USER);
    }

    public static String admin(JwtService jwtService) {
        return bearer(jwtService, 1, "admin", Role.ADMIN);
    }
}
