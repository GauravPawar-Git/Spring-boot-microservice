package com.practice.first_project.security;

import com.practice.first_project.repositories.UserRepositories;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Used only at login: the AuthenticationManager calls this to load the user, then compares
 * the submitted password with the stored BCrypt hash.
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
