package com.example.payheurebackend.exception;

/** Période de calcul incohérente (ex. date de fin antérieure à la date de début). */
public class InvalidPeriodException extends RuntimeException {

    public InvalidPeriodException(String message) {
        super(message);
    }
}
