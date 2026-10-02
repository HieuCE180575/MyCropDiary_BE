/*==============================================================
  MyCropDiary - Relational Database for Microsoft SQL Server
  Derived from the supplied ERD (7 diagrams)
  Target: SQL Server 2019+
==============================================================*/

IF DB_ID(N'MyCropDiary') IS NULL
    CREATE DATABASE MyCropDiary;
GO

USE MyCropDiary;
GO

SET ANSI_NULLS ON;
SET QUOTED_IDENTIFIER ON;
GO

/*==============================================================
  1. ACCOUNT, FARM REGISTRATION, MEMBERSHIP
==============================================================*/

CREATE TABLE dbo.AppUser (
    UserID              BIGINT IDENTITY(1,1) NOT NULL,
    Email               VARCHAR(254) NOT NULL,
    PasswordHash        VARCHAR(255) NOT NULL,
    FullName            NVARCHAR(150) NOT NULL,
    PhoneNumber         VARCHAR(20) NULL,
    SystemRole          VARCHAR(20) NOT NULL CONSTRAINT DF_AppUser_SystemRole DEFAULT ('USER'),
    AccountStatus       VARCHAR(20) NOT NULL CONSTRAINT DF_AppUser_Status DEFAULT ('PENDING'),
    EmailVerifiedAt     DATETIME2(0) NULL,
    CreatedAt           DATETIME2(0) NOT NULL CONSTRAINT DF_AppUser_CreatedAt DEFAULT (SYSUTCDATETIME()),
    UpdatedAt           DATETIME2(0) NOT NULL CONSTRAINT DF_AppUser_UpdatedAt DEFAULT (SYSUTCDATETIME()),
    CONSTRAINT PK_AppUser PRIMARY KEY (UserID),
    CONSTRAINT UQ_AppUser_Email UNIQUE (Email),
    CONSTRAINT CK_AppUser_SystemRole CHECK (SystemRole IN ('USER','ADMIN')),
    CONSTRAINT CK_AppUser_Status CHECK (AccountStatus IN ('PENDING','ACTIVE','LOCKED','DISABLED'))
);
GO

CREATE TABLE dbo.AccountToken (
    TokenID             BIGINT IDENTITY(1,1) NOT NULL,
    UserID              BIGINT NOT NULL,
    TokenHash           VARCHAR(255) NOT NULL,
    TokenType           VARCHAR(30) NOT NULL,
    ExpiresAt           DATETIME2(0) NOT NULL,
    UsedAt              DATETIME2(0) NULL,
    RevokedAt           DATETIME2(0) NULL,
    CreatedAt           DATETIME2(0) NOT NULL CONSTRAINT DF_AccountToken_CreatedAt DEFAULT (SYSUTCDATETIME()),
    CONSTRAINT PK_AccountToken PRIMARY KEY (TokenID),
    CONSTRAINT UQ_AccountToken_TokenHash UNIQUE (TokenHash),
    CONSTRAINT FK_AccountToken_User FOREIGN KEY (UserID) REFERENCES dbo.AppUser(UserID) ON DELETE CASCADE,
    CONSTRAINT CK_AccountToken_Type CHECK (TokenType IN ('EMAIL_OTP','PASSWORD_RESET','REFRESH_TOKEN')),
    CONSTRAINT CK_AccountToken_Expiry CHECK (ExpiresAt > CreatedAt)
);
GO

CREATE TABLE dbo.FarmRegistration (
    RegistrationID      BIGINT IDENTITY(1,1) NOT NULL,
    ApplicantUserID     BIGINT NOT NULL,
    FarmName             NVARCHAR(200) NOT NULL,
    AddressLine          NVARCHAR(300) NOT NULL,
    Province             NVARCHAR(100) NULL,
    District             NVARCHAR(100) NULL,
    Ward                 NVARCHAR(100) NULL,
    Description          NVARCHAR(1000) NULL,
    DocumentUrl          NVARCHAR(1000) NULL,
    Status               VARCHAR(20) NOT NULL CONSTRAINT DF_FarmRegistration_Status DEFAULT ('PENDING'),
    HandlerUserID        BIGINT NULL,
    SubmittedAt          DATETIME2(0) NOT NULL CONSTRAINT DF_FarmRegistration_SubmittedAt DEFAULT (SYSUTCDATETIME()),
    HandledAt            DATETIME2(0) NULL,
    RejectionReason      NVARCHAR(1000) NULL,
    CONSTRAINT PK_FarmRegistration PRIMARY KEY (RegistrationID),
    CONSTRAINT FK_FarmRegistration_Applicant FOREIGN KEY (ApplicantUserID) REFERENCES dbo.AppUser(UserID),
    CONSTRAINT FK_FarmRegistration_Handler FOREIGN KEY (HandlerUserID) REFERENCES dbo.AppUser(UserID),
    CONSTRAINT CK_FarmRegistration_Status CHECK (Status IN ('PENDING','APPROVED','REJECTED','CANCELLED')),
    CONSTRAINT CK_FarmRegistration_Handle CHECK (
        (Status = 'PENDING' AND HandlerUserID IS NULL AND HandledAt IS NULL)
        OR Status IN ('CANCELLED')
        OR (Status IN ('APPROVED','REJECTED') AND HandlerUserID IS NOT NULL AND HandledAt IS NOT NULL)
    )
);
GO

CREATE TABLE dbo.Farm (
    FarmID               BIGINT IDENTITY(1,1) NOT NULL,
    RegistrationID       BIGINT NOT NULL,
    FarmCode             VARCHAR(30) NOT NULL,
    FarmName             NVARCHAR(200) NOT NULL,
    AddressLine          NVARCHAR(300) NOT NULL,
    Province             NVARCHAR(100) NULL,
    District             NVARCHAR(100) NULL,
    Ward                 NVARCHAR(100) NULL,
    Latitude             DECIMAL(9,6) NULL,
    Longitude            DECIMAL(9,6) NULL,
    TotalAreaM2          DECIMAL(18,2) NULL,
    Status               VARCHAR(20) NOT NULL CONSTRAINT DF_Farm_Status DEFAULT ('ACTIVE'),
    CreatedAt            DATETIME2(0) NOT NULL CONSTRAINT DF_Farm_CreatedAt DEFAULT (SYSUTCDATETIME()),
    CONSTRAINT PK_Farm PRIMARY KEY (FarmID),
    CONSTRAINT UQ_Farm_Registration UNIQUE (RegistrationID),
    CONSTRAINT UQ_Farm_Code UNIQUE (FarmCode),
    CONSTRAINT FK_Farm_Registration FOREIGN KEY (RegistrationID) REFERENCES dbo.FarmRegistration(RegistrationID),
    CONSTRAINT CK_Farm_Status CHECK (Status IN ('ACTIVE','INACTIVE','SUSPENDED')),
    CONSTRAINT CK_Farm_Area CHECK (TotalAreaM2 IS NULL OR TotalAreaM2 > 0),
    CONSTRAINT CK_Farm_Coordinates CHECK (
        (Latitude IS NULL AND Longitude IS NULL)
        OR (Latitude BETWEEN -90 AND 90 AND Longitude BETWEEN -180 AND 180)
    )
);
GO

CREATE TABLE dbo.FarmMember (
    FarmMemberID         BIGINT IDENTITY(1,1) NOT NULL,
    FarmID               BIGINT NOT NULL,
    UserID               BIGINT NOT NULL,
    FarmRole             VARCHAR(20) NOT NULL,
    JobTitle             NVARCHAR(100) NULL,
    JoinedAt             DATE NOT NULL CONSTRAINT DF_FarmMember_JoinedAt DEFAULT (CONVERT(date, SYSUTCDATETIME())),
    LeftAt               DATE NULL,
    Status               VARCHAR(20) NOT NULL CONSTRAINT DF_FarmMember_Status DEFAULT ('ACTIVE'),
    CONSTRAINT PK_FarmMember PRIMARY KEY (FarmMemberID),
    CONSTRAINT UQ_FarmMember_Farm_User UNIQUE (FarmID, UserID),
    CONSTRAINT FK_FarmMember_Farm FOREIGN KEY (FarmID) REFERENCES dbo.Farm(FarmID),
    CONSTRAINT FK_FarmMember_User FOREIGN KEY (UserID) REFERENCES dbo.AppUser(UserID),
    CONSTRAINT CK_FarmMember_Role CHECK (FarmRole IN ('OWNER','STAFF')),
    CONSTRAINT CK_FarmMember_Status CHECK (Status IN ('INVITED','ACTIVE','INACTIVE')),
    CONSTRAINT CK_FarmMember_Dates CHECK (LeftAt IS NULL OR LeftAt >= JoinedAt)
);
GO

CREATE UNIQUE INDEX UX_Farm_OneActiveOwner
ON dbo.FarmMember(FarmID)
WHERE FarmRole = 'OWNER' AND Status = 'ACTIVE';
GO

CREATE TABLE dbo.FarmWorker (
    FarmWorkerID         BIGINT IDENTITY(1,1) NOT NULL,
    FarmID               BIGINT NOT NULL,
    WorkerCode           VARCHAR(30) NOT NULL,
    FullName             NVARCHAR(150) NOT NULL,
    PhoneNumber          VARCHAR(20) NULL,
    DateOfBirth          DATE NULL,
    HireDate             DATE NULL,
    Status               VARCHAR(20) NOT NULL CONSTRAINT DF_FarmWorker_Status DEFAULT ('ACTIVE'),
    Notes                NVARCHAR(1000) NULL,
    CONSTRAINT PK_FarmWorker PRIMARY KEY (FarmWorkerID),
    CONSTRAINT UQ_FarmWorker_Code UNIQUE (FarmID, WorkerCode),
    CONSTRAINT FK_FarmWorker_Farm FOREIGN KEY (FarmID) REFERENCES dbo.Farm(FarmID),
    CONSTRAINT CK_FarmWorker_Status CHECK (Status IN ('ACTIVE','INACTIVE'))
);
GO

/*==============================================================
  2. PRODUCTION AREAS, PLOTS, ENVIRONMENTAL ASSESSMENTS
==============================================================*/

CREATE TABLE dbo.ProductionArea (
    ProductionAreaID     BIGINT IDENTITY(1,1) NOT NULL,
    FarmID               BIGINT NOT NULL,
    AreaCode             VARCHAR(30) NOT NULL,
    AreaName             NVARCHAR(150) NOT NULL,
    AreaM2               DECIMAL(18,2) NULL,
    Description          NVARCHAR(1000) NULL,
    Status               VARCHAR(20) NOT NULL CONSTRAINT DF_ProductionArea_Status DEFAULT ('ACTIVE'),
    CreatedAt            DATETIME2(0) NOT NULL CONSTRAINT DF_ProductionArea_CreatedAt DEFAULT (SYSUTCDATETIME()),
    CONSTRAINT PK_ProductionArea PRIMARY KEY (ProductionAreaID),
    CONSTRAINT UQ_ProductionArea_Code UNIQUE (FarmID, AreaCode),
    CONSTRAINT FK_ProductionArea_Farm FOREIGN KEY (FarmID) REFERENCES dbo.Farm(FarmID),
    CONSTRAINT CK_ProductionArea_Area CHECK (AreaM2 IS NULL OR AreaM2 > 0),
    CONSTRAINT CK_ProductionArea_Status CHECK (Status IN ('ACTIVE','INACTIVE'))
);
GO

CREATE TABLE dbo.StaffAreaAssignment (
    AssignmentID         BIGINT IDENTITY(1,1) NOT NULL,
    FarmMemberID         BIGINT NOT NULL,
    ProductionAreaID     BIGINT NOT NULL,
    AssignedByMemberID   BIGINT NULL,
    StartDate            DATE NOT NULL CONSTRAINT DF_StaffAreaAssignment_Start DEFAULT (CONVERT(date, SYSUTCDATETIME())),
    EndDate              DATE NULL,
    IsActive             BIT NOT NULL CONSTRAINT DF_StaffAreaAssignment_Active DEFAULT (1),
    CONSTRAINT PK_StaffAreaAssignment PRIMARY KEY (AssignmentID),
    CONSTRAINT UQ_StaffAreaAssignment UNIQUE (FarmMemberID, ProductionAreaID, StartDate),
    CONSTRAINT FK_StaffAreaAssignment_Member FOREIGN KEY (FarmMemberID) REFERENCES dbo.FarmMember(FarmMemberID),
    CONSTRAINT FK_StaffAreaAssignment_Area FOREIGN KEY (ProductionAreaID) REFERENCES dbo.ProductionArea(ProductionAreaID),
    CONSTRAINT FK_StaffAreaAssignment_Assigner FOREIGN KEY (AssignedByMemberID) REFERENCES dbo.FarmMember(FarmMemberID),
    CONSTRAINT CK_StaffAreaAssignment_Dates CHECK (EndDate IS NULL OR EndDate >= StartDate)
);
GO

CREATE TABLE dbo.Plot (
    PlotID               BIGINT IDENTITY(1,1) NOT NULL,
    ProductionAreaID     BIGINT NOT NULL,
    PlotCode             VARCHAR(30) NOT NULL,
    PlotName             NVARCHAR(150) NOT NULL,
    AreaM2               DECIMAL(18,2) NOT NULL,
    Latitude             DECIMAL(9,6) NULL,
    Longitude            DECIMAL(9,6) NULL,
    BoundaryGeoJson      NVARCHAR(MAX) NULL,
    Status               VARCHAR(20) NOT NULL CONSTRAINT DF_Plot_Status DEFAULT ('AVAILABLE'),
    CONSTRAINT PK_Plot PRIMARY KEY (PlotID),
    CONSTRAINT UQ_Plot_Code UNIQUE (ProductionAreaID, PlotCode),
    CONSTRAINT FK_Plot_Area FOREIGN KEY (ProductionAreaID) REFERENCES dbo.ProductionArea(ProductionAreaID),
    CONSTRAINT CK_Plot_Area CHECK (AreaM2 > 0),
    CONSTRAINT CK_Plot_Status CHECK (Status IN ('AVAILABLE','IN_USE','RESTING','INACTIVE')),
    CONSTRAINT CK_Plot_Coordinates CHECK (
        (Latitude IS NULL AND Longitude IS NULL)
        OR (Latitude BETWEEN -90 AND 90 AND Longitude BETWEEN -180 AND 180)
    ),
    CONSTRAINT CK_Plot_BoundaryJson CHECK (BoundaryGeoJson IS NULL OR ISJSON(BoundaryGeoJson) = 1)
);
GO

CREATE TABLE dbo.WaterSource (
    WaterSourceID        BIGINT IDENTITY(1,1) NOT NULL,
    FarmID               BIGINT NOT NULL,
    SourceCode           VARCHAR(30) NOT NULL,
    SourceName           NVARCHAR(150) NOT NULL,
    SourceType           VARCHAR(30) NOT NULL,
    Description          NVARCHAR(1000) NULL,
    Status               VARCHAR(20) NOT NULL CONSTRAINT DF_WaterSource_Status DEFAULT ('ACTIVE'),
    CONSTRAINT PK_WaterSource PRIMARY KEY (WaterSourceID),
    CONSTRAINT UQ_WaterSource_Code UNIQUE (FarmID, SourceCode),
    CONSTRAINT FK_WaterSource_Farm FOREIGN KEY (FarmID) REFERENCES dbo.Farm(FarmID),
    CONSTRAINT CK_WaterSource_Type CHECK (SourceType IN ('WELL','RIVER','LAKE','RESERVOIR','TAP','RAINWATER','OTHER')),
    CONSTRAINT CK_WaterSource_Status CHECK (Status IN ('ACTIVE','INACTIVE','CONTAMINATED'))
);
GO

