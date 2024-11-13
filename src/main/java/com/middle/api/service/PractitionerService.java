package com.middle.api.service;

import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.gclient.ReferenceClientParam;
import com.middle.api.exception.PatientNotFoundException;
import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.CareTeam;
import org.hl7.fhir.r4.model.Encounter;
import org.hl7.fhir.r4.model.Practitioner;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class PractitionerService {

    @Autowired
    private IGenericClient fhirClient;

    public Practitioner createPractitioner(Practitioner practitioner) {
        MethodOutcome outcome = fhirClient.create()
                .resource(practitioner)
                .execute();
        IIdType practitionerId = outcome.getId();
        practitioner.setId(practitionerId);
        return practitioner;
    }

    public Practitioner getPractitionerById(String practitionerId) {
        Practitioner practitioner = fhirClient.read()
                .resource(Practitioner.class)
                .withId(practitionerId)
                .execute();
        if (practitioner == null) {
            throw new PatientNotFoundException("Practitioner not found with ID: " + practitionerId);
        }
        return practitioner;
    }

    public List<Encounter> getEncountersForPractitioner(String practitionerId) {
        // Retrieve all CareTeams where the Practitioner is a member
        Bundle careTeamBundle = fhirClient.search().forResource(CareTeam.class)
                .where(new ReferenceClientParam("participant.member").hasId("Practitioner/" + practitionerId))
                .returnBundle(Bundle.class)
                .execute();

        List<Encounter> encounters = new ArrayList<>();

        // For each CareTeam, retrieve the associated Encounters
        for (Bundle.BundleEntryComponent entry : careTeamBundle.getEntry()) {
            CareTeam careTeam = (CareTeam) entry.getResource();

            // Search for Encounters associated with this CareTeam
            Bundle encounterBundle = fhirClient.search().forResource(Encounter.class)
                    .where(new ReferenceClientParam("careTeam").hasId(careTeam.getIdElement().getIdPart()))
                    .returnBundle(Bundle.class)
                    .execute();

            for (Bundle.BundleEntryComponent encEntry : encounterBundle.getEntry()) {
                encounters.add((Encounter) encEntry.getResource());
            }
        }

        return encounters;
    }
}
