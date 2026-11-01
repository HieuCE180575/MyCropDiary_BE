package com.mycropdiary.api.repository.cultivation;

import com.mycropdiary.api.entity.cultivation.Expense;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ExpenseRepository extends JpaRepository<Expense, Integer> {

    @Query("SELECT e FROM Expense e " +
            "JOIN FETCH e.farm f " +
            "LEFT JOIN FETCH e.cropSeason cs " +
            "LEFT JOIN FETCH e.farmTask t " +
            "LEFT JOIN FETCH e.recordedBy m " +
            "LEFT JOIN FETCH m.user u " +
            "WHERE f.id = :farmId " +
            "AND (:cropSeasonId IS NULL OR cs.id = :cropSeasonId) " +
            "AND (:category IS NULL OR LOWER(e.expenseCategory) = LOWER(:category)) " +
            "AND (:startDate IS NULL OR e.expenseDate >= :startDate) " +
            "AND (:endDate IS NULL OR e.expenseDate <= :endDate) " +
            "AND (:keyword IS NULL OR (LOWER(e.description) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(e.expenseCategory) LIKE LOWER(CONCAT('%', :keyword, '%')))) " +
            "ORDER BY e.expenseDate DESC, e.createdAt DESC")
    List<Expense> searchExpenses(
            @Param("farmId") Integer farmId,
            @Param("cropSeasonId") Integer cropSeasonId,
            @Param("category") String category,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("keyword") String keyword
    );

    @Query("SELECT e FROM Expense e " +
            "JOIN FETCH e.farm f " +
            "LEFT JOIN FETCH e.cropSeason cs " +
            "LEFT JOIN FETCH e.farmTask t " +
            "LEFT JOIN FETCH e.recordedBy m " +
            "LEFT JOIN FETCH m.user u " +
            "WHERE e.id = :expenseId")
    Optional<Expense> findByIdWithDetails(@Param("expenseId") Integer expenseId);

    @Query("SELECT e FROM Expense e " +
            "JOIN FETCH e.farm f " +
            "LEFT JOIN FETCH e.cropSeason cs " +
            "WHERE f.id = :farmId " +
            "AND (:cropSeasonId IS NULL OR cs.id = :cropSeasonId) " +
            "AND (:startDate IS NULL OR e.expenseDate >= :startDate) " +
            "AND (:endDate IS NULL OR e.expenseDate <= :endDate)")
    List<Expense> findExpensesForSummary(
            @Param("farmId") Integer farmId,
            @Param("cropSeasonId") Integer cropSeasonId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );
}

