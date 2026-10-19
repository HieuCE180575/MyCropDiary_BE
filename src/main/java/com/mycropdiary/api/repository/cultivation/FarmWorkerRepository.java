package com.mycropdiary.api.repository.cultivation;

import com.mycropdiary.api.entity.cultivation.FarmWorker;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Repository
public interface FarmWorkerRepository extends JpaRepository<FarmWorker, UUID> {
    @Query("SELECT fw FROM FarmWorker fw WHERE fw.farmMember.farm.id = :farmId")
    List<FarmWorker> findByFarmId(@Param("farmId") UUID farmId);

    @Query("SELECT fw FROM FarmWorker fw WHERE fw.id IN :ids AND fw.farmMember.farm.id = :farmId")
    List<FarmWorker> findByIdInAndFarmId(@Param("ids") Collection<UUID> ids, @Param("farmId") UUID farmId);
}
