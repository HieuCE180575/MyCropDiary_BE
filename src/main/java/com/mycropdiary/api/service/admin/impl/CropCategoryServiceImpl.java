package com.mycropdiary.api.service.admin.impl;

import com.mycropdiary.api.dto.cultivation.CreateCropCategoryReqDTO;
import com.mycropdiary.api.dto.cultivation.CropCategoryRespDTO;
import com.mycropdiary.api.dto.cultivation.UpdateCropCategoryReqDTO;
import com.mycropdiary.api.dto.cultivation.UpdateCropCategoryStatusReqDTO;
import com.mycropdiary.api.entity.cultivation.CropCategory;
import com.mycropdiary.api.exception.ResourceNotFoundException;
import com.mycropdiary.api.mapper.CropCategoryMapper;
import com.mycropdiary.api.repository.cultivation.CropCategoryRepository;
import com.mycropdiary.api.service.admin.CropCategoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class CropCategoryServiceImpl implements CropCategoryService {

    private final CropCategoryRepository cropCategoryRepository;
    private final CropCategoryMapper cropCategoryMapper;

    @Autowired
    public CropCategoryServiceImpl(CropCategoryRepository cropCategoryRepository, CropCategoryMapper cropCategoryMapper) {
        this.cropCategoryRepository = cropCategoryRepository;
        this.cropCategoryMapper = cropCategoryMapper != null ? cropCategoryMapper : new CropCategoryMapper();
    }

    public CropCategoryServiceImpl(CropCategoryRepository cropCategoryRepository) {
        this(cropCategoryRepository, new CropCategoryMapper());
    }

    @Override
    @Transactional(readOnly = true)
    public List<CropCategoryRespDTO> getCropCategories() {
        return cropCategoryMapper.toResponseList(cropCategoryRepository.findAll());
    }

    @Override
    public CropCategoryRespDTO createCropCategory(CreateCropCategoryReqDTO request) {
        CropCategory category = new CropCategory(request.categoryName(), request.description());
        category.setImageUrl(request.imageUrl());
        if (request.status() != null && !request.status().isBlank()) {
            category.setStatus(request.status());
        }
        CropCategory saved = cropCategoryRepository.save(category);
        return cropCategoryMapper.toResponse(saved);
    }

    @Override
    public CropCategoryRespDTO updateCropCategory(Integer id, UpdateCropCategoryReqDTO request) {
        CropCategory category = cropCategoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy danh mục ID: " + id));
        category.setCategoryName(request.categoryName());
        category.setDescription(request.description());
        category.setImageUrl(request.imageUrl());
        if (request.status() != null && !request.status().isBlank()) {
            category.setStatus(request.status());
        }
        CropCategory saved = cropCategoryRepository.save(category);
        return cropCategoryMapper.toResponse(saved);
    }

    @Override
    public CropCategoryRespDTO updateCropCategoryStatus(Integer id, UpdateCropCategoryStatusReqDTO request) {
        CropCategory category = cropCategoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy danh mục ID: " + id));
        category.setStatus(request != null ? request.status() : "ACTIVE");
        CropCategory saved = cropCategoryRepository.save(category);
        return cropCategoryMapper.toResponse(saved);
    }
}
