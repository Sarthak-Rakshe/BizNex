package com.sarthak.BizNex.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springdoc.core.models.GroupedOpenApi;

/**
 * Central OpenAPI / Swagger configuration: API metadata and JWT bearer auth scheme.
 */
@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "BizNex API",
                version = "v1.4.0",
                description = "REST API for BizNex (products, customers, billing, auth)\n\nChangelog:\n- v1.4.0: " +
                        "The get all credits now return total credits and average credits \n- v1.3.0: Added customer " +
                        "search " +
                        "pagination " +
                        "aggregate" +
                        " ordering & stabilized " +
                        "product PATCH behavior (quantity can be set to zero). Documented paged response envelope.\n- v1.2.0: Added default low-stock-first product ordering (quantity < 10 first), default alphabetical customer sorting, automatic creditsPayment bill on customer credit decrease, documented list endpoint response ordering.\n- v1.1.0: DELETE /users now 204 (idempotent) + 409 guard; detailed Auth docs.\n- v1: Initial version.",
                contact = @Contact(name = "BizNex Support", email = "abc@gmail.com"),
                license = @License(name = "Apache 2.0", url = "https://www.apache.org/licenses/LICENSE-2.0")
        ),
        servers = {
                @Server(url = "http://localhost:8081", description = "Local Dev")
        },
        security = {@SecurityRequirement(name = "bearer-jwt")}
)
@SecurityScheme(
        name = "bearer-jwt",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT",
        in = SecuritySchemeIn.HEADER
)
public class OpenApiConfig {
    // Additional OpenAPI custom beans (like OpenApiCustomiser) can be added here later.

    @Bean
    public GroupedOpenApi publicApiV1() {
        return GroupedOpenApi.builder()
                .group("v1-public")
                .pathsToMatch("/api/v1/**")
                .build();
    }
}
