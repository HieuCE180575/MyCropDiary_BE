package com.mycropdiary.api.repository.cultivation;

import com.mycropdiary.api.entity.cultivation.CropCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CropCategoryRepository extends JpaRepository<CropCategory, Integer> {
    Optional<CropCategory> findByCategoryNameIgnoreCase(String categoryName);
    boolean existsByCategoryNameIgnoreCase(String categoryName);
}

