package com.mycropdiary.api.dto.farm;

import com.mycropdiary.api.dto.block.BlockRespDTO;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * DTO chi tiết Vùng canh tác bao gồm danh sách các Lô / Thửa (Block) bên trong (UC-16.2 & UC-16.6).
 */
public record ZoneDetailRespDTO(
        Integer zoneId,
        Integer farmId,
        String zoneName,
        BigDecimal areaValue,
        String areaUnit,
        String description,
        String imageUrl,
        String status,
        Instant createdAt,
        Instant updatedAt,
        int totalBlocks,
        List<BlockRespDTO> blocks
) {}
