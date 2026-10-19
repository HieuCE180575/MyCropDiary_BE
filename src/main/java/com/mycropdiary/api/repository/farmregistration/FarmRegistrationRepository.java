package com.mycropdiary.api.repository.farmregistration;

import com.mycropdiary.api.entity.farmregistration.FarmRegistration;
import com.mycropdiary.api.entity.farmregistration.FarmRegistrationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FarmRegistrationRepository extends JpaRepository<FarmRegistration, Integer> {

    @Query("SELECT r FROM FarmRegistration r WHERE r.user.id = :userId")
    List<FarmRegistration> findByApplicantUserId(@Param("userId") Integer userId);

    @Query("SELECT r FROM FarmRegistration r WHERE r.user.id = :userId")
    Page<FarmRegistration> findByApplicantUserId(@Param("userId") Integer userId, Pageable pageable);

    @Query("SELECT r FROM FarmRegistration r WHERE r.user.id = :userId AND LOWER(r.registrationStatus) = LOWER(:status)")
    List<FarmRegistration> findByApplicantUserIdAndStatusString(@Param("userId") Integer userId, @Param("status") String status);

    default List<FarmRegistration> findByApplicantUserIdAndStatus(Integer userId, FarmRegistrationStatus status) {
        return findByApplicantUserIdAndStatusString(userId, status.name());
    }

    @Query("SELECT r FROM FarmRegistration r WHERE LOWER(r.registrationStatus) = LOWER(:status)")
    Page<FarmRegistration> findByStatusString(@Param("status") String status, Pageable pageable);

    default Page<FarmRegistration> findByStatus(FarmRegistrationStatus status, Pageable pageable) {
        return findByStatusString(status.name(), pageable);
    }

    @Query("SELECT r FROM FarmRegistration r WHERE r.id = :id AND r.user.id = :userId")
    Optional<FarmRegistration> findByIdAndApplicantUserId(@Param("id") Integer id, @Param("userId") Integer userId);
}
