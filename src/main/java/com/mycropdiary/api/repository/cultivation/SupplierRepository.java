package com.mycropdiary.api.repository.cultivation;

import com.mycropdiary.api.entity.cultivation.Supplier;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SupplierRepository extends JpaRepository<Supplier, Integer> {

    @Query("SELECT s FROM Supplier s WHERE s.farm.id = :farmId ORDER BY s.createdAt DESC")
    List<Supplier> findByFarmIdOrderByCreatedAtDesc(@Param("farmId") Integer farmId);

    @Query("SELECT s FROM Supplier s WHERE s.farm.id = :farmId " +
            "AND (:keyword IS NULL OR LOWER(s.supplierName) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "OR LOWER(s.contactName) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "OR s.phone LIKE CONCAT('%', :keyword, '%')) " +
            "ORDER BY s.createdAt DESC")
    List<Supplier> searchSuppliers(@Param("farmId") Integer farmId, @Param("keyword") String keyword);

    boolean existsByFarmIdAndSupplierNameIgnoreCase(Integer farmId, String supplierName);

    boolean existsByFarmIdAndSupplierNameIgnoreCaseAndIdNot(Integer farmId, String supplierName, Integer id);
}
