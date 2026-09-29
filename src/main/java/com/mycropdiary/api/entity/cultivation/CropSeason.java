package com.mycropdiary.api.entity.cultivation;

import com.mycropdiary.api.entity.Farm;
import com.mycropdiary.api.entity.FarmMember;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Entity biểu diễn bảng dbo.CropSeason (Mùa vụ canh tác nông nghiệp).
 */
@Entity
@Table(name = "CropSeason", schema = "dbo", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"FarmID", "SeasonCode"})
})
public class CropSeason {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "CropSeasonID")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "FarmID", nullable = false)
    private Farm farm;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "PlotID", nullable = false)
    private Plot plot;

    @Column(name = "CropCategoryID", nullable = false)
    private Long cropCategoryId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "CreatedByMemberID", nullable = false)
    private FarmMember createdByMember;

    @Column(name = "SeasonCode", nullable = false, length = 40)
    private String seasonCode;

    @Column(name = "SeasonName", nullable = false, length = 200)
    private String seasonName;

    @Column(name = "VarietyName", length = 150)
    private String varietyName;

    @Column(name = "StartDate", nullable = false)
    private LocalDate startDate;

    @Column(name = "ExpectedHarvestDate")
    private LocalDate expectedHarvestDate;

    @Column(name = "ActualEndDate")
    private LocalDate actualEndDate;

    @Column(name = "CultivatedAreaM2", precision = 18, scale = 2)
    private BigDecimal cultivatedAreaM2;

    @Column(name = "Status", nullable = false, length = 20)
    private String status = "PLANNED"; // PLANNED, ACTIVE, HARVESTING, COMPLETED, CANCELLED

    @Column(name = "Notes", length = 2000)
    private String notes;

    public CropSeason() { }

    public CropSeason(Farm farm, Plot plot, Long cropCategoryId, FarmMember createdByMember,
                      String seasonCode, String seasonName, LocalDate startDate) {
        this.farm = farm;
        this.plot = plot;
        this.cropCategoryId = cropCategoryId;
        this.createdByMember = createdByMember;
        this.seasonCode = seasonCode;
        this.seasonName = seasonName;
        this.startDate = startDate;
        this.status = "ACTIVE";
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Farm getFarm() { return farm; }
    public void setFarm(Farm farm) { this.farm = farm; }

    public Plot getPlot() { return plot; }
    public void setPlot(Plot plot) { this.plot = plot; }

    public Long getCropCategoryId() { return cropCategoryId; }
    public void setCropCategoryId(Long cropCategoryId) { this.cropCategoryId = cropCategoryId; }

    public FarmMember getCreatedByMember() { return createdByMember; }
    public void setCreatedByMember(FarmMember createdByMember) { this.createdByMember = createdByMember; }

    public String getSeasonCode() { return seasonCode; }
    public void setSeasonCode(String seasonCode) { this.seasonCode = seasonCode; }

    public String getSeasonName() { return seasonName; }
    public void setSeasonName(String seasonName) { this.seasonName = seasonName; }

    public String getVarietyName() { return varietyName; }
    public void setVarietyName(String varietyName) { this.varietyName = varietyName; }

    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }

    public LocalDate getExpectedHarvestDate() { return expectedHarvestDate; }
    public void setExpectedHarvestDate(LocalDate expectedHarvestDate) { this.expectedHarvestDate = expectedHarvestDate; }

    public LocalDate getActualEndDate() { return actualEndDate; }
    public void setActualEndDate(LocalDate actualEndDate) { this.actualEndDate = actualEndDate; }

    public BigDecimal getCultivatedAreaM2() { return cultivatedAreaM2; }
    public void setCultivatedAreaM2(BigDecimal cultivatedAreaM2) { this.cultivatedAreaM2 = cultivatedAreaM2; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
