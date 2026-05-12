package com.codesync.auth.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;

@Configuration
public class OpenApiConfig {

	private static final String SECURITY_SCHEME_NAME = "bearerAuth";
	private static final String SCHEME_HTTP = "bearer";
	private static final String BEARER_FORMAT = "JWT";

	@Bean
	OpenAPI authServiceOpenApi() {
		return new OpenAPI()
				.info(new Info()
						.title("CodeSync Auth Service API")
						.description("Authentication and user management APIs for CodeSync.")
						.version("v1")
						.contact(new Contact().name("CodeSync Team").email("support@codesync.local"))
						.license(new License().name("Internal Use")))
				.addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME))
				.components(new Components().addSecuritySchemes(SECURITY_SCHEME_NAME,
						new SecurityScheme()
								.name(SECURITY_SCHEME_NAME)
								.type(SecurityScheme.Type.HTTP)
								.scheme(SCHEME_HTTP)
								.bearerFormat(BEARER_FORMAT)));
	}
}
