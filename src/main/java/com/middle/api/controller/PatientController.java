package com.middle.api.controller;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import com.middle.api.service.FhirClientService;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Patient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class PatientController {

    @Autowired
    private FhirClientService fhirClientService;

    @Autowired
    private IGenericClient fhirClient;

    @Autowired
    private FhirContext fhirContext;

    // GET a Patient by its Family Name
    @GetMapping("/patients/by-family-name")
    public ResponseEntity<String> findPatientsByFamilyName(@RequestParam String family) {
        List<Patient> patients = fhirClientService.findPatientsByFamilyName(family);
        if (patients.isEmpty()) {
            return ResponseEntity.ok("No patients found with family name: " + family);
        }
        // Convert the list of Patients into a JSON string
        String patientsJson = fhirContext.newJsonParser().setPrettyPrint(true).encodeResourceToString(new Bundle());
        for (Patient patient : patients) {
            patientsJson = patientsJson.concat(fhirContext.newJsonParser().encodeResourceToString(patient) + "\n");
        }
        return ResponseEntity.ok(patientsJson);
    }

    // Get a Patient with ID
    @GetMapping("/patient/{id}")
    public ResponseEntity<String> getPatientById(@PathVariable String id) {
        Patient patient = fhirClientService.getPatientById(id);
        String patientString = fhirContext.newJsonParser().encodeResourceToString(patient);
        return ResponseEntity.ok(patientString);
    }

    // POST a new Patient
    @PostMapping(value = "/patient", consumes = "application/fhir+json")
    public ResponseEntity<String> createPatient(@RequestBody Patient patient) {
        MethodOutcome outcome = fhirClient.create().resource(patient).execute();
        IdType id = (IdType) outcome.getId();
        Patient createdPatient = fhirClient.read().resource(Patient.class).withId(id.getIdPart()).execute();
        String patientString = fhirContext.newJsonParser().setPrettyPrint(true).encodeResourceToString(createdPatient);
        return ResponseEntity.status(HttpStatus.CREATED).body(patientString);
    }

    // Get ALL Patients
    @GetMapping("/patients")
    public ResponseEntity<String> getAllPatients() {
        Bundle bundle = fhirClientService.getAllPatients();
        String bundleJson = fhirContext.newJsonParser().setPrettyPrint(true).encodeResourceToString(bundle);
        return ResponseEntity.ok(bundleJson);
    }

    // DELETE Patient by ID
    @DeleteMapping("/patient/{id}")
    public ResponseEntity<String> deletePatient(@PathVariable String id) {
        try {
            fhirClient.delete().resourceById(new IdType("Patient", id)).execute();
            return ResponseEntity.status(HttpStatus.NO_CONTENT).body("Patient resource deleted successfully");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error deleting patient resource: " + e.getMessage());
        }
    }

    // DELETE All Patients
    @DeleteMapping("/patients")
    public ResponseEntity<String> deleteAllPatients() {
        Bundle bundle = fhirClient.search().forResource(Patient.class).returnBundle(Bundle.class).execute();
        for (Bundle.BundleEntryComponent entry : bundle.getEntry()) {
            Patient patient = (Patient) entry.getResource();
            fhirClient.delete().resourceById(new IdType("Patient", patient.getIdElement().getIdPart())).execute();
        }
        return ResponseEntity.status(HttpStatus.NO_CONTENT).body("All patients deleted successfully");
    }
}
