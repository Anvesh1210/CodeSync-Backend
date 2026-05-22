package com.codesync.execution.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI executionServiceOpenAPI() {
        return new OpenAPI()
                .info(new Info().title("CodeSync Execution Service API")
                        .description("REST APIs for CodeSync Execution Service. Manages secure code execution in isolated environments.")
                        .version("v1.0.0")
                        .contact(new Contact().name("CodeSync Team").email("support@codesync.com")));
    }
}
