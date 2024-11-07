package com.middle.api.controller;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.exceptions.FhirClientConnectionException;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import com.middle.api.service.EncounterService;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Encounter;
import org.hl7.fhir.r4.model.IdType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
public class EncounterController {

    @Autowired
    private IGenericClient fhirClient;

    @Autowired
    private FhirContext fhirContext;

    @Autowired
    private EncounterService encounterService;


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
        fhirClient.update().resource(encounter).execute();
        Encounter updatedEncounter = fhirClient.read().resource(Encounter.class).withId(id).execute();
        String encounterString = fhirContext.newJsonParser().setPrettyPrint(true).encodeResourceToString(updatedEncounter);
        return ResponseEntity.ok(encounterString);
    }

    // GET an Encounter by ID
    @GetMapping(value = "/encounter/{id}", produces = "application/fhir+json")
    public ResponseEntity<String> getEncounterById(@PathVariable String id) {
        Encounter encounter = fhirClient.read().resource(Encounter.class).withId(id).execute();
        String encounterString = fhirContext.newJsonParser().setPrettyPrint(true).encodeResourceToString(encounter);
        return ResponseEntity.ok(encounterString);
    }

    // DELETE an Encounter by ID
    @DeleteMapping("/encounter/{id}")
    public ResponseEntity<String> deleteEncounter(@PathVariable String id) {
        try {
            fhirClient.delete().resourceById(new IdType("Encounter", id)).execute();
            return ResponseEntity.status(HttpStatus.NO_CONTENT).body("Encounter resource deleted successfully");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error deleting Encounter resource: " + e.getMessage());
        }
    }

    // DELETE All Encounters
    @DeleteMapping("/encounters")
    public ResponseEntity<String> deleteAllEncounters() {
        Bundle bundle = fhirClient.search().forResource(Encounter.class).returnBundle(Bundle.class).execute();
        bundle.getEntry().forEach(entry -> {
            Encounter encounter = (Encounter) entry.getResource();
            String encounterId = encounter.getIdElement().getIdPart();
            fhirClient.delete().resourceById(new IdType("Encounter", encounterId)).execute();
        });
        return ResponseEntity.status(HttpStatus.NO_CONTENT).body("All encounters deleted successfully");
    }


    // GET all encounters for a patient
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
                .toList();

        String encountersString = fhirContext.newJsonParser().setPrettyPrint(true).encodeResourceToString(bundle);
        return ResponseEntity.ok(encountersString);
    }

//    // PUT update an Encounter status
//    @PutMapping("/encounter/{encounterId}/status")
//    public Encounter updateEncounterStatus(
//            @PathVariable String encounterId,
//            @RequestBody Map<String, String> requestBody) {
//        String status = requestBody.get("status");
//        return encounterService.updateEncounterStatus(encounterId, status);
//    }

    @PutMapping("/encounter/{encounterId}/status")
    public ResponseEntity<?> updateEncounterStatus(
            @PathVariable String encounterId,
            @RequestBody Map<String, String> requestBody) {
        try {
            // Log incoming data
            System.out.println("Updating encounter with ID: " + encounterId);
            System.out.println("Requested status: " + requestBody.get("status"));

            // Check if status is provided in the request body
            String status = requestBody.get("status");
            if (status == null) {
                return ResponseEntity.badRequest().body("Status is required");
            }

            // Attempt to update the encounter status
            Encounter updatedEncounter = encounterService.updateEncounterStatus(encounterId, status);
            System.out.println("Encounter updated successfully: " + updatedEncounter);
            return ResponseEntity.ok(updatedEncounter);

        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Encounter not found with ID: " + encounterId);
        } catch (FhirClientConnectionException e) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body("FHIR server is unavailable: " + e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An unexpected error occurred: " + e.getMessage());
        }
    }






}
