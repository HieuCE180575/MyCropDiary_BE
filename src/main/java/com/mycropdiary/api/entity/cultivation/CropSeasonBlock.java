package com.mycropdiary.api.entity.cultivation;

import com.mycropdiary.api.entity.Block;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Entity biểu diễn bảng [CropSeasonBlock] (Gán lô đất vào mùa vụ canh tác).
 * Quan hệ Many-to-Many giữa CropSeason và Block.
 */
@Entity
@Table(name = "CropSeasonBlock")
public class CropSeasonBlock {

    @EmbeddedId
    private CropSeasonBlockId id = new CropSeasonBlockId();

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("cropSeasonId")
    @JoinColumn(name = "crop_season_id", nullable = false)
    private CropSeason cropSeason;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("blockId")
    @JoinColumn(name = "block_id", nullable = false)
    private Block block;

    @Column(name = "planted_area", precision = 12, scale = 2)
    private BigDecimal plantedArea;

    @Column(name = "area_unit", length = 20)
    private String areaUnit = "m2";

    @Column(name = "planted_at")
    private LocalDate plantedAt;

    @Column(name = "expected_harvest_at")
    private LocalDate expectedHarvestAt;

    public CropSeasonBlock() {
    }

    public CropSeasonBlock(CropSeason cropSeason, Block block, BigDecimal plantedArea, String areaUnit, LocalDate plantedAt, LocalDate expectedHarvestAt) {
        this.cropSeason = cropSeason;
        this.block = block;
        this.id = new CropSeasonBlockId(cropSeason != null ? cropSeason.getId() : null, block != null ? block.getId() : null);
        this.plantedArea = plantedArea;
        this.areaUnit = areaUnit != null ? areaUnit : "m2";
        this.plantedAt = plantedAt;
        this.expectedHarvestAt = expectedHarvestAt;
    }

    public CropSeasonBlockId getId() {
        return id;
    }

    public void setId(CropSeasonBlockId id) {
        this.id = id;
    }

    public CropSeason getCropSeason() {
        return cropSeason;
    }

    public void setCropSeason(CropSeason cropSeason) {
        this.cropSeason = cropSeason;
        if (cropSeason != null) {
            if (this.id == null) {
                this.id = new CropSeasonBlockId();
            }
            this.id.setCropSeasonId(cropSeason.getId());
        }
    }

    public Block getBlock() {
        return block;
    }

    public void setBlock(Block block) {
        this.block = block;
        if (block != null) {
            if (this.id == null) {
                this.id = new CropSeasonBlockId();
            }
            this.id.setBlockId(block.getId());
        }
    }

    public BigDecimal getPlantedArea() {
        return plantedArea;
    }

    public void setPlantedArea(BigDecimal plantedArea) {
        this.plantedArea = plantedArea;
    }

    public String getAreaUnit() {
        return areaUnit;
    }

    public void setAreaUnit(String areaUnit) {
        this.areaUnit = areaUnit;
    }

    public LocalDate getPlantedAt() {
        return plantedAt;
    }

    public void setPlantedAt(LocalDate plantedAt) {
        this.plantedAt = plantedAt;
    }

    public LocalDate getExpectedHarvestAt() {
        return expectedHarvestAt;
    }

    public void setExpectedHarvestAt(LocalDate expectedHarvestAt) {
        this.expectedHarvestAt = expectedHarvestAt;
    }
}
