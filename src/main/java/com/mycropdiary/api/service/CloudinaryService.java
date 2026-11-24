package com.mycropdiary.api.service;

import com.mycropdiary.api.dto.file.FileUploadRespDTO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface CloudinaryService {

    /**
     * Tải một file (hình ảnh hoặc tài liệu PDF) lên Cloudinary.
     *
     * @param file Tập tin được tải lên từ request
     * @param folder Thư mục lưu trữ trên Cloudinary (vd: "mycropdiary/evidence")
     * @return Thông tin URL và metadata của file đã lưu
     */
    FileUploadRespDTO uploadFile(MultipartFile file, String folder);

    /**
     * Tải nhiều file lên Cloudinary cùng lúc.
     *
     * @param files Danh sách tập tin
     * @param folder Thư mục lưu trữ trên Cloudinary
     * @return Danh sách thông tin URL và metadata
     */
    List<FileUploadRespDTO> uploadFiles(List<MultipartFile> files, String folder);

    /**
     * Xóa file khỏi Cloudinary theo publicId.
     *
     * @param publicId Public ID của asset trên Cloudinary
     * @return true nếu xóa thành công
     */
    boolean deleteFile(String publicId);
}
