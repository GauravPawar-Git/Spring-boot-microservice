package com.practice.users.security;

import com.practice.users.handlers.ErrorDetails;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;

/**
 * Security errors happen in the filter chain, BEFORE any controller runs, so @RestControllerAdvice
 * never sees them. These two handlers write the same ErrorDetails JSON the rest of the API uses.
 *
 *   registered in: SecurityConfig.securityFilterChain() -> exceptionHandling(authenticationEntryPoint / accessDeniedHandler)
 *   commence()   : called by Spring Security when a protected URL is hit without a valid token (401);
 *                  reads the reason left by JwtAuthenticationFilter (JWT_ERROR_ATTRIBUTE)
 *   handle()     : called when a URL rule denies a logged-in user (403). @PreAuthorize denials go to
 *                  ErrorResponseHandler.handleAccessDenied() instead.
 */
@Component
public class JsonSecurityErrorHandlers implements AuthenticationEntryPoint, AccessDeniedHandler {

    private final JsonMapper jsonMapper;

    public JsonSecurityErrorHandlers(JsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }

    // 401 Unauthorized = "we don't know who you are" (no token, expired token, bad signature)
    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException)
            throws IOException {
        Object jwtError = request.getAttribute(JwtAuthenticationFilter.JWT_ERROR_ATTRIBUTE);
        String message = jwtError == null
                ? "Authentication required: send 'Authorization: Bearer <token>'"
                : "Invalid token: " + jwtError;
        write(response, HttpStatus.UNAUTHORIZED, message, request);
    }

    // 403 Forbidden = "we know who you are, and you may not do this"
    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException accessDeniedException)
            throws IOException {
        write(response, HttpStatus.FORBIDDEN, "You do not have permission for this action", request);
    }

    private void write(HttpServletResponse response, HttpStatus status, String message, HttpServletRequest request)
            throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        jsonMapper.writeValue(response.getOutputStream(), new ErrorDetails(message, "uri=" + request.getRequestURI()));
    }
}
