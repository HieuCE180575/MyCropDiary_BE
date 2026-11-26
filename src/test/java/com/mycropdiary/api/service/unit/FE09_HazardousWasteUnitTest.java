package com.mycropdiary.api.service.unit;

import com.mycropdiary.api.dto.compliance.HazardousWasteReqDTO;
import com.mycropdiary.api.dto.compliance.HazardousWasteRespDTO;
import com.mycropdiary.api.entity.Farm;
import com.mycropdiary.api.entity.compliance.HazardousWasteLog;
import com.mycropdiary.api.exception.BadRequestException;
import com.mycropdiary.api.exception.BusinessRuleException;
import com.mycropdiary.api.exception.ResourceNotFoundException;
import com.mycropdiary.api.mapper.HazardousWasteMapper;
import com.mycropdiary.api.repository.FarmRepository;
import com.mycropdiary.api.repository.compliance.HazardousWasteLogRepository;
import com.mycropdiary.api.service.impl.HazardousWasteServiceImpl;
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
@DisplayName("FE09_WST - HazardousWasteServiceImpl (UC-47: Quản lý Rác thải Nguy hại VietGAP Mục 4.8 / BR-98)")
public class FE09_HazardousWasteUnitTest {

    @Mock private FarmRepository farmRepository;
    @Mock private HazardousWasteLogRepository wasteLogRepository;
    @Mock private FarmAccessGuard accessGuard;
    @Spy  private HazardousWasteMapper mapper;

    private HazardousWasteServiceImpl service;
    private Farm farm;
    private HazardousWasteLog sampleLog;

    @BeforeEach
    void setUp() {
        service = new HazardousWasteServiceImpl(farmRepository, wasteLogRepository, accessGuard, mapper);
        farm = new Farm();
        farm.setId(1);
        farm.setFarmName("Nông trại VietGAP");

        sampleLog = new HazardousWasteLog();
        sampleLog.setId(20);
        sampleLog.setFarm(farm);
        sampleLog.setWasteType("pesticide_bottle");
        sampleLog.setQuantity(new BigDecimal("12.5"));
        sampleLog.setUnit("kg");
        sampleLog.setDisposalMethod("store_holding");
    }

    @Test
    @DisplayName("FE09_WST_UTC01 [Normal] - Ghi nhận bàn giao bao bì thuốc BVTV cho đơn vị xử lý kèm đầy đủ tên đơn vị & số biên bản (BR-98)")
    void UTC01_createWasteLog_transferContractorWithReceipt_succeeds() {
        HazardousWasteReqDTO req = new HazardousWasteReqDTO(
                "pesticide_bottle", new BigDecimal("15.0"), "kg", LocalDate.now(),
                "transfer_contractor", "Công ty Môi trường Đô thị Cần Thơ", "BB-2026-0088",
                "Trần Văn An", "Đã súc rửa và đóng bao riêng"
        );
        when(farmRepository.findById(1)).thenReturn(Optional.of(farm));
        when(wasteLogRepository.save(any(HazardousWasteLog.class))).thenAnswer(i -> {
            HazardousWasteLog l = i.getArgument(0);
            l.setId(21);
            return l;
        });

        HazardousWasteRespDTO resp = service.createWasteLog(10, 1, req);

        assertNotNull(resp);
        assertEquals("pesticide_bottle", resp.wasteType());
        assertEquals("BB-2026-0088", resp.handoverReceiptNo());
        verify(accessGuard).requireFarmAccess(10, 1);
    }

    @Test
    @DisplayName("FE09_WST_UTC02 [Normal] - Ghi nhận lưu kho tạm thời (store_holding) không yêu cầu đơn vị thu gom -> Lưu thành công")
    void UTC02_createWasteLog_storeHoldingWithoutContractor_succeeds() {
        HazardousWasteReqDTO req = new HazardousWasteReqDTO(
                "fertilizer_pack", new BigDecimal("8.0"), "kg", LocalDate.now(),
                "store_holding", null, null, "Lê Thị Bình", "Lưu bể chứa đúng quy định"
        );
        when(farmRepository.findById(1)).thenReturn(Optional.of(farm));
        when(wasteLogRepository.save(any(HazardousWasteLog.class))).thenAnswer(i -> i.getArgument(0));

        HazardousWasteRespDTO resp = service.createWasteLog(10, 1, req);

        assertEquals("store_holding", resp.disposalMethod());
        assertNull(resp.contractorName());
    }

