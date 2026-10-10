package com.practice.first_project.service;

import com.practice.first_project.entity.User;
import com.practice.first_project.dto.LoginRequest;
import com.practice.first_project.dto.RegisterRequest;
import com.practice.first_project.dto.TokenResponse;
import com.practice.first_project.dto.UserResponse;
import com.practice.first_project.handlers.UsernameTakenException;
import com.practice.first_project.repositories.UserRepositories;
import com.practice.first_project.security.AppUserPrincipal;
import com.practice.first_project.security.JwtService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Registration and login.
 *
 *   called by: AuthController.register() (POST /auth/register), AuthController.login() (POST /auth/login)
 *   login flow: authenticationManager.authenticate()
 *                 -> AppUserDetailsService.loadUserByUsername() loads the user from the database
 *                 -> BCrypt (PasswordEncoder bean in SecurityConfig) compares the password with the hash
 *               -> JwtService.generateToken() builds the JWT returned to the client
 *   the token is then checked on every later request by JwtAuthenticationFilter (this service) and by
 *   comment-service's SecurityConfig.jwtDecoder() (same secret).
 */
@Service
public class AuthService {

    private final UserRepositories userRepositories;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public AuthService(UserRepositories userRepositories, PasswordEncoder passwordEncoder,
                       AuthenticationManager authenticationManager, JwtService jwtService) {
        this.userRepositories = userRepositories;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    @Transactional
    public UserResponse register(RegisterRequest request) {
        if (userRepositories.existsByUsername(request.username())) {
            throw new UsernameTakenException(request.username());
        }
        User user = new User(request.name(), request.birthDate());
        // hash before storing; the role is always USER here, a client can never register as ADMIN
        user.setCredentials(request.username(), passwordEncoder.encode(request.password()));
        return UserResponse.from(userRepositories.save(user));
    }

    /**
     * The AuthenticationManager loads the user via AppUserDetailsService and checks the password with BCrypt.
     * Wrong username or password -> BadCredentialsException (the same error for both, so attackers
     * can't find out which usernames exist).
     */
    public TokenResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated(request.username(), request.password()));
        AppUserPrincipal principal = (AppUserPrincipal) authentication.getPrincipal();
        return new TokenResponse(jwtService.generateToken(principal), jwtService.getExpiration().toSeconds());
    }
}
