package com.example.payheurebackend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Autorise n'importe quelle origine (tous domaines et tous ports) à appeler l'API. Contrairement
 * au projet catalog, il n'y a pas de session admin ici (aucune authentification), donc pas de
 * {@code SecurityConfig} Spring Security : cette configuration MVC classique suffit.
 */
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "OPTIONS")
                .allowedHeaders("*");
    }
}
