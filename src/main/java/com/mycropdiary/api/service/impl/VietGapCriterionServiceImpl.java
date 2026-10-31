package com.mycropdiary.api.service.impl;

import com.mycropdiary.api.dto.vietgap.CreateVietGapCriterionReq;
import com.mycropdiary.api.dto.vietgap.UpdateVietGapCriterionReq;
import com.mycropdiary.api.dto.vietgap.VietGapCriterionResp;
import com.mycropdiary.api.entity.Farm;
import com.mycropdiary.api.entity.cultivation.AssessmentCriterion;
import com.mycropdiary.api.exception.BadRequestException;
import com.mycropdiary.api.exception.ResourceNotFoundException;
import com.mycropdiary.api.repository.FarmRepository;
import com.mycropdiary.api.repository.cultivation.AssessmentCriterionRepository;
import com.mycropdiary.api.service.cultivation.VietGapCriterionService;
import com.mycropdiary.api.util.FarmAccessGuard;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
@Transactional
public class VietGapCriterionServiceImpl implements VietGapCriterionService {

    private final AssessmentCriterionRepository criterionRepository;
    private final FarmRepository farmRepository;
    private final FarmAccessGuard accessGuard;

    public VietGapCriterionServiceImpl(
            AssessmentCriterionRepository criterionRepository,
            FarmRepository farmRepository,
            FarmAccessGuard accessGuard) {
        this.criterionRepository = criterionRepository;
        this.farmRepository = farmRepository;
        this.accessGuard = accessGuard;
    }

