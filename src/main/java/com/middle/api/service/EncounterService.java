package com.middle.api.service;

import org.hl7.fhir.r4.model.Encounter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;

@Service
public class EncounterService {

    @Autowired
    private FhirContext fhirContext;

    public Encounter updateEncounterStatus(String encounterId, String status) {
        IGenericClient client = fhirContext.newRestfulGenericClient("http://localhost:8080/fhir");
        Encounter encounter = client.read().resource(Encounter.class).withId(encounterId).execute();
        encounter.setStatus(Encounter.EncounterStatus.fromCode(status));
        client.update().resource(encounter).execute();
        return encounter;
    }
}
