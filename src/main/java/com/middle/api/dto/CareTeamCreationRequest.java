package com.middle.api.dto;

import java.util.List;
import java.util.Map;

public class CareTeamCreationRequest {
    private String patientId;
    private List<String> practitionerIds;
    private Map<String, String> roles;

    public CareTeamCreationRequest(String patientId, List<String> practitionerIds, Map<String, String> roles) {
        this.patientId = patientId;
        this.practitionerIds = practitionerIds;
        this.roles = roles;
    }

    public String getRoleForPractitioner(String practitionerId) {
        return roles.getOrDefault(practitionerId, "general"); // Default to "general" if no specific role
    }

    public String getPatientId() {
        return patientId;
    }

    public void setPatientId(String patientId) {
        this.patientId = patientId;
    }

    public List<String> getPractitionerIds() {
        return practitionerIds;
    }

    public void setPractitionerIds(List<String> practitionerIds) {
        this.practitionerIds = practitionerIds;
    }

    public Map<String, String> getRoles() {
        return roles;
    }

    public void setRoles(Map<String, String> roles) {
        this.roles = roles;
    }
}
