package com.middle.api.controller;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import com.middle.api.service.DiagnosticReportService;
import org.hl7.fhir.r4.model.Attachment;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.DiagnosticReport;
import org.hl7.fhir.r4.model.IdType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
public class DiagnosticReportController {

    @Autowired
    private IGenericClient fhirClient;

    @Autowired
    private FhirContext fhirContext;

    @Autowired
    private DiagnosticReportService diagnosticReportService;

    // Finalize the DiagnosticReport
    @PutMapping(value = "/diagnostic-report/{id}/finalize", consumes = "application/fhir+json")
    public ResponseEntity<String> finalizeDiagnosticReport(
            @PathVariable("id") String reportId,
            @RequestBody Map<String, Object> updates) {
        try {
            // Fetch the existing DiagnosticReport
            DiagnosticReport diagnosticReport = fhirClient.read()
                    .resource(DiagnosticReport.class)
                    .withId(reportId)
                    .execute();

            // Update the status to "final"
            diagnosticReport.setStatus(DiagnosticReport.DiagnosticReportStatus.FINAL);

            // Update the conclusion if provided
            if (updates.containsKey("conclusion")) {
                diagnosticReport.setConclusion(updates.get("conclusion").toString());
            }

            // Update the presentedForm with image references, if provided
            if (updates.containsKey("presentedForm")) {
                List<Map<String, String>> presentedFormUpdates = (List<Map<String, String>>) updates.get("presentedForm");
                List<Attachment> attachments = presentedFormUpdates.stream().map(form -> {
                    Attachment attachment = new Attachment();
                    attachment.setContentType(form.get("contentType"));
                    attachment.setUrl(form.get("url"));
                    attachment.setTitle(form.get("title"));
                    return attachment;
                }).collect(Collectors.toList());
                diagnosticReport.setPresentedForm(attachments);
            }

            // Update the DiagnosticReport in the FHIR server
            fhirClient.update().resource(diagnosticReport).execute();

            return ResponseEntity.ok("Diagnostic report finalized successfully");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error finalizing diagnostic report: " + e.getMessage());
        }
    }


    // POST new Diagnostic Report
    @PostMapping(value = "/diagnostic-report", consumes = "application/fhir+json")
    public ResponseEntity<String> createDiagnosticReport(@RequestBody DiagnosticReport diagnosticReport) {
        MethodOutcome outcome = fhirClient.create().resource(diagnosticReport).execute();
        IdType id = (IdType) outcome.getId();
        DiagnosticReport createdReport = fhirClient.read().resource(DiagnosticReport.class).withId(id.getIdPart()).execute();
        String reportString = fhirContext.newJsonParser().setPrettyPrint(true).encodeResourceToString(createdReport);
        return ResponseEntity.status(HttpStatus.CREATED).body(reportString);
    }

    // GET a Diagnostic Report with an ID
    @GetMapping("/diagnostic-report/{id}")
    public ResponseEntity<String> getDiagnosticReportById(@PathVariable String id) {
        DiagnosticReport diagnosticReport = fhirClient.read().resource(DiagnosticReport.class).withId(id).execute();
        String diagnosticReportJson = fhirContext.newJsonParser().setPrettyPrint(true).encodeResourceToString(diagnosticReport);
        return ResponseEntity.ok(diagnosticReportJson);
    }

    // GET a Diagnostic Report of an Encounter
    @GetMapping("/diagnostic-report")
    public ResponseEntity<DiagnosticReport> getDiagnosticReportByEncounterId(@RequestParam String encounterId) {
        Optional<DiagnosticReport> diagnosticReport = diagnosticReportService.getDiagnosticReportByEncounterId(encounterId);
        return diagnosticReport.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    // GET All Diagnostic Reports
    @GetMapping("/diagnostic-reports")
    public ResponseEntity<String> getAllDiagnosticReports() {
        Bundle bundle = diagnosticReportService.getAllDiagnosticReports();
        String bundleJson = fhirContext.newJsonParser().setPrettyPrint(true).encodeResourceToString(bundle);
        return ResponseEntity.ok(bundleJson);
    }

    @DeleteMapping("/diagnostic-report/{id}")
    public ResponseEntity<Void> deleteDiagnosticReportById(@PathVariable String id) {
        boolean deleted = diagnosticReportService.deleteDiagnosticReportById(id);
        if (deleted) {
            return ResponseEntity.noContent().build(); // Return 204 No Content if successfully deleted
        } else {
            return ResponseEntity.notFound().build(); // Return 404 Not Found if the ID doesn't exist
        }
    }
}