    @Override
    @Transactional(readOnly = true)
    public List<VietGapCriterionResp> getCriterionList(Integer currentUserId, Integer farmId, Boolean isActiveOnly, String keyword) {
        accessGuard.requireFarmAccess(currentUserId, farmId);

        List<AssessmentCriterion> list;
        if (Boolean.TRUE.equals(isActiveOnly)) {
            list = criterionRepository.findByFarmIdAndIsActiveOrderByCriterionNameAsc(farmId, true);
        } else {
            list = criterionRepository.findByFarmIdOrderByCriterionNameAsc(farmId);
        }

        if (keyword != null && !keyword.trim().isEmpty()) {
            String lower = keyword.trim().toLowerCase();
            list = list.stream().filter(c ->
                    (c.getCriterionName() != null && c.getCriterionName().toLowerCase().contains(lower)) ||
                    (c.getDescription() != null && c.getDescription().toLowerCase().contains(lower))
            ).toList();
        }

        return list.stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public VietGapCriterionResp getCriterionDetails(Integer currentUserId, Integer criterionId) {
        AssessmentCriterion criterion = criterionRepository.findById(criterionId)
                .orElseThrow(() -> new ResourceNotFoundException("Tiêu chuẩn đánh giá không tồn tại"));

        accessGuard.requireFarmAccess(currentUserId, criterion.getFarm().getId());
        return toResponse(criterion);
    }

    @Override
    public VietGapCriterionResp createCriterion(Integer currentUserId, Integer farmId, CreateVietGapCriterionReq request) {
        accessGuard.requireFarmOwner(currentUserId, farmId);

        Farm farm = farmRepository.findById(farmId)
                .orElseThrow(() -> new ResourceNotFoundException("Trang trại không tồn tại"));

        String name = request.criterionName().trim();
        if (criterionRepository.existsByFarmIdAndCriterionName(farmId, name)) {
            throw new BadRequestException("Tiêu chuẩn đánh giá '" + name + "' đã tồn tại trong trang trại");
        }

        String fullDescription = combineDescriptionAndMethod(request.description(), request.evaluationMethod());
        BigDecimal maxScore = request.maxScore() != null ? request.maxScore() : new BigDecimal("100.00");
        boolean isActive = request.isActive() != null ? request.isActive() : true;

        AssessmentCriterion criterion = new AssessmentCriterion(farm, name, fullDescription, maxScore, isActive);
        AssessmentCriterion saved = criterionRepository.save(criterion);

        return toResponse(saved);
    }

    @Override
    public List<VietGapCriterionResp> importTcvnStandardCriteria(Integer currentUserId, Integer farmId) {
        accessGuard.requireFarmOwner(currentUserId, farmId);

        Farm farm = farmRepository.findById(farmId)
                .orElseThrow(() -> new ResourceNotFoundException("Trang trại không tồn tại"));

        List<StandardCriterionDef> defaults = List.of(
                new StandardCriterionDef(
                        "TCVN 11892-1:2017: Đánh giá vị trí và mối nguy khu vực sản xuất",
                        "Khu vực sản xuất không bị ô nhiễm bởi khói bụi, hóa chất nguy hại từ các nhà máy công nghiệp hoặc bãi rác. Có vùng đệm cách ly an toàn.",
                        "Kiểm tra thực địa định kỳ, xem xét bản đồ quy hoạch và kết quả phân tích mối nguy ban đầu."
                ),
                new StandardCriterionDef(
                        "TCVN 11892-1:2017: Quản lý đất, giá thể và kiểm soát kim loại nặng",
                        "Đất và giá thể gieo trồng không chứa dư lượng kim loại nặng (As, Pb, Cd, Hg) vượt ngưỡng giới hạn cho phép theo quy chuẩn hiện hành.",
                        "Lấy mẫu xét nghiệm thổ nhưỡng định kỳ tại các phòng kiểm nghiệm được chỉ định, lưu giữ phiếu kết quả phân tích."
                ),
                new StandardCriterionDef(
                        "TCVN 11892-1:2017: Quản lý nguồn nước tưới và nước sơ chế",
                        "Nguồn nước tưới đáp ứng QCVN 39:2011/BTNMT, nước rửa sơ chế đáp ứng quy chuẩn nước sinh hoạt QCVN 01-1:2018/BYT.",
                        "Kiểm tra cảm quan nguồn nước hàng ngày, định kỳ phân tích vi sinh (E. coli, Coliforms) và hóa lý."
                ),
                new StandardCriterionDef(
                        "TCVN 11892-1:2017: Nguồn gốc hạt giống và vật liệu nhân giống",
                        "Giống cây trồng có xuất xứ rõ ràng, có hóa đơn chứng từ hợp pháp, hạt giống sạch sâu bệnh và xử lý đúng quy chuẩn.",
                        "Đối chiếu hóa đơn mua hàng, phiếu kiểm định chất lượng giống và nhật ký ươm trồng tại vườn ươm."
                ),
                new StandardCriterionDef(
                        "TCVN 11892-1:2017: Quản lý phân bón và chất bổ sung dinh dưỡng",
                        "Chỉ sử dụng phân bón trong Danh mục được phép lưu hành tại Việt Nam. Không dùng phân tươi chưa qua ủ hoai mục.",
                        "Kiểm tra kho lưu trữ phân bón, đối chiếu định mức bón với nhật ký bón phân thực tế trên lô đất."
                ),
                new StandardCriterionDef(
                        "TCVN 11892-1:2017: Quản lý thuốc BVTV và tuân thủ thời gian cách ly (PHI)",
                        "Áp dụng nguyên tắc 4 đúng (đúng thuốc, đúng liều, đúng lúc, đúng cách). Tuyệt đối tuân thủ thời gian cách ly (PHI) trước khi thu hoạch.",
                        "Kiểm tra tủ thuốc BVTV có khóa, sổ theo dõi xuất nhập kho thuốc và nhật ký phun xịt đối chiếu ngày thu hoạch."
                ),
                new StandardCriterionDef(
                        "TCVN 11892-1:2017: Quy trình thu hoạch, sơ chế và đóng gói",
                        "Dụng cụ thu hoạch sạch sẽ, nông sản thu hoạch không tiếp xúc trực tiếp với mặt đất. Bao bì ghi nhãn đầy đủ thông tin truy xuất.",
                        "Giám sát thao tác thu hái của công nhân, kiểm tra vệ sinh khu vực sơ chế và kho mát bảo quản."
                ),
                new StandardCriterionDef(
                        "TCVN 11892-1:2017: Quản lý chất thải, vỏ bao gói thuốc BVTV",
                        "Vỏ bao gói phân bón và thuốc BVTV sau sử dụng phải thu gom vào bể chứa chuyên dụng có nắp đậy, không vứt bỏ bừa bãi ra đồng ruộng.",
                        "Kiểm tra số lượng và hiện trạng các bể chứa bao gói thuốc BVTV trên toàn bộ các khu vực canh tác."
                ),
                new StandardCriterionDef(
                        "TCVN 11892-1:2017: An toàn lao động, trang bị bảo hộ (PPE) và tập huấn",
                        "Người lao động được trang bị đầy đủ bảo hộ (khẩu trang, găng tay, ủng, kính) khi tiếp xúc hóa chất và được đào tạo định kỳ.",
                        "Kiểm tra cấp phát trang thiết bị bảo hộ, hồ sơ tập huấn kỹ thuật và tủ thuốc sơ cấp cứu tại chỗ."
                ),
                new StandardCriterionDef(
                        "TCVN 11892-1:2017: Ghi chép nhật ký điện tử và lưu trữ hồ sơ truy xuất",
                        "Toàn bộ các hoạt động canh tác, vật tư sử dụng, thu hoạch và xuất bán được ghi chép kịp thời, đầy đủ trong hệ thống MyCropDiary.",
                        "Truy vấn đối chiếu dữ liệu nhật ký canh tác số hóa, kiểm tra mã lô sản phẩm (Lot Code) khi xuất xưởng."
                )
        );

        List<AssessmentCriterion> savedList = new ArrayList<>();
        for (StandardCriterionDef def : defaults) {
            if (!criterionRepository.existsByFarmIdAndCriterionName(farmId, def.title())) {
                String fullDesc = def.desc() + "\n[Phương pháp đánh giá]: " + def.method();
                AssessmentCriterion c = new AssessmentCriterion(farm, def.title(), fullDesc, new BigDecimal("100.00"), true);
                savedList.add(criterionRepository.save(c));
            }
        }

        return savedList.stream().map(this::toResponse).toList();
    }

    @Override
    public VietGapCriterionResp updateCriterion(Integer currentUserId, Integer criterionId, UpdateVietGapCriterionReq request) {
        AssessmentCriterion criterion = criterionRepository.findById(criterionId)
                .orElseThrow(() -> new ResourceNotFoundException("Tiêu chuẩn đánh giá không tồn tại"));

        accessGuard.requireFarmOwner(currentUserId, criterion.getFarm().getId());

        if (request.criterionName() != null && !request.criterionName().trim().isEmpty()) {
            String newName = request.criterionName().trim();
            if (!newName.equalsIgnoreCase(criterion.getCriterionName()) &&
                    criterionRepository.existsByFarmIdAndCriterionName(criterion.getFarm().getId(), newName)) {
                throw new BadRequestException("Tiêu chuẩn '" + newName + "' đã tồn tại trong trang trại");
            }
            criterion.setCriterionName(newName);
        }

        if (request.description() != null || request.evaluationMethod() != null) {
            String updatedDesc = combineDescriptionAndMethod(
                    request.description() != null ? request.description() : extractBaseDescription(criterion.getDescription()),
                    request.evaluationMethod() != null ? request.evaluationMethod() : extractEvaluationMethod(criterion.getDescription())
            );
            criterion.setDescription(updatedDesc);
        }

        if (request.maxScore() != null) {
            criterion.setMaxScore(request.maxScore());
        }

        if (request.isActive() != null) {
            criterion.setIsActive(request.isActive());
        }

        AssessmentCriterion saved = criterionRepository.save(criterion);
        return toResponse(saved);
    }

    @Override
    public void deleteCriterion(Integer currentUserId, Integer criterionId) {
        AssessmentCriterion criterion = criterionRepository.findById(criterionId)
                .orElseThrow(() -> new ResourceNotFoundException("Tiêu chuẩn đánh giá không tồn tại"));

        accessGuard.requireFarmOwner(currentUserId, criterion.getFarm().getId());
        criterionRepository.delete(criterion);
    }

    private String combineDescriptionAndMethod(String desc, String method) {
        StringBuilder sb = new StringBuilder();
        if (desc != null && !desc.trim().isEmpty()) {
            sb.append(desc.trim());
        }
        if (method != null && !method.trim().isEmpty()) {
            if (sb.length() > 0) sb.append("\n[Phương pháp đánh giá]: ");
            else sb.append("[Phương pháp đánh giá]: ");
            sb.append(method.trim());
        }
        return sb.length() > 0 ? sb.toString() : null;
    }

    private String extractBaseDescription(String full) {
        if (full == null) return null;
        int idx = full.indexOf("[Phương pháp đánh giá]:");
        return idx >= 0 ? full.substring(0, idx).trim() : full.trim();
    }

    private String extractEvaluationMethod(String full) {
        if (full == null) return null;
        int idx = full.indexOf("[Phương pháp đánh giá]:");
        return idx >= 0 ? full.substring(idx + "[Phương pháp đánh giá]:".length()).trim() : null;
    }

    private VietGapCriterionResp toResponse(AssessmentCriterion c) {
        String baseDesc = extractBaseDescription(c.getDescription());
        String method = extractEvaluationMethod(c.getDescription());
        return new VietGapCriterionResp(
                c.getId(),
                c.getFarm().getId(),
                c.getFarm().getFarmName(),
                c.getCriterionName(),
                baseDesc,
                c.getMaxScore(),
                method,
                "TCVN 11892-1:2017",
                c.getIsActive()
        );
    }

    private record StandardCriterionDef(String title, String desc, String method) {
    }
}