CREATE TABLE dbo.PlotWaterSource (
    PlotID               BIGINT NOT NULL,
    WaterSourceID        BIGINT NOT NULL,
    IsPrimary            BIT NOT NULL CONSTRAINT DF_PlotWaterSource_Primary DEFAULT (0),
    CONSTRAINT PK_PlotWaterSource PRIMARY KEY (PlotID, WaterSourceID),
    CONSTRAINT FK_PlotWaterSource_Plot FOREIGN KEY (PlotID) REFERENCES dbo.Plot(PlotID) ON DELETE CASCADE,
    CONSTRAINT FK_PlotWaterSource_Water FOREIGN KEY (WaterSourceID) REFERENCES dbo.WaterSource(WaterSourceID)
);
GO

CREATE TABLE dbo.AreaRiskAssessment (
    AreaRiskAssessmentID BIGINT IDENTITY(1,1) NOT NULL,
    ProductionAreaID     BIGINT NOT NULL,
    ConductedByMemberID  BIGINT NOT NULL,
    AssessmentDate       DATE NOT NULL,
    RiskType             VARCHAR(40) NOT NULL,
    RiskLevel            VARCHAR(20) NOT NULL,
    Findings             NVARCHAR(MAX) NOT NULL,
    MitigationAction     NVARCHAR(MAX) NULL,
    NextReviewDate       DATE NULL,
    CONSTRAINT PK_AreaRiskAssessment PRIMARY KEY (AreaRiskAssessmentID),
    CONSTRAINT FK_AreaRiskAssessment_Area FOREIGN KEY (ProductionAreaID) REFERENCES dbo.ProductionArea(ProductionAreaID),
    CONSTRAINT FK_AreaRiskAssessment_Member FOREIGN KEY (ConductedByMemberID) REFERENCES dbo.FarmMember(FarmMemberID),
    CONSTRAINT CK_AreaRiskAssessment_Level CHECK (RiskLevel IN ('LOW','MEDIUM','HIGH','CRITICAL')),
    CONSTRAINT CK_AreaRiskAssessment_Date CHECK (NextReviewDate IS NULL OR NextReviewDate >= AssessmentDate)
);
GO

CREATE TABLE dbo.WaterAssessment (
    WaterAssessmentID    BIGINT IDENTITY(1,1) NOT NULL,
    WaterSourceID        BIGINT NOT NULL,
    ConductedByMemberID  BIGINT NOT NULL,
    AssessmentDate       DATE NOT NULL,
    LaboratoryName       NVARCHAR(200) NULL,
    PH                   DECIMAL(4,2) NULL,
    EColiCFU             DECIMAL(18,2) NULL,
    ResultStatus         VARCHAR(20) NOT NULL,
    ResultJson           NVARCHAR(MAX) NULL,
    EvidenceUrl          NVARCHAR(1000) NULL,
    Notes                NVARCHAR(2000) NULL,
    CONSTRAINT PK_WaterAssessment PRIMARY KEY (WaterAssessmentID),
    CONSTRAINT FK_WaterAssessment_Source FOREIGN KEY (WaterSourceID) REFERENCES dbo.WaterSource(WaterSourceID),
    CONSTRAINT FK_WaterAssessment_Member FOREIGN KEY (ConductedByMemberID) REFERENCES dbo.FarmMember(FarmMemberID),
    CONSTRAINT CK_WaterAssessment_PH CHECK (PH IS NULL OR PH BETWEEN 0 AND 14),
    CONSTRAINT CK_WaterAssessment_Result CHECK (ResultStatus IN ('PASS','FAIL','PENDING','NOT_EVALUATED')),
    CONSTRAINT CK_WaterAssessment_Json CHECK (ResultJson IS NULL OR ISJSON(ResultJson) = 1)
);
GO

CREATE TABLE dbo.SoilAssessment (
    SoilAssessmentID     BIGINT IDENTITY(1,1) NOT NULL,
    PlotID               BIGINT NOT NULL,
    ConductedByMemberID  BIGINT NOT NULL,
    AssessmentDate       DATE NOT NULL,
    SoilType             NVARCHAR(100) NULL,
    PH                   DECIMAL(4,2) NULL,
    OrganicMatterPercent DECIMAL(5,2) NULL,
    ResultStatus         VARCHAR(20) NOT NULL,
    ResultJson           NVARCHAR(MAX) NULL,
    EvidenceUrl          NVARCHAR(1000) NULL,
    Notes                NVARCHAR(2000) NULL,
    CONSTRAINT PK_SoilAssessment PRIMARY KEY (SoilAssessmentID),
    CONSTRAINT FK_SoilAssessment_Plot FOREIGN KEY (PlotID) REFERENCES dbo.Plot(PlotID),
    CONSTRAINT FK_SoilAssessment_Member FOREIGN KEY (ConductedByMemberID) REFERENCES dbo.FarmMember(FarmMemberID),
    CONSTRAINT CK_SoilAssessment_PH CHECK (PH IS NULL OR PH BETWEEN 0 AND 14),
    CONSTRAINT CK_SoilAssessment_Organic CHECK (OrganicMatterPercent IS NULL OR OrganicMatterPercent BETWEEN 0 AND 100),
    CONSTRAINT CK_SoilAssessment_Result CHECK (ResultStatus IN ('PASS','FAIL','PENDING','NOT_EVALUATED')),
    CONSTRAINT CK_SoilAssessment_Json CHECK (ResultJson IS NULL OR ISJSON(ResultJson) = 1)
);
GO

CREATE TABLE dbo.PlotConditionObservation (
    ObservationID        BIGINT IDENTITY(1,1) NOT NULL,
    PlotID               BIGINT NOT NULL,
    RecordedByMemberID   BIGINT NOT NULL,
    ObservedAt           DATETIME2(0) NOT NULL,
    ConditionType        VARCHAR(40) NOT NULL,
    Severity             VARCHAR(20) NULL,
    Description          NVARCHAR(MAX) NOT NULL,
    ImageUrl             NVARCHAR(1000) NULL,
    RecommendedAction    NVARCHAR(MAX) NULL,
    CONSTRAINT PK_PlotConditionObservation PRIMARY KEY (ObservationID),
    CONSTRAINT FK_PlotObservation_Plot FOREIGN KEY (PlotID) REFERENCES dbo.Plot(PlotID),
    CONSTRAINT FK_PlotObservation_Member FOREIGN KEY (RecordedByMemberID) REFERENCES dbo.FarmMember(FarmMemberID),
    CONSTRAINT CK_PlotObservation_Severity CHECK (Severity IS NULL OR Severity IN ('LOW','MEDIUM','HIGH','CRITICAL'))
);
GO

/*==============================================================
  3. CROP SEASONS, TASKS, FARMING ACTIVITIES
==============================================================*/

CREATE TABLE dbo.CropCategory (
    CropCategoryID       BIGINT IDENTITY(1,1) NOT NULL,
    CategoryCode         VARCHAR(30) NOT NULL,
    CategoryName         NVARCHAR(150) NOT NULL,
    ScientificName       NVARCHAR(200) NULL,
    TypicalDurationDays  INT NULL,
    IsActive             BIT NOT NULL CONSTRAINT DF_CropCategory_Active DEFAULT (1),
    CONSTRAINT PK_CropCategory PRIMARY KEY (CropCategoryID),
    CONSTRAINT UQ_CropCategory_Code UNIQUE (CategoryCode),
    CONSTRAINT CK_CropCategory_Duration CHECK (TypicalDurationDays IS NULL OR TypicalDurationDays > 0)
);
GO

CREATE TABLE dbo.CropSeason (
    CropSeasonID         BIGINT IDENTITY(1,1) NOT NULL,
    FarmID               BIGINT NOT NULL,
    PlotID               BIGINT NOT NULL,
    CropCategoryID       BIGINT NOT NULL,
    CreatedByMemberID    BIGINT NOT NULL,
    SeasonCode           VARCHAR(40) NOT NULL,
    SeasonName           NVARCHAR(200) NOT NULL,
    VarietyName          NVARCHAR(150) NULL,
    StartDate            DATE NOT NULL,
    ExpectedHarvestDate  DATE NULL,
    ActualEndDate        DATE NULL,
    CultivatedAreaM2     DECIMAL(18,2) NULL,
    Status               VARCHAR(20) NOT NULL CONSTRAINT DF_CropSeason_Status DEFAULT ('PLANNED'),
    Notes                NVARCHAR(2000) NULL,
    CONSTRAINT PK_CropSeason PRIMARY KEY (CropSeasonID),
    CONSTRAINT UQ_CropSeason_Code UNIQUE (FarmID, SeasonCode),
    CONSTRAINT FK_CropSeason_Farm FOREIGN KEY (FarmID) REFERENCES dbo.Farm(FarmID),
    CONSTRAINT FK_CropSeason_Plot FOREIGN KEY (PlotID) REFERENCES dbo.Plot(PlotID),
    CONSTRAINT FK_CropSeason_Category FOREIGN KEY (CropCategoryID) REFERENCES dbo.CropCategory(CropCategoryID),
    CONSTRAINT FK_CropSeason_Creator FOREIGN KEY (CreatedByMemberID) REFERENCES dbo.FarmMember(FarmMemberID),
    CONSTRAINT CK_CropSeason_Dates CHECK (
        (ExpectedHarvestDate IS NULL OR ExpectedHarvestDate >= StartDate)
        AND (ActualEndDate IS NULL OR ActualEndDate >= StartDate)
    ),
    CONSTRAINT CK_CropSeason_Area CHECK (CultivatedAreaM2 IS NULL OR CultivatedAreaM2 > 0),
    CONSTRAINT CK_CropSeason_Status CHECK (Status IN ('PLANNED','ACTIVE','HARVESTING','COMPLETED','CANCELLED'))
);
GO

CREATE TABLE dbo.FarmTask (
    FarmTaskID           BIGINT IDENTITY(1,1) NOT NULL,
    FarmID               BIGINT NOT NULL,
    ProductionAreaID     BIGINT NULL,
    PlotID               BIGINT NULL,
    CropSeasonID         BIGINT NULL,
    CreatedByMemberID    BIGINT NOT NULL,
    HandledByMemberID    BIGINT NULL,
    Title                NVARCHAR(200) NOT NULL,
    Description          NVARCHAR(MAX) NULL,
    Priority             VARCHAR(20) NOT NULL CONSTRAINT DF_FarmTask_Priority DEFAULT ('MEDIUM'),
    Status               VARCHAR(20) NOT NULL CONSTRAINT DF_FarmTask_Status DEFAULT ('OPEN'),
    StartAt              DATETIME2(0) NULL,
    DueAt                DATETIME2(0) NULL,
    CompletedAt          DATETIME2(0) NULL,
    CreatedAt            DATETIME2(0) NOT NULL CONSTRAINT DF_FarmTask_CreatedAt DEFAULT (SYSUTCDATETIME()),
    CONSTRAINT PK_FarmTask PRIMARY KEY (FarmTaskID),
    CONSTRAINT FK_FarmTask_Farm FOREIGN KEY (FarmID) REFERENCES dbo.Farm(FarmID),
    CONSTRAINT FK_FarmTask_Area FOREIGN KEY (ProductionAreaID) REFERENCES dbo.ProductionArea(ProductionAreaID),
    CONSTRAINT FK_FarmTask_Plot FOREIGN KEY (PlotID) REFERENCES dbo.Plot(PlotID),
    CONSTRAINT FK_FarmTask_Season FOREIGN KEY (CropSeasonID) REFERENCES dbo.CropSeason(CropSeasonID),
    CONSTRAINT FK_FarmTask_Creator FOREIGN KEY (CreatedByMemberID) REFERENCES dbo.FarmMember(FarmMemberID),
    CONSTRAINT FK_FarmTask_Handler FOREIGN KEY (HandledByMemberID) REFERENCES dbo.FarmMember(FarmMemberID),
    CONSTRAINT CK_FarmTask_Priority CHECK (Priority IN ('LOW','MEDIUM','HIGH','URGENT')),
    CONSTRAINT CK_FarmTask_Status CHECK (Status IN ('OPEN','IN_PROGRESS','DONE','CANCELLED','OVERDUE')),
    CONSTRAINT CK_FarmTask_Dates CHECK (DueAt IS NULL OR StartAt IS NULL OR DueAt >= StartAt)
);
GO

CREATE TABLE dbo.FarmTaskWorker (
    FarmTaskID           BIGINT NOT NULL,
    FarmWorkerID         BIGINT NOT NULL,
    AssignedAt           DATETIME2(0) NOT NULL CONSTRAINT DF_FarmTaskWorker_Assigned DEFAULT (SYSUTCDATETIME()),
    AssignmentStatus     VARCHAR(20) NOT NULL CONSTRAINT DF_FarmTaskWorker_Status DEFAULT ('ASSIGNED'),
    CONSTRAINT PK_FarmTaskWorker PRIMARY KEY (FarmTaskID, FarmWorkerID),
    CONSTRAINT FK_FarmTaskWorker_Task FOREIGN KEY (FarmTaskID) REFERENCES dbo.FarmTask(FarmTaskID) ON DELETE CASCADE,
    CONSTRAINT FK_FarmTaskWorker_Worker FOREIGN KEY (FarmWorkerID) REFERENCES dbo.FarmWorker(FarmWorkerID),
    CONSTRAINT CK_FarmTaskWorker_Status CHECK (AssignmentStatus IN ('ASSIGNED','ACCEPTED','COMPLETED','CANCELLED'))
);
GO

CREATE TABLE dbo.FarmingActivity (
    FarmingActivityID    BIGINT IDENTITY(1,1) NOT NULL,
    CropSeasonID         BIGINT NOT NULL,
    FarmTaskID           BIGINT NULL,
    SupervisedByMemberID BIGINT NOT NULL,
    ActivityType         VARCHAR(40) NOT NULL,
    ActivityName         NVARCHAR(200) NOT NULL,
    StartedAt            DATETIME2(0) NOT NULL,
    EndedAt              DATETIME2(0) NULL,
    Description          NVARCHAR(MAX) NULL,
    ResultNotes          NVARCHAR(MAX) NULL,
    WeatherNotes         NVARCHAR(1000) NULL,
    CreatedAt            DATETIME2(0) NOT NULL CONSTRAINT DF_FarmingActivity_CreatedAt DEFAULT (SYSUTCDATETIME()),
    CONSTRAINT PK_FarmingActivity PRIMARY KEY (FarmingActivityID),
    CONSTRAINT FK_FarmingActivity_Season FOREIGN KEY (CropSeasonID) REFERENCES dbo.CropSeason(CropSeasonID),
    CONSTRAINT FK_FarmingActivity_Task FOREIGN KEY (FarmTaskID) REFERENCES dbo.FarmTask(FarmTaskID),
    CONSTRAINT FK_FarmingActivity_Supervisor FOREIGN KEY (SupervisedByMemberID) REFERENCES dbo.FarmMember(FarmMemberID),
    CONSTRAINT CK_FarmingActivity_Dates CHECK (EndedAt IS NULL OR EndedAt >= StartedAt)
);
GO

