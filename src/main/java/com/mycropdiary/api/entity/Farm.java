package com.mycropdiary.api.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "Farm", schema = "dbo")
public class Farm {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "FarmID")
    private Long id;

    @Column(name = "FarmCode", nullable = false, unique = true, length = 30)
    private String farmCode;

    @Column(name = "FarmName", nullable = false, length = 200)
    private String farmName;

    @Column(name = "Status", nullable = false, length = 20)
    private String status = "ACTIVE";

    protected Farm() { }

    public Long getId() { return id; }
    public String getFarmCode() { return farmCode; }
    public String getFarmName() { return farmName; }
    public String getStatus() { return status; }
}
