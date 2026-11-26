package com.mycropdiary.api.repository.cultivation;

import com.mycropdiary.api.entity.cultivation.FarmWorker;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface FarmWorkerRepository extends JpaRepository<FarmWorker, Integer> {
    @Query("SELECT fw FROM FarmWorker fw LEFT JOIN fw.farm f LEFT JOIN fw.farmMember fm LEFT JOIN fm.farm fmf WHERE f.id = :farmId OR fmf.id = :farmId")
    List<FarmWorker> findByFarmId(@Param("farmId") Integer farmId);

    @Query("SELECT fw FROM FarmWorker fw LEFT JOIN fw.farm f LEFT JOIN fw.farmMember fm LEFT JOIN fm.farm fmf WHERE fw.id IN :ids AND (f.id = :farmId OR fmf.id = :farmId)")
    List<FarmWorker> findByIdInAndFarmId(@Param("ids") Collection<Integer> ids, @Param("farmId") Integer farmId);

    @Query("SELECT fw FROM FarmWorker fw LEFT JOIN fw.farm f LEFT JOIN fw.farmMember fm LEFT JOIN fm.farm fmf WHERE fw.id = :id AND (f.id = :farmId OR fmf.id = :farmId)")
    Optional<FarmWorker> findByIdAndFarmMemberFarmId(@Param("id") Integer id, @Param("farmId") Integer farmId);

    Optional<FarmWorker> findByFarmMemberId(Integer farmMemberId);

    @Query("SELECT CASE WHEN COUNT(fw) > 0 THEN true ELSE false END FROM FarmWorker fw LEFT JOIN fw.farm f LEFT JOIN fw.farmMember fm LEFT JOIN fm.farm fmf WHERE (f.id = :farmId OR fmf.id = :farmId) AND LOWER(fw.workerCode) = LOWER(:workerCode)")
    boolean existsByFarmMemberFarmIdAndWorkerCodeIgnoreCase(@Param("farmId") Integer farmId, @Param("workerCode") String workerCode);
}
