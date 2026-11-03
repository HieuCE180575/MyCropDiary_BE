package com.mycropdiary.api.service.impl;

import com.mycropdiary.api.dto.ai.*;
import com.mycropdiary.api.entity.*;
import com.mycropdiary.api.exception.BadRequestException;
import com.mycropdiary.api.exception.ResourceNotFoundException;
import com.mycropdiary.api.repository.*;
import com.mycropdiary.api.service.AIAssistantService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
public class AIAssistantServiceImpl implements AIAssistantService {

    private final AIConversationRepository conversationRepository;
    private final AIMessageRepository messageRepository;
    private final AIFeedbackRepository feedbackRepository;
    private final UserRepository userRepository;
    private final FarmRepository farmRepository;

    public AIAssistantServiceImpl(
            AIConversationRepository conversationRepository,
            AIMessageRepository messageRepository,
            AIFeedbackRepository feedbackRepository,
            UserRepository userRepository,
            FarmRepository farmRepository) {
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.feedbackRepository = feedbackRepository;
        this.userRepository = userRepository;
        this.farmRepository = farmRepository;
    }

    @Override
    @Transactional
    public AIConversationResp createConversation(Integer userId, CreateAIConversationReq req) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng ID: " + userId));
        Farm farm = req.farmId() != null ? farmRepository.findById(req.farmId()).orElse(null) : null;

        AIConversation conversation = new AIConversation(user, farm, req.title().trim());
        AIConversation saved = conversationRepository.save(conversation);

        // Welcome greeting message
        AIMessage welcomeMsg = new AIMessage(
                saved,
                "assistant",
                "Xin chào! Tôi là Trợ lý AI nông nghiệp VietGAP. Tôi có thể hỗ trợ bạn chẩn đoán sâu bệnh, tư vấn danh mục thuốc BVTV sinh học, quy trình bón phân và nhật ký canh tác chuẩn TCVN 11892-1:2017. Bạn cần hỗ trợ gì hôm nay?",
                "VietGAP-LLM-v2"
        );
        messageRepository.save(welcomeMsg);

        return new AIConversationResp(
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
    public List<AIConversationResp> getMyConversations(Integer userId, String keyword) {
        List<AIConversation> list;
        if (keyword != null && !keyword.trim().isEmpty()) {
            list = conversationRepository.searchByUserIdAndKeyword(userId, keyword.trim());
        } else {
            list = conversationRepository.findByUserIdOrderByUpdatedAtDesc(userId);
        }

        List<AIConversationResp> result = new ArrayList<>();
        for (AIConversation conv : list) {
            long count = messageRepository.countByConversationId(conv.getId());
            String lastSnippet = messageRepository.findTopByConversationIdOrderByCreatedAtDesc(conv.getId())
                    .map(m -> {
                        String txt = m.getMessageText();
                        return txt.length() > 80 ? txt.substring(0, 77) + "..." : txt;
                    })
                    .orElse("Chưa có tin nhắn");
            result.add(new AIConversationResp(
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
    public AIConversationDetailResp getConversationDetail(Integer userId, Integer conversationId) {
        AIConversation conversation = conversationRepository.findByIdAndUserId(conversationId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy cuộc hội thoại hoặc bạn không có quyền truy cập."));

        List<AIMessage> messages = messageRepository.findByConversationIdOrderByCreatedAtAsc(conversationId);
        List<AIMessageResp> messageResps = messages.stream()
                .map(m -> AIMessageResp.fromEntity(m, extractSources(m.getMessageText(), m.getSenderType())))
                .toList();

        return new AIConversationDetailResp(
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
    public AIMessageResp askAIAssistant(Integer userId, AskAIQuestionReq req) {
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

        // 1. Save user message
        AIMessage userMsg = new AIMessage(conversation, "user", req.question().trim(), null);
        messageRepository.save(userMsg);

        // 2. Generate intelligent VietGAP answer
        String aiAnswer = generateVietGAPAdvice(req.question().trim());
        List<String> sources = extractSources(aiAnswer, "assistant");

        // 3. Save assistant message
        AIMessage assistantMsg = new AIMessage(conversation, "assistant", aiAnswer, "VietGAP-LLM-v2");
        messageRepository.save(assistantMsg);

        // 4. Update conversation updated_at
        conversation.setUpdatedAt(Instant.now());
        conversationRepository.save(conversation);

        return AIMessageResp.fromEntity(assistantMsg, sources);
    }

    @Override
    @Transactional
    public AIFeedbackResp reportAIMessage(Integer userId, Integer messageId, ReportAIMessageReq req) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng ID: " + userId));
        AIMessage message = messageRepository.findById(messageId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tin nhắn AI ID: " + messageId));

        if (!"assistant".equalsIgnoreCase(message.getSenderType())) {
            throw new BadRequestException("Chỉ có thể phản hồi hoặc báo cáo các câu trả lời từ AI.");
        }

        AIFeedback feedback = feedbackRepository.findByMessageIdAndUserId(messageId, userId)
                .orElseGet(() -> new AIFeedback(message, user, req.rating(), req.reason().trim()));
        feedback.setRating(req.rating());
        feedback.setFeedbackText(req.reason().trim());

        AIFeedback saved = feedbackRepository.save(feedback);

        // Automatically acknowledge
        if (saved.getReplies().isEmpty()) {
            AIFeedbackReply acknowledgment = new AIFeedbackReply(
                    saved,
                    null,
                    "Ban cố vấn chuyên gia VietGAP của MyCropDiary đã tiếp nhận phản hồi của bạn để kiểm định lại dữ liệu tri thức. Cảm ơn đóng góp của bạn!"
            );
            saved.getReplies().add(acknowledgment);
            saved = feedbackRepository.save(saved);
        }

        return AIFeedbackResp.fromEntity(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AIFeedbackResp> getMyFeedbackHistory(Integer userId) {
        List<AIFeedback> list = feedbackRepository.findByUserIdOrderByCreatedAtDesc(userId);
        return list.stream().map(AIFeedbackResp::fromEntity).toList();
    }

    private String generateVietGAPAdvice(String question) {
        String q = question.toLowerCase();
        if (q.contains("sâu") || q.contains("bọ trĩ") || q.contains("rầy") || q.contains("nhện đỏ") || q.contains("dịch hại")) {
            return "Theo tiêu chuẩn Quốc gia TCVN 11892-1:2017 về quản lý dịch hại tổng hợp (IPM) trên cây trồng:\n\n" +
                    "1. Biện pháp sinh học ưu tiên: Phun chế phẩm sinh học chứa nấm ký sinh Beauveria bassiana hoặc Metarhizium anisopliae. Đối với bọ trĩ và nhện đỏ, sử dụng dầu khoáng nông nghiệp (SK Enspray 99EC) hoặc tinh dầu Neem nồng độ 0.2 - 0.3% lúc trời râm mát.\n" +
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
}

