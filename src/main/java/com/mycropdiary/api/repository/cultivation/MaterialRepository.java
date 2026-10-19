package com.mycropdiary.api.repository.cultivation;

import com.mycropdiary.api.entity.cultivation.Material;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository truy vấn thông tin Material (Vật tư).
 */
@Repository
public interface MaterialRepository extends JpaRepository<Material, UUID> {
    Optional<Material> findByIdAndFarmId(UUID id, UUID farmId);
}
