package com.bazar.bazar.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;

@Configuration
public class CorsConfig {

    @Value("${FRONTEND_URL:}")
    private String frontendUrl;

    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                List<String> origins = new ArrayList<>(Arrays.asList(
                    "http://localhost:3000",   // React dev (CRA / Vite preview)
                    "http://localhost:5173",   // React dev (Vite)
                    "http://localhost:4200"    // Angular (compatibilidade)
                ));

                if (frontendUrl != null && !frontendUrl.isBlank()) {
                    origins.add(frontendUrl);
                }

                registry.addMapping("/api/**")
                        .allowedOrigins(origins.toArray(new String[0]))
                        .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                        .allowedHeaders("*")
                        .allowCredentials(true)
                        .maxAge(3600);
            }
        };
    }
}
