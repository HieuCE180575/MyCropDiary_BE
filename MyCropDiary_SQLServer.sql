-- MyCropDiary database schema derived from the conceptual ERDs
-- Target: Microsoft SQL Server 2019+
-- Notes:
-- 1) Table names follow the ERD entity labels; join tables were added for many-to-many relations.
-- 2) Primary keys use INT IDENTITY(1,1) and integer foreign keys (1, 2, 3, 4...).
-- 3) Image data is stored outside the database; *_url columns store the resulting URL.
-- 4) Includes system_role on [User] to support system admin operations (e.g. farm registration review).

USE master;
GO

-- ============================================================================
-- DROP DATABASE IF EXISTS & RECREATE
-- ============================================================================
IF DB_ID(N'MyCropDiary') IS NOT NULL
BEGIN
    ALTER DATABASE [MyCropDiary] SET SINGLE_USER WITH ROLLBACK IMMEDIATE;
    DROP DATABASE [MyCropDiary];
END;
GO

CREATE DATABASE [MyCropDiary];
GO

USE [MyCropDiary];
GO

-- ============================================================================
-- 1. ACCOUNTS AND FARM MEMBERSHIP
-- ============================================================================
CREATE TABLE [User] (
    user_id INT IDENTITY(1,1) PRIMARY KEY,
    full_name NVARCHAR(150) NOT NULL,
    email NVARCHAR(255) NOT NULL UNIQUE,
    phone NVARCHAR(30),
    password_hash NVARCHAR(MAX) NOT NULL,
    avatar_url NVARCHAR(MAX),
    system_role NVARCHAR(20) NOT NULL DEFAULT 'user'
        CHECK (system_role IN ('user','admin')),
    account_status NVARCHAR(20) NOT NULL DEFAULT 'active'
        CHECK (account_status IN ('active','pending','locked','disabled')),
    email_verified_at DATETIME2(0),
    last_login_at DATETIME2(0),
    created_at DATETIME2(0) NOT NULL DEFAULT SYSUTCDATETIME(),
    updated_at DATETIME2(0) NOT NULL DEFAULT SYSUTCDATETIME()
);

CREATE TABLE [AccountToken] (
    token_id INT IDENTITY(1,1) PRIMARY KEY,
    user_id INT NOT NULL REFERENCES [User](user_id) ON DELETE CASCADE,
    token_hash NVARCHAR(255) NOT NULL UNIQUE,
    token_type NVARCHAR(30) NOT NULL
        CHECK (token_type IN ('email_verification','password_reset','refresh','invite')),
    expires_at DATETIME2(0) NOT NULL,
    used_at DATETIME2(0),
    created_at DATETIME2(0) NOT NULL DEFAULT SYSUTCDATETIME()
);

CREATE TABLE [Farm] (
    farm_id INT IDENTITY(1,1) PRIMARY KEY,
    farm_name NVARCHAR(160) NOT NULL,
    description NVARCHAR(MAX),
    address NVARCHAR(MAX),
    latitude NUMERIC(9,6),
    longitude NUMERIC(9,6),
    boundary_coordinates NVARCHAR(MAX),
    cover_image_url NVARCHAR(MAX),
    created_by INT REFERENCES [User](user_id) ON DELETE NO ACTION,
    farm_status NVARCHAR(20) NOT NULL DEFAULT 'active'
        CHECK (farm_status IN ('active','inactive','archived')),
    created_at DATETIME2(0) NOT NULL DEFAULT SYSUTCDATETIME(),
    updated_at DATETIME2(0) NOT NULL DEFAULT SYSUTCDATETIME()
);

CREATE TABLE [FarmRegistration] (
    registration_id INT IDENTITY(1,1) PRIMARY KEY,
    user_id INT NOT NULL REFERENCES [User](user_id) ON DELETE NO ACTION,
    farm_name NVARCHAR(160) NOT NULL,
    address NVARCHAR(MAX),
    province NVARCHAR(100),
    district NVARCHAR(100),
    ward NVARCHAR(100),
    latitude NUMERIC(9,6),
    longitude NUMERIC(9,6),
    boundary_coordinates NVARCHAR(MAX),
    document_url NVARCHAR(1000),
    registration_note NVARCHAR(MAX),
    rejection_reason NVARCHAR(1000),
    registration_status NVARCHAR(20) NOT NULL DEFAULT 'pending'
        CHECK (registration_status IN ('pending','approved','rejected','cancelled')),
    reviewed_by INT REFERENCES [User](user_id) ON DELETE NO ACTION,
    reviewed_at DATETIME2(0),
    resulting_farm_id INT REFERENCES [Farm](farm_id) ON DELETE NO ACTION,
    created_at DATETIME2(0) NOT NULL DEFAULT SYSUTCDATETIME()
);

CREATE TABLE [FarmMember] (
    farm_member_id INT IDENTITY(1,1) PRIMARY KEY,
    farm_id INT NOT NULL REFERENCES [Farm](farm_id) ON DELETE NO ACTION,
    user_id INT NOT NULL REFERENCES [User](user_id) ON DELETE NO ACTION,
    member_role NVARCHAR(30) NOT NULL DEFAULT 'member'
        CHECK (member_role IN ('owner','admin','manager','member')),
    membership_status NVARCHAR(20) NOT NULL DEFAULT 'active'
        CHECK (membership_status IN ('invited','active','suspended','left')),
    joined_at DATETIME2(0) NOT NULL DEFAULT SYSUTCDATETIME(),
    UNIQUE (farm_id, user_id)
);

CREATE TABLE [FarmWorker] (
    farm_worker_id INT IDENTITY(1,1) PRIMARY KEY,
    farm_member_id INT NOT NULL UNIQUE REFERENCES [FarmMember](farm_member_id) ON DELETE NO ACTION,
    worker_code NVARCHAR(40),
    job_title NVARCHAR(100),
    employment_status NVARCHAR(20) NOT NULL DEFAULT 'active'
        CHECK (employment_status IN ('active','inactive','seasonal')),
    hired_at DATE,
    notes NVARCHAR(MAX)
);

-- ============================================================================
-- 2. ZONES, BLOCKS, CROPS, SEASONS, TASKS AND ACTIVITIES
-- ============================================================================
CREATE TABLE [CropCategory] (
    crop_category_id INT IDENTITY(1,1) PRIMARY KEY,
    category_name NVARCHAR(100) NOT NULL UNIQUE,
    description NVARCHAR(MAX),
    image_url NVARCHAR(MAX),
    created_at DATETIME2(0) NOT NULL DEFAULT SYSUTCDATETIME()
);

CREATE TABLE [CropSeason] (
    crop_season_id INT IDENTITY(1,1) PRIMARY KEY,
    farm_id INT NOT NULL REFERENCES [Farm](farm_id) ON DELETE NO ACTION,
    crop_category_id INT NOT NULL REFERENCES [CropCategory](crop_category_id) ON DELETE NO ACTION,
    season_name NVARCHAR(120) NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE,
    season_status NVARCHAR(20) NOT NULL DEFAULT 'planned'
        CHECK (season_status IN ('planned','active','completed','cancelled')),
    notes NVARCHAR(MAX),
    created_by INT REFERENCES [User](user_id) ON DELETE NO ACTION,
    created_at DATETIME2(0) NOT NULL DEFAULT SYSUTCDATETIME(),
    CHECK (end_date IS NULL OR end_date >= start_date)
);

CREATE TABLE [Zone] (
    zone_id INT IDENTITY(1,1) PRIMARY KEY,
    farm_id INT NOT NULL REFERENCES [Farm](farm_id) ON DELETE NO ACTION,
    zone_name NVARCHAR(120) NOT NULL,
    description NVARCHAR(MAX),
    image_url NVARCHAR(MAX),
    area_value NUMERIC(12,2),
    area_unit NVARCHAR(20),
    created_at DATETIME2(0) NOT NULL DEFAULT SYSUTCDATETIME(),
    UNIQUE (farm_id, zone_name)
);

CREATE TABLE [Block] (
    block_id INT IDENTITY(1,1) PRIMARY KEY,
    zone_id INT NOT NULL REFERENCES [Zone](zone_id) ON DELETE NO ACTION,
    block_name NVARCHAR(120) NOT NULL,
    description NVARCHAR(MAX),
    image_url NVARCHAR(MAX),
    area_value NUMERIC(12,2),
    area_unit NVARCHAR(20),
    soil_type NVARCHAR(100),
    created_at DATETIME2(0) NOT NULL DEFAULT SYSUTCDATETIME(),
    UNIQUE (zone_id, block_name)
);

CREATE TABLE [CropSeasonBlock] (
    crop_season_id INT NOT NULL REFERENCES [CropSeason](crop_season_id) ON DELETE NO ACTION,
    block_id INT NOT NULL REFERENCES [Block](block_id) ON DELETE NO ACTION,
    planted_area NUMERIC(12,2),
    area_unit NVARCHAR(20),
    planted_at DATE,
    expected_harvest_at DATE,
    PRIMARY KEY (crop_season_id, block_id)
);

CREATE TABLE [StaffZoneAssignment] (
    assignment_id INT IDENTITY(1,1) PRIMARY KEY,
    farm_member_id INT NOT NULL REFERENCES [FarmMember](farm_member_id) ON DELETE NO ACTION,
    zone_id INT NOT NULL REFERENCES [Zone](zone_id) ON DELETE NO ACTION,
    assigned_at DATETIME2(0) NOT NULL DEFAULT SYSUTCDATETIME(),
    ended_at DATETIME2(0),
    UNIQUE (farm_member_id, zone_id, assigned_at),
    CHECK (ended_at IS NULL OR ended_at >= assigned_at)
);

CREATE TABLE [FarmTask] (
    farm_task_id INT IDENTITY(1,1) PRIMARY KEY,
    farm_id INT NOT NULL REFERENCES [Farm](farm_id) ON DELETE NO ACTION,
    crop_season_id INT REFERENCES [CropSeason](crop_season_id) ON DELETE NO ACTION,
    zone_id INT REFERENCES [Zone](zone_id) ON DELETE NO ACTION,
    block_id INT REFERENCES [Block](block_id) ON DELETE NO ACTION,
    task_name NVARCHAR(160) NOT NULL,
    description NVARCHAR(MAX),
    task_type NVARCHAR(60),
    priority NVARCHAR(20) NOT NULL DEFAULT 'normal'
        CHECK (priority IN ('low','normal','high','urgent')),
    status NVARCHAR(20) NOT NULL DEFAULT 'planned'
        CHECK (status IN ('planned','in_progress','completed','cancelled')),
    starts_at DATETIME2(0),
    due_at DATETIME2(0),
    created_by INT REFERENCES [User](user_id) ON DELETE NO ACTION,
    created_at DATETIME2(0) NOT NULL DEFAULT SYSUTCDATETIME(),
    CHECK (due_at IS NULL OR starts_at IS NULL OR due_at >= starts_at)
);

CREATE TABLE [FarmingActivity] (
    farming_activity_id INT IDENTITY(1,1) PRIMARY KEY,
    farm_id INT NOT NULL REFERENCES [Farm](farm_id) ON DELETE NO ACTION,
    farm_task_id INT REFERENCES [FarmTask](farm_task_id) ON DELETE NO ACTION,
    crop_season_id INT REFERENCES [CropSeason](crop_season_id) ON DELETE NO ACTION,
    zone_id INT REFERENCES [Zone](zone_id) ON DELETE NO ACTION,
    block_id INT REFERENCES [Block](block_id) ON DELETE NO ACTION,
    activity_name NVARCHAR(160) NOT NULL,
    activity_type NVARCHAR(60),
    activity_notes NVARCHAR(MAX),
    performed_at DATETIME2(0) NOT NULL DEFAULT SYSUTCDATETIME(),
    image_url NVARCHAR(MAX),
    created_by INT REFERENCES [User](user_id) ON DELETE NO ACTION,
    created_at DATETIME2(0) NOT NULL DEFAULT SYSUTCDATETIME()
);

CREATE TABLE [ActivityWorker] (
    farming_activity_id INT NOT NULL REFERENCES [FarmingActivity](farming_activity_id) ON DELETE NO ACTION,
    farm_worker_id INT NOT NULL REFERENCES [FarmWorker](farm_worker_id) ON DELETE NO ACTION,
    participation_role NVARCHAR(60),
    PRIMARY KEY (farming_activity_id, farm_worker_id)
);

CREATE TABLE [TaskAssignment] (
    farm_task_id INT NOT NULL REFERENCES [FarmTask](farm_task_id) ON DELETE NO ACTION,
    farm_worker_id INT NOT NULL REFERENCES [FarmWorker](farm_worker_id) ON DELETE NO ACTION,
    assigned_by INT REFERENCES [User](user_id) ON DELETE NO ACTION,
    assigned_at DATETIME2(0) NOT NULL DEFAULT SYSUTCDATETIME(),
    assignment_status NVARCHAR(20) NOT NULL DEFAULT 'assigned'
        CHECK (assignment_status IN ('assigned','accepted','completed','cancelled')),
    PRIMARY KEY (farm_task_id, farm_worker_id)
);

CREATE TABLE [ActivitySupervisor] (
    farming_activity_id INT NOT NULL REFERENCES [FarmingActivity](farming_activity_id) ON DELETE NO ACTION,
    farm_member_id INT NOT NULL REFERENCES [FarmMember](farm_member_id) ON DELETE NO ACTION,
    PRIMARY KEY (farming_activity_id, farm_member_id)
);

