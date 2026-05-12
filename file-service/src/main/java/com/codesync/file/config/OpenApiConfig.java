package com.codesync.file.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(info = @Info(title = "File Service API", version = "1.0", description = "File Service API documentation"))
public class OpenApiConfig {
}
