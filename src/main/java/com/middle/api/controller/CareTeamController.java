package com.middle.api.controller;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import com.middle.api.dto.CareTeamCreationRequest;
import com.middle.api.service.CareTeamService;
import org.hl7.fhir.r4.model.CareTeam;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Reference;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/api/careTeams")
public class CareTeamController {

    @Autowired
    private IGenericClient fhirClient;

    @Autowired
    private FhirContext fhirContext;

    @Autowired
    private CareTeamService careTeamService;

    @PostMapping
    public ResponseEntity<CareTeam> createCareTeam(
            @RequestBody CareTeamCreationRequest request) { // Define CareTeamCreationRequest to include practitioners
        CareTeam careTeam = new CareTeam();
        careTeam.setStatus(CareTeam.CareTeamStatus.ACTIVE);
        careTeam.setSubject(new Reference("Patient/" + request.getPatientId())); // Assign patient

        // Add participants (practitioners) to the CareTeam
        List<CareTeam.CareTeamParticipantComponent> participants = new ArrayList<>();
        for (String practitionerId : request.getPractitionerIds()) {
            CareTeam.CareTeamParticipantComponent participant = new CareTeam.CareTeamParticipantComponent();
            participant.setMember(new Reference("Practitioner/" + practitionerId));

            // Set the role if provided in the request
            participant.setRole(Collections.singletonList(
                    new CodeableConcept().setText(request.getRoleForPractitioner(practitionerId))
            ));

            participants.add(participant);
        }
        careTeam.setParticipant(participants);

        // Save to FHIR server
        MethodOutcome outcome = fhirClient.create().resource(careTeam).execute();
        careTeam.setId(outcome.getId().getIdPart());

        return ResponseEntity.status(HttpStatus.CREATED).body(careTeam);
    }

    @GetMapping("/{careTeamId}")
    public ResponseEntity<String> getCareTeamById(@PathVariable String careTeamId) {
        try {
            // Retrieve CareTeam by ID from FHIR server
            CareTeam careTeam = fhirClient.read()
                    .resource(CareTeam.class)
                    .withId(careTeamId)
                    .execute();

            // Convert the CareTeam resource to a JSON string for easier viewing
            String careTeamJson = fhirContext.newJsonParser().setPrettyPrint(true).encodeResourceToString(careTeam);

            return ResponseEntity.ok(careTeamJson);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("CareTeam not found with ID: " + careTeamId);
        }
    }

    @DeleteMapping("/care-team/{id}")
    public ResponseEntity<Void> deleteCareTeamById(@PathVariable String id) {
        boolean deleted = careTeamService.deleteCareTeamById(id);
        if (deleted) {
            return ResponseEntity.noContent().build(); // Return 204 No Content if successfully deleted
        } else {
            return ResponseEntity.notFound().build(); // Return 404 Not Found if the ID doesn't exist
        }
    }
}
