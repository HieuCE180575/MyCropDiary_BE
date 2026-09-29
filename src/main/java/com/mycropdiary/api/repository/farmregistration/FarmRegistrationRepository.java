package com.mycropdiary.api.repository.farmregistration;

import com.mycropdiary.api.entity.farmregistration.FarmRegistration;
import com.mycropdiary.api.entity.farmregistration.FarmRegistrationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FarmRegistrationRepository extends JpaRepository<FarmRegistration, Long> {

    List<FarmRegistration> findByApplicantUserId(Long applicantUserId);

    Page<FarmRegistration> findByApplicantUserId(Long applicantUserId, Pageable pageable);

    List<FarmRegistration> findByApplicantUserIdAndStatus(Long applicantUserId, FarmRegistrationStatus status);

    // [AI_CHANGE] UC-39: Admin lọc đơn đăng ký theo trạng thái với phân trang
    Page<FarmRegistration> findByStatus(FarmRegistrationStatus status, Pageable pageable);

    Optional<FarmRegistration> findByIdAndApplicantUserId(Long id, Long applicantUserId);
}