    @Test
    @DisplayName("FE09_WST_UTC03 [Abnormal] - Loại rác thải không nằm trong danh mục cho phép -> Ném BadRequestException")
    void UTC03_createWasteLog_invalidWasteType_throwsBadRequest() {
        HazardousWasteReqDTO req = new HazardousWasteReqDTO(
                "radioactive", new BigDecimal("2.0"), "kg", LocalDate.now(),
                "store_holding", null, null, "An", null
        );
        when(farmRepository.findById(1)).thenReturn(Optional.of(farm));

        assertThrows(BadRequestException.class, () -> service.createWasteLog(10, 1, req));
    }

    @Test
    @DisplayName("FE09_WST_UTC04 [Abnormal] - Phương pháp xử lý rác thải không hợp lệ -> Ném BadRequestException")
    void UTC04_createWasteLog_invalidDisposalMethod_throwsBadRequest() {
        HazardousWasteReqDTO req = new HazardousWasteReqDTO(
                "pesticide_bottle", new BigDecimal("5.0"), "kg", LocalDate.now(),
                "throw_away", null, null, "An", null
        );
        when(farmRepository.findById(1)).thenReturn(Optional.of(farm));

        assertThrows(BadRequestException.class, () -> service.createWasteLog(10, 1, req));
    }

    @Test
    @DisplayName("FE09_WST_UTC05 [Boundary] - Chọn transfer_contractor nhưng bỏ trống tên đơn vị xử lý (BR-98) -> Ném BusinessRuleException")
    void UTC05_createWasteLog_transferContractorMissingName_throwsBR98() {
        HazardousWasteReqDTO req = new HazardousWasteReqDTO(
                "pesticide_bottle", new BigDecimal("5.0"), "kg", LocalDate.now(),
                "transfer_contractor", "   ", "BB-001", "An", null
        );
        when(farmRepository.findById(1)).thenReturn(Optional.of(farm));

        BusinessRuleException ex = assertThrows(BusinessRuleException.class, () -> service.createWasteLog(10, 1, req));
        assertTrue(ex.getMessage().contains("BR-98"));
    }

    @Test
    @DisplayName("FE09_WST_UTC06 [Boundary] - Chọn transfer_contractor nhưng bỏ trống số biên bản bàn giao (BR-98) -> Ném BusinessRuleException")
    void UTC06_createWasteLog_transferContractorMissingReceipt_throwsBR98() {
        HazardousWasteReqDTO req = new HazardousWasteReqDTO(
                "medical", new BigDecimal("1.5"), "kg", LocalDate.now(),
                "transfer_contractor", "Công ty Xử lý Rác", "", "An", null
        );
        when(farmRepository.findById(1)).thenReturn(Optional.of(farm));

        BusinessRuleException ex = assertThrows(BusinessRuleException.class, () -> service.createWasteLog(10, 1, req));
        assertTrue(ex.getMessage().contains("BR-98"));
    }

    @Test
    @DisplayName("FE09_WST_UTC07 [Normal] - Cập nhật nhật ký rác thải hợp lệ -> Lưu khối lượng & ghi chú mới")
    void UTC07_updateWasteLog_validRequest_updatesSuccessfully() {
        HazardousWasteReqDTO req = new HazardousWasteReqDTO(
                "pesticide_bottle", new BigDecimal("20.0"), "kg", LocalDate.now(),
                "incinerate_farm", null, null, "Trần Văn An", "Cập nhật khối lượng"
        );
        when(wasteLogRepository.findById(20)).thenReturn(Optional.of(sampleLog));
        when(wasteLogRepository.save(any(HazardousWasteLog.class))).thenAnswer(i -> i.getArgument(0));

        HazardousWasteRespDTO resp = service.updateWasteLog(10, 20, req);

        assertEquals(new BigDecimal("20.0"), resp.quantity());
        assertEquals("incinerate_farm", resp.disposalMethod());
    }

    @Test
    @DisplayName("FE09_WST_UTC08 [Abnormal] - Xóa nhật ký rác thải không tồn tại ném ResourceNotFound; tồn tại thì xóa sạch")
    void UTC08_deleteWasteLog_notFoundAndExisting() {
        when(wasteLogRepository.findById(999)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.deleteWasteLog(10, 999));

        when(wasteLogRepository.findById(20)).thenReturn(Optional.of(sampleLog));
        service.deleteWasteLog(10, 20);
        verify(wasteLogRepository).delete(sampleLog);
    }
}
