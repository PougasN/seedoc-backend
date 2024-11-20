package com.middle.api.controller;

import com.middle.api.entity.Finding;
import com.middle.api.repository.FindingRepository;
import com.middle.api.service.FindingService;
import com.middle.api.service.MinioService;
import io.minio.*;
import io.minio.messages.Item;
import org.apache.commons.compress.utils.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

@RestController
public class FindingController {

    @Autowired
    private FindingService findingService;
    @Autowired
    private MinioService minioService;
    @Autowired
    private MinioClient minioClient;

    @Autowired
    private FindingRepository findingRepository;

    @PostMapping("/findings")
    public Finding addFinding(
            @RequestParam("mediaId") Long mediaId,
            @RequestParam("time") Double time,
            @RequestParam("comment") String comment,
            @RequestParam("frame") MultipartFile frame) throws IOException {
        try {
            String bucketName = "images-bucket";
            String objectName = mediaId + "/" + mediaId + "-" + frame.getOriginalFilename();

            // Upload the image to Minio
            String frameUrl = minioService.uploadFile(
                    bucketName,
                    objectName,
                    frame.getInputStream(),
                    frame.getSize(),
                    frame.getContentType()
            );
            Finding finding = new Finding();
            finding.setMediaId(mediaId);
            finding.setTime(time);
            finding.setComment(comment);
            finding.setFrameUrl(objectName);
            return findingService.saveFinding(finding);
        } catch (Exception e) {
            throw new IOException("Error occurred while uploading frame: " + e.getMessage(), e);
        }
    }

    @GetMapping("/findings/{mediaId}")
    public ResponseEntity<List<Map<String, Object>>> getFindingsWithFrames(@PathVariable Long mediaId) {
        List<Finding> findings = findingService.getFindingsByMediaId(mediaId);
        List<Map<String, Object>> findingsWithFrames = new ArrayList<>();

        findings.forEach(finding -> {
            Map<String, Object> findingData = new HashMap<>();
            findingData.put("id", finding.getId());
            findingData.put("mediaId", finding.getMediaId());
            findingData.put("time", finding.getTime());
            findingData.put("comment", finding.getComment());

            try {
                // Get the frame image from MinIO
                String bucketName = "images-bucket";
                String objectName = finding.getMediaId() + "/" + finding.getFrameUrl().substring(finding.getFrameUrl().lastIndexOf("/") + 1);
                InputStream frameStream = minioClient.getObject(
                        GetObjectArgs.builder()
                                .bucket(bucketName)
                                .object(objectName)
                                .build()
                );

                // Convert image to base64
                @SuppressWarnings("deprecation")
                byte[] bytes = IOUtils.toByteArray(frameStream);
                String base64Frame = Base64.getEncoder().encodeToString(bytes);
                findingData.put("frameData", base64Frame);

            } catch (Exception e) {
                e.printStackTrace();
                findingData.put("frameData", null); // or handle as needed
            }

            findingsWithFrames.add(findingData);
        });

        return ResponseEntity.ok(findingsWithFrames);
    }

    @PostMapping("/uploadFrame/{encounterId}")
    public ResponseEntity<String> uploadFrame(
            @PathVariable String encounterId,
            @RequestParam("frame") MultipartFile frame) {
        try {
            String bucketName = "images-bucket";
            String folderName = encounterId; // Folder in MinIO for this encounter

            // Ensure the bucket exists
            if (!minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build())) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
            }

            // List existing frames to determine the next frame number
            Iterable<Result<Item>> items = minioClient.listObjects(ListObjectsArgs.builder().bucket(bucketName).prefix(folderName + "/").build());
            int frameNumber = 1;
            for (Result<Item> item : items) {
                String fileName = item.get().objectName();
                int currentFrameNumber = Integer.parseInt(fileName.substring(fileName.lastIndexOf("frame-") + 6, fileName.lastIndexOf(".png")));
                frameNumber = Math.max(frameNumber, currentFrameNumber + 1); // Increment to get the next frame number
            }

            // Set the object name with the calculated frame number
            String objectName = folderName + "/" + encounterId + "-frame-" + frameNumber + ".png";

            // Upload the frame to MinIO
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .stream(frame.getInputStream(), frame.getSize(), -1)
                            .contentType(frame.getContentType())
                            .build()
            );

            return ResponseEntity.ok("Frame uploaded successfully with name: " + objectName);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error uploading frame: " + e.getMessage());
        }
    }

    @GetMapping("/findings/get/{mediaId}")
    public List<Finding> getFindings(@PathVariable Long mediaId) {
        return findingService.getFindingsByMediaId(mediaId);
    }

    @GetMapping("/download")
    public ResponseEntity<InputStreamResource> downloadImage(@RequestParam("title") String title) {
        try {
            String bucketName = "test-bucket";
            String objectName = title;

            // Get the object as a stream from MinIO
            InputStream imageStream = minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .build()
            );

            // Wrap the InputStream in InputStreamResource to send as response
            InputStreamResource resource = new InputStreamResource(imageStream);

            // Set appropriate headers and content type
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + objectName + "\"")
                    .contentType(MediaType.IMAGE_JPEG)  // Adjust if your file is a different type
                    .body(resource);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(null);
        }
    }

    // In FindingController.java
    @GetMapping("/findings/predefined-comments")
    public ResponseEntity<List<String>> getPredefinedComments() {
        return ResponseEntity.ok(findingService.getPredefinedComments());
    }

    @PutMapping("/findings/{id}")
    public ResponseEntity<?> updateFinding(@PathVariable Long id, @RequestBody Map<String, String> request) {
        try {
            Optional<Finding> optionalFinding = findingRepository.findById(id);
            if (optionalFinding.isPresent()) {
                Finding finding = optionalFinding.get();
                if (request.containsKey("comment")) {
                    finding.setComment(request.get("comment"));
                    findingRepository.save(finding);
                    return ResponseEntity.ok(finding);
                } else {
                    return ResponseEntity.badRequest().body("Missing comment field");
                }
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Finding not found");
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error updating finding");
        }
    }
}
