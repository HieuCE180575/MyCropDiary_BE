package com.mycropdiary.api.entity;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * Entity biểu diễn bảng [BlockConditionObservation] (Ghi nhận tình trạng lô đất - UC-17.4).
 */
@Entity
@Table(name = "BlockConditionObservation")
public class BlockConditionObservation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "observation_id", updatable = false, nullable = false)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "block_id", nullable = false)
    private Block block;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recorded_by")
    private FarmMember recordedBy;

    @Column(name = "condition_type", length = 80)
    private String conditionType;

    @Column(name = "description", nullable = false, columnDefinition = "NVARCHAR(MAX)")
    private String description;

    @Column(name = "image_url", columnDefinition = "NVARCHAR(MAX)")
    private String imageUrl;

    @Column(name = "observed_at", nullable = false)
    private Instant observedAt;

    @PrePersist
    void onCreate() {
        if (observedAt == null) {
            observedAt = Instant.now();
        }
    }

    public BlockConditionObservation() {}

    public BlockConditionObservation(Block block, FarmMember recordedBy, String conditionType, String description, String imageUrl, Instant observedAt) {
        this.block = block;
        this.recordedBy = recordedBy;
        this.conditionType = conditionType;
        this.description = description;
        this.imageUrl = imageUrl;
        this.observedAt = observedAt != null ? observedAt : Instant.now();
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public Block getBlock() { return block; }
    public void setBlock(Block block) { this.block = block; }

    public FarmMember getRecordedBy() { return recordedBy; }
    public void setRecordedBy(FarmMember recordedBy) { this.recordedBy = recordedBy; }

    public String getConditionType() { return conditionType; }
    public void setConditionType(String conditionType) { this.conditionType = conditionType; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public Instant getObservedAt() { return observedAt; }
    public void setObservedAt(Instant observedAt) { this.observedAt = observedAt; }
}
