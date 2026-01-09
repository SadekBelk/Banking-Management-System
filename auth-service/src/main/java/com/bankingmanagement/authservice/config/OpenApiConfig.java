package com.bankingmanagement.authservice.config;

/*
╔══════════════════════════════════════════════════════════════════════════════════════════╗
║                            OPENAPI CONFIGURATION                                          ║
║                                                                                           ║
║  BUILD ORDER: STEP 9c of 12 (Configuration - API Documentation)                          ║
║  PREVIOUS STEP: DataInitializer (database is ready, now document the API)                ║
║  NEXT STEP: AuthController (the endpoints we're documenting)                             ║
║                                                                                           ║
║  WHAT IS OPENAPI / SWAGGER?                                                              ║
╠══════════════════════════════════════════════════════════════════════════════════════════╣
║                                                                                           ║
║  OpenAPI Specification (OAS):                                                            ║
║  ┌─────────────────────────────────────────────────────────────────────────────────────┐ ║
║  │                                                                                     │ ║
║  │  - Industry standard for describing REST APIs                                       │ ║
║  │  - Machine-readable format (JSON/YAML)                                              │ ║
║  │  - Swagger = UI tool that renders OpenAPI specs                                     │ ║
║  │                                                                                     │ ║
║  │  Benefits:                                                                          │ ║
║  │  • Interactive documentation at /swagger-ui.html                                    │ ║
║  │  • Try out API endpoints directly in browser                                        │ ║
║  │  • Generate client SDKs in any language                                             │ ║
║  │  • Contract-first development                                                       │ ║
║  │                                                                                     │ ║
║  └─────────────────────────────────────────────────────────────────────────────────────┘ ║
║                                                                                           ║
║  SPRINGDOC-OPENAPI:                                                                      ║
║  ┌─────────────────────────────────────────────────────────────────────────────────────┐ ║
║  │                                                                                     │ ║
║  │  Library: springdoc-openapi-starter-webmvc-ui                                       │ ║
║  │                                                                                     │ ║
║  │  Auto-generates OpenAPI spec from:                                                  │ ║
║  │  • @RestController classes                                                          │ ║
║  │  • @RequestMapping, @GetMapping, @PostMapping                                       │ ║
║  │  • @RequestBody, @PathVariable, @RequestParam                                       │ ║
║  │  • Jakarta validation annotations                                                   │ ║
║  │                                                                                     │ ║
║  │  URLs after startup:                                                                │ ║
║  │  • /swagger-ui.html     - Interactive UI                                            │ ║
║  │  • /v3/api-docs        - Raw OpenAPI JSON                                           │ ║
║  │  • /v3/api-docs.yaml   - Raw OpenAPI YAML                                           │ ║
║  │                                                                                     │ ║
║  └─────────────────────────────────────────────────────────────────────────────────────┘ ║
║                                                                                           ║
║  JWT SECURITY IN SWAGGER:                                                                ║
║  ┌─────────────────────────────────────────────────────────────────────────────────────┐ ║
║  │                                                                                     │ ║
║  │  This config adds "Authorize" button to Swagger UI:                                 │ ║
║  │                                                                                     │ ║
║  │  ┌──────────────────────────────────────────────────────────────────────────────┐  │ ║
║  │  │ [Authorize 🔓]                                                               │  │ ║
║  │  │                                                                              │  │ ║
║  │  │ bearerAuth (http, Bearer)                                                    │  │ ║
║  │  │ Enter JWT token                                                              │  │ ║
║  │  │ ┌────────────────────────────────────────────────────────────────────────┐  │  │ ║
║  │  │ │ eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxMjM0...                               │  │  │ ║
║  │  │ └────────────────────────────────────────────────────────────────────────┘  │  │ ║
║  │  │                                                                              │  │ ║
║  │  │ [Authorize]  [Close]                                                         │  │ ║
║  │  └──────────────────────────────────────────────────────────────────────────────┘  │ ║
║  │                                                                                     │ ║
║  │  After authorizing, all requests include: Authorization: Bearer <token>             │ ║
║  │                                                                                     │ ║
║  └─────────────────────────────────────────────────────────────────────────────────────┘ ║
╚══════════════════════════════════════════════════════════════════════════════════════════╝
*/

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI/Swagger configuration for API documentation.
 * 
 * Customizes the auto-generated OpenAPI specification:
 * - API info (title, description, version)
 * - Server URLs (localhost, Docker)
 * - JWT security scheme
 * 
 * Access Swagger UI at: http://localhost:4005/swagger-ui.html
 */
@Configuration
public class OpenApiConfig {

    /*
     * Server port from application.yml (default: 4005)
     * Used to generate correct server URL in documentation
     */
    @Value("${server.port:4005}")
    private String serverPort;

    /**
     * Customize OpenAPI specification.
     * 
     * @Bean makes this available to springdoc-openapi
     * @return Customized OpenAPI object
     */
    @Bean
    public OpenAPI customOpenAPI() {
        // Security scheme name - referenced by @SecurityRequirement in controllers
        final String securitySchemeName = "bearerAuth";

        return new OpenAPI()
                /*
                 * API Information - appears at top of Swagger UI
                 */
                .info(new Info()
                        .title("Auth Service API")
                        .description("JWT Authentication Service for Digital Banking Management System")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Banking Management Team")
                                .email("support@bankingmanagement.com"))
                        .license(new License()
                                .name("MIT License")
                                .url("https://opensource.org/licenses/MIT")))
                
                /*
                 * Server URLs - dropdown in Swagger UI to select target server
                 * Useful for testing against different environments
                 */
                .servers(List.of(
                        new Server()
                                .url("http://localhost:" + serverPort)
                                .description("Local Development Server"),
                        new Server()
                                .url("http://auth-service:4005")
                                .description("Docker Server")
                ))
                
                /*
                 * Default Security Requirement
                 * 
                 * This adds "bearerAuth" security requirement to ALL endpoints.
                 * Individual endpoints can override with @SecurityRequirement annotation.
                 * Public endpoints (login, register) don't need the Authorize header.
                 */
                .addSecurityItem(new SecurityRequirement().addList(securitySchemeName))
                
                /*
                 * Security Scheme Definition
                 * 
                 * Defines HOW authentication works:
                 * - Type: HTTP (vs API key, OAuth2)
                 * - Scheme: bearer (Authorization: Bearer <token>)
                 * - Format: JWT (helps clients understand the token format)
                 */
                .components(new Components()
                        .addSecuritySchemes(securitySchemeName,
                                new SecurityScheme()
                                        .name(securitySchemeName)
                                        .type(SecurityScheme.Type.HTTP)  // HTTP authentication
                                        .scheme("bearer")                // Bearer token scheme
                                        .bearerFormat("JWT")             // JWT format
                                        .description("Enter JWT token")));
    }
}
