package com.codesync.notification.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI notificationServiceOpenAPI() {
        return new OpenAPI()
                .info(new Info().title("CodeSync Notification Service API")
                        .description("REST APIs for CodeSync Notification Service. Dispatches and persists in-app and email alerts.")
                        .version("v1.0.0")
                        .contact(new Contact().name("CodeSync Team").email("support@codesync.com")));
    }
}
