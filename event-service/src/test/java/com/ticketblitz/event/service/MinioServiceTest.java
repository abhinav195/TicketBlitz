package com.ticketblitz.event.service;

import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.ObjectWriteResponse;  // FIX: Import this
import io.minio.PutObjectArgs;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MinioServiceTest {

    @Mock
    private MinioClient minioClient;

    private MinioService minioService;

    @BeforeEach
    void setUp() {
        minioService = new MinioService(minioClient);
        ReflectionTestUtils.setField(minioService, "bucketName", "test-bucket");
    }

    @Test
    @DisplayName("init: Should create bucket if it doesn't exist")
    void init_CreatesBucket() throws Exception {
        when(minioClient.bucketExists(any(BucketExistsArgs.class))).thenReturn(false);

        minioService.init();

        verify(minioClient).bucketExists(any(BucketExistsArgs.class));
        verify(minioClient).makeBucket(any(MakeBucketArgs.class));
    }

    @Test
    @DisplayName("init: Should not create bucket if it already exists")
    void init_BucketExists() throws Exception {
        when(minioClient.bucketExists(any(BucketExistsArgs.class))).thenReturn(true);

        minioService.init();

        verify(minioClient).bucketExists(any(BucketExistsArgs.class));
        verify(minioClient, never()).makeBucket(any(MakeBucketArgs.class));
    }

    @Test
    @DisplayName("init: Should throw RuntimeException when MinIO initialization fails")
    void init_ThrowsException() throws Exception {
        when(minioClient.bucketExists(any(BucketExistsArgs.class)))
                .thenThrow(new RuntimeException("Connection failed"));

        assertThatThrownBy(() -> minioService.init())
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Could not initialize MinIO bucket");
    }

    @Test
    @DisplayName("uploadImages: Should upload files and return URLs")
    void uploadImages_Success() throws Exception {
        MockMultipartFile file1 = new MockMultipartFile(
                "file", "test1.jpg", "image/jpeg", "content1".getBytes());
        MockMultipartFile file2 = new MockMultipartFile(
                "file", "test2.jpg", "image/jpeg", "content2".getBytes());
        List<MultipartFile> files = List.of(file1, file2);

        // FIX: putObject returns ObjectWriteResponse, not void
        when(minioClient.putObject(any(PutObjectArgs.class)))
                .thenReturn(mock(ObjectWriteResponse.class));

        List<String> result = minioService.uploadImages(files);

        assertThat(result).hasSize(2);
        assertThat(result.get(0)).contains("test1.jpg");
        assertThat(result.get(1)).contains("test2.jpg");
        verify(minioClient, times(2)).putObject(any(PutObjectArgs.class));
    }

    @Test
    @DisplayName("uploadImages: Should throw RuntimeException when upload fails")
    void uploadImages_Failure() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.jpg", "image/jpeg", "content".getBytes());

        // FIX: Use when().thenThrow() instead of doThrow()
        when(minioClient.putObject(any(PutObjectArgs.class)))
                .thenThrow(new RuntimeException("Upload failed"));

        assertThatThrownBy(() -> minioService.uploadImages(List.of(file)))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to upload file");
    }
}
