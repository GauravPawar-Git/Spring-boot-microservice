package com.practice.users.security;

import com.practice.users.entity.Role;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.security.SignatureException;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Plain unit test, no Spring: what a token contains and which tokens are rejected.
 */
class JwtServiceTest {

    private static final String SECRET = "fOdhuXCFjqBH0/XJj72/vvAFRr11A622jbndlv8cSu8=";
    private static final String OTHER_SECRET = "c2VjcmV0LWtleS1vZi1hbi1hdHRhY2tlci0zMi1ieXQ=";

    private final JwtService jwtService = new JwtService(new JwtProperties(SECRET, Duration.ofHours(1)));
    private final AppUserPrincipal rambo = new AppUserPrincipal(1, "rambo", null, Role.ADMIN);

    @Test
    void tokenRoundTrip_keepsIdUsernameAndRole() {
        String token = jwtService.generateToken(rambo);

        AppUserPrincipal parsed = jwtService.parseToken(token);

        assertThat(token.split("\\.")).hasSize(3); // header.payload.signature
        assertThat(parsed.id()).isEqualTo(1);
        assertThat(parsed.getUsername()).isEqualTo("rambo");
        assertThat(parsed.role()).isEqualTo(Role.ADMIN);
    }

    @Test
    void tamperedPayload_isRejected() {
        String[] parts = jwtService.generateToken(rambo).split("\\.");
        // attacker swaps in a payload of their own (here: the payload of a USER token) but can't re-sign it
        String userPayload = jwtService.generateToken(new AppUserPrincipal(2, "shamlal", null, Role.USER)).split("\\.")[1];
        String forged = parts[0] + "." + userPayload + "." + parts[2];

        assertThatThrownBy(() -> jwtService.parseToken(forged)).isInstanceOf(SignatureException.class);
    }

    @Test
    void tokenSignedWithAnotherKey_isRejected() {
        JwtService attacker = new JwtService(new JwtProperties(OTHER_SECRET, Duration.ofHours(1)));
        String forged = attacker.generateToken(rambo);

        assertThatThrownBy(() -> jwtService.parseToken(forged)).isInstanceOf(SignatureException.class);
    }

    @Test
    void expiredToken_isRejected() {
        JwtService shortLived = new JwtService(new JwtProperties(SECRET, Duration.ofSeconds(-5)));
        String expired = shortLived.generateToken(rambo);

        assertThatThrownBy(() -> jwtService.parseToken(expired)).isInstanceOf(ExpiredJwtException.class);
    }
}