-- ============================================================================
-- 3. ENVIRONMENT AND FIELD OBSERVATIONS
-- ============================================================================
CREATE TABLE [ZoneRiskAssessment] (
    zone_risk_assessment_id INT IDENTITY(1,1) PRIMARY KEY,
    zone_id INT NOT NULL REFERENCES [Zone](zone_id) ON DELETE NO ACTION,
    assessed_by INT REFERENCES [FarmMember](farm_member_id) ON DELETE NO ACTION,
    risk_type NVARCHAR(80) NOT NULL,
    risk_level NVARCHAR(20) NOT NULL
        CHECK (risk_level IN ('low','medium','high','critical')),
    description NVARCHAR(MAX),
    mitigation_plan NVARCHAR(MAX),
    image_url NVARCHAR(MAX),
    assessed_at DATETIME2(0) NOT NULL DEFAULT SYSUTCDATETIME()
);

CREATE TABLE [WaterSource] (
    water_source_id INT IDENTITY(1,1) PRIMARY KEY,
    farm_id INT NOT NULL REFERENCES [Farm](farm_id) ON DELETE NO ACTION,
    source_name NVARCHAR(120) NOT NULL,
    source_type NVARCHAR(60),
    description NVARCHAR(MAX),
    image_url NVARCHAR(MAX),
    created_at DATETIME2(0) NOT NULL DEFAULT SYSUTCDATETIME()
);

CREATE TABLE [WaterAssessment] (
    water_assessment_id INT IDENTITY(1,1) PRIMARY KEY,
    water_source_id INT NOT NULL REFERENCES [WaterSource](water_source_id) ON DELETE NO ACTION,
    block_id INT REFERENCES [Block](block_id) ON DELETE NO ACTION,
    conducted_by INT REFERENCES [FarmMember](farm_member_id) ON DELETE NO ACTION,
    water_ph NUMERIC(5,2),
    temperature_c NUMERIC(5,2),
    quality_status NVARCHAR(30),
    notes NVARCHAR(MAX),
    image_url NVARCHAR(MAX),
    assessed_at DATETIME2(0) NOT NULL DEFAULT SYSUTCDATETIME()
);

CREATE TABLE [SoilAssessment] (
    soil_assessment_id INT IDENTITY(1,1) PRIMARY KEY,
    block_id INT NOT NULL REFERENCES [Block](block_id) ON DELETE NO ACTION,
    conducted_by INT REFERENCES [FarmMember](farm_member_id) ON DELETE NO ACTION,
    soil_ph NUMERIC(5,2),
    moisture_percent NUMERIC(5,2),
    nutrient_data NVARCHAR(MAX) NOT NULL DEFAULT N'{}',
    notes NVARCHAR(MAX),
    image_url NVARCHAR(MAX),
    assessed_at DATETIME2(0) NOT NULL DEFAULT SYSUTCDATETIME()
);

CREATE TABLE [BlockConditionObservation] (
    observation_id INT IDENTITY(1,1) PRIMARY KEY,
    block_id INT NOT NULL REFERENCES [Block](block_id) ON DELETE NO ACTION,
    recorded_by INT REFERENCES [FarmMember](farm_member_id) ON DELETE NO ACTION,
    condition_type NVARCHAR(80),
    description NVARCHAR(MAX) NOT NULL,
    image_url NVARCHAR(MAX),
    observed_at DATETIME2(0) NOT NULL DEFAULT SYSUTCDATETIME()
);

-- ============================================================================
-- 4. MATERIALS, PURCHASES, INVENTORY AND EXPENSES
-- ============================================================================
CREATE TABLE [Supplier] (
    supplier_id INT IDENTITY(1,1) PRIMARY KEY,
    farm_id INT NOT NULL REFERENCES [Farm](farm_id) ON DELETE NO ACTION,
    supplier_name NVARCHAR(160) NOT NULL,
    contact_name NVARCHAR(120),
    phone NVARCHAR(30),
    email NVARCHAR(255),
    address NVARCHAR(MAX),
    notes NVARCHAR(MAX),
    created_at DATETIME2(0) NOT NULL DEFAULT SYSUTCDATETIME()
);

CREATE TABLE [Material] (
    material_id INT IDENTITY(1,1) PRIMARY KEY,
    farm_id INT NOT NULL REFERENCES [Farm](farm_id) ON DELETE NO ACTION,
    material_name NVARCHAR(160) NOT NULL,
    material_type NVARCHAR(80),
    material_form NVARCHAR(60),
    unit NVARCHAR(30) NOT NULL,
    description NVARCHAR(MAX),
    image_url NVARCHAR(MAX),
    is_active BIT NOT NULL DEFAULT 1,
    created_at DATETIME2(0) NOT NULL DEFAULT SYSUTCDATETIME(),
    UNIQUE (farm_id, material_name)
);

CREATE TABLE [Warehouse] (
    warehouse_id INT IDENTITY(1,1) PRIMARY KEY,
    farm_id INT NOT NULL REFERENCES [Farm](farm_id) ON DELETE NO ACTION,
    warehouse_name NVARCHAR(120) NOT NULL,
    address NVARCHAR(MAX),
    image_url NVARCHAR(MAX),
    created_at DATETIME2(0) NOT NULL DEFAULT SYSUTCDATETIME(),
    UNIQUE (farm_id, warehouse_name)
);

CREATE TABLE [Inventory] (
    inventory_id INT IDENTITY(1,1) PRIMARY KEY,
    warehouse_id INT NOT NULL REFERENCES [Warehouse](warehouse_id) ON DELETE NO ACTION,
    material_id INT NOT NULL REFERENCES [Material](material_id) ON DELETE NO ACTION,
    quantity_on_hand NUMERIC(14,3) NOT NULL DEFAULT 0 CHECK (quantity_on_hand >= 0),
    reorder_level NUMERIC(14,3) NOT NULL DEFAULT 0 CHECK (reorder_level >= 0),
    updated_at DATETIME2(0) NOT NULL DEFAULT SYSUTCDATETIME(),
    UNIQUE (warehouse_id, material_id)
);

CREATE TABLE [InputPurchase] (
    input_purchase_id INT IDENTITY(1,1) PRIMARY KEY,
    farm_id INT NOT NULL REFERENCES [Farm](farm_id) ON DELETE NO ACTION,
    supplier_id INT REFERENCES [Supplier](supplier_id) ON DELETE NO ACTION,
    purchased_by INT REFERENCES [FarmMember](farm_member_id) ON DELETE NO ACTION,
    purchase_date DATE NOT NULL DEFAULT CONVERT(date, SYSUTCDATETIME()),
    reference_no NVARCHAR(80),
    total_amount NUMERIC(14,2) NOT NULL DEFAULT 0 CHECK (total_amount >= 0),
    receipt_image_url NVARCHAR(MAX),
    notes NVARCHAR(MAX),
    created_at DATETIME2(0) NOT NULL DEFAULT SYSUTCDATETIME()
);

CREATE TABLE [InputPurchaseItem] (
    purchase_item_id INT IDENTITY(1,1) PRIMARY KEY,
    input_purchase_id INT NOT NULL REFERENCES [InputPurchase](input_purchase_id) ON DELETE NO ACTION,
    material_id INT NOT NULL REFERENCES [Material](material_id) ON DELETE NO ACTION,
    warehouse_id INT REFERENCES [Warehouse](warehouse_id) ON DELETE NO ACTION,
    quantity NUMERIC(14,3) NOT NULL CHECK (quantity > 0),
    unit_cost NUMERIC(14,2) NOT NULL CHECK (unit_cost >= 0),
    line_total AS (CAST(quantity * unit_cost AS NUMERIC(14,2))) PERSISTED
);

CREATE TABLE [MaterialUsage] (
    material_usage_id INT IDENTITY(1,1) PRIMARY KEY,
    farm_id INT NOT NULL REFERENCES [Farm](farm_id) ON DELETE NO ACTION,
    material_id INT NOT NULL REFERENCES [Material](material_id) ON DELETE NO ACTION,
    farm_task_id INT REFERENCES [FarmTask](farm_task_id) ON DELETE NO ACTION,
    farming_activity_id INT REFERENCES [FarmingActivity](farming_activity_id) ON DELETE NO ACTION,
    block_id INT REFERENCES [Block](block_id) ON DELETE NO ACTION,
    used_by INT REFERENCES [FarmMember](farm_member_id) ON DELETE NO ACTION,
    quantity NUMERIC(14,3) NOT NULL CHECK (quantity > 0),
    dosage NVARCHAR(100),
    method NVARCHAR(200),
    material_form NVARCHAR(60),
    applied_area NUMERIC(12,2),
    applied_area_unit NVARCHAR(20),
    applied_plant_count INT,
    safety_interval_days INT,
    used_at DATETIME2(0) NOT NULL DEFAULT SYSUTCDATETIME(),
    notes NVARCHAR(MAX),
    image_url NVARCHAR(MAX)
);

CREATE TABLE [InventoryTransaction] (
    inventory_transaction_id INT IDENTITY(1,1) PRIMARY KEY,
    inventory_id INT NOT NULL REFERENCES [Inventory](inventory_id) ON DELETE NO ACTION,
    transaction_type NVARCHAR(20) NOT NULL
        CHECK (transaction_type IN ('purchase','usage','adjustment','transfer_in','transfer_out','return')),
    quantity_change NUMERIC(14,3) NOT NULL CHECK (quantity_change <> 0),
    reference_type NVARCHAR(40),
    reference_id INT,
    notes NVARCHAR(MAX),
    created_by INT REFERENCES [User](user_id) ON DELETE NO ACTION,
    created_at DATETIME2(0) NOT NULL DEFAULT SYSUTCDATETIME()
);

CREATE TABLE [Expense] (
    expense_id INT IDENTITY(1,1) PRIMARY KEY,
    farm_id INT NOT NULL REFERENCES [Farm](farm_id) ON DELETE NO ACTION,
    crop_season_id INT REFERENCES [CropSeason](crop_season_id) ON DELETE NO ACTION,
    farm_task_id INT REFERENCES [FarmTask](farm_task_id) ON DELETE NO ACTION,
    expense_category NVARCHAR(80) NOT NULL,
    description NVARCHAR(MAX),
    amount NUMERIC(14,2) NOT NULL CHECK (amount >= 0),
    expense_date DATE NOT NULL DEFAULT CONVERT(date, SYSUTCDATETIME()),
    recorded_by INT REFERENCES [FarmMember](farm_member_id) ON DELETE NO ACTION,
    receipt_image_url NVARCHAR(MAX),
    created_at DATETIME2(0) NOT NULL DEFAULT SYSUTCDATETIME()
);

-- ============================================================================
-- 5. HARVEST AND SALES
-- ============================================================================
CREATE TABLE [HarvestRecord] (
    harvest_record_id INT IDENTITY(1,1) PRIMARY KEY,
    farm_id INT NOT NULL REFERENCES [Farm](farm_id) ON DELETE NO ACTION,
    crop_season_id INT REFERENCES [CropSeason](crop_season_id) ON DELETE NO ACTION,
    block_id INT REFERENCES [Block](block_id) ON DELETE NO ACTION,
    farming_activity_id INT REFERENCES [FarmingActivity](farming_activity_id) ON DELETE NO ACTION,
    recorded_by INT REFERENCES [FarmMember](farm_member_id) ON DELETE NO ACTION,
    harvest_date DATE NOT NULL,
    quantity NUMERIC(14,3) NOT NULL CHECK (quantity > 0),
    unit NVARCHAR(30) NOT NULL,
    quality_grade NVARCHAR(50),
    image_url NVARCHAR(MAX),
    notes NVARCHAR(MAX),
    created_at DATETIME2(0) NOT NULL DEFAULT SYSUTCDATETIME()
);

CREATE TABLE [ProductSale] (
    product_sale_id INT IDENTITY(1,1) PRIMARY KEY,
    farm_id INT NOT NULL REFERENCES [Farm](farm_id) ON DELETE NO ACTION,
    harvest_record_id INT NOT NULL REFERENCES [HarvestRecord](harvest_record_id) ON DELETE NO ACTION,
    recorded_by INT REFERENCES [FarmMember](farm_member_id) ON DELETE NO ACTION,
    buyer_name NVARCHAR(160),
    sale_date DATE NOT NULL DEFAULT CONVERT(date, SYSUTCDATETIME()),
    quantity NUMERIC(14,3) NOT NULL CHECK (quantity > 0),
    unit NVARCHAR(30) NOT NULL,
    unit_price NUMERIC(14,2) NOT NULL CHECK (unit_price >= 0),
    total_amount AS (CAST(quantity * unit_price AS NUMERIC(14,2))) PERSISTED,
    product_image_url NVARCHAR(MAX),
    notes NVARCHAR(MAX),
    created_at DATETIME2(0) NOT NULL DEFAULT SYSUTCDATETIME()
);

-- ============================================================================
-- 6. TRAINING, CHECKLISTS, REPORTS AND INTERNAL ASSESSMENTS
-- ============================================================================
CREATE TABLE [ChecklistRule] (
    checklist_rule_id INT IDENTITY(1,1) PRIMARY KEY,
    farm_id INT NOT NULL REFERENCES [Farm](farm_id) ON DELETE NO ACTION,
    created_by INT REFERENCES [User](user_id) ON DELETE NO ACTION,
    rule_name NVARCHAR(160) NOT NULL,
    description NVARCHAR(MAX),
    category NVARCHAR(80),
    is_active BIT NOT NULL DEFAULT 1,
    created_at DATETIME2(0) NOT NULL DEFAULT SYSUTCDATETIME()
);

CREATE TABLE [ChecklistRun] (
    checklist_run_id INT IDENTITY(1,1) PRIMARY KEY,
    checklist_rule_id INT NOT NULL REFERENCES [ChecklistRule](checklist_rule_id) ON DELETE NO ACTION,
    crop_season_id INT REFERENCES [CropSeason](crop_season_id) ON DELETE NO ACTION,
    block_id INT REFERENCES [Block](block_id) ON DELETE NO ACTION,
    assigned_to INT REFERENCES [FarmMember](farm_member_id) ON DELETE NO ACTION,
    run_status NVARCHAR(20) NOT NULL DEFAULT 'open'
        CHECK (run_status IN ('open','in_progress','completed','cancelled')),
    scheduled_at DATETIME2(0),
    completed_at DATETIME2(0),
    created_at DATETIME2(0) NOT NULL DEFAULT SYSUTCDATETIME()
);

