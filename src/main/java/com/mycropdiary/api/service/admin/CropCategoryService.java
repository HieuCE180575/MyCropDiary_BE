package com.mycropdiary.api.service.admin;

import com.mycropdiary.api.dto.cultivation.CreateCropCategoryReqDTO;
import com.mycropdiary.api.dto.cultivation.CropCategoryRespDTO;
import com.mycropdiary.api.dto.cultivation.UpdateCropCategoryReqDTO;
import com.mycropdiary.api.dto.cultivation.UpdateCropCategoryStatusReqDTO;

import java.util.List;

public interface CropCategoryService {
    List<CropCategoryRespDTO> getCropCategories();
    CropCategoryRespDTO createCropCategory(CreateCropCategoryReqDTO request);
    CropCategoryRespDTO updateCropCategory(Integer id, UpdateCropCategoryReqDTO request);
    CropCategoryRespDTO updateCropCategoryStatus(Integer id, UpdateCropCategoryStatusReqDTO request);
}
