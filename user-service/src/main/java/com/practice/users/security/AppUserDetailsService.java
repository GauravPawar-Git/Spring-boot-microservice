package com.practice.users.security;

import com.practice.users.repositories.UserRepositories;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Used only at login: the AuthenticationManager calls this to load the user, then compares
 * the submitted password with the stored BCrypt hash.
 *
 *   called by: Spring Security's AuthenticationManager (bean in SecurityConfig), during AuthService.login().
 *              Nothing calls it by name: Spring finds the only UserDetailsService bean and wires it in.
 *   calls    : UserRepositories.findByUsername(), then AppUserPrincipal.from()
 * Not used for JWT requests: JwtAuthenticationFilter rebuilds the user from the token without the database.
 */
@Service
public class AppUserDetailsService implements UserDetailsService {

    private final UserRepositories userRepositories;

    public AppUserDetailsService(UserRepositories userRepositories) {
        this.userRepositories = userRepositories;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return userRepositories.findByUsername(username)
                .map(AppUserPrincipal::from)
                .orElseThrow(() -> new UsernameNotFoundException("No user " + username));
    }
}
