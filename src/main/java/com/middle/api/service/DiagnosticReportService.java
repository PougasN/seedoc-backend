// DiagnosticReportService.java
package com.middle.api.service;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.gclient.IQuery;
import com.middle.api.exception.PatientNotFoundException;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.DiagnosticReport;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class DiagnosticReportService {

    @Autowired
    private FhirContext fhirContext;

    @Autowired
    private IGenericClient fhirClient;

    @Value("${hapi.fhir.server.url}")
    private String hapiFhirUrl;

    public Optional<DiagnosticReport> getDiagnosticReportByEncounterId(String encounterId) {
        IGenericClient client = fhirContext.newRestfulGenericClient(hapiFhirUrl);

        Bundle bundle = client.search()
                .forResource(DiagnosticReport.class)
                .where(DiagnosticReport.ENCOUNTER.hasId("Encounter/" + encounterId))
                .returnBundle(Bundle.class)
                .execute();

        return bundle.getEntry().stream()
                .map(entry -> (DiagnosticReport) entry.getResource())
                .findFirst();
    }

    public Bundle getAllDiagnosticReports() {
        IQuery<IBaseBundle> query = fhirClient.search().forResource(DiagnosticReport.class);
        Bundle bundle = query.returnBundle(Bundle.class).execute();

        Bundle resultBundle = new Bundle();
        resultBundle.setType(Bundle.BundleType.SEARCHSET);
        resultBundle.setTotal(bundle.getTotal());

        while (bundle != null && bundle.hasEntry()) {
            bundle.getEntry().forEach(resultBundle::addEntry);
            if (bundle.getLink("next") != null) {
                bundle = fhirClient.loadPage().next(bundle).execute();
            } else {
                break;
            }
        }
        return resultBundle;
    }

    public boolean deleteDiagnosticReportById(String id) {
        try {
            fhirClient
                    .delete()
                    .resourceById("DiagnosticReport", id)
                    .execute();
            return true;
        } catch (PatientNotFoundException e) {
            return false; // Resource with the specified ID not found
        }
    }
}
