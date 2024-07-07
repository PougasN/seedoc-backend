package com.middle.api.controller;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import com.middle.api.service.DiagnosticReportService;
import org.hl7.fhir.r4.model.DiagnosticReport;
import org.hl7.fhir.r4.model.IdType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
public class DiagnosticReportController {

    @Autowired
    private IGenericClient fhirClient;

    @Autowired
    private FhirContext fhirContext;

    @Autowired
    private DiagnosticReportService diagnosticReportService;

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

    @GetMapping("/diagnostic-report")
    public ResponseEntity<DiagnosticReport> getDiagnosticReportByEncounterId(@RequestParam String encounterId) {
        Optional<DiagnosticReport> diagnosticReport = diagnosticReportService.getDiagnosticReportByEncounterId(encounterId);
        return diagnosticReport.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/diagnostic-Reports")
    public ResponseEntity<List<DiagnosticReport>> getAllDiagnosticReports() {
        List<DiagnosticReport> diagnosticReports = diagnosticReportService.getAllDiagnosticReports();
        return ResponseEntity.ok(diagnosticReports);
    }
}

