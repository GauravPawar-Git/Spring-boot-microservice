package com.practice.users.security;

import com.practice.users.entity.Role;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

/**
 * Creates and verifies JSON Web Tokens signed with HMAC-SHA256 (HS256).
 * A JWT is header.payload.signature, each part Base64URL-encoded. The payload is readable by anyone;
 * the signature only proves nobody changed it. Never put secrets in a token.
 *
 *   generateToken() called by: AuthService.login()
 *   parseToken()    called by: JwtAuthenticationFilter.doFilterInternal() on every request with a Bearer header
 *   settings        : JwtProperties (app.jwt.secret, app.jwt.expiration)
 * comment-service verifies the same tokens without this class: its SecurityConfig.jwtDecoder() uses the same
 * secret, and reads the "uid" and "role" claims written here (CommentController.userId(), jwtAuthenticationConverter()).
 */
@Service
public class JwtService {

    private final SecretKey key;
    private final Duration expiration;

    public JwtService(JwtProperties properties) {
        this.key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(properties.secret()));
        this.expiration = properties.expiration();
    }

    public String generateToken(AppUserPrincipal user) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(user.getUsername())          // "sub": who the token is about
                .claim("uid", user.id())              // custom claims: saves a DB lookup per request
                .claim("role", user.role().name())
                .issuedAt(Date.from(now))             // "iat"
                .expiration(Date.from(now.plus(expiration)))  // "exp": rejected after this
                .signWith(key)                        // HS256 signature with our secret
                .compact();
    }

    /**
     * Verifies the signature and expiry, then rebuilds the principal from the claims.
     * Throws JwtException (ExpiredJwtException, SignatureException, MalformedJwtException...) if anything is wrong.
     */
    public AppUserPrincipal parseToken(String token) throws JwtException {
        Claims claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return new AppUserPrincipal(
                claims.get("uid", Integer.class),
                claims.getSubject(),
                null,
                Role.valueOf(claims.get("role", String.class)));
    }

    public Duration getExpiration() {
        return expiration;
    }
}
