package com.middle.api.service;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.gclient.IQuery;
import com.middle.api.exception.PatientNotFoundException;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Patient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class FhirClientService {

    @Autowired
    private IGenericClient fhirClient;
    @Autowired
    private FhirContext fhirContext;

    public Patient getPatientById(String id) {
        try {
            return fhirClient.read().resource(Patient.class).withId(id).execute();
        } catch (Exception e) {
            throw new PatientNotFoundException(id);
        }
    }
    public List<Patient> findPatientsByFamilyName(String familyName) {
        IGenericClient client = fhirContext.newRestfulGenericClient(fhirClient.getServerBase());
        Bundle results = client.search().forResource(Patient.class)
                .where(Patient.FAMILY.matches().value(familyName))
                .returnBundle(Bundle.class)
                .execute();

        List<Patient> patients = new ArrayList<>();
        for (Bundle.BundleEntryComponent entry : results.getEntry()) {
            if (entry.getResource() instanceof Patient) {
                patients.add((Patient) entry.getResource());
            }
        }
        return patients;
    }

    public Bundle getAllPatients() {
        IQuery<IBaseBundle> query = fhirClient.search().forResource(Patient.class);
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
}
