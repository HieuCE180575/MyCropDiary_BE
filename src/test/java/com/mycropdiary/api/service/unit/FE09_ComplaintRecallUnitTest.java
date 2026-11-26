package com.mycropdiary.api.service.unit;

import com.mycropdiary.api.dto.compliance.CreateComplaintReqDTO;
import com.mycropdiary.api.dto.compliance.CustomerComplaintRespDTO;
import com.mycropdiary.api.dto.compliance.RecallProductReqDTO;
import com.mycropdiary.api.dto.compliance.ResolveComplaintReqDTO;
import com.mycropdiary.api.entity.Farm;
import com.mycropdiary.api.entity.compliance.CustomerComplaintLog;
import com.mycropdiary.api.entity.cultivation.HarvestRecord;
import com.mycropdiary.api.exception.BadRequestException;
import com.mycropdiary.api.exception.BusinessRuleException;
import com.mycropdiary.api.exception.ResourceNotFoundException;
import com.mycropdiary.api.mapper.CustomerComplaintMapper;
import com.mycropdiary.api.repository.FarmRepository;
import com.mycropdiary.api.repository.compliance.CustomerComplaintLogRepository;
import com.mycropdiary.api.repository.cultivation.HarvestRecordRepository;
import com.mycropdiary.api.service.impl.CustomerComplaintServiceImpl;
import com.mycropdiary.api.util.FarmAccessGuard;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("FE09_CMP - CustomerComplaintServiceImpl (UC-48: Khiếu nại Khách hàng & Thu hồi Sản phẩm VietGAP Mục 4.10 / BR-100)")
public class FE09_ComplaintRecallUnitTest {

    @Mock private FarmRepository farmRepository;
    @Mock private HarvestRecordRepository harvestRecordRepository;
    @Mock private CustomerComplaintLogRepository complaintRepository;
    @Mock private FarmAccessGuard accessGuard;
    @Spy  private CustomerComplaintMapper mapper;

    private CustomerComplaintServiceImpl service;
    private Farm farm;
    private Farm otherFarm;
    private HarvestRecord harvestRecord;
    private CustomerComplaintLog complaint;

    @BeforeEach
    void setUp() {
        service = new CustomerComplaintServiceImpl(farmRepository, harvestRecordRepository, complaintRepository, accessGuard, mapper);

        farm = new Farm();
        farm.setId(1);
        farm.setFarmName("Nông trại VietGAP");

        otherFarm = new Farm();
        otherFarm.setId(2);

        harvestRecord = new HarvestRecord();
        harvestRecord.setId(300);
        harvestRecord.setFarm(farm);
        harvestRecord.setHarvestLotCode("GAP-VN-202611-0001");
        harvestRecord.setQuantity(new BigDecimal("500.00"));
        harvestRecord.setUnit("kg");

        complaint = new CustomerComplaintLog();
        complaint.setId(40);
        complaint.setFarm(farm);
        complaint.setHarvestRecord(harvestRecord);
        complaint.setLotCode("GAP-VN-202611-0001");
        complaint.setCustomerName("Siêu thị Co.opmart");
        complaint.setIssueType("chemical_residue_suspect");
        complaint.setSeverity("critical");
        complaint.setStatus("received");
        complaint.setIsRecallRequired(false);
    }

