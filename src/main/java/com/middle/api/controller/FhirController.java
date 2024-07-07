package com.middle.api.controller;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import com.middle.api.service.FhirClientService;
import org.hl7.fhir.r4.model.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

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

    // DELETE All Resources (Patients, Encounters, Media)
    @DeleteMapping("/all-resources")
    public ResponseEntity<String> deleteAllResources() {

        // Delete all Diagnostic Reports first
        Bundle diagnosticBundle = fhirClient.search().forResource(DiagnosticReport.class).returnBundle(Bundle.class).execute();
        for (Bundle.BundleEntryComponent entry : diagnosticBundle.getEntry()) {
            DiagnosticReport diagnosticReport = (DiagnosticReport) entry.getResource();
            fhirClient.delete().resourceById(new IdType("DiagnosticReport", diagnosticReport.getIdElement().getIdPart())).execute();
        }

        // Delete all Media first
        Bundle mediaBundle = fhirClient.search().forResource(Media.class).returnBundle(Bundle.class).execute();
        for (Bundle.BundleEntryComponent entry : mediaBundle.getEntry()) {
            Media media = (Media) entry.getResource();
            fhirClient.delete().resourceById(new IdType("Media", media.getIdElement().getIdPart())).execute();
        }

        // Delete all Encounters next
        Bundle encounterBundle = fhirClient.search().forResource(Encounter.class).returnBundle(Bundle.class).execute();
        for (Bundle.BundleEntryComponent entry : encounterBundle.getEntry()) {
            Encounter encounter = (Encounter) entry.getResource();
            fhirClient.delete().resourceById(new IdType("Encounter", encounter.getIdElement().getIdPart())).execute();
        }

        // Finally, delete all Patients
        Bundle patientBundle = fhirClient.search().forResource(Patient.class).returnBundle(Bundle.class).execute();
        for (Bundle.BundleEntryComponent entry : patientBundle.getEntry()) {
            Patient patient = (Patient) entry.getResource();
            fhirClient.delete().resourceById(new IdType("Patient", patient.getIdElement().getIdPart())).execute();
        }

        return ResponseEntity.status(HttpStatus.NO_CONTENT).body("All resources deleted successfully");
    }
    



}
