package com.mycropdiary.api.repository.cultivation;

import com.mycropdiary.api.entity.cultivation.InputPurchaseDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository truy vấn InputPurchaseDetail.
 */
@Repository
public interface InputPurchaseDetailRepository extends JpaRepository<InputPurchaseDetail, Long> {
}
