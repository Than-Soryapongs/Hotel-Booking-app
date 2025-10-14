package com.system.hotel_room_booking.config;

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

@Configuration
public class OpenApiConfig {
    
    @Value("${app.api.url}")
    private String apiUrl;
    
    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("KHQR Authentication API")
                .version("1.0")
                .description("Comprehensive Authentication & Authorization System with Best Security Practices")
                .contact(new Contact()
                    .name("API Support")
                    .email("support@khqr.com"))
                .license(new License()
                    .name("Apache 2.0")
                    .url("https://www.apache.org/licenses/LICENSE-2.0.html")))
            .servers(List.of(
                new Server().url(apiUrl).description("Development Server")
            ))
            .addSecurityItem(new SecurityRequirement()
                .addList("Bearer Authentication")
                .addList("API Key Authentication"))
            .components(new Components()
                .addSecuritySchemes("Bearer Authentication", new SecurityScheme()
                    .type(SecurityScheme.Type.HTTP)
                    .scheme("bearer")
                    .bearerFormat("JWT")
                    .description("Enter JWT token"))
                .addSecuritySchemes("API Key Authentication", new SecurityScheme()
                    .type(SecurityScheme.Type.APIKEY)
                    .in(SecurityScheme.In.HEADER)
                    .name("X-API-Key")
                    .description("Enter API Key")));
    }
}

