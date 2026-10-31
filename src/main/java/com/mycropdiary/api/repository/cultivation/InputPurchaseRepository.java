package com.mycropdiary.api.repository.cultivation;

import com.mycropdiary.api.entity.cultivation.InputPurchase;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface InputPurchaseRepository extends JpaRepository<InputPurchase, Integer> {

    @Query("SELECT p FROM InputPurchase p " +
            "JOIN FETCH p.farm f " +
            "LEFT JOIN FETCH p.supplier s " +
            "LEFT JOIN FETCH p.purchasedBy pb " +
            "LEFT JOIN FETCH pb.user u " +
            "WHERE f.id = :farmId " +
            "ORDER BY p.purchaseDate DESC, p.id DESC")
    List<InputPurchase> findByFarmIdOrderByPurchaseDateDesc(@Param("farmId") Integer farmId);

    @Query("SELECT p FROM InputPurchase p " +
            "JOIN FETCH p.farm f " +
            "LEFT JOIN FETCH p.supplier s " +
            "LEFT JOIN FETCH p.purchasedBy pb " +
            "LEFT JOIN FETCH pb.user u " +
            "WHERE f.id = :farmId " +
            "AND (:keyword IS NULL OR LOWER(p.referenceNo) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "     OR LOWER(p.warehouseReceiptNo) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "     OR LOWER(s.supplierName) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
            "AND (:supplierId IS NULL OR s.id = :supplierId) " +
            "AND (:fromDate IS NULL OR p.purchaseDate >= :fromDate) " +
            "AND (:toDate IS NULL OR p.purchaseDate <= :toDate) " +
            "ORDER BY p.purchaseDate DESC, p.id DESC")
    List<InputPurchase> searchPurchases(
            @Param("farmId") Integer farmId,
            @Param("keyword") String keyword,
            @Param("supplierId") Integer supplierId,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate);

    boolean existsBySupplierId(Integer supplierId);

    long countBySupplierId(Integer supplierId);
}
