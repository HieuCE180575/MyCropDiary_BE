package com.mycropdiary.api.service.impl;

import com.mycropdiary.api.dto.ai.*;
import com.mycropdiary.api.entity.*;
import com.mycropdiary.api.entity.cultivation.ChecklistRule;
import com.mycropdiary.api.entity.knowledge.KnowledgeArticle;
import com.mycropdiary.api.exception.BadRequestException;
import com.mycropdiary.api.exception.ResourceNotFoundException;
import com.mycropdiary.api.repository.*;
import com.mycropdiary.api.repository.cultivation.ChecklistRuleRepository;
import com.mycropdiary.api.repository.knowledge.KnowledgeArticleRepository;
import com.mycropdiary.api.service.AIAssistantService;
import com.mycropdiary.api.service.ai.OpenAIService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;

@Service
public class AIAssistantServiceImpl implements AIAssistantService {

    private static final Logger log = LoggerFactory.getLogger(AIAssistantServiceImpl.class);

    private final AIConversationRepository conversationRepository;
    private final AIMessageRepository messageRepository;
    private final AIFeedbackRepository feedbackRepository;
    private final UserRepository userRepository;
    private final FarmRepository farmRepository;
    private final ChecklistRuleRepository checklistRuleRepository;
    private final KnowledgeArticleRepository knowledgeArticleRepository;
    private final OpenAIService openAIService;

    public AIAssistantServiceImpl(
            AIConversationRepository conversationRepository,
            AIMessageRepository messageRepository,
            AIFeedbackRepository feedbackRepository,
            UserRepository userRepository,
            FarmRepository farmRepository) {
        this(conversationRepository, messageRepository, feedbackRepository, userRepository, farmRepository, null, null, null);
    }

    @Autowired
    public AIAssistantServiceImpl(
            AIConversationRepository conversationRepository,
            AIMessageRepository messageRepository,
            AIFeedbackRepository feedbackRepository,
            UserRepository userRepository,
            FarmRepository farmRepository,
            ChecklistRuleRepository checklistRuleRepository,
            KnowledgeArticleRepository knowledgeArticleRepository,
            OpenAIService openAIService) {
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.feedbackRepository = feedbackRepository;
        this.userRepository = userRepository;
        this.farmRepository = farmRepository;
        this.checklistRuleRepository = checklistRuleRepository;
        this.knowledgeArticleRepository = knowledgeArticleRepository;
        this.openAIService = openAIService;
    }

    @Override
    @Transactional
    public AIConversationRespDTO createConversation(Integer userId, CreateAIConversationReqDTO req) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng ID: " + userId));
        Farm farm = req.farmId() != null ? farmRepository.findById(req.farmId()).orElse(null) : null;

        AIConversation conversation = new AIConversation(user, farm, req.title().trim());
        AIConversation saved = conversationRepository.save(conversation);

        // Welcome greeting message
        AIMessage welcomeMsg = new AIMessage(
                saved,
                "assistant",
                "Xin chào! Tôi là Trợ lý AI nông nghiệp VietGAP của MyCropDiary. Tôi được kết nối trực tiếp với Cơ sở tri thức và Bộ tiêu chí đánh giá VietGAP của trang trại để hỗ trợ bạn chẩn đoán sâu bệnh, tư vấn danh mục phân bón/thuốc BVTV, thời gian cách ly (PHI) và nhật ký canh tác chuẩn TCVN 11892-1:2017. Bạn cần hỗ trợ gì hôm nay?",
                "VietGAP-LLM-RAG"
        );
        messageRepository.save(welcomeMsg);

