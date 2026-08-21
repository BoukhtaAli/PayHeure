package com.example.payheurebackend.config;

import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.config.annotation.CorsRegistry;

import static org.assertj.core.api.Assertions.assertThatCode;

class CorsConfigTest {

    @Test
    void addCorsMappings_nefEchouePasEtEnregistreLeMapping() {
        CorsConfig corsConfig = new CorsConfig();

        // Pas d'assertion fine possible sur le contenu de CorsRegistry (pas d'accesseur public) :
        // ce test vérifie que le mapping s'enregistre sans lever d'exception, ce qui exerce
        // réellement addMapping().allowedOrigins().allowedMethods().allowedHeaders().
        assertThatCode(() -> corsConfig.addCorsMappings(new CorsRegistry())).doesNotThrowAnyException();
    }
}