CREATE TABLE [ChecklistResult] (
    checklist_result_id INT IDENTITY(1,1) PRIMARY KEY,
    checklist_run_id INT NOT NULL REFERENCES [ChecklistRun](checklist_run_id) ON DELETE NO ACTION,
    checked_by INT REFERENCES [FarmMember](farm_member_id) ON DELETE NO ACTION,
    result_status NVARCHAR(20) NOT NULL
        CHECK (result_status IN ('pass','fail','not_applicable')),
    notes NVARCHAR(MAX),
    evidence_image_url NVARCHAR(MAX),
    checked_at DATETIME2(0) NOT NULL DEFAULT SYSUTCDATETIME()
);

CREATE TABLE [ProductionReport] (
    production_report_id INT IDENTITY(1,1) PRIMARY KEY,
    farm_id INT NOT NULL REFERENCES [Farm](farm_id) ON DELETE NO ACTION,
    crop_season_id INT NOT NULL REFERENCES [CropSeason](crop_season_id) ON DELETE NO ACTION,
    generated_by INT REFERENCES [FarmMember](farm_member_id) ON DELETE NO ACTION,
    report_title NVARCHAR(180) NOT NULL,
    report_data NVARCHAR(MAX) NOT NULL DEFAULT N'{}',
    report_image_url NVARCHAR(MAX),
    generated_at DATETIME2(0) NOT NULL DEFAULT SYSUTCDATETIME()
);

CREATE TABLE [TrainingRecord] (
    training_record_id INT IDENTITY(1,1) PRIMARY KEY,
    farm_id INT NOT NULL REFERENCES [Farm](farm_id) ON DELETE NO ACTION,
    organized_by INT REFERENCES [FarmMember](farm_member_id) ON DELETE NO ACTION,
    training_title NVARCHAR(180) NOT NULL,
    description NVARCHAR(MAX),
    training_date DATE NOT NULL,
    image_url NVARCHAR(MAX),
    created_at DATETIME2(0) NOT NULL DEFAULT SYSUTCDATETIME()
);

CREATE TABLE [TrainingAttendee] (
    training_record_id INT NOT NULL REFERENCES [TrainingRecord](training_record_id) ON DELETE NO ACTION,
    farm_worker_id INT NOT NULL REFERENCES [FarmWorker](farm_worker_id) ON DELETE NO ACTION,
    attendance_status NVARCHAR(20) NOT NULL DEFAULT 'attended'
        CHECK (attendance_status IN ('attended','absent','excused')),
    notes NVARCHAR(MAX),
    PRIMARY KEY (training_record_id, farm_worker_id)
);

CREATE TABLE [AssessmentCriterion] (
    assessment_criterion_id INT IDENTITY(1,1) PRIMARY KEY,
    farm_id INT NOT NULL REFERENCES [Farm](farm_id) ON DELETE NO ACTION,
    criterion_name NVARCHAR(160) NOT NULL,
    description NVARCHAR(MAX),
    max_score NUMERIC(8,2) NOT NULL DEFAULT 100 CHECK (max_score > 0),
    is_active BIT NOT NULL DEFAULT 1,
    UNIQUE (farm_id, criterion_name)
);

CREATE TABLE [InternalAssessment] (
    internal_assessment_id INT IDENTITY(1,1) PRIMARY KEY,
    farm_id INT NOT NULL REFERENCES [Farm](farm_id) ON DELETE NO ACTION,
    crop_season_id INT REFERENCES [CropSeason](crop_season_id) ON DELETE NO ACTION,
    zone_id INT REFERENCES [Zone](zone_id) ON DELETE NO ACTION,
    assessed_by INT REFERENCES [FarmMember](farm_member_id) ON DELETE NO ACTION,
    assessment_title NVARCHAR(180) NOT NULL,
    assessed_at DATETIME2(0) NOT NULL DEFAULT SYSUTCDATETIME(),
    notes NVARCHAR(MAX),
    evidence_image_url NVARCHAR(MAX)
);

CREATE TABLE [InternalAssessmentItem] (
    internal_assessment_item_id INT IDENTITY(1,1) PRIMARY KEY,
    internal_assessment_id INT NOT NULL REFERENCES [InternalAssessment](internal_assessment_id) ON DELETE NO ACTION,
    assessment_criterion_id INT NOT NULL REFERENCES [AssessmentCriterion](assessment_criterion_id) ON DELETE NO ACTION,
    score NUMERIC(8,2) NOT NULL CHECK (score >= 0),
    result_notes NVARCHAR(MAX),
    evidence_image_url NVARCHAR(MAX),
    UNIQUE (internal_assessment_id, assessment_criterion_id)
);

-- ============================================================================
-- 7. KNOWLEDGE BASE AND AI
-- ============================================================================
CREATE TABLE [KnowledgeArticle] (
    knowledge_article_id INT IDENTITY(1,1) PRIMARY KEY,
    created_by INT REFERENCES [User](user_id) ON DELETE NO ACTION,
    title NVARCHAR(220) NOT NULL,
    slug NVARCHAR(240) NOT NULL UNIQUE,
    summary NVARCHAR(MAX),
    body NVARCHAR(MAX) NOT NULL,
    cover_image_url NVARCHAR(MAX),
    category NVARCHAR(100),
    tags NVARCHAR(500),
    view_count INT NOT NULL DEFAULT 0,
    source_url NVARCHAR(500),
    article_status NVARCHAR(20) NOT NULL DEFAULT 'draft'
        CHECK (article_status IN ('draft','published','archived')),
    published_at DATETIME2(0),
    created_at DATETIME2(0) NOT NULL DEFAULT SYSUTCDATETIME(),
    updated_at DATETIME2(0) NOT NULL DEFAULT SYSUTCDATETIME()
);

CREATE TABLE [AIConversation] (
    ai_conversation_id INT IDENTITY(1,1) PRIMARY KEY,
    user_id INT NOT NULL REFERENCES [User](user_id) ON DELETE NO ACTION,
    farm_id INT REFERENCES [Farm](farm_id) ON DELETE NO ACTION,
    block_id INT REFERENCES [Block](block_id) ON DELETE NO ACTION,
    crop_season_id INT REFERENCES [CropSeason](crop_season_id) ON DELETE NO ACTION,
    conversation_title NVARCHAR(200),
    conversation_status NVARCHAR(20) NOT NULL DEFAULT 'open'
        CHECK (conversation_status IN ('open','closed','archived')),
    created_at DATETIME2(0) NOT NULL DEFAULT SYSUTCDATETIME(),
    updated_at DATETIME2(0) NOT NULL DEFAULT SYSUTCDATETIME()
);

CREATE TABLE [AIMessage] (
    ai_message_id INT IDENTITY(1,1) PRIMARY KEY,
    ai_conversation_id INT NOT NULL REFERENCES [AIConversation](ai_conversation_id) ON DELETE NO ACTION,
    sender_type NVARCHAR(20) NOT NULL CHECK (sender_type IN ('user','assistant','system')),
    message_text NVARCHAR(MAX) NOT NULL,
    attachment_urls NVARCHAR(MAX) NOT NULL DEFAULT N'[]',
    model_name NVARCHAR(100),
    created_at DATETIME2(0) NOT NULL DEFAULT SYSUTCDATETIME()
);

CREATE TABLE [AIFeedback] (
    ai_feedback_id INT IDENTITY(1,1) PRIMARY KEY,
    ai_message_id INT NOT NULL REFERENCES [AIMessage](ai_message_id) ON DELETE NO ACTION,
    user_id INT NOT NULL REFERENCES [User](user_id) ON DELETE NO ACTION,
    rating SMALLINT CHECK (rating BETWEEN 1 AND 5),
    feedback_text NVARCHAR(MAX),
    created_at DATETIME2(0) NOT NULL DEFAULT SYSUTCDATETIME(),
    UNIQUE (ai_message_id, user_id)
);

CREATE TABLE [AIFeedbackReply] (
    ai_feedback_reply_id INT IDENTITY(1,1) PRIMARY KEY,
    ai_feedback_id INT NOT NULL REFERENCES [AIFeedback](ai_feedback_id) ON DELETE NO ACTION,
    user_id INT REFERENCES [User](user_id) ON DELETE NO ACTION,
    reply_text NVARCHAR(MAX) NOT NULL,
    created_at DATETIME2(0) NOT NULL DEFAULT SYSUTCDATETIME()
);

-- ============================================================================
-- INDEXES
-- ============================================================================
CREATE INDEX idx_farm_members_user ON [FarmMember](user_id);
CREATE INDEX idx_crop_seasons_farm_dates ON [CropSeason](farm_id, start_date, end_date);
CREATE INDEX idx_zones_farm ON [Zone](farm_id);
CREATE INDEX idx_blocks_zone ON [Block](zone_id);
CREATE INDEX idx_tasks_farm_status_due ON [FarmTask](farm_id, status, due_at);
CREATE INDEX idx_activities_farm_performed ON [FarmingActivity](farm_id, performed_at DESC);
CREATE INDEX idx_harvest_farm_date ON [HarvestRecord](farm_id, harvest_date DESC);
CREATE INDEX idx_sales_farm_date ON [ProductSale](farm_id, sale_date DESC);
CREATE INDEX idx_expenses_farm_date ON [Expense](farm_id, expense_date DESC);
CREATE INDEX idx_inventory_transactions_inventory_date ON [InventoryTransaction](inventory_id, created_at DESC);
CREATE INDEX idx_checklist_runs_status ON [ChecklistRun](run_status, scheduled_at);
CREATE INDEX idx_ai_conversations_user_recent ON [AIConversation](user_id, updated_at DESC);
CREATE INDEX idx_ai_messages_conversation_recent ON [AIMessage](ai_conversation_id, created_at);
CREATE INDEX idx_articles_status_published ON [KnowledgeArticle](article_status, published_at DESC);
GO

-- ============================================================================
-- SEED SAMPLE DATA FOR TESTING & DEMONSTRATION
-- Default login password for all accounts: Password@123
-- BCrypt Hash: $2a$10$8jIMVzui5SoBFkUR1udxQO3Nqp09gvVoXobsoScb/zlisb4JIieDC
-- Default OTP: 123456 ($2a$10$0pPbCigvJwUsf6/sskJIe.SwZ2mNtfIFaXiXTCF1kCjTwmXi6ghqO)
-- ============================================================================

-- 1. ACCOUNTS & USERS (IDs: 1 to 9)
SET IDENTITY_INSERT [User] ON;
INSERT INTO [User] (user_id, full_name, email, phone, password_hash, system_role, account_status, email_verified_at)
VALUES 
(1, N'System Administrator', 'admin@cropdiary.com', '0901234567', '$2a$10$8jIMVzui5SoBFkUR1udxQO3Nqp09gvVoXobsoScb/zlisb4JIieDC', 'admin', 'active', SYSUTCDATETIME()),
(2, N'Tran Van (Da Lat Farm Owner)', 'owner1@dalatfarm.com', '0912345678', '$2a$10$8jIMVzui5SoBFkUR1udxQO3Nqp09gvVoXobsoScb/zlisb4JIieDC', 'user', 'active', SYSUTCDATETIME()),
(3, N'Le Thi (Mekong Orchard Owner)', 'owner2@mekongfarm.com', '0923456789', '$2a$10$8jIMVzui5SoBFkUR1udxQO3Nqp09gvVoXobsoScb/zlisb4JIieDC', 'user', 'active', SYSUTCDATETIME()),
(4, N'Nguyen Van (Da Lat Farm Technician)', 'staff1@dalatfarm.com', '0934567890', '$2a$10$8jIMVzui5SoBFkUR1udxQO3Nqp09gvVoXobsoScb/zlisb4JIieDC', 'user', 'active', SYSUTCDATETIME()),
(5, N'Hoang Minh (Production Area Supervisor)', 'staff2@dalatfarm.com', '0945678901', '$2a$10$8jIMVzui5SoBFkUR1udxQO3Nqp09gvVoXobsoScb/zlisb4JIieDC', 'user', 'active', SYSUTCDATETIME()),
(6, N'Pham Van (Pending Activation)', 'pending.user@cropdiary.com', '0956789012', '$2a$10$8jIMVzui5SoBFkUR1udxQO3Nqp09gvVoXobsoScb/zlisb4JIieDC', 'user', 'pending', NULL),
(7, N'Tran Van Binh (Field Care Worker)', 'worker1@dalatfarm.com', '0967890123', '$2a$10$8jIMVzui5SoBFkUR1udxQO3Nqp09gvVoXobsoScb/zlisb4JIieDC', 'user', 'active', SYSUTCDATETIME()),
(8, N'Le Thi Hoa (Harvest Worker)', 'worker2@dalatfarm.com', '0978901234', '$2a$10$8jIMVzui5SoBFkUR1udxQO3Nqp09gvVoXobsoScb/zlisb4JIieDC', 'user', 'active', SYSUTCDATETIME()),
(9, N'Dang Huu Phuc (Applicant Farm Owner)', 'register.farm@cropdiary.com', '0989012345', '$2a$10$8jIMVzui5SoBFkUR1udxQO3Nqp09gvVoXobsoScb/zlisb4JIieDC', 'user', 'active', SYSUTCDATETIME());
SET IDENTITY_INSERT [User] OFF;
GO