CREATE UNIQUE INDEX UX_FarmingActivity_OneActivityPerTask
ON dbo.FarmingActivity(FarmTaskID)
WHERE FarmTaskID IS NOT NULL;
GO

CREATE TABLE dbo.ActivityWorker (
    FarmingActivityID    BIGINT NOT NULL,
    FarmWorkerID         BIGINT NOT NULL,
    WorkHours            DECIMAL(8,2) NULL,
    Notes                NVARCHAR(500) NULL,
    CONSTRAINT PK_ActivityWorker PRIMARY KEY (FarmingActivityID, FarmWorkerID),
    CONSTRAINT FK_ActivityWorker_Activity FOREIGN KEY (FarmingActivityID) REFERENCES dbo.FarmingActivity(FarmingActivityID) ON DELETE CASCADE,
    CONSTRAINT FK_ActivityWorker_Worker FOREIGN KEY (FarmWorkerID) REFERENCES dbo.FarmWorker(FarmWorkerID),
    CONSTRAINT CK_ActivityWorker_Hours CHECK (WorkHours IS NULL OR WorkHours >= 0)
);
GO

/*==============================================================
  4. SUPPLIERS, PURCHASES, MATERIAL USAGE, EXPENSES
==============================================================*/

CREATE TABLE dbo.Supplier (
    SupplierID           BIGINT IDENTITY(1,1) NOT NULL,
    FarmID               BIGINT NOT NULL,
    SupplierCode         VARCHAR(30) NOT NULL,
    SupplierName         NVARCHAR(200) NOT NULL,
    ContactPerson        NVARCHAR(150) NULL,
    PhoneNumber          VARCHAR(20) NULL,
    Email                VARCHAR(254) NULL,
    AddressLine          NVARCHAR(300) NULL,
    TaxCode              VARCHAR(30) NULL,
    IsActive             BIT NOT NULL CONSTRAINT DF_Supplier_Active DEFAULT (1),
    CONSTRAINT PK_Supplier PRIMARY KEY (SupplierID),
    CONSTRAINT UQ_Supplier_Code UNIQUE (FarmID, SupplierCode),
    CONSTRAINT FK_Supplier_Farm FOREIGN KEY (FarmID) REFERENCES dbo.Farm(FarmID)
);
GO

CREATE TABLE dbo.Material (
    MaterialID           BIGINT IDENTITY(1,1) NOT NULL,
    FarmID               BIGINT NOT NULL,
    MaterialCode         VARCHAR(30) NOT NULL,
    MaterialName         NVARCHAR(200) NOT NULL,
    MaterialType         VARCHAR(30) NOT NULL,
    Unit                 NVARCHAR(30) NOT NULL,
    ActiveIngredient     NVARCHAR(300) NULL,
    Manufacturer         NVARCHAR(200) NULL,
    IsActive             BIT NOT NULL CONSTRAINT DF_Material_Active DEFAULT (1),
    CONSTRAINT PK_Material PRIMARY KEY (MaterialID),
    CONSTRAINT UQ_Material_Code UNIQUE (FarmID, MaterialCode),
    CONSTRAINT FK_Material_Farm FOREIGN KEY (FarmID) REFERENCES dbo.Farm(FarmID),
    CONSTRAINT CK_Material_Type CHECK (MaterialType IN ('SEED','FERTILIZER','PESTICIDE','BIOLOGICAL','SOIL_AMENDMENT','PACKAGING','TOOL','OTHER'))
);
GO

CREATE TABLE dbo.InputPurchase (
    InputPurchaseID      BIGINT IDENTITY(1,1) NOT NULL,
    FarmID               BIGINT NOT NULL,
    SupplierID           BIGINT NOT NULL,
    RecordedByMemberID   BIGINT NOT NULL,
    InvoiceNumber        NVARCHAR(50) NULL,
    PurchaseDate         DATE NOT NULL,
    TotalAmount          DECIMAL(19,2) NOT NULL CONSTRAINT DF_InputPurchase_Total DEFAULT (0),
    DocumentUrl          NVARCHAR(1000) NULL,
    Notes                NVARCHAR(1000) NULL,
    CONSTRAINT PK_InputPurchase PRIMARY KEY (InputPurchaseID),
    CONSTRAINT FK_InputPurchase_Farm FOREIGN KEY (FarmID) REFERENCES dbo.Farm(FarmID),
    CONSTRAINT FK_InputPurchase_Supplier FOREIGN KEY (SupplierID) REFERENCES dbo.Supplier(SupplierID),
    CONSTRAINT FK_InputPurchase_Member FOREIGN KEY (RecordedByMemberID) REFERENCES dbo.FarmMember(FarmMemberID),
    CONSTRAINT CK_InputPurchase_Total CHECK (TotalAmount >= 0)
);
GO

CREATE TABLE dbo.InputPurchaseDetail (
    InputPurchaseDetailID BIGINT IDENTITY(1,1) NOT NULL,
    InputPurchaseID      BIGINT NOT NULL,
    MaterialID           BIGINT NOT NULL,
    BatchNumber          NVARCHAR(80) NULL,
    ManufactureDate      DATE NULL,
    ExpiryDate           DATE NULL,
    Quantity             DECIMAL(18,3) NOT NULL,
    UnitPrice            DECIMAL(19,2) NOT NULL,
    LineAmount AS CONVERT(DECIMAL(19,2), Quantity * UnitPrice) PERSISTED,
    CONSTRAINT PK_InputPurchaseDetail PRIMARY KEY (InputPurchaseDetailID),
    CONSTRAINT FK_PurchaseDetail_Purchase FOREIGN KEY (InputPurchaseID) REFERENCES dbo.InputPurchase(InputPurchaseID) ON DELETE CASCADE,
    CONSTRAINT FK_PurchaseDetail_Material FOREIGN KEY (MaterialID) REFERENCES dbo.Material(MaterialID),
    CONSTRAINT CK_PurchaseDetail_Quantity CHECK (Quantity > 0),
    CONSTRAINT CK_PurchaseDetail_Price CHECK (UnitPrice >= 0),
    CONSTRAINT CK_PurchaseDetail_Dates CHECK (ExpiryDate IS NULL OR ManufactureDate IS NULL OR ExpiryDate >= ManufactureDate)
);
GO

CREATE TABLE dbo.MaterialUsage (
    MaterialUsageID      BIGINT IDENTITY(1,1) NOT NULL,
    FarmingActivityID    BIGINT NOT NULL,
    InputPurchaseDetailID BIGINT NULL,
    MaterialID           BIGINT NOT NULL,
    RecordedByMemberID   BIGINT NOT NULL,
    UsedAt               DATETIME2(0) NOT NULL,
    Quantity             DECIMAL(18,3) NOT NULL,
    Unit                 NVARCHAR(30) NOT NULL,
    Dosage               NVARCHAR(100) NULL,
    Method               NVARCHAR(200) NULL,
    SafetyIntervalDays   INT NULL,
    Notes                NVARCHAR(1000) NULL,
    CONSTRAINT PK_MaterialUsage PRIMARY KEY (MaterialUsageID),
    CONSTRAINT FK_MaterialUsage_Activity FOREIGN KEY (FarmingActivityID) REFERENCES dbo.FarmingActivity(FarmingActivityID),
    CONSTRAINT FK_MaterialUsage_PurchaseDetail FOREIGN KEY (InputPurchaseDetailID) REFERENCES dbo.InputPurchaseDetail(InputPurchaseDetailID),
    CONSTRAINT FK_MaterialUsage_Material FOREIGN KEY (MaterialID) REFERENCES dbo.Material(MaterialID),
    CONSTRAINT FK_MaterialUsage_Member FOREIGN KEY (RecordedByMemberID) REFERENCES dbo.FarmMember(FarmMemberID),
    CONSTRAINT CK_MaterialUsage_Quantity CHECK (Quantity > 0),
    CONSTRAINT CK_MaterialUsage_Safety CHECK (SafetyIntervalDays IS NULL OR SafetyIntervalDays >= 0)
);
GO

CREATE TABLE dbo.Expense (
    ExpenseID            BIGINT IDENTITY(1,1) NOT NULL,
    FarmID               BIGINT NOT NULL,
    CropSeasonID         BIGINT NULL,
    FarmingActivityID    BIGINT NULL,
    InputPurchaseID      BIGINT NULL,
    RecordedByMemberID   BIGINT NOT NULL,
    ExpenseType          VARCHAR(30) NOT NULL,
    ExpenseDate          DATE NOT NULL,
    Amount               DECIMAL(19,2) NOT NULL,
    Description          NVARCHAR(1000) NULL,
    EvidenceUrl          NVARCHAR(1000) NULL,
    CONSTRAINT PK_Expense PRIMARY KEY (ExpenseID),
    CONSTRAINT FK_Expense_Farm FOREIGN KEY (FarmID) REFERENCES dbo.Farm(FarmID),
    CONSTRAINT FK_Expense_Season FOREIGN KEY (CropSeasonID) REFERENCES dbo.CropSeason(CropSeasonID),
    CONSTRAINT FK_Expense_Activity FOREIGN KEY (FarmingActivityID) REFERENCES dbo.FarmingActivity(FarmingActivityID),
    CONSTRAINT FK_Expense_Purchase FOREIGN KEY (InputPurchaseID) REFERENCES dbo.InputPurchase(InputPurchaseID),
    CONSTRAINT FK_Expense_Member FOREIGN KEY (RecordedByMemberID) REFERENCES dbo.FarmMember(FarmMemberID),
    CONSTRAINT CK_Expense_Type CHECK (ExpenseType IN ('INPUT','LABOR','EQUIPMENT','TRANSPORT','UTILITY','SERVICE','OTHER')),
    CONSTRAINT CK_Expense_Amount CHECK (Amount > 0)
);
GO

/*==============================================================
  5. HARVEST AND PRODUCT SALES / TRACEABILITY
==============================================================*/

CREATE TABLE dbo.HarvestRecord (
    HarvestRecordID      BIGINT IDENTITY(1,1) NOT NULL,
    CropSeasonID         BIGINT NOT NULL,
    RecordedByMemberID   BIGINT NOT NULL,
    HarvestLotCode       VARCHAR(60) NOT NULL,
    HarvestedAt          DATETIME2(0) NOT NULL,
    Quantity             DECIMAL(18,3) NOT NULL,
    Unit                 NVARCHAR(30) NOT NULL,
    QualityGrade         NVARCHAR(50) NULL,
    StorageLocation      NVARCHAR(200) NULL,
    TraceabilityCode     VARCHAR(100) NULL,
    Notes                NVARCHAR(1000) NULL,
    CONSTRAINT PK_HarvestRecord PRIMARY KEY (HarvestRecordID),
    CONSTRAINT UQ_HarvestRecord_Lot UNIQUE (HarvestLotCode),
    CONSTRAINT FK_HarvestRecord_Season FOREIGN KEY (CropSeasonID) REFERENCES dbo.CropSeason(CropSeasonID),
    CONSTRAINT FK_HarvestRecord_Member FOREIGN KEY (RecordedByMemberID) REFERENCES dbo.FarmMember(FarmMemberID),
    CONSTRAINT CK_HarvestRecord_Quantity CHECK (Quantity > 0)
);
GO

CREATE UNIQUE INDEX UX_HarvestRecord_TraceabilityCode
ON dbo.HarvestRecord(TraceabilityCode)
WHERE TraceabilityCode IS NOT NULL;
GO

CREATE TABLE dbo.HarvestActivitySource (
    HarvestRecordID      BIGINT NOT NULL,
    FarmingActivityID    BIGINT NOT NULL,
    CONSTRAINT PK_HarvestActivitySource PRIMARY KEY (HarvestRecordID, FarmingActivityID),
    CONSTRAINT FK_HarvestActivity_Harvest FOREIGN KEY (HarvestRecordID) REFERENCES dbo.HarvestRecord(HarvestRecordID) ON DELETE CASCADE,
    CONSTRAINT FK_HarvestActivity_Activity FOREIGN KEY (FarmingActivityID) REFERENCES dbo.FarmingActivity(FarmingActivityID)
);
GO

CREATE TABLE dbo.ProductSale (
    ProductSaleID        BIGINT IDENTITY(1,1) NOT NULL,
    HarvestRecordID      BIGINT NOT NULL,
    RecordedByMemberID   BIGINT NOT NULL,
    SaleDate             DATE NOT NULL,
    BuyerName            NVARCHAR(200) NULL,
    Quantity             DECIMAL(18,3) NOT NULL,
    UnitPrice            DECIMAL(19,2) NOT NULL,
    TotalAmount AS CONVERT(DECIMAL(19,2), Quantity * UnitPrice) PERSISTED,
    InvoiceNumber        NVARCHAR(50) NULL,
    Notes                NVARCHAR(1000) NULL,
    CONSTRAINT PK_ProductSale PRIMARY KEY (ProductSaleID),
    CONSTRAINT FK_ProductSale_Harvest FOREIGN KEY (HarvestRecordID) REFERENCES dbo.HarvestRecord(HarvestRecordID),
    CONSTRAINT FK_ProductSale_Member FOREIGN KEY (RecordedByMemberID) REFERENCES dbo.FarmMember(FarmMemberID),
    CONSTRAINT CK_ProductSale_Quantity CHECK (Quantity > 0),
    CONSTRAINT CK_ProductSale_Price CHECK (UnitPrice >= 0)
);
GO

/*==============================================================
  6. CHECKLISTS, INTERNAL ASSESSMENTS, TRAINING, REPORTS
==============================================================*/

CREATE TABLE dbo.ChecklistRule (
    ChecklistRuleID      BIGINT IDENTITY(1,1) NOT NULL,
    CreatedByUserID      BIGINT NOT NULL,
    RuleCode             VARCHAR(50) NOT NULL,
    RuleName             NVARCHAR(250) NOT NULL,
    RuleCategory         VARCHAR(50) NOT NULL,
    Description          NVARCHAR(MAX) NOT NULL,
    EvaluationType       VARCHAR(20) NOT NULL,
    RuleExpression       NVARCHAR(MAX) NULL,
    ReferenceDocument    NVARCHAR(500) NULL,
    EffectiveFrom        DATE NOT NULL,
    EffectiveTo          DATE NULL,
    IsActive             BIT NOT NULL CONSTRAINT DF_ChecklistRule_Active DEFAULT (1),
    CONSTRAINT PK_ChecklistRule PRIMARY KEY (ChecklistRuleID),
    CONSTRAINT UQ_ChecklistRule_Code UNIQUE (RuleCode),
    CONSTRAINT FK_ChecklistRule_User FOREIGN KEY (CreatedByUserID) REFERENCES dbo.AppUser(UserID),
    CONSTRAINT CK_ChecklistRule_Type CHECK (EvaluationType IN ('BOOLEAN','NUMBER','TEXT','DOCUMENT','SYSTEM_QUERY')),
    CONSTRAINT CK_ChecklistRule_Dates CHECK (EffectiveTo IS NULL OR EffectiveTo >= EffectiveFrom)
);
GO

