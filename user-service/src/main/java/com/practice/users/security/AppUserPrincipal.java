package com.practice.users.security;

import com.practice.users.entity.Role;
import com.practice.users.entity.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/**
 * The logged-in user as Spring Security sees it. It is what `authentication.principal` returns,
 * so @PreAuthorize expressions can use principal.id.
 * password is only filled during login; when built from a JWT it is null (the token already proved identity).
 *
 *   built by  : AppUserDetailsService.loadUserByUsername() (login, from the User entity)
 *               JwtService.parseToken() (every authenticated request, from the token's claims)
 *   used by   : AuthService.login() -> JwtService.generateToken(); AuthController.me() via @AuthenticationPrincipal;
 *               @PreAuthorize("... #id == principal.id") in UserJPAResources and PostJPAResources
 */
public record AppUserPrincipal(Integer id, String username, String password, Role role) implements UserDetails {

    public static AppUserPrincipal from(User user) {
        return new AppUserPrincipal(user.getId(), user.getUsername(), user.getPassword(), user.getRole());
    }

    // Spring Security checks roles as authorities with a "ROLE_" prefix: hasRole('ADMIN') == hasAuthority('ROLE_ADMIN')
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    // JavaBean-style getter so SpEL can read "principal.id" in @PreAuthorize
    public Integer getId() {
        return id;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
    }
}
