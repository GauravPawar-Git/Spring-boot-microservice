package com.practice.first_project.controller;

import com.practice.first_project.dto.LoginRequest;
import com.practice.first_project.dto.RegisterRequest;
import com.practice.first_project.dto.TokenResponse;
import com.practice.first_project.dto.UserResponse;
import com.practice.first_project.security.AppUserPrincipal;
import com.practice.first_project.service.AuthService;
import com.practice.first_project.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * Public login/register endpoints (permitAll in SecurityConfig), plus /auth/me.
 *
 *   requests arrive from: api-gateway route "user-service" (Path=/auth/**)
 *   calls               : AuthService.register() / AuthService.login(), UserService.getUserById()
 *   the token from login is sent back as "Authorization: Bearer <token>" on later requests and checked by
 *   JwtAuthenticationFilter here, and by the OAuth2 resource server in comment-service.
 */
@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;
    private final UserService userService;

    public AuthController(AuthService authService, UserService userService) {
        this.authService = authService;
        this.userService = userService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    @PostMapping("/login")
    public TokenResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    // @AuthenticationPrincipal injects the user the JwtAuthenticationFilter put in the SecurityContext
    @GetMapping("/me")
    public UserResponse me(@AuthenticationPrincipal AppUserPrincipal principal) {
        return userService.getUserById(principal.id());
    }
}