CREATE TABLE dbo.ChecklistRun (
    ChecklistRunID       BIGINT IDENTITY(1,1) NOT NULL,
    FarmID               BIGINT NOT NULL,
    CropSeasonID         BIGINT NULL,
    CreatedByMemberID    BIGINT NOT NULL,
    RunName              NVARCHAR(200) NOT NULL,
    StartedAt            DATETIME2(0) NOT NULL CONSTRAINT DF_ChecklistRun_Started DEFAULT (SYSUTCDATETIME()),
    CompletedAt          DATETIME2(0) NULL,
    OverallStatus        VARCHAR(20) NOT NULL CONSTRAINT DF_ChecklistRun_Status DEFAULT ('IN_PROGRESS'),
    ScorePercent         DECIMAL(5,2) NULL,
    Notes                NVARCHAR(2000) NULL,
    CONSTRAINT PK_ChecklistRun PRIMARY KEY (ChecklistRunID),
    CONSTRAINT FK_ChecklistRun_Farm FOREIGN KEY (FarmID) REFERENCES dbo.Farm(FarmID),
    CONSTRAINT FK_ChecklistRun_Season FOREIGN KEY (CropSeasonID) REFERENCES dbo.CropSeason(CropSeasonID),
    CONSTRAINT FK_ChecklistRun_Member FOREIGN KEY (CreatedByMemberID) REFERENCES dbo.FarmMember(FarmMemberID),
    CONSTRAINT CK_ChecklistRun_Status CHECK (OverallStatus IN ('IN_PROGRESS','PASS','FAIL','NEEDS_REVIEW')),
    CONSTRAINT CK_ChecklistRun_Score CHECK (ScorePercent IS NULL OR ScorePercent BETWEEN 0 AND 100),
    CONSTRAINT CK_ChecklistRun_Dates CHECK (CompletedAt IS NULL OR CompletedAt >= StartedAt)
);
GO

CREATE TABLE dbo.ChecklistResult (
    ChecklistResultID    BIGINT IDENTITY(1,1) NOT NULL,
    ChecklistRunID       BIGINT NOT NULL,
    ChecklistRuleID      BIGINT NOT NULL,
    ResultStatus         VARCHAR(20) NOT NULL,
    ActualValue          NVARCHAR(1000) NULL,
    EvidenceUrl          NVARCHAR(1000) NULL,
    Explanation          NVARCHAR(MAX) NULL,
    EvaluatedAt          DATETIME2(0) NOT NULL CONSTRAINT DF_ChecklistResult_At DEFAULT (SYSUTCDATETIME()),
    CONSTRAINT PK_ChecklistResult PRIMARY KEY (ChecklistResultID),
    CONSTRAINT UQ_ChecklistResult UNIQUE (ChecklistRunID, ChecklistRuleID),
    CONSTRAINT FK_ChecklistResult_Run FOREIGN KEY (ChecklistRunID) REFERENCES dbo.ChecklistRun(ChecklistRunID) ON DELETE CASCADE,
    CONSTRAINT FK_ChecklistResult_Rule FOREIGN KEY (ChecklistRuleID) REFERENCES dbo.ChecklistRule(ChecklistRuleID),
    CONSTRAINT CK_ChecklistResult_Status CHECK (ResultStatus IN ('PASS','FAIL','NOT_APPLICABLE','NOT_EVALUATED'))
);
GO

CREATE TABLE dbo.AssessmentCriterion (
    AssessmentCriterionID BIGINT IDENTITY(1,1) NOT NULL,
    CriterionCode        VARCHAR(50) NOT NULL,
    CriterionName        NVARCHAR(250) NOT NULL,
    Category             VARCHAR(50) NOT NULL,
    Description          NVARCHAR(MAX) NOT NULL,
    MaxScore             DECIMAL(8,2) NOT NULL,
    IsCritical           BIT NOT NULL CONSTRAINT DF_AssessmentCriterion_Critical DEFAULT (0),
    IsActive             BIT NOT NULL CONSTRAINT DF_AssessmentCriterion_Active DEFAULT (1),
    CONSTRAINT PK_AssessmentCriterion PRIMARY KEY (AssessmentCriterionID),
    CONSTRAINT UQ_AssessmentCriterion_Code UNIQUE (CriterionCode),
    CONSTRAINT CK_AssessmentCriterion_Score CHECK (MaxScore > 0)
);
GO

CREATE TABLE dbo.InternalAssessment (
    InternalAssessmentID BIGINT IDENTITY(1,1) NOT NULL,
    FarmID               BIGINT NOT NULL,
    ProductionAreaID     BIGINT NULL,
    ConductedByMemberID  BIGINT NOT NULL,
    AssessmentDate       DATE NOT NULL,
    AssessmentName       NVARCHAR(200) NOT NULL,
    Status               VARCHAR(20) NOT NULL CONSTRAINT DF_InternalAssessment_Status DEFAULT ('DRAFT'),
    TotalScore           DECIMAL(10,2) NULL,
    Conclusion           NVARCHAR(MAX) NULL,
    CorrectiveAction     NVARCHAR(MAX) NULL,
    CONSTRAINT PK_InternalAssessment PRIMARY KEY (InternalAssessmentID),
    CONSTRAINT FK_InternalAssessment_Farm FOREIGN KEY (FarmID) REFERENCES dbo.Farm(FarmID),
    CONSTRAINT FK_InternalAssessment_Area FOREIGN KEY (ProductionAreaID) REFERENCES dbo.ProductionArea(ProductionAreaID),
    CONSTRAINT FK_InternalAssessment_Member FOREIGN KEY (ConductedByMemberID) REFERENCES dbo.FarmMember(FarmMemberID),
    CONSTRAINT CK_InternalAssessment_Status CHECK (Status IN ('DRAFT','COMPLETED','APPROVED','CANCELLED')),
    CONSTRAINT CK_InternalAssessment_Score CHECK (TotalScore IS NULL OR TotalScore >= 0)
);
GO

CREATE TABLE dbo.InternalAssessmentItem (
    InternalAssessmentItemID BIGINT IDENTITY(1,1) NOT NULL,
    InternalAssessmentID BIGINT NOT NULL,
    AssessmentCriterionID BIGINT NOT NULL,
    ResultStatus         VARCHAR(20) NOT NULL,
    Score                DECIMAL(8,2) NULL,
    Finding              NVARCHAR(MAX) NULL,
    EvidenceUrl          NVARCHAR(1000) NULL,
    CONSTRAINT PK_InternalAssessmentItem PRIMARY KEY (InternalAssessmentItemID),
    CONSTRAINT UQ_InternalAssessmentItem UNIQUE (InternalAssessmentID, AssessmentCriterionID),
    CONSTRAINT FK_InternalItem_Assessment FOREIGN KEY (InternalAssessmentID) REFERENCES dbo.InternalAssessment(InternalAssessmentID) ON DELETE CASCADE,
    CONSTRAINT FK_InternalItem_Criterion FOREIGN KEY (AssessmentCriterionID) REFERENCES dbo.AssessmentCriterion(AssessmentCriterionID),
    CONSTRAINT CK_InternalItem_Status CHECK (ResultStatus IN ('COMPLIANT','NON_COMPLIANT','PARTIAL','NOT_APPLICABLE')),
    CONSTRAINT CK_InternalItem_Score CHECK (Score IS NULL OR Score >= 0)
);
GO

CREATE TABLE dbo.TrainingRecord (
    TrainingRecordID     BIGINT IDENTITY(1,1) NOT NULL,
    FarmID               BIGINT NOT NULL,
    OrganizedByMemberID  BIGINT NOT NULL,
    TrainingTitle        NVARCHAR(250) NOT NULL,
    TrainingTopic        NVARCHAR(250) NULL,
    TrainerName          NVARCHAR(200) NULL,
    StartedAt            DATETIME2(0) NOT NULL,
    EndedAt              DATETIME2(0) NULL,
    Location             NVARCHAR(250) NULL,
    DocumentUrl          NVARCHAR(1000) NULL,
    Notes                NVARCHAR(2000) NULL,
    CONSTRAINT PK_TrainingRecord PRIMARY KEY (TrainingRecordID),
    CONSTRAINT FK_TrainingRecord_Farm FOREIGN KEY (FarmID) REFERENCES dbo.Farm(FarmID),
    CONSTRAINT FK_TrainingRecord_Organizer FOREIGN KEY (OrganizedByMemberID) REFERENCES dbo.FarmMember(FarmMemberID),
    CONSTRAINT CK_TrainingRecord_Dates CHECK (EndedAt IS NULL OR EndedAt >= StartedAt)
);
GO

CREATE TABLE dbo.TrainingAttendance (
    TrainingRecordID     BIGINT NOT NULL,
    FarmWorkerID         BIGINT NOT NULL,
    AttendanceStatus     VARCHAR(20) NOT NULL CONSTRAINT DF_TrainingAttendance_Status DEFAULT ('REGISTERED'),
    Result               NVARCHAR(500) NULL,
    CONSTRAINT PK_TrainingAttendance PRIMARY KEY (TrainingRecordID, FarmWorkerID),
    CONSTRAINT FK_TrainingAttendance_Training FOREIGN KEY (TrainingRecordID) REFERENCES dbo.TrainingRecord(TrainingRecordID) ON DELETE CASCADE,
    CONSTRAINT FK_TrainingAttendance_Worker FOREIGN KEY (FarmWorkerID) REFERENCES dbo.FarmWorker(FarmWorkerID),
    CONSTRAINT CK_TrainingAttendance_Status CHECK (AttendanceStatus IN ('REGISTERED','ATTENDED','ABSENT','COMPLETED'))
);
GO

CREATE TABLE dbo.ProductionReport (
    ProductionReportID   BIGINT IDENTITY(1,1) NOT NULL,
    FarmID               BIGINT NOT NULL,
    CropSeasonID         BIGINT NOT NULL,
    GeneratedByMemberID  BIGINT NOT NULL,
    ReportType           VARCHAR(30) NOT NULL,
    PeriodFrom           DATE NOT NULL,
    PeriodTo             DATE NOT NULL,
    GeneratedAt          DATETIME2(0) NOT NULL CONSTRAINT DF_ProductionReport_Generated DEFAULT (SYSUTCDATETIME()),
    ReportDataJson       NVARCHAR(MAX) NOT NULL,
    FileUrl              NVARCHAR(1000) NULL,
    CONSTRAINT PK_ProductionReport PRIMARY KEY (ProductionReportID),
    CONSTRAINT FK_ProductionReport_Farm FOREIGN KEY (FarmID) REFERENCES dbo.Farm(FarmID),
    CONSTRAINT FK_ProductionReport_Season FOREIGN KEY (CropSeasonID) REFERENCES dbo.CropSeason(CropSeasonID),
    CONSTRAINT FK_ProductionReport_Member FOREIGN KEY (GeneratedByMemberID) REFERENCES dbo.FarmMember(FarmMemberID),
    CONSTRAINT CK_ProductionReport_Type CHECK (ReportType IN ('PRODUCTION','COST','HARVEST','MATERIAL','TRACEABILITY','COMPLIANCE')),
    CONSTRAINT CK_ProductionReport_Period CHECK (PeriodTo >= PeriodFrom),
    CONSTRAINT CK_ProductionReport_Json CHECK (ISJSON(ReportDataJson) = 1)
);
GO

/*==============================================================
  7. KNOWLEDGE AND AI ASSISTANCE
==============================================================*/

CREATE TABLE dbo.KnowledgeArticle (
    KnowledgeArticleID   BIGINT IDENTITY(1,1) NOT NULL,
    CreatedByUserID      BIGINT NOT NULL,
    Title                NVARCHAR(250) NOT NULL,
    Slug                 VARCHAR(250) NOT NULL,
    Summary              NVARCHAR(1000) NULL,
    Content              NVARCHAR(MAX) NOT NULL,
    Category             NVARCHAR(100) NULL,
    SourceUrl            NVARCHAR(1000) NULL,
    Status               VARCHAR(20) NOT NULL CONSTRAINT DF_KnowledgeArticle_Status DEFAULT ('DRAFT'),
    PublishedAt          DATETIME2(0) NULL,
    CreatedAt            DATETIME2(0) NOT NULL CONSTRAINT DF_KnowledgeArticle_Created DEFAULT (SYSUTCDATETIME()),
    UpdatedAt            DATETIME2(0) NOT NULL CONSTRAINT DF_KnowledgeArticle_Updated DEFAULT (SYSUTCDATETIME()),
    CONSTRAINT PK_KnowledgeArticle PRIMARY KEY (KnowledgeArticleID),
    CONSTRAINT UQ_KnowledgeArticle_Slug UNIQUE (Slug),
    CONSTRAINT FK_KnowledgeArticle_User FOREIGN KEY (CreatedByUserID) REFERENCES dbo.AppUser(UserID),
    CONSTRAINT CK_KnowledgeArticle_Status CHECK (Status IN ('DRAFT','PUBLISHED','INACTIVE'))
);
GO

CREATE TABLE dbo.AIConversation (
    AIConversationID     BIGINT IDENTITY(1,1) NOT NULL,
    UserID               BIGINT NOT NULL,
    FarmID               BIGINT NULL,
    PlotID               BIGINT NULL,
    CropSeasonID         BIGINT NULL,
    Title                NVARCHAR(250) NULL,
    Purpose              VARCHAR(30) NOT NULL,
    Status               VARCHAR(20) NOT NULL CONSTRAINT DF_AIConversation_Status DEFAULT ('ACTIVE'),
    CreatedAt            DATETIME2(0) NOT NULL CONSTRAINT DF_AIConversation_Created DEFAULT (SYSUTCDATETIME()),
    UpdatedAt            DATETIME2(0) NOT NULL CONSTRAINT DF_AIConversation_Updated DEFAULT (SYSUTCDATETIME()),
    CONSTRAINT PK_AIConversation PRIMARY KEY (AIConversationID),
    CONSTRAINT FK_AIConversation_User FOREIGN KEY (UserID) REFERENCES dbo.AppUser(UserID),
    CONSTRAINT FK_AIConversation_Farm FOREIGN KEY (FarmID) REFERENCES dbo.Farm(FarmID),
    CONSTRAINT FK_AIConversation_Plot FOREIGN KEY (PlotID) REFERENCES dbo.Plot(PlotID),
    CONSTRAINT FK_AIConversation_Season FOREIGN KEY (CropSeasonID) REFERENCES dbo.CropSeason(CropSeasonID),
    CONSTRAINT CK_AIConversation_Purpose CHECK (Purpose IN ('DATA_ENTRY','KNOWLEDGE_QA','SUMMARY','RECOMMENDATION','OTHER')),
    CONSTRAINT CK_AIConversation_Status CHECK (Status IN ('ACTIVE','ARCHIVED','DELETED'))
);
GO

