package com.mycropdiary.api.controller;

import com.mycropdiary.api.dto.file.FileUploadRespDTO;
import com.mycropdiary.api.exception.BadRequestException;
import com.mycropdiary.api.exception.GlobalExHandler;
import com.mycropdiary.api.service.CloudinaryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class FileUploadControllerTest {

    private MockMvc mockMvc;

    @Mock
    private CloudinaryService cloudinaryService;

    @InjectMocks
    private FileUploadController fileUploadController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(fileUploadController)
                .setControllerAdvice(new GlobalExHandler())
                .build();
    }

    @Test
    @DisplayName("POST /api/v1/files/upload: Tải lên file minh chứng farm thành công trả về 200 OK")
    void uploadFile_Success() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "vietgap_certificate.pdf",
                "application/pdf",
                "Sample PDF content".getBytes()
        );

        FileUploadRespDTO resp = new FileUploadRespDTO(
                "https://res.cloudinary.com/drajm922/image/upload/v12345/vietgap_certificate.pdf",
                "mycropdiary/evidence/vietgap_cert",
                "vietgap_certificate.pdf",
                "image",
                "pdf",
                1024L
        );

        when(cloudinaryService.uploadFile(any(), eq("mycropdiary/evidence"))).thenReturn(resp);

        mockMvc.perform(multipart("/api/v1/files/upload")
                        .file(file)
                        .param("folder", "mycropdiary/evidence"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Tải lên tập tin thành công."))
                .andExpect(jsonPath("$.data.url").value("https://res.cloudinary.com/drajm922/image/upload/v12345/vietgap_certificate.pdf"))
                .andExpect(jsonPath("$.data.publicId").value("mycropdiary/evidence/vietgap_cert"))
                .andExpect(jsonPath("$.data.originalFilename").value("vietgap_certificate.pdf"));
    }

    @Test
    @DisplayName("POST /api/v1/files/upload: File không hợp lệ ném BadRequestException trả về 400")
    void uploadFile_InvalidFile_BadRequest() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "dangerous_script.exe",
                "application/octet-stream",
                "binary".getBytes()
        );

        when(cloudinaryService.uploadFile(any(), any()))
                .thenThrow(new BadRequestException("Định dạng tập tin (.exe) không được hỗ trợ."));

        mockMvc.perform(multipart("/api/v1/files/upload").file(file))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Định dạng tập tin (.exe) không được hỗ trợ."));
    }

    @Test
    @DisplayName("POST /api/v1/files/upload-multiple: Tải lên nhiều file thành công")
    void uploadMultipleFiles_Success() throws Exception {
        MockMultipartFile file1 = new MockMultipartFile("files", "img1.png", "image/png", "img1".getBytes());
        MockMultipartFile file2 = new MockMultipartFile("files", "img2.jpg", "image/jpeg", "img2".getBytes());

        FileUploadRespDTO resp1 = new FileUploadRespDTO("https://res.cloudinary.com/img1.png", "p1", "img1.png", "image", "png", 100L);
        FileUploadRespDTO resp2 = new FileUploadRespDTO("https://res.cloudinary.com/img2.jpg", "p2", "img2.jpg", "image", "jpg", 200L);

        when(cloudinaryService.uploadFiles(any(), any())).thenReturn(List.of(resp1, resp2));

        mockMvc.perform(multipart("/api/v1/files/upload-multiple").file(file1).file(file2))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].url").value("https://res.cloudinary.com/img1.png"))
                .andExpect(jsonPath("$.data[1].url").value("https://res.cloudinary.com/img2.jpg"));
    }

    @Test
    @DisplayName("DELETE /api/v1/files/{publicId}: Xóa file thành công trả về 200")
    void deleteFile_Success() throws Exception {
        when(cloudinaryService.deleteFile("test-public-id")).thenReturn(true);

        mockMvc.perform(delete("/api/v1/files/test-public-id"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").value(true));
    }
}
