package com.practice.first_project.entity;

/**
 * Stored in User.role. Registration always gives USER; the seed user rambo is ADMIN (V4__add_credentials.sql).
 * Travels inside the JWT as the "role" claim (JwtService.generateToken()), becomes ROLE_USER / ROLE_ADMIN in
 * AppUserPrincipal.getAuthorities(), and is checked by hasRole('ADMIN') in @PreAuthorize.
 */
public enum Role {
    USER,
    ADMIN
}
