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

    



}