    @Test
    @DisplayName("FE09_CMP_UTC01 [Normal] - Ghi nhận khiếu nại gắn với lô thu hoạch của trang trại -> Tự động điền mã lô & trạng thái received")
    void UTC01_createComplaint_linkedToHarvestLot_autoResolvesLotCode() {
        CreateComplaintReqDTO req = new CreateComplaintReqDTO(
                300, null, "Siêu thị Co.opmart", "0909123456", LocalDate.now(),
                "quality_defect", "Một số trái dập nát khi vận chuyển", "medium"
        );
        when(farmRepository.findById(1)).thenReturn(Optional.of(farm));
        when(harvestRecordRepository.findById(300)).thenReturn(Optional.of(harvestRecord));
        when(complaintRepository.save(any(CustomerComplaintLog.class))).thenAnswer(i -> {
            CustomerComplaintLog l = i.getArgument(0);
            l.setId(41);
            return l;
        });

        CustomerComplaintRespDTO resp = service.createComplaint(10, 1, req);

        assertNotNull(resp);
        assertEquals("LOT-000300", resp.lotCode());
        assertEquals("received", resp.status());
        assertEquals(Boolean.FALSE, resp.isRecallRequired());
        verify(accessGuard).requireFarmAccess(10, 1);
    }

    @Test
    @DisplayName("FE09_CMP_UTC02 [Abnormal] - Phân loại sự cố khiếu nại không hợp lệ -> Ném BadRequestException")
    void UTC02_createComplaint_invalidIssueType_throwsBadRequest() {
        CreateComplaintReqDTO req = new CreateComplaintReqDTO(
                null, null, "Khách A", null, LocalDate.now(),
                "invalid_issue", "Mô tả", "low"
        );
        when(farmRepository.findById(1)).thenReturn(Optional.of(farm));

        assertThrows(BadRequestException.class, () -> service.createComplaint(10, 1, req));
    }

    @Test
    @DisplayName("FE09_CMP_UTC03 [Abnormal] - Mức độ nghiêm trọng không hợp lệ -> Ném BadRequestException")
    void UTC03_createComplaint_invalidSeverity_throwsBadRequest() {
        CreateComplaintReqDTO req = new CreateComplaintReqDTO(
                null, null, "Khách A", null, LocalDate.now(),
                "packaging_damage", "Mô tả", "extreme"
        );
        when(farmRepository.findById(1)).thenReturn(Optional.of(farm));

        assertThrows(BadRequestException.class, () -> service.createComplaint(10, 1, req));
    }

    @Test
    @DisplayName("FE09_CMP_UTC04 [Abnormal] - Chọn lô thu hoạch thuộc trang trại khác (Cross-Farm) -> Ném BusinessRuleException")
    void UTC04_createComplaint_harvestFromOtherFarm_throwsBusinessRule() {
        HarvestRecord foreignHr = new HarvestRecord();
        foreignHr.setId(999);
        foreignHr.setFarm(otherFarm);

        CreateComplaintReqDTO req = new CreateComplaintReqDTO(
                999, null, "Khách B", null, LocalDate.now(),
                "quality_defect", "Hư hỏng", "high".replace("high", "medium")
        );
        when(farmRepository.findById(1)).thenReturn(Optional.of(farm));
        when(harvestRecordRepository.findById(999)).thenReturn(Optional.of(foreignHr));

        assertThrows(BusinessRuleException.class, () -> service.createComplaint(10, 1, req));
    }

    @Test
    @DisplayName("FE09_CMP_UTC05 [Normal] - Owner xử lý khiếu nại chuyển trạng thái resolved kèm ghi chú khắc phục (UC-48.4)")
    void UTC05_resolveComplaint_validStatus_updatesSuccessfully() {
        ResolveComplaintReqDTO req = new ResolveComplaintReqDTO("resolved", "Đã đổi lô hàng đạt chuẩn và hoàn tiền chênh lệch.");
        when(complaintRepository.findById(40)).thenReturn(Optional.of(complaint));
        when(complaintRepository.save(any(CustomerComplaintLog.class))).thenAnswer(i -> i.getArgument(0));

        CustomerComplaintRespDTO resp = service.resolveComplaint(100, 40, req);

        assertEquals("resolved", resp.status());
        assertEquals("Đã đổi lô hàng đạt chuẩn và hoàn tiền chênh lệch.", resp.resolutionNotes());
        verify(accessGuard).requireFarmOwner(100, 1);
    }

