package com.mycropdiary.api.service.compliance;

import com.mycropdiary.api.dto.compliance.CreateComplaintReqDTO;
import com.mycropdiary.api.dto.compliance.CustomerComplaintRespDTO;
import com.mycropdiary.api.dto.compliance.RecallProductReqDTO;
import com.mycropdiary.api.dto.compliance.ResolveComplaintReqDTO;
import java.util.List;

/**
 * UC-48: Manage Customer Complaints & Product Recall (Mục 4.10 VietGAP, BR-100).
 */
public interface CustomerComplaintService {
    // Staff + Owner
    List<CustomerComplaintRespDTO> getComplaints(Integer currentUserId, Integer farmId);
    CustomerComplaintRespDTO createComplaint(Integer currentUserId, Integer farmId, CreateComplaintReqDTO request);

    // Owner-only: UC-48.4 Resolve Complaint
    CustomerComplaintRespDTO resolveComplaint(Integer currentUserId, Integer complaintId, ResolveComplaintReqDTO request);

    // Owner-only: UC-48.5 Record Product Recall (BR-100: recalledQuantity <= harvestedQuantity)
    CustomerComplaintRespDTO recordProductRecall(Integer currentUserId, Integer complaintId, RecallProductReqDTO request);

    // Owner-only: Delete complaint log
    void deleteComplaint(Integer currentUserId, Integer complaintId);
}
