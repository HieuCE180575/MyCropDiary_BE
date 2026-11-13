package com.mycropdiary.api.service.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;

/**
 * Service tích hợp OpenAI Chat Completions API (gpt-4o-mini)
 * Hỗ trợ cơ chế RAG nạp ngữ cảnh tiêu chuẩn VietGAP & cơ chế Fallback an toàn.
 */
@Service
public class OpenAIService {

    private static final Logger log = LoggerFactory.getLogger(OpenAIService.class);

    private final String apiKey;
    private final String model;
    private final String baseUrl;
    private final int timeoutSeconds;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public OpenAIService(
            @Value("${openai.api-key:}") String apiKey,
            @Value("${openai.model:gemini-flash-lite-latest}") String model,
            @Value("${openai.base-url:https://generativelanguage.googleapis.com/v1beta/openai}") String baseUrl,
            @Value("${openai.timeout-seconds:25}") int timeoutSeconds,
            @org.springframework.beans.factory.annotation.Autowired(required = false) ObjectMapper objectMapper) {
        String resolvedKey = apiKey != null ? apiKey.trim() : "";
        if (resolvedKey.isBlank() || resolvedKey.startsWith("YOUR_") || resolvedKey.equalsIgnoreCase("sk-placeholder")) {
            resolvedKey = resolveFallbackKey();
        }
        this.apiKey = resolvedKey;
        this.model = model != null && !model.isBlank() ? model.trim() : "gemini-flash-lite-latest";
        this.baseUrl = baseUrl != null && !baseUrl.isBlank() ? baseUrl.trim().replaceAll("/+$", "") : "https://generativelanguage.googleapis.com/v1beta/openai";
        this.timeoutSeconds = timeoutSeconds > 0 ? timeoutSeconds : 25;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.objectMapper = objectMapper != null ? objectMapper : new ObjectMapper().findAndRegisterModules();
        if (isConfigured()) {
            log.info("[OpenAI/Gemini] Khởi tạo AI Service thành công. Base URL: {}, Model: {}", this.baseUrl, this.model);
        } else {
            log.warn("[OpenAI/Gemini] Chưa cấu hình API Key. Sẽ dùng cơ chế Fallback nội bộ.");
        }
    }

    private static String resolveFallbackKey() {
        String envKey = System.getenv("OPENAI_API_KEY");
        if (envKey != null && !envKey.isBlank()) return envKey.trim();

        String geminiEnv = System.getenv("GEMINI_API_KEY");
        if (geminiEnv != null && !geminiEnv.isBlank()) return geminiEnv.trim();

        for (String filename : List.of(".env.local", ".env")) {
            for (String dir : List.of(".", "..", "dev/MyCropDiary_BE")) {
                try {
                    java.nio.file.Path p = java.nio.file.Path.of(dir, filename);
                    if (java.nio.file.Files.exists(p)) {
                        for (String line : java.nio.file.Files.readAllLines(p)) {
                            String trimmed = line.trim();
                            if (trimmed.startsWith("OPENAI_API_KEY=") || trimmed.startsWith("openai.api-key=")) {
                                String val = trimmed.substring(trimmed.indexOf('=') + 1).trim();
                                if (!val.isBlank() && !val.startsWith("YOUR_")) return val;
                            }
                        }
                    }
                } catch (Exception ignored) {
                }
            }
        }
        return "";
    }

    /**
     * Kiểm tra xem cấu hình OpenAI API Key đã được thiết lập hay chưa.
     */
    public boolean isConfigured() {
        return !apiKey.isEmpty() && !apiKey.startsWith("YOUR_") && !apiKey.equalsIgnoreCase("sk-placeholder");
    }

    public String getModel() {
        return model;
    }

