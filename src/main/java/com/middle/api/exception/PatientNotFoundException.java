package com.middle.api.exception;

public class PatientNotFoundException extends RuntimeException {
    public PatientNotFoundException(String id) {
        super("Patient not found with ID: " + id);
    }
}
