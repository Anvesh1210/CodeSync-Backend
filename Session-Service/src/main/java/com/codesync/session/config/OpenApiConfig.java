package com.codesync.session.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(info = @Info(title = "Session Service API", version = "1.0", description = "Collaboration Session Service API documentation"))
public class OpenApiConfig {
}
