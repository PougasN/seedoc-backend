package com.middle.api.dto;

public class ParticipantRequest {
    private String practitionerId;
    private String role; // Either "doctor" or "nurse"

    public String getPractitionerId() {
        return practitionerId;
    }

    public void setPractitionerId(String practitionerId) {
        this.practitionerId = practitionerId;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    // Getters and Setters
}

