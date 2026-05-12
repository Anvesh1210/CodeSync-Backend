package com.codesync.version.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(info = @Info(title = "Version Service API", version = "1.0", description = "Version/Snapshot Service API documentation"))
public class OpenApiConfig {
}
