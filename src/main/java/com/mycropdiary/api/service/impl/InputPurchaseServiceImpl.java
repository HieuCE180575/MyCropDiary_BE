package com.mycropdiary.api.service.impl;

import com.mycropdiary.api.dto.procurement.*;
import com.mycropdiary.api.entity.Farm;
import com.mycropdiary.api.entity.FarmMember;
import com.mycropdiary.api.entity.cultivation.InputPurchase;
import com.mycropdiary.api.entity.cultivation.InputPurchaseItem;
import com.mycropdiary.api.entity.cultivation.Material;
import com.mycropdiary.api.entity.cultivation.Supplier;
import com.mycropdiary.api.exception.BadRequestException;
import com.mycropdiary.api.exception.ResourceNotFoundException;
import com.mycropdiary.api.repository.FarmMemberRepository;
import com.mycropdiary.api.repository.FarmRepository;
import com.mycropdiary.api.repository.cultivation.InputPurchaseItemRepository;
import com.mycropdiary.api.repository.cultivation.InputPurchaseRepository;
import com.mycropdiary.api.repository.cultivation.MaterialRepository;
import com.mycropdiary.api.repository.cultivation.MaterialUsageRepository;
import com.mycropdiary.api.repository.cultivation.SupplierRepository;
import com.mycropdiary.api.service.cultivation.InputPurchaseService;
import com.mycropdiary.api.util.FarmAccessGuard;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@Transactional
public class InputPurchaseServiceImpl implements InputPurchaseService {

    private final InputPurchaseRepository inputPurchaseRepository;
    private final InputPurchaseItemRepository inputPurchaseItemRepository;
    private final SupplierRepository supplierRepository;
    private final MaterialRepository materialRepository;
    private final MaterialUsageRepository materialUsageRepository;
    private final FarmRepository farmRepository;
    private final FarmMemberRepository farmMemberRepository;
    private final FarmAccessGuard accessGuard;

    public InputPurchaseServiceImpl(InputPurchaseRepository inputPurchaseRepository,
                                    InputPurchaseItemRepository inputPurchaseItemRepository,
                                    SupplierRepository supplierRepository,
                                    MaterialRepository materialRepository,
                                    MaterialUsageRepository materialUsageRepository,
                                    FarmRepository farmRepository,
                                    FarmMemberRepository farmMemberRepository,
                                    FarmAccessGuard accessGuard) {
        this.inputPurchaseRepository = inputPurchaseRepository;
        this.inputPurchaseItemRepository = inputPurchaseItemRepository;
        this.supplierRepository = supplierRepository;
        this.materialRepository = materialRepository;
        this.materialUsageRepository = materialUsageRepository;
        this.farmRepository = farmRepository;
        this.farmMemberRepository = farmMemberRepository;
        this.accessGuard = accessGuard;
    }