-- 2. ACCOUNT TOKENS (IDs: 1 to 2)
SET IDENTITY_INSERT [AccountToken] ON;
INSERT INTO [AccountToken] (token_id, user_id, token_hash, token_type, expires_at)
VALUES
(1, 6, '$2a$10$0pPbCigvJwUsf6/sskJIe.SwZ2mNtfIFaXiXTCF1kCjTwmXi6ghqO', 'email_verification', '2026-12-31 23:59:59'),
(2, 4, 'token-hash-refresh-staff1-2026', 'refresh', '2026-12-31 23:59:59');
SET IDENTITY_INSERT [AccountToken] OFF;
GO

-- 3. FARMS (IDs: 1 to 3)
SET IDENTITY_INSERT [Farm] ON;
INSERT INTO [Farm] (farm_id, farm_name, description, address, latitude, longitude, cover_image_url, created_by, farm_status)
VALUES
(1, N'Da Lat Clean Produce Farm', N'High-tech VietGAP melon and sweet bell pepper specialist farm', N'123 Mai Anh Dao, Ward 8, Da Lat City, Lam Dong', 11.954500, 108.445200, 'https://images.unsplash.com/photo-1500937386664-56d1dfef3854', 2, 'active'),
(2, N'Mekong Fruit Tree Cooperative', N'Export-grade Cat Chu mango cooperative under GlobalGAP standards', N'Binh Hoa Hamlet, My Xuong Commune, Cao Lanh District, Dong Thap', 10.450100, 105.632200, 'https://images.unsplash.com/photo-1592982537447-7440770cbfc9', 3, 'active'),
(3, N'Ba Vi Ecological Organic Farm', N'Ecological organic vegetable farm under planning', N'Yen Bai Commune, Ba Vi District, Hanoi', 21.054300, 105.352100, NULL, 1, 'inactive');
SET IDENTITY_INSERT [Farm] OFF;
GO

-- 4. FARM REGISTRATIONS (IDs: 1 to 4)
SET IDENTITY_INSERT [FarmRegistration] ON;
INSERT INTO [FarmRegistration] (registration_id, user_id, farm_name, address, province, district, ward, document_url, registration_note, registration_status, reviewed_by, reviewed_at, resulting_farm_id)
VALUES
(1, 2, N'Da Lat Clean Produce Farm', N'123 Mai Anh Đào', N'Lâm Đồng', N'TP Đà Lạt', N'Phường 8', 'https://example.com/docs/vietgap-dalat.pdf', N'Valid application with full VietGAP certification attached', 'approved', 1, '2025-01-02 10:00:00', 1),
(2, 3, N'Mekong Fruit Tree Cooperative', N'Ấp Bình Hòa', N'Đồng Tháp', N'Huyện Cao Lãnh', N'Xã Mỹ Xương', 'https://example.com/docs/globalgap-mekong.pdf', N'Export-ready cooperative application complying with GlobalGAP', 'approved', 1, '2025-02-15 14:30:00', 2),
(3, 9, N'Central Highlands Green Farm', N'Thôn 4', N'Đắk Lắk', N'TP Buôn Ma Thuột', N'Xã Ea Kao', 'https://example.com/docs/taynguyen-farm.pdf', N'Clean coffee and durian farm application, pending admin review', 'pending', NULL, NULL, NULL),
(4, 9, N'Kon Tum Herbal Garden', N'Xã Măng Cành', N'Kon Tum', N'Huyện Kon Plông', N'Xã Măng Cành', NULL, N'Missing official land use rights certificate and GPS survey coordinates', 'rejected', 1, '2025-11-20 09:15:00', NULL);
SET IDENTITY_INSERT [FarmRegistration] OFF;
GO

-- 5. FARM MEMBERS (IDs: 1 to 6)
SET IDENTITY_INSERT [FarmMember] ON;
INSERT INTO [FarmMember] (farm_member_id, farm_id, user_id, member_role, membership_status)
VALUES
(1, 1, 2, 'owner', 'active'),
(2, 1, 4, 'member', 'active'),
(3, 1, 5, 'manager', 'active'),
(4, 1, 7, 'member', 'active'),
(5, 1, 8, 'member', 'active'),
(6, 2, 3, 'owner', 'active');
SET IDENTITY_INSERT [FarmMember] OFF;
GO

-- 6. FARM WORKERS (IDs: 1 to 3)
SET IDENTITY_INSERT [FarmWorker] ON;
INSERT INTO [FarmWorker] (farm_worker_id, farm_member_id, worker_code, job_title, employment_status, hired_at, notes)
VALUES
(1, 2, 'CN-DL-01', N'Nutrition & Irrigation Engineer', 'active', '2025-01-01', N'Responsible for A+B hydroponic stock solution formula and dosage control'),
(2, 4, 'CN-DL-02', N'Plant Pruning & Care Worker', 'active', '2025-03-01', N'Skilled in lateral shoot pruning, canopy management, and manual pollination'),
(3, 5, 'CN-DL-03', N'Harvest & Packaging Specialist', 'seasonal', '2025-09-01', N'Seasonal harvest and refrigerated packaging contract');
SET IDENTITY_INSERT [FarmWorker] OFF;
GO

-- 7. CROP CATEGORIES (IDs: 1 to 20)
SET IDENTITY_INSERT [CropCategory] ON;
INSERT INTO [CropCategory] (crop_category_id, category_name, description, image_url)
VALUES
(1, N'Cantaloupe Melon', N'High-tech Japanese orange flesh cantaloupe melon', 'https://images.unsplash.com/photo-1598170845058-32b9d6a5da37'),
(2, N'Dalat Bell Pepper', N'Greenhouse red, yellow and orange sweet crisp bell peppers', 'https://images.unsplash.com/photo-1563565375-f3fdfdbefa83'),
(3, N'Cherry Tomato', N'Sweet truss cherry tomatoes for export', 'https://images.unsplash.com/photo-1592924357228-91a4daadcfea'),
(4, N'Cat Chu Mango', N'Dong Thap specialty Cat Chu export mango', 'https://images.unsplash.com/photo-1553279768-865429fa0078'),
(5, N'Water Spinach', N'Short-cycle leafy vegetable, 25-30 days to harvest', 'https://images.unsplash.com/photo-1540420773420-3366772f4999'),
(6, N'Butterhead Lettuce', N'Hydroponic or clean soil leafy greens, 35-45 days cycle', 'https://images.unsplash.com/photo-1556801712-76c8eb07bbc9'),
(7, N'Bok Choy', N'Nutrient-rich VietGAP leafy green, 30-40 days cycle', 'https://images.unsplash.com/photo-1594282486552-05b4d80fbb9f'),
(8, N'Spinach', N'Temperate leafy green rich in iron and minerals, 35-45 days cycle', 'https://images.unsplash.com/photo-1576045057995-568f588f82fb'),
(9, N'Choy Sum', N'Popular fast-growing Asian green, 25-35 days cycle', 'https://images.unsplash.com/photo-1540420773420-3366772f4999'),
(10, N'Cucumber', N'High-yielding trellised fruiting crop, 45-60 days cycle', 'https://images.unsplash.com/photo-1604977042946-1eecc30f269e'),
(11, N'Green Beans', N'Nitrogen-fixing bush bean, 50-60 days cycle', 'https://images.unsplash.com/photo-1551462147-37885acc36f1'),
(12, N'Okra', N'Heat-tolerant fiber-rich fruiting vegetable, 45-55 days cycle', 'https://images.unsplash.com/photo-1425543103986-22abb7d7e8d2'),
(13, N'Baby Carrot', N'Crisp and sweet VietGAP root vegetable, 60-75 days cycle', 'https://images.unsplash.com/photo-1598170845058-32b9d6a5da37'),
(14, N'White Radish', N'Fast-growing nutritious daikon radish, 40-50 days cycle', 'https://images.unsplash.com/photo-1590779033100-9f60a05a013d'),
(15, N'Scallion & Fresh Herbs', N'Continuous harvest culinary herbs, 30-40 days cycle', 'https://images.unsplash.com/photo-1615485290382-441e4d049cb5'),
(16, N'Malabar Spinach', N'Summer cooling leafy vine green, 30-40 days cycle', 'https://images.unsplash.com/photo-1540420773420-3366772f4999'),
(17, N'Cabbage', N'Winter-spring heading brassica, 65-75 days cycle', 'https://images.unsplash.com/photo-1550828520-4cb496926fc9'),
(18, N'Watermelon', N'Crisp sweet red flesh fruit, 60-70 days cycle', 'https://images.unsplash.com/photo-1587049352846-4a222e784d38'),
(19, N'Winter Melon', N'Trellised VietGAP wax gourd, 60-70 days cycle', 'https://images.unsplash.com/photo-1570715621210-b74704bdfa57'),
(20, N'Sweet Corn', N'High-grade sweet cereal crop, 70-80 days cycle', 'https://images.unsplash.com/photo-1551754655-cd27e38d2076');
SET IDENTITY_INSERT [CropCategory] OFF;
GO

-- 8. CROP SEASONS (IDs: 1 to 3)
SET IDENTITY_INSERT [CropSeason] ON;
INSERT INTO [CropSeason] (crop_season_id, farm_id, crop_category_id, season_name, start_date, end_date, season_status, notes, created_by)
VALUES
(1, 1, 1, N'Spring 2026 Cantaloupe Season', '2026-01-15', '2026-04-30', 'active', N'Key spring melon season serving festival market demand', 2),
(2, 1, 2, N'Winter 2025 Bell Pepper Season', '2025-09-01', '2025-12-25', 'completed', N'Season fully harvested, packed, and accounts settled', 2),
(3, 1, 3, N'Summer 2026 Tomato Season', '2026-05-01', '2026-08-30', 'planned', N'Planned planting schedule for Greenhouse #2', 2);
SET IDENTITY_INSERT [CropSeason] OFF;
GO

-- 9. ZONES (IDs: 1 to 4)
SET IDENTITY_INSERT [Zone] ON;
INSERT INTO [Zone] (zone_id, farm_id, zone_name, description, area_value, area_unit)
VALUES
(1, 1, N'Zone A - High-Tech Greenhouse', N'Automated hydroponic greenhouse zone for cantaloupe melons', 5000.00, 'm2'),
(2, 2, N'Zone B - Open Field Beds', N'Outdoor cultivation zone for bell peppers and root crops', 8000.00, 'm2'),
(3, 1, N'Zone C - Certified Nursery', N'Certified disease-free seedling nursery zone', 2000.00, 'm2'),
(4, 2, N'Zone 1 - Mango Orchard', N'Mature grafted Cat Chu mango orchard zone', 15000.00, 'm2');
SET IDENTITY_INSERT [Zone] OFF;
GO

-- 10. BLOCKS (IDs: 1 to 5)
SET IDENTITY_INSERT [Block] ON;
INSERT INTO [Block] (block_id, zone_id, block_name, description, area_value, area_unit, soil_type)
VALUES
(1, 1, N'Block A1', N'Block F1 Japanese Taka cantaloupe melon', 2500.00, 'm2', N'Desalinated coco peat hydroponic substrate'),
(2, 1, N'Block A2', N'Block Huynh Long golden cantaloupe melon', 2500.00, 'm2', N'Desalinated coco peat hydroponic substrate'),
(3, 2, N'Block B1', N'Block sweet red bell peppers', 4000.00, 'm2', N'Lam Dong basalt red soil'),
(4, 2, N'Block B2', N'Block sweet yellow bell peppers', 4000.00, 'm2', N'Lam Dong basalt red soil'),
(5, 3, N'Block C1', N'Block melon nursery seedling trays', 2000.00, 'm2', N'Coco peat with bio-char and beneficial microbes');
SET IDENTITY_INSERT [Block] OFF;
GO

-- 11. CROP SEASON BLOCKS (Composite PK)
INSERT INTO [CropSeasonBlock] (crop_season_id, block_id, planted_area, area_unit, planted_at, expected_harvest_at)
VALUES
(1, 1, 2500.00, 'm2', '2026-01-15', '2026-04-25'),
(1, 2, 2500.00, 'm2', '2026-01-20', '2026-04-30'),
(2, 3, 4000.00, 'm2', '2025-09-01', '2025-12-20');
GO

-- 12. STAFF ZONE ASSIGNMENTS (IDs: 1 to 3)
SET IDENTITY_INSERT [StaffZoneAssignment] ON;
INSERT INTO [StaffZoneAssignment] (assignment_id, farm_member_id, zone_id, assigned_at, ended_at)
VALUES
(1, 2, 1, '2025-01-01 00:00:00', NULL),
(2, 3, 1, '2025-01-01 00:00:00', NULL),
(3, 4, 1, '2025-06-01 00:00:00', NULL);
SET IDENTITY_INSERT [StaffZoneAssignment] OFF;
GO

-- 13. FARM TASKS (IDs: 1 to 6)
SET IDENTITY_INSERT [FarmTask] ON;
INSERT INTO [FarmTask] (farm_task_id, farm_id, crop_season_id, zone_id, block_id, task_name, description, task_type, priority, status, starts_at, due_at, created_by)
VALUES
(1, 1, 1, 1, 1, N'Coco peat substrate conditioning', N'Rinse tannins and treat with Trichoderma fungal biocontrol', N'Land Prep', 'high', 'completed', '2026-01-10 07:00:00', '2026-01-14 17:00:00', 2),
(2, 1, 1, 1, 1, N'Transplant melon seedlings', N'Move 12-day-old nursery seedlings to production grow bags', N'Planting', 'urgent', 'completed', '2026-01-15 06:30:00', '2026-01-16 11:30:00', 2),
(3, 1, 1, 1, 1, N'Drip fertigation run #3', N'Mix nutrient solution A+B at EC 1.8 and execute automated drip cycle', N'Fertilization & Irrigation', 'normal', 'completed', '2026-02-10 08:00:00', '2026-02-10 11:00:00', 2),
(4, 1, 1, 1, 1, N'Manual melon pollination', N'Hand pollinate female flowers at nodes 9 through 12', N'Crop Care', 'high', 'in_progress', '2026-02-25 07:00:00', '2026-03-05 10:00:00', 2),
(5, 1, 1, 1, 1, N'Biological thrips prevention', N'Apply organic Neem Chito botanical spray against thrips', N'Plant Protection', 'normal', 'planned', '2026-03-08 16:00:00', '2026-03-08 18:00:00', 2),
(6, 1, 1, 1, 1, N'Harvest ripe sweet melons', N'Cut T-stems, weigh batches, and apply QR traceability labels', N'Harvest', 'urgent', 'planned', '2026-04-22 06:00:00', '2026-04-25 17:00:00', 2);
SET IDENTITY_INSERT [FarmTask] OFF;
GO

-- 14. TASK ASSIGNMENTS (Composite PK)
INSERT INTO [TaskAssignment] (farm_task_id, farm_worker_id, assigned_by, assigned_at, assignment_status)
VALUES
(3, 1, 2, '2026-02-09 15:00:00', 'completed'),
(4, 2, 2, '2026-02-24 16:30:00', 'accepted'),
(5, 2, 2, '2026-03-01 08:00:00', 'assigned');
GO

-- 15. FARMING ACTIVITIES (IDs: 1 to 4)
SET IDENTITY_INSERT [FarmingActivity] ON;
INSERT INTO [FarmingActivity] (farming_activity_id, farm_id, farm_task_id, crop_season_id, zone_id, block_id, activity_name, activity_type, activity_notes, performed_at, image_url, created_by)
VALUES
(1, 1, 3, 1, 1, 1, N'Drip fertigation run A+B EC 1.8', N'Fertilization & Irrigation', N'Dispensed 10L of A+B stock solution into 1000L central tank', '2026-02-10 08:30:00', NULL, 4),
(2, 1, 4, 1, 1, 1, N'Shoot pruning and melon pollination', N'Crop Care', N'Pollinated approx 1,200 plants along rows 1 through 15', '2026-02-28 07:45:00', NULL, 4),
(3, 1, NULL, 1, 1, 1, N'Organic preventative fungal spray', N'Plant Protection', N'Applied 2 bottles of Neem Chito in late afternoon cool weather', '2026-03-01 16:30:00', NULL, 4),
(4, 1, NULL, 2, 2, 3, N'Final harvest of Winter 2025 bell peppers', N'Harvest', N'Harvested all red ripe fruit across the parcel', '2025-12-20 09:00:00', NULL, 4);
SET IDENTITY_INSERT [FarmingActivity] OFF;
GO

-- 16. ACTIVITY WORKERS (Composite PK)
INSERT INTO [ActivityWorker] (farming_activity_id, farm_worker_id, participation_role)
VALUES
(1, 1, N'Nutrient mixing technician'),
(2, 2, N'Lead pollination worker'),
(4, 2, N'Picker'),
(4, 3, N'Grading and crate packing worker');
GO

-- 17. ACTIVITY SUPERVISORS (Composite PK)
INSERT INTO [ActivitySupervisor] (farming_activity_id, farm_member_id)
VALUES
(1, 3),
(4, 3);
GO

-- 18. ZONE RISK ASSESSMENTS (IDs: 1 to 2)
SET IDENTITY_INSERT [ZoneRiskAssessment] ON;
INSERT INTO [ZoneRiskAssessment] (zone_risk_assessment_id, zone_id, assessed_by, risk_type, risk_level, description, mitigation_plan, assessed_at)
VALUES
(1, 1, 3, N'High Humidity & Downy Mildew', 'medium', N'Nighttime greenhouse humidity exceeds 92%, increasing downy mildew fungal risk', N'Activate convection fans and crack roof ventilation open starting at 05:00 AM', '2026-01-20 10:00:00'),
(2, 2, 3, N'Hillside Soil Erosion', 'low', N'Heavy rains may erode surface topsoil along edge beds', N'Plant vetiver grass along embankment edges and dig cross-slope drainage trenches', '2026-01-22 14:00:00');
SET IDENTITY_INSERT [ZoneRiskAssessment] OFF;
GO

-- 19. WATER SOURCES (IDs: 1 to 2)
SET IDENTITY_INSERT [WaterSource] ON;
INSERT INTO [WaterSource] (water_source_id, farm_id, source_name, source_type, description)
VALUES
(1, 1, N'Cam Ly Stream Settling Reservoir', N'Surface reservoir', N'Capacity 2,500 m3 equipped with sand filtration and microbial disc filtration systems'),
(2, 1, N'Shallow Aquifer Borehole Well #1', N'Groundwater', N'Well depth 70m, clear water with low mineral hardness');
SET IDENTITY_INSERT [WaterSource] OFF;
GO

-- 20. WATER ASSESSMENTS (IDs: 1 to 2)
SET IDENTITY_INSERT [WaterAssessment] ON;
INSERT INTO [WaterAssessment] (water_assessment_id, water_source_id, block_id, conducted_by, water_ph, temperature_c, quality_status, notes, assessed_at)
VALUES
(1, 1, 1, 2, 6.60, 20.50, N'VietGAP Irrigation Compliant', N'Heavy metal content and E. coli counts are well below permissible safety thresholds', '2026-01-12 08:30:00'),
(2, 2, 1, 2, 6.20, 21.00, N'Excellent Quality', N'Groundwater is exceptionally soft and clean, optimal for hydroponic nutrient formulation', '2026-02-15 09:00:00');
SET IDENTITY_INSERT [WaterAssessment] OFF;
GO

-- 21. SOIL ASSESSMENTS (IDs: 1 to 2)
SET IDENTITY_INSERT [SoilAssessment] ON;
INSERT INTO [SoilAssessment] (soil_assessment_id, block_id, conducted_by, soil_ph, moisture_percent, nutrient_data, notes, assessed_at)
VALUES
(1, 1, 2, 6.40, 72.50, N'{"EC_mS_cm": 1.82, "N_ppm": 140, "P_ppm": 45, "K_ppm": 210, "Ca_ppm": 160, "Mg_ppm": 55}', N'Coco peat substrate is stable, electrical conductivity (EC) is at optimal level for flowering and fruit set', '2026-02-18 10:15:00'),
(2, 3, 2, 5.80, 65.00, N'{"EC_mS_cm": 1.15, "N_ppm": 95, "P_ppm": 32, "K_ppm": 140, "Organic_matter_pct": 3.8}', N'Basalt red soil is fertile; apply agricultural lime to slightly raise pH to 6.2 prior to new crop cycle', '2026-02-20 11:00:00');
SET IDENTITY_INSERT [SoilAssessment] OFF;
GO

-- 22. BLOCK CONDITION OBSERVATIONS (IDs: 1 to 2)
SET IDENTITY_INSERT [BlockConditionObservation] ON;
INSERT INTO [BlockConditionObservation] (observation_id, block_id, recorded_by, condition_type, description, observed_at)
VALUES
(1, 1, 2, N'Crop Growth', N'Cantaloupe plants at node 10 develop uniformly, female flower buds are robust with no micro-nutrient deficiency signs', '2026-02-26 08:00:00'),
(2, 3, 2, N'Pest & Disease Status', N'Soil is dry, scattered light powdery mildew spots observed on older leaves in the southeast corner', '2026-02-27 15:30:00');
SET IDENTITY_INSERT [BlockConditionObservation] OFF;
GO

-- 23. SUPPLIERS (IDs: 1 to 2)
SET IDENTITY_INSERT [Supplier] ON;
INSERT INTO [Supplier] (supplier_id, farm_id, supplier_name, contact_name, phone, email, address, notes)
VALUES
(1, 1, N'Da Lat Green Agrochemical Co.', N'Nguyễn Phi Hùng', '0988123456', 'contact@nongduocxanh.vn', N'Loc Phat Industrial Zone, Bao Loc City, Lam Dong', N'Reputable supplier of soluble fertilizers and greenhouse materials'),
(2, 1, N'AgriBio Hydroponics Solutions', N'Lê Thị Mai', '0977234567', 'sales@agribio.com.vn', N'54 Hung Vuong, Ward 9, Da Lat City', N'Exclusive regional distributor of GreenHydro hydroponic nutrient solutions');
SET IDENTITY_INSERT [Supplier] OFF;
GO

-- 24. MATERIALS (IDs: 1 to 5)
SET IDENTITY_INSERT [Material] ON;
INSERT INTO [Material] (material_id, farm_id, material_name, material_type, unit, description, is_active)
VALUES
(1, 1, N'Haifa NPK 20-20-20 Soluble Fertilizer', N'Soluble Fertilizer', 'bag', N'Pure fully-soluble NPK fertilizer imported from Israel (25kg Bag)', 1),
(2, 1, N'GreenHydro A+B Hydroponic Solution', N'Liquid Nutrient', 'canister', N'Concentrated macro and micro mineral solution for melons (5L Canister)', 1),
(3, 1, N'Neem Chito Organic Insecticide', N'Biological Pest Control', 'bottle', N'Cold-pressed neem oil and chitosan formulation against thrips (1L Bottle)', 1),
(4, 1, N'Japanese F1 Taka Cantaloupe Seeds', N'Seeds', 'pack', N'Certified hybrid seed stock with >98% germination and 14-16 brix potential', 1),
(5, 1, N'5kg Export Cantaloupe Carton Boxes', N'Packaging & Storage', 'box', N'5-ply corrugated carton boxes branded with VietGAP MyCropDiary QR codes', 1);
SET IDENTITY_INSERT [Material] OFF;
GO

-- 25. WAREHOUSES (IDs: 1 to 2)
SET IDENTITY_INSERT [Warehouse] ON;
INSERT INTO [Warehouse] (warehouse_id, farm_id, warehouse_name, address)
VALUES
(1, 1, N'Central Agricultural Supplies Warehouse', N'Technical Facility #1, Da Lat Farm'),
(2, 1, N'Cold Storage Facility for Fresh Produce', N'Packaging & Post-Harvest Center, Da Lat Farm');
SET IDENTITY_INSERT [Warehouse] OFF;
GO

-- 26. INVENTORY (IDs: 1 to 5)
SET IDENTITY_INSERT [Inventory] ON;
INSERT INTO [Inventory] (inventory_id, warehouse_id, material_id, quantity_on_hand, reorder_level)
VALUES
(1, 1, 1, 450.000, 100.000),
(2, 1, 2, 180.000, 50.000),
(3, 1, 3, 45.000, 15.000),
(4, 1, 4, 25.000, 10.000),
(5, 2, 5, 850.000, 200.000);
SET IDENTITY_INSERT [Inventory] OFF;
GO

-- 27. INPUT PURCHASES (IDs: 1 to 2)
SET IDENTITY_INSERT [InputPurchase] ON;
INSERT INTO [InputPurchase] (input_purchase_id, farm_id, supplier_id, purchased_by, purchase_date, reference_no, total_amount, notes)
VALUES
(1, 1, 2, 3, '2026-01-05', 'PO-2026-001', 18500000.00, N'Procurement of nutrients and soluble fertilizers for early cantaloupe season'),
(2, 1, 1, 3, '2026-01-20', 'PO-2026-002', 7200000.00, N'Procurement of Neem Chito biological insecticide');
SET IDENTITY_INSERT [InputPurchase] OFF;
GO

-- 28. INPUT PURCHASE ITEMS (IDs: 1 to 3)
SET IDENTITY_INSERT [InputPurchaseItem] ON;
INSERT INTO [InputPurchaseItem] (purchase_item_id, input_purchase_id, material_id, warehouse_id, quantity, unit_cost)
VALUES
(1, 1, 1, 1, 300.000, 35000.00),
(2, 1, 2, 1, 160.000, 50000.00),
(3, 2, 3, 1, 40.000, 180000.00);
SET IDENTITY_INSERT [InputPurchaseItem] OFF;
GO

-- 29. MATERIAL USAGES (IDs: 1 to 2)
SET IDENTITY_INSERT [MaterialUsage] ON;
INSERT INTO [MaterialUsage] (material_usage_id, farm_id, material_id, farm_task_id, farming_activity_id, block_id, used_by, quantity, dosage, method, safety_interval_days, used_at, notes)
VALUES
(1, 1, 2, 3, 1, 1, 2, 10.000, N'10kg/1000L', N'Drip irrigation', 0, '2026-02-10 08:30:00', N'Mixed into 1,000L mother tank for automated drip irrigation cycle'),
(2, 1, 3, NULL, 3, 1, 2, 2.000, N'20ml/16L knapsack', N'Foliar spray', 7, '2026-03-01 16:30:00', N'Fine mist sprayed evenly on both sides of foliage in late afternoon cool weather');
SET IDENTITY_INSERT [MaterialUsage] OFF;
GO

-- 30. INVENTORY TRANSACTIONS (IDs: 1 to 3)
SET IDENTITY_INSERT [InventoryTransaction] ON;
INSERT INTO [InventoryTransaction] (inventory_transaction_id, inventory_id, transaction_type, quantity_change, reference_type, reference_id, notes, created_by)
VALUES
(1, 1, 'purchase', 300.000, 'InputPurchase', 1, N'Warehouse receipt for PO-2026-001', 2),
(2, 2, 'purchase', 160.000, 'InputPurchase', 1, N'Warehouse receipt for PO-2026-001', 2),
(3, 2, 'usage', -10.000, 'MaterialUsage', 1, N'Warehouse dispatch for fertigation on Block A1', 4);
SET IDENTITY_INSERT [InventoryTransaction] OFF;
GO

-- 31. EXPENSES (IDs: 1 to 3)
SET IDENTITY_INSERT [Expense] ON;
INSERT INTO [Expense] (expense_id, farm_id, crop_season_id, farm_task_id, expense_category, description, amount, expense_date, recorded_by)
VALUES
(1, 1, 1, NULL, N'Materials & Fertilizers', N'Purchased soluble fertilizers and concentrated mineral solutions', 18500000.00, '2026-01-05', 3),
(2, 1, 1, 1, N'Labor', N'Wages for washing and conditioning 5,000 coco peat grow bags', 4500000.00, '2026-01-14', 3),
(3, 1, 1, NULL, N'Utilities & Power', N'Electricity utility bill for automated pump operations in Jan 2026', 2800000.00, '2026-02-01', 3);
SET IDENTITY_INSERT [Expense] OFF;
GO

-- 32. HARVEST RECORDS (IDs: 1 to 2)
SET IDENTITY_INSERT [HarvestRecord] ON;
INSERT INTO [HarvestRecord] (harvest_record_id, farm_id, crop_season_id, block_id, farming_activity_id, recorded_by, harvest_date, quantity, unit, quality_grade, notes)
VALUES
(1, 1, 2, 3, 4, 2, '2025-12-20', 1250.000, 'kg', N'Grade 1 (Export)', N'Uniform red bell peppers, firm fruit with fresh green calyxes'),
(2, 1, 2, 3, 4, 2, '2025-12-20', 350.000, 'kg', N'Grade 2 (Domestic)', N'Slight curvature, sweet and crisp quality preserved');
SET IDENTITY_INSERT [HarvestRecord] OFF;
GO

-- 33. PRODUCT SALES (IDs: 1 to 2)
SET IDENTITY_INSERT [ProductSale] ON;
INSERT INTO [ProductSale] (product_sale_id, farm_id, harvest_record_id, recorded_by, buyer_name, sale_date, quantity, unit, unit_price, notes)
VALUES
(1, 1, 1, 3, N'WinMart Southern Supermarket Chain', '2025-12-21', 1200.000, 'kg', 42000.00, N'VietGAP fresh produce supply agreement contract'),
(2, 1, 2, 3, N'Bac Tom Clean Food Chain Hanoi', '2025-12-22', 350.000, 'kg', 28000.00, N'Overnight air freight refrigerated express distribution');
SET IDENTITY_INSERT [ProductSale] OFF;
GO

-- 34. CHECKLIST RULES (IDs: 1 to 3)
SET IDENTITY_INSERT [ChecklistRule] ON;
INSERT INTO [ChecklistRule] (checklist_rule_id, farm_id, created_by, rule_name, description, category, is_active)
VALUES
(1, 1, 2, N'Monitor irrigation system pH and EC levels', N'Inspect irrigation nutrient concentration and pH prior to morning operational shift', N'Irrigation & Nutrition', 1),
(2, 1, 2, N'Observe Pre-Harvest Interval (PHI) compliance', N'Strict quarantine of at least 14 days following pest control before commencing harvest', N'VietGAP Food Safety', 1),
(3, 1, 2, N'Provide certified worker personal protective gear', N'Equip operators with nitrile gloves, activated carbon respirators, and rubber boots', N'Occupational Safety', 1);
SET IDENTITY_INSERT [ChecklistRule] OFF;
GO

-- 35. CHECKLIST RUNS (IDs: 1 to 3)
SET IDENTITY_INSERT [ChecklistRun] ON;
INSERT INTO [ChecklistRun] (checklist_run_id, checklist_rule_id, crop_season_id, block_id, assigned_to, run_status, scheduled_at, completed_at)
VALUES
(1, 1, 1, 1, 2, 'completed', '2026-02-10 07:00:00', '2026-02-10 07:30:00'),
(2, 2, 2, 3, 3, 'completed', '2025-12-18 09:00:00', '2025-12-18 09:45:00'),
(3, 3, 1, 1, 2, 'open', '2026-03-08 15:30:00', NULL);
SET IDENTITY_INSERT [ChecklistRun] OFF;
GO

-- 36. CHECKLIST RESULTS (IDs: 1 to 2)
SET IDENTITY_INSERT [ChecklistResult] ON;
INSERT INTO [ChecklistResult] (checklist_result_id, checklist_run_id, checked_by, result_status, notes)
VALUES
(1, 1, 2, 'pass', N'pH measured at 6.35, EC measured at 1.82 mS/cm - Fully compliant'),
(2, 2, 3, 'pass', N'18 days observed since final neem oil application, pre-harvest safety criteria satisfied');
SET IDENTITY_INSERT [ChecklistResult] OFF;
GO

-- 37. PRODUCTION REPORTS (ID: 1)
SET IDENTITY_INSERT [ProductionReport] ON;
INSERT INTO [ProductionReport] (production_report_id, farm_id, crop_season_id, generated_by, report_title, report_data)
VALUES
(1, 1, 2, 3, N'Production Summary Report - Winter 2025 Bell Pepper Season', N'{"season":"Winter Season 2025","crop":"Bell Pepper","planted_area_m2":4000,"total_yield_kg":1600.0,"grade_1_kg":1250.0,"grade_2_kg":350.0,"total_revenue_vnd":60200000,"production_cost_vnd":25800000,"profit_vnd":34400000,"roi_pct":133.3}');
SET IDENTITY_INSERT [ProductionReport] OFF;
GO

-- 38. TRAINING RECORDS (IDs: 1 to 2)
SET IDENTITY_INSERT [TrainingRecord] ON;
INSERT INTO [TrainingRecord] (training_record_id, farm_id, organized_by, training_title, description, training_date)
VALUES
(1, 1, 3, N'Manual Cantaloupe Pollination Technical Workshop', N'Standardized training on selecting male flowers, detaching stamens, and dusting female stigmas at optimal hours', '2026-01-25'),
(2, 1, 3, N'VietGAP Fruit Harvesting and Net Packaging Protocols', N'Proper pedicel T-cut technique, grading tolerances, and temperature-controlled carton boxing', '2025-12-15');
SET IDENTITY_INSERT [TrainingRecord] OFF;
GO

-- 39. TRAINING ATTENDEES (Composite PK)
INSERT INTO [TrainingAttendee] (training_record_id, farm_worker_id, attendance_status, notes)
VALUES
(1, 1, 'attended', N'Demonstrated comprehensive mastery of technical procedures'),
(1, 2, 'attended', N'Practical performance evaluated as meeting standards'),
(2, 2, 'attended', N'Attended full session duration'),
(2, 3, 'attended', N'Attended full session duration');
GO

-- 40. ASSESSMENT CRITERIA (IDs: 1 to 3)
SET IDENTITY_INSERT [AssessmentCriterion] ON;
INSERT INTO [AssessmentCriterion] (assessment_criterion_id, farm_id, criterion_name, description, max_score, is_active)
VALUES
(1, 1, N'Farming Logbook Recordkeeping Integrity', N'Detailed and timely logging of fertigation schedules, chemical applications, and assigned supervisors', 100.00, 1),
(2, 1, N'Production Area Sanitation and Biosecurity', N'Greenhouse perimeter is clean and weed-free, insect netting mesh is fully intact with airlock entry', 100.00, 1),
(3, 1, N'Agrochemical Storage and Packaging Safety', N'Agrochemical storage is secured with lock and key, hazardous warning signage, personal PPE, and eyewash stations', 100.00, 1);
SET IDENTITY_INSERT [AssessmentCriterion] OFF;
GO

-- 41. INTERNAL ASSESSMENTS (ID: 1)
SET IDENTITY_INSERT [InternalAssessment] ON;
INSERT INTO [InternalAssessment] (internal_assessment_id, farm_id, crop_season_id, zone_id, assessed_by, assessment_title, notes)
VALUES
(1, 1, 1, 1, 3, N'Quarterly VietGAP Internal Audit - High-Tech Greenhouse Q1/2026', N'The farm rigorously adheres to good agricultural practice standards and environmental safety protocols');
SET IDENTITY_INSERT [InternalAssessment] OFF;
GO

-- 42. INTERNAL ASSESSMENT ITEMS (IDs: 1 to 3)
SET IDENTITY_INSERT [InternalAssessmentItem] ON;
INSERT INTO [InternalAssessmentItem] (internal_assessment_item_id, internal_assessment_id, assessment_criterion_id, score, result_notes)
VALUES
(1, 1, 1, 96.00, N'MyCropDiary digital logging provides end-to-end transparent traceability'),
(2, 1, 2, 92.00, N'Insect exclusion mesh is clean, sealed, and devoid of tears'),
(3, 1, 3, 98.00, N'All agrochemical containers carry authentic manufacturer labels and traceable batch numbers');
SET IDENTITY_INSERT [InternalAssessmentItem] OFF;
GO

-- 43. KNOWLEDGE ARTICLES (IDs: 1 to 30)
SET IDENTITY_INSERT [KnowledgeArticle] ON;
INSERT INTO [KnowledgeArticle] (knowledge_article_id, created_by, title, slug, summary, body, cover_image_url, category, tags, view_count, source_url, article_status, published_at)
VALUES
(1, 1, N'High-Tech Greenhouse Cantaloupe Cultivation under VietGAP', 'high-tech-greenhouse-cantaloupe-cultivation-vietgap', N'Comprehensive guidelines on substrate preparation, fertigation management, and humidity control', N'# High-Tech Greenhouse Cantaloupe Cultivation under VietGAP

## 1. Substrate Preparation
Coco peat must be thoroughly desalted and rinsed 3-4 times with fresh water to flush out tannins and lignin, followed by soaking in 1% agricultural lime solution for 24 hours.

## 2. Automated Drip Fertigation
- Seedling Stage (Days 1-15): EC 1.2 - 1.5 mS/cm, pH 5.8 - 6.2.
- Flowering and Pollination (Days 20-35): EC 1.8 - 2.0 mS/cm.
- Fruit Sizing and Netting (Days 40-60): Increase Potassium ratio, maintain EC 2.2 - 2.5 mS/cm.', 'https://images.unsplash.com/photo-1598170845058-32b9d6a5da37', 'seed-cultivation', N'cantaloupe, vietgap, greenhouse', 128, 'https://vietgap.gov.vn', 'published', SYSUTCDATETIME()),
(2, 1, N'Biological Control of Thrips and Whiteflies in Greenhouses', 'biological-control-thrips-whiteflies-greenhouses', N'Practical manual for scouting, trapping, and biologically suppressing sucking insect pests without toxic chemicals', N'# Biological Control of Sucking Pests in Greenhouses

Thrips are among the most destructive pests in melon production, causing leaf curling, deformed shoots, and transmitting viral mosaic pathogens.

### Recommended Integrated Pest Management (IPM) Practices
1. Seal all greenhouse ventilation openings with insect netting of at least 50 mesh density.
2. Hang sticky yellow and blue traps 15-20cm above the plant canopy to detect and capture adult pests.
3. Apply alternating preventative sprays of biological neem extract and Beauveria bassiana entomopathogenic fungus.', 'https://images.unsplash.com/photo-1563565375-f3fdfdbefa83', 'fertilizer-pesticide', N'thrips, biological control, whiteflies', 95, 'https://bvtv.gov.vn', 'published', SYSUTCDATETIME()),
(3, 1, N'What is VietGAP and Why Should Farm Households Adopt It?', 'vietgap-la-gi-va-vi-sao-nong-ho-nen-ap-dung', N'An overview of Good Agricultural Practices in Vietnam (VietGAP - TCVN 11892-1:2017), economic benefits for smallholders, and access to supermarket chains and export channels.', N'VietGAP (Vietnamese Good Agricultural Practices) is a comprehensive set of principles, procedures, and guidance for agricultural producers during cultivation, harvesting, and post-harvest handling. It guarantees food safety, elevates product quality, safeguards farmer and consumer health, protects natural ecosystems, and ensures end-to-end traceability.

By adopting VietGAP standards, farm owners and agricultural cooperatives establish steadfast trust with retail supermarkets, institutional food services, and export traders, while substantially optimizing expenditure on fertilizers and crop protection materials through quantifiable logging records.', 'https://images.unsplash.com/photo-1500937386664-56d1dfef3854?auto=format&fit=crop&w=800&q=80', 'regulation', N'overview, standards, certification', 1520, 'https://vietgap.gov.vn', 'published', DATEADD(DAY, -2, SYSUTCDATETIME())),
(4, 1, N'4 Mandatory Criteria Groups for VietGAP Certification', '4-nhom-tieu-chi-bat-buoc-trong-chung-nhan-vietgap', N'Detailed breakdown of the 4 core pillars of VietGAP: Food Safety, Environmental Protection, Occupational Health & Safety, and Product Traceability.', N'The VietGAP crop production standard comprises four foundational requirement pillars:
1. Assessment and selection of suitable production zones free from chemical and microbial contamination hazards.
2. Management of certified disease-free seeds, rootstocks, and propagules with authentic origin records.
3. Safe utilization of soils, substrates, organic fertilizers, irrigation water, and crop protection materials from approved registries.
4. Comprehensive farm logging and record archiving for a minimum of 12-24 months to support audit inspections and product traceability.

Every individual criterion is classified into Level A (100% mandatory compliance), Level B (recommended, requiring at least 90% compliance), and Level C (advisory best practices).', 'https://images.unsplash.com/photo-1738598668890-5f4d28842df5?auto=format&fit=crop&w=800&q=80', 'regulation', N'criteria, certification, class A standards', 1180, 'https://vietgap.gov.vn', 'published', DATEADD(DAY, -4, SYSUTCDATETIME())),
(5, 1, N'Step-by-Step Procedure for VietGAP Application and Certification', 'quy-trinh-dang-ky-va-cap-giay-chung-nhan-vietgap', N'Step-by-step guidance from site surveying, soil/water laboratory testing, and internal auditing to submitting official applications to MARD-accredited certification bodies.', N'The standard VietGAP certification pathway follows 6 structured phases:
- Step 1: Conduct site surveys, collect soil and irrigation water samples for accredited laboratory analysis of heavy metals and microbial pathogens.
- Step 2: Establish standardized operating procedures (SOPs) and institute farm production logbooks.
- Step 3: Organize comprehensive internal training workshops for farm staff and operators.
- Step 4: Execute cultivation, log all farm activities for at least one complete crop cycle, and conduct internal audit checklists.
- Step 5: Implement corrective actions for non-conformities and compile official documentation dossiers for accredited certification bodies.
- Step 6: Host on-site auditor field evaluations, pre-harvest crop residue sampling, and receive official VietGAP certification (valid for up to 3 years).', 'https://images.unsplash.com/photo-1727099079513-952d40de9d78?auto=format&fit=crop&w=800&q=80', 'regulation', N'procedure, application, certification body', 860, 'https://vietgap.gov.vn', 'published', DATEADD(DAY, -6, SYSUTCDATETIME())),
(6, 1, N'Comparing VietGAP, GlobalGAP, and Organic Agricultural Standards', 'so-sanh-tieu-chuan-vietgap-globalgap-va-huu-co', N'Similarities and operational distinctions among VietGAP, GlobalGAP, and Organic farming to help farm enterprises chart optimal certification pathways.', N'VietGAP is tailored specifically for Vietnamese cultivation conditions, permitting the judicious, regulated application of inorganic fertilizers and registered crop protection products subject to strict Pre-Harvest Intervals (PHI).

In contrast, GlobalGAP imposes rigorous standards concerning fair labor welfare, energy resource management, environmental biodiversity, and supply chain integrity for high-value export markets in the EU, North America, and Japan. Certified Organic farming completely bans synthetic chemical fertilizers, artificial pesticides, and genetically modified organisms (GMOs).', 'https://images.unsplash.com/photo-1464226184884-fa280b87c399?auto=format&fit=crop&w=800&q=80', 'regulation', N'VietGAP, GlobalGAP, organic', 945, 'https://vietgap.gov.vn', 'published', DATEADD(DAY, -7, SYSUTCDATETIME())),
(7, 1, N'Assessing and Selecting Safe Production Zones for VietGAP', 'danh-gia-va-lua-chon-vung-san-xuat-an-toan', N'Essential site survey factors: historical land usage, flood risks, isolation distances from industrial zones, hospitals, and waste facilities.', N'Before designating a land parcel for VietGAP production, farm enterprises must investigate historical land usage for at least the preceding 2 years to identify persistent chemical residues or endemic soil-borne pathogens.

Production zones must maintain adequate separation buffers from industrial parks, brick kilns, cemeteries, hospitals, and untreated municipal wastewater outfalls. Where contamination risks exist along perimeters, farms are mandated to establish perimeter dykes, independent drainage canals, and multi-layered agro-forestry vegetative shelterbelts (biological buffer zones).', 'https://images.unsplash.com/photo-1697897400733-46e12551a012?auto=format&fit=crop&w=800&q=80', 'soil-water', N'production zone, site survey, buffer zones', 640, 'https://vietgap.gov.vn', 'published', DATEADD(DAY, -9, SYSUTCDATETIME())),
(8, 1, N'Sustainable Soil Management and Substrate Remediation Techniques', 'quan-ly-va-cai-tao-dat-trong-theo-huong-ben-vung', N'Leguminous crop rotation practices, mulching for moisture preservation, Trichoderma bio-inoculants, and microbial composts to combat soil degradation.', N'To preserve natural soil fertility and maintain porous, crumbly soil structure, farm operators should practice rotational cropping with leguminous green manures while minimizing aggressive deep tillage that disrupts beneficial mycorrhizal networks.

Following each harvest cycle, complete thorough field sanitation, apply agricultural lime to neutralize acidity and sanitize soil, and incorporate fermented straw treated with Trichoderma fungal biocontrols. Conduct soil chemistry tests every 6-12 months for pH, electrical conductivity (EC), and organic matter percentage to calibrate fertilizer formulations accurately.', 'https://images.unsplash.com/photo-1586771107445-d3ca888129ff?auto=format&fit=crop&w=800&q=80', 'soil-water', N'soil management, sustainable farming, Trichoderma', 780, 'https://vietgap.gov.vn', 'published', DATEADD(DAY, -11, SYSUTCDATETIME())),
(9, 1, N'Irrigation Water Quality Standards in VietGAP Agriculture', 'tieu-chuan-chat-luong-nuoc-tuoi-trong-vietgap', N'Permissible threshold limits for microbial contamination (E. coli, Coliforms) and heavy metals (As, Cd, Pb, Hg) in agricultural water sources.', N'Water utilized for crop irrigation, foliar fertigation, and chemical spraying must comply with national technical regulations governing surface water and groundwater for agricultural use (specifically monitoring 4 heavy metals: Arsenic, Cadmium, Lead, and Mercury alongside fecal coliform counts).

Under no circumstances may untreated municipal sewage, animal manure slurry, or industrial effluent be channeled into crop irrigation reservoirs.', 'https://images.unsplash.com/photo-1752775312083-1cefe2f93358?auto=format&fit=crop&w=800&q=80', 'soil-water', N'irrigation water, testing standards, heavy metals', 540, 'https://vietgap.gov.vn', 'published', DATEADD(DAY, -13, SYSUTCDATETIME())),
(10, 1, N'Proper Management and Disposal of Agricultural Waste and Packaging', 'quan-ly-va-xu-ly-chat-thai-nong-nghiep-dung-cach', N'Triple-rinse procedures for empty agrochemical containers, dedicated locked storage pits, and safe composting protocols for vegetative residues.', N'Empty chemical containers and bottles must undergo the mandatory triple-rinse procedure immediately after preparation (with rinse water poured directly back into the sprayer tank), followed by prompt deposit into dedicated, locked collection containment pits fitted with rainproof roofs and hazard warning signs.

Burning or burying chemical packaging on farm grounds or discarding containers into irrigation ditches is strictly prohibited. Organic vegetative residues should be transferred to dedicated composting areas situated far from water sources to be recycled into organic mulch.', 'https://images.unsplash.com/photo-1742071853823-f9f87d9f3894?auto=format&fit=crop&w=800&q=80', 'soil-water', N'agricultural waste, environmental safety, chemical containers', 490, 'https://vietgap.gov.vn', 'published', DATEADD(DAY, -15, SYSUTCDATETIME())),
(11, 1, N'Selecting Certified Seed Stocks and Seedlings under VietGAP', 'chon-giong-cay-trong-dat-chuan-vietgap', N'Mandatory procurement invoices, varietal certification labels, seed lot provenance records, and pre-sowing seed sanitization protocols.', N'All crop cultivars, certified hybrid seed stocks, and rootstocks integrated into VietGAP production must be registered on the official national registry of approved crop varieties permitted for commercial distribution in Vietnam.

Farm enterprises must systematically archive commercial invoices, seed packets, and technical labels documenting lot numbers, germination test percentages, production dates, and expiration intervals.', 'https://images.unsplash.com/photo-1781782411066-c68b1e5610bc?auto=format&fit=crop&w=800&q=80', 'seed-cultivation', N'certified seed, varietal origin, seed lot provenance', 705, 'https://vietgap.gov.vn', 'published', DATEADD(DAY, -16, SYSUTCDATETIME())),
(12, 1, N'Seasonal Planting Techniques and Optimal Canopy Density', 'ky-thuat-gieo-trong-va-mat-do-hop-ly-theo-mua-vu', N'Designing raised planting beds oriented along sunlight vectors, calculating plant-to-row spacing, and enhancing canopy aeration against fungal diseases.', N'Plant spacing and population density must be calibrated to the growth habits of each crop cultivar, seasonal weather patterns, and substrate fertility levels.

Constructing raised beds 20-30 cm high during the rainy season promotes rapid surface drainage and prevents crown root rot. Maintaining adequate canopy spacing ensures uniform light penetration across all leaf layers, lowering local relative humidity and dramatically curbing downy mildew.', 'https://images.unsplash.com/photo-1773994788915-7fb0f462f223?auto=format&fit=crop&w=800&q=80', 'seed-cultivation', N'planting season, spacing density, raised beds', 530, 'https://vietgap.gov.vn', 'published', DATEADD(DAY, -18, SYSUTCDATETIME())),
(13, 1, N'Nursery Management Protocols and Vigorous Seedling Production', 'ky-thuat-quan-ly-vuon-uom-va-cham-soc-cay-con', N'Preparing sterilized germination media, managing light and moisture regimes, and hardening off seedlings prior to field transplanting.', N'Vigorous, disease-free seedlings determine over 50% of eventual crop harvest success. Germination media (desalted coco peat, carbonized rice hulls, and vermicompost) must be sterile and tested free of damping-off pathogens and root-knot nematodes.

Nursery facilities must be enclosed with ultra-fine insect netting and equipped with seedling benches elevated at least 50 cm above ground level.', 'https://images.unsplash.com/photo-1592982537447-7440770cbfc9?auto=format&fit=crop&w=800&q=80', 'seed-cultivation', N'nursery management, germination media, seedlings', 625, 'https://vietgap.gov.vn', 'published', DATEADD(DAY, -19, SYSUTCDATETIME())),
(14, 1, N'Canopy Pruning, Trellising, and Fruit Netting Techniques', 'ky-thuat-tia-canh-tao-tan-va-bao-trai', N'Timing suckering and pruning operations, fruit bagging methodologies to safeguard appearance, and eliminating fruit flies without chemical sprays.', N'For fruit trees and fruiting vegetables, systematic removal of side shoots, lateral suckers, and senescent bottom foliage channels assimilates directly to developing flowers and fruit clusters.

Pruning shears must be disinfected with 70% alcohol when transitioning between plants showing disease symptoms and healthy specimens. Combining pruning with protective micro-perforated fruit bags shields produce against fruit flies without chemical applications.', 'https://images.unsplash.com/photo-1530836369250-ef72a3f5cda8?auto=format&fit=crop&w=800&q=80', 'seed-cultivation', N'canopy pruning, fruit bagging, fruit crops', 710, 'https://vietgap.gov.vn', 'published', DATEADD(DAY, -20, SYSUTCDATETIME())),
(15, 1, N'The "4 Rights" Principle in Responsible Agrochemical Usage', 'nguyen-tac-4-dung-khi-su-dung-thuoc-bao-ve-thuc-vat', N'Right product, right dosage, right timing, and right method to effectively control pests, prevent resistance, and guarantee food hygiene.', N'The "4 Rights" principle serves as the foundational cornerstone for crop protection under VietGAP:
1. Right Product: Utilize only registered products officially permitted for agriculture; prioritize botanical and biological controls.
2. Right Dosage: Formulate exact dilution concentrations in strict accordance with manufacturer packaging labels.
3. Right Timing: Apply sprays when pest larvae are in early instars or when fungal lesions first appear.
4. Right Method: Spray uniformly across the abaxial (underside) surfaces of leaves where insects conceal themselves, avoiding drift against wind vectors.', 'https://images.unsplash.com/photo-1625246333195-78d9c38ad449?auto=format&fit=crop&w=800&q=80', 'fertilizer-pesticide', N'crop protection, 4 rights, chemical safety', 1120, 'https://vietgap.gov.vn', 'published', DATEADD(DAY, -21, SYSUTCDATETIME())),
(16, 1, N'Pre-Harvest Interval (PHI) Compliance Prior to Fruit Picking', 'thoi-gian-cach-ly-thuoc-bvtv-truoc-khi-thu-hoach', N'Interpreting PHI specifications on agrochemical packaging, calculating safe harvest dates, and leveraging automated safeguards in MyCropDiary.', N'The Pre-Harvest Interval (PHI) designates the mandatory minimum duration that must elapse from the final chemical application or foliar spray until crops may be harvested for human consumption.

When operators record a pesticide application in the MyCropDiary system, automated compliance algorithms immediately lock harvest authorization on the affected parcel until the PHI window safely clears, preventing Maximum Residue Limit (MRL) violations.', 'https://images.unsplash.com/photo-1595974482597-4b8da8879bc5?auto=format&fit=crop&w=800&q=80', 'fertilizer-pesticide', N'pre-harvest interval, PHI, residue limits', 980, 'https://vietgap.gov.vn', 'published', DATEADD(DAY, -22, SYSUTCDATETIME())),
(17, 1, N'Balanced Application of Organic and Inorganic Crop Nutrition', 'su-dung-phan-bon-huu-co-va-vo-co-can-doi', N'Strict prohibition of raw manure, thermophilic aerobic composting protocols, and balanced NPK micronutrient delivery by phenological stage.', N'Under VietGAP standards, the direct application of raw nightsoil, fresh livestock manure, or untreated sewer sludge is strictly prohibited due to severe risks of Salmonella and pathogenic E. coli contamination.

Animal manures must undergo complete thermophilic composting (reaching pile temperatures of 55-65°C for a minimum of 30-45 days) and be accompanied by aerobic composting temperature logs.', 'https://images.unsplash.com/photo-1729009511263-329748943004?auto=format&fit=crop&w=800&q=80', 'fertilizer-pesticide', N'fertilizers, aerobic composting, crop nutrition', 645, 'https://vietgap.gov.vn', 'published', DATEADD(DAY, -23, SYSUTCDATETIME())),
(18, 1, N'Integrated Pest Management (IPM) to Reduce Chemical Dependency', 'quan-ly-dich-hai-tong-hop-ipm-giam-phu-thuoc-hoa-chat', N'Deploying chromatic yellow/blue sticky traps, pheromone lures, beneficial predatory insects, and bio-insecticides (Bt, Beauveria, Metarhizium).', N'Integrated Pest Management (IPM) is the bedrock of modern VietGAP farming, prioritizing preventative agro-ecological cultural measures prior to synthetic chemical interventions: crop rotation, resistant varieties, yellow and blue adhesive monitoring cards, sex pheromone traps, and the conservation of natural parasitoid wasps.', 'https://images.unsplash.com/photo-1416879595882-3373a0480b5b?auto=format&fit=crop&w=800&q=80', 'fertilizer-pesticide', N'IPM, biological control, insect traps', 815, 'https://vietgap.gov.vn', 'published', DATEADD(DAY, -24, SYSUTCDATETIME())),
(19, 1, N'Harvesting Techniques to Minimize Post-Harvest Losses', 'ky-thuat-thu-hoach-giam-ton-that-sau-thu-hoach', N'Selecting optimal diurnal harvesting windows, sanitary pedicel cutting protocols, and rapid pre-cooling to extend commercial produce shelf life.', N'Crops may only be harvested after all chemical Pre-Harvest Intervals (PHI) have fully elapsed and produce has achieved optimal physiological and commercial maturity.

Harvest operations should take place in early morning or late afternoon cool hours. Use sanitized cutting shears and place fruit gently into foam-lined plastic crates, ensuring harvested produce never comes into contact with bare soil.', 'https://images.unsplash.com/photo-1746014929708-fcb859fd3185?auto=format&fit=crop&w=800&q=80', 'harvest-post-harvest', N'harvesting, post-harvest handling, shelf life', 610, 'https://vietgap.gov.vn', 'published', DATEADD(DAY, -25, SYSUTCDATETIME())),
(20, 1, N'Sanitation Standards for Post-Harvest Handling and Sorting Facilities', 've-sinh-dung-cu-va-khu-vuc-so-che-nong-san', N'Unidirectional packhouse floor plans, physical segregation of produce crates from waste bins, and scheduled chemical sanitization protocols.', N'Packhouses, grading areas, and packaging stations must be well-ventilated, roofed, and elevated with impervious drainage floors, equipped with exclusion barriers to deter rodents, domestic animals, and pests.

Crates designated for washed produce must be clearly color-coded and sanitized, strictly segregated from receptacles used for agricultural inputs or reject material.', 'https://images.unsplash.com/photo-1657288089316-c0350003ca49?auto=format&fit=crop&w=800&q=80', 'harvest-post-harvest', N'sanitation, post-harvest sorting, contamination prevention', 475, 'https://vietgap.gov.vn', 'published', DATEADD(DAY, -26, SYSUTCDATETIME())),
(21, 1, N'Packaging, Labeling, and Cold Chain Storage for VietGAP Produce', 'quy-chuan-dong-goi-ghi-nhan-va-bao-quan-lanh-nong-san', N'Mandatory information on agricultural packaging labels, cold room temperature and humidity setpoints by vegetable and fruit category.', N'Packaging materials must be fabricated from virgin, food-contact certified materials that are clean and undamaged. VietGAP packaging labels must clearly indicate: Product Name, Producer Name & Physical Address, VietGAP Certification Code, Harvest Batch Lot / Traceability QR Code, and Packaging Date.', 'https://images.unsplash.com/photo-1598170845058-32b9d6a5da37?auto=format&fit=crop&w=800&q=80', 'harvest-post-harvest', N'packaging, labeling, cold chain storage', 590, 'https://vietgap.gov.vn', 'published', DATEADD(DAY, -27, SYSUTCDATETIME())),
(22, 1, N'Regulations for Sanitary Produce Transport from Farm to Market', 'quy-dinh-van-chuyen-nong-san-an-toan-tu-trang-trai', N'Pre-loading cargo hold sanitation inspections, stacking guidelines, and strict prohibition of co-transporting produce with chemicals or livestock.', N'Vehicles chartered for agricultural transportation from farm gate to market distribution centers must be thoroughly washed and inspected prior to produce pallet loading.

Produce cargo compartments must never co-transport chemical fertilizers, agrochemical containers, petroleum fuels, or live animals under any circumstances.', 'https://images.unsplash.com/photo-1542838132-92c53300491e?auto=format&fit=crop&w=800&q=80', 'harvest-post-harvest', N'transportation, cold logistics, food safety', 430, 'https://vietgap.gov.vn', 'published', DATEADD(DAY, -28, SYSUTCDATETIME())),
(23, 1, N'Standardized VietGAP Farm Logbook Documentation System', 'he-thong-ghi-chep-nhat-ky-canh-tac-theo-vietgap', N'Compulsory operational registries: seed stock, fertilizer applications, crop protection sprays, irrigation events, yields, and customer feedback.', N'VietGAP farm records serve as the legal documentation scrutinized during certification audits. Farms must preserve comprehensive records of input purchases, inventory stock ledgers, block-level cultivation activity logs, spray logs, and harvest distribution manifests for a minimum duration of 24 months.', 'https://images.unsplash.com/photo-1748742633127-941ebec208da?auto=format&fit=crop&w=800&q=80', 'record-traceability', N'recordkeeping, farming logs, VietGAP audit forms', 830, 'https://vietgap.gov.vn', 'published', DATEADD(DAY, -29, SYSUTCDATETIME())),
(24, 1, N'Planting Area Codes and Digital QR Traceability Systems', 'ma-so-vung-trong-va-truy-xuat-nguon-goc-dien-tu', N'Configuring production lot and batch codes linked to specific zones and blocks, generating consumer-facing VietGAP traceability QR codes.', N'Each batch of produce sold must be assigned an immutable Lot/Batch Code that directly traces back to the specific Production Zone, Block, and Crop Season.

When consumers scan the packaging QR code with a smartphone, the digital ledger displays certified farm details, geolocation maps, crop cultivar information, and chronological care records.', 'https://images.unsplash.com/photo-1775681150283-04970adf1b98?auto=format&fit=crop&w=800&q=80', 'record-traceability', N'planting area code, QR code, electronic traceability', 670, 'https://vietgap.gov.vn', 'published', DATEADD(DAY, -30, SYSUTCDATETIME())),
(25, 1, N'Recall Procedures for Compromised Produce and Incident Response', 'quy-trinh-thu-hoi-san-pham-khong-an-toan-va-xu-ly-su-co', N'Procedures for isolating compromised lots, notifying commercial buyers, conducting root-cause investigations, and recording corrective actions.', N'Under VietGAP standards, every commercial farm must possess written Standard Operating Procedures for initiating product recalls if food safety hazards are detected. Upon discovery of a non-conformance, farm management must halt distribution and cross-reference batch shipping records to notify consignees within 24 hours.', 'https://images.unsplash.com/photo-1551288049-bebda4e38f71?auto=format&fit=crop&w=800&q=80', 'record-traceability', N'product recall, incident management, audit compliance', 515, 'https://vietgap.gov.vn', 'published', DATEADD(DAY, -31, SYSUTCDATETIME())),
(26, 1, N'Digital Farm Logging: Mobile Workflows Replacing Paper Records', 'so-hoa-nhat-ky-nong-trai-tren-dien-thoai-thay-the-so-giay', N'Empowering field technicians to record activities on mobile devices, attaching photographic evidence, and exporting automated audit reports.', N'With the MyCropDiary digital farm management system, field technicians assigned to specific Zones and Blocks can update cultural operations directly on mobile devices alongside timestamped photos. Farm owners monitor operational workflows in real time and export standardized VietGAP audit compliance dossiers on demand.', 'https://images.unsplash.com/photo-1454165804606-c3d57bc86b40?auto=format&fit=crop&w=800&q=80', 'record-traceability', N'digital logbook, smart agriculture, MyCropDiary', 910, 'https://vietgap.gov.vn', 'published', DATEADD(DAY, -32, SYSUTCDATETIME())),
(27, 1, N'Worker Safety Training and Personal Protective Equipment (PPE)', 'dao-tao-va-trang-bi-bao-ho-cho-nguoi-lao-dong', N'Mandatory annual training requirements, certified PPE gear for chemical handlers, and emergency first-aid station setup across field blocks.', N'All farm operators directly engaged in VietGAP agricultural production must undergo certified occupational training at least once per year on safe handling procedures, chemical dilution, and emergency poison control protocols.

Personnel preparing or applying crop protection materials must wear approved PPE: nitrile chemical gloves, rubber boots, splash-proof goggles, and activated carbon respirators.', 'https://images.unsplash.com/photo-1781785186628-e1a04569776d?auto=format&fit=crop&w=800&q=80', 'training-certification', N'worker training, occupational safety, PPE gear', 560, 'https://vietgap.gov.vn', 'published', DATEADD(DAY, -33, SYSUTCDATETIME())),
(28, 1, N'Conducting Internal Audits Prior to Official VietGAP Certification', 'danh-gia-noi-bo-truoc-khi-dang-ky-chung-nhan', N'Methodology for inspecting Class A and Class B criteria, compiling physical evidence, and closing Non-Conformance Reports (NCRs).', N'Internal audits are a mandatory pre-condition conducted at least once annually prior to inviting accredited external certification bodies. Farm technical supervisors use the comprehensive VietGAP checklist to evaluate chemical storage facilities, composting areas, field warning signage, container disposal pits, and logbooks.', 'https://images.unsplash.com/photo-1738244969496-eeabfacec172?auto=format&fit=crop&w=800&q=80', 'training-certification', N'internal audit, checklist, certification assessment', 615, 'https://vietgap.gov.vn', 'published', DATEADD(DAY, -34, SYSUTCDATETIME())),
(29, 1, N'Design Standards for Chemical Storages and Farm Warning Signage', 'tieu-chuan-thiet-ke-kho-chua-phan-bon-thuoc-bvtv-va-bien-bao', N'Structural specifications for locked agrochemical storage, pallet elevation, containment curbing, dry sand spill kits, and warning signage.', N'Crop protection chemical warehouses must be physically isolated from living quarters and packing areas, equipped with secure locks, ventilation louvers, and dry sand spill containment kits. Liquid agrochemicals must be stored on bottom shelves, dry powders on upper tiers, with high-visibility warning signage installed across treated blocks.', 'https://images.unsplash.com/photo-1574943320219-553eb213f72d?auto=format&fit=crop&w=800&q=80', 'training-certification', N'chemical storage, warning signage, facility standards', 740, 'https://vietgap.gov.vn', 'published', DATEADD(DAY, -35, SYSUTCDATETIME())),
(30, 1, N'Best Practices for Sustaining VietGAP Certification Through Surveillance Audits', 'kinh-nghiem-duy-tri-chung-nhan-vietgap-qua-cac-ky-giam-sat', N'Common non-conformances that trigger certification suspension, and strategies for maintaining persistent daily digital logging habits.', N'While VietGAP certificates carry a maximum validity of 3 years, accredited certification bodies conduct periodic surveillance audits every 12 months. Maintaining daily digital logging on MyCropDiary ensures the farm is continuously prepared for unannounced inspections and scheduled surveillance visits.', 'https://images.unsplash.com/photo-1589923188900-85dae523342b?auto=format&fit=crop&w=800&q=80', 'training-certification', N'certification maintenance, surveillance audits, best practices', 685, 'https://vietgap.gov.vn', 'published', DATEADD(DAY, -36, SYSUTCDATETIME()));
SET IDENTITY_INSERT [KnowledgeArticle] OFF;
GO

-- 44. AI CONVERSATIONS (ID: 1)
SET IDENTITY_INSERT [AIConversation] ON;
INSERT INTO [AIConversation] (ai_conversation_id, user_id, farm_id, block_id, crop_season_id, conversation_title, conversation_status)
VALUES
(1, 2, 1, 1, 1, N'Leaf edge yellowing consultation on 30-day-old melon plants', 'open');
SET IDENTITY_INSERT [AIConversation] OFF;
GO

-- 45. AI MESSAGES (IDs: 1 to 2)
SET IDENTITY_INSERT [AIMessage] ON;
INSERT INTO [AIMessage] (ai_message_id, ai_conversation_id, sender_type, message_text, model_name)
VALUES
(1, 1, 'user', N'My 30-day-old cantaloupe plants in Block A1 show interveinal yellowing with yellow leaf margins on the lower mature leaves, while the main leaf veins remain green. What is this deficiency and how should I treat it?', 'gemini-1.5-pro'),
(2, 1, 'assistant', N'Hello! Based on the symptom description—interveinal chlorosis and yellowing margins on mature lower leaves while primary veins stay dark green—this is a classic symptom of Magnesium (Mg) deficiency or an antagonistic Potassium/Magnesium (K/Mg) imbalance in your nutrient solution.

Recommended Corrective Actions:
1. Verify the root zone substrate pH: the ideal range is 5.8 - 6.2 (if pH drops below 5.5, Magnesium uptake is severely inhibited).
2. Foliar application: Dissolve Magnesium Sulfate (MgSO4) heptahydrate at 1.0 g/L and apply a fine mist spray across both sides of the canopy in early morning.
3. Review your fertigation injection ratio: Ensure Potassium (K) levels in the stock solution are not excessively high, as excessive K outcompetes Magnesium absorption.', 'gemini-1.5-pro');
SET IDENTITY_INSERT [AIMessage] OFF;
GO

-- 46. AI FEEDBACK (ID: 1)
SET IDENTITY_INSERT [AIFeedback] ON;
INSERT INTO [AIFeedback] (ai_feedback_id, ai_message_id, user_id, rating, feedback_text)
VALUES
(1, 2, 2, 5, N'Accurate and scientifically sound diagnosis. Within 3 days of supplemental MgSO4 foliar treatment, healthy green canopy vigor was fully restored.');
SET IDENTITY_INSERT [AIFeedback] OFF;
GO

-- 47. AI FEEDBACK REPLIES (ID: 1)
SET IDENTITY_INSERT [AIFeedbackReply] ON;
INSERT INTO [AIFeedbackReply] (ai_feedback_reply_id, ai_feedback_id, user_id, reply_text)
VALUES
(1, 1, 1, N'The MyCropDiary agronomy team is glad to have assisted you. Wishing your farm an abundant and high-yielding harvest!');
SET IDENTITY_INSERT [AIFeedbackReply] OFF;
GO

SELECT N'MyCropDiary database schema and seed data created successfully.' AS Result;
GO
