package com.example.payheurebackend.exception;

/** Ressource demandée inexistante : traduite en 404 par le gestionnaire d'erreurs. */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