        return new AIConversationRespDTO(
                saved.getId(),
                saved.getConversationTitle(),
                saved.getConversationStatus(),
                saved.getCreatedAt(),
                saved.getUpdatedAt(),
                1,
                welcomeMsg.getMessageText()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<AIConversationRespDTO> getMyConversations(Integer userId, String keyword) {
        List<AIConversation> list;
        if (keyword != null && !keyword.trim().isEmpty()) {
            list = conversationRepository.searchByUserIdAndKeyword(userId, keyword.trim());
        } else {
            list = conversationRepository.findByUserIdOrderByUpdatedAtDesc(userId);
        }

        List<AIConversationRespDTO> result = new ArrayList<>();
        for (AIConversation conv : list) {
            long count = messageRepository.countByConversationId(conv.getId());
            String lastSnippet = messageRepository.findTopByConversationIdOrderByCreatedAtDesc(conv.getId())
                    .map(m -> {
                        String txt = m.getMessageText();
                        return txt.length() > 80 ? txt.substring(0, 77) + "..." : txt;
                    })
                    .orElse("Chưa có tin nhắn");
            result.add(new AIConversationRespDTO(
                    conv.getId(),
                    conv.getConversationTitle(),
                    conv.getConversationStatus(),
                    conv.getCreatedAt(),
                    conv.getUpdatedAt(),
                    count,
                    lastSnippet
            ));
        }
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public AIConversationDetailRespDTO getConversationDetail(Integer userId, Integer conversationId) {
        AIConversation conversation = conversationRepository.findByIdAndUserId(conversationId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy cuộc hội thoại hoặc bạn không có quyền truy cập."));

        List<AIMessage> messages = messageRepository.findByConversationIdOrderByCreatedAtAsc(conversationId);
        List<AIMessageRespDTO> messageResps = messages.stream()
                .map(m -> AIMessageRespDTO.fromEntity(m, extractSources(m.getMessageText(), m.getSenderType())))
                .toList();

        return new AIConversationDetailRespDTO(
                conversation.getId(),
                conversation.getConversationTitle(),
                conversation.getConversationStatus(),
                conversation.getCreatedAt(),
                conversation.getUpdatedAt(),
                messageResps
        );
    }

    @Override
    @Transactional
    public AIMessageRespDTO askAIAssistant(Integer userId, AskAIQuestionReqDTO req) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng ID: " + userId));

        AIConversation conversation;
        if (req.conversationId() != null) {
            conversation = conversationRepository.findByIdAndUserId(req.conversationId(), userId)
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy cuộc hội thoại ID: " + req.conversationId()));
        } else {
            // Find latest conversation or create new one
            List<AIConversation> existing = conversationRepository.findByUserIdOrderByUpdatedAtDesc(userId);
            if (!existing.isEmpty()) {
                conversation = existing.get(0);
            } else {
                String title = req.question().trim();
                if (title.length() > 40) title = title.substring(0, 37) + "...";
                conversation = new AIConversation(user, null, title);
                conversation = conversationRepository.save(conversation);
            }
        }

        // 1. Lưu tin nhắn của người dùng
        AIMessage userMsg = new AIMessage(conversation, "user", req.question().trim(), null);
        messageRepository.save(userMsg);

        // 2. Thu thập lịch sử hội thoại nhiều lượt (để AI hiểu ngữ cảnh trước sau)
        List<AIMessage> previousMessages = messageRepository.findByConversationIdOrderByCreatedAtAsc(conversation.getId());
        List<Map<String, String>> historyList = new ArrayList<>();
        for (AIMessage m : previousMessages) {
            if (m.getId() != null && !m.getId().equals(userMsg.getId())) {
                historyList.add(Map.of(
                        "role", "user".equalsIgnoreCase(m.getSenderType()) ? "user" : "assistant",
                        "content", m.getMessageText()
                ));
            }
        }

        // 3. RAG: Lấy danh sách tiêu chí đánh giá (Checklist Rules) và cẩm nang từ Database
        Integer farmId = conversation.getFarm() != null ? conversation.getFarm().getId() : null;
        List<ChecklistRule> activeRules = getRelevantChecklistRules(farmId, req.question().trim());
        List<KnowledgeArticle> articles = getRelevantArticles(req.question().trim());

        // 4. Xây dựng System Prompt nạp tài liệu & tiêu chuẩn
        String systemPrompt = buildRAGSystemPrompt(activeRules, articles);

        // 5. Gọi OpenAI API (hoặc fallback nếu chưa cấu hình key/mạng lỗi)
        String aiAnswer;
        String modelUsed;
        List<String> sources = new ArrayList<>();

        Optional<String> openAIAnswerOpt = openAIService != null
                ? openAIService.generateAnswer(systemPrompt, historyList, req.question().trim())
                : Optional.empty();

        if (openAIAnswerOpt.isPresent()) {
            aiAnswer = openAIAnswerOpt.get();
            String rawModel = openAIService.getModel();
            modelUsed = rawModel + (rawModel.toLowerCase().contains("gemini") ? " (Gemini AI RAG)" : " (OpenAI RAG)");

            // Ghi nhận nguồn tài liệu đã trích xuất
            sources.add("TCVN 11892-1:2017 (Quy chuẩn VietGAP Trồng trọt)");
            for (ChecklistRule r : activeRules) {
                sources.add("Tiêu chí VietGAP: " + r.getRuleName() + " (" + (r.getCategory() != null ? r.getCategory() : "Canh tác") + ")");
            }
            for (KnowledgeArticle a : articles) {
                sources.add("Cẩm nang: " + a.getTitle());
            }
        } else {
            // Fallback sang bộ quy tắc tra cứu chuẩn VietGAP nội bộ đảm bảo 100% demo thành công
            aiAnswer = generateVietGAPAdvice(req.question().trim());
            modelUsed = "VietGAP-Expert-Fallback";
            sources = extractSources(aiAnswer, "assistant");
            for (ChecklistRule r : activeRules) {
                if (sources.size() < 4) {
                    sources.add("Tiêu chí: " + r.getRuleName());
                }
            }
        }

        // 6. Lưu tin nhắn trả lời của trợ lý AI
        AIMessage assistantMsg = new AIMessage(conversation, "assistant", aiAnswer, modelUsed);
        messageRepository.save(assistantMsg);

        // 7. Cập nhật thời gian hội thoại
        conversation.setUpdatedAt(Instant.now());
        conversationRepository.save(conversation);

        return AIMessageRespDTO.fromEntity(assistantMsg, sources);
    }

    /**
     * Thu thập các tiêu chí đánh giá VietGAP liên quan đến câu hỏi từ DB.
     */
    private List<ChecklistRule> getRelevantChecklistRules(Integer farmId, String question) {
        if (checklistRuleRepository == null) return List.of();
        try {
            List<ChecklistRule> allRules = farmId != null
                    ? checklistRuleRepository.findActiveByFarmId(farmId)
                    : checklistRuleRepository.findAll();

            if (allRules.isEmpty()) {
                allRules = checklistRuleRepository.findAll();
            }

            String q = question.toLowerCase();
            List<ChecklistRule> matched = new ArrayList<>();

            for (ChecklistRule rule : allRules) {
                if (rule.getIsActive() != null && !rule.getIsActive()) continue;
                String name = (rule.getRuleName() != null ? rule.getRuleName() : "").toLowerCase();
                String desc = (rule.getDescription() != null ? rule.getDescription() : "").toLowerCase();
                String cat = (rule.getCategory() != null ? rule.getCategory() : "").toLowerCase();

                if (q.contains("phân") || q.contains("bón") || q.contains("đạm") || q.contains("dinh dưỡng")) {
                    if (name.contains("phân") || desc.contains("phân") || cat.contains("phân") || cat.contains("dinh dưỡng")) {
                        matched.add(rule);
                    }
                } else if (q.contains("sâu") || q.contains("bệnh") || q.contains("thuốc") || q.contains("bvtv")) {
                    if (name.contains("bvtv") || name.contains("thuốc") || desc.contains("sâu") || cat.contains("bvtv")) {
                        matched.add(rule);
                    }
                } else if (q.contains("nước") || q.contains("tưới") || q.contains("ph")) {
                    if (name.contains("nước") || desc.contains("nước") || cat.contains("nước")) {
                        matched.add(rule);
                    }
                } else if (q.contains("đất") || q.contains("giá thể")) {
                    if (name.contains("đất") || desc.contains("đất") || cat.contains("đất")) {
                        matched.add(rule);
                    }
                } else if (q.contains("thu hoạch") || q.contains("bảo quản")) {
                    if (name.contains("thu hoạch") || desc.contains("thu hoạch") || cat.contains("thu hoạch")) {
                        matched.add(rule);
                    }
                }
            }

            // Nếu không khớp từ khóa chuyên biệt, lấy tối đa 5 tiêu chí tiêu biểu
            if (matched.isEmpty()) {
                return allRules.stream().limit(5).toList();
            }
            return matched.stream().limit(6).toList();
        } catch (Exception e) {
            log.warn("Không thể truy vấn ChecklistRule cho AI context: {}", e.getMessage());
            return List.of();
        }
    }

    /**
     * Thu thập các bài viết cẩm nang kỹ thuật đã xuất bản từ DB.
     */
    private List<KnowledgeArticle> getRelevantArticles(String question) {
        if (knowledgeArticleRepository == null) return List.of();
        try {
            List<KnowledgeArticle> published = knowledgeArticleRepository.findByStatusString("published");
            if (published.isEmpty()) return List.of();

            String q = question.toLowerCase();
            List<KnowledgeArticle> matched = new ArrayList<>();
            for (KnowledgeArticle a : published) {
                String title = (a.getTitle() != null ? a.getTitle() : "").toLowerCase();
                String tags = (a.getTags() != null ? a.getTags() : "").toLowerCase();
                if (q.contains("bọ trĩ") && (title.contains("bọ trĩ") || tags.contains("bọ trĩ"))) {
                    matched.add(a);
                } else if (q.contains("phân") && (title.contains("phân") || tags.contains("phân"))) {
                    matched.add(a);
                } else if (q.contains("nước") && (title.contains("nước") || tags.contains("nước"))) {
                    matched.add(a);
                } else if (q.contains("đất") && (title.contains("đất") || tags.contains("đất"))) {
                    matched.add(a);
                }
            }
            if (matched.isEmpty()) {
                return published.stream().limit(3).toList();
            }
            return matched.stream().limit(3).toList();
        } catch (Exception e) {
            log.warn("Không thể truy vấn KnowledgeArticle cho AI context: {}", e.getMessage());
            return List.of();
        }
    }

    /**
     * Đóng gói System Prompt RAG nạp tài liệu và tiêu chí đánh giá vào OpenAI.
     */
    private String buildRAGSystemPrompt(List<ChecklistRule> rules, List<KnowledgeArticle> articles) {
        StringBuilder sb = new StringBuilder();
        sb.append("Bạn là Trợ lý Cố vấn Nông nghiệp & Chuyên gia Kiểm định VietGAP của nền tảng quản lý trang trại MyCropDiary.\n");
        sb.append("Nhiệm vụ của bạn là giải đáp thắc mắc, hướng dẫn kỹ thuật canh tác và tư vấn đánh giá tuân thủ quy chuẩn VietGAP (TCVN 11892-1:2017) cho nông dân, kỹ sư và chủ trang trại.\n\n");

        sb.append("DƯỚI ĐÂY LÀ BỘ TIÊU CHÍ ĐÁNH GIÁ VIETGAP VÀ CẨM NANG TRI THỨC ĐƯỢC LẤY TRỰC TIẾP TỪ HỆ THỐNG TRANG TRẠI:\n");
        sb.append("============================================================\n");

        if (!rules.isEmpty()) {
            sb.append("[CÁC TIÊU CHÍ ĐÁNH GIÁ VIETGAP CẦN TUÂN THỦ (CHECKLIST RULES)]:\n");
            int idx = 1;
            for (ChecklistRule r : rules) {
                sb.append(idx++).append(". [Nhóm: ").append(r.getCategory() != null ? r.getCategory() : "Chung").append("] ")
                        .append(r.getRuleName()).append(": ")
                        .append(r.getDescription() != null ? r.getDescription() : "Tuân thủ theo quy trình kiểm tra thực địa.")
                        .append("\n");
            }
            sb.append("\n");
        }

        if (!articles.isEmpty()) {
            sb.append("[CẨM NANG TRI THỨC NÔNG NGHIỆP TRANG TRẠI]:\n");
            for (KnowledgeArticle a : articles) {
                sb.append("- Bài viết: ").append(a.getTitle()).append("\n");
                if (a.getSummary() != null && !a.getSummary().isBlank()) {
                    sb.append("  Tóm tắt: ").append(a.getSummary()).append("\n");
                }
            }
            sb.append("\n");
        }

        sb.append("============================================================\n\n");
        sb.append("QUY TẮC PHẢN HỒI:\n");
        sb.append("1. Trả lời một cách chuyên nghiệp, dễ hiểu, thực tế và bám sát các tiêu chuẩn VietGAP TCVN 11892-1:2017.\n");
        sb.append("2. Nếu câu hỏi liên quan đến tiêu chí đánh giá ở trên, hãy trích dẫn cụ thể tên tiêu chí và hướng dẫn cách tuân thủ, ghi chép bằng chứng.\n");
        sb.append("3. Luôn nhấn mạnh các nguyên tắc an toàn: Thời gian cách ly an toàn (PHI), nguyên tắc 4 đúng khi dùng phân/thuốc BVTV, kiểm tra nguồn nước/đất, bảo hộ lao động và ghi chép nhật ký canh tác điện tử.\n");
        sb.append("4. Trình bày rõ ràng theo từng đầu mục 1, 2, 3 có xuống dòng mạch lạc.");

        return sb.toString();
    }

    @Override
    @Transactional
    public void deleteConversation(Integer userId, Integer conversationId) {
        AIConversation conversation = conversationRepository.findByIdAndUserId(conversationId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy cuộc hội thoại ID: " + conversationId));

        List<AIMessage> messages = messageRepository.findByConversationIdOrderByCreatedAtAsc(conversationId);
        if (!messages.isEmpty()) {
            List<Integer> msgIds = messages.stream().map(AIMessage::getId).toList();
            List<AIFeedback> feedbacks = feedbackRepository.findByMessageIdIn(msgIds);
            if (!feedbacks.isEmpty()) {
                feedbackRepository.deleteAll(feedbacks);
            }
            messageRepository.deleteAll(messages);
        }
        conversationRepository.delete(conversation);
    }

    private String generateVietGAPAdvice(String question) {
        String q = question.toLowerCase();
        if (q.contains("bọ trĩ") || q.contains("sâu") || q.contains("rầy") || q.contains("phấn trắng") || q.contains("nấm")) {
            return "Quy trình xử lý sâu bệnh hại sinh học chuẩn VietGAP (IPM):\n\n" +
                    "1. Biện pháp sinh học: Sử dụng nấm đối kháng Chaetomium spp., Trichoderma hoặc tinh dầu sầu đâu (Neem oil) nồng độ 0.3% phun ướt đều 2 mặt lá vào sáng sớm hoặc chiều mát.\n" +
                    "2. Biện pháp cơ học: Căng bẫy dính màu vàng/xanh biển với mật độ 1 bẫy/15-20m2 trong nhà màng để phát hiện và tiêu diệt bọ trĩ trưởng thành.\n" +
                    "3. Thời gian cách ly (PHI): Các chế phẩm sinh học đối kháng có PHI rất ngắn (3-5 ngày), đảm bảo tuyệt đối không tồn dư hóa chất trước thu hoạch.";
        } else if (q.contains("bón") || q.contains("phân") || q.contains("dinh dưỡng") || q.contains("đạm") || q.contains("kali")) {
            return "Quy trình quản lý dinh dưỡng và bón phân theo chuẩn VietGAP:\n\n" +
                    "1. Nguyên tắc 4 đúng: Đúng loại phân, Đúng liều lượng, Đúng thời điểm và Đúng phương pháp.\n" +
                    "2. Kiểm soát dư lượng Nitrate (NO3-): Ngừng bón phân đạm (N) vô cơ tối thiểu 15 ngày trước thu hoạch đối với rau ăn lá và cây ăn quả ngắn ngày.\n" +
                    "3. Phân hữu cơ: Chỉ sử dụng phân chuồng đã qua ủ nóng hoai mục bằng chế phẩm vi sinh Trichoderma đạt tiêu chuẩn vi sinh vật (không chứa vi khuẩn E.coli và Salmonella sống sót).\n" +
                    "4. Ghi chép nhật ký: Bắt buộc ghi chép ngày bón, loại phân, mã lô sản xuất và người bón phân vào nhật ký canh tác.";
        } else if (q.contains("nước") || q.contains("tưới") || q.contains("ph") || q.contains("ec")) {
            return "Tiêu chuẩn nguồn nước tưới tiêu theo Quy chuẩn kỹ thuật Quốc gia QCVN 39:2011/BTNMT:\n\n" +
                    "1. Giới hạn vi sinh: Coliform tổng số < 200 MPN/100ml; E.Coli phải âm tính đối với nước tưới cho rau ăn sống hoặc quả ăn liền.\n" +
                    "2. Kim loại nặng: Hàm lượng Asen (Thạch tín) < 0.05 mg/L, Chì (Pb) < 0.1 mg/L, Cadimi (Cd) < 0.01 mg/L.\n" +
                    "3. Thông số kỹ thuật canh tác: Duy trì độ pH nước tưới từ 6.0 - 6.8; Chỉ số dẫn điện EC từ 1.2 - 1.8 mS/cm tùy theo từng giai đoạn sinh trưởng của cây trồng.";
        } else if (q.contains("đất") || q.contains("giá thể") || q.contains("kim loại")) {
            return "Quy định đánh giá chất lượng đất và giá thể canh tác theo VietGAP:\n\n" +
                    "1. Khảo sát tiền sử vùng sản xuất: Không canh tác trên vùng đất từng là bãi rác, kho hóa chất, nghĩa trang hoặc nước thải công nghiệp chưa qua xử lý.\n" +
                    "2. Đánh giá mẫu đất định kỳ: Kiểm nghiệm dư lượng kim loại nặng (Pb, Cd, As) định kỳ hàng năm theo QCVN 03-MT:2015/BTNMT.\n" +
                    "3. Vùng đệm cách ly: Nếu tiếp giáp vùng canh tác truyền thống có nguy cơ trôi dạt hóa chất, phải thiết lập rào chắn sinh học hoặc dải đệm tối thiểu 3 - 5 mét.";
        } else if (q.contains("thu hoạch") || q.contains("bảo quản") || q.contains("đóng gói")) {
            return "Quy trình thu hoạch và xử lý sau thu hoạch chuẩn VietGAP:\n\n" +
                    "1. Thời gian cách ly: Bắt buộc kiểm tra sổ theo dõi PHI trước khi quyết định ngày thu hái.\n" +
                    "2. Dụng cụ chứa đựng: Sử dụng thùng nhựa nguyên sinh chuyên dụng đã rửa sạch và khử khuẩn. Tuyệt đối không để nông sản tiếp xúc trực tiếp với mặt đất.\n" +
                    "3. Sơ chế & đóng gói: Nước rửa nông sản phải là nước sạch đạt quy chuẩn nước sinh hoạt QCVN 01-1:2018/BYT. Dán tem truy xuất nguồn gốc đầy đủ thông tin trang trại và lô thu hoạch.";
        }

        return "Căn cứ theo tiêu chuẩn VietGAP TCVN 11892-1:2017:\n\n" +
                "Mọi hoạt động canh tác tại trang trại cần tuân thủ nguyên tắc an toàn thực phẩm, bảo vệ môi trường, bảo vệ sức khỏe người lao động và truy xuất nguồn gốc rõ ràng.\n" +
                "Bạn có thể đặt thêm câu hỏi chi tiết về: 1) Phòng trừ sâu bệnh sinh học, 2) Công thức bón phân theo giai đoạn, 3) Tiêu chuẩn kiểm nghiệm đất/nước, hoặc 4) Quy trình thu hoạch và ghi chép nhật ký điện tử.";
    }

    private List<String> extractSources(String text, String senderType) {
        if (!"assistant".equalsIgnoreCase(senderType)) return List.of();
        List<String> sources = new ArrayList<>();
        sources.add("TCVN 11892-1:2017 (Quy chuẩn VietGAP)");
        if (text.contains("QCVN 39")) {
            sources.add("QCVN 39:2011/BTNMT (Chất lượng nước tưới tiêu)");
        }
        if (text.contains("QCVN 03")) {
            sources.add("QCVN 03-MT:2015/BTNMT (Dư lượng kim loại nặng trong đất)");
        }
        if (text.contains("sinh học") || text.contains("IPM") || text.contains("Neem")) {
            sources.add("Danh mục thuốc BVTV sinh học được phép sử dụng tại VN");
        }
        return sources;
    }

    @Override
    public FarmingDraftRespDTO parseVoiceActivity(GenerateDraftReqDTO req) {
        String text = req.text() != null ? req.text().trim() : "";
        if (text.isEmpty()) {
            return new FarmingDraftRespDTO("Other", "Hoạt động canh tác", null, "", "Trời nắng nhẹ", "");
        }

        List<String> blocks = req.availableBlocks() != null ? req.availableBlocks() : List.of();

        if (openAIService != null && openAIService.isConfigured()) {
            try {
                String systemPrompt = "Bạn là trợ lý AI của hệ thống MyCropDiary. " +
                        "Nhiệm vụ của bạn là bóc tách lời nói của nông dân/kỹ thuật viên thành dữ liệu JSON để tự động điền vào form hoạt động canh tác.\n" +
                        "Các loại hoạt động hợp lệ (activityType): Fertilizing (Bón phân), Spraying (Phun thuốc), Watering (Tưới nước), Harvesting (Thu hoạch), Planting (Gieo trồng), Pruning (Cắt tỉa/làm cỏ), SoilCare (Xử lý đất), Other (Khác).\n" +
                        "Danh sách lô đất hiện có của mùa vụ: " + String.join(", ", blocks) + ".\n" +
                        "Hãy trả về JSON thuần túy (không kèm markdown ```json): " +
                        "{\"activityType\": \"...\", \"activityName\": \"...\", \"matchedBlockName\": \"...\", \"description\": \"...\", \"weatherNotes\": \"...\", \"resultNotes\": \"...\"}";

                Optional<String> aiAnswer = openAIService.generateAnswer(systemPrompt, List.of(), text);
                if (aiAnswer.isPresent()) {
                    String raw = aiAnswer.get().trim();
                    if (raw.startsWith("```json")) raw = raw.substring(7);
                    if (raw.startsWith("```")) raw = raw.substring(3);
                    if (raw.endsWith("```")) raw = raw.substring(0, raw.length() - 3);
                    raw = raw.trim();

                    com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                    com.fasterxml.jackson.databind.JsonNode root = mapper.readTree(raw);
                    String actType = root.hasNonNull("activityType") ? root.get("activityType").asText() : "Fertilizing";
                    String actName = root.hasNonNull("activityName") ? root.get("activityName").asText() : text;
                    String matchedBlock = root.hasNonNull("matchedBlockName") ? root.get("matchedBlockName").asText() : null;
                    String desc = root.hasNonNull("description") ? root.get("description").asText() : text;
                    String weather = root.hasNonNull("weatherNotes") ? root.get("weatherNotes").asText() : "Trời nắng nhẹ";
                    String result = root.hasNonNull("resultNotes") ? root.get("resultNotes").asText() : "";

                    return new FarmingDraftRespDTO(actType, actName, matchedBlock, desc, weather, result);
                }
            } catch (Exception e) {
                log.warn("[AI Voice Parser] Lỗi phân tích bằng AI, fallback sang heuristic regex: {}", e.getMessage());
            }
        }

        // Fallback Heuristic parsing
        String lower = text.toLowerCase();
        String actType = "Other";
        String actName = "Hoạt động canh tác";

        if (lower.contains("bón") || lower.contains("phân") || lower.contains("đạm") || lower.contains("lân") || lower.contains("kali") || lower.contains("npk")) {
            actType = "Fertilizing";
            actName = "Bón phân bổ sung dinh dưỡng";
        } else if (lower.contains("phun") || lower.contains("thuốc") || lower.contains("trừ sâu") || lower.contains("nấm") || lower.contains("diệt cỏ")) {
            actType = "Spraying";
            actName = "Phun chế phẩm phòng trừ dịch hại";
        } else if (lower.contains("tưới") || lower.contains("nước") || lower.contains("nhỏ giọt") || lower.contains("ẩm")) {
            actType = "Watering";
            actName = "Tưới nước giữ ẩm";
        } else if (lower.contains("thu hoạch") || lower.contains("cắt củ") || lower.contains("hái quả")) {
            actType = "Harvesting";
            actName = "Thu hoạch nông sản";
        } else if (lower.contains("gieo") || lower.contains("trồng") || lower.contains("cấy") || lower.contains("hạt")) {
            actType = "Planting";
            actName = "Gieo trồng cây giống vụ mới";
        } else if (lower.contains("tỉa") || lower.contains("làm cỏ") || lower.contains("bấm ngọn")) {
            actType = "Pruning";
            actName = "Cắt tỉa nhánh và làm sạch cỏ dại";
        }

        // Tìm lô đất xuất hiện trong văn bản
        String matchedBlock = null;
        for (String block : blocks) {
            if (lower.contains(block.toLowerCase())) {
                matchedBlock = block;
                break;
            }
        }

        String weather = "Trời nắng nhẹ, độ ẩm 75%";
        if (lower.contains("mưa")) weather = "Trời mưa nhỏ, đất ẩm";
        else if (lower.contains("nắng")) weather = "Trời nắng ráo, thông thoáng";
        else if (lower.contains("âm u") || lower.contains("mát")) weather = "Trời râm mát";

        return new FarmingDraftRespDTO(actType, actName, matchedBlock, text, weather, "Ghi nhận nhanh qua giọng nói");
    }
}
