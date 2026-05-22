package com.codesync.website.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Security configuration for Website-Controller.
 *
 * All existing routes remain open (matching the previous no-security behaviour).
 * Only /admin/** is restricted to ROLE_ADMIN, enforced via the JWT role claim.
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtRoleFilter jwtRoleFilter;

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session ->
                    session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                    // Allow preflight OPTIONS requests
                    .requestMatchers(org.springframework.http.HttpMethod.OPTIONS, "/**").permitAll()
                    .requestMatchers("/api/web/app/executions/**", "/api/web/app/versions/**", "/api/web/app/comments/**", "/api/web/app/session/**", "/api/web/app/project/**", "/api/web/projects/**").authenticated()
                    .requestMatchers("/api/web/admin/**").hasRole("ADMIN")
                    // Everything else (existing routes) — no change in behaviour
                    .anyRequest().permitAll()
            )
            .addFilterBefore(jwtRoleFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

}
