package com.mycropdiary.api.repository.farmregistration;

import com.mycropdiary.api.entity.farmregistration.FarmRegistration;
import com.mycropdiary.api.entity.farmregistration.FarmRegistrationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FarmRegistrationRepository extends JpaRepository<FarmRegistration, Long> {

    List<FarmRegistration> findByApplicantUserId(Long applicantUserId);

    List<FarmRegistration> findByApplicantUserIdAndStatus(Long applicantUserId, FarmRegistrationStatus status);
}
