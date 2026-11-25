package com.mycropdiary.api.repository.compliance;

import com.mycropdiary.api.entity.compliance.CustomerComplaintLog;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CustomerComplaintLogRepository extends JpaRepository<CustomerComplaintLog, Integer> {
    List<CustomerComplaintLog> findByFarmIdOrderByReceivedDateDescIdDesc(Integer farmId);
}
