package com.mycropdiary.api.entity.cultivation;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class CropSeasonBlockId implements Serializable {

    @Column(name = "crop_season_id")
    private Integer cropSeasonId;

    @Column(name = "block_id")
    private Integer blockId;

    public CropSeasonBlockId() {
    }

    public CropSeasonBlockId(Integer cropSeasonId, Integer blockId) {
        this.cropSeasonId = cropSeasonId;
        this.blockId = blockId;
    }

    public Integer getCropSeasonId() {
        return cropSeasonId;
    }

    public void setCropSeasonId(Integer cropSeasonId) {
        this.cropSeasonId = cropSeasonId;
    }

    public Integer getBlockId() {
        return blockId;
    }

    public void setBlockId(Integer blockId) {
        this.blockId = blockId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CropSeasonBlockId that = (CropSeasonBlockId) o;
        return Objects.equals(cropSeasonId, that.cropSeasonId) &&
                Objects.equals(blockId, that.blockId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(cropSeasonId, blockId);
    }
}
