package com.practice.first_project.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableMethodSecurity // turns on @PreAuthorize on controller/service methods
@EnableConfigurationProperties(JwtProperties.class)
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, JwtService jwtService,
                                                   JsonSecurityErrorHandlers errorHandlers) throws Exception {
        http
                // CSRF protects cookie-based sessions. We send the token in a header and keep no session,
                // so a malicious site can't make the browser attach credentials automatically.
                .csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
                // STATELESS: no HttpSession, no JSESSIONID cookie. Every request must carry its own JWT.
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())

                // URL rules: checked top to bottom, first match wins
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/auth/register", "/auth/login").permitAll()
                        .requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        // "/**" also allows the Kubernetes probe groups /actuator/health/liveness and /readiness (Phase 8)
                        .requestMatchers("/actuator/health", "/actuator/health/**", "/error").permitAll()
                        // Phase 6: metrics for Prometheus; in production expose actuator on an internal port only
                        .requestMatchers("/actuator/prometheus").permitAll()
                        .requestMatchers("/h2-console/**").permitAll()
                        // the N+1 demo stays behind login; must come before the "/jpa/posts/*" rule below
                        .requestMatchers("/jpa/posts/n-plus-one").authenticated()
                        .requestMatchers(HttpMethod.GET, "/jpa/posts", "/jpa/posts/*", "/jpa/users/*/posts").permitAll()
                        .anyRequest().authenticated())

                // the H2 console is drawn in <frame>s: allow framing from our own origin only
                .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()))

                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(errorHandlers)   // 401
                        .accessDeniedHandler(errorHandlers))       // 403

                // our JWT check runs before Spring's own username/password filter
                .addFilterBefore(new JwtAuthenticationFilter(jwtService), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    // BCrypt: slow on purpose (cost 10 = 2^10 rounds) and salted, so equal passwords get different hashes
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // Built by Spring from our UserDetailsService + PasswordEncoder beans; AuthService uses it to check logins
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    // CORS: lets a browser app on another origin (e.g. a React dev server) call this API
    @Bean
    public CorsConfigurationSource corsConfigurationSource(
            @Value("${app.cors.allowed-origins:http://localhost:3000}") List<String> allowedOrigins) {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(allowedOrigins);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
