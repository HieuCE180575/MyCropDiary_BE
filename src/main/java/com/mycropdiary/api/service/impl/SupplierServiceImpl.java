package com.mycropdiary.api.service.impl;

import com.mycropdiary.api.dto.procurement.CreateSupplierReq;
import com.mycropdiary.api.dto.procurement.SupplierResp;
import com.mycropdiary.api.dto.procurement.UpdateSupplierReq;
import com.mycropdiary.api.entity.Farm;
import com.mycropdiary.api.entity.cultivation.Supplier;
import com.mycropdiary.api.exception.BadRequestException;
import com.mycropdiary.api.exception.ResourceConflictException;
import com.mycropdiary.api.exception.ResourceNotFoundException;
import com.mycropdiary.api.repository.FarmRepository;
import com.mycropdiary.api.repository.cultivation.InputPurchaseRepository;
import com.mycropdiary.api.repository.cultivation.SupplierRepository;
import com.mycropdiary.api.service.cultivation.SupplierService;
import com.mycropdiary.api.util.FarmAccessGuard;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class SupplierServiceImpl implements SupplierService {

    private final SupplierRepository supplierRepository;
    private final InputPurchaseRepository inputPurchaseRepository;
    private final FarmRepository farmRepository;
    private final FarmAccessGuard accessGuard;

    public SupplierServiceImpl(SupplierRepository supplierRepository,
                               InputPurchaseRepository inputPurchaseRepository,
                               FarmRepository farmRepository,
                               FarmAccessGuard accessGuard) {
        this.supplierRepository = supplierRepository;
        this.inputPurchaseRepository = inputPurchaseRepository;
        this.farmRepository = farmRepository;
        this.accessGuard = accessGuard;
    }

    @Override
    @Transactional(readOnly = true)
    public List<SupplierResp> getSuppliers(Integer currentUserId, Integer farmId, String keyword) {
        accessGuard.requireFarmAccess(currentUserId, farmId);
        List<Supplier> list = (keyword != null && !keyword.isBlank())
                ? supplierRepository.searchSuppliers(farmId, keyword.trim())
                : supplierRepository.findByFarmIdOrderByCreatedAtDesc(farmId);

        return list.stream().map(s -> {
            Long count = inputPurchaseRepository.countBySupplierId(s.getId());
            return SupplierResp.fromEntity(s, count);
        }).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public SupplierResp getSupplierDetails(Integer currentUserId, Integer supplierId) {
        Supplier supplier = supplierRepository.findById(supplierId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy nhà cung cấp ID: " + supplierId));
        accessGuard.requireFarmAccess(currentUserId, supplier.getFarm().getId());
        Long count = inputPurchaseRepository.countBySupplierId(supplier.getId());
        return SupplierResp.fromEntity(supplier, count);
    }

    @Override
    public SupplierResp createSupplier(Integer currentUserId, Integer farmId, CreateSupplierReq request) {
        accessGuard.requireFarmOwner(currentUserId, farmId);
        Farm farm = farmRepository.findById(farmId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy nông trại ID: " + farmId));

        String trimmedName = request.supplierName().trim();
        if (supplierRepository.existsByFarmIdAndSupplierNameIgnoreCase(farmId, trimmedName)) {
            throw new ResourceConflictException("Nhà cung cấp có tên '" + trimmedName + "' đã tồn tại trong nông trại");
        }

        Supplier supplier = new Supplier(
                farm,
                trimmedName,
                request.contactName() != null ? request.contactName().trim() : null,
                request.phone() != null ? request.phone().trim() : null,
                request.email() != null ? request.email().trim() : null,
                request.address() != null ? request.address().trim() : null,
                request.notes() != null ? request.notes().trim() : null
        );

        Supplier saved = supplierRepository.save(supplier);
        return SupplierResp.fromEntity(saved, 0L);
    }

    @Override
    public SupplierResp updateSupplier(Integer currentUserId, Integer supplierId, UpdateSupplierReq request) {
        Supplier supplier = supplierRepository.findById(supplierId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy nhà cung cấp ID: " + supplierId));
        Integer farmId = supplier.getFarm().getId();
        accessGuard.requireFarmOwner(currentUserId, farmId);

        String trimmedName = request.supplierName().trim();
        if (supplierRepository.existsByFarmIdAndSupplierNameIgnoreCaseAndIdNot(farmId, trimmedName, supplierId)) {
            throw new ResourceConflictException("Nhà cung cấp có tên '" + trimmedName + "' đã tồn tại trong nông trại");
        }

        supplier.setSupplierName(trimmedName);
        if (request.contactName() != null) supplier.setContactName(request.contactName().trim());
        if (request.phone() != null) supplier.setPhone(request.phone().trim());
        if (request.email() != null) supplier.setEmail(request.email().trim());
        if (request.address() != null) supplier.setAddress(request.address().trim());
        if (request.notes() != null) supplier.setNotes(request.notes().trim());

        Supplier updated = supplierRepository.save(supplier);
        Long count = inputPurchaseRepository.countBySupplierId(updated.getId());
        return SupplierResp.fromEntity(updated, count);
    }

    @Override
    public void deleteSupplier(Integer currentUserId, Integer supplierId) {
        Supplier supplier = supplierRepository.findById(supplierId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy nhà cung cấp ID: " + supplierId));
        accessGuard.requireFarmOwner(currentUserId, supplier.getFarm().getId());

        // Business Rule: Delete Eligible Supplier - Chỉ xóa nhà cung cấp hợp lệ (chưa phát sinh hóa đơn)
        if (inputPurchaseRepository.existsBySupplierId(supplierId)) {
            throw new BadRequestException("Nhà cung cấp '" + supplier.getSupplierName()
                    + "' không đủ điều kiện xóa vì đã phát sinh hóa đơn mua vật tư. Để bảo toàn hồ sơ truy xuất VietGAP, không được phép xóa nhà cung cấp này.");
        }

        supplierRepository.delete(supplier);
    }
}
