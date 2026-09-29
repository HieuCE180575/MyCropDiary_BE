package com.mycropdiary.api.repository.cultivation;

import com.mycropdiary.api.entity.cultivation.Plot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PlotRepository extends JpaRepository<Plot, Long> {
    List<Plot> findByProductionAreaId(Long productionAreaId);
}
