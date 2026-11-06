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

CREATE TABLE [Notification] (
    notification_id INT IDENTITY(1,1) PRIMARY KEY,
    user_id INT NOT NULL REFERENCES [User](user_id) ON DELETE CASCADE,
    title NVARCHAR(250) NOT NULL,
    message NVARCHAR(MAX) NOT NULL,
    notification_type NVARCHAR(50) NOT NULL DEFAULT 'SYSTEM',
    is_read BIT NOT NULL DEFAULT 0,
    action_url NVARCHAR(500),
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
CREATE INDEX idx_notifications_user_recent ON [Notification](user_id, is_read, created_at DESC);
CREATE INDEX idx_articles_status_published ON [KnowledgeArticle](article_status, published_at DESC);
GO

-- ============================================================================
-- MyCropDiary - Comprehensive English Sample Data (Seed Script)
-- Target: Microsoft SQL Server 2019+
-- Database: MyCropDiary
-- 
-- Default Password for all demo accounts: Password@123
-- BCrypt Hash: $2a$10$8jIMVzui5SoBFkUR1udxQO3Nqp09gvVoXobsoScb/zlisb4JIieDC
--
-- INSTRUCTIONS FOR USE:
-- 1. Open SQL Server Management Studio (SSMS) or Azure Data Studio.
-- 2. Connect to your SQL Server instance.
-- 3. Run this script against the [MyCropDiary] database.
-- ============================================================================

USE [MyCropDiary];
GO

SET NOCOUNT ON;
PRINT N'--> Starting MyCropDiary English Sample Data Seeding...';

-- ============================================================================
-- STEP 1: SAFE CLEANUP IN REVERSE FOREIGN KEY DEPENDENCY ORDER
-- ============================================================================
PRINT N'--> Cleaning up existing records...';

DELETE FROM [Notification];
DELETE FROM [AIFeedbackReply];
DELETE FROM [AIFeedback];
DELETE FROM [AIMessage];
DELETE FROM [AIConversation];
DELETE FROM [KnowledgeArticle];
DELETE FROM [InternalAssessmentItem];
DELETE FROM [InternalAssessment];
DELETE FROM [AssessmentCriterion];
DELETE FROM [TrainingAttendee];
DELETE FROM [TrainingRecord];
DELETE FROM [ProductionReport];
DELETE FROM [ChecklistResult];
DELETE FROM [ChecklistRun];
DELETE FROM [ChecklistRule];
DELETE FROM [ProductSale];
DELETE FROM [HarvestRecord];
DELETE FROM [Expense];
DELETE FROM [InventoryTransaction];
DELETE FROM [MaterialUsage];
DELETE FROM [InputPurchaseItem];
DELETE FROM [InputPurchase];
DELETE FROM [Inventory];
DELETE FROM [Warehouse];
DELETE FROM [Material];
DELETE FROM [Supplier];
DELETE FROM [BlockConditionObservation];
DELETE FROM [SoilAssessment];
DELETE FROM [WaterAssessment];
DELETE FROM [WaterSource];
DELETE FROM [ZoneRiskAssessment];
DELETE FROM [ActivitySupervisor];
DELETE FROM [ActivityWorker];
DELETE FROM [FarmingActivity];
DELETE FROM [TaskAssignment];
DELETE FROM [FarmTask];
DELETE FROM [StaffZoneAssignment];
DELETE FROM [CropSeasonBlock];
DELETE FROM [Block];
DELETE FROM [Zone];
DELETE FROM [CropSeason];
DELETE FROM [CropCategory];
DELETE FROM [FarmWorker];
DELETE FROM [FarmMember];
DELETE FROM [FarmRegistration];
DELETE FROM [Farm];
DELETE FROM [AccountToken];
DELETE FROM [User];
GO

-- ============================================================================
-- 1. ACCOUNTS & USERS (9 Demo Users)
-- Default Login: Password@123
-- ============================================================================
PRINT N'--> Seeding Users...';
SET IDENTITY_INSERT [User] ON;
INSERT INTO [User] (user_id, full_name, email, phone, password_hash, avatar_url, system_role, account_status, email_verified_at, created_at, updated_at)
VALUES 
(1, N'System Administrator', 'admin@cropdiary.com', '0901234567', '$2a$10$8jIMVzui5SoBFkUR1udxQO3Nqp09gvVoXobsoScb/zlisb4JIieDC', 'https://images.unsplash.com/photo-1534528741775-53994a69daeb', 'admin', 'active', SYSUTCDATETIME(), SYSUTCDATETIME(), SYSUTCDATETIME()),
(2, N'Michael Green (Farm Owner)', 'owner1@greenfarm.com', '0912345678', '$2a$10$8jIMVzui5SoBFkUR1udxQO3Nqp09gvVoXobsoScb/zlisb4JIieDC', 'https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d', 'user', 'active', SYSUTCDATETIME(), SYSUTCDATETIME(), SYSUTCDATETIME()),
(3, N'Sarah Jenkins (Eco Orchard Owner)', 'owner2@highlandfarm.com', '0923456789', '$2a$10$8jIMVzui5SoBFkUR1udxQO3Nqp09gvVoXobsoScb/zlisb4JIieDC', 'https://images.unsplash.com/photo-1544005313-94ddf0286df2', 'user', 'active', SYSUTCDATETIME(), SYSUTCDATETIME(), SYSUTCDATETIME()),
(4, N'David Nguyen (Senior Agronomist)', 'staff1@greenfarm.com', '0934567890', '$2a$10$8jIMVzui5SoBFkUR1udxQO3Nqp09gvVoXobsoScb/zlisb4JIieDC', 'https://images.unsplash.com/photo-1500648767791-00dcc994a43e', 'user', 'active', SYSUTCDATETIME(), SYSUTCDATETIME(), SYSUTCDATETIME()),
(5, N'Emily Tran (Field Operations Supervisor)', 'staff2@greenfarm.com', '0945678901', '$2a$10$8jIMVzui5SoBFkUR1udxQO3Nqp09gvVoXobsoScb/zlisb4JIieDC', 'https://images.unsplash.com/photo-1494790108377-be9c29b29330', 'user', 'active', SYSUTCDATETIME(), SYSUTCDATETIME(), SYSUTCDATETIME()),
(6, N'Alex Turner (Pending Verification)', 'pending.user@cropdiary.com', '0956789012', '$2a$10$8jIMVzui5SoBFkUR1udxQO3Nqp09gvVoXobsoScb/zlisb4JIieDC', NULL, 'user', 'pending', NULL, SYSUTCDATETIME(), SYSUTCDATETIME()),
(7, N'John Carter (Greenhouse Technician)', 'worker1@greenfarm.com', '0967890123', '$2a$10$8jIMVzui5SoBFkUR1udxQO3Nqp09gvVoXobsoScb/zlisb4JIieDC', 'https://images.unsplash.com/photo-1522075469751-3a6694fb2f61', 'user', 'active', SYSUTCDATETIME(), SYSUTCDATETIME(), SYSUTCDATETIME()),
(8, N'Maria Lopez (Crop Care Specialist)', 'worker2@greenfarm.com', '0978901234', '$2a$10$8jIMVzui5SoBFkUR1udxQO3Nqp09gvVoXobsoScb/zlisb4JIieDC', 'https://images.unsplash.com/photo-1517841905240-472988babdf9', 'user', 'active', SYSUTCDATETIME(), SYSUTCDATETIME(), SYSUTCDATETIME()),
(9, N'Robert Chen (Applicant Farm Owner)', 'register.farm@cropdiary.com', '0989012345', '$2a$10$8jIMVzui5SoBFkUR1udxQO3Nqp09gvVoXobsoScb/zlisb4JIieDC', NULL, 'user', 'active', SYSUTCDATETIME(), SYSUTCDATETIME(), SYSUTCDATETIME());
SET IDENTITY_INSERT [User] OFF;
GO

-- ============================================================================
-- 2. ACCOUNT TOKENS
-- ============================================================================
PRINT N'--> Seeding Account Tokens...';
SET IDENTITY_INSERT [AccountToken] ON;
INSERT INTO [AccountToken] (token_id, user_id, token_hash, token_type, expires_at)
VALUES
(1, 6, '$2a$10$0pPbCigvJwUsf6/sskJIe.SwZ2mNtfIFaXiXTCF1kCjTwmXi6ghqO', 'email_verification', '2026-12-31 23:59:59'),
(2, 4, 'token-hash-refresh-staff1-2026', 'refresh', '2026-12-31 23:59:59');
SET IDENTITY_INSERT [AccountToken] OFF;
GO

-- ============================================================================
-- 3. FARMS (3 Production Farms)
-- ============================================================================
PRINT N'--> Seeding Farms...';
SET IDENTITY_INSERT [Farm] ON;
INSERT INTO [Farm] (farm_id, farm_name, description, address, latitude, longitude, cover_image_url, created_by, farm_status)
VALUES
(1, N'Green Valley High-Tech Farm', N'High-tech VietGAP melon and greenhouse specialty crop farm with automated fertigation', N'123 Pine Hill Road, Ward 8, Da Lat City, Lam Dong', 11.954500, 108.445200, 'https://images.unsplash.com/photo-1500937386664-56d1dfef3854', 2, 'active'),
(2, N'Highland Eco Orchard & Cooperative', N'Export-grade tropical fruit cooperative operating under GlobalGAP & VietGAP certification', N'45 Mango Grove Avenue, My Xuong, Cao Lanh District, Dong Thap', 10.450100, 105.632200, 'https://images.unsplash.com/photo-1592982537447-7440770cbfc9', 3, 'active'),
(3, N'Sunrise Organic Vegetable Farm', N'Ecological organic vegetable enterprise producing temperate root and leafy crops', N'Yen Bai Valley, Ba Vi District, Hanoi', 21.054300, 105.352100, NULL, 1, 'inactive');
SET IDENTITY_INSERT [Farm] OFF;
GO

-- ============================================================================
-- 4. FARM REGISTRATIONS
-- ============================================================================
PRINT N'--> Seeding Farm Registrations...';
SET IDENTITY_INSERT [FarmRegistration] ON;
INSERT INTO [FarmRegistration] (registration_id, user_id, farm_name, address, province, district, ward, document_url, registration_note, registration_status, reviewed_by, reviewed_at, resulting_farm_id)
VALUES
(1, 2, N'Green Valley High-Tech Farm', N'123 Pine Hill Road', N'Lam Dong', N'Da Lat City', N'Ward 8', 'https://example.com/docs/vietgap-greenvalley.pdf', N'Valid registration with complete VietGAP certificate and land entitlement attached.', 'approved', 1, '2025-01-02 10:00:00', 1),
(2, 3, N'Highland Eco Orchard & Cooperative', N'45 Mango Grove Avenue', N'Dong Thap', N'Cao Lanh District', N'My Xuong Commune', 'https://example.com/docs/globalgap-highland.pdf', N'Comprehensive cooperative application fully meeting export safety standards.', 'approved', 1, '2025-02-15 14:30:00', 2),
(3, 9, N'Central Highlands Agro Farm', N'Hamlet 4', N'Dak Lak', N'Buon Ma Thuot City', N'Ea Kao Commune', 'https://example.com/docs/daklak-agro.pdf', N'Clean specialty coffee and durian farm application, pending admin document audit.', 'pending', NULL, NULL, NULL),
(4, 9, N'Kon Tum Medicinal Herb Garden', N'Mang Canh Village', N'Kon Tum', N'Kon Plong District', N'Mang Canh Commune', NULL, N'Application incomplete: Missing soil test audit and land tenure verification.', 'rejected', 1, '2025-03-01 09:15:00', NULL);
SET IDENTITY_INSERT [FarmRegistration] OFF;
GO

-- ============================================================================
-- 5. FARM MEMBERSHIP
-- ============================================================================
PRINT N'--> Seeding Farm Members...';
SET IDENTITY_INSERT [FarmMember] ON;
INSERT INTO [FarmMember] (farm_member_id, farm_id, user_id, member_role, membership_status)
VALUES
(1, 1, 2, 'owner', 'active'),
(2, 1, 4, 'manager', 'active'),
(3, 1, 5, 'member', 'active'),
(4, 2, 3, 'owner', 'active'),
(5, 1, 7, 'member', 'active'),
(6, 1, 8, 'member', 'active');
SET IDENTITY_INSERT [FarmMember] OFF;
GO

-- ============================================================================
-- 6. FARM WORKERS (CRUD Worker Records UC-15)
-- ============================================================================
PRINT N'--> Seeding Farm Workers...';
SET IDENTITY_INSERT [FarmWorker] ON;
INSERT INTO [FarmWorker] (farm_worker_id, farm_member_id, worker_code, job_title, employment_status, hired_at, notes)
VALUES
(1, 2, 'WK-GV-001', N'Senior Agronomist & Fertigation Engineer', 'active', '2025-01-01', N'Responsible for A+B hydroponic stock solution formula, EC/pH targets, and fertigation schedules.'),
(2, 5, 'WK-GV-002', N'Greenhouse Automation Technician', 'active', '2025-02-15', N'Specializes in climate sensors, irrigation solenoids, misting lines, and nutrient injection pumps.'),
(3, 6, 'WK-GV-003', N'Crop Training & Pollination Specialist', 'active', '2025-03-01', N'Skilled in lateral shoot pruning, trellis wire training, hand pollination, and fruit thinning.'),
(4, 3, 'WK-GV-004', N'Quality Control & Post-Harvest Lead', 'active', '2025-01-10', N'Oversees VietGAP field harvesting standards, sorting tolerances, brix testing, and cold storage.');
SET IDENTITY_INSERT [FarmWorker] OFF;
GO

-- ============================================================================
-- 7. CROP CATEGORIES (20 VietGAP Commercial Crops)
-- ============================================================================
PRINT N'--> Seeding Crop Categories...';
SET IDENTITY_INSERT [CropCategory] ON;
INSERT INTO [CropCategory] (crop_category_id, category_name, description, image_url)
VALUES
(1, N'Cantaloupe Melon', N'High-tech Japanese hybrid orange-flesh cantaloupe melon (85-95 days cycle)', 'https://images.unsplash.com/photo-1598170845058-32b9d6a5da37'),
(2, N'Bell Pepper', N'Greenhouse sweet blocky red, yellow, and orange bell peppers (100-120 days cycle)', 'https://images.unsplash.com/photo-1563565375-f3fdfdbefa83'),
(3, N'Cherry Tomato', N'High-brix sweet indeterminate truss cherry tomatoes for fresh retail (90-110 days cycle)', 'https://images.unsplash.com/photo-1592924357228-91a4daadcfea'),
(4, N'Tropical Mango', N'Premium export-grade Cat Chu and aromatic mangoes (Perennial fruit crop)', 'https://images.unsplash.com/photo-1553279768-865429fa0078'),
(5, N'Water Spinach', N'Fast-growing aquatic/semi-aquatic leafy vegetable (25-30 days cycle)', 'https://images.unsplash.com/photo-1540420773420-3366772f4999'),
(6, N'Butterhead Lettuce', N'Hydroponic delicate tender leafy lettuce with loose heads (35-45 days cycle)', 'https://images.unsplash.com/photo-1556801712-76c8eb07bbc9'),
(7, N'Bok Choy', N'Nutrient-dense Asian brassica green with crisp white stems (30-40 days cycle)', 'https://images.unsplash.com/photo-1594282486552-05b4d80fbb9f'),
(8, N'Baby Spinach', N'Cool-season leafy green rich in iron and folates (35-45 days cycle)', 'https://images.unsplash.com/photo-1576045057995-568f588f82fb'),
(9, N'Choy Sum', N'Popular tender Asian flowering green vegetable (28-35 days cycle)', 'https://images.unsplash.com/photo-1540420773420-3366772f4999'),
(10, N'Japanese Cucumber', N'Thin-skinned sweet parthenocarpic trellised slicing cucumber (45-60 days cycle)', 'https://images.unsplash.com/photo-1604977042946-1eecc30f269e'),
(11, N'French Green Beans', N'Slender tender bush string beans with high nutritional value (50-60 days cycle)', 'https://images.unsplash.com/photo-1551462147-37885acc36f1'),
(12, N'Tender Okra', N'Heat-loving ribbed green pods rich in dietary fiber (45-55 days cycle)', 'https://images.unsplash.com/photo-1425543103986-22abb7d7e8d2'),
(13, N'Baby Carrot', N'Sweet crisp root vegetable with uniform cylindrical taper (65-75 days cycle)', 'https://images.unsplash.com/photo-1598170845058-32b9d6a5da37'),
(14, N'White Daikon Radish', N'High-moisture crunchy Asian taproot vegetable (40-50 days cycle)', 'https://images.unsplash.com/photo-1590779033100-9f60a05a013d'),
(15, N'Scallion & Fresh Herbs', N'Aromatic culinary alliums and fresh table herbs (30-40 days continuous cycle)', 'https://images.unsplash.com/photo-1615485290382-441e4d049cb5'),
(16, N'Malabar Spinach', N'Heat-tolerant climbing leafy green with succulent leaves (35-45 days cycle)', 'https://images.unsplash.com/photo-1540420773420-3366772f4999'),
(17, N'Savoy Cabbage', N'Wrinkled dark green leaves with sweet mild flavor (70-80 days cycle)', 'https://images.unsplash.com/photo-1550828520-4cb496926fc9'),
(18, N'Crimson Watermelon', N'Crisp sweet red-flesh melon with striped rind (65-75 days cycle)', 'https://images.unsplash.com/photo-1587049352846-4a222e784d38'),
(19, N'Wax Winter Gourd', N'Mild trellised storage gourd with waxy green skin (60-70 days cycle)', 'https://images.unsplash.com/photo-1570715621210-b74704bdfa57'),
(20, N'Golden Sweet Corn', N'Tender high-sugar golden yellow corn kernels (75-85 days cycle)', 'https://images.unsplash.com/photo-1551754655-cd27e38d2076');
SET IDENTITY_INSERT [CropCategory] OFF;
GO

-- ============================================================================
-- 8. CROP SEASONS
-- ============================================================================
PRINT N'--> Seeding Crop Seasons...';
SET IDENTITY_INSERT [CropSeason] ON;
INSERT INTO [CropSeason] (crop_season_id, farm_id, crop_category_id, season_name, start_date, end_date, season_status, notes, created_by)
VALUES
(1, 1, 1, N'Spring 2026 Hydroponic Cantaloupe Season', '2026-01-15', '2026-04-30', 'active', N'Key commercial melon production cycle targeting premium holiday market demand.', 2),
(2, 1, 2, N'Winter 2025 Greenhouse Bell Pepper Season', '2025-09-01', '2025-12-25', 'completed', N'Fully harvested, graded, packaged, and settled with wholesale supermarket buyers.', 2),
(3, 1, 3, N'Summer 2026 Truss Tomato Season', '2026-05-01', '2026-08-30', 'planned', N'Planned indeterminate cherry tomato cultivation scheduled for Greenhouse Wing #2.', 2),
(4, 2, 4, N'Annual 2026 Export Mango Season', '2026-01-01', '2026-11-30', 'active', N'Cooperative Cat Chu mango season adhering strictly to GlobalGAP residue thresholds.', 3);
SET IDENTITY_INSERT [CropSeason] OFF;
GO

-- ============================================================================
-- 9. ZONES (Production Zones UC-16)
-- ============================================================================
PRINT N'--> Seeding Production Zones...';
SET IDENTITY_INSERT [Zone] ON;
INSERT INTO [Zone] (zone_id, farm_id, zone_name, description, area_value, area_unit)
VALUES
(1, 1, N'Zone A - Smart Greenhouse Complex', N'Multi-span automated polyhouse with climate screens, misting, and drip fertigation.', 5000.00, 'm2'),
(2, 1, N'Zone B - High-Tunnel Polyhouse Wing', N'High-tunnel ventilated greenhouse beds for specialty nightshades and peppers.', 6000.00, 'm2'),
(3, 1, N'Zone C - Certified Propagation Nursery', N'Sterile seedling nursery equipped with germination chambers and micro-misting.', 2000.00, 'm2'),
(4, 2, N'Zone 1 - Export Fruit Orchard Block', N'Mature grafted mango plantation with sub-canopy micro-sprinklers.', 15000.00, 'm2');
SET IDENTITY_INSERT [Zone] OFF;
GO

-- ============================================================================
-- 10. BLOCKS (Production Blocks UC-17)
-- ============================================================================
PRINT N'--> Seeding Production Blocks...';
SET IDENTITY_INSERT [Block] ON;
INSERT INTO [Block] (block_id, zone_id, block_name, description, area_value, area_unit, soil_type)
VALUES
(1, 1, N'Block A1', N'Hydroponic rows 1 to 20 planted with F1 Taka Japanese Cantaloupe.', 2500.00, 'm2', N'Desalinated coco peat grow bags'),
(2, 1, N'Block A2', N'Hydroponic rows 21 to 40 planted with Golden Honey Cantaloupe.', 2500.00, 'm2', N'Desalinated coco peat grow bags'),
(3, 2, N'Block B1', N'Soil-ridged beds cultivated with Sweet Red Bell Peppers.', 3000.00, 'm2', N'Rich volcanic basalt red loam'),
(4, 2, N'Block B2', N'Soil-ridged beds cultivated with Sweet Yellow Bell Peppers.', 3000.00, 'm2', N'Rich volcanic basalt red loam'),
(5, 3, N'Block C1', N'Grafted seedling bench trays and micro-sprinkler misting lines.', 2000.00, 'm2', N'Peat moss and biochar sterile plug mix'),
(6, 4, N'Block 1A', N'Cat Chu grafted trees aged 7 years, pruned for open canopy sunlight.', 15000.00, 'm2', N'Mekong alluvial river silt');
SET IDENTITY_INSERT [Block] OFF;
GO

-- ============================================================================
-- 11. CROP SEASON BLOCKS
-- ============================================================================
PRINT N'--> Seeding Crop Season Blocks...';
INSERT INTO [CropSeasonBlock] (crop_season_id, block_id, planted_area, area_unit, planted_at, expected_harvest_at)
VALUES
(1, 1, 2500.00, 'm2', '2026-01-15', '2026-04-25'),
(1, 2, 2500.00, 'm2', '2026-01-20', '2026-04-30'),
(2, 3, 3000.00, 'm2', '2025-09-01', '2025-12-20'),
(2, 4, 3000.00, 'm2', '2025-09-05', '2025-12-25'),
(4, 6, 15000.00, 'm2', '2026-01-05', '2026-06-15');
GO

-- ============================================================================
-- 12. STAFF ZONE ASSIGNMENTS (UC-35)
-- ============================================================================
PRINT N'--> Seeding Zone Assignments...';
SET IDENTITY_INSERT [StaffZoneAssignment] ON;
INSERT INTO [StaffZoneAssignment] (assignment_id, farm_member_id, zone_id, assigned_at, ended_at)
VALUES
(1, 2, 1, '2025-01-01 00:00:00', NULL),
(2, 3, 1, '2025-01-01 00:00:00', NULL),
(3, 5, 2, '2025-06-01 00:00:00', NULL);
SET IDENTITY_INSERT [StaffZoneAssignment] OFF;
GO

-- ============================================================================
-- 13. FARM TASKS (Farm Tasks UC-22)
-- ============================================================================
PRINT N'--> Seeding Farm Tasks...';
SET IDENTITY_INSERT [FarmTask] ON;
INSERT INTO [FarmTask] (farm_task_id, farm_id, crop_season_id, zone_id, block_id, task_name, description, task_type, priority, status, starts_at, due_at, created_by)
VALUES
(1, 1, 1, 1, 1, N'Substrate Conditioning & Buffering', N'Flush coco peat slabs with fresh water and buffer with calcium nitrate solution.', N'Land Prep', 'high', 'completed', '2026-01-10 07:00:00', '2026-01-14 17:00:00', 2),
(2, 1, 1, 1, 1, N'Melon Seedling Transplanting', N'Transfer 14-day-old nursery plug seedlings into production grow bags.', N'Planting', 'urgent', 'completed', '2026-01-15 06:30:00', '2026-01-16 11:30:00', 2),
(3, 1, 1, 1, 1, N'Calibrate Drip Fertigation Cycle #3', N'Inject concentrated A+B nutrient solution at target EC 1.8 mS/cm and pH 6.0.', N'Fertilization & Irrigation', 'normal', 'completed', '2026-02-10 08:00:00', '2026-02-10 11:00:00', 2),
(4, 1, 1, 1, 1, N'Manual Flower Pollination & Trellising', N'Hand pollinate female flowers at nodes 9-12 and clip vines to vertical trellis strings.', N'Crop Care', 'high', 'in_progress', '2026-02-25 07:00:00', '2026-03-05 10:00:00', 2),
(5, 1, 1, 1, 1, N'Preventive Organic Bio-Control Application', N'Apply cold-pressed Neem Chito botanical spray against early thrips and spider mites.', N'Plant Protection', 'normal', 'planned', '2026-03-08 16:00:00', '2026-03-08 18:00:00', 2),
(6, 1, 1, 1, 1, N'Peak Harvest & QR Traceability Tagging', N'Clip ripe melon fruit with T-stems, verify minimum 13 brix, and apply QR tags.', N'Harvest', 'urgent', 'planned', '2026-04-22 06:00:00', '2026-04-25 17:00:00', 2);
SET IDENTITY_INSERT [FarmTask] OFF;
GO

-- ============================================================================
-- 14. TASK ASSIGNMENTS
-- ============================================================================
PRINT N'--> Seeding Task Assignments...';
INSERT INTO [TaskAssignment] (farm_task_id, farm_worker_id, assigned_by, assigned_at, assignment_status)
VALUES
(3, 1, 2, '2026-02-09 15:00:00', 'completed'),
(4, 3, 2, '2026-02-24 16:30:00', 'accepted'),
(5, 2, 2, '2026-03-01 08:00:00', 'assigned');
GO

-- ============================================================================
-- 15. FARMING ACTIVITIES (Farming Activities UC-23)
-- ============================================================================
PRINT N'--> Seeding Farming Activities...';
SET IDENTITY_INSERT [FarmingActivity] ON;
INSERT INTO [FarmingActivity] (farming_activity_id, farm_id, farm_task_id, crop_season_id, zone_id, block_id, activity_name, activity_type, activity_notes, performed_at, image_url, created_by)
VALUES
(1, 1, 3, 1, 1, 1, N'Automated Drip Fertigation Run (A+B EC 1.8)', N'Fertilization & Irrigation', N'Dispensed 10 liters of A+B stock solution into 1,000L header tank. Uniform emitter flow verified.', '2026-02-10 08:30:00', NULL, 4),
(2, 1, 4, 1, 1, 1, N'Vine Trellising and Hand Pollination Shift', N'Crop Care', N'Successfully pollinated approximately 1,200 flowers along rows 1 to 10.', '2026-02-28 07:45:00', NULL, 4),
(3, 1, NULL, 1, 1, 1, N'Organic Neem Botanical Pest Treatment', N'Plant Protection', N'Applied 2 bottles of Neem Chito in late afternoon cool weather. No leaf burn observed.', '2026-03-01 16:30:00', NULL, 4),
(4, 1, NULL, 2, 2, 3, N'Winter 2025 Bell Pepper Final Harvest', N'Harvest', N'Harvested all red ripe fruit across Block B1. Crates sanitized prior to transport.', '2025-12-20 09:00:00', NULL, 4);
SET IDENTITY_INSERT [FarmingActivity] OFF;
GO

-- ============================================================================
-- 16. ACTIVITY WORKERS & SUPERVISORS
-- ============================================================================
PRINT N'--> Seeding Activity Workers & Supervisors...';
INSERT INTO [ActivityWorker] (farming_activity_id, farm_worker_id, participation_role)
VALUES
(1, 1, N'Nutrient dosing technician'),
(2, 3, N'Lead pollination specialist'),
(4, 2, N'Field fruit harvester'),
(4, 4, N'Grading and cold-chain inspector');

INSERT INTO [ActivitySupervisor] (farming_activity_id, farm_member_id)
VALUES
(1, 3),
(4, 3);
GO

-- ============================================================================
-- 17. ZONE RISK ASSESSMENTS
-- ============================================================================
PRINT N'--> Seeding Zone Risk Assessments...';
SET IDENTITY_INSERT [ZoneRiskAssessment] ON;
INSERT INTO [ZoneRiskAssessment] (zone_risk_assessment_id, zone_id, assessed_by, risk_type, risk_level, description, mitigation_plan, assessed_at)
VALUES
(1, 1, 3, N'High Relative Humidity & Powdery Mildew', 'medium', N'Nighttime polyhouse humidity reaches 92%, elevating risk of powdery and downy mildew sporulation.', N'Activate convection air circulation fans and open motorized roof vents starting at 05:30 AM.', '2026-01-20 10:00:00'),
(2, 2, 3, N'Heavy Monsoon Surface Runoff', 'low', N'Prolonged seasonal rains could erode perimeter raised beds and transport soil sediment.', N'Plant vetiver grass along embankment contours and install gravel perimeter drainage trenches.', '2026-01-22 14:00:00');
SET IDENTITY_INSERT [ZoneRiskAssessment] OFF;
GO

-- ============================================================================
-- 18. WATER SOURCES (Water Sources UC-20)
-- ============================================================================
PRINT N'--> Seeding Water Sources...';
SET IDENTITY_INSERT [WaterSource] ON;
INSERT INTO [WaterSource] (water_source_id, farm_id, source_name, source_type, description)
VALUES
(1, 1, N'Pine Valley Stream Settling Reservoir', N'Surface Water', N'2,500 m3 storage capacity equipped with sand media and automatic disc filtration.'),
(2, 1, N'Deep Aquifer Borehole Well #1', N'Groundwater', N'85-meter depth borehole supplying low-hardness mineral water optimal for hydroponics.');
SET IDENTITY_INSERT [WaterSource] OFF;
GO

-- ============================================================================
-- 19. WATER ASSESSMENTS (Water Testing UC-20)
-- ============================================================================
PRINT N'--> Seeding Water Assessments...';
SET IDENTITY_INSERT [WaterAssessment] ON;
INSERT INTO [WaterAssessment] (water_assessment_id, water_source_id, block_id, conducted_by, water_ph, temperature_c, quality_status, notes, assessed_at)
VALUES
(1, 1, 1, 2, 6.60, 20.50, N'VietGAP Irrigation Compliant', N'Heavy metal traces, arsenic, and E. coli counts tested well below maximum statutory thresholds.', '2026-01-12 08:30:00'),
(2, 2, 1, 2, 6.20, 21.00, N'Premium Hydroponic Quality', N'Clean groundwater with negligible bicarbonate buffer; ideal for precision hydroponic nutrient mixing.', '2026-02-15 09:00:00');
SET IDENTITY_INSERT [WaterAssessment] OFF;
GO

-- ============================================================================
-- 20. SOIL ASSESSMENTS (Soil Testing UC-19)
-- ============================================================================
PRINT N'--> Seeding Soil Assessments...';
SET IDENTITY_INSERT [SoilAssessment] ON;
INSERT INTO [SoilAssessment] (soil_assessment_id, block_id, conducted_by, soil_ph, moisture_percent, nutrient_data, notes, assessed_at)
VALUES
(1, 1, 2, 6.40, 72.50, N'{"EC_mS_cm": 1.82, "N_ppm": 140, "P_ppm": 45, "K_ppm": 210, "Ca_ppm": 160, "Mg_ppm": 55}', N'Coco peat substrate EC is stable; macro and micro mineral ratios are balanced for melon flowering.', '2026-02-18 10:15:00'),
(2, 3, 2, 5.90, 65.00, N'{"EC_mS_cm": 1.15, "N_ppm": 95, "P_ppm": 32, "K_ppm": 140, "Organic_matter_pct": 4.1}', N'Volcanic basalt soil is nutrient-rich; broadcast agricultural lime at 200 kg/ha before next planting.', '2026-02-20 11:00:00');
SET IDENTITY_INSERT [SoilAssessment] OFF;
GO

-- ============================================================================
-- 21. BLOCK CONDITION OBSERVATIONS
-- ============================================================================
PRINT N'--> Seeding Block Observations...';
SET IDENTITY_INSERT [BlockConditionObservation] ON;
INSERT INTO [BlockConditionObservation] (observation_id, block_id, recorded_by, condition_type, description, observed_at)
VALUES
(1, 1, 2, N'Crop Growth', N'Cantaloupe canopy at node 11 is thriving with dark green foliage and thick stems. No nutrient chlorosis detected.', '2026-02-26 08:00:00'),
(2, 3, 2, N'Pest Scouting', N'Inspection of 50 randomly sampled plants revealed no thrips or whiteflies on yellow sticky traps.', '2026-02-27 15:30:00');
SET IDENTITY_INSERT [BlockConditionObservation] OFF;
GO

-- ============================================================================
-- 22. SUPPLIERS (Suppliers UC-36)
-- ============================================================================
PRINT N'--> Seeding Suppliers...';
SET IDENTITY_INSERT [Supplier] ON;
INSERT INTO [Supplier] (supplier_id, farm_id, supplier_name, contact_name, phone, email, address, notes)
VALUES
(1, 1, N'Da Lat AgriChemical & BioSolutions', N'Robert King', '0988123456', 'sales@dalatagri.com', N'Plot 12, Loc Phat Industrial Park, Bao Loc, Lam Dong', N'Certified distributor of high-purity water-soluble fertilizers and organic bio-fungicides.'),
(2, 1, N'AgriBio Hydroponics International', N'Sarah Miller', '0977234567', 'orders@agribiohydro.com', N'54 Hung Vuong Avenue, Ward 9, Da Lat City', N'Exclusive supplier of GreenHydro chelated mineral solutions and coco peat grow slabs.'),
(3, 1, N'Pacific Seed Technologies Inc.', N'Kenji Sato', '0966345678', 'service@pacificseeds.com', N'88 Tran Hung Dao Boulevard, District 1, Ho Chi Minh City', N'Premier breeder of high-brix Japanese hybrid cantaloupe and parthenocarpic seeds.'),
(4, 1, N'EcoPack Export Packaging Supplies', N'Alan Green', '0955456789', 'logistics@ecopack.vn', N'22 Song Hanh Highway, Thu Duc City, Ho Chi Minh City', N'Manufacturer of 5-ply corrugated fruit cartons with customized VietGAP traceability labels.');
SET IDENTITY_INSERT [Supplier] OFF;
GO

-- ============================================================================
-- 23. MATERIALS
-- ============================================================================
PRINT N'--> Seeding Materials...';
SET IDENTITY_INSERT [Material] ON;
INSERT INTO [Material] (material_id, farm_id, material_name, material_type, unit, description, is_active)
VALUES
(1, 1, N'Haifa Poly-Feed NPK 20-20-20+ME', N'Soluble Fertilizer', 'bag', N'Fully water-soluble greenhouse NPK formula enriched with chelated trace elements (25kg Bag).', 1),
(2, 1, N'GreenHydro A+B Stock Solution', N'Liquid Nutrient', 'canister', N'Concentrated Part A (Calcium/Nitrate/Iron) & Part B (Phosphorus/Potassium/Magnesium) (5L Canister).', 1),
(3, 1, N'Neem Chito Botanical Insecticide', N'Biological Pest Control', 'bottle', N'Cold-pressed azadirachtin neem extract combined with chitosan bio-surfactant (1L Bottle).', 1),
(4, 1, N'Trichoderma Bio-Fungicide Inoculant', N'Biological Fungicide', 'pack', N'Beneficial antagonistic soil microbe formulation preventing damping-off and root rot (1kg Pack).', 1),
(5, 1, N'Japanese F1 Taka Cantaloupe Hybrid Seeds', N'Seeds', 'pack', N'Certified hybrid seed lot with 99% germination rate and 14-16 brix sweetness potential (1,000 Seeds).', 1),
(6, 1, N'Export Grade 5-Ply Corrugated Cartons', N'Packaging & Storage', 'box', N'Sturdy ventilated export cartons designed for 4-5 melons (approx 10kg capacity) with QR code print.', 1),
(7, 1, N'Compensating Drip Emitters 2.0 L/h', N'Irrigation Equipment', 'piece', N'Anti-clogging pressure-compensating button drippers for uniform container irrigation.', 1),
(8, 1, N'Agricultural Grade Dolomite Lime', N'Soil Amendment', 'bag', N'Finely ground calcium-magnesium carbonate for substrate pH buffering and neutralisation (50kg Bag).', 1);
SET IDENTITY_INSERT [Material] OFF;
GO

-- ============================================================================
-- 24. WAREHOUSES (Warehouses UC-32)
-- ============================================================================
PRINT N'--> Seeding Warehouses...';
SET IDENTITY_INSERT [Warehouse] ON;
INSERT INTO [Warehouse] (warehouse_id, farm_id, warehouse_name, address, image_url)
VALUES
(1, 1, N'Central Agricultural Supplies Warehouse', N'Building W1, Technical Operations Facility, Green Valley Farm', 'https://images.unsplash.com/photo-1586528116311-ad8dd3c8310d'),
(2, 1, N'Post-Harvest Cold Storage Center', N'Building W2, Packing & Cold Chain Logistics Center, Green Valley Farm', 'https://images.unsplash.com/photo-1553413077-190dd305871c'),
(3, 1, N'Seed & Nursery Storage Depot', N'Building W3, Certified Propagation Nursery, Green Valley Farm', NULL);
SET IDENTITY_INSERT [Warehouse] OFF;
GO

-- ============================================================================
-- 25. INVENTORY (Inventory UC-33)
-- ============================================================================
PRINT N'--> Seeding Inventory...';
SET IDENTITY_INSERT [Inventory] ON;
INSERT INTO [Inventory] (inventory_id, warehouse_id, material_id, quantity_on_hand, reorder_level)
VALUES
(1, 1, 1, 450.000, 100.000),
(2, 1, 2, 180.000, 50.000),
(3, 1, 3, 45.000, 15.000),
(4, 1, 4, 30.000, 10.000),
(5, 3, 5, 25.000, 10.000),
(6, 2, 6, 850.000, 200.000),
(7, 1, 7, 1200.000, 300.000),
(8, 1, 8, 80.000, 25.000);
SET IDENTITY_INSERT [Inventory] OFF;
GO

-- ============================================================================
-- 26. INPUT PURCHASES (Input Purchases UC-37)
-- ============================================================================
PRINT N'--> Seeding Input Purchases...';
SET IDENTITY_INSERT [InputPurchase] ON;
INSERT INTO [InputPurchase] (input_purchase_id, farm_id, supplier_id, purchased_by, purchase_date, reference_no, total_amount, receipt_image_url, notes)
VALUES
(1, 1, 2, 2, '2026-01-05', 'PO-2026-001', 18500000.00, 'https://example.com/receipts/po-2026-001.pdf', N'Procurement of early-season hydroponic nutrients and soluble fertilizer bags.'),
(2, 1, 1, 2, '2026-01-20', 'PO-2026-002', 7200000.00, 'https://example.com/receipts/po-2026-002.pdf', N'Procurement of organic botanical pest protection solutions and bio-fungicides.'),
(3, 1, 3, 2, '2026-01-10', 'PO-2026-003', 12500000.00, 'https://example.com/receipts/po-2026-003.pdf', N'Procurement of certified Japanese F1 hybrid seeds and seed germination plugs.');
SET IDENTITY_INSERT [InputPurchase] OFF;
GO

-- ============================================================================
-- 27. INPUT PURCHASE ITEMS
-- Note: line_total is a computed column, do not insert into it.
-- ============================================================================
PRINT N'--> Seeding Input Purchase Items...';
SET IDENTITY_INSERT [InputPurchaseItem] ON;
INSERT INTO [InputPurchaseItem] (purchase_item_id, input_purchase_id, material_id, warehouse_id, quantity, unit_cost)
VALUES
(1, 1, 1, 1, 300.000, 35000.00),
(2, 1, 2, 1, 160.000, 50000.00),
(3, 2, 3, 1, 40.000, 180000.00),
(4, 3, 5, 3, 25.000, 500000.00);
SET IDENTITY_INSERT [InputPurchaseItem] OFF;
GO

-- ============================================================================
-- 28. MATERIAL USAGES (Material Usages UC-24)
-- ============================================================================
PRINT N'--> Seeding Material Usages...';
SET IDENTITY_INSERT [MaterialUsage] ON;
INSERT INTO [MaterialUsage] (material_usage_id, farm_id, material_id, farm_task_id, farming_activity_id, block_id, used_by, quantity, dosage, method, safety_interval_days, used_at, notes)
VALUES
(1, 1, 2, 3, 1, 1, 2, 10.000, N'10L / 1,000L water tank', N'Drip irrigation injection', 0, '2026-02-10 08:30:00', N'Mixed into central mother tank for automated morning fertigation cycle.'),
(2, 1, 3, NULL, 3, 1, 2, 2.000, N'20ml / 16L backpack sprayer', N'Fine foliar mist', 7, '2026-03-01 16:30:00', N'Applied evenly over upper and lower leaf surfaces during cool late afternoon.'),
(3, 1, 4, 1, NULL, 1, 2, 5.000, N'1kg / 200L water', N'Root zone drench', 0, '2026-01-12 09:00:00', N'Inoculated substrate coco peat prior to seedling transplanting.');
SET IDENTITY_INSERT [MaterialUsage] OFF;
GO

-- ============================================================================
-- 29. INVENTORY TRANSACTIONS
-- ============================================================================
PRINT N'--> Seeding Inventory Transactions...';
SET IDENTITY_INSERT [InventoryTransaction] ON;
INSERT INTO [InventoryTransaction] (inventory_transaction_id, inventory_id, transaction_type, quantity_change, reference_type, reference_id, notes, created_by)
VALUES
(1, 1, 'purchase', 300.000, 'InputPurchase', 1, N'Warehouse receipt for PO-2026-001 (Soluble Fertilizer)', 2),
(2, 2, 'purchase', 160.000, 'InputPurchase', 1, N'Warehouse receipt for PO-2026-001 (Hydroponic Stock Solution)', 2),
(3, 3, 'purchase', 40.000, 'InputPurchase', 2, N'Warehouse receipt for PO-2026-002 (Neem Botanical Spray)', 2),
(4, 2, 'usage', -10.000, 'MaterialUsage', 1, N'Dispatched from inventory for automated drip fertigation on Block A1', 2),
(5, 3, 'usage', -2.000, 'MaterialUsage', 2, N'Dispatched from inventory for preventive foliar spray on Block A1', 2);
SET IDENTITY_INSERT [InventoryTransaction] OFF;
GO

-- ============================================================================
-- 30. EXPENSES (Farm Expenses UC-38)
-- ============================================================================
PRINT N'--> Seeding Farm Expenses...';
SET IDENTITY_INSERT [Expense] ON;
INSERT INTO [Expense] (expense_id, farm_id, crop_season_id, farm_task_id, expense_category, description, amount, expense_date, recorded_by)
VALUES
(1, 1, 1, NULL, N'Materials & Fertilizers', N'Bulk purchase of soluble fertilizer bags and concentrated hydroponic minerals', 18500000.00, '2026-01-05', 2),
(2, 1, 1, 1, N'Labor', N'Contract wages for washing, flushing, and buffering 5,000 coco peat grow slabs', 4500000.00, '2026-01-14', 2),
(3, 1, 1, NULL, N'Utilities & Power', N'Monthly electric power utility invoice for automated greenhouse irrigation pumps', 2800000.00, '2026-02-01', 2),
(4, 1, 1, NULL, N'Machinery & Fuel', N'Routine servicing of ventilation motors and tractor fuel for compost transport', 1500000.00, '2026-02-15', 2),
(5, 1, 1, 5, N'Pest Control', N'Biological pest control supplies including Neem Chito and predatory mite sachets', 3200000.00, '2026-02-20', 2),
(6, 1, 2, NULL, N'Packaging', N'Branded 5-ply export carton boxes with QR traceability labels for bell peppers', 4200000.00, '2025-12-18', 2);
SET IDENTITY_INSERT [Expense] OFF;
GO

-- ============================================================================
-- 31. HARVEST RECORDS (Harvest UC-25)
-- ============================================================================
PRINT N'--> Seeding Harvest Records...';
SET IDENTITY_INSERT [HarvestRecord] ON;
INSERT INTO [HarvestRecord] (harvest_record_id, farm_id, crop_season_id, block_id, farming_activity_id, recorded_by, harvest_date, quantity, unit, quality_grade, notes)
VALUES
(1, 1, 2, 3, 4, 2, '2025-12-20', 1450.000, 'kg', N'Grade 1 (Export Premium)', N'Firm, uniform bright red sweet bell peppers with intact fresh green calyxes.'),
(2, 1, 2, 3, 4, 2, '2025-12-20', 380.000, 'kg', N'Grade 2 (Domestic Clean Food)', N'Minor skin curvature but excellent sweetness, crispness, and zero pest damage.'),
(3, 1, 1, 1, NULL, 2, '2026-04-24', 2500.000, 'kg', N'Grade 1 (Export Premium)', N'Dense netted F1 Taka melons measuring 14.5 brix average sweetness.');
SET IDENTITY_INSERT [HarvestRecord] OFF;
GO

-- ============================================================================
-- 32. PRODUCT SALES
-- Note: total_amount is a computed column, do not insert into it.
-- ============================================================================
PRINT N'--> Seeding Product Sales...';
SET IDENTITY_INSERT [ProductSale] ON;
INSERT INTO [ProductSale] (product_sale_id, farm_id, harvest_record_id, recorded_by, buyer_name, sale_date, quantity, unit, unit_price, notes)
VALUES
(1, 1, 1, 2, N'WinMart Southern Supermarket Chain', '2025-12-21', 1400.000, 'kg', 42000.00, N'Official VietGAP agricultural produce supply agreement contract.'),
(2, 1, 2, 2, N'Organic Gourmet Market Hanoi', '2025-12-22', 380.000, 'kg', 28000.00, N'Overnight express refrigerated air transport distribution.'),
(3, 1, 3, 2, N'High-Tech Fresh Produce Co-op', '2026-04-26', 2200.000, 'kg', 55000.00, N'Direct purchase contract for festival holiday melon gift baskets.');
SET IDENTITY_INSERT [ProductSale] OFF;
GO

-- ============================================================================
-- 33. CHECKLIST RULES (Checklist Rules UC-42)
-- ============================================================================
PRINT N'--> Seeding Checklist Rules...';
SET IDENTITY_INSERT [ChecklistRule] ON;
INSERT INTO [ChecklistRule] (checklist_rule_id, farm_id, created_by, rule_name, description, category, is_active)
VALUES
(1, 1, 2, N'Irrigation Solution EC & pH Calibration', N'Measure and record nutrient solution electrical conductivity and pH prior to daily morning irrigation runs.', N'Irrigation & Nutrition', 1),
(2, 1, 2, N'Mandatory Pre-Harvest Chemical Safety Interval', N'Verify that at least 14 days have elapsed since the most recent organic or chemical treatment before starting harvest.', N'Food Safety & Pre-Harvest', 1),
(3, 1, 2, N'Personal Hygiene and Handwashing Sanitization', N'All field harvesters and packaging personnel must wash and disinfect hands before entering processing bays.', N'Personnel & Biosecurity', 1),
(4, 1, 2, N'Cold Storage Holding Temperature Verification', N'Confirm that post-harvest cold room thermometer reads consistently between 8Â°C and 12Â°C.', N'Storage & Post-Harvest', 1);
SET IDENTITY_INSERT [ChecklistRule] OFF;
GO

-- ============================================================================
-- 34. CHECKLIST RUNS & RESULTS
-- ============================================================================
PRINT N'--> Seeding Checklist Runs & Results...';
SET IDENTITY_INSERT [ChecklistRun] ON;
INSERT INTO [ChecklistRun] (checklist_run_id, checklist_rule_id, crop_season_id, block_id, assigned_to, run_status, scheduled_at, completed_at)
VALUES
(1, 1, 1, 1, 2, 'completed', '2026-02-10 07:00:00', '2026-02-10 07:30:00'),
(2, 2, 2, 3, 2, 'completed', '2025-12-18 14:00:00', '2025-12-18 14:45:00'),
(3, 4, 1, 1, 2, 'completed', '2026-02-15 08:00:00', '2026-02-15 08:20:00');
SET IDENTITY_INSERT [ChecklistRun] OFF;
GO

SET IDENTITY_INSERT [ChecklistResult] ON;
INSERT INTO [ChecklistResult] (checklist_result_id, checklist_run_id, checked_by, result_status, notes)
VALUES
(1, 1, 2, 'pass', N'pH calibrated at 6.05, EC calibrated at 1.81 mS/cm - Fully compliant with target recipe.'),
(2, 2, 2, 'pass', N'19 days elapsed since last Neem Chito application; pre-harvest safety criteria fully satisfied.'),
(3, 3, 2, 'pass', N'Cold room thermostat verified at 9.5Â°C; humidity level steady at 85%.');
SET IDENTITY_INSERT [ChecklistResult] OFF;
GO

-- ============================================================================
-- 35. PRODUCTION REPORTS
-- ============================================================================
PRINT N'--> Seeding Production Reports...';
SET IDENTITY_INSERT [ProductionReport] ON;
INSERT INTO [ProductionReport] (production_report_id, farm_id, crop_season_id, generated_by, report_title, report_data)
VALUES
(1, 1, 2, 2, N'Executive Performance Report - Winter 2025 Bell Pepper Season', N'{"season":"Winter 2025","crop":"Bell Pepper","planted_area_m2":6000,"total_yield_kg":1830.0,"grade_1_kg":1450.0,"grade_2_kg":380.0,"total_revenue_vnd":69440000,"production_cost_vnd":28500000,"net_profit_vnd":40940000,"roi_pct":143.6}'),
(2, 1, 1, 2, N'Mid-Season Operational Review - Spring 2026 Cantaloupe Season', N'{"season":"Spring 2026","crop":"Cantaloupe Melon","planted_area_m2":5000,"plants_count":11500,"pollination_rate_pct":96.5,"estimated_yield_kg":14000.0,"projected_revenue_vnd":770000000}');
SET IDENTITY_INSERT [ProductionReport] OFF;
GO

-- ============================================================================
-- 36. TRAINING RECORDS (Training Records UC-26)
-- ============================================================================
PRINT N'--> Seeding Training Records...';
SET IDENTITY_INSERT [TrainingRecord] ON;
INSERT INTO [TrainingRecord] (training_record_id, farm_id, organized_by, training_title, description, training_date)
VALUES
(1, 1, 2, N'Precision Melon Hand Pollination & Trellising Workshop', N'Standardized training on selecting male flowers, detaching petals, and dusting female stigmas at optimal morning hours.', '2026-01-25'),
(2, 1, 2, N'VietGAP Safe Agrochemical Handling, PPE & Waste Disposal', N'Safe mixing procedures, triple-rinsing chemical empty bottles, proper respirator fit, and emergency eyewash protocols.', '2025-12-15');
SET IDENTITY_INSERT [TrainingRecord] OFF;
GO

-- ============================================================================
-- 37. TRAINING ATTENDEES
-- ============================================================================
PRINT N'--> Seeding Training Attendees...';
INSERT INTO [TrainingAttendee] (training_record_id, farm_worker_id, attendance_status, notes)
VALUES
(1, 1, 'attended', N'Exhibited complete theoretical and practical mastery of the pollination technique.'),
(1, 3, 'attended', N'Practical performance evaluated as meeting export greenhouse standards.'),
(2, 2, 'attended', N'Completed PPE inspection and certified for pesticide handling.'),
(2, 4, 'attended', N'Passed post-training compliance exam with perfect score.');
GO

-- ============================================================================
-- 38. ASSESSMENT CRITERIA & INTERNAL AUDITS
-- ============================================================================
PRINT N'--> Seeding Internal Assessments...';
SET IDENTITY_INSERT [AssessmentCriterion] ON;
INSERT INTO [AssessmentCriterion] (assessment_criterion_id, farm_id, criterion_name, description, max_score, is_active)
VALUES
(1, 1, N'Digital Cultivation Logbook Recordkeeping Integrity', N'Detailed and timely logging of daily fertigation schedules, chemical applications, and supervisor sign-offs.', 100.00, 1),
(2, 1, N'Greenhouse Sanitation & Biological Exclusion', N'Polyhouse perimeters are clean and weed-free; insect exclusion mesh is completely sealed without tears.', 100.00, 1),
(3, 1, N'Agrochemical Storage & Hazardous Waste Safety', N'Agrochemical storage is secured with lock and key, hazard warning signage, SDS sheets, and personal PPE.', 100.00, 1);
SET IDENTITY_INSERT [AssessmentCriterion] OFF;
GO

SET IDENTITY_INSERT [InternalAssessment] ON;
INSERT INTO [InternalAssessment] (internal_assessment_id, farm_id, crop_season_id, zone_id, assessed_by, assessment_title, notes)
VALUES
(1, 1, 1, 1, 2, N'Quarterly VietGAP Internal Audit - Greenhouse Complex Q1/2026', N'The farm demonstrates rigorous compliance with National Technical Regulation TCVN 11892-1:2017.');
SET IDENTITY_INSERT [InternalAssessment] OFF;
GO

SET IDENTITY_INSERT [InternalAssessmentItem] ON;
INSERT INTO [InternalAssessmentItem] (internal_assessment_item_id, internal_assessment_id, assessment_criterion_id, score, result_notes)
VALUES
(1, 1, 1, 98.00, N'MyCropDiary digital logging provides end-to-end transparent traceability for all farm inputs.'),
(2, 1, 2, 95.00, N'Insect exclusion screens and double-door airlock entry are intact and well maintained.'),
(3, 1, 3, 97.00, N'All chemical containers carry authentic manufacturer labels and traceable batch numbers.');
SET IDENTITY_INSERT [InternalAssessmentItem] OFF;
GO

-- ============================================================================
-- 39. KNOWLEDGE BASE ARTICLES (Knowledge Base UC-43)
-- ============================================================================
PRINT N'--> Seeding Knowledge Base Articles...';
SET IDENTITY_INSERT [KnowledgeArticle] ON;
INSERT INTO [KnowledgeArticle] (knowledge_article_id, created_by, title, slug, summary, body, cover_image_url, category, tags, view_count, source_url, article_status, published_at)
VALUES
(1, 1, N'High-Tech Greenhouse Cantaloupe Cultivation under VietGAP', 'high-tech-greenhouse-cantaloupe-cultivation-vietgap', N'Comprehensive guidelines on substrate preparation, automated fertigation recipes, and environmental humidity control.', N'# High-Tech Greenhouse Cantaloupe Cultivation under VietGAP

## 1. Substrate Preparation
Coco peat must be thoroughly desalted and flushed 3 to 4 times with clean water to eliminate excess sodium, tannins, and lignin, followed by buffering with calcium nitrate for 24 hours.

## 2. Precision Automated Fertigation
- **Seedling Stage (Days 1 to 15)**: Target EC 1.2 - 1.5 mS/cm, pH 5.8 - 6.2.
- **Vegetative & Trellising (Days 16 to 30)**: Target EC 1.6 - 1.8 mS/cm.
- **Pollination & Fruit Set (Days 31 to 45)**: Target EC 1.8 - 2.0 mS/cm.
- **Fruit Netting & Sugar Ripening (Days 46 to 75)**: Increase Potassium ratio, maintain EC 2.2 - 2.5 mS/cm.

## 3. Crop Protection Protocols
Implement preventive bio-control measures utilizing cold-pressed neem formulations and beneficial Trichoderma inoculants to prevent fungal root diseases.', 'https://images.unsplash.com/photo-1598170845058-32b9d6a5da37', N'Technical Cultivation Guide', N'cantaloupe,hydroponics,vietgap,fertigation', 342, 'https://vietgap.gov.vn', 'published', '2026-01-05 08:00:00'),

(2, 1, N'Balanced Hydroponic Nutrient Management: EC & pH Control', 'balanced-hydroponic-nutrient-management-ec-ph', N'In-depth guide on electrical conductivity (EC), pH dynamics, and essential macro/micro mineral interactions.', N'# Balanced Hydroponic Nutrient Management

## Understanding EC and pH
Electrical Conductivity (EC) reflects total dissolved mineral salts in the fertigation solution. pH governs plant root bio-availability for essential nutrients:
- **Optimal Root Zone pH**: 5.8 to 6.2.
- If pH drops below 5.5, Calcium and Magnesium uptake becomes restricted.
- If pH rises above 6.8, Iron, Manganese, and Zinc precipitate out of solution.

## Best Practices
1. Check drainage leachate daily (aim for 20-25% runoff fraction).
2. Clean injection venturis and calibrate pH/EC probes weekly with standardized buffer solutions.', 'https://images.unsplash.com/photo-1585320806297-9794b3e4eeae', N'Irrigation & Nutrition', N'hydroponics,nutrients,pH,EC,water-management', 518, 'https://vietgap.gov.vn', 'published', '2026-01-10 10:30:00'),

(3, 1, N'Integrated Pest Management (IPM) in Protected Polyhouses', 'integrated-pest-management-ipm-protected-polyhouses', N'Safe, compliant, and sustainable pest scouting, bio-control releases, and threshold spraying strategies.', N'# Integrated Pest Management (IPM) in Protected Polyhouses

## The Four Pillars of VietGAP IPM
1. **Physical Exclusion**: 50-mesh insect netting across all greenhouse vents, airlock vestibules, and sticky traps.
2. **Scouting & Thresholds**: Deploy 1 yellow sticky trap per 100 mÂ² to monitor thrips, whiteflies, and leafminers.
3. **Biological Control**: Encourage beneficial parasitoid wasps, ladybugs, and entomopathogenic fungi.
4. **Targeted Organic Treatments**: Use botanical extracts (Neem, Chitosan) and Bacillus thuringiensis (Bt) only when scouting counts exceed economic damage thresholds.', 'https://images.unsplash.com/photo-1471193945509-9ad0617afabf', N'Crop Protection', N'IPM,pest-control,organic,biological,safety', 429, 'https://vietgap.gov.vn', 'published', '2026-01-18 14:00:00'),

(4, 1, N'Post-Harvest Cold Chain Protocol for Fresh Vegetables', 'post-harvest-cold-chain-protocol-fresh-vegetables', N'Preserving freshness, reducing field heat, and maintaining export quality standards.', N'# Post-Harvest Cold Chain Protocol

## Key Processing Steps
- **Field Heat Removal**: Pre-cool freshly picked produce within 2 hours of harvest to reduce cellular respiration.
- **Sanitary Sorting & Grading**: Remove mechanically damaged fruit to prevent ethylene release and fungal spread.
- **Storage Temperature**: Maintain cold room holding temperatures between 8Â°C and 12Â°C with 85-90% relative humidity.
- **Traceability Tagging**: Affix individual QR labels indicating farm source, parcel ID, harvest date, and VietGAP certificate number.', 'https://images.unsplash.com/photo-1553413077-190dd305871c', N'Post-Harvest & Storage', N'post-harvest,cold-chain,storage,traceability,packaging', 285, 'https://vietgap.gov.vn', 'published', '2026-01-25 16:00:00');
SET IDENTITY_INSERT [KnowledgeArticle] OFF;
GO

-- ============================================================================
-- 40. AI CONVERSATIONS, MESSAGES & FEEDBACK (UC-12, UC-13)
-- ============================================================================
PRINT N'--> Seeding AI Conversations & Messages...';
SET IDENTITY_INSERT [AIConversation] ON;
INSERT INTO [AIConversation] (ai_conversation_id, user_id, farm_id, block_id, crop_season_id, conversation_title, conversation_status)
VALUES
(1, 2, 1, 1, 1, N'Consultation: Lower Leaf Interveinal Chlorosis on 30-Day Melons', 'open'),
(2, 2, 1, 3, 2, N'Consultation: Optimal Pre-Harvest Irrigation Withholding Strategy', 'closed');
SET IDENTITY_INSERT [AIConversation] OFF;
GO

SET IDENTITY_INSERT [AIMessage] ON;
INSERT INTO [AIMessage] (ai_message_id, ai_conversation_id, sender_type, message_text, model_name)
VALUES
(1, 1, 'user', N'My 30-day-old cantaloupe plants in Block A1 show interveinal yellowing with pale margins on mature lower leaves, while the main leaf veins remain dark green. What is this deficiency and how should I treat it according to VietGAP?', 'gpt-4o-mini'),
(2, 1, 'assistant', N'Hello Michael! Based on your symptom descriptionâ€”interveinal chlorosis starting on older, mature lower leaves while primary veins stay dark greenâ€”this is a classic symptom of Magnesium (Mg) deficiency, often aggravated by an antagonistic Potassium/Magnesium (K/Mg) imbalance in your hydroponic solution.

### Recommended VietGAP-Compliant Corrective Actions:
1. **Verify Root Zone pH**: The optimal range is 5.8 - 6.2. If root substrate pH drops below 5.5, Magnesium uptake is severely inhibited.
2. **Immediate Foliar Correction**: Dissolve agricultural-grade Magnesium Sulfate (MgSO4 Â· 7H2O) at 1.0 g per liter of clean water. Apply a fine foliar mist over both leaf surfaces in early morning (06:30 - 08:00 AM).
3. **Fertigation Balance Adjustment**: Review your fertilizer stock solution ratio. Ensure Potassium (K) concentration is not excessively elevated, as high Potassium outcompetes Magnesium absorption.
4. **Log the Application**: Make sure to record the foliar spray in your MyCropDiary material usage log to maintain full VietGAP audit compliance.', 'gpt-4o-mini'),
(3, 2, 'user', N'How many days before harvest should I withhold water or cut back irrigation on sweet bell peppers to prevent fruit cracking?', 'gpt-4o-mini'),
(4, 2, 'assistant', N'For sweet bell peppers in greenhouse conditions, follow these guidelines:
1. **Gradual Reduction**: Do not cut water off abruptly as this causes blossom end rot or plant collapse. Instead, reduce daily irrigation volume by 25-30% starting 5 to 7 days before harvest.
2. **Harvest Morning Protocol**: Ensure plants receive no heavy irrigation on the morning of harvest. Picking slightly dry fruit ensures higher post-harvest firmness and prevents micro-skin cracking during transport.', 'gpt-4o-mini');
SET IDENTITY_INSERT [AIMessage] OFF;
GO

-- AI Feedback (UC-13)
SET IDENTITY_INSERT [AIFeedback] ON;
INSERT INTO [AIFeedback] (ai_feedback_id, ai_message_id, user_id, rating, feedback_text)
VALUES
(1, 2, 2, 5, N'Extremely accurate and scientifically sound advice! After applying the foliar MgSO4 spray, the plants recovered vibrant green foliage within 3 days without any crop setback.'),
(2, 4, 2, 5, N'Very practical advice that prevented fruit splitting in our winter harvest batch.');
SET IDENTITY_INSERT [AIFeedback] OFF;
GO

SET IDENTITY_INSERT [AIFeedbackReply] ON;
INSERT INTO [AIFeedbackReply] (ai_feedback_reply_id, ai_feedback_id, user_id, reply_text)
VALUES
(1, 1, 1, N'The MyCropDiary agronomic advisory team is glad to have assisted you! Wishing Green Valley Farm a bountiful and high-yielding harvest.');
SET IDENTITY_INSERT [AIFeedbackReply] OFF;
GO

-- ============================================================================
-- 41. NOTIFICATIONS
-- ============================================================================
PRINT N'--> Seeding Notifications...';
SET IDENTITY_INSERT [Notification] ON;
INSERT INTO [Notification] (notification_id, user_id, title, message, notification_type, is_read, action_url, created_at)
VALUES
(1, 2, N'VietGAP Seasonal Checklist Due', N'Reminder: Mandatory internal self-inspection checklist for Spring 2026 Cantaloupe Season is due within 48 hours.', 'CHECKLIST', 0, '/checklists', SYSUTCDATETIME()),
(2, 2, N'Soil & Substrate Lab Analysis Updated', N'Sample results for Block A1 (Zone A) recorded: pH 6.4, EC 1.82 mS/cm, fully compliant with TCVN 11892-1:2017.', 'SYSTEM', 0, '/environment', SYSUTCDATETIME()),
(3, 4, N'New Task Assignment: Fertigation Calibration', N'You have been designated as lead technician to calibrate automated drip fertigation on Greenhouse Block A1.', 'TASK', 1, '/tasks', SYSUTCDATETIME()),
(4, 2, N'Weather Alert: High Humidity Forecast', N'Relative humidity predicted to exceed 90% tonight. Please verify that automated greenhouse convection fans are operational.', 'ALERT', 0, '/zones', SYSUTCDATETIME());
SET IDENTITY_INSERT [Notification] OFF;
GO

PRINT N'============================================================================';
PRINT N'--> MyCropDiary English Sample Data Seeding Completed Successfully!';
PRINT N'============================================================================';
GO