    @Override
    @Transactional(readOnly = true)
    public List<InputPurchaseResp> getInputPurchases(Integer currentUserId, Integer farmId) {
        accessGuard.requireFarmAccess(currentUserId, farmId);
        List<InputPurchase> list = inputPurchaseRepository.findByFarmIdOrderByPurchaseDateDesc(farmId);
        return list.stream().map(p -> {
            List<PurchaseItemResp> items = inputPurchaseItemRepository.findByInputPurchaseIdWithMaterial(p.getId())
                    .stream().map(PurchaseItemResp::fromEntity).toList();
            return InputPurchaseResp.fromEntity(p, items);
        }).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public InputPurchaseResp getInputPurchaseDetails(Integer currentUserId, Integer purchaseId) {
        InputPurchase purchase = inputPurchaseRepository.findById(purchaseId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy phiếu mua vật tư ID: " + purchaseId));
        accessGuard.requireFarmAccess(currentUserId, purchase.getFarm().getId());
        List<PurchaseItemResp> items = inputPurchaseItemRepository.findByInputPurchaseIdWithMaterial(purchase.getId())
                .stream().map(PurchaseItemResp::fromEntity).toList();
        return InputPurchaseResp.fromEntity(purchase, items);
    }

    @Override
    @Transactional(readOnly = true)
    public List<InputPurchaseResp> searchInputPurchases(Integer currentUserId, Integer farmId, String keyword,
                                                        Integer supplierId, LocalDate fromDate, LocalDate toDate) {
        accessGuard.requireFarmAccess(currentUserId, farmId);
        String trimmedKeyword = (keyword != null && !keyword.isBlank()) ? keyword.trim() : null;
        List<InputPurchase> list = inputPurchaseRepository.searchPurchases(farmId, trimmedKeyword, supplierId, fromDate, toDate);
        return list.stream().map(p -> {
            List<PurchaseItemResp> items = inputPurchaseItemRepository.findByInputPurchaseIdWithMaterial(p.getId())
                    .stream().map(PurchaseItemResp::fromEntity).toList();
            return InputPurchaseResp.fromEntity(p, items);
        }).toList();
    }

    @Override
    public InputPurchaseResp recordInputPurchase(Integer currentUserId, Integer farmId, CreateInputPurchaseReq request) {
        accessGuard.requireFarmAccess(currentUserId, farmId);
        Farm farm = farmRepository.findById(farmId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy nông trại ID: " + farmId));

        Supplier supplier = null;
        if (request.supplierId() != null) {
            supplier = supplierRepository.findById(request.supplierId())
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy nhà cung cấp ID: " + request.supplierId()));
            if (!supplier.getFarm().getId().equals(farmId)) {
                throw new BadRequestException("Nhà cung cấp không thuộc nông trại này.");
            }
        }

        FarmMember purchaser = farmMemberRepository.findByFarmIdAndUserIdAndStatus(farmId, currentUserId, "active")
                .orElse(null);

        InputPurchase purchase = new InputPurchase(
                farm,
                supplier,
                purchaser,
                request.purchaseDate() != null ? request.purchaseDate() : LocalDate.now(),
                request.referenceNo() != null ? request.referenceNo().trim() : null,
                BigDecimal.ZERO,
                request.receiptImageUrl(),
                request.notes() != null ? request.notes().trim() : null
        );
        if (request.warehouseReceiptNo() != null) {
            purchase.setWarehouseReceiptNo(request.warehouseReceiptNo().trim());
        }

        InputPurchase savedPurchase = inputPurchaseRepository.save(purchase);

        BigDecimal calculatedTotal = BigDecimal.ZERO;
        List<InputPurchaseItem> savedItems = new ArrayList<>();

        for (PurchaseItemReq itemReq : request.items()) {
            Material material = materialRepository.findById(itemReq.materialId())
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy vật tư ID: " + itemReq.materialId()));
            if (!material.getFarm().getId().equals(farmId)) {
                throw new BadRequestException("Vật tư '" + material.getMaterialName() + "' không thuộc nông trại này.");
            }

            InputPurchaseItem item = new InputPurchaseItem(
                    savedPurchase,
                    material,
                    itemReq.warehouseId(),
                    itemReq.quantity(),
                    itemReq.unitCost()
            );
            InputPurchaseItem persistedItem = inputPurchaseItemRepository.save(item);
            savedItems.add(persistedItem);

            calculatedTotal = calculatedTotal.add(itemReq.quantity().multiply(itemReq.unitCost()));
        }

        savedPurchase.setTotalAmount(calculatedTotal);
        savedPurchase = inputPurchaseRepository.save(savedPurchase);

        List<PurchaseItemResp> itemResps = savedItems.stream().map(PurchaseItemResp::fromEntity).toList();
        return InputPurchaseResp.fromEntity(savedPurchase, itemResps);
    }

    @Override
    public InputPurchaseResp updateInputPurchase(Integer currentUserId, Integer purchaseId, UpdateInputPurchaseReq request) {
        InputPurchase purchase = inputPurchaseRepository.findById(purchaseId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy phiếu mua vật tư ID: " + purchaseId));
        Integer farmId = purchase.getFarm().getId();
        accessGuard.requireFarmOwner(currentUserId, farmId);

        if (request.supplierId() != null) {
            Supplier supplier = supplierRepository.findById(request.supplierId())
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy nhà cung cấp ID: " + request.supplierId()));
            if (!supplier.getFarm().getId().equals(farmId)) {
                throw new BadRequestException("Nhà cung cấp không thuộc nông trại này.");
            }
            purchase.setSupplier(supplier);
        }

        if (request.purchaseDate() != null) purchase.setPurchaseDate(request.purchaseDate());
        if (request.referenceNo() != null) purchase.setReferenceNo(request.referenceNo().trim());
        if (request.receiptImageUrl() != null) purchase.setReceiptImageUrl(request.receiptImageUrl());
        if (request.warehouseReceiptNo() != null) purchase.setWarehouseReceiptNo(request.warehouseReceiptNo().trim());
        if (request.notes() != null) purchase.setNotes(request.notes().trim());

        if (request.items() != null && !request.items().isEmpty()) {
            inputPurchaseItemRepository.deleteByInputPurchaseId(purchaseId);
            BigDecimal newTotal = BigDecimal.ZERO;
            List<InputPurchaseItem> newItems = new ArrayList<>();

            for (PurchaseItemReq itemReq : request.items()) {
                Material material = materialRepository.findById(itemReq.materialId())
                        .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy vật tư ID: " + itemReq.materialId()));

                InputPurchaseItem item = new InputPurchaseItem(
                        purchase,
                        material,
                        itemReq.warehouseId(),
                        itemReq.quantity(),
                        itemReq.unitCost()
                );
                newItems.add(inputPurchaseItemRepository.save(item));
                newTotal = newTotal.add(itemReq.quantity().multiply(itemReq.unitCost()));
            }
            purchase.setTotalAmount(newTotal);
        }

        InputPurchase updated = inputPurchaseRepository.save(purchase);
        List<PurchaseItemResp> items = inputPurchaseItemRepository.findByInputPurchaseIdWithMaterial(updated.getId())
                .stream().map(PurchaseItemResp::fromEntity).toList();
        return InputPurchaseResp.fromEntity(updated, items);
    }

    @Override
    public void deleteInputPurchase(Integer currentUserId, Integer purchaseId) {
        InputPurchase purchase = inputPurchaseRepository.findById(purchaseId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy phiếu mua vật tư ID: " + purchaseId));
        accessGuard.requireFarmOwner(currentUserId, purchase.getFarm().getId());

        // Business Rule: Delete Eligible Input Purchase - Kiểm tra xem vật tư trong phiếu mua đã xuất dùng chưa
        List<InputPurchaseItem> items = inputPurchaseItemRepository.findByInputPurchaseIdWithMaterial(purchaseId);
        for (InputPurchaseItem item : items) {
            boolean isUsed = materialUsageRepository.existsByMaterialId(item.getMaterial().getId());
            if (isUsed) {
                throw new BadRequestException("Phiếu mua vật tư '" + (purchase.getReferenceNo() != null ? purchase.getReferenceNo() : "#" + purchaseId)
                        + "' không đủ điều kiện xóa vì vật tư '" + item.getMaterial().getMaterialName()
                        + "' đã được ghi nhận xuất dùng trong nhật ký canh tác (Material Usage). Nhằm bảo toàn nhật ký VietGAP, không được phép xóa phiếu này.");
            }
        }

        inputPurchaseItemRepository.deleteByInputPurchaseId(purchaseId);
        inputPurchaseRepository.delete(purchase);
    }

    @Override
    public InputPurchaseResp linkWarehouseReceipt(Integer currentUserId, Integer purchaseId, LinkWarehouseReceiptReq request) {
        InputPurchase purchase = inputPurchaseRepository.findById(purchaseId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy phiếu mua vật tư ID: " + purchaseId));
        accessGuard.requireFarmAccess(currentUserId, purchase.getFarm().getId());

        purchase.setWarehouseReceiptNo(request.warehouseReceiptNo().trim());
        if (request.notes() != null && !request.notes().isBlank()) {
            String combinedNotes = (purchase.getNotes() != null ? purchase.getNotes() + "; " : "") + "[Nhập kho]: " + request.notes().trim();
            purchase.setNotes(combinedNotes);
        }

        InputPurchase updated = inputPurchaseRepository.save(purchase);
        List<PurchaseItemResp> items = inputPurchaseItemRepository.findByInputPurchaseIdWithMaterial(updated.getId())
                .stream().map(PurchaseItemResp::fromEntity).toList();
        return InputPurchaseResp.fromEntity(updated, items);
    }
}

