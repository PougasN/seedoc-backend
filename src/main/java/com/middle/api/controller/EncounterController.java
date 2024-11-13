package com.middle.api.controller;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.exceptions.FhirClientConnectionException;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import com.middle.api.dto.ParticipantRequest;
import com.middle.api.service.EncounterService;
import org.hl7.fhir.r4.model.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
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

    @PutMapping("/{encounterId}/nursePreRead")
    public ResponseEntity<String> addNursePreReadStatus(@PathVariable String encounterId) {
        try {
            // Fetch the Encounter resource from the FHIR server
            Encounter encounter = fhirClient.read().resource(Encounter.class).withId(encounterId).execute();

            // Add or update the custom extension for nurse pre-reading
            Extension nursePreReadExtension = new Extension("http://example.com/fhir/StructureDefinition/nursePreReadStatus", new BooleanType(true));
            encounter.addExtension(nursePreReadExtension);

            // Update the Encounter resource on the FHIR server
            fhirClient.update().resource(encounter).execute();

            return ResponseEntity.ok("Nurse pre-read status added to Encounter.");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Failed to add nurse pre-read status: " + e.getMessage());
        }
    }

    // POST new Encounter
    @PostMapping(value = "/encounter", consumes = "application/fhir+json")
    public ResponseEntity<String> createEncounter(@RequestBody Encounter encounter) {
        MethodOutcome outcome = fhirClient.create().resource(encounter).execute();
        IdType id = (IdType) outcome.getId();
        Encounter createdEncounter = fhirClient.read().resource(Encounter.class).withId(id.getIdPart()).execute();
        String encounterString = fhirContext.newJsonParser().setPrettyPrint(true).encodeResourceToString(createdEncounter);
        return ResponseEntity.status(HttpStatus.CREATED).body(encounterString);
    }


    @PostMapping("/encounter/{encounterId}/addParticipant")
    public ResponseEntity<?> addParticipantToEncounter(
            @PathVariable String encounterId,
            @RequestBody ParticipantRequest request) {

        boolean success = encounterService.addParticipantToEncounter(encounterId, request.getPractitionerId(), request.getRole());

        if (success) {
            return ResponseEntity.ok("Participant added successfully");
        } else {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to add participant");
        }
    }


    @PostMapping(value = "/encounterWithReport", consumes = "application/fhir+json")
    public ResponseEntity<String> createEncounterWithDiagnosticReport(@RequestBody Encounter encounter) {
        try {
            // Create the Encounter resource
            MethodOutcome encounterOutcome = fhirClient.create().resource(encounter).execute();
            IdType encounterId = (IdType) encounterOutcome.getId();
            Encounter createdEncounter = fhirClient.read().resource(Encounter.class).withId(encounterId.getIdPart()).execute();

            // Create the DiagnosticReport resource linked to the created encounter
            DiagnosticReport diagnosticReport = new DiagnosticReport();
            diagnosticReport.setStatus(DiagnosticReport.DiagnosticReportStatus.REGISTERED);
            diagnosticReport.setEncounter(new Reference("Encounter/" + encounterId.getIdPart()));

            MethodOutcome reportOutcome = fhirClient.create().resource(diagnosticReport).execute();
            DiagnosticReport createdReport = fhirClient.read().resource(DiagnosticReport.class).withId(reportOutcome.getId().getIdPart()).execute();

            // Convert each resource to JSON separately
            String encounterJson = fhirContext.newJsonParser().setPrettyPrint(true).encodeResourceToString(createdEncounter);
            String diagnosticReportJson = fhirContext.newJsonParser().setPrettyPrint(true).encodeResourceToString(createdReport);

            // Manually combine the JSON strings into a single JSON object
            String combinedJson = String.format("{\"encounter\": %s, \"diagnosticReport\": %s}", encounterJson, diagnosticReportJson);

            return ResponseEntity.status(HttpStatus.CREATED).body(combinedJson);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error occurred: " + e.getMessage());
        }
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

    @PutMapping("/encounter/{encounterId}/finalize")
    public ResponseEntity<String> finalizeEncounter(@PathVariable String encounterId, @RequestBody DiagnosticReport diagnosticReport) {
        try {
            // Update Encounter Status to "completed"
            Encounter encounter = fhirClient.read().resource(Encounter.class).withId(encounterId).execute();
            encounter.setStatus(Encounter.EncounterStatus.FINISHED);
            fhirClient.update().resource(encounter).execute();

            // Create Diagnostic Report
            MethodOutcome outcome = fhirClient.create().resource(diagnosticReport).execute();

            return ResponseEntity.ok("Encounter finalized and diagnostic report created.");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error finalizing encounter");
        }
    }


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

    @GetMapping("/encounter/practitioner")
    public ResponseEntity<List<Encounter>> getEncountersForPractitioner(@RequestParam String practitionerId) {
        List<CareTeam> careTeams = fhirClient.search()
                .forResource(CareTeam.class)
                .where(CareTeam.PARTICIPANT.hasId("Practitioner/" + practitionerId))
                .returnBundle(Bundle.class)
                .execute()
                .getEntry().stream()
                .map(Bundle.BundleEntryComponent::getResource)
                .map(CareTeam.class::cast)
                .toList();

        // Collect all encounters related to the care teams
        List<Encounter> encounters = new ArrayList<>();
        for (CareTeam careTeam : careTeams) {
            Reference patientRef = careTeam.getSubject();
            if (patientRef != null) {
                List<Encounter> patientEncounters = fhirClient.search()
                        .forResource(Encounter.class)
                        .where(Encounter.PATIENT.hasId(patientRef.getReference()))
                        .returnBundle(Bundle.class)
                        .execute()
                        .getEntry().stream()
                        .map(Bundle.BundleEntryComponent::getResource)
                        .map(Encounter.class::cast)
                        .toList();
                encounters.addAll(patientEncounters);
            }
        }
        return ResponseEntity.ok(encounters);
    }

    @GetMapping("encounters/byPractitioner/{practitionerId}")
    public List<Map<String, Object>> getEncountersByPractitioner(@PathVariable String practitionerId) {
        List<Map<String, Object>> simplifiedEncounters = new ArrayList<>();

        Bundle bundle = fhirClient
                .search()
                .forResource(Encounter.class)
                .where(Encounter.PARTICIPANT.hasId("Practitioner/" + practitionerId))
                .returnBundle(Bundle.class)
                .execute();

        for (Bundle.BundleEntryComponent entry : bundle.getEntry()) {
            if (entry.getResource() instanceof Encounter) {
                Encounter encounter = (Encounter) entry.getResource();
                simplifiedEncounters.add(Map.of(
                        "id", encounter.getIdElement().getIdPart(),
                        "description", encounter.hasReasonCode() && !encounter.getReasonCode().isEmpty() ? encounter.getReasonCodeFirstRep().getText() : "No Description",
                        "status", encounter.hasStatus() ? encounter.getStatus().toCode() : "Unknown",
                        "date", encounter.hasPeriod() && encounter.getPeriod().hasStart() ? encounter.getPeriod().getStart().toString() : "No Date"
                ));
            }
        }

        return simplifiedEncounters;
    }

    // GET all encounters for a patient
    @GetMapping(value = "/practitioner/{practitionerId}/encounters", produces = "application/fhir+json")
    public ResponseEntity<String> getAllEncountersForPractitioner(@PathVariable String practitionerId) {
        Bundle bundle = fhirClient.search().forResource(Encounter.class)
                .where(Encounter.PARTICIPANT.hasId(practitionerId))
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

}
