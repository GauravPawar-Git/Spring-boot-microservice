package com.practice.first_project.security;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Runs once per request, before Spring Security's authorization checks.
 * Valid "Authorization: Bearer <jwt>" header -> put the user into the SecurityContext.
 * No header or bad token -> do nothing; the request continues as anonymous, and the
 * authorization rules later answer 401 if the endpoint needs a login.
 *
 * Deliberately NOT a @Component: Spring Boot registers every Filter bean with the servlet container too,
 * so it would run twice (once outside the security chain). SecurityConfig creates it with `new`.
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    static final String JWT_ERROR_ATTRIBUTE = "jwt_error";
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;

    public JwtAuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.startsWith(BEARER_PREFIX)) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            AppUserPrincipal principal = jwtService.parseToken(header.substring(BEARER_PREFIX.length()));

            UsernamePasswordAuthenticationToken authentication =
                    UsernamePasswordAuthenticationToken.authenticated(principal, null, principal.getAuthorities());
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(authentication);
            SecurityContextHolder.setContext(context);
        } catch (JwtException | IllegalArgumentException e) {
            // remembered so the 401 response can say why (expired, bad signature, malformed)
            request.setAttribute(JWT_ERROR_ATTRIBUTE, e.getClass().getSimpleName());
        }

        filterChain.doFilter(request, response);
    }
}
