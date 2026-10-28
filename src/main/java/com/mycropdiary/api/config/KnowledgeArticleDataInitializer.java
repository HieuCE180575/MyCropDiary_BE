package com.mycropdiary.api.config;

import com.mycropdiary.api.entity.knowledge.KnowledgeArticle;
import com.mycropdiary.api.repository.knowledge.KnowledgeArticleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Tự động khởi tạo bộ cẩm nang 28 bài viết Kiến thức VietGAP chuẩn quốc gia vào cơ sở dữ liệu
 * nếu chưa tồn tại theo slug, đảm bảo API /api/v1/knowledge/articles luôn đầy đủ dữ liệu cho cả 7 chuyên đề.
 */
@Component
public class KnowledgeArticleDataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeArticleDataInitializer.class);

    private final KnowledgeArticleRepository knowledgeArticleRepository;

    public KnowledgeArticleDataInitializer(KnowledgeArticleRepository knowledgeArticleRepository) {
        this.knowledgeArticleRepository = knowledgeArticleRepository;
    }

    private record SeedArticle(
            String slug,
            String title,
            String summary,
            String body,
            String coverImageUrl,
            String category,
            String tags,
            int viewCount,
            int daysAgo
    ) {}

    @Override
    @Transactional
    public void run(String... args) {
        try {
            List<SeedArticle> seeds = List.of(
                    // 1. Quy định chung (regulation)
                    new SeedArticle(
                            "vietgap-la-gi-va-vi-sao-nong-ho-nen-ap-dung",
                            "VietGAP là gì và vì sao nông hộ nên áp dụng?",
                            "Tổng quan về tiêu chuẩn Thực hành nông nghiệp tốt tại Việt Nam (VietGAP - TCVN 11892-1:2017), lợi ích kinh tế cho nông hộ và cơ hội tiếp cận chuỗi siêu thị, xuất khẩu.",
                            "VietGAP (Vietnamese Good Agricultural Practices) là tập hợp các nguyên tắc, trình tự và thủ tục hướng dẫn tổ chức, cá nhân sản xuất, thu hoạch, sơ chế nhằm đảm bảo an toàn thực phẩm, nâng cao chất lượng sản phẩm, bảo vệ sức khoẻ người sản xuất - người tiêu dùng, bảo vệ môi trường và truy xuất nguồn gốc sản phẩm.\n\nKhi áp dụng VietGAP, nông hộ và hợp tác xã tạo dựng được niềm tin vững chắc với hệ thống siêu thị, bếp ăn công nghiệp và thương lái xuất khẩu; đồng thời tối ưu chi phí phân bón, thuốc BVTV nhờ quy trình ghi chép định lượng rõ ràng.",
                            "https://images.unsplash.com/photo-1500937386664-56d1dfef3854?auto=format&fit=crop&w=800&q=80",
                            "regulation", "tổng quan, tiêu chuẩn, chứng nhận", 1520, 2
                    ),
                    new SeedArticle(
                            "4-nhom-tieu-chi-bat-buoc-trong-chung-nhan-vietgap",
                            "4 nhóm tiêu chí bắt buộc trong chứng nhận VietGAP",
                            "Phân tích chi tiết 4 trụ cột của VietGAP: An toàn thực phẩm, Bảo vệ môi trường, Sức khỏe an toàn lao động và Truy xuất nguồn gốc sản phẩm.",
                            "Bộ tiêu chí VietGAP trồng trọt được chia thành các nhóm yêu cầu cốt lõi:\n1. Đánh giá và lựa chọn vùng sản xuất phù hợp, không bị ô nhiễm hoá học và sinh học.\n2. Quản lý giống, gốc ghép có nguồn gốc rõ ràng và sạch bệnh.\n3. Quản lý đất, giá thể, phân bón, nước tưới và thuốc BVTV theo đúng danh mục cho phép.\n4. Ghi chép, lưu trữ hồ sơ tối thiểu 12–24 tháng phục vụ truy xuất nguồn gốc.\n\nMỗi tiêu chí được phân loại thành mức A (bắt buộc đạt 100%), mức B (khuyến khích, cần đạt tối thiểu 90%) và mức C (khuyến nghị).",
                            "https://images.unsplash.com/photo-1738598668890-5f4d28842df5?auto=format&fit=crop&w=800&q=80",
                            "regulation", "tiêu chí, chứng nhận, chỉ tiêu loại A", 1180, 4
                    ),
                    new SeedArticle(
                            "quy-trinh-dang-ky-va-cap-giay-chung-nhan-vietgap",
                            "Quy trình đăng ký và cấp giấy chứng nhận VietGAP",
                            "Hướng dẫn từng bước từ chuẩn bị hồ sơ vùng trồng, tự đánh giá nội bộ đến nộp đơn đăng ký với tổ chức chứng nhận được Bộ NN&PTNT chỉ định.",
                            "Quy trình đăng ký chứng nhận VietGAP gồm 6 bước chuẩn:\n- Bước 1: Khảo sát vùng trồng, lấy mẫu đất - nước phân tích các chỉ tiêu kim loại nặng và vi sinh.\n- Bước 2: Xây dựng quy trình canh tác chuẩn và ban hành sổ nhật ký sản xuất.\n- Bước 3: Tập huấn nội bộ cho toàn bộ thành viên/lao động trong trang trại.\n- Bước 4: Thực hiện canh tác, ghi chép nhật ký ít nhất 1 vụ mùa và tiến hành đánh giá nội bộ.\n- Bước 5: Khắc phục các điểm không phù hợp và nộp hồ sơ cho Tổ chức chứng nhận.\n- Bước 6: Đoàn chuyên gia đánh giá thực địa, lấy mẫu nông sản trước thu hoạch và cấp Giấy chứng nhận VietGAP (hiệu lực tối đa 3 năm).",
                            "https://images.unsplash.com/photo-1727099079513-952d40de9d78?auto=format&fit=crop&w=800&q=80",
                            "regulation", "quy trình, hồ sơ, tổ chức chứng nhận", 860, 6
                    ),
                    new SeedArticle(
                            "so-sanh-tieu-chuan-vietgap-globalgap-va-huu-co",
                            "So sánh tiêu chuẩn VietGAP, GlobalGAP và Hữu cơ (Organic)",
                            "Điểm giống và khác nhau giữa VietGAP, GlobalGAP và canh tác hữu cơ để chủ trang trại lựa chọn lộ trình chứng nhận phù hợp với thị trường mục tiêu.",
                            "VietGAP được thiết kế phù hợp với điều kiện canh tác tại Việt Nam, cho phép sử dụng phân bón vô cơ và thuốc BVTV có kiểm soát trong danh mục cho phép và tuân thủ thời gian cách ly (PHI).\n\nTrong khi đó, GlobalGAP bổ sung các tiêu chuẩn khắt khe hơn về phúc lợi người lao động, quản lý năng lượng và bảo tồn đa dạng sinh học phục vụ xuất khẩu sang EU, Mỹ, Nhật Bản. Tiêu chuẩn Hữu cơ (Organic) nghiêm cấm hoàn toàn phân bón hóa học, thuốc trừ sâu tổng hợp và giống biến đổi gen (GMO).",
                            "https://images.unsplash.com/photo-1464226184884-fa280b87c399?auto=format&fit=crop&w=800&q=80",
                            "regulation", "VietGAP, GlobalGAP, hữu cơ", 945, 7
                    ),

                    // 2. Đất & nguồn nước (soil-water)
                    new SeedArticle(
                            "danh-gia-va-lua-chon-vung-san-xuat-an-toan",
                            "Đánh giá và lựa chọn vùng sản xuất an toàn",
                            "Các yếu tố bắt buộc khảo sát: lịch sử sử dụng đất, nguy cơ ngập úng, khoảng cách cách ly với khu công nghiệp, bệnh viện và bãi rác.",
                            "Trước khi đưa lô đất vào sản xuất VietGAP, chủ trang trại cần khảo sát lịch sử canh tác tối thiểu 2 năm gần nhất, đánh giá nguy cơ tồn dư hóa chất hoặc mầm bệnh trong đất.\n\nVùng trồng phải nằm cách xa khu công nghiệp, lò gạch, nghĩa trang, bệnh viện hoặc nguồn nước thải chưa xử lý. Trường hợp gần khu vực có nguy cơ ô nhiễm, bắt buộc thiết lập bờ bao, rãnh thoát nước riêng và hàng cây chắn gió (vùng đệm sinh học).",
                            "https://images.unsplash.com/photo-1697897400733-46e12551a012?auto=format&fit=crop&w=800&q=80",
                            "soil-water", "vùng sản xuất, khảo sát, vùng đệm", 640, 9
                    ),
                    new SeedArticle(
                            "quan-ly-va-cai-tao-dat-trong-theo-huong-ben-vung",
                            "Quản lý và cải tạo đất trồng theo hướng bền vững",
                            "Biện pháp luân canh cây họ đậu, che phủ đất giữ ẩm, bổ sung nấm đối kháng Trichoderma và phân hữu cơ vi sinh để chống thoái hóa đất.",
                            "Để duy trì độ phì nhiêu tự nhiên và cấu tượng đất tơi xốp, nông hộ nên áp dụng luân canh giữa cây trồng chính với cây họ đậu, hạn chế cày xới sâu làm đứt gãy hệ vi sinh vật có lợi.\n\nSau mỗi vụ thu hoạch, tiến hành vệ sinh đồng ruộng, bón vôi hạ phèn khử trùng kết hợp ủ rơm rạ với chế phẩm Trichoderma. Định kỳ 6–12 tháng kiểm tra độ pH, EC và hàm lượng mùn để điều chỉnh phác đồ bón phân chính xác.",
                            "https://images.unsplash.com/photo-1586771107445-d3ca888129ff?auto=format&fit=crop&w=800&q=80",
                            "soil-water", "đất trồng, canh tác bền vững, Trichoderma", 780, 11
                    ),
                    new SeedArticle(
                            "tieu-chuan-chat-luong-nuoc-tuoi-trong-vietgap",
                            "Tiêu chuẩn chất lượng nước tưới trong VietGAP",
                            "Ngưỡng cho phép về vi sinh (E.coli, Coliform) và kim loại nặng (As, Cd, Pb, Hg) trong nước tưới cùng hướng dẫn lấy mẫu kiểm nghiệm định kỳ.",
                            "Nước sử dụng cho tưới tiêu và pha thuốc BVTV phải đáp ứng quy chuẩn kỹ thuật quốc gia về chất lượng nước mặt hoặc nước ngầm phục vụ tưới tiêu (kiểm soát chặt 4 kim loại nặng: Asen, Cadimi, Chì, Thủy ngân và vi sinh vật E.coli).\n\nTuyệt đối không dùng nước thải sinh hoạt, nước thải chăn nuôi hoặc công nghiệp chưa qua xử lý.",
                            "https://images.unsplash.com/photo-1752775312083-1cefe2f93358?auto=format&fit=crop&w=800&q=80",
                            "soil-water", "nước tưới, kiểm nghiệm, kim loại nặng", 540, 13
                    ),
                    new SeedArticle(
                            "quan-ly-va-xu-ly-chat-thai-nong-nghiep-dung-cach",
                            "Quản lý và xử lý chất thải nông nghiệp đúng cách",
                            "Quy định xây dựng bể thu gom vỏ bao bì thuốc BVTV sau súc rửa 3 lần và quy trình ủ compost phụ phẩm cây trồng an toàn.",
                            "Bao bì, chai lọ đựng thuốc BVTV sau khi pha xong phải được súc rửa 3 lần (nước tráng đổ vào bình phun), sau đó thu gom ngay vào bể chứa chuyên dụng có mái che, khóa an toàn và biển cảnh báo.\n\nTuyệt đối không đốt hoặc chôn lấp vỏ thuốc BVTV tại trang trại hay vứt xuống kênh mương. Đối với phụ phẩm hữu cơ, thu gom về khu vực ủ compost cách xa nguồn nước tưới để tái tạo phân bón hữu cơ.",
                            "https://images.unsplash.com/photo-1742071853823-f9f87d9f3894?auto=format&fit=crop&w=800&q=80",
                            "soil-water", "chất thải, môi trường, bao bì BVTV", 490, 15
                    ),

                    // 3. Giống & gieo trồng (seed-cultivation)
                    new SeedArticle(
                            "chon-giong-cay-trong-dat-chuan-vietgap",
                            "Chọn giống cây trồng đạt chuẩn VietGAP",
                            "Yêu cầu bắt buộc về hóa đơn, nhãn mác, nguồn gốc lô hạt giống/cây giống và quy trình xử lý hạt giống trước khi gieo.",
                            "Giống cây trồng, hạt giống hoặc gốc ghép đưa vào sản xuất VietGAP phải nằm trong Danh mục giống cây trồng được phép sản xuất, kinh doanh tại Việt Nam.\n\nKhi mua giống, chủ trang trại cần lưu giữ đầy đủ hóa đơn, bao bì, nhãn mác ghi rõ số lô, ngày sản xuất và hạn sử dụng.",
                            "https://images.unsplash.com/photo-1781782411066-c68b1e5610bc?auto=format&fit=crop&w=800&q=80",
                            "seed-cultivation", "giống cây trồng, nguồn gốc, lô giống", 705, 16
                    ),
                    new SeedArticle(
                            "ky-thuat-gieo-trong-va-mat-do-hop-ly-theo-mua-vu",
                            "Kỹ thuật gieo trồng và mật độ hợp lý theo mùa vụ",
                            "Cách thiết kế luống trồng theo hướng nắng, tính toán khoảng cách cây x hàng để thông thoáng tán lá và hạn chế nấm bệnh phát sinh.",
                            "Mật độ gieo trồng cần căn cứ vào đặc tính sinh trưởng của từng giống, mùa vụ và độ phì nhiêu của đất.\n\nLên luống cao 20–30 cm vào mùa mưa giúp thoát nước nhanh, hạn chế thối rễ. Việc bố trí khoảng cách hợp lý giúp ánh sáng phân bổ đều khắp tầng lá, giảm ẩm độ cục bộ trong tán cây.",
                            "https://images.unsplash.com/photo-1773994788915-7fb0f462f223?auto=format&fit=crop&w=800&q=80",
                            "seed-cultivation", "thời vụ, mật độ trồng, lên luống", 530, 18
                    ),
                    new SeedArticle(
                            "ky-thuat-quan-ly-vuon-uom-va-cham-soc-cay-con",
                            "Kỹ thuật quản lý vườn ươm và chăm sóc cây con khỏe mạnh",
                            "Chuẩn bị giá thể ươm vô trùng, kiểm soát ánh sáng - độ ẩm trong nhà ươm và tiêu chuẩn huấn luyện cây con trước khi đem trồng ra ruộng.",
                            "Cây con khỏe mạnh quyết định 50% sự thành công của vụ mùa. Giá thể ươm hạt (xơ dừa đã xử lý chát, tro trấu, phân trùn quế) phải sạch mầm bệnh và tuyến trùng.\n\nNhà ươm cần có lưới chắn côn trùng và khay ươm đặt trên giàn cách mặt đất tối thiểu 50 cm.",
                            "https://images.unsplash.com/photo-1592982537447-7440770cbfc9?auto=format&fit=crop&w=800&q=80",
                            "seed-cultivation", "vườn ươm, giá thể, cây con", 625, 19
                    ),
                    new SeedArticle(
                            "ky-thuat-tia-canh-tao-tan-va-bao-trai",
                            "Kỹ thuật tỉa cành, tạo tán và bao trái nâng cao phẩm chất nông sản",
                            "Thời điểm cắt tỉa cành vượt, cành sâu bệnh và kỹ thuật bao trái đúng giai đoạn giúp mẫu mã quả đẹp, hạn chế ruồi đục quả mà không cần phun thuốc.",
                            "Đối với cây ăn quả và rau ăn quả, việc tỉa bỏ cành tăm, cành vượt, lá già dưới gốc giúp tập trung dinh dưỡng nuôi hoa và quả non.\n\nDụng cụ cắt tỉa phải được khử trùng bằng cồn 70 độ khi chuyển từ cây có dấu hiệu bệnh sang cây khỏe. Kết hợp bao trái bằng túi chuyên dụng giúp ngăn chặn triệt để ruồi vàng đục quả.",
                            "https://images.unsplash.com/photo-1530836369250-ef72a3f5cda8?auto=format&fit=crop&w=800&q=80",
                            "seed-cultivation", "tỉa cành, bao trái, cây ăn quả", 710, 20
                    ),

                    // 4. Phân bón & thuốc BVTV (fertilizer-pesticide)
                    new SeedArticle(
                            "nguyen-tac-4-dung-khi-su-dung-thuoc-bao-ve-thuc-vat",
                            "Nguyên tắc \"4 đúng\" khi sử dụng thuốc bảo vệ thực vật",
                            "Đúng thuốc, đúng liều lượng, đúng lúc và đúng cách để diệt trừ dịch hại hiệu quả, tránh kháng thuốc và đảm bảo an toàn vệ sinh thực phẩm.",
                            "Nguyên tắc \"4 đúng\" là kim chỉ nam trong bảo vệ thực vật VietGAP:\n1. Đúng thuốc: Chỉ dùng thuốc nằm trong Danh mục được phép sử dụng tại Việt Nam; ưu tiên thuốc sinh học, thảo mộc.\n2. Đúng liều lượng: Pha đúng nồng độ khuyến cáo trên nhãn.\n3. Đúng lúc: Phun khi sâu non mới nở hoặc bệnh mới chớm xuất hiện.\n4. Đúng cách: Phun ướt đều mặt dưới lá nơi sâu rầy ẩn nấp, không phun ngược chiều gió.",
                            "https://images.unsplash.com/photo-1625246333195-78d9c38ad449?auto=format&fit=crop&w=800&q=80",
                            "fertilizer-pesticide", "thuốc BVTV, 4 đúng, an toàn", 1120, 21
                    ),
                    new SeedArticle(
                            "thoi-gian-cach-ly-thuoc-bvtv-truoc-khi-thu-hoach",
                            "Thời gian cách ly thuốc BVTV trước khi thu hoạch",
                            "Cách tra cứu chỉ số PHI trên nhãn thuốc, tính toán ngày thu hoạch an toàn và cơ chế cảnh báo tự động trên nhật ký điện tử MyCropDiary.",
                            "Thời gian cách ly (Pre-Harvest Interval - PHI) là khoảng thời gian tối thiểu tính từ ngày phun thuốc BVTV hoặc bón phân lần cuối cùng đến ngày được phép thu hoạch sản phẩm.\n\nKhi ghi nhận hoạt động phun thuốc trên phần mềm MyCropDiary, hệ thống sẽ tự động cảnh báo khóa thu hoạch trên lô đất đang trong thời gian cách ly để ngăn ngừa tuyệt đối nguy cơ vượt ngưỡng dư lượng MRL.",
                            "https://images.unsplash.com/photo-1595974482597-4b8da8879bc5?auto=format&fit=crop&w=800&q=80",
                            "fertilizer-pesticide", "thời gian cách ly, PHI, dư lượng", 980, 22
                    ),
                    new SeedArticle(
                            "su-dung-phan-bon-huu-co-va-vo-co-can-doi",
                            "Sử dụng phân bón hữu cơ và vô cơ cân đối",
                            "Quy định nghiêm cấm sử dụng phân chuồng tươi và kỹ thuật phối trộn NPK - trung vi lượng theo từng giai đoạn sinh trưởng của cây.",
                            "Trong tiêu chuẩn VietGAP, tuyệt đối không sử dụng phân bắc, phân chuồng tươi hoặc bùn thải chưa qua xử lý để bón trực tiếp cho cây trồng vì nguy cơ nhiễm khuẩn Salmonella và E.coli rất cao.\n\nPhân chuồng phải được ủ hoai mục hoàn toàn (nhiệt độ đống ủ đạt 55–65°C trong ít nhất 30–45 ngày) và có nhật ký theo dõi mẻ ủ.",
                            "https://images.unsplash.com/photo-1729009511263-329748943004?auto=format&fit=crop&w=800&q=80",
                            "fertilizer-pesticide", "phân bón, ủ hoai mục, dinh dưỡng", 645, 23
                    ),
                    new SeedArticle(
                            "quan-ly-dich-hai-tong-hop-ipm-giam-phu-thuoc-hoa-chat",
                            "Quản lý dịch hại tổng hợp (IPM) giảm phụ thuộc hóa chất",
                            "Kết hợp bẫy dính màu vàng/xanh, bẫy pheromone, thiên địch có ích và chế phẩm vi sinh (Bt, Beauveria, Metarhizium) trong nhà màng và ngoài đồng.",
                            "Quản lý dịch hại tổng hợp (IPM) là trụ cột của canh tác VietGAP hiện đại, ưu tiên các biện pháp phòng ngừa trước khi can thiệp hóa học: luân canh, chọn giống kháng bệnh, treo bẫy dính màu vàng/xanh, bẫy Pheromone và bảo vệ thiên địch tự nhiên.",
                            "https://images.unsplash.com/photo-1416879595882-3373a0480b5b?auto=format&fit=crop&w=800&q=80",
                            "fertilizer-pesticide", "IPM, sinh học, bẫy côn trùng", 815, 24
                    ),

                    // 5. Thu hoạch & sau thu hoạch (harvest-post-harvest)
                    new SeedArticle(
                            "ky-thuat-thu-hoach-giam-ton-that-sau-thu-hoach",
                            "Kỹ thuật thu hoạch giảm tổn thất sau thu hoạch",
                            "Lựa chọn thời điểm thu hoạch trong ngày, quy cách cắt cuống và biện pháp hạ nhiệt sơ bộ để kéo dài tuổi thọ bảo quản nông sản.",
                            "Nông sản chỉ được thu hoạch khi đã hết thời gian cách ly thuốc BVTV và đạt độ chín sinh lý hoặc độ chín thương phẩm phù hợp.\n\nNên thu hoạch vào sáng sớm hoặc chiều mát, sử dụng kéo cắt chuyên dụng sạch, đặt nhẹ nhàng vào sọt nhựa có lót xốp, tuyệt đối không để nông sản tiếp xúc trực tiếp với mặt đất.",
                            "https://images.unsplash.com/photo-1746014929708-fcb859fd3185?auto=format&fit=crop&w=800&q=80",
                            "harvest-post-harvest", "thu hoạch, bảo quản, giảm tổn thất", 610, 25
                    ),
                    new SeedArticle(
                            "ve-sinh-dung-cu-va-khu-vuc-so-che-nong-san",
                            "Vệ sinh dụng cụ và khu vực sơ chế nông sản",
                            "Quy chuẩn thiết kế nhà sơ chế một chiều, phân biệt sọt đựng nông sản với sọt đựng phế phẩm và lịch vệ sinh khử trùng định kỳ.",
                            "Nhà sơ chế, phân loại và đóng gói phải thông thoáng, có mái che, nền cao ráo dễ thoát nước và có biện pháp ngăn chặn chuột, côn trùng, gia súc xâm nhập.\n\nSọt chứa nông sản sạch phải được đánh dấu rõ ràng, không dùng chung với dụng cụ đựng phân bón hay hóa chất.",
                            "https://images.unsplash.com/photo-1657288089316-c0350003ca49?auto=format&fit=crop&w=800&q=80",
                            "harvest-post-harvest", "vệ sinh, sơ chế, lây nhiễm chéo", 475, 26
                    ),
                    new SeedArticle(
                            "quy-chuan-dong-goi-ghi-nhan-va-bao-quan-lanh-nong-san",
                            "Quy chuẩn đóng gói, ghi nhãn và bảo quản lạnh nông sản VietGAP",
                            "Các thông tin bắt buộc trên tem nhãn bao bì nông sản VietGAP và chế độ nhiệt độ - độ ẩm kho mát cho từng nhóm rau quả.",
                            "Bao bì đóng gói nông sản phải làm từ vật liệu an toàn thực phẩm, sạch và mới. Trên nhãn sản phẩm VietGAP cần thể hiện rõ: Tên sản phẩm, Tên & địa chỉ cơ sở sản xuất, Mã số chứng nhận VietGAP, Mã lô thu hoạch / QR truy xuất nguồn gốc và Ngày thu hoạch.",
                            "https://images.unsplash.com/photo-1598170845058-32b9d6a5da37?auto=format&fit=crop&w=800&q=80",
                            "harvest-post-harvest", "đóng gói, ghi nhãn, bảo quản lạnh", 590, 27
                    ),
                    new SeedArticle(
                            "quy-dinh-van-chuyen-nong-san-an-toan-tu-trang-trai",
                            "Quy định vận chuyển nông sản an toàn từ trang trại đến điểm tiêu thụ",
                            "Kiểm tra vệ sinh thùng xe vận chuyển, quy tắc xếp dỡ hàng hóa và nghiêm cấm vận chuyển chung với hóa chất, vật tư nông nghiệp.",
                            "Phương tiện vận chuyển nông sản từ trang trại đến nơi tiêu thụ phải được vệ sinh sạch sẽ trước khi bốc xếp hàng.\n\nTuyệt đối không vận chuyển nông sản chung khoang với phân bón, thuốc BVTV, xăng dầu hoặc động vật sống.",
                            "https://images.unsplash.com/photo-1542838132-92c53300491e?auto=format&fit=crop&w=800&q=80",
                            "harvest-post-harvest", "vận chuyển, chuỗi cung ứng, an toàn thực phẩm", 430, 28
                    ),

                    // 6. Ghi chép & truy xuất nguồn gốc (record-traceability)
                    new SeedArticle(
                            "he-thong-ghi-chep-nhat-ky-canh-tac-theo-vietgap",
                            "Hệ thống ghi chép nhật ký canh tác theo VietGAP",
                            "Danh mục các loại sổ nhật ký cần ghi chép đầy đủ: giống, phân bón, thuốc BVTV, tưới nước, thu hoạch, tiêu thụ và xử lý khiếu nại.",
                            "Hồ sơ nhật ký VietGAP là bằng chứng pháp lý quan trọng nhất khi đoàn kiểm toán đánh giá. Trang trại cần duy trì đầy đủ sổ theo dõi nhập - xuất - tồn kho vật tư, nhật ký canh tác từng lô đất, nhật ký bón phân - phun thuốc BVTV và sổ ghi nhận thu hoạch trong tối thiểu 24 tháng.",
                            "https://images.unsplash.com/photo-1748742633127-941ebec208da?auto=format&fit=crop&w=800&q=80",
                            "record-traceability", "ghi chép, nhật ký, biểu mẫu VietGAP", 830, 29
                    ),
                    new SeedArticle(
                            "ma-so-vung-trong-va-truy-xuat-nguon-goc-dien-tu",
                            "Mã số vùng trồng và truy xuất nguồn gốc điện tử",
                            "Hướng dẫn thiết lập mã lô sản xuất (Batch Code) gắn với từng khu vực (Zone/Block) và xuất mã QR minh bạch nhật ký canh tác.",
                            "Mỗi lô nông sản xuất bán phải được gắn mã nhận diện (Lot/Batch Code) liên kết trực tiếp tới Khu sản xuất (Zone), Lô đất (Block) và Mùa vụ tương ứng.\n\nKhi người tiêu dùng quét mã QR trên bao bì, hệ thống hiển thị minh bạch tên trang trại, vị trí vùng trồng, giống cây và lịch sử chăm sóc.",
                            "https://images.unsplash.com/photo-1775681150283-04970adf1b98?auto=format&fit=crop&w=800&q=80",
                            "record-traceability", "mã vùng trồng, QR code, truy xuất nguồn gốc", 670, 30
                    ),
                    new SeedArticle(
                            "quy-trinh-thu-hoi-san-pham-khong-an-toan-va-xu-ly-su-co",
                            "Quy trình thu hồi sản phẩm không an toàn và xử lý sự cố VietGAP",
                            "Các bước khoanh vùng lô hàng lỗi, thông báo khách hàng, điều tra nguyên nhân gốc rễ và lập biên bản hành động khắc phục.",
                            "Theo tiêu chuẩn VietGAP, mọi cơ sở sản xuất phải có quy trình bằng văn bản về việc thu hồi sản phẩm khi phát hiện nguy cơ mất an toàn thực phẩm. Ngay khi phát hiện sự cố, chủ trang trại phải dừng xuất hàng, tra cứu sổ xuất bán theo mã lô để thông báo thu hồi trong vòng 24 giờ.",
                            "https://images.unsplash.com/photo-1551288049-bebda4e38f71?auto=format&fit=crop&w=800&q=80",
                            "record-traceability", "thu hồi sản phẩm, xử lý sự cố, kiểm toán", 515, 31
                    ),
                    new SeedArticle(
                            "so-hoa-nhat-ky-nong-trai-tren-dien-thoai-thay-the-so-giay",
                            "Số hóa nhật ký nông trại trên điện thoại thay thế sổ giấy truyền thống",
                            "Cách phân quyền cho nhân sự kỹ thuật ghi nhật ký trực tiếp tại ruộng, đính kèm ảnh chụp thực tế và xuất báo cáo kiểm định nhanh chóng.",
                            "Với ứng dụng nhật ký số MyCropDiary, nhân viên được phân công phụ trách từng Khu vực (Zone) có thể cập nhật công việc ngay tại ruộng kèm hình ảnh thực tế. Chủ trang trại theo dõi tiến độ theo thời gian thực và xuất file báo cáo tổng hợp đúng chuẩn biểu mẫu VietGAP bất cứ lúc nào.",
                            "https://images.unsplash.com/photo-1454165804606-c3d57bc86b40?auto=format&fit=crop&w=800&q=80",
                            "record-traceability", "nhật ký điện tử, chuyển đổi số, MyCropDiary", 910, 32
                    ),

                    // 7. Đào tạo & chứng nhận (training-certification)
                    new SeedArticle(
                            "dao-tao-va-trang-bi-bao-ho-cho-nguoi-lao-dong",
                            "Đào tạo và trang bị bảo hộ cho người lao động",
                            "Yêu cầu tập huấn định kỳ cho công nhân, trang bị đồ bảo hộ khi pha phun thuốc và bố trí tủ thuốc sơ cứu tại khu sản xuất.",
                            "Người lao động trực tiếp tham gia sản xuất VietGAP phải đủ sức khỏe và được tập huấn ít nhất 1 lần/năm về quy trình canh tác an toàn, kỹ thuật pha phun thuốc BVTV và cách xử lý sự cố ngộ độc hóa chất.\n\nKhi tiếp xúc với thuốc BVTV, người lao động bắt buộc mặc quần áo bảo hộ chuyên dụng, đeo găng tay cao su, ủng, kính chắn giọt bắn và mặt nạ phòng độc.",
                            "https://images.unsplash.com/photo-1781785186628-e1a04569776d?auto=format&fit=crop&w=800&q=80",
                            "training-certification", "đào tạo, an toàn lao động, bảo hộ PPE", 560, 33
                    ),
                    new SeedArticle(
                            "danh-gia-noi-bo-truoc-khi-dang-ky-chung-nhan",
                            "Đánh giá nội bộ trước khi đăng ký chứng nhận",
                            "Phương pháp rà soát từng chỉ tiêu loại A và loại B, cách ghi nhận bằng chứng tuân thủ và đóng các điểm không phù hợp (NCR).",
                            "Đánh giá nội bộ là yêu cầu bắt buộc tối thiểu 1 lần/năm trước khi mời Tổ chức chứng nhận xuống thẩm định chính thức. Người phụ trách kỹ thuật sử dụng bảng Checklist VietGAP để kiểm tra thực tế kho vật tư, khu ủ phân, biển báo vùng trồng, bể thu gom vỏ thuốc và sổ nhật ký.",
                            "https://images.unsplash.com/photo-1738244969496-eeabfacec172?auto=format&fit=crop&w=800&q=80",
                            "training-certification", "đánh giá nội bộ, checklist, thẩm định", 615, 34
                    ),
                    new SeedArticle(
                            "tieu-chuan-thiet-ke-kho-chua-phan-bon-thuoc-bvtv-va-bien-bao",
                            "Tiêu chuẩn thiết kế kho chứa phân bón, thuốc BVTV và biển báo trang trại",
                            "Các yêu cầu kỹ thuật về kho vật tư có khóa, kệ kê cách mặt đất, gờ chống tràn, cát cứu hỏa và hệ thống sơ đồ - biển báo tại nông trại.",
                            "Kho thuốc BVTV phải tách biệt với khu sinh hoạt và khu sơ chế nông sản, có khóa an toàn, thông gió tốt. Thuốc dạng lỏng xếp kệ dưới, thuốc dạng bột xếp kệ trên; trong kho có xô cát khô, xẻng, vôi bột xử lý tràn đổ và toàn trang trại có biển cảnh báo khu vực mới phun thuốc.",
                            "https://images.unsplash.com/photo-1574943320219-553eb213f72d?auto=format&fit=crop&w=800&q=80",
                            "training-certification", "kho vật tư, biển báo, đánh giá thực địa", 740, 35
                    ),
                    new SeedArticle(
                            "kinh-nghiem-duy-tri-chung-nhan-vietgap-qua-cac-ky-giam-sat",
                            "Kinh nghiệm duy trì chứng nhận VietGAP qua các kỳ giám sát hàng năm",
                            "Những lỗi thường gặp khiến trang trại bị đình chỉ hoặc thu hồi giấy chứng nhận VietGAP và bí quyết duy trì thói quen ghi chép liên tục.",
                            "Giấy chứng nhận VietGAP có hiệu lực tối đa 3 năm nhưng Tổ chức chứng nhận sẽ thực hiện đánh giá giám sát định kỳ 12 tháng/lần. Duy trì nhật ký số hằng ngày trên MyCropDiary giúp trang trại luôn trong tâm thế sẵn sàng cho mọi kỳ thanh tra.",
                            "https://images.unsplash.com/photo-1589923188900-85dae523342b?auto=format&fit=crop&w=800&q=80",
                            "training-certification", "duy trì chứng nhận, giám sát định kỳ, kinh nghiệm", 685, 36
                    )
            );

            int inserted = 0;
            for (SeedArticle seed : seeds) {
                if (!knowledgeArticleRepository.existsBySlug(seed.slug())) {
                    KnowledgeArticle article = new KnowledgeArticle();
                    article.setTitle(seed.title());
                    article.setSlug(seed.slug());
                    article.setSummary(seed.summary());
                    article.setBody(seed.body());
                    article.setCoverImageUrl(seed.coverImageUrl());
                    article.setCategory(seed.category());
                    article.setTags(seed.tags());
                    article.setViewCount(seed.viewCount());
                    article.setArticleStatus("published");
                    article.setPublishedAt(Instant.now().minus(seed.daysAgo(), ChronoUnit.DAYS));
                    knowledgeArticleRepository.save(article);
                    inserted++;
                }
            }
            if (inserted > 0) {
                log.info("Đã khởi tạo bổ sung {} bài viết Kiến thức VietGAP vào cơ sở dữ liệu.", inserted);
            }
        } catch (Exception ex) {
            log.warn("Bỏ qua khởi tạo dữ liệu mẫu KnowledgeArticle: {}", ex.getMessage());
        }
    }
}
