package com.mycropdiary.api.service.unit;

import com.cloudinary.Cloudinary;
import com.cloudinary.Uploader;
import com.cloudinary.utils.ObjectUtils;
import com.mycropdiary.api.controller.FileUploadController;
import com.mycropdiary.api.dto.common.ApiResp;
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

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("FE04_FILE - Cloudinary File Upload & Attachment Management Unit Tests")
public class FE04_FileUploadAndCloudinaryUnitTest {

    @Mock
    private Cloudinary cloudinary;

    @Mock
    private Uploader uploader;

    private CloudinaryServiceImpl cloudinaryService;
    private FileUploadController fileUploadController;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        cloudinaryService = new CloudinaryServiceImpl(cloudinary);
        ReflectionTestUtils.setField(cloudinaryService, "uploadDir", tempDir.toString());
        fileUploadController = new FileUploadController(cloudinaryService);
    }

    @Test
    @DisplayName("UTC01 - Upload valid PNG image successfully to Cloudinary")
    void UTC01_uploadFile_validPng_success() throws IOException {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "farm_certificate.png",
                "image/png",
                "fake image content".getBytes()
        );

        Map<String, Object> uploadResult = new HashMap<>();
        uploadResult.put("secure_url", "https://res.cloudinary.com/mcd/image/upload/v1/farm_certificate.png");
        uploadResult.put("public_id", "mycropdiary/proofs/cert_001");
        uploadResult.put("format", "png");
        uploadResult.put("bytes", (long) file.getSize());

        when(cloudinary.uploader()).thenReturn(uploader);
        when(uploader.upload(any(byte[].class), anyMap())).thenReturn(uploadResult);

        FileUploadResp resp = cloudinaryService.uploadFile(file, "farm_proofs");

        assertNotNull(resp);
        assertEquals("https://res.cloudinary.com/mcd/image/upload/v1/farm_certificate.png", resp.url());
        assertEquals("mycropdiary/proofs/cert_001", resp.publicId());
        assertEquals("farm_certificate.png", resp.originalFilename());
        assertEquals("png", resp.format());
        verify(uploader, times(1)).upload(any(byte[].class), anyMap());
    }

    @Test
    @DisplayName("UTC02 - Upload empty file throws BadRequestException")
    void UTC02_uploadFile_emptyFile_throwsBadRequestException() {
        MockMultipartFile emptyFile = new MockMultipartFile(
                "file",
                "empty.jpg",
                "image/jpeg",
                new byte[0]
        );

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                cloudinaryService.uploadFile(emptyFile, "proofs")
        );
        assertTrue(ex.getMessage().contains("Vui lòng chọn một tập tin hợp lệ"));
    }

    @Test
    @DisplayName("UTC03 - Upload file exceeding 15MB limit throws BadRequestException")
    void UTC03_uploadFile_exceeds10MB_throwsBadRequestException() {
        byte[] largeBytes = new byte[16 * 1024 * 1024]; // 16MB
        MockMultipartFile largeFile = new MockMultipartFile(
                "file",
                "huge_file.pdf",
                "application/pdf",
                largeBytes
        );

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                cloudinaryService.uploadFile(largeFile, "legal_docs")
        );
        assertTrue(ex.getMessage().contains("vượt quá giới hạn 15MB"));
    }

    @Test
    @DisplayName("UTC04 - Upload unsupported executable extension throws BadRequestException")
    void UTC04_uploadFile_unsupportedExtensionExe_throwsBadRequestException() {
        MockMultipartFile exeFile = new MockMultipartFile(
                "file",
                "script.exe",
                "application/octet-stream",
                "binary content".getBytes()
        );

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                cloudinaryService.uploadFile(exeFile, "proofs")
        );
        assertTrue(ex.getMessage().contains("không được hỗ trợ"));
    }

    @Test
    @DisplayName("UTC05 - Upload falls back to local storage when Cloudinary fails")
    void UTC05_uploadFile_cloudinaryUploadException_fallsBackToLocal() throws IOException {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "soil_report.pdf",
                "application/pdf",
                "PDF report binary content".getBytes()
        );

        when(cloudinary.uploader()).thenReturn(uploader);
        when(uploader.upload(any(byte[].class), anyMap())).thenThrow(new RuntimeException("Cloudinary API Timeout"));

        FileUploadResp resp = cloudinaryService.uploadFile(file, "soil_tests");

        assertNotNull(resp);
        assertTrue(resp.publicId().startsWith("local_"));
        assertTrue(resp.url().startsWith("/files/"));
        assertEquals("soil_report.pdf", resp.originalFilename());
    }

    @Test
    @DisplayName("UTC06 - Batch upload multiple valid files successfully")
    void UTC06_uploadMultipleFiles_validList_success() throws IOException {
        MockMultipartFile file1 = new MockMultipartFile("files", "land_title.jpg", "image/jpeg", "image1".getBytes());
        MockMultipartFile file2 = new MockMultipartFile("files", "water_test.pdf", "application/pdf", "pdf2".getBytes());

        Map<String, Object> res1 = Map.of(
                "secure_url", "https://cloudinary.com/land_title.jpg",
                "public_id", "proofs/land_title",
                "format", "jpg",
                "bytes", 100L
        );
        Map<String, Object> res2 = Map.of(
                "secure_url", "https://cloudinary.com/water_test.pdf",
                "public_id", "proofs/water_test",
                "format", "pdf",
                "bytes", 200L
        );

        when(cloudinary.uploader()).thenReturn(uploader);
        when(uploader.upload(any(byte[].class), anyMap())).thenReturn(res1).thenReturn(res2);

        List<FileUploadResp> results = cloudinaryService.uploadFiles(List.of(file1, file2), "proofs");

        assertEquals(2, results.size());
        assertEquals("land_title.jpg", results.get(0).originalFilename());
        assertEquals("water_test.pdf", results.get(1).originalFilename());
    }

    @Test
    @DisplayName("UTC07 - Batch upload empty file list throws BadRequestException")
    void UTC07_uploadMultipleFiles_emptyList_throwsBadRequestException() {
        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                cloudinaryService.uploadFiles(List.of(), "proofs")
        );
        assertTrue(ex.getMessage().contains("Danh sách tập tin"));
    }

    @Test
    @DisplayName("UTC08 - Delete file with Cloudinary publicId successfully")
    void UTC08_deleteFile_cloudinaryPublicId_success() throws IOException {
        when(cloudinary.uploader()).thenReturn(uploader);
        when(uploader.destroy(eq("proofs/cert_001"), anyMap())).thenReturn(Map.of("result", "ok"));

        boolean deleted = cloudinaryService.deleteFile("proofs/cert_001");
        assertTrue(deleted);
        verify(uploader, times(1)).destroy(eq("proofs/cert_001"), anyMap());
    }

    @Test
    @DisplayName("UTC09 - Delete file with local storage path returns true")
    void UTC09_deleteFile_localPath_success() throws IOException {
        File dummy = new File(tempDir.toFile(), "dummy.jpg");
        assertTrue(dummy.createNewFile());

        boolean deleted = cloudinaryService.deleteFile("local_" + dummy.getName());
        assertTrue(deleted);
    }

    @Test
    @DisplayName("UTC10 - File extension spoofing with executable magic bytes is blocked cleanly (Passed)")
    void UTC10_uploadFile_disguisedExtension_defectDF025() {
        // Verified fix for DF-025: CloudinaryServiceImpl inspects binary magic bytes to reject disguised executables
        byte[] peExeMagic = new byte[] { 'M', 'Z', (byte) 0x90, 0x00, 0x03, 0x00 };
        MockMultipartFile disguisedExe = new MockMultipartFile(
                "file",
                "malicious_payload.png",
                "image/png",
                peExeMagic
        );

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                cloudinaryService.uploadFile(disguisedExe, "proofs")
        );
        assertTrue(ex.getMessage().contains("mã thực thi"));
        verifyNoInteractions(uploader);
    }
}
