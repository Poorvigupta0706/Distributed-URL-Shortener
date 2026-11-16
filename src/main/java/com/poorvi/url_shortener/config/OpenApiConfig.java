
package com.poorvi.url_shortener.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;

import org.springdoc.core.customizers.OpenApiCustomizer;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    private static final String SECURITY_SCHEME_NAME = "bearerAuth";

    @Bean
    public OpenAPI customOpenAPI() {

        return new OpenAPI()
                .info(new Info()
                        .title("Distributed URL Shortener API")
                        .version("1.0.0")
                        .description(
                                "REST API documentation for URL Shortener, "
                                + "Authentication and Analytics modules."
                        )
                )
                .components(
                        new Components()
                                .addSecuritySchemes(
                                        SECURITY_SCHEME_NAME,
                                        new SecurityScheme()
                                                .type(SecurityScheme.Type.HTTP)
                                                .scheme("bearer")
                                                .bearerFormat("JWT")
                                )
                );
    }

    @Bean
    public OpenApiCustomizer securityOpenApiCustomizer() {

        return openApi -> {

            // POST /api/urls requires JWT
            Operation createUrl = openApi.getPaths()
                    .get("/api/urls") != null
                    ? openApi.getPaths()
                        .get("/api/urls").getPost()
                    : null;

            addJwtSecurity(createUrl);

            // DELETE /api/urls/{shortCode} requires JWT
            Operation deleteUrl = openApi.getPaths()
                    .get("/api/urls/{shortCode}") != null
                    ? openApi.getPaths()
                        .get("/api/urls/{shortCode}").getDelete()
                    : null;

            addJwtSecurity(deleteUrl);

            // GET /api/analytics/{shortCode} requires JWT
            Operation analytics = openApi.getPaths()
                    .get("/api/analytics/{shortCode}") != null
                    ? openApi.getPaths()
                        .get("/api/analytics/{shortCode}").getGet()
                    : null;

            addJwtSecurity(analytics);
        };
    }

    private void addJwtSecurity(Operation operation) {

        if (operation != null) {
            operation.addSecurityItem(
                    new io.swagger.v3.oas.models.security.SecurityRequirement()
                            .addList(SECURITY_SCHEME_NAME)
            );
        }
    }
}
