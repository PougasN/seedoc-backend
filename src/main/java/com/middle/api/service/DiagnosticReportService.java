// DiagnosticReportService.java
package com.middle.api.service;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import org.hl7.fhir.r4.model.DiagnosticReport;
import org.hl7.fhir.r4.model.Bundle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class DiagnosticReportService {

    @Autowired
    private FhirContext fhirContext;

    public Optional<DiagnosticReport> getDiagnosticReportByEncounterId(String encounterId) {
        IGenericClient client = fhirContext.newRestfulGenericClient("http://localhost:8080/fhir");

        Bundle bundle = client.search()
                .forResource(DiagnosticReport.class)
                .where(DiagnosticReport.ENCOUNTER.hasId("Encounter/" + encounterId))
                .returnBundle(Bundle.class)
                .execute();

        return bundle.getEntry().stream()
                .map(entry -> (DiagnosticReport) entry.getResource())
                .findFirst();
    }

    public List<DiagnosticReport> getAllDiagnosticReports() {
        IGenericClient client = fhirContext.newRestfulGenericClient("http://localhost:8080/fhir");

        Bundle bundle = client.search()
                .forResource(DiagnosticReport.class)
                .returnBundle(Bundle.class)
                .execute();

        return bundle.getEntry().stream()
                .map(entry -> (DiagnosticReport) entry.getResource())
                .collect(Collectors.toList());
    }
}
