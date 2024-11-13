package com.middle.api.service;

import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import com.middle.api.exception.PatientNotFoundException;
import org.hl7.fhir.r4.model.Encounter;
import org.hl7.fhir.r4.model.Reference;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class CareTeamService {

    private final IGenericClient fhirClient;

    @Autowired
    public CareTeamService(IGenericClient fhirClient) {
        this.fhirClient = fhirClient;
    }

//    public void assignEncounterToCareTeam(String careTeamId, String encounterId) {
//        // Retrieve the Encounter by ID
//        Encounter encounter = fhirClient.read().resource(Encounter.class).withId(encounterId).execute();
//
//        // Set the CareTeam reference in the Encounter as a participant
//        encounter.addParticipant()
//                .setIndividual(new Reference("CareTeam/" + careTeamId));
//
//        // Update the Encounter with the CareTeam reference
//        fhirClient.update().resource(encounter).execute();
//    }

//    public void assignCareTeamToEncounter(String encounterId, String careTeamId) {
//        // Retrieve the Encounter by its ID
//        Encounter encounter = fhirClient.read().resource(Encounter.class).withId(encounterId).execute();
//
//        // Set the careTeam reference
//        Reference careTeamReference = new Reference("CareTeam/" + careTeamId);
//        encounter.getCareTeam().add(careTeamReference); // This adds the careTeam reference
//        encounter.get
//
//        // Update the Encounter resource
//        MethodOutcome outcome = fhirClient.update().resource(encounter).execute();
//        System.out.println("Encounter updated with careTeam reference. Outcome: " + outcome.getId());
//    }

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
