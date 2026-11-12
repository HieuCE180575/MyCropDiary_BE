package com.mycropdiary.api.service.cultivation;

import com.mycropdiary.api.dto.vietgap.ChecklistResultItemResp;
import com.mycropdiary.api.dto.vietgap.ChecklistRunResp;
import com.mycropdiary.api.entity.Block;
import com.mycropdiary.api.entity.Farm;
import com.mycropdiary.api.entity.FarmMember;
import com.mycropdiary.api.entity.User;
import com.mycropdiary.api.entity.cultivation.ChecklistResult;
import com.mycropdiary.api.entity.cultivation.ChecklistRule;
import com.mycropdiary.api.entity.cultivation.ChecklistRun;
import com.mycropdiary.api.entity.cultivation.CropSeason;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class VietGapPdfExportServiceTest {

    private final VietGapPdfExportService pdfExportService = new VietGapPdfExportService();

    @Test
    @DisplayName("Tạo tài liệu PDF biên bản tự đánh giá VietGAP A4 hợp lệ")
    void testGenerateAuditReportPdf_Success() {
        Farm farm = new Farm();
        farm.setId(1);
        farm.setFarmName("Trang Trại Xanh VietGAP");

        User user = new User();
        user.setId(10);
        user.setFullName("Nguyễn Văn Kỹ Sư");

        FarmMember member = new FarmMember();
        member.setId(100);
        member.setUser(user);
        member.setFarm(farm);

        CropSeason season = new CropSeason();
        season.setId(1);
        season.setFarm(farm);
        season.setSeasonName("Dưa lưới vụ Xuân 2026");
        season.setStartDate(LocalDate.of(2026, 1, 1));

        Block block = new Block();
        block.setId(1);
        block.setBlockName("Lô A1");

        ChecklistRun run = new ChecklistRun();
        run.setId(1);
        run.setFarm(farm);
        run.setCropSeason(season);
        run.setBlock(block);
        run.setAssignedTo(member);
        run.setRunStatus("completed");
        run.setScheduledAt(Instant.now());
        run.setCompletedAt(Instant.now());
        run.setCreatedAt(Instant.now());
        run.setTotalItems(2);
        run.setPassedItems(2);
        run.setFailedItems(0);
        run.setNaItems(0);
        run.setCompliancePercent(100.0);

        ChecklistRule rule1 = new ChecklistRule(farm, user, "VG-NL-01", "Chất lượng đất và nước",
                "Định kỳ 12 tháng", "NGUỒN_LỰC", "MUST", "ALL", true);
        rule1.setId(101);

        ChecklistResult result1 = new ChecklistResult();
        result1.setId(1);
        result1.setChecklistRun(run);
        result1.setChecklistRule(rule1);
        result1.setCheckedBy(member);
        result1.setRuleCode("VG-NL-01");
        result1.setRuleName("Chất lượng đất và nước");
        result1.setCategory("NGUỒN_LỰC");
        result1.setRuleLevel("MUST");
        result1.setResultStatus("pass");
        result1.setNotes("Đất pH 6.5, Nước tưới đạt chuẩn");
        result1.setCheckedAt(Instant.now());
        result1.setIsAutoEvaluated(true);

        ChecklistRule rule2 = new ChecklistRule(farm, user, "VG-TH-01", "Thời gian cách ly PHI",
                "Tối thiểu 14 ngày", "THU_HOẠCH", "MUST", "ALL", true);
        rule2.setId(102);

        ChecklistResult result2 = new ChecklistResult();
        result2.setId(2);
        result2.setChecklistRun(run);
        result2.setChecklistRule(rule2);
        result2.setCheckedBy(member);
        result2.setRuleCode("VG-TH-01");
        result2.setRuleName("Thời gian cách ly PHI");
        result2.setCategory("THU_HOẠCH");
        result2.setRuleLevel("MUST");
        result2.setResultStatus("pass");
        result2.setNotes("Lần phun cuối cách ngày thu hoạch 18 ngày");
        result2.setCheckedAt(Instant.now());
        result2.setIsAutoEvaluated(true);

        List<ChecklistResultItemResp> items = List.of(
                ChecklistResultItemResp.fromEntity(result1),
                ChecklistResultItemResp.fromEntity(result2)
        );

        ChecklistRunResp runResp = ChecklistRunResp.fromEntity(run, items);

        byte[] pdfBytes = pdfExportService.generateAuditReportPdf(runResp);

        assertNotNull(pdfBytes);
        assertTrue(pdfBytes.length > 500, "File PDF phải có dung lượng lớn hơn 500 bytes");

        // Kiểm tra header signature tiêu chuẩn của file PDF ("%PDF-")
        String header = new String(pdfBytes, 0, 5);
        assertEquals("%PDF-", header);
    }
}
