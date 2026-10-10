package com.practice.comments.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

/**
 * Unlike user-service (hand-written JwtAuthenticationFilter), this service uses Spring Security's
 * built-in OAuth2 Resource Server: BearerTokenAuthenticationFilter reads the header and our JwtDecoder
 * verifies it. Same tokens, same secret, a fraction of the code.
 *
 *   tokens come from : user-service AuthService.login() -> JwtService.generateToken() (claims sub, uid, role)
 *   secret           : app.jwt.secret in application.properties (JWT_SECRET env var); must equal user-service's
 *   verified token   : reaches CommentController as @AuthenticationPrincipal Jwt
 *   no / bad token   : Spring answers 401 itself (no JSON body, unlike user-service's JsonSecurityErrorHandlers)
 */
@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.GET, "/comments/**").permitAll()
                        // "/**" also allows the Kubernetes probe groups /actuator/health/liveness and /readiness (Phase 8)
                        .requestMatchers("/actuator/health", "/actuator/health/**", "/h2-console/**", "/error").permitAll()
                        // metrics are scraped by Prometheus; in production keep this port internal-only
                        .requestMatchers("/actuator/prometheus").permitAll()
                        // reloading configuration changes behaviour: admins only
                        .requestMatchers("/actuator/refresh").hasRole("ADMIN")
                        .anyRequest().authenticated())
                .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()))
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())));
        return http.build();
    }

    // Verifies the HS256 signature with the shared secret, and checks "exp"
    @Bean
    public JwtDecoder jwtDecoder(@Value("${app.jwt.secret}") String secret) {
        SecretKey key = new SecretKeySpec(Base64.getDecoder().decode(secret), "HmacSHA256");
        return NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build();
    }

    // user-service puts the role in a "role" claim ("ADMIN"); turn it into the authority ROLE_ADMIN
    private JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter authorities = new JwtGrantedAuthoritiesConverter();
        authorities.setAuthoritiesClaimName("role");
        authorities.setAuthorityPrefix("ROLE_");
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(authorities);
        return converter;
    }
}
