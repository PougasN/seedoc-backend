package com.middle.api.service;

import org.hl7.fhir.r4.model.Encounter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;

@Service
public class EncounterService {

    @Autowired
    private FhirContext fhirContext;

    @Value("${hapi.fhir.server.url}")
    private String hapiFhirUrl;

    public Encounter updateEncounterStatus(String encounterId, String status) {
        IGenericClient client = fhirContext.newRestfulGenericClient(hapiFhirUrl);
        Encounter encounter = client.read().resource(Encounter.class).withId(encounterId).execute();
        encounter.setStatus(Encounter.EncounterStatus.fromCode(status));
        client.update().resource(encounter).execute();
        return encounter;
    }
}
