package com.middle.api.controller;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import com.middle.api.service.MinioService;
import io.minio.*;
import io.minio.errors.MinioException;
import jakarta.servlet.http.HttpServletResponse;
import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.r4.model.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
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

    @Autowired
    private MinioClient minioClient;

    @Value("${hapi.fhir.server.url}")
    private String hapiFhirUrl;

    @PostMapping("/upload")
    public ResponseEntity<String> uploadVideo(
            @RequestParam("file") MultipartFile file,
            @RequestParam("patientId") String patientId,
            @RequestParam("encounterId") String encounterId) {
        try {
            String bucketName = "videos-bucket";
            String objectName = encounterId + ".mp4";

            // Check if the bucket exists, if not Create it
            if (!minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build())) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
            }

            // Upload the video
            InputStream inputStream = file.getInputStream();
            String videoUrl = minioService.uploadVideo(bucketName, objectName, inputStream, file.getSize(), file.getContentType());
            System.out.println("Generated Presigned URL: " + videoUrl);


            // String externalVideoUrl = videoUrl.replace("http://minio:9000", "http://172.19.112.1:9000");
            // System.out.println("Generated externalVideoUrl : " + externalVideoUrl);

            // Create FHIR client
            IGenericClient client = fhirContext.newRestfulGenericClient(hapiFhirUrl);

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

            IIdType mediaId = outcome.getId();
            System.out.println("Media ID = " + mediaId.getIdPart());

            // Read the encounter resource
            Encounter encounter = client.read().resource(Encounter.class).withId(encounterId).execute();

            // Add extensions to the encounter resource
            Extension videoUploadedExtension = new Extension("http://example.com/fhir/StructureDefinition/videoUploaded", new BooleanType(true));
            encounter.addExtension(videoUploadedExtension);

            Extension mediaIdExt = new Extension("http://example.com/fhir/StructureDefinition/mediaId", new Reference("Media/" + mediaId.getIdPart()));
            encounter.addExtension(mediaIdExt);

            // Update the encounter status to IN-PROGRESS
            encounter.setStatus(Encounter.EncounterStatus.INPROGRESS);

            // Update the encounter resource
            client.update().resource(encounter).execute();

            return ResponseEntity.ok(fhirContext.newJsonParser().encodeResourceToString(outcome.getResource()));
        } catch (MinioException | IOException | InvalidKeyException | NoSuchAlgorithmException e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Error occurred: " + e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("An unexpected error occurred: " + e.getMessage());
        }
    }

    @GetMapping("/video/{encounterId}")
    public ResponseEntity<InputStreamResource> getVideo(@PathVariable String encounterId) {
    try {
        String bucketName = "videos-bucket";
        String objectName = encounterId + ".mp4";
        
        // Fetch the video file from MinIO
        InputStream videoStream = minioClient.getObject(
            GetObjectArgs.builder()
                .bucket(bucketName)
                .object(objectName)
                .build()
        );

        // Prepare the InputStreamResource for streaming
        InputStreamResource resource = new InputStreamResource(videoStream);

        // Set headers
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + objectName + "\"");
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);

        return ResponseEntity.ok()
                .headers(headers)
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(resource);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(null);
        }
    }

    @GetMapping("/download/{mediaId}")
    public void getMedia(@PathVariable String mediaId, HttpServletResponse response) {
        try {
            // Create FHIR client
            IGenericClient client = fhirContext.newRestfulGenericClient(hapiFhirUrl);
            System.out.println("this is the client = " + client.toString());

            // Retrieve the Media resource
            Media media = client.read().resource(Media.class).withId(mediaId).execute();
            System.out.println("this is the media = " + media.toString());

            // Fetch the video from MinIO
            try (InputStream stream = minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket("videos-bucket")
                            .object("33.mp4")
                            .build())) {

                response.setContentType(MediaType.APPLICATION_OCTET_STREAM_VALUE);
                response.setHeader("Content-Disposition", "inline; filename=\"" + "33" + "\"");
                stream.transferTo(response.getOutputStream());
            }
        } catch (MinioException e) {
            throw new RuntimeException("Error fetching media", e);
        } catch (Exception e) {
            throw new RuntimeException("Error streaming media", e);
        }
    }
}