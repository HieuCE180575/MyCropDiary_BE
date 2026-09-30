package com.mycropdiary.api.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;

/**
 * Entity biểu diễn bảng dbo.Plot (Thửa đất / Lô sản xuất).
 */
@Entity
@Table(name = "Plot", schema = "dbo", uniqueConstraints = {
        @UniqueConstraint(name = "UQ_Plot_Code", columnNames = {"ProductionAreaID", "PlotCode"})
})
// [AI_CHANGE] Root cause: Bảng dbo.Plot trong SQL Server không có cột CreatedAt/UpdatedAt, kế thừa BaseEntity gây lỗi "Invalid column name 'CreatedAt'".
// [AI_CHANGE] Mechanism: Bỏ kế thừa BaseEntity để khớp 100% với bảng dbo.Plot.
public class Plot {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "PlotID")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ProductionAreaID", nullable = false)
    private ProductionArea productionArea;

    @Column(name = "PlotCode", nullable = false, length = 30)
    private String plotCode;

    @Column(name = "PlotName", nullable = false, length = 150)
    private String plotName;

    @Column(name = "AreaM2", nullable = false, precision = 18, scale = 2)
    private BigDecimal areaM2;

    @Column(name = "Latitude", precision = 9, scale = 6)
    private BigDecimal latitude;

    @Column(name = "Longitude", precision = 9, scale = 6)
    private BigDecimal longitude;

    @Column(name = "BoundaryGeoJson", columnDefinition = "NVARCHAR(MAX)")
    private String boundaryGeoJson;

    @Column(name = "Status", nullable = false, length = 20)
    private String status = "AVAILABLE"; // AVAILABLE, IN_USE, RESTING, INACTIVE

    public Plot() { }

    public Plot(ProductionArea productionArea, String plotCode, String plotName, BigDecimal areaM2, BigDecimal latitude, BigDecimal longitude, String boundaryGeoJson, String status) {
        this.productionArea = productionArea;
        this.plotCode = plotCode;
        this.plotName = plotName;
        this.areaM2 = areaM2;
        this.latitude = latitude;
        this.longitude = longitude;
        this.boundaryGeoJson = boundaryGeoJson;
        this.status = status != null ? status : "AVAILABLE";
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public ProductionArea getProductionArea() { return productionArea; }
    public void setProductionArea(ProductionArea productionArea) { this.productionArea = productionArea; }
    public String getPlotCode() { return plotCode; }
    public void setPlotCode(String plotCode) { this.plotCode = plotCode; }
    public String getPlotName() { return plotName; }
    public void setPlotName(String plotName) { this.plotName = plotName; }
    public BigDecimal getAreaM2() { return areaM2; }
    public void setAreaM2(BigDecimal areaM2) { this.areaM2 = areaM2; }
    public BigDecimal getLatitude() { return latitude; }
    public void setLatitude(BigDecimal latitude) { this.latitude = latitude; }
    public BigDecimal getLongitude() { return longitude; }
    public void setLongitude(BigDecimal longitude) { this.longitude = longitude; }
    public String getBoundaryGeoJson() { return boundaryGeoJson; }
    public void setBoundaryGeoJson(String boundaryGeoJson) { this.boundaryGeoJson = boundaryGeoJson; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
