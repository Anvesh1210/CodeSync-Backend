package com.codesync.gateway.config;

import java.util.Arrays;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

@Configuration
public class CorsConfig {

    @Bean
    public CorsWebFilter corsWebFilter() {
        CorsConfiguration corsConfig = new CorsConfiguration();
        corsConfig.setAllowedOriginPatterns(Arrays.asList("*"));
        corsConfig.setMaxAge(3600L);
        corsConfig.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "HEAD", "PATCH"));
        corsConfig.setAllowedHeaders(Arrays.asList("*"));
        corsConfig.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/auth/**", corsConfig);
        source.registerCorsConfiguration("/projects/**", corsConfig);
        source.registerCorsConfiguration("/files/**", corsConfig);
        source.registerCorsConfiguration("/sessions/**", corsConfig);
        source.registerCorsConfiguration("/executions/**", corsConfig);
        source.registerCorsConfiguration("/versions/**", corsConfig);
        source.registerCorsConfiguration("/comments/**", corsConfig);
        source.registerCorsConfiguration("/notifications/**", corsConfig);
        source.registerCorsConfiguration("/api/web/**", corsConfig);
        source.registerCorsConfiguration("/admin/**", corsConfig);
        source.registerCorsConfiguration("/ws-execution/**", corsConfig);
        source.registerCorsConfiguration("/ws-editor/**", corsConfig);
        source.registerCorsConfiguration("/sessions-ws/**", corsConfig);
        source.registerCorsConfiguration("/notifications-ws/**", corsConfig);
        source.registerCorsConfiguration("/yjs/**", corsConfig);
        source.registerCorsConfiguration("/payments/**", corsConfig);

        return new CorsWebFilter(source);
    }
}
