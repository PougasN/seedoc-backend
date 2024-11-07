package com.middle.api.service;

import io.minio.*;
import io.minio.http.Method;
import io.minio.messages.Item;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.InputStream;

@Service
public class MinioService {

    @Autowired
    private MinioClient minioClient;

    public int getNextFrameNumber(String bucketName, String mediaId) throws Exception {
        Iterable<Result<Item>> results = minioClient.listObjects(
                ListObjectsArgs.builder().bucket(bucketName).prefix(mediaId + "-frame-").build()
        );

        int maxFrameNumber = 0;
        for (Result<Item> result : results) {
            Item item = result.get();
            String objectName = item.objectName();

            // Extract frame number from the format "mediaId-frame-frameNumber.jpg"
            String[] parts = objectName.split("-frame-");
            if (parts.length == 2) {
                try {
                    int frameNumber = Integer.parseInt(parts[1].replaceAll("\\D", ""));
                    maxFrameNumber = Math.max(maxFrameNumber, frameNumber);
                } catch (NumberFormatException e) {
                    // Ignore if the frame number is not parsable
                }
            }
        }
        return maxFrameNumber + 1;
    }

    public String uploadFile(String bucketName, String objectName, InputStream inputStream, long size, String contentType) throws Exception {
        minioClient.putObject(
                PutObjectArgs.builder()
                        .bucket(bucketName)
                        .object(objectName)
                        .stream(inputStream, size, -1)
                        .contentType(contentType)
                        .build()
        );

        String presignedUrl = minioClient.getPresignedObjectUrl(
                GetPresignedObjectUrlArgs.builder()
                        .method(Method.GET)
                        .bucket(bucketName)
                        .object(objectName)
                        .build()
        );

        System.out.println("Generated Presigned URL: " + presignedUrl);

        return presignedUrl;
    }

    public String uploadVideo(String bucketName, String objectName, InputStream inputStream, long size, String contentType) throws Exception {
        return uploadFile(bucketName, objectName, inputStream, size, contentType);
    }
}
