package com.middle.api.ExceptionHandling;

public class PatientNotFoundException extends RuntimeException {
    public PatientNotFoundException(String id) {
        super("Patient not found with ID: " + id);
    }
}