CREATE TABLE dbo.AIMessage (
    AIMessageID          BIGINT IDENTITY(1,1) NOT NULL,
    AIConversationID     BIGINT NOT NULL,
    SenderRole           VARCHAR(20) NOT NULL,
    MessageContent       NVARCHAR(MAX) NOT NULL,
    ModelName            VARCHAR(100) NULL,
    PromptTokens         INT NULL,
    CompletionTokens     INT NULL,
    IsConfirmedByUser    BIT NOT NULL CONSTRAINT DF_AIMessage_Confirmed DEFAULT (0),
    CreatedAt            DATETIME2(0) NOT NULL CONSTRAINT DF_AIMessage_Created DEFAULT (SYSUTCDATETIME()),
    CONSTRAINT PK_AIMessage PRIMARY KEY (AIMessageID),
    CONSTRAINT FK_AIMessage_Conversation FOREIGN KEY (AIConversationID) REFERENCES dbo.AIConversation(AIConversationID) ON DELETE CASCADE,
    CONSTRAINT CK_AIMessage_Role CHECK (SenderRole IN ('SYSTEM','USER','ASSISTANT','TOOL')),
    CONSTRAINT CK_AIMessage_Tokens CHECK (
        (PromptTokens IS NULL OR PromptTokens >= 0)
        AND (CompletionTokens IS NULL OR CompletionTokens >= 0)
    )
);
GO

CREATE TABLE dbo.AIFeedback (
    AIFeedbackID         BIGINT IDENTITY(1,1) NOT NULL,
    AIMessageID          BIGINT NOT NULL,
    SubmittedByUserID    BIGINT NOT NULL,
    Rating               TINYINT NULL,
    FeedbackType         VARCHAR(30) NOT NULL,
    Comment              NVARCHAR(2000) NULL,
    Status               VARCHAR(20) NOT NULL CONSTRAINT DF_AIFeedback_Status DEFAULT ('OPEN'),
    SubmittedAt          DATETIME2(0) NOT NULL CONSTRAINT DF_AIFeedback_Submitted DEFAULT (SYSUTCDATETIME()),
    CONSTRAINT PK_AIFeedback PRIMARY KEY (AIFeedbackID),
    CONSTRAINT FK_AIFeedback_Message FOREIGN KEY (AIMessageID) REFERENCES dbo.AIMessage(AIMessageID),
    CONSTRAINT FK_AIFeedback_User FOREIGN KEY (SubmittedByUserID) REFERENCES dbo.AppUser(UserID),
    CONSTRAINT CK_AIFeedback_Rating CHECK (Rating IS NULL OR Rating BETWEEN 1 AND 5),
    CONSTRAINT CK_AIFeedback_Type CHECK (FeedbackType IN ('HELPFUL','INCORRECT','HALLUCINATION','UNSAFE','PRIVACY','OTHER')),
    CONSTRAINT CK_AIFeedback_Status CHECK (Status IN ('OPEN','REVIEWING','RESOLVED','REJECTED'))
);
GO

CREATE TABLE dbo.AIFeedbackReply (
    AIFeedbackReplyID    BIGINT IDENTITY(1,1) NOT NULL,
    AIFeedbackID         BIGINT NOT NULL,
    WrittenByUserID      BIGINT NOT NULL,
    ReplyContent         NVARCHAR(MAX) NOT NULL,
    CreatedAt            DATETIME2(0) NOT NULL CONSTRAINT DF_AIFeedbackReply_Created DEFAULT (SYSUTCDATETIME()),
    CONSTRAINT PK_AIFeedbackReply PRIMARY KEY (AIFeedbackReplyID),
    CONSTRAINT FK_AIFeedbackReply_Feedback FOREIGN KEY (AIFeedbackID) REFERENCES dbo.AIFeedback(AIFeedbackID) ON DELETE CASCADE,
    CONSTRAINT FK_AIFeedbackReply_User FOREIGN KEY (WrittenByUserID) REFERENCES dbo.AppUser(UserID)
);
GO

/*==============================================================
  8. USEFUL INDEXES
==============================================================*/

CREATE INDEX IX_FarmRegistration_Applicant_Status ON dbo.FarmRegistration(ApplicantUserID, Status);
CREATE INDEX IX_FarmMember_User_Status ON dbo.FarmMember(UserID, Status);
CREATE INDEX IX_StaffAreaAssignment_Area_Active ON dbo.StaffAreaAssignment(ProductionAreaID, IsActive);
CREATE INDEX IX_Plot_Area_Status ON dbo.Plot(ProductionAreaID, Status);
CREATE INDEX IX_CropSeason_Plot_Status ON dbo.CropSeason(PlotID, Status);
CREATE INDEX IX_FarmTask_Farm_Status_DueAt ON dbo.FarmTask(FarmID, Status, DueAt);
CREATE INDEX IX_FarmingActivity_Season_StartedAt ON dbo.FarmingActivity(CropSeasonID, StartedAt);
CREATE INDEX IX_MaterialUsage_Activity_UsedAt ON dbo.MaterialUsage(FarmingActivityID, UsedAt);
CREATE INDEX IX_Expense_Season_Date ON dbo.Expense(CropSeasonID, ExpenseDate);
CREATE INDEX IX_HarvestRecord_Season_Date ON dbo.HarvestRecord(CropSeasonID, HarvestedAt);
CREATE INDEX IX_ChecklistRun_Farm_StartedAt ON dbo.ChecklistRun(FarmID, StartedAt);
CREATE INDEX IX_AIConversation_User_CreatedAt ON dbo.AIConversation(UserID, CreatedAt);
CREATE INDEX IX_AIMessage_Conversation_CreatedAt ON dbo.AIMessage(AIConversationID, CreatedAt);
GO

/*==============================================================
  9. EXAMPLE TRACEABILITY VIEW
==============================================================*/

CREATE VIEW dbo.vw_HarvestTraceability
AS
SELECT
    h.HarvestRecordID,
    h.HarvestLotCode,
    h.TraceabilityCode,
    h.HarvestedAt,
    h.Quantity,
    h.Unit,
    cs.CropSeasonID,
    cs.SeasonCode,
    cs.SeasonName,
    cc.CategoryName AS CropName,
    p.PlotID,
    p.PlotCode,
    p.PlotName,
    pa.ProductionAreaID,
    pa.AreaCode,
    pa.AreaName,
    f.FarmID,
    f.FarmCode,
    f.FarmName
FROM dbo.HarvestRecord h
JOIN dbo.CropSeason cs ON cs.CropSeasonID = h.CropSeasonID
JOIN dbo.CropCategory cc ON cc.CropCategoryID = cs.CropCategoryID
JOIN dbo.Plot p ON p.PlotID = cs.PlotID
JOIN dbo.ProductionArea pa ON pa.ProductionAreaID = p.ProductionAreaID
JOIN dbo.Farm f ON f.FarmID = cs.FarmID;
GO

/*==============================================================
  IMPORTANT APPLICATION RULES
  SQL foreign keys ensure entity existence. The service layer or triggers
  must additionally ensure that related rows belong to the same Farm, e.g.:
  - FarmMember.FarmID matches ProductionArea.FarmID for assignments.
  - CropSeason.FarmID matches its Plot -> ProductionArea -> Farm.
  - FarmTask references only areas/plots/seasons in FarmTask.FarmID.
  - AI context is accessible to AIConversation.UserID under RBAC.
  - AI output is a draft; IsConfirmedByUser must be true before operational use.
==============================================================*/

SELECT N'MyCropDiary database schema created successfully.' AS Result;
GO

/*==============================================================
  10. DỮ LIỆU MẪU (SAMPLE / SEED DATA) ĐỂ TEST TOÀN BỘ CÁC API
  
  MẬT KHẨU ĐĂNG NHẬP CHUNG: Password@123
  HASH BCRYPT: $2a$10$8jIMVzui5SoBFkUR1udxQO3Nqp09gvVoXobsoScb/zlisb4JIieDC
  MÃ OTP MẶC ĐỊNH TEST VERIFY / RESET-PASS: 123456
  HASH BCRYPT CỦA OTP: $2a$10$0pPbCigvJwUsf6/sskJIe.SwZ2mNtfIFaXiXTCF1kCjTwmXi6ghqO

  DANH SÁCH TÀI KHOẢN MẪU:
  1. Quản trị hệ thống:       admin@cropdiary.com           (Role: ADMIN, Status: ACTIVE)
  2. Chủ trang trại 1:        owner1@dalatfarm.com          (Role: USER,  Status: ACTIVE - FarmID: 1)
  3. Chủ trang trại 2:        owner2@mekongfarm.com         (Role: USER,  Status: ACTIVE - FarmID: 2)
  4. Nhân viên kỹ thuật 1:    staff1@dalatfarm.com          (Role: USER,  Status: ACTIVE - FarmID: 1)
  5. Nhân viên kỹ thuật 2:    staff2@dalatfarm.com          (Role: USER,  Status: ACTIVE - FarmID: 1)
  6. Tài khoản chờ xác thực:  pending.user@cropdiary.com    (Role: USER,  Status: PENDING - Test verify OTP: 123456)
  7. Tài khoản quên mật khẩu: resetpass.user@cropdiary.com  (Role: USER,  Status: ACTIVE - Test reset pass OTP: 123456)
  8. Tài khoản bị khóa:       locked.user@cropdiary.com     (Role: USER,  Status: LOCKED - Test lock error)
  9. Người nộp đơn trang trại: applicant@newfarm.com        (Role: USER,  Status: ACTIVE - Có đơn chờ Admin duyệt)
  10. Nhân viên mới được mời: invited.staff@dalatfarm.com   (Role: USER,  Status: ACTIVE - Status trong Farm: INVITED)
==============================================================*/

PRINT N'Starting sample data insertion...';
GO

-- 10.1 Xóa dữ liệu cũ theo thứ tự ngược lại ràng buộc khóa ngoại (để có thể chạy script nhiều lần)
DELETE FROM dbo.AIFeedbackReply;
DELETE FROM dbo.AIFeedback;
DELETE FROM dbo.AIMessage;
DELETE FROM dbo.AIConversation;
DELETE FROM dbo.KnowledgeArticle;
DELETE FROM dbo.ProductionReport;
DELETE FROM dbo.TrainingAttendance;
DELETE FROM dbo.TrainingRecord;
DELETE FROM dbo.InternalAssessmentItem;
DELETE FROM dbo.InternalAssessment;
DELETE FROM dbo.AssessmentCriterion;
DELETE FROM dbo.ChecklistResult;
DELETE FROM dbo.ChecklistRun;
DELETE FROM dbo.ChecklistRule;
DELETE FROM dbo.ProductSale;
DELETE FROM dbo.HarvestActivitySource;
DELETE FROM dbo.HarvestRecord;
DELETE FROM dbo.Expense;
DELETE FROM dbo.MaterialUsage;
DELETE FROM dbo.InputPurchaseDetail;
DELETE FROM dbo.InputPurchase;
DELETE FROM dbo.Material;
DELETE FROM dbo.Supplier;
DELETE FROM dbo.ActivityWorker;
DELETE FROM dbo.FarmingActivity;
DELETE FROM dbo.FarmTaskWorker;
DELETE FROM dbo.FarmTask;
DELETE FROM dbo.CropSeason;
DELETE FROM dbo.CropCategory;
DELETE FROM dbo.PlotConditionObservation;
DELETE FROM dbo.SoilAssessment;
DELETE FROM dbo.WaterAssessment;
DELETE FROM dbo.AreaRiskAssessment;
DELETE FROM dbo.PlotWaterSource;
DELETE FROM dbo.WaterSource;
DELETE FROM dbo.Plot;
DELETE FROM dbo.StaffAreaAssignment;
DELETE FROM dbo.ProductionArea;
DELETE FROM dbo.FarmWorker;
DELETE FROM dbo.FarmMember;
DELETE FROM dbo.Farm;
DELETE FROM dbo.FarmRegistration;
DELETE FROM dbo.AccountToken;
DELETE FROM dbo.AppUser;
GO

-- 10.2 Bảng AppUser
SET IDENTITY_INSERT dbo.AppUser ON;
INSERT INTO dbo.AppUser (
    UserID, Email, PasswordHash, FullName, PhoneNumber, SystemRole, AccountStatus, EmailVerifiedAt, CreatedAt, UpdatedAt
) VALUES 
(1, 'admin@cropdiary.com', '$2a$10$8jIMVzui5SoBFkUR1udxQO3Nqp09gvVoXobsoScb/zlisb4JIieDC', N'Quản Trị Viên Hệ Thống', '0901234567', 'ADMIN', 'ACTIVE', '2025-01-01 08:00:00', '2025-01-01 08:00:00', '2025-01-01 08:00:00'),
(2, 'owner1@dalatfarm.com', '$2a$10$8jIMVzui5SoBFkUR1udxQO3Nqp09gvVoXobsoScb/zlisb4JIieDC', N'Trần Văn Chủ Nông Trại Đà Lạt', '0912345678', 'USER', 'ACTIVE', '2025-01-02 08:00:00', '2025-01-02 08:00:00', '2025-01-02 08:00:00'),
(3, 'owner2@mekongfarm.com', '$2a$10$8jIMVzui5SoBFkUR1udxQO3Nqp09gvVoXobsoScb/zlisb4JIieDC', N'Lê Thị Chủ Vườn Miền Tây', '0923456789', 'USER', 'ACTIVE', '2025-01-03 08:00:00', '2025-01-03 08:00:00', '2025-01-03 08:00:00'),
(4, 'staff1@dalatfarm.com', '$2a$10$8jIMVzui5SoBFkUR1udxQO3Nqp09gvVoXobsoScb/zlisb4JIieDC', N'Nguyễn Kỹ Thuật Viên Đà Lạt', '0934567890', 'USER', 'ACTIVE', '2025-01-04 08:00:00', '2025-01-04 08:00:00', '2025-01-04 08:00:00'),
(5, 'staff2@dalatfarm.com', '$2a$10$8jIMVzui5SoBFkUR1udxQO3Nqp09gvVoXobsoScb/zlisb4JIieDC', N'Hoàng Quản Lý Vùng Trồng', '0945678901', 'USER', 'ACTIVE', '2025-01-04 09:00:00', '2025-01-04 09:00:00', '2025-01-04 09:00:00'),
(6, 'pending.user@cropdiary.com', '$2a$10$8jIMVzui5SoBFkUR1udxQO3Nqp09gvVoXobsoScb/zlisb4JIieDC', N'Phạm Đăng Ký Chờ Kích Hoạt', '0956789012', 'USER', 'PENDING', NULL, '2026-02-01 10:00:00', '2026-02-01 10:00:00'),
(7, 'resetpass.user@cropdiary.com', '$2a$10$8jIMVzui5SoBFkUR1udxQO3Nqp09gvVoXobsoScb/zlisb4JIieDC', N'Vũ Yêu Cầu Đặt Lại Mật Khẩu', '0967890123', 'USER', 'ACTIVE', '2025-01-05 10:00:00', '2025-01-05 10:00:00', '2025-01-05 10:00:00'),
(8, 'locked.user@cropdiary.com', '$2a$10$8jIMVzui5SoBFkUR1udxQO3Nqp09gvVoXobsoScb/zlisb4JIieDC', N'Đặng Tài Khoản Đang Bị Khóa', '0978901234', 'USER', 'LOCKED', '2025-01-06 11:00:00', '2025-01-06 11:00:00', '2025-01-06 11:00:00'),
(9, 'applicant@newfarm.com', '$2a$10$8jIMVzui5SoBFkUR1udxQO3Nqp09gvVoXobsoScb/zlisb4JIieDC', N'Bùi Văn Nộp Đơn Trang Trại', '0989012345', 'USER', 'ACTIVE', '2025-01-07 14:00:00', '2025-01-07 14:00:00', '2025-01-07 14:00:00'),
(10, 'invited.staff@dalatfarm.com', '$2a$10$8jIMVzui5SoBFkUR1udxQO3Nqp09gvVoXobsoScb/zlisb4JIieDC', N'Triệu Kỹ Thuật Viên Được Mời', '0990123456', 'USER', 'ACTIVE', '2025-01-08 15:00:00', '2025-01-08 15:00:00', '2025-01-08 15:00:00');
SET IDENTITY_INSERT dbo.AppUser OFF;
GO

