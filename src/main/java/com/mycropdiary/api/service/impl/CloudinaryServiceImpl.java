package com.mycropdiary.api.service.impl;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.mycropdiary.api.dto.file.FileUploadResp;
import com.mycropdiary.api.exception.BadRequestException;
import com.mycropdiary.api.service.CloudinaryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;

@Service
public class CloudinaryServiceImpl implements CloudinaryService {

    private static final Logger log = LoggerFactory.getLogger(CloudinaryServiceImpl.class);

    private static final long MAX_FILE_SIZE = 15 * 1024 * 1024; // 15 MB

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            "jpg", "jpeg", "png", "webp", "gif", "bmp", "svg",
            "pdf", "doc", "docx", "xls", "xlsx"
    );

    private final Cloudinary cloudinary;

    @Value("${file.upload-dir:uploads/}")
    private String uploadDir;

    public CloudinaryServiceImpl(Cloudinary cloudinary) {
        this.cloudinary = cloudinary;
    }

    @Override
    public FileUploadResp uploadFile(MultipartFile file, String folder) {
        validateFile(file);

        String targetFolder = (folder != null && !folder.isBlank()) ? folder.trim() : "mycropdiary/evidence";
        String originalFilename = file.getOriginalFilename() != null ? file.getOriginalFilename() : "file";

        try {
            Map<String, Object> params = new HashMap<>();
            params.put("folder", targetFolder);
            params.put("resource_type", "auto");
            params.put("use_filename", true);
            params.put("unique_filename", true);
            params.put("access_mode", "public");
            params.put("type", "upload");

            Map<?, ?> uploadResult = cloudinary.uploader().upload(file.getBytes(), params);

            String secureUrl = (String) uploadResult.get("secure_url");
            String publicId = (String) uploadResult.get("public_id");
            String resourceType = (String) uploadResult.get("resource_type");
            String format = (String) uploadResult.get("format");
            Number bytesNum = (Number) uploadResult.get("bytes");
            Long bytes = bytesNum != null ? bytesNum.longValue() : file.getSize();

            log.info("File uploaded successfully to Cloudinary: publicId={}, url={}", publicId, secureUrl);
            return new FileUploadResp(secureUrl, publicId, originalFilename, resourceType, format, bytes);

        } catch (Exception ex) {
            log.warn("Cloudinary upload failed ({}), falling back to local file storage...", ex.getMessage());
            return saveLocally(file, originalFilename);
        }
    }

    @Override
    public List<FileUploadResp> uploadFiles(List<MultipartFile> files, String folder) {
        if (files == null || files.isEmpty()) {
            throw new BadRequestException("Danh sách tập tin tải lên không được để trống.");
        }
        List<FileUploadResp> results = new ArrayList<>();
        for (MultipartFile file : files) {
            if (file != null && !file.isEmpty()) {
                results.add(uploadFile(file, folder));
            }
        }
        return results;
    }

    @Override
    public boolean deleteFile(String publicId) {
        if (publicId == null || publicId.isBlank()) {
            return false;
        }
        try {
            if (publicId.startsWith("local_")) {
                String filename = publicId.substring("local_".length());
                Path path = Paths.get(uploadDir).resolve(filename);
                return Files.deleteIfExists(path);
            }
            Map<?, ?> result = cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
            return "ok".equalsIgnoreCase((String) result.get("result"));
        } catch (Exception ex) {
            log.error("Failed to delete file with publicId {}: {}", publicId, ex.getMessage());
            return false;
        }
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Vui lòng chọn một tập tin hợp lệ để tải lên.");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BadRequestException("Dung lượng tập tin vượt quá giới hạn 15MB.");
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isBlank()) {
            throw new BadRequestException("Tên tập tin không hợp lệ.");
        }

        String extension = getFileExtension(originalFilename).toLowerCase();
        if (extension.isEmpty() || !ALLOWED_EXTENSIONS.contains(extension)) {
            throw new BadRequestException("Định dạng tập tin (." + extension + ") không được hỗ trợ. Vui lòng chỉ tải lên hình ảnh (JPG, PNG, WEBP) hoặc tài liệu (PDF, DOCX).");
        }

        try {
            validateFileMagicBytes(extension, file.getBytes());
        } catch (IOException e) {
            throw new BadRequestException("Không thể đọc nội dung nhị phân của tập tin.");
        }
    }

    private void validateFileMagicBytes(String extension, byte[] bytes) {
        if (bytes == null || bytes.length < 2) {
            return;
        }
        // Block Windows PE/EXE (MZ), ELF binaries, Mach-O executables, and Shebang scripts regardless of extension
        boolean isPeExe = bytes[0] == 'M' && bytes[1] == 'Z';
        boolean isElf = bytes.length >= 4 && bytes[0] == 0x7F && bytes[1] == 'E' && bytes[2] == 'L' && bytes[3] == 'F';
        boolean isShebang = bytes[0] == '#' && bytes[1] == '!';
        boolean isMachO = bytes.length >= 4 && (
                (bytes[0] == (byte) 0xFE && bytes[1] == (byte) 0xED && bytes[2] == (byte) 0xFA && (bytes[3] == (byte) 0xCE || bytes[3] == (byte) 0xCF)) ||
                (bytes[0] == (byte) 0xCF && bytes[1] == (byte) 0xFA && bytes[2] == (byte) 0xED && bytes[3] == (byte) 0xFE) ||
                (bytes[0] == (byte) 0xCA && bytes[1] == (byte) 0xFE && bytes[2] == (byte) 0xBA && bytes[3] == (byte) 0xBE)
        );
        if (isPeExe || isElf || isShebang || isMachO) {
            throw new BadRequestException("Nội dung tập tin (." + extension + ") chứa mã thực thi nhị phân không hợp lệ.");
        }

        // Block HTML/script payloads disguised as images or documents
        String head = new String(bytes, 0, Math.min(bytes.length, 64), java.nio.charset.StandardCharsets.UTF_8).trim().toLowerCase();
        if (head.startsWith("<script") || head.startsWith("<!doctype html") || head.startsWith("<html")) {
            throw new BadRequestException("Nội dung tập tin (." + extension + ") không hợp lệ (ngụy trang HTML/Script).");
        }

        // Block mismatched container signatures (e.g., ZIP/DOCX disguised as image/pdf, or PDF disguised as image)
        boolean isZip = bytes.length >= 4 && bytes[0] == 'P' && bytes[1] == 'K' && bytes[2] == 0x03 && bytes[3] == 0x04;
        boolean isPdf = bytes.length >= 4 && bytes[0] == '%' && bytes[1] == 'P' && bytes[2] == 'D' && bytes[3] == 'F';
        if (isZip && !Set.of("docx", "xlsx").contains(extension)) {
            throw new BadRequestException("Nội dung nhị phân ZIP không khớp với phần mở rộng ." + extension);
        }
        if (isPdf && !"pdf".equals(extension)) {
            throw new BadRequestException("Nội dung nhị phân PDF không khớp với phần mở rộng ." + extension);
        }
    }

    private String getFileExtension(String filename) {
        int lastDotIndex = filename.lastIndexOf('.');
        if (lastDotIndex > 0 && lastDotIndex < filename.length() - 1) {
            return filename.substring(lastDotIndex + 1);
        }
        return "";
    }

    private FileUploadResp saveLocally(MultipartFile file, String originalFilename) {
        try {
            Path uploadPath = Paths.get(uploadDir);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            String sanitized = originalFilename.replaceAll("[^a-zA-Z0-9._-]", "_");
            String uniqueFilename = UUID.randomUUID().toString() + "_" + sanitized;
            Path filePath = uploadPath.resolve(uniqueFilename);

            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            String localUrl = "/files/" + uniqueFilename;
            String format = getFileExtension(originalFilename);

            log.info("Saved file locally at: {}", filePath.toAbsolutePath());
            return new FileUploadResp(
                    localUrl,
                    "local_" + uniqueFilename,
                    originalFilename,
                    format.equalsIgnoreCase("pdf") ? "raw" : "image",
                    format,
                    file.getSize()
            );
        } catch (IOException ioException) {
            log.error("Error saving file locally: {}", ioException.getMessage(), ioException);
            throw new BadRequestException("Không thể lưu tập tin: " + ioException.getMessage());
        }
    }
}
