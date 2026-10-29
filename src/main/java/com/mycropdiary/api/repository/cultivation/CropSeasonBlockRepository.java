package com.mycropdiary.api.repository.cultivation;

import com.mycropdiary.api.entity.cultivation.CropSeasonBlock;
import com.mycropdiary.api.entity.cultivation.CropSeasonBlockId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CropSeasonBlockRepository extends JpaRepository<CropSeasonBlock, CropSeasonBlockId> {

    @Query("SELECT csb FROM CropSeasonBlock csb " +
            "JOIN FETCH csb.block b " +
            "JOIN FETCH b.zone z " +
            "JOIN FETCH csb.cropSeason cs " +
            "WHERE cs.id = :cropSeasonId " +
            "ORDER BY b.blockName ASC")
    List<CropSeasonBlock> findByCropSeasonIdWithDetails(@Param("cropSeasonId") Integer cropSeasonId);

    @Query("SELECT csb FROM CropSeasonBlock csb " +
            "JOIN FETCH csb.block b " +
            "JOIN FETCH b.zone z " +
            "JOIN FETCH csb.cropSeason cs " +
            "WHERE cs.id = :cropSeasonId AND b.id = :blockId")
    Optional<CropSeasonBlock> findByCropSeasonIdAndBlockIdWithDetails(
            @Param("cropSeasonId") Integer cropSeasonId,
            @Param("blockId") Integer blockId);

    boolean existsByIdCropSeasonIdAndIdBlockId(Integer cropSeasonId, Integer blockId);

    void deleteByIdCropSeasonIdAndIdBlockId(Integer cropSeasonId, Integer blockId);

    List<CropSeasonBlock> findByIdBlockId(Integer blockId);
}