-- 10.3 Bảng AccountToken (Hỗ trợ test Verify OTP, Reset Password và Refresh Token)
SET IDENTITY_INSERT dbo.AccountToken ON;
INSERT INTO dbo.AccountToken (
    TokenID, UserID, TokenHash, TokenType, ExpiresAt, UsedAt, RevokedAt, CreatedAt
) VALUES 
(1, 6, '$2a$10$0pPbCigvJwUsf6/sskJIe.SwZ2mNtfIFaXiXTCF1kCjTwmXi6ghqO', 'EMAIL_OTP', '2026-12-31 23:59:59', NULL, NULL, '2026-02-01 10:00:00'),
(2, 7, '$2a$10$0pPbCigvJwUsf6/sskJIe.SwZ2mNtfIFaXiXTCF1kCjTwmXi6ghqO', 'PASSWORD_RESET', '2026-12-31 23:59:59', NULL, NULL, '2026-02-01 10:00:00'),
(3, 2, '4d03923010b9d997d4c885bb4a123984e1b802613d508c909e7f722a46e1074e', 'REFRESH_TOKEN', '2026-12-31 23:59:59', NULL, NULL, '2026-02-01 10:00:00');
SET IDENTITY_INSERT dbo.AccountToken OFF;
GO

-- 10.4 Bảng FarmRegistration (Đơn đăng ký mở trang trại)
SET IDENTITY_INSERT dbo.FarmRegistration ON;
INSERT INTO dbo.FarmRegistration (
    RegistrationID, ApplicantUserID, FarmName, AddressLine, Province, District, Ward, Description, DocumentUrl, Status, HandlerUserID, SubmittedAt, HandledAt, RejectionReason
) VALUES 
(1, 2, N'Trang Trại Rau Củ Sạch Đà Lạt', N'123 Đường Mai Anh Đào, Phường 8', N'Tỉnh Lâm Đồng', N'Thành phố Đà Lạt', N'Phường 8', N'Trang trại chuyên canh rau thủy canh và củ quả theo tiêu chuẩn VietGAP', N'https://storage.cropdiary.com/docs/reg-dalat-2025.pdf', 'APPROVED', 1, '2025-01-03 08:30:00', '2025-01-05 09:00:00', NULL),
(2, 3, N'Hợp Tác Xã Cây Ăn Trái Mekong Delta', N'456 Quốc lộ 1A, Xã Long An', N'Tỉnh Tiền Giang', N'Huyện Châu Thành', N'Xã Long An', N'Vùng chuyên canh sầu riêng Ri6 và bưởi da xanh chất lượng xuất khẩu', N'https://storage.cropdiary.com/docs/reg-mekong-2025.pdf', 'APPROVED', 1, '2025-01-08 14:00:00', '2025-01-10 10:00:00', NULL),
(3, 9, N'Nông Trại Hữu Cơ Xanh Ba Vì', N'Thôn Yên Sơn, Xã Ba Vì', N'Thành phố Hà Nội', N'Huyện Ba Vì', N'Xã Ba Vì', N'Đơn đăng ký trang trại trồng rau hữu cơ và dược liệu sạch (Dùng để test API Admin Duyệt/Từ chối)', N'https://storage.cropdiary.com/docs/reg-bavi-2026.pdf', 'PENDING', NULL, '2026-02-15 08:00:00', NULL, NULL),
(4, 9, N'Khu Canh Tác Thử Nghiệm Bến Cát', N'Khu phố 3, Phường Mỹ Phước', N'Tỉnh Bình Dương', N'Thị xã Bến Cát', N'Phường Mỹ Phước', N'Dự án trồng nấm công nghệ cao', N'https://storage.cropdiary.com/docs/reg-bencat-2025.pdf', 'REJECTED', 1, '2025-01-11 09:30:00', '2025-01-12 16:00:00', N'Hồ sơ chưa có giấy chứng nhận quyền sử dụng đất nông nghiệp hợp lệ.');
SET IDENTITY_INSERT dbo.FarmRegistration OFF;
GO

-- 10.5 Bảng Farm (Trang trại đã được tạo)
SET IDENTITY_INSERT dbo.Farm ON;
INSERT INTO dbo.Farm (
    FarmID, RegistrationID, FarmCode, FarmName, AddressLine, Province, District, Ward, Latitude, Longitude, TotalAreaM2, Status, CreatedAt
) VALUES 
(1, 1, 'FARM-DL-001', N'Trang Trại Rau Củ Sạch Đà Lạt', N'123 Đường Mai Anh Đào, Phường 8', N'Tỉnh Lâm Đồng', N'Thành phố Đà Lạt', N'Phường 8', 11.954500, 108.445200, 25000.00, 'ACTIVE', '2025-01-05 09:15:00'),
(2, 2, 'FARM-MK-002', N'Hợp Tác Xã Cây Ăn Trái Mekong Delta', N'456 Quốc lộ 1A, Xã Long An', N'Tỉnh Tiền Giang', N'Huyện Châu Thành', N'Xã Long An', 10.375600, 106.331200, 50000.00, 'ACTIVE', '2025-01-10 10:30:00');
SET IDENTITY_INSERT dbo.Farm OFF;
GO

-- 10.6 Bảng FarmMember (Thành viên trang trại - OWNER & STAFF)
SET IDENTITY_INSERT dbo.FarmMember ON;
INSERT INTO dbo.FarmMember (
    FarmMemberID, FarmID, UserID, FarmRole, JobTitle, JoinedAt, LeftAt, Status
) VALUES 
(1, 1, 2, 'OWNER', N'Chủ Trang Trại kiêm Giám Đốc', '2025-01-05', NULL, 'ACTIVE'),
(2, 1, 4, 'STAFF', N'Kỹ Sư Nông Học Phụ Trách Dinh Dưỡng', '2025-01-10', NULL, 'ACTIVE'),
(3, 1, 5, 'STAFF', N'Kỹ Thuật Viên Giám Sát Nhà Màng', '2025-01-10', NULL, 'ACTIVE'),
(4, 1, 10, 'STAFF', N'Kỹ Thuật Viên Tập Sự', '2026-02-01', NULL, 'INVITED'),
(5, 2, 3, 'OWNER', N'Chủ Vườn & Trưởng Hợp Tác Xã', '2025-01-10', NULL, 'ACTIVE');
SET IDENTITY_INSERT dbo.FarmMember OFF;
GO

-- 10.7 Bảng FarmWorker (Công nhân lao động tại nông trại)
SET IDENTITY_INSERT dbo.FarmWorker ON;
INSERT INTO dbo.FarmWorker (
    FarmWorkerID, FarmID, WorkerCode, FullName, PhoneNumber, DateOfBirth, HireDate, Status, Notes
) VALUES 
(1, 1, 'CN-DL-01', N'Nguyễn Văn Nam', '0981112233', '1988-05-12', '2025-02-01', 'ACTIVE', N'Chuyên vận hành pha dung dịch thủy canh'),
(2, 1, 'CN-DL-02', N'Trần Thị Bích', '0982223344', '1992-09-20', '2025-02-01', 'ACTIVE', N'Chuyên chăm sóc, thụ phấn và thu hoạch'),
(3, 1, 'CN-DL-03', N'Lê Văn Cường', '0983334455', '1985-11-03', '2025-03-15', 'ACTIVE', N'Vận hành cơ giới hóa và hệ thống tưới tự động'),
(4, 2, 'CN-MK-01', N'Phạm Văn Út', '0984445566', '1982-01-18', '2025-02-10', 'ACTIVE', N'Công nhân chuyên tỉa cành và bao trái sầu riêng');
SET IDENTITY_INSERT dbo.FarmWorker OFF;
GO

-- 10.8 Bảng ProductionArea (Khu vực / Phân khu sản xuất)
SET IDENTITY_INSERT dbo.ProductionArea ON;
INSERT INTO dbo.ProductionArea (
    ProductionAreaID, FarmID, AreaCode, AreaName, AreaM2, Description, Status, CreatedAt
) VALUES 
(1, 1, 'KV-RAU-LA', N'Khu Vực Rau Lá Thủy Canh', 10000.00, N'Hệ thống nhà kính chuyên canh xà lách và rau ăn lá công nghệ NFT', 'ACTIVE', '2025-01-12 08:00:00'),
(2, 1, 'KV-CU-QUA', N'Khu Vực Củ Quả Nhà Màng', 12000.00, N'Nhà màng Israel trồng dưa lưới Ichiba và cà chua bi trên giá thể xơ dừa', 'ACTIVE', '2025-01-12 08:30:00'),
(3, 1, 'KV-SO-CHE', N'Khu Nhà Kho & Sơ Chế Đóng Gói', 3000.00, N'Khu vực sơ chế nông sản, kho lạnh và đóng gói tem truy xuất', 'ACTIVE', '2025-01-12 09:00:00'),
(4, 2, 'KV-CAY-TRAI', N'Khu Vực Vườn Cây Ăn Trái Xuất Khẩu', 50000.00, N'Vườn sầu riêng Ri6 và bưởi da xanh đạt chuẩn GlobalGAP', 'ACTIVE', '2025-01-15 08:00:00');
SET IDENTITY_INSERT dbo.ProductionArea OFF;
GO

-- 10.9 Bảng StaffAreaAssignment (Phân công nhân viên phụ trách khu vực)
SET IDENTITY_INSERT dbo.StaffAreaAssignment ON;
INSERT INTO dbo.StaffAreaAssignment (
    AssignmentID, FarmMemberID, ProductionAreaID, AssignedByMemberID, StartDate, EndDate, IsActive
) VALUES 
(1, 2, 1, 1, '2025-01-15', NULL, 1),
(2, 3, 2, 1, '2025-01-15', NULL, 1),
(3, 2, 2, 1, '2025-06-01', '2025-12-31', 0);
SET IDENTITY_INSERT dbo.StaffAreaAssignment OFF;
GO

-- 10.10 Bảng Plot (Lô đất / Nhà trồng)
SET IDENTITY_INSERT dbo.Plot ON;
INSERT INTO dbo.Plot (
    PlotID, ProductionAreaID, PlotCode, PlotName, AreaM2, Latitude, Longitude, BoundaryGeoJson, Status
) VALUES 
(1, 1, 'LOH-01', N'Lô H1 - Xà Lách Thủy Canh', 3000.00, 11.954800, 108.445500, '{"type":"Polygon","coordinates":[[[108.4450,11.9540],[108.4460,11.9540],[108.4460,11.9550],[108.4450,11.9550],[108.4450,11.9540]]]}', 'IN_USE'),
(2, 1, 'LOH-02', N'Lô H2 - Cải Bó Xôi & Cải Kale', 3500.00, 11.955200, 108.446000, '{"type":"Polygon","coordinates":[[[108.4460,11.9540],[108.4470,11.9540],[108.4470,11.9550],[108.4460,11.9550],[108.4460,11.9540]]]}', 'AVAILABLE'),
(3, 2, 'LOG-01', N'Lô G1 - Dưa Lưới Ichiba Nhật', 4000.00, 11.956000, 108.447000, '{"type":"Polygon","coordinates":[[[108.4470,11.9550],[108.4485,11.9550],[108.4485,11.9565],[108.4470,11.9565],[108.4470,11.9550]]]}', 'IN_USE'),
(4, 2, 'LOG-02', N'Lô G2 - Cà Chua Cherry Vàng', 4000.00, 11.956500, 108.447500, '{"type":"Polygon","coordinates":[[[108.4485,11.9550],[108.4500,11.9550],[108.4500,11.9565],[108.4485,11.9565],[108.4485,11.9550]]]}', 'RESTING'),
(5, 4, 'LOM-01', N'Lô M1 - Sầu Riêng Ri6 Chín Sớm', 25000.00, 10.375800, 106.331500, '{"type":"Polygon","coordinates":[[[106.3310,10.3750],[106.3330,10.3750],[106.3330,10.3770],[106.3310,10.3770],[106.3310,10.3750]]]}', 'IN_USE');
SET IDENTITY_INSERT dbo.Plot OFF;
GO

-- 10.11 Bảng WaterSource & PlotWaterSource (Nguồn nước tưới)
SET IDENTITY_INSERT dbo.WaterSource ON;
INSERT INTO dbo.WaterSource (
    WaterSourceID, FarmID, SourceCode, SourceName, SourceType, Description, Status
) VALUES 
(1, 1, 'WS-DL-01', N'Giếng Khoan Tầng Sâu Số 1', 'WELL', N'Nguồn cấp nước chính qua hệ thống lọc RO phục vụ bồn dinh dưỡng thủy canh', 'ACTIVE'),
(2, 1, 'WS-DL-02', N'Hồ Thu Gom Nước Mưa & Hồ Lắng', 'RESERVOIR', N'Hồ dung tích 2500m3 lót bạt HDPE thu nước mái nhà kính', 'ACTIVE'),
(3, 2, 'WS-MK-01', N'Kênh Thủy Lợi Sông Tiền', 'RIVER', N'Trạm bơm tưới tiêu ngọt hóa có giám sát độ mặn', 'ACTIVE');
SET IDENTITY_INSERT dbo.WaterSource OFF;
GO

INSERT INTO dbo.PlotWaterSource (PlotID, WaterSourceID, IsPrimary) VALUES 
(1, 1, 1),
(2, 1, 1),
(3, 2, 1),
(4, 2, 1),
(5, 3, 1);
GO

