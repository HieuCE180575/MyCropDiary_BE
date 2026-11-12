package com.mycropdiary.api.config;

import com.mycropdiary.api.entity.cultivation.ChecklistRule;
import com.mycropdiary.api.repository.cultivation.ChecklistRuleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Khởi tạo tự động 33 tiêu chí Master VietGAP Trồng trọt cho cây ngắn ngày & rau màu (TCVN 11892-1).
 * Các tiêu chí này thuộc quyền quản trị của Hệ thống (farm_id = NULL) và dùng chung cho mọi trang trại.
 */
@Component
@Order(10)
public class VietGapMasterChecklistDataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(VietGapMasterChecklistDataInitializer.class);

    private final ChecklistRuleRepository checklistRuleRepository;

    public VietGapMasterChecklistDataInitializer(ChecklistRuleRepository checklistRuleRepository) {
        this.checklistRuleRepository = checklistRuleRepository;
    }

    private record MasterRuleSeed(
            String ruleCode,
            String ruleName,
            String description,
            String category,
            String ruleLevel,
            String targetCropType
    ) {}

    @Override
    public void run(String... args) {
        try {
            List<MasterRuleSeed> masterSeeds = List.of(
                    // PHẦN 1 – HỒ SƠ VÀ TẬP HUẤN (RECORDS_AND_TRAINING)
                    new MasterRuleSeed("VG-HS-01",
                            "Người quản lý được tập huấn VietGAP hoặc có xác nhận ATTP",
                            "Người quản lý hoặc phụ trách kỹ thuật phải được đào tạo, tập huấn về VietGAP trồng trọt hoặc an toàn thực phẩm, có chứng chỉ còn hiệu lực.",
                            "RECORDS_AND_TRAINING", "MUST", "ALL"),
                    new MasterRuleSeed("VG-HS-02",
                            "Công nhân được tập huấn phù hợp với công việc",
                            "Người lao động trực tiếp tham gia sản xuất phải được hướng dẫn/tập huấn kỹ thuật canh tác, vệ sinh cá nhân, an toàn lao động và thu hoạch rau màu.",
                            "RECORDS_AND_TRAINING", "MUST", "ALL"),
                    new MasterRuleSeed("VG-HS-03",
                            "Người kiểm tra nội bộ có tập huấn kỹ năng đánh giá",
                            "Người thực hiện đánh giá nội bộ VietGAP của cơ sở phải có chứng chỉ hoặc biên bản xác nhận đã được tập huấn kỹ năng kiểm tra, đánh giá VietGAP.",
                            "RECORDS_AND_TRAINING", "RECOMMENDED", "ALL"),
                    new MasterRuleSeed("VG-HS-04",
                            "Có quy trình sản xuất nội bộ phù hợp với điều kiện thực tế",
                            "Trang trại phải ban hành và lưu trữ văn bản quy trình kỹ thuật canh tác chi tiết cho từng loại rau quả (dưa lưới, ớt chuông, cà chua, rau ăn lá).",
                            "RECORDS_AND_TRAINING", "MUST", "ALL"),
                    new MasterRuleSeed("VG-HS-05",
                            "Có nhật ký ghi chép sản xuất đầy đủ, cập nhật thường xuyên",
                            "Nhật ký canh tác điện tử phải ghi chép đầy đủ các hoạt động làm đất, gieo trồng, bón phân, phun thuốc, tưới tiêu và thu hoạch theo từng lô sản phẩm.",
                            "RECORDS_AND_TRAINING", "MUST", "ALL"),
                    new MasterRuleSeed("VG-HS-06",
                            "Có hồ sơ lưu trữ (tập huấn, vật tư, thu hoạch)",
                            "Lưu trữ toàn bộ hồ sơ sổ sách, phiếu kiểm nghiệm, hóa đơn vật tư và nhật ký canh tác tối thiểu 02 năm kể từ ngày thu hoạch.",
                            "RECORDS_AND_TRAINING", "MUST", "ALL"),

                    // PHẦN 2 – CƠ SỞ VẬT CHẤT VÀ TRANG THIẾT BỊ (INFRASTRUCTURE_EQUIPMENT)
                    new MasterRuleSeed("VG-CS-01",
                            "Kho/bể chứa phân bón, thuốc BVTV riêng biệt, đảm bảo",
                            "Kho hoặc khu vực chứa phân bón, thuốc BVTV phải thông thoáng, có cửa khóa an toàn, cách ly nguồn nước sinh hoạt/nước tưới và có biển cảnh báo nguy hiểm.",
                            "INFRASTRUCTURE_EQUIPMENT", "MUST", "ALL"),
                    new MasterRuleSeed("VG-CS-02",
                            "Có nhà sơ chế được bố trí hợp lý, đảm bảo ATTP",
                            "Khu vực sơ chế, đóng gói phải có mái che, nền sạch, bàn sơ chế bằng inox hoặc vật liệu không gỉ, kệ kê rau quả cách mặt đất tối thiểu 15cm.",
                            "INFRASTRUCTURE_EQUIPMENT", "MUST", "ALL"),
                    new MasterRuleSeed("VG-CS-03",
                            "Thiết bị, dụng cụ sản xuất/sơ chế được vệ sinh định kỳ",
                            "Kéo tỉa, dao cắt, sọt khay đựng rau quả phải được làm sạch, khử trùng định kỳ và trước mỗi đợt thu hoạch để ngăn ngừa lây nhiễm vi sinh.",
                            "INFRASTRUCTURE_EQUIPMENT", "RECOMMENDED", "ALL"),
                    new MasterRuleSeed("VG-CS-04",
                            "Có sơ đồ vùng sản xuất được cập nhật và treo tại nơi dễ thấy",
                            "Có sơ đồ mặt bằng tổng thể thể hiện rõ ranh giới các lô đất, phân khu nhà màng, vị trí trạm bơm tưới, kho chứa và khu xử lý rác thải.",
                            "INFRASTRUCTURE_EQUIPMENT", "RECOMMENDED", "ALL"),

                    // PHẦN 3 – NGUYÊN LIỆU ĐẦU VÀO & KIỂM NGHIỆM (INPUTS_AND_TESTING)
                    new MasterRuleSeed("VG-NL-01",
                            "Có kết quả kiểm nghiệm đất/giá thể và nguồn nước tưới",
                            "Phiếu phân tích mẫu đất/giá thể (pH 5.5-6.8, kim loại nặng) và mẫu nước tưới (an toàn vi sinh E.coli, Salmonella) còn hiệu lực trong vòng 12 tháng theo QCVN.",
                            "INPUTS_AND_TESTING", "MUST", "ALL"),
                    new MasterRuleSeed("VG-NL-02",
                            "Có biện pháp xử lý khi kết quả đất/nước không đạt",
                            "Khi kết quả đất/nước vượt ngưỡng giới hạn cho phép, phải có biện pháp xử lý bằng vôi khử chua, lọc khử trùng nước tưới hoặc đổi nguồn nước an toàn.",
                            "INPUTS_AND_TESTING", "MUST", "ALL"),
                    new MasterRuleSeed("VG-NL-03",
                            "Có hồ sơ kiểm tra/chứng nhận nguồn gốc giống cây trồng",
                            "Hạt giống, cây giống phải có hóa đơn chứng từ mua hàng, nhãn mác rõ ràng từ cơ sở sản xuất giống hợp pháp, tỷ lệ nảy mầm đạt tiêu chuẩn.",
                            "INPUTS_AND_TESTING", "MUST", "ALL"),
                    new MasterRuleSeed("VG-NL-04",
                            "Sử dụng giống phù hợp, không sâu bệnh, năng suất tốt",
                            "Cây giống đưa vào trồng phải khỏe mạnh, không có triệu chứng nhiễm virus, sạch sâu bệnh hại và thích nghi với điều kiện canh tác của vùng.",
                            "INPUTS_AND_TESTING", "MUST", "ALL"),

                    // PHẦN 4 – QUẢN LÝ PHÂN BÓN & THUỐC BVTV (FERTILIZER_PESTICIDE_MANAGEMENT)
                    new MasterRuleSeed("VG-PB-01",
                            "Chỉ sử dụng phân bón được phép, có nhãn mác rõ ràng",
                            "Phân bón hòa tan tưới nhỏ giọt NPK, trung vi lượng và phân bón lá phải nằm trong Danh mục phân bón được phép lưu hành tại Việt Nam.",
                            "FERTILIZER_PESTICIDE_MANAGEMENT", "MUST", "ALL"),
                    new MasterRuleSeed("VG-PB-02",
                            "Phân hữu cơ được ủ hoai mục đúng kỹ thuật (CẤM PHÂN TƯƠI)",
                            "Phân chuồng phải được ủ hoai mục bằng chế phẩm vi sinh Trichoderma đủ thời gian tối thiểu 60 ngày. Tuyệt đối nghiêm cấm sử dụng phân tươi bón trực tiếp.",
                            "FERTILIZER_PESTICIDE_MANAGEMENT", "MUST", "ALL"),
                    new MasterRuleSeed("VG-PB-03",
                            "Có kế hoạch và hồ sơ sử dụng phân bón phù hợp với cây trồng",
                            "Lập lịch châm phân Fertigation theo từng giai đoạn phát triển của rau quả; ghi chép chi tiết ngày bón, loại phân, nồng độ EC/pH dung dịch và liều lượng.",
                            "FERTILIZER_PESTICIDE_MANAGEMENT", "MUST", "ALL"),
                    new MasterRuleSeed("VG-TV-01",
                            "Thuốc BVTV được mua đúng nguồn, có danh mục rõ ràng",
                            "Chỉ mua và sử dụng thuốc BVTV trong danh mục cho phép. Ưu tiên sử dụng thuốc sinh học, dầu khoáng, thảo mộc, bẫy bả pheromone an toàn.",
                            "FERTILIZER_PESTICIDE_MANAGEMENT", "MUST", "ALL"),
                    new MasterRuleSeed("VG-TV-02",
                            "Có sổ ghi chép chi tiết về loại thuốc, ngày phun, người phun",
                            "Ghi chép chính xác trong nhật ký: ngày phun, tên thương phẩm và hoạt chất, nồng độ pha, liều lượng phun, dịch hại mục tiêu và họ tên người phun.",
                            "FERTILIZER_PESTICIDE_MANAGEMENT", "MUST", "ALL"),
                    new MasterRuleSeed("VG-TV-03",
                            "Có biển cảnh báo sau khi phun thuốc, không để thuốc lan ra vùng khác",
                            "Sau khi phun thuốc phải treo biển cảnh báo khu vực đang cách ly; thao tác cẩn thận không để thuốc phun trôi dạt sang các lô rau sắp thu hoạch kế bên.",
                            "FERTILIZER_PESTICIDE_MANAGEMENT", "RECOMMENDED", "ALL"),
                    new MasterRuleSeed("VG-TV-04",
                            "Thu gom, xử lý vỏ bao bì thuốc đúng quy định",
                            "Vỏ bao bì, chai lọ thuốc BVTV sau khi sử dụng phải súc rửa 3 lần, bỏ vào bể/thùng chứa rác nguy hại có nắp đậy và hợp đồng chuyển giao xử lý chuyên dụng.",
                            "FERTILIZER_PESTICIDE_MANAGEMENT", "MUST", "ALL"),

                    // PHẦN 5 – THU HOẠCH, BẢO QUẢN, VẬN CHUYỂN (HARVEST_STORAGE_TRANSPORT)
                    new MasterRuleSeed("VG-TH-01",
                            "Thu hoạch đúng thời điểm, đúng cách ly sau khi phun thuốc (PHI)",
                            "Nghiêm ngặt tuân thủ thời gian cách ly (PHI) ghi trên nhãn thuốc (tối thiểu 7-14 ngày trước thu hoạch). Cấm thu hoạch khi chưa hết thời gian cách ly an toàn.",
                            "HARVEST_STORAGE_TRANSPORT", "MUST", "ALL"),
                    new MasterRuleSeed("VG-TH-02",
                            "Sản phẩm không để tiếp xúc với đất",
                            "Rau quả tươi sau khi cắt phải đặt trực tiếp vào khay, sọt nhựa sạch có lót vải hoặc đệm xốp; tuyệt đối không để sản phẩm chạm trực tiếp xuống mặt đất.",
                            "HARVEST_STORAGE_TRANSPORT", "MUST", "ALL"),
                    new MasterRuleSeed("VG-TH-03",
                            "Nơi bảo quản sạch, khô ráo, tránh nhiễm chéo",
                            "Kho mát hoặc phòng bảo quản phải sạch sẽ, duy trì nhiệt độ thích hợp (10-15°C với dưa lưới/cà chua, 4-8°C với rau ăn lá), tách biệt với kho hóa chất.",
                            "HARVEST_STORAGE_TRANSPORT", "RECOMMENDED", "ALL"),
                    new MasterRuleSeed("VG-TH-04",
                            "Có biện pháp kiểm soát động vật gây hại",
                            "Hệ thống nhà màng sử dụng lưới chắn côn trùng 50 mesh kín, bố trí bẫy dính màu vàng, bẫy bả sinh học và lưới ngăn chuột, chim xâm nhập.",
                            "HARVEST_STORAGE_TRANSPORT", "RECOMMENDED", "ALL"),
                    new MasterRuleSeed("VG-TH-05",
                            "Vận chuyển bằng phương tiện sạch, không lẫn hàng hóa khác",
                            "Phương tiện vận chuyển nông sản phải sạch sẽ, có mái che hoặc thùng kín; không vận chuyển chung với phân bón, thuốc bảo vệ thực vật hoặc gia súc gia cầm.",
                            "HARVEST_STORAGE_TRANSPORT", "RECOMMENDED", "ALL"),

                    // PHẦN 6 – TRUY XUẤT NGUỒN GỐC & SẢN PHẨM (TRACEABILITY_PRODUCT)
                    new MasterRuleSeed("VG-TX-01",
                            "Có phân biệt rõ sản phẩm VietGAP và không VietGAP",
                            "Sản phẩm đạt chuẩn VietGAP khi đóng gói phải dán tem nhãn nhận diện hoặc mã QR riêng biệt, không để lẫn với sản phẩm thu mua trôi nổi bên ngoài.",
                            "TRACEABILITY_PRODUCT", "MUST", "ALL"),
                    new MasterRuleSeed("VG-TX-02",
                            "Có mã số vùng trồng và quy trình truy xuất (Lot ID)",
                            "Mỗi đợt thu hoạch phải được gán Mã lô thu hoạch (Lot ID) liên kết trực tiếp với nhà màng, ngày thu hái và nhật ký canh tác của mùa vụ tương ứng.",
                            "TRACEABILITY_PRODUCT", "MUST", "ALL"),
                    new MasterRuleSeed("VG-TX-03",
                            "Có xử lý sản phẩm không đạt chất lượng đúng quy trình",
                            "Rau quả dập nát, thối hỏng hoặc phát hiện dư lượng không đạt chuẩn phải được cách ly, ghi nhận số lượng loại bỏ và lập biên bản xử lý tiêu hủy.",
                            "TRACEABILITY_PRODUCT", "MUST", "ALL"),

                    // PHẦN 7 – AN TOÀN LAO ĐỘNG & MÔI TRƯỜNG (LABOR_SAFETY_ENVIRONMENT)
                    new MasterRuleSeed("VG-AT-01",
                            "Người lao động sử dụng đầy đủ bảo hộ (quần áo, găng tay, khẩu trang...)",
                            "Trang bị đầy đủ quần áo bảo hộ, găng tay cao su, kính mắt, khẩu trang than hoạt tính cho người phun thuốc; công nhân thu hái đeo găng tay vệ sinh.",
                            "LABOR_SAFETY_ENVIRONMENT", "MUST", "ALL"),
                    new MasterRuleSeed("VG-AT-02",
                            "Có trang bị bộ sơ cứu, nội quy an toàn treo nơi làm việc",
                            "Bố trí tủ thuốc y tế sơ cứu có sẵn bông băng, dung dịch sát khuẩn, thuốc rửa mắt khẩn cấp tại khu vực sơ chế và kho vật tư kèm bảng nội quy an toàn.",
                            "LABOR_SAFETY_ENVIRONMENT", "RECOMMENDED", "ALL"),
                    new MasterRuleSeed("VG-AT-03",
                            "Không tái sử dụng bao bì thuốc, phân cho mục đích khác",
                            "Tuyệt đối cấm sử dụng can thùng đựng thuốc BVTV, bao bì phân bón để đựng rau củ thành phẩm hoặc trữ nước tưới sinh hoạt.",
                            "LABOR_SAFETY_ENVIRONMENT", "MUST", "ALL"),
                    new MasterRuleSeed("VG-AT-04",
                            "Có thu gom, xử lý rác thải, nước thải đúng quy định",
                            "Nước rửa rau quả sơ chế được thu gom qua hố lắng lọc; tàn dư lá tỉa, cành nhánh sau vụ được gom ủ làm phân compost hữu cơ theo đúng quy trình.",
                            "LABOR_SAFETY_ENVIRONMENT", "RECOMMENDED", "ALL")
            );

            int insertedCount = 0;
            for (MasterRuleSeed seed : masterSeeds) {
                var existing = checklistRuleRepository.findMasterRuleByCode(seed.ruleCode());
                if (existing.isEmpty()) {
                    ChecklistRule rule = new ChecklistRule(
                            null, // farm = null (Master System Rule)
                            null, // createdBy = system
                            seed.ruleCode(),
                            seed.ruleName(),
                            seed.description(),
                            seed.category(),
                            seed.ruleLevel(),
                            seed.targetCropType(),
                            true
                    );
                    checklistRuleRepository.save(rule);
                    insertedCount++;
                }
            }

            if (insertedCount > 0) {
                log.info("--> [VietGapMasterChecklistDataInitializer] Seeded {} master VietGAP rules successfully.", insertedCount);
            } else {
                log.info("--> [VietGapMasterChecklistDataInitializer] 33 VietGAP master rules already exist. No seeding needed.");
            }

        } catch (Exception e) {
            log.error("--> [VietGapMasterChecklistDataInitializer] Error seeding VietGAP master rules: {}", e.getMessage(), e);
        }
    }
}

