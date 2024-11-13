package com.middle.api.controller;

import com.middle.api.service.PractitionerService;
import org.hl7.fhir.r4.model.Encounter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.hl7.fhir.r4.model.Practitioner;

import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/api/practitioners")
public class PractitionerController {
    @Autowired
    private PractitionerService practitionerService;

    public PractitionerController(PractitionerService practitionerService) {
        this.practitionerService = practitionerService;
    }

    @PostMapping(value = "/create", consumes = "application/fhir+json")
    public ResponseEntity<Practitioner> createPractitioner(@RequestBody Practitioner practitioner) {
        Practitioner createdPractitioner = practitionerService.createPractitioner(practitioner);
        return new ResponseEntity<>(createdPractitioner, HttpStatus.CREATED);
    }

    @GetMapping("/{practitionerId}")
    public ResponseEntity<Practitioner> getPractitionerById(@PathVariable String practitionerId) {
        Practitioner practitioner = practitionerService.getPractitionerById(practitionerId);
        return new ResponseEntity<>(practitioner, HttpStatus.OK);
    }

    @GetMapping("/{practitionerId}/encounters")
    public ResponseEntity<List<Encounter>> getEncountersForPractitioner(@PathVariable String practitionerId) {
        try {
            List<Encounter> encounters = practitionerService.getEncountersForPractitioner(practitionerId);
            return ResponseEntity.ok(encounters);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Collections.emptyList());
        }
    }
}

