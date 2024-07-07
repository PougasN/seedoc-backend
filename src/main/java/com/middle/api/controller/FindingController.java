package com.middle.api.controller;

import com.middle.api.entity.Finding;
import com.middle.api.service.FindingService;
import com.middle.api.service.MinioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
public class FindingController {

    @Autowired
    private FindingService findingService;

    @Autowired
    private MinioService minioService;

    // POST a new Finding
    @PostMapping("/findings")
    public Finding addFinding(
            @RequestParam("mediaId") Long mediaId,
            @RequestParam("time") Double time,
            @RequestParam("comment") String comment,
            @RequestParam("frame") MultipartFile frame) throws IOException {
        try {
            String bucketName = "images-bucket";
            String objectName = mediaId + "/" + frame.getOriginalFilename();
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
            finding.setFrameUrl(frameUrl);
            return findingService.saveFinding(finding);
        } catch (Exception e) {
            throw new IOException("Error occurred while uploading frame: " + e.getMessage(), e);
        }
    }

    // GET all Findings of a video/media
    @GetMapping("/findings/{mediaId}")
    public List<Finding> getFindings(@PathVariable Long mediaId) {
        return findingService.getFindingsByMediaId(mediaId);
    }
}