-- 10.12 Đánh giá nguy cơ, kiểm nghiệm nguồn nước và thổ nhưỡng
SET IDENTITY_INSERT dbo.AreaRiskAssessment ON;
INSERT INTO dbo.AreaRiskAssessment (
    AreaRiskAssessmentID, ProductionAreaID, ConductedByMemberID, AssessmentDate, RiskType, RiskLevel, Findings, MitigationAction, NextReviewDate
) VALUES 
(1, 1, 2, '2026-01-15', 'SOIL_WATER_CONTAMINATION', 'LOW', N'Khu vực nhà kính biệt lập với khu chăn nuôi, độ dốc thoát nước tốt, không có dấu hiệu nhiễm bẩn hóa chất.', N'Duy trì hàng rào cách ly và rãnh thoát lũ xung quanh nhà kính.', '2026-07-15');
SET IDENTITY_INSERT dbo.AreaRiskAssessment OFF;
GO

SET IDENTITY_INSERT dbo.WaterAssessment ON;
INSERT INTO dbo.WaterAssessment (
    WaterAssessmentID, WaterSourceID, ConductedByMemberID, AssessmentDate, LaboratoryName, PH, EColiCFU, ResultStatus, ResultJson, EvidenceUrl, Notes
) VALUES 
(1, 1, 2, '2026-01-20', N'Trung tâm Kỹ thuật Tiêu chuẩn Đo lường Chất lượng 3 (QUATEST 3)', 6.80, 0.00, 'PASS', '{"heavyMetals":{"Pb":"undetected","Cd":"undetected","As":"undetected"},"coliform":0}', N'https://storage.cropdiary.com/reports/water-test-2026.pdf', N'Đạt tiêu chuẩn QCVN 39:2011/BTNMT về chất lượng nước tưới tiêu.');
SET IDENTITY_INSERT dbo.WaterAssessment OFF;
GO

SET IDENTITY_INSERT dbo.SoilAssessment ON;
INSERT INTO dbo.SoilAssessment (
    SoilAssessmentID, PlotID, ConductedByMemberID, AssessmentDate, SoilType, PH, OrganicMatterPercent, ResultStatus, ResultJson, EvidenceUrl, Notes
) VALUES 
(1, 3, 2, '2026-01-18', N'Giá thể xơ dừa lên men xử lý nhiệt', 6.20, 12.50, 'PASS', '{"EC":0.4,"NPK":{"N":1.2,"P":0.8,"K":1.5}}', N'https://storage.cropdiary.com/reports/soil-test-2026.pdf', N'Độ mặn EC thấp, thông thoáng rễ tốt cho dưa lưới phát triển.');
SET IDENTITY_INSERT dbo.SoilAssessment OFF;
GO

SET IDENTITY_INSERT dbo.PlotConditionObservation ON;
INSERT INTO dbo.PlotConditionObservation (
    ObservationID, PlotID, RecordedByMemberID, ObservedAt, ConditionType, Severity, Description, ImageUrl, RecommendedAction
) VALUES 
(1, 1, 2, '2026-02-05 07:30:00', 'PEST_SURVEILLANCE', 'LOW', N'Phát hiện bọ trĩ mật độ thấp dưới mặt lá xà lách ở hàng số 3 và 4.', N'https://storage.cropdiary.com/images/pest-obs-01.jpg', N'Treo bẫy dính màu vàng và phun bổ sung chế phẩm sinh học tinh dầu tỏi ớt.');
SET IDENTITY_INSERT dbo.PlotConditionObservation OFF;
GO

-- 10.13 Danh mục cây trồng, Mùa vụ (CropSeason), Công việc (FarmTask) và Hoạt động (FarmingActivity)
SET IDENTITY_INSERT dbo.CropCategory ON;
INSERT INTO dbo.CropCategory (
    CropCategoryID, CategoryCode, CategoryName, ScientificName, TypicalDurationDays, IsActive
) VALUES 
(1, 'CROP-SALAD', N'Rau Xà Lách', N'Lactuca sativa', 45, 1),
(2, 'CROP-MELON', N'Dưa Lưới Ichiba', N'Cucumis melo', 75, 1),
(3, 'CROP-TOMATO', N'Cà Chua Cherry', N'Solanum lycopersicum', 90, 1),
(4, 'CROP-DURIAN', N'Sầu Riêng Ri6', N'Durio zibethinus', 120, 1);
SET IDENTITY_INSERT dbo.CropCategory OFF;
GO

SET IDENTITY_INSERT dbo.CropSeason ON;
INSERT INTO dbo.CropSeason (
    CropSeasonID, FarmID, PlotID, CropCategoryID, CreatedByMemberID, SeasonCode, SeasonName, VarietyName, StartDate, ExpectedHarvestDate, ActualEndDate, CultivatedAreaM2, Status, Notes
) VALUES 
(1, 1, 1, 1, 2, 'VU-XL-2026-Q1', N'Vụ Xà Lách Thủy Canh Mùa Xuân 2026', N'Lô Lô Xanh Rijk Zwaan', '2026-01-10', '2026-02-25', NULL, 3000.00, 'ACTIVE', N'Canh tác thủy canh hồi lưu tự động trong nhà kính công nghệ cao'),
(2, 1, 3, 2, 2, 'VU-DL-2026-Q1', N'Vụ Dưa Lưới Ichiba Nhật Bản Xuân Hè', N'Dưa lưới Taka Ichiba', '2026-01-05', '2026-03-20', NULL, 4000.00, 'HARVESTING', N'Trồng trên giá thể túi xơ dừa, thụ phấn bằng ong mật tự nhiên'),
(3, 2, 5, 4, 5, 'VU-SR-2026', N'Vụ Sầu Riêng Xuất Khẩu 2026', N'Ri6 Đầu Dòng Miền Tây', '2025-11-01', '2026-05-15', NULL, 25000.00, 'ACTIVE', N'Chăm sóc chuẩn VietGAP hướng tới xuất khẩu chính ngạch');
SET IDENTITY_INSERT dbo.CropSeason OFF;
GO

SET IDENTITY_INSERT dbo.FarmTask ON;
INSERT INTO dbo.FarmTask (
    FarmTaskID, FarmID, ProductionAreaID, PlotID, CropSeasonID, CreatedByMemberID, HandledByMemberID, Title, Description, Priority, Status, StartAt, DueAt, CompletedAt, CreatedAt
) VALUES 
(1, 1, 1, 1, 1, 2, 2, N'Kiểm tra pH và EC bồn dinh dưỡng hồi lưu', N'Đo đạc nồng độ ppm, EC và pH bể chứa dinh dưỡng bồn A và B', 'HIGH', 'DONE', '2026-01-12 08:00:00', '2026-01-12 11:00:00', '2026-01-12 10:30:00', '2026-01-12 07:45:00'),
(2, 1, 2, 3, 2, 2, 3, N'Tỉa nhánh phụ và cố định dây leo dưa lưới', N'Tỉa các nhánh phụ từ nách lá 1 đến 8, cố định ngọn dây bằng kẹp chuyên dụng', 'MEDIUM', 'IN_PROGRESS', '2026-02-10 07:00:00', '2026-02-12 17:00:00', NULL, '2026-02-09 16:30:00');
SET IDENTITY_INSERT dbo.FarmTask OFF;
GO

INSERT INTO dbo.FarmTaskWorker (FarmTaskID, FarmWorkerID, AssignedAt, AssignmentStatus) VALUES 
(2, 1, '2026-02-10 07:15:00', 'ACCEPTED'),
(2, 2, '2026-02-10 07:15:00', 'ACCEPTED');
GO

SET IDENTITY_INSERT dbo.FarmingActivity ON;
INSERT INTO dbo.FarmingActivity (
    FarmingActivityID, CropSeasonID, FarmTaskID, SupervisedByMemberID, ActivityType, ActivityName, StartedAt, EndedAt, Description, ResultNotes, WeatherNotes, CreatedAt
) VALUES 
(1, 1, 1, 2, 'FERTILIZING', N'Châm dinh dưỡng bổ sung cho bể thủy canh xà lách', '2026-01-12 08:30:00', '2026-01-12 10:00:00', N'Pha thêm dung dịch Hydro Umat V theo tỷ lệ 1:200 để ổn định EC', N'Đã cân bằng đạt EC = 1.6 mS/cm, pH = 6.0', N'Trời nắng nhẹ, nhiệt độ 22 độ C', '2026-01-12 10:15:00'),
(2, 2, NULL, 3, 'HARVEST', N'Thu hoạch đợt 1 dưa lưới Lô G1', '2026-03-15 06:00:00', '2026-03-15 11:30:00', N'Thu hoạch trái dưa lưới đạt độ chín và cuống nứt tròn đều', N'Tổng sản lượng thu hoạch đạt 3,200 kg, chất lượng xuất sắc, độ ngọt brix trung bình 14.2', N'Trời mát mẻ, 18 độ C', '2026-03-15 12:00:00');
SET IDENTITY_INSERT dbo.FarmingActivity OFF;
GO

INSERT INTO dbo.ActivityWorker (FarmingActivityID, FarmWorkerID, WorkHours, Notes) VALUES 
(2, 1, 5.50, N'Cắt trái và phân loại sơ bộ tại luống'),
(2, 2, 5.50, N'Vận chuyển trái về khu sơ chế và lau sạch cuống');
GO

-- 10.14 Nhà cung cấp, Vật tư, Nhập kho và Nhật ký sử dụng vật tư
SET IDENTITY_INSERT dbo.Supplier ON;
INSERT INTO dbo.Supplier (
    SupplierID, FarmID, SupplierCode, SupplierName, ContactPerson, PhoneNumber, Email, AddressLine, TaxCode, IsActive
) VALUES 
(1, 1, 'SUP-VT-01', N'Công Ty Cổ Phần Giống Cây Trồng Đà Lạt', N'Lê Văn Thắng', '02633888999', 'sales@dalatseeds.vn', N'78 Đường Phan Chu Trinh, Phường 9, TP. Đà Lạt', '5801234567', 1),
(2, 1, 'SUP-VT-02', N'Công Ty Phân Bón Sinh Học Bio-Green', N'Trần Thị Mai', '02838999888', 'contact@biogreen.vn', N'102 Đường Võ Văn Ngân, TP. Thủ Đức, TP.HCM', '0309876543', 1);
SET IDENTITY_INSERT dbo.Supplier OFF;
GO

SET IDENTITY_INSERT dbo.Material ON;
INSERT INTO dbo.Material (
    MaterialID, FarmID, MaterialCode, MaterialName, MaterialType, Unit, ActiveIngredient, Manufacturer, IsActive
) VALUES 
(1, 1, 'MAT-SEED-01', N'Hạt Giống Xà Lách Lô Lô Xanh Rijk Zwaan', 'SEED', N'Gói', N'Hạt F1 tinh khiết 99%', N'Rijk Zwaan Hà Lan', 1),
(2, 1, 'MAT-FERT-01', N'Dinh Dưỡng Thủy Canh Chuyên Dụng Hydro Umat V', 'FERTILIZER', N'Bộ', N'Đa trung vi lượng chelate (N, P2O5, K2O, Ca, Mg, Fe)', N'Bio-Green Việt Nam', 1),
(3, 1, 'MAT-BIO-01', N'Chế Phẩm Sinh Học Phòng Trừ Sâu Neem Oil 80EC', 'BIOLOGICAL', N'Chai', N'Azadirachtin chiết xuất hạt Neem hữu cơ', N'Công Ty Thảo Mộc Xanh', 1);
SET IDENTITY_INSERT dbo.Material OFF;
GO

SET IDENTITY_INSERT dbo.InputPurchase ON;
INSERT INTO dbo.InputPurchase (
    InputPurchaseID, FarmID, SupplierID, RecordedByMemberID, InvoiceNumber, PurchaseDate, TotalAmount, DocumentUrl, Notes
) VALUES 
(1, 1, 2, 2, 'HD-BIO-2026-001', '2026-01-08', 15000000.00, N'https://storage.cropdiary.com/invoices/inv-2026-001.pdf', N'Nhập vật tư dinh dưỡng thủy canh và chế phẩm phòng trừ sinh học cho vụ đầu năm 2026');
SET IDENTITY_INSERT dbo.InputPurchase OFF;
GO

SET IDENTITY_INSERT dbo.InputPurchaseDetail ON;
INSERT INTO dbo.InputPurchaseDetail (
    InputPurchaseDetailID, InputPurchaseID, MaterialID, BatchNumber, ManufactureDate, ExpiryDate, Quantity, UnitPrice
) VALUES 
(1, 1, 2, 'LOT-202601-DDT', '2025-12-15', '2027-12-15', 50.000, 200000.00),
(2, 1, 3, 'LOT-202601-NEEM', '2025-12-20', '2027-12-20', 20.000, 250000.00);
SET IDENTITY_INSERT dbo.InputPurchaseDetail OFF;
GO

SET IDENTITY_INSERT dbo.MaterialUsage ON;
INSERT INTO dbo.MaterialUsage (
    MaterialUsageID, FarmingActivityID, InputPurchaseDetailID, MaterialID, RecordedByMemberID, UsedAt, Quantity, Unit, Dosage, Method, SafetyIntervalDays, Notes
) VALUES 
(1, 1, 1, 2, 2, '2026-01-12 09:00:00', 5.000, N'Bộ', N'1 bộ / 1000 lít nước', N'Châm trực tiếp vào bồn pha dinh dưỡng tuần hoàn', 0, N'Dinh dưỡng gốc khoáng thủy canh an toàn');
SET IDENTITY_INSERT dbo.MaterialUsage OFF;
GO

-- 10.15 Chi phí (Expense), Thu hoạch (HarvestRecord) và Bán hàng (ProductSale)
SET IDENTITY_INSERT dbo.Expense ON;
INSERT INTO dbo.Expense (
    ExpenseID, FarmID, CropSeasonID, FarmingActivityID, InputPurchaseID, RecordedByMemberID, ExpenseType, ExpenseDate, Amount, Description, EvidenceUrl
) VALUES 
(1, 1, 1, 1, 1, 2, 'INPUT', '2026-01-08', 15000000.00, N'Chi phí mua vật tư dinh dưỡng thủy canh và chế phẩm sinh học', N'https://storage.cropdiary.com/receipts/exp-001.pdf'),
(2, 1, 2, 2, NULL, 3, 'LABOR', '2026-03-15', 1100000.00, N'Tiền công nhật nhân công phụ trách thu hoạch dưa lưới lô G1', N'https://storage.cropdiary.com/receipts/exp-002.pdf');
SET IDENTITY_INSERT dbo.Expense OFF;
GO

SET IDENTITY_INSERT dbo.HarvestRecord ON;
INSERT INTO dbo.HarvestRecord (
    HarvestRecordID, CropSeasonID, RecordedByMemberID, HarvestLotCode, HarvestedAt, Quantity, Unit, QualityGrade, StorageLocation, TraceabilityCode, Notes
) VALUES 
(1, 2, 3, 'LOT-DL-20260315-01', '2026-03-15 11:00:00', 3200.000, N'kg', N'Loại 1 - Xuất khẩu', N'Kho Lạnh K1 Bảo Quản Nông Sản Đà Lạt', 'TRC-DL26-0001', N'Trái đều đẹp, lưới nổi rõ, trọng lượng 1.5 - 1.8 kg/trái, độ ngọt 14.2 độ brix');
SET IDENTITY_INSERT dbo.HarvestRecord OFF;
GO

INSERT INTO dbo.HarvestActivitySource (HarvestRecordID, FarmingActivityID) VALUES 
(1, 2);
GO

