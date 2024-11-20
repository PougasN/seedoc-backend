package com.middle.api.service;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import com.middle.api.exception.PatientNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class CareTeamService {

    private final IGenericClient fhirClient;

    @Autowired
    public CareTeamService(IGenericClient fhirClient) {
        this.fhirClient = fhirClient;
    }

    public boolean deleteCareTeamById(String id) {
        try {
            fhirClient
                    .delete()
                    .resourceById("CareTeam", id)
                    .execute();
            return true;
        } catch (PatientNotFoundException e) {
            return false; // Resource with the specified ID not found
        }
    }
}
