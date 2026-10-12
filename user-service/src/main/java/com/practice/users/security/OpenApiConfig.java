package com.practice.users.security;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

/**
 * Adds an "Authorize" button to Swagger UI: paste the accessToken from /auth/login once,
 * and Swagger sends "Authorization: Bearer <token>" on every request.
 * Read by springdoc (dependency springdoc-openapi-starter-webmvc-ui) when it builds /v3/api-docs and
 * /swagger-ui.html; no class of ours calls it. SecurityConfig makes those URLs public.
 */
@Configuration
@OpenAPIDefinition(security = @SecurityRequirement(name = "bearerAuth"))
@SecurityScheme(name = "bearerAuth", type = SecuritySchemeType.HTTP, scheme = "bearer", bearerFormat = "JWT")
public class OpenApiConfig {
}
