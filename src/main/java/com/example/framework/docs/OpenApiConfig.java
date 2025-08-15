package com.example.framework.docs;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.*;
import io.swagger.v3.oas.models.security.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI openAPI() {
        final String schemeName = "BearerAuth";
        return new OpenAPI()
                .info(new Info()
                        .title("Framework API")
                        .version("v1")
                        .description("Spring Boot 3.3 + OAuth2 + JWT 샘플 API 문서"))
                .addSecurityItem(new SecurityRequirement().addList(schemeName))
                .components(new io.swagger.v3.oas.models.Components()
                        .addSecuritySchemes(schemeName, new SecurityScheme()
                                .name(schemeName)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")));
    }
}
