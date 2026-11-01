package com.mycropdiary.api.service.cultivation;

import com.mycropdiary.api.dto.vietgap.ChecklistResultItemResp;
import com.mycropdiary.api.dto.vietgap.ChecklistRunResp;
import com.mycropdiary.api.dto.vietgap.RunChecklistReq;

import java.util.List;

public interface VietGapChecklistService {

    /**
     * Run VietGAP Evidence Checklist: Kích hoạt / thực thi kiểm tra danh mục bằng chứng VietGAP
     */
    ChecklistRunResp runChecklist(Integer currentUserId, Integer farmId, RunChecklistReq request);

    /**
     * View Checklist Result: Xem kết quả tổng hợp của đợt chạy danh mục kiểm tra
     */
    ChecklistRunResp getChecklistResult(Integer currentUserId, Integer runId);

    /**
     * View Checklist Run History: Xem lịch sử các lượt thực thi danh mục kiểm tra của trang trại
     */
    List<ChecklistRunResp> getChecklistRunHistory(Integer currentUserId, Integer farmId, Integer cropSeasonId, String runStatus);

    /**
     * View Checklist Item Details and Missing Evidence: Xem chi tiết từng mục kiểm tra và cảnh báo thiếu bằng chứng
     */
    List<ChecklistResultItemResp> getChecklistItemDetails(Integer currentUserId, Integer runId);
}

