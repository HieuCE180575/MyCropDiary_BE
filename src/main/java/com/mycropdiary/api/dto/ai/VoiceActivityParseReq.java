package com.mycropdiary.api.dto.ai;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import java.util.List;

@Schema(description = "Request bóc tách thông tin hoạt động canh tác từ giọng nói/văn bản tự nhiên")
public record VoiceActivityParseReq(
        @NotBlank(message = "Nội dung giọng nói không được để trống")
        @Schema(description = "Nội dung văn bản nhận diện từ giọng nói người dùng", example = "Hôm nay bón phân hữu cơ vi sinh 20kg cho lô A1, thời tiết nắng ráo, cách ly 7 ngày")
        String text,

        @Schema(description = "Danh sách tên các lô đất hiện có của mùa vụ để đối chiếu", example = "[\"Lô A1\", \"Lô B2\", \"Lô C3\"]")
        List<String> availableBlocks
) {
}
