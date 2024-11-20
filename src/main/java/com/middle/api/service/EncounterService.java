package com.middle.api.service;

import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Encounter;
import org.hl7.fhir.r4.model.Reference;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;

@Service
public class EncounterService {

    @Autowired
    private FhirContext fhirContext;

    @Autowired
    private IGenericClient fhirClient;

    @Value("${hapi.fhir.server.url}")
    private String hapiFhirUrl;

    public Encounter updateEncounterStatus(String encounterId, String status) {
        IGenericClient client = fhirContext.newRestfulGenericClient(hapiFhirUrl);
        Encounter encounter = client.read().resource(Encounter.class).withId(encounterId).execute();
        encounter.setStatus(Encounter.EncounterStatus.fromCode(status));
        client.update().resource(encounter).execute();
        return encounter;
    }

    public boolean addParticipantToEncounter(String encounterId, String practitionerId, String role) {
        try {
            // Retrieve the encounter
            Encounter encounter = fhirClient.read()
                    .resource(Encounter.class)
                    .withId(encounterId)
                    .execute();

            // Create a new participant component
            Encounter.EncounterParticipantComponent participant = new Encounter.EncounterParticipantComponent();
            participant.setIndividual(new Reference("Practitioner/" + practitionerId));

            // Set the role of the participant (e.g., doctor or nurse)
            CodeableConcept roleConcept = new CodeableConcept();
            roleConcept.setText(role.equalsIgnoreCase("doctor") ? "Doctor" : "PreReader");
            participant.addType(roleConcept);

            // Add the participant to the encounter
            encounter.addParticipant(participant);

            // Update the encounter in FHIR
            fhirClient.update()
                    .resource(encounter)
                    .withId(encounterId)
                    .execute();

            return true;
        } catch (Exception e) {
            System.err.println("Error adding participant to encounter: " + e.getMessage());
            return false;
        }
    }
}
