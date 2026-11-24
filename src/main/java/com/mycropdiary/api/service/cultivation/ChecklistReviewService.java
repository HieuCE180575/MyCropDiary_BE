package com.mycropdiary.api.service.cultivation;

import com.mycropdiary.api.dto.vietgap.*;

import java.util.List;

public interface ChecklistReviewService {

    /**
     * Khởi tạo đợt tự đánh giá VietGAP toàn diện (33 tiêu chí) và tự động quét dữ liệu
     */
    ChecklistResultRespDTO initVietGapAuditRun(Integer currentUserId, Integer farmId, InitVietGapAuditRunReqDTO request);

    /**
     * Run VietGAP Evidence Checklist: Kích hoạt / thực thi kiểm tra danh mục bằng chứng VietGAP
     */
    ChecklistResultRespDTO runChecklist(Integer currentUserId, Integer farmId, RunChecklistReqDTO request);

    /**
     * Cập nhật kết quả, ghi chú và ảnh minh chứng cho một tiêu chí kiểm tra hiện trường
     */
    ChecklistResultItemRespDTO updateChecklistItem(Integer currentUserId, Integer farmId, Integer runId, Integer resultId, UpdateChecklistItemReqDTO request);

    /**
     * Chốt và hoàn tất đợt kiểm tra: tính toán % tuân thủ và khóa bất biến (Snapshot)
     */
    ChecklistResultRespDTO completeChecklistRun(Integer currentUserId, Integer farmId, Integer runId);

    /**
     * Chạy lại thuật toán đối soát tự động từ dữ liệu nhật ký, đất, nước, thu hoạch
     */
    ChecklistResultRespDTO autoCheckRun(Integer currentUserId, Integer farmId, Integer runId);

    /**
     * Xuất Báo cáo tự kiểm tra nội bộ VietGAP khổ A4 in ấn (PDF)
     */
    byte[] exportAuditReportPdf(Integer currentUserId, Integer farmId, Integer runId);

    /**
     * View Checklist Result: Xem kết quả tổng hợp của đợt chạy danh mục kiểm tra
     */
    ChecklistResultRespDTO getChecklistResult(Integer currentUserId, Integer runId);

    /**
     * View Checklist Run History: Xem lịch sử các lượt thực thi danh mục kiểm tra của trang trại
     */
    List<ChecklistResultRespDTO> getChecklistRunHistory(Integer currentUserId, Integer farmId, Integer cropSeasonId, String runStatus);

    /**
     * View Checklist Item Details and Missing Evidence: Xem chi tiết từng mục kiểm tra và cảnh báo thiếu bằng chứng
     */
    List<ChecklistResultItemRespDTO> getChecklistItemDetails(Integer currentUserId, Integer runId);
}
