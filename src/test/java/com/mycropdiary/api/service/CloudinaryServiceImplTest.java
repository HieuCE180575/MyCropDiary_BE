package com.mycropdiary.api.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.Uploader;
import com.mycropdiary.api.dto.file.FileUploadResp;
import com.mycropdiary.api.exception.BadRequestException;
import com.mycropdiary.api.service.impl.CloudinaryServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CloudinaryServiceImplTest {

    @Mock
    private Cloudinary cloudinary;

    @Mock
    private Uploader uploader;

    private CloudinaryServiceImpl cloudinaryService;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        cloudinaryService = new CloudinaryServiceImpl(cloudinary);
        ReflectionTestUtils.setField(cloudinaryService, "uploadDir", tempDir.toString());
    }

    @Test
    @DisplayName("uploadFile: Ném BadRequestException khi file rỗng")
    void uploadFile_EmptyFile_ThrowsException() {
        MockMultipartFile emptyFile = new MockMultipartFile("file", "test.png", "image/png", new byte[0]);
        assertThrows(BadRequestException.class, () -> cloudinaryService.uploadFile(emptyFile, "folder"));
    }

    @Test
    @DisplayName("uploadFile: Ném BadRequestException khi đuôi file không hợp lệ (.exe)")
    void uploadFile_InvalidExtension_ThrowsException() {
        MockMultipartFile badFile = new MockMultipartFile("file", "malware.exe", "application/octet-stream", "bad".getBytes());
        assertThrows(BadRequestException.class, () -> cloudinaryService.uploadFile(badFile, "folder"));
    }

    @Test
    @DisplayName("uploadFile: Tải lên Cloudinary thành công trả về secure_url và publicId")
    void uploadFile_CloudinarySuccess() throws IOException {
        MockMultipartFile file = new MockMultipartFile(
                "file", "vietgap_cert.pdf", "application/pdf", "Valid pdf content".getBytes()
        );

        when(cloudinary.uploader()).thenReturn(uploader);

        Map<String, Object> uploadResult = new HashMap<>();
        uploadResult.put("secure_url", "https://res.cloudinary.com/drajm922/image/upload/v12345/vietgap_cert.pdf");
        uploadResult.put("public_id", "mycropdiary/evidence/vietgap_cert");
        uploadResult.put("resource_type", "image");
        uploadResult.put("format", "pdf");
        uploadResult.put("bytes", 1024);

        when(uploader.upload(any(byte[].class), anyMap())).thenReturn(uploadResult);

        FileUploadResp resp = cloudinaryService.uploadFile(file, "mycropdiary/evidence");

        assertNotNull(resp);
        assertEquals("https://res.cloudinary.com/drajm922/image/upload/v12345/vietgap_cert.pdf", resp.url());
        assertEquals("mycropdiary/evidence/vietgap_cert", resp.publicId());
        assertEquals("vietgap_cert.pdf", resp.originalFilename());
        assertEquals("pdf", resp.format());
    }

    @Test
    @DisplayName("uploadFile: Fallback lưu local khi Cloudinary gặp sự cố mạng")
    void uploadFile_CloudinaryFails_FallbackToLocal() throws IOException {
        MockMultipartFile file = new MockMultipartFile(
                "file", "farm_photo.jpg", "image/jpeg", "Image JPEG binary data".getBytes()
        );

        when(cloudinary.uploader()).thenReturn(uploader);
        when(uploader.upload(any(byte[].class), anyMap())).thenThrow(new RuntimeException("Cloudinary network timeout"));

        FileUploadResp resp = cloudinaryService.uploadFile(file, "mycropdiary/evidence");

        assertNotNull(resp);
        assertTrue(resp.url().startsWith("/files/"));
        assertTrue(resp.publicId().startsWith("local_"));
        assertEquals("farm_photo.jpg", resp.originalFilename());
    }
}
