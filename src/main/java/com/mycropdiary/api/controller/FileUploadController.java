package com.mycropdiary.api.controller;

import com.mycropdiary.api.dto.common.ApiResp;
import com.mycropdiary.api.dto.file.FileUploadResp;
import com.mycropdiary.api.service.CloudinaryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/files")
@Tag(name = "00. Tải lên tập tin (File Upload)", description = "APIs tải lên hình ảnh và tài liệu minh chứng nông trại qua Cloudinary")
public class FileUploadController {

    private final CloudinaryService cloudinaryService;

    public FileUploadController(CloudinaryService cloudinaryService) {
        this.cloudinaryService = cloudinaryService;
    }

    @Operation(
            summary = "Tải lên 1 tập tin / hình ảnh minh chứng",
            description = "Tải lên 1 file (hình ảnh JPG, PNG, WEBP hoặc tài liệu PDF, DOCX) lên Cloudinary và trả về URL HTTPS an toàn để lưu vào cơ sở dữ liệu."
    )
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.OK)
    public ApiResp<FileUploadResp> uploadFile(
            @Parameter(description = "Tập tin cần tải lên (tối đa 15MB)")
            @RequestParam("file") MultipartFile file,
            @Parameter(description = "Thư mục đích trên Cloudinary (mặc định: mycropdiary/evidence)")
            @RequestParam(value = "folder", defaultValue = "mycropdiary/evidence") String folder
    ) {
        FileUploadResp response = cloudinaryService.uploadFile(file, folder);
        return ApiResp.ok("Tải lên tập tin thành công.", response);
    }

    @Operation(
            summary = "Tải lên nhiều tập tin minh chứng",
            description = "Tải lên đồng thời danh sách nhiều file (ảnh chụp, tài liệu minh chứng VietGAP, hợp đồng thuê đất...)."
    )
    @PostMapping(value = "/upload-multiple", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.OK)
    public ApiResp<List<FileUploadResp>> uploadMultipleFiles(
            @Parameter(description = "Danh sách tập tin cần tải lên")
            @RequestParam("files") List<MultipartFile> files,
            @Parameter(description = "Thư mục đích trên Cloudinary")
            @RequestParam(value = "folder", defaultValue = "mycropdiary/evidence") String folder
    ) {
        List<FileUploadResp> responses = cloudinaryService.uploadFiles(files, folder);
        return ApiResp.ok("Tải lên các tập tin thành công.", responses);
    }

    @Operation(
            summary = "Xóa tập tin khỏi Cloudinary",
            description = "Xóa tập tin đã tải lên Cloudinary dựa trên publicId."
    )
    @DeleteMapping("/{publicId}")
    public ApiResp<Boolean> deleteFile(
            @Parameter(description = "Public ID của asset trên Cloudinary")
            @PathVariable String publicId
    ) {
        boolean deleted = cloudinaryService.deleteFile(publicId);
        return ApiResp.ok("Xóa tập tin thành công.", deleted);
    }
}
