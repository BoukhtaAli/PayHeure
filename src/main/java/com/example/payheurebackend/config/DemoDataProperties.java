package com.example.payheurebackend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Bascule le jeu de données de démonstration créé par {@link DemoDataInitializer}. */
@ConfigurationProperties(prefix = "payheure.demo")
public record DemoDataProperties(boolean seedEnabled) {
}
