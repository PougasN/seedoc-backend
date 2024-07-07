package com.middle.api.controller;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import org.hl7.fhir.r4.model.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class FhirController {

    @Autowired
    private IGenericClient fhirClient;

    // DELETE All Resources (Patients, Encounters, Media)
    @DeleteMapping("/all-resources")
    public ResponseEntity<String> deleteAllResources() {
        // Delete all Diagnostic Reports
        Bundle diagnosticBundle = fhirClient.search().forResource(DiagnosticReport.class).returnBundle(Bundle.class).execute();
        for (Bundle.BundleEntryComponent entry : diagnosticBundle.getEntry()) {
            DiagnosticReport diagnosticReport = (DiagnosticReport) entry.getResource();
            fhirClient.delete().resourceById(new IdType("DiagnosticReport", diagnosticReport.getIdElement().getIdPart())).execute();
        }
        // Delete all Media
        Bundle mediaBundle = fhirClient.search().forResource(Media.class).returnBundle(Bundle.class).execute();
        for (Bundle.BundleEntryComponent entry : mediaBundle.getEntry()) {
            Media media = (Media) entry.getResource();
            fhirClient.delete().resourceById(new IdType("Media", media.getIdElement().getIdPart())).execute();
        }
        // Delete all Encounters
        Bundle encounterBundle = fhirClient.search().forResource(Encounter.class).returnBundle(Bundle.class).execute();
        for (Bundle.BundleEntryComponent entry : encounterBundle.getEntry()) {
            Encounter encounter = (Encounter) entry.getResource();
            fhirClient.delete().resourceById(new IdType("Encounter", encounter.getIdElement().getIdPart())).execute();
        }
        // Delete all Patients
        Bundle patientBundle = fhirClient.search().forResource(Patient.class).returnBundle(Bundle.class).execute();
        for (Bundle.BundleEntryComponent entry : patientBundle.getEntry()) {
            Patient patient = (Patient) entry.getResource();
            fhirClient.delete().resourceById(new IdType("Patient", patient.getIdElement().getIdPart())).execute();
        }
        return ResponseEntity.status(HttpStatus.NO_CONTENT).body("All resources deleted successfully");
    }
}
