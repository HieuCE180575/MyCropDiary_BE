package com.mycropdiary.api.repository.cultivation;

import com.mycropdiary.api.entity.cultivation.InputPurchaseItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InputPurchaseItemRepository extends JpaRepository<InputPurchaseItem, Integer> {

    @Query("SELECT i FROM InputPurchaseItem i " +
            "JOIN FETCH i.material m " +
            "WHERE i.inputPurchase.id = :purchaseId")
    List<InputPurchaseItem> findByInputPurchaseIdWithMaterial(@Param("purchaseId") Integer purchaseId);

    void deleteByInputPurchaseId(Integer purchaseId);

    boolean existsByMaterialId(Integer materialId);
}
