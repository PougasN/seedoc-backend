package com.middle.api.service;

import com.middle.api.entity.Finding;
import com.middle.api.repository.FindingRepository;
import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class FindingService {

    @Autowired
    private FindingRepository findingRepository;

    @Autowired
    private MinioClient minioClient;

    public Finding saveFinding(Finding finding) {
        return findingRepository.save(finding);
    }

    public List<Finding> getFindingsByMediaId(Long mediaId) {
        return findingRepository.findByMediaId(mediaId).stream().map(finding -> {
            try {
                String bucketName = "images-bucket";

                // Retrieve the image from MinIO using the frame URL as the object name
                InputStream imageStream = minioClient.getObject(
                        GetObjectArgs.builder()
                                .bucket(bucketName)
                                .object(finding.getFrameUrl())
                                .build()
                );

                // Convert image to Base64
                byte[] imageBytes = imageStream.readAllBytes();
                String base64Image = Base64.getEncoder().encodeToString(imageBytes);

                // Set base64 encoded image as frameUrl
                finding.setFrameUrl("data:image/jpeg;base64," + base64Image);

            } catch (Exception e) {
                e.printStackTrace();
            }
            return finding;
        }).collect(Collectors.toList());
    }

    // In FindingService.java
    public List<String> getPredefinedComments() {
        return Arrays.asList("Fresh Blood", "Vascular Lesion", "Ulcerative Lesion", "Polyp", "Other");
    }

}
