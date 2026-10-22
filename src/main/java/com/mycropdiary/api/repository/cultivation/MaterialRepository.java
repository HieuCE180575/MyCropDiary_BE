package com.mycropdiary.api.repository.cultivation;

import com.mycropdiary.api.entity.cultivation.Material;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository truy vấn thông tin Material (Vật tư).
 */
@Repository
public interface MaterialRepository extends JpaRepository<Material, Integer> {
    Optional<Material> findByIdAndFarmId(Integer id, Integer farmId);
    java.util.List<Material> findAllByFarmId(Integer farmId);
    java.util.List<Material> findAllByFarmIdAndIsActiveTrue(Integer farmId);
    Optional<Material> findByFarmIdAndMaterialNameIgnoreCase(Integer farmId, String materialName);
}
