package com.middle.api.controller;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import com.middle.api.service.FhirClientService;
import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.DiagnosticReport;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Patient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
public class FhirController {

    @Autowired
    private FhirClientService fhirClientService;
    private final FhirContext fhirContext;
    private final IGenericClient fhirClient;

    @Autowired
    public FhirController(FhirContext fhirContext, IGenericClient fhirClient) {
        this.fhirContext = fhirContext;
        this.fhirClient = fhirClient;
    }

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

    @PostMapping(value = "/diagnostic-report", consumes = "application/fhir+json")
    public ResponseEntity<String> createDiagnosticReport(@RequestBody DiagnosticReport diagnosticReport) {
        // Send the DiagnosticReport to the FHIR server and capture the response
        MethodOutcome outcome = fhirClient.create().resource(diagnosticReport).execute();

        // Extract the ID of the newly created diagnostic report
        IdType id = (IdType) outcome.getId();

        // Fetch the created diagnostic report to include it in the response body
        DiagnosticReport createdReport = fhirClient.read().resource(DiagnosticReport.class).withId(id.getIdPart()).execute();
        String reportString = fhirContext.newJsonParser().setPrettyPrint(true).encodeResourceToString(createdReport);

        return ResponseEntity.status(HttpStatus.CREATED).body(reportString);
    }

    @GetMapping("/diagnostic-report/{id}")
    public ResponseEntity<String> getDiagnosticReportById(@PathVariable String id) {
        // Retrieve the DiagnosticReport from the FHIR server
        DiagnosticReport diagnosticReport = fhirClient.read().resource(DiagnosticReport.class).withId(id).execute();

        // Convert the DiagnosticReport to JSON string
        String diagnosticReportJson = fhirContext.newJsonParser().setPrettyPrint(true).encodeResourceToString(diagnosticReport);

        return ResponseEntity.ok(diagnosticReportJson);
    }

    @GetMapping("/patient/{id}")
    public ResponseEntity<String> getPatientById(@PathVariable String id) {
        Patient patient = fhirClientService.getPatientById(id);
        String patientString = fhirContext.newJsonParser().encodeResourceToString(patient);
        return ResponseEntity.ok(patientString);
    }

    @PostMapping(value = "/patient", consumes = "application/fhir+json")
    public ResponseEntity<String> createPatient(@RequestBody Patient patient) {
        // Send the Patient to the FHIR server and capture the response
        MethodOutcome outcome = fhirClient.create().resource(patient).execute();

        // Extract the ID of the newly created patient
        IdType id = (IdType) outcome.getId();

        // Fetch the created patient to include it in the response body
        Patient createdPatient = fhirClient.read().resource(Patient.class).withId(id.getIdPart()).execute();
        String patientString = fhirContext.newJsonParser().setPrettyPrint(true).encodeResourceToString(createdPatient);

        return ResponseEntity.status(HttpStatus.CREATED).body(patientString);
    }

    @GetMapping("/patients")
    public ResponseEntity<String> getAllPatients() {
        Bundle bundle = fhirClientService.getAllPatients();
        String bundleJson = fhirContext.newJsonParser().setPrettyPrint(true).encodeResourceToString(bundle);
        return ResponseEntity.ok(bundleJson);
    }
}