SET IDENTITY_INSERT dbo.ProductSale ON;
INSERT INTO dbo.ProductSale (
    ProductSaleID, HarvestRecordID, RecordedByMemberID, SaleDate, BuyerName, Quantity, UnitPrice, InvoiceNumber, Notes
) VALUES 
(1, 1, 1, '2026-03-16', N'Hệ Thống Chuỗi Siêu Thị WinCommerce / WinMart', 3000.000, 45000.00, 'INV-WIN-2026-001', N'Giao hàng tại Tổng kho WinMart Miền Đông, Dĩ An, Bình Dương');
SET IDENTITY_INSERT dbo.ProductSale OFF;
GO

-- 10.16 Tiêu chuẩn Checklist VietGAP, Đánh giá nội bộ & Tập huấn
SET IDENTITY_INSERT dbo.ChecklistRule ON;
INSERT INTO dbo.ChecklistRule (
    ChecklistRuleID, CreatedByUserID, RuleCode, RuleName, RuleCategory, Description, EvaluationType, RuleExpression, ReferenceDocument, EffectiveFrom, EffectiveTo, IsActive
) VALUES 
(1, 1, 'VIETGAP-WATER-01', N'Kiểm tra định kỳ chất lượng nguồn nước tưới', 'VIETGAP', N'Nguồn nước tưới phải được kiểm nghiệm chỉ tiêu vi sinh và kim loại nặng đạt chuẩn theo QCVN 39:2011/BTNMT', 'BOOLEAN', 'WaterTest.Pass == true', N'TCVN 11892-1:2017 Quy trình thực hành sản xuất nông nghiệp tốt (VietGAP)', '2025-01-01', NULL, 1),
(2, 1, 'VIETGAP-PEST-02', N'Tuân thủ thời gian cách ly thuốc BVTV (PHI)', 'VIETGAP', N'Phải ghi chép nhật ký sử dụng thuốc và đảm bảo tuyệt đối đủ số ngày cách ly trước khi thu hoạch', 'BOOLEAN', 'PestControl.DaysSinceLastSpray >= PHI', N'Quy định an toàn thực phẩm Bộ Nông nghiệp & PTNT', '2025-01-01', NULL, 1);
SET IDENTITY_INSERT dbo.ChecklistRule OFF;
GO

SET IDENTITY_INSERT dbo.ChecklistRun ON;
INSERT INTO dbo.ChecklistRun (
    ChecklistRunID, FarmID, CropSeasonID, CreatedByMemberID, RunName, StartedAt, CompletedAt, OverallStatus, ScorePercent, Notes
) VALUES 
(1, 1, 2, 2, N'Đánh giá tuân thủ VietGAP tiền thu hoạch Vụ Dưa Lưới Q1', '2026-03-10 08:00:00', '2026-03-10 11:30:00', 'PASS', 95.00, N'Tất cả chỉ tiêu an toàn thực phẩm và vệ sinh thu hoạch đều đáp ứng yêu cầu.');
SET IDENTITY_INSERT dbo.ChecklistRun OFF;
GO

SET IDENTITY_INSERT dbo.ChecklistResult ON;
INSERT INTO dbo.ChecklistResult (
    ChecklistResultID, ChecklistRunID, ChecklistRuleID, ResultStatus, ActualValue, EvidenceUrl, Explanation, EvaluatedAt
) VALUES 
(1, 1, 1, 'PASS', N'Đạt chuẩn QCVN 39:2011', N'https://storage.cropdiary.com/reports/water-test-2026.pdf', N'Có phiếu kiểm nghiệm nước định kỳ còn hiệu lực', '2026-03-10 09:00:00'),
(2, 1, 2, 'PASS', N'Đã cách ly 16 ngày', N'https://storage.cropdiary.com/logs/pest-log-2026.pdf', N'Ngưng phun chế phẩm Neem Oil từ ngày 28/02, đảm bảo cách ly an toàn', '2026-03-10 09:30:00');
SET IDENTITY_INSERT dbo.ChecklistResult OFF;
GO

SET IDENTITY_INSERT dbo.AssessmentCriterion ON;
INSERT INTO dbo.AssessmentCriterion (
    AssessmentCriterionID, CriterionCode, CriterionName, Category, Description, MaxScore, IsCritical, IsActive
) VALUES 
(1, 'CRIT-HYGIENE-01', N'Vệ sinh thùng chứa và dụng cụ thu hoạch', 'HYGIENE', N'Thùng chứa nông sản, kéo cắt và xe đẩy phải được khử trùng sạch sẽ trước khi đưa vào lô thu hoạch', 10.00, 1, 1);
SET IDENTITY_INSERT dbo.AssessmentCriterion OFF;
GO

SET IDENTITY_INSERT dbo.InternalAssessment ON;
INSERT INTO dbo.InternalAssessment (
    InternalAssessmentID, FarmID, ProductionAreaID, ConductedByMemberID, AssessmentDate, AssessmentName, Status, TotalScore, Conclusion, CorrectiveAction
) VALUES 
(1, 1, 2, 2, '2026-03-12', N'Đánh giá nội bộ tiền thu hoạch Lô G1', 'COMPLETED', 9.50, N'Đủ điều kiện xuất bán vào hệ thống siêu thị cao cấp', NULL);
SET IDENTITY_INSERT dbo.InternalAssessment OFF;
GO

SET IDENTITY_INSERT dbo.InternalAssessmentItem ON;
INSERT INTO dbo.InternalAssessmentItem (
    InternalAssessmentItemID, InternalAssessmentID, AssessmentCriterionID, ResultStatus, Score, Finding, EvidenceUrl
) VALUES 
(1, 1, 1, 'COMPLIANT', 9.50, N'Dụng cụ và giỏ thu hoạch đã được rửa sạch bằng dung dịch khử trùng hữu cơ và phơi khô ráo.', N'https://storage.cropdiary.com/images/assess-hygiene.jpg');
SET IDENTITY_INSERT dbo.InternalAssessmentItem OFF;
GO

SET IDENTITY_INSERT dbo.TrainingRecord ON;
INSERT INTO dbo.TrainingRecord (
    TrainingRecordID, FarmID, OrganizedByMemberID, TrainingTitle, TrainingTopic, TrainerName, StartedAt, EndedAt, Location, DocumentUrl, Notes
) VALUES 
(1, 1, 2, N'Tập huấn an toàn lao động và kỹ thuật thu hoạch VietGAP', N'Quy trình vệ sinh, mang bảo hộ và bảo quản nông sản sau thu hoạch', N'Kỹ sư Nguyễn Kỹ Thuật Viên', '2026-03-01 08:00:00', '2026-03-01 11:30:00', N'Hội trường nhà điều hành Trang Trại Đà Lạt', N'https://storage.cropdiary.com/training/vietgap-slides.pdf', N'Toàn bộ công nhân tham gia đầy đủ và đạt bài sát hạch');
SET IDENTITY_INSERT dbo.TrainingRecord OFF;
GO

INSERT INTO dbo.TrainingAttendance (TrainingRecordID, FarmWorkerID, AttendanceStatus, Result) VALUES 
(1, 1, 'COMPLETED', N'Đạt 95/100 bài kiểm tra thực hành'),
(1, 2, 'COMPLETED', N'Đạt 100/100 bài kiểm tra thực hành');
GO

SET IDENTITY_INSERT dbo.ProductionReport ON;
INSERT INTO dbo.ProductionReport (
    ProductionReportID, FarmID, CropSeasonID, GeneratedByMemberID, ReportType, PeriodFrom, PeriodTo, GeneratedAt, ReportDataJson, FileUrl
) VALUES 
(1, 1, 2, 2, 'PRODUCTION', '2026-01-05', '2026-03-20', '2026-03-20 17:00:00', '{"totalHarvestKg":3200,"yieldPerM2":0.8,"qualityGradeA_Percent":93.75,"revenueVND":135000000}', N'https://storage.cropdiary.com/reports/production-rep-dl-q1.pdf');
SET IDENTITY_INSERT dbo.ProductionReport OFF;
GO

-- 10.17 Bài viết kiến thức nông nghiệp (KnowledgeArticle)
SET IDENTITY_INSERT dbo.KnowledgeArticle ON;
INSERT INTO dbo.KnowledgeArticle (
    KnowledgeArticleID, CreatedByUserID, Title, Slug, Summary, Content, Category, SourceUrl, Status, PublishedAt, CreatedAt, UpdatedAt
) VALUES 
(1, 1, N'Hướng Dẫn Kỹ Thuật Trồng Rau Xà Lách Thủy Canh Tiêu Chuẩn VietGAP', 'huong-dan-ky-thuat-trong-rau-xa-lach-thuy-canh-vietgap', N'Tài liệu chi tiết hướng dẫn quản lý nồng độ dinh dưỡng EC, pH và biện pháp kiểm soát dịch hại sinh học trên xà lách thủy canh.', N'# 1. Chuẩn bị dung dịch thủy canh\nCần kiểm tra độ dẫn điện EC ở mức 1.4 - 1.8 mS/cm và pH từ 5.8 đến 6.5 để rễ cây hấp thu dinh dưỡng tối ưu.\n\n# 2. Quản lý sâu bệnh bằng biện pháp sinh học\nSử dụng bẫy dính màu vàng để bẫy bọ trĩ và rầy mềm. Định kỳ phun chế phẩm Neem Oil nồng độ 0.3% khi phát hiện sâu non.\n\n# 3. Quy trình thu hoạch\nThu hoạch vào sáng sớm trước 9 giờ để tránh cây bị mất nước và giòn lá.', N'Rau ăn lá', N'https://khuyennongdalat.gov.vn/ky-thuat-thuy-canh', 'PUBLISHED', '2026-01-15 09:00:00', '2026-01-15 08:30:00', '2026-01-15 09:00:00'),
(2, 1, N'Biện Pháp Phòng Trừ Bệnh Nứt Thân Chảy Mủ Trên Cây Dưa Lưới Nhà Màng', 'bien-phap-phong-tru-benh-nut-than-chay-mu-dua-luoi', N'Nhận diện triệu chứng bệnh do nấm Didymella bryoniae gây ra và các bước xử lý hữu cơ kịp thời.', N'# 1. Triệu chứng nhận biết\nThân cây xuất hiện các đốm nâu ủng nước, sau đó tiết ra giọt gôm màu nâu đỏ. Vết bệnh khô lại làm nứt thân và héo rũ cây.\n\n# 2. Biện pháp xử lý\nGiảm độ ẩm không khí trong nhà màng bằng quạt thông gió đối lưu. Cắt tỉa cành thông thoáng và quét dung dịch vôi pha đồng sunfat quanh gốc.', N'Cây ăn quả', N'https://nongnghiepthongminh.vn/dua-luoi-nut-than', 'PUBLISHED', '2026-01-20 14:00:00', '2026-01-20 13:30:00', '2026-01-20 14:00:00'),
(3, 1, N'Dự Thảo: Tiêu Chuẩn GlobalGAP Cho Sầu Riêng Xuất Khẩu Sang Thị Trường Châu Âu', 'du-thao-tieu-chuan-globalgap-sau-rieng-xuat-khau', N'Bản thảo quy chuẩn quản lý mã số vùng trồng, kiểm soát dư lượng kim loại nặng và nhật ký điện tử.', N'Nội dung dự thảo đang được thẩm định và tổng hợp ý kiến từ các chuyên gia Viện Cây ăn quả Miền Nam...', N'Tiêu chuẩn xuất khẩu', NULL, 'DRAFT', NULL, '2026-02-10 10:00:00', '2026-02-10 10:00:00');
SET IDENTITY_INSERT dbo.KnowledgeArticle OFF;
GO

-- 10.18 Trợ lý nông nghiệp AI (AIConversation, AIMessage, AIFeedback, AIFeedbackReply)
SET IDENTITY_INSERT dbo.AIConversation ON;
INSERT INTO dbo.AIConversation (
    AIConversationID, UserID, FarmID, PlotID, CropSeasonID, Title, Purpose, Status, CreatedAt, UpdatedAt
) VALUES 
(1, 2, 1, 1, 1, N'Tư vấn khắc phục bọ trĩ trên xà lách thủy canh', 'KNOWLEDGE_QA', 'ACTIVE', '2026-02-05 08:00:00', '2026-02-05 08:05:00');
SET IDENTITY_INSERT dbo.AIConversation OFF;
GO

SET IDENTITY_INSERT dbo.AIMessage ON;
INSERT INTO dbo.AIMessage (
    AIMessageID, AIConversationID, SenderRole, MessageContent, ModelName, PromptTokens, CompletionTokens, IsConfirmedByUser, CreatedAt
) VALUES 
(1, 1, 'USER', N'Vườn xà lách thủy canh Lô H1 của tôi xuất hiện bọ trĩ nhỏ li ti ở mặt dưới lá. Có cách nào xử lý sinh học không dùng hóa chất không?', NULL, NULL, NULL, 0, '2026-02-05 08:00:00'),
(2, 1, 'ASSISTANT', N'Chào bạn, đối với rau xà lách thủy canh sắp thu hoạch, bạn tuyệt đối không nên dùng thuốc hóa học có thời gian cách ly dài. Bạn có thể áp dụng ngay các biện pháp sinh học sau:\n1. Treo bẫy dính màu vàng mật độ 10m²/tấm ở độ cao ngang ngọn rau.\n2. Phun tinh dầu Neem (Neem Oil 80EC) nồng độ 0.3% kết hợp chất bám dính hữu cơ vào lúc chiều mát.\n3. Rửa trôi bọ trĩ bằng vòi phun sương áp lực nhẹ vào sáng sớm.', 'gemini-1.5-pro', 85, 120, 1, '2026-02-05 08:01:00');
SET IDENTITY_INSERT dbo.AIMessage OFF;
GO

SET IDENTITY_INSERT dbo.AIFeedback ON;
INSERT INTO dbo.AIFeedback (
    AIFeedbackID, AIMessageID, SubmittedByUserID, Rating, FeedbackType, Comment, Status, SubmittedAt
) VALUES 
(1, 2, 2, 5, 'HELPFUL', N'Câu trả lời rất chính xác và áp dụng hiệu quả, sau 3 ngày bọ trĩ giảm rõ rệt mà không ảnh hưởng chất lượng xà lách.', 'RESOLVED', '2026-02-08 09:30:00');
SET IDENTITY_INSERT dbo.AIFeedback OFF;
GO

SET IDENTITY_INSERT dbo.AIFeedbackReply ON;
INSERT INTO dbo.AIFeedbackReply (
    AIFeedbackReplyID, AIFeedbackID, WrittenByUserID, ReplyContent, CreatedAt
) VALUES 
(1, 1, 1, N'Cảm ơn anh Trần Văn Chủ đã phản hồi tích cực! Hệ thống AI MyCropDiary luôn cập nhật phác đồ canh tác hữu cơ mới nhất.', '2026-02-08 11:00:00');
SET IDENTITY_INSERT dbo.AIFeedbackReply OFF;
GO

PRINT N'Sample data inserted successfully for all existing modules and APIs.';
GO

