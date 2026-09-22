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
