package com.middle.api.controller;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import io.minio.errors.MinioException;
import jakarta.servlet.http.HttpServletResponse;

import java.io.InputStream;

import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Media;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
public class MediaController {

    @Autowired
    private IGenericClient fhirClient;

    @Autowired
    private FhirContext fhirContext;

    @Autowired
    private MinioClient minioClient;

    // GET all Media resources
    @GetMapping("/media")
    public ResponseEntity<String> getAllMedia() {
        Bundle bundle = fhirClient.search().forResource(Media.class).returnBundle(Bundle.class).execute();
        String mediaString = fhirContext.newJsonParser().setPrettyPrint(true).encodeResourceToString(bundle);
        return ResponseEntity.ok(mediaString);
    }

    // POST new Media
    @PostMapping(value = "/media", consumes = "application/fhir+json")
    public ResponseEntity<String> createMedia(@RequestBody Media media) {
        MethodOutcome outcome = fhirClient.create().resource(media).execute();
        IIdType id = outcome.getId();
        Media createdMedia = fhirClient.read().resource(Media.class).withId(id.getIdPart()).execute();
        String mediaString = fhirContext.newJsonParser().setPrettyPrint(true).encodeResourceToString(createdMedia);
        return ResponseEntity.status(HttpStatus.CREATED).body(mediaString);
    }

    // @GetMapping("/media/{mediaId}")
    // public void getMedia(@PathVariable String mediaId, HttpServletResponse response) {
    //     try (
    //         InputStream stream = minioClient.getObject(
    //             GetObjectArgs.builder()
    //                     .bucket("videos-bucket")
    //                     .object("path/to/your/video/" + mediaId) // Adjust the path as needed
    //                     .build())) {

    //         response.setContentType("video/mp4");
    //         response.setHeader("Content-Disposition", "inline; filename=\"" + mediaId + "\"");
    //         stream.transferTo(response.getOutputStream());
    //     } catch (MinioException e) {
    //         throw new RuntimeException("Error fetching media", e);
    //     } catch (Exception e) {
    //         throw new RuntimeException("Error streaming media", e);
    //     }
    // }

    // GET Media by ID
    @GetMapping("/media/{id}")
    public ResponseEntity<String> getMedia(@PathVariable String id) {
        Media media = fhirClient.read().resource(Media.class).withId(id).execute();
        String mediaString = fhirContext.newJsonParser().setPrettyPrint(true).encodeResourceToString(media);
        return ResponseEntity.ok(mediaString);
    }

    // PUT Media by ID
    @PutMapping(value = "/media/{id}", consumes = "application/fhir+json")
    public ResponseEntity<String> updateMedia(@PathVariable String id, @RequestBody Media media) {
        media.setId(id);
        fhirClient.update().resource(media).execute();
        Media updatedMedia = fhirClient.read().resource(Media.class).withId(id).execute();
        String mediaString = fhirContext.newJsonParser().setPrettyPrint(true).encodeResourceToString(updatedMedia);
        return ResponseEntity.ok(mediaString);
    }

    // DELETE Media by ID
    @DeleteMapping("/media/{id}")
    public ResponseEntity<String> deleteMedia(@PathVariable String id) {
        fhirClient.delete().resourceById(new IdType("Media", id)).execute();
        return ResponseEntity.status(HttpStatus.NO_CONTENT).body("Media resource deleted successfully");
    }

    // DELETE All Media
    @DeleteMapping("/media")
    public ResponseEntity<String> deleteAllMedia() {
        Bundle mediaBundle = fhirClient.search().forResource(Media.class).returnBundle(Bundle.class).execute();
        for (Bundle.BundleEntryComponent entry : mediaBundle.getEntry()) {
            Media media = (Media) entry.getResource();
            fhirClient.delete().resourceById(new IdType("Media", media.getIdElement().getIdPart())).execute();
        }
        return ResponseEntity.status(HttpStatus.NO_CONTENT).body("All media resources deleted successfully");
    }
}