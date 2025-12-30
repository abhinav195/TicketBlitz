package com.ticketblitz.event.service;

import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class MinioService {

    private final MinioClient minioClient;

    @Value("${minio.bucket}")
    private String bucketName;

    public MinioService(MinioClient minioClient) {
        this.minioClient = minioClient;
    }

    //Auto-create bucket on startup
    @PostConstruct
    public void init() {
        try {
            boolean found = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());
            if (!found) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
                System.out.println("✅ MinIO Bucket '" + bucketName + "' created successfully.");
            } else {
                System.out.println("ℹ️ MinIO Bucket '" + bucketName + "' already exists.");
            }
        } catch (Exception e) {
            throw new RuntimeException("Could not initialize MinIO bucket", e);
        }
    }

    public List<String> uploadImages(List<MultipartFile> files) {
        List<String> urls = new ArrayList<>();
        // ... (rest of your existing logic) ...
        // Ensure you use the try-catch block correctly inside the loop
        for (MultipartFile file : files) {
            String fileName = UUID.randomUUID() + "-" + file.getOriginalFilename();
            try (InputStream is = file.getInputStream()) {
                minioClient.putObject(
                        PutObjectArgs.builder()
                                .bucket(bucketName)
                                .object(fileName)
                                .stream(is, file.getSize(), -1)
                                .contentType(file.getContentType())
                                .build()
                );
                // Assuming public access is configured, or just storing the filename
                urls.add(fileName);
            } catch (Exception e) {
                throw new RuntimeException("Failed to upload file: " + file.getOriginalFilename(), e);
            }
        }
        return urls;
    }
}