    @Test
    @DisplayName("FE09_CMP_UTC06 [Abnormal] - Cập nhật trạng thái giải quyết không hợp lệ -> Ném BadRequestException")
    void UTC06_resolveComplaint_invalidStatus_throwsBadRequest() {
        ResolveComplaintReqDTO req = new ResolveComplaintReqDTO("closed", "Đóng hồ sơ");
        when(complaintRepository.findById(40)).thenReturn(Optional.of(complaint));

        assertThrows(BadRequestException.class, () -> service.resolveComplaint(100, 40, req));
    }

    @Test
    @DisplayName("FE09_CMP_UTC07 [Normal] - Owner phát lệnh thu hồi sản phẩm với khối lượng hợp lệ (<= sản lượng lô) -> Chuyển trạng thái recalled (UC-48.5)")
    void UTC07_recordProductRecall_validQuantity_setsRecalled() {
        RecallProductReqDTO req = new RecallProductReqDTO(new BigDecimal("150.00"), "Thu hồi 150 kg nghi ngờ tồn dư thuốc BVTV");
        when(complaintRepository.findById(40)).thenReturn(Optional.of(complaint));
        when(complaintRepository.save(any(CustomerComplaintLog.class))).thenAnswer(i -> i.getArgument(0));

        CustomerComplaintRespDTO resp = service.recordProductRecall(100, 40, req);

        assertEquals("recalled", resp.status());
        assertEquals(Boolean.TRUE, resp.isRecallRequired());
        assertEquals(new BigDecimal("150.00"), resp.recalledQuantity());
        verify(accessGuard).requireFarmOwner(100, 1);
    }

    @Test
    @DisplayName("FE09_CMP_UTC08 [Boundary] - Phát lệnh thu hồi đúng bằng 100% sản lượng thu hoạch (500 kg / 500 kg) -> Hợp lệ theo BR-100")
    void UTC08_recordProductRecall_exactMaxQuantity_succeeds() {
        RecallProductReqDTO req = new RecallProductReqDTO(new BigDecimal("500.00"), "Thu hồi toàn bộ lô GAP-VN-202611-0001");
        when(complaintRepository.findById(40)).thenReturn(Optional.of(complaint));
        when(complaintRepository.save(any(CustomerComplaintLog.class))).thenAnswer(i -> i.getArgument(0));

        CustomerComplaintRespDTO resp = service.recordProductRecall(100, 40, req);

        assertEquals("recalled", resp.status());
        assertEquals(new BigDecimal("500.00"), resp.recalledQuantity());
    }

    @Test
    @DisplayName("FE09_CMP_UTC09 [Boundary] - Số lượng thu hồi vượt sản lượng lô (500.01 kg > 500 kg) -> Chặn vi phạm BR-100")
    void UTC09_recordProductRecall_exceedsHarvestQuantity_throwsBR100() {
        RecallProductReqDTO req = new RecallProductReqDTO(new BigDecimal("500.01"), "Vượt sản lượng lô");
        when(complaintRepository.findById(40)).thenReturn(Optional.of(complaint));

        BusinessRuleException ex = assertThrows(BusinessRuleException.class, () -> service.recordProductRecall(100, 40, req));
        assertTrue(ex.getMessage().contains("BR-100"));
        verify(complaintRepository, never()).save(any());
    }

    @Test
    @DisplayName("FE09_CMP_UTC10 [Normal] - Xóa hồ sơ khiếu nại yêu cầu quyền Owner; ID không tồn tại ném ResourceNotFoundException")
    void UTC10_deleteComplaint_ownerDeletesAndNotFoundThrows() {
        when(complaintRepository.findById(999)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.deleteComplaint(100, 999));

        when(complaintRepository.findById(40)).thenReturn(Optional.of(complaint));
        service.deleteComplaint(100, 40);
        verify(accessGuard).requireFarmOwner(100, 1);
        verify(complaintRepository).delete(complaint);
    }
}
