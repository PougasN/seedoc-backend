package com.middle.api.controller;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Encounter;
import org.hl7.fhir.r4.model.IdType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
public class EncounterController {

    @Autowired
    private IGenericClient fhirClient;

    @Autowired
    private FhirContext fhirContext;


    // POST new Encounter
    @PostMapping(value = "/encounter", consumes = "application/fhir+json")
    public ResponseEntity<String> createEncounter(@RequestBody Encounter encounter) {
        MethodOutcome outcome = fhirClient.create().resource(encounter).execute();
        IdType id = (IdType) outcome.getId();
        Encounter createdEncounter = fhirClient.read().resource(Encounter.class).withId(id.getIdPart()).execute();
        String encounterString = fhirContext.newJsonParser().setPrettyPrint(true).encodeResourceToString(createdEncounter);
        return ResponseEntity.status(HttpStatus.CREATED).body(encounterString);
    }

    // PUT update Encounter
    @PutMapping(value = "/encounter/{id}", consumes = "application/fhir+json")
    public ResponseEntity<String> updateEncounter(@PathVariable String id, @RequestBody Encounter encounter) {
        encounter.setId(id);
        MethodOutcome outcome = fhirClient.update().resource(encounter).execute();
        Encounter updatedEncounter = fhirClient.read().resource(Encounter.class).withId(id).execute();
        String encounterString = fhirContext.newJsonParser().setPrettyPrint(true).encodeResourceToString(updatedEncounter);
        return ResponseEntity.ok(encounterString);
    }

    // Get an encounter by ID
    @GetMapping(value = "/encounter/{id}", produces = "application/fhir+json")
    public ResponseEntity<String> getEncounterById(@PathVariable String id) {
        Encounter encounter = fhirClient.read().resource(Encounter.class).withId(id).execute();
        String encounterString = fhirContext.newJsonParser().setPrettyPrint(true).encodeResourceToString(encounter);
        return ResponseEntity.ok(encounterString);
    }

    // Get all encounters for a patient
    @GetMapping(value = "/patient/{patientId}/encounters", produces = "application/fhir+json")
    public ResponseEntity<String> getAllEncountersForPatient(@PathVariable String patientId) {
        Bundle bundle = fhirClient.search().forResource(Encounter.class)
                .where(Encounter.SUBJECT.hasId(patientId))
                .returnBundle(Bundle.class)
                .execute();

        List<Encounter> encounters = bundle.getEntry().stream()
                .map(Bundle.BundleEntryComponent::getResource)
                .filter(resource -> resource instanceof Encounter)
                .map(resource -> (Encounter) resource)
                .collect(Collectors.toList());

        String encountersString = fhirContext.newJsonParser().setPrettyPrint(true).encodeResourceToString(bundle);
        return ResponseEntity.ok(encountersString);
    }
}
