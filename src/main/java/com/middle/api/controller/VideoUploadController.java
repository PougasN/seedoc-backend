package com.middle.api.controller;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import com.middle.api.service.MinioService;
import io.minio.errors.MinioException;
import org.hl7.fhir.r4.model.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

@RestController
public class VideoUploadController {

    @Autowired
    private MinioService minioService;

    @Autowired
    private FhirContext fhirContext;

    @PostMapping("/upload")
    public ResponseEntity<String> uploadVideo(
            @RequestParam("file") MultipartFile file,
            @RequestParam("patientId") String patientId,
            @RequestParam("encounterId") String encounterId) {
        try {
            String bucketName = "videos-bucket";
            String objectName = file.getOriginalFilename();

            // Upload the video
            InputStream inputStream = file.getInputStream();
            String videoUrl = minioService.uploadVideo(bucketName, objectName, inputStream, file.getSize(), file.getContentType());

            // Create FHIR client
            IGenericClient client = fhirContext.newRestfulGenericClient("http://localhost:8080/fhir");

            // Create FHIR Media resource
            Media media = new Media();
            media.setStatus(Media.MediaStatus.COMPLETED);
            media.setType(new CodeableConcept().setText("video"));
            media.setContent(new Attachment()
                    .setUrl(videoUrl)
                    .setContentType(file.getContentType()));
            media.setSubject(new Reference("Patient/" + patientId));
            media.setEncounter(new Reference("Encounter/" + encounterId));
            media.setCreated(new DateTimeType(Date.from(LocalDateTime.now().atZone(ZoneId.systemDefault()).toInstant())));

            MethodOutcome outcome = client.create().resource(media).execute();

            return ResponseEntity.ok(fhirContext.newJsonParser().encodeResourceToString(outcome.getResource()));
        } catch (MinioException | IOException | InvalidKeyException | NoSuchAlgorithmException e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Error occurred: " + e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("An unexpected error occurred: " + e.getMessage());
        }
    }
}