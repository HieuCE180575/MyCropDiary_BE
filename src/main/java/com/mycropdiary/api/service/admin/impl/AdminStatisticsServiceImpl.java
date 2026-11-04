package com.mycropdiary.api.service.admin.impl;

import com.mycropdiary.api.dto.admin.AdminStatisticsResp;
import com.mycropdiary.api.repository.AIFeedbackRepository;
import com.mycropdiary.api.repository.FarmMemberRepository;
import com.mycropdiary.api.repository.FarmRepository;
import com.mycropdiary.api.repository.UserRepository;
import com.mycropdiary.api.repository.cultivation.CropSeasonRepository;
import com.mycropdiary.api.repository.farmregistration.FarmRegistrationRepository;
import com.mycropdiary.api.service.admin.AdminStatisticsService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AdminStatisticsServiceImpl implements AdminStatisticsService {

    private final UserRepository userRepository;
    private final FarmMemberRepository farmMemberRepository;
    private final FarmRegistrationRepository farmRegistrationRepository;
    private final FarmRepository farmRepository;
    private final CropSeasonRepository cropSeasonRepository;
    private final AIFeedbackRepository aiFeedbackRepository;

    public AdminStatisticsServiceImpl(
            UserRepository userRepository,
            FarmMemberRepository farmMemberRepository,
            FarmRegistrationRepository farmRegistrationRepository,
            FarmRepository farmRepository,
            CropSeasonRepository cropSeasonRepository,
            AIFeedbackRepository aiFeedbackRepository) {
        this.userRepository = userRepository;
        this.farmMemberRepository = farmMemberRepository;
        this.farmRegistrationRepository = farmRegistrationRepository;
        this.farmRepository = farmRepository;
        this.cropSeasonRepository = cropSeasonRepository;
        this.aiFeedbackRepository = aiFeedbackRepository;
    }

    @Override
    public AdminStatisticsResp getSystemStatistics() {
        // 1. Account Statistics (UC-45.1)
        long totalUsers = userRepository.count();
        long activeUsers = userRepository.countByAccountStatusIgnoreCase("ACTIVE");
        long lockedUsers = userRepository.countByAccountStatusIgnoreCase("LOCKED");
        long ownerCount = userRepository.countBySystemRoleIgnoreCase("OWNER");
        long staffCount = userRepository.countBySystemRoleIgnoreCase("STAFF");

        var accountStats = new AdminStatisticsResp.AccountStatistics(
                totalUsers, activeUsers, lockedUsers, ownerCount, staffCount
        );

        // 2. Registration Statistics (UC-45.2)
        long totalReg = farmRegistrationRepository.count();
        long pendingReg = farmRegistrationRepository.countByRegistrationStatusIgnoreCase("PENDING");
        long approvedReg = farmRegistrationRepository.countByRegistrationStatusIgnoreCase("APPROVED");
        long rejectedReg = farmRegistrationRepository.countByRegistrationStatusIgnoreCase("REJECTED");

        var regStats = new AdminStatisticsResp.RegistrationStatistics(
                totalReg, pendingReg, approvedReg, rejectedReg
        );

        // 3. Farm Statistics (UC-45.3)
        long totalFarms = farmRepository.count();
        long activeFarms = farmRepository.findAll().stream()
                .filter(f -> f.getFarmStatus() == null || f.getFarmStatus().equalsIgnoreCase("ACTIVE"))
                .count();

        var farmStats = new AdminStatisticsResp.FarmStatistics(
                totalFarms, activeFarms
        );

        // 4. Season Statistics (UC-45.4)
        long totalSeasons = cropSeasonRepository.count();
        long activeSeasons = cropSeasonRepository.countBySeasonStatusIgnoreCase("ACTIVE")
                + cropSeasonRepository.countBySeasonStatusIgnoreCase("IN_PROGRESS");
        long completedSeasons = cropSeasonRepository.countBySeasonStatusIgnoreCase("COMPLETED");

        var seasonStats = new AdminStatisticsResp.SeasonStatistics(
                totalSeasons, activeSeasons, completedSeasons
        );

        // 5. AI Feedback Statistics (UC-45.5)
        long totalFeedbacks = aiFeedbackRepository.count();
        long openFeedbacks = aiFeedbackRepository.countByRepliesIsEmpty();
        long resolvedFeedbacks = aiFeedbackRepository.countByRepliesIsNotEmpty();
        Double avgRating = aiFeedbackRepository.getAverageRating();
        double ratingVal = avgRating != null ? Math.round(avgRating * 10.0) / 10.0 : 5.0;

        var aiFeedbackStats = new AdminStatisticsResp.AIFeedbackStatistics(
                totalFeedbacks, openFeedbacks, resolvedFeedbacks, ratingVal
        );

        return new AdminStatisticsResp(accountStats, regStats, farmStats, seasonStats, aiFeedbackStats);
    }
}