    /**
     * Gửi câu hỏi kèm System Prompt chứa dữ liệu tiêu chuẩn VietGAP và lịch sử đối thoại đến OpenAI.
     *
     * @param systemPrompt Ngữ cảnh quy chuẩn VietGAP, Checklist Rules và cẩm nang kỹ thuật
     * @param conversationHistory Danh sách tin nhắn trước đó (role: user / assistant, content)
     * @param userQuestion Câu hỏi mới nhất của người dùng
     * @return Optional chứa câu trả lời từ OpenAI nếu thành công, hoặc Optional.empty() nếu có lỗi/chưa có key
     */
    public Optional<String> generateAnswer(
            String systemPrompt,
            List<Map<String, String>> conversationHistory,
            String userQuestion) {

        if (!isConfigured()) {
            log.warn("[OpenAI/Gemini] API key chưa được cấu hình. Chuyển sang cơ chế Fallback nội bộ.");
            return Optional.empty();
        }

        try {
            List<Map<String, String>> messages = new ArrayList<>();

            // 1. System Prompt chứa toàn bộ tài liệu & tiêu chí
            if (systemPrompt != null && !systemPrompt.isBlank()) {
                messages.add(Map.of("role", "system", "content", systemPrompt.trim()));
            }

            // 2. Lịch sử hội thoại nhiều lượt (giới hạn 6 tin gần nhất để tiết kiệm token)
            if (conversationHistory != null) {
                int start = Math.max(0, conversationHistory.size() - 6);
                for (int i = start; i < conversationHistory.size(); i++) {
                    messages.add(conversationHistory.get(i));
                }
            }

            // 3. Câu hỏi hiện tại của người dùng
            messages.add(Map.of("role", "user", "content", userQuestion.trim()));

            // Chuẩn bị danh sách model ưu tiên (Model chính và fallback model nếu có)
            List<String> candidateModels = new ArrayList<>();
            candidateModels.add(this.model);
            if (this.model.toLowerCase().contains("gemini")) {
                for (String alt : List.of("gemini-flash-lite-latest", "gemini-2.0-flash", "gemini-1.5-flash", "gemini-2.5-flash", "gemini-3.5-flash-lite")) {
                    if (!candidateModels.contains(alt)) {
                        candidateModels.add(alt);
                    }
                }
            }

            for (String candidateModel : candidateModels) {
                Optional<String> answer = executeRequest(candidateModel, messages);
                if (answer.isPresent()) {
                    return answer;
                }
            }
        } catch (Exception e) {
            log.error("[OpenAI/Gemini] Ngoại lệ khi kết nối AI API: {}", e.getMessage(), e);
        }

        return Optional.empty();
    }

    private Optional<String> executeRequest(String targetModel, List<Map<String, String>> messages) {
        try {
            Map<String, Object> requestPayload = new LinkedHashMap<>();
            requestPayload.put("model", targetModel);
            requestPayload.put("messages", messages);
            requestPayload.put("temperature", 0.4);
            requestPayload.put("max_tokens", 1200);

            String requestBodyJson = objectMapper.writeValueAsString(requestPayload);

            HttpRequest.Builder reqBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/chat/completions"))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(timeoutSeconds));

            if (baseUrl.contains("googleapis.com")) {
                reqBuilder.header("x-goog-api-key", apiKey);
            }

            HttpRequest request = reqBuilder
                    .POST(HttpRequest.BodyPublishers.ofString(requestBodyJson))
                    .build();

            log.info("[OpenAI/Gemini] Đang gọi {} với model {} (số lượng messages: {})...", baseUrl, targetModel, messages.size());
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                JsonNode root = objectMapper.readTree(response.body());
                JsonNode choices = root.path("choices");
                if (choices.isArray() && !choices.isEmpty()) {
                    String answer = choices.get(0).path("message").path("content").asText();
                    if (answer != null && !answer.isBlank()) {
                        log.info("[OpenAI/Gemini] Model {} phản hồi thành công (độ dài: {} ký tự).", targetModel, answer.length());
                        return Optional.of(answer.trim());
                    }
                }
            } else {
                log.warn("[OpenAI/Gemini] Lỗi từ API với model {} (Status {}): {}", targetModel, response.statusCode(), response.body());
            }
        } catch (Exception ex) {
            log.warn("[OpenAI/Gemini] Thất bại khi gọi model {}: {}", targetModel, ex.getMessage());
        }
        return Optional.empty();
    }
}

