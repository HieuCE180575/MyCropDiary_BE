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
(1, N'Quản Trị Viên Hệ Thống', 'admin@cropdiary.com', '0901234567', '$2a$10$8jIMVzui5SoBFkUR1udxQO3Nqp09gvVoXobsoScb/zlisb4JIieDC', 'admin', 'active', SYSUTCDATETIME()),
(2, N'Trần Văn Chủ Nông Trại Đà Lạt', 'owner1@dalatfarm.com', '0912345678', '$2a$10$8jIMVzui5SoBFkUR1udxQO3Nqp09gvVoXobsoScb/zlisb4JIieDC', 'user', 'active', SYSUTCDATETIME()),
(3, N'Lê Thị Chủ Vườn Miền Tây', 'owner2@mekongfarm.com', '0923456789', '$2a$10$8jIMVzui5SoBFkUR1udxQO3Nqp09gvVoXobsoScb/zlisb4JIieDC', 'user', 'active', SYSUTCDATETIME()),
(4, N'Nguyễn Kỹ Thuật Viên Đà Lạt', 'staff1@dalatfarm.com', '0934567890', '$2a$10$8jIMVzui5SoBFkUR1udxQO3Nqp09gvVoXobsoScb/zlisb4JIieDC', 'user', 'active', SYSUTCDATETIME()),
(5, N'Hoàng Quản Lý Vùng Trồng', 'staff2@dalatfarm.com', '0945678901', '$2a$10$8jIMVzui5SoBFkUR1udxQO3Nqp09gvVoXobsoScb/zlisb4JIieDC', 'user', 'active', SYSUTCDATETIME()),
(6, N'Phạm Đăng Ký Chờ Kích Hoạt', 'pending.user@cropdiary.com', '0956789012', '$2a$10$8jIMVzui5SoBFkUR1udxQO3Nqp09gvVoXobsoScb/zlisb4JIieDC', 'user', 'pending', NULL),
(7, N'Trần Văn Bình (Công Nhân Chăm Sóc)', 'worker1@dalatfarm.com', '0967890123', '$2a$10$8jIMVzui5SoBFkUR1udxQO3Nqp09gvVoXobsoScb/zlisb4JIieDC', 'user', 'active', SYSUTCDATETIME()),
(8, N'Lê Thị Hoa (Công Nhân Thu Hoạch)', 'worker2@dalatfarm.com', '0978901234', '$2a$10$8jIMVzui5SoBFkUR1udxQO3Nqp09gvVoXobsoScb/zlisb4JIieDC', 'user', 'active', SYSUTCDATETIME()),
(9, N'Đặng Hữu Phúc (Chủ Hộ Đăng Ký)', 'register.farm@cropdiary.com', '0989012345', '$2a$10$8jIMVzui5SoBFkUR1udxQO3Nqp09gvVoXobsoScb/zlisb4JIieDC', 'user', 'active', SYSUTCDATETIME());
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
(1, N'Trang Trại Rau Củ Sạch Đà Lạt', N'Chuyên canh dưa lưới, ớt chuông công nghệ cao VietGAP', N'123 Mai Anh Đào, Phường 8, TP Đà Lạt, Lâm Đồng', 11.954500, 108.445200, 'https://images.unsplash.com/photo-1500937386664-56d1dfef3854', 2, 'active'),
(2, N'Hợp Tác Xã Cây Ăn Trái Mekong', N'Chuyên canh xoài cát chu xuất khẩu tiêu chuẩn GlobalGAP', N'Ấp Bình Hòa, Xã Mỹ Xương, Huyện Cao Lãnh, Đồng Tháp', 10.450100, 105.632200, 'https://images.unsplash.com/photo-1592982537447-7440770cbfc9', 3, 'active'),
(3, N'Nông Trại Hữu Cơ Ba Vì', N'Nông trại rau hữu cơ sinh thái đang quy hoạch', N'Xã Yên Bài, Huyện Ba Vì, TP Hà Nội', 21.054300, 105.352100, NULL, 1, 'inactive');
SET IDENTITY_INSERT [Farm] OFF;
GO

-- 4. FARM REGISTRATIONS (IDs: 1 to 4)
SET IDENTITY_INSERT [FarmRegistration] ON;
INSERT INTO [FarmRegistration] (registration_id, user_id, farm_name, address, province, district, ward, document_url, registration_note, registration_status, reviewed_by, reviewed_at, resulting_farm_id)
VALUES
(1, 2, N'Trang Trại Rau Củ Sạch Đà Lạt', N'123 Mai Anh Đào', N'Lâm Đồng', N'TP Đà Lạt', N'Phường 8', 'https://example.com/docs/vietgap-dalat.pdf', N'Đơn đăng ký hợp lệ, đầy đủ giấy chứng nhận VietGAP', 'approved', 1, '2025-01-02 10:00:00', 1),
(2, 3, N'Hợp Tác Xã Cây Ăn Trái Mekong', N'Ấp Bình Hòa', N'Đồng Tháp', N'Huyện Cao Lãnh', N'Xã Mỹ Xương', 'https://example.com/docs/globalgap-mekong.pdf', N'Đơn đăng ký hợp tác xã đạt chuẩn xuất khẩu', 'approved', 1, '2025-02-15 14:30:00', 2),
(3, 9, N'Nông Trại Xanh Tây Nguyên', N'Thôn 4', N'Đắk Lắk', N'TP Buôn Ma Thuột', N'Xã Ea Kao', 'https://example.com/docs/taynguyen-farm.pdf', N'Đăng ký nông trại cà phê & sầu riêng sạch, đang chờ admin xem xét duyệt', 'pending', NULL, NULL, NULL),
(4, 9, N'Vườn Dược Liệu Kon Tum', N'Xã Măng Cành', N'Kon Tum', N'Huyện Kon Plông', N'Xã Măng Cành', NULL, N'Thiếu giấy tờ quyền sử dụng đất và tọa độ GPS', 'rejected', 1, '2025-11-20 09:15:00', NULL);
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
(1, 2, 'CN-DL-01', N'Kỹ Sư Dinh Dưỡng & Tưới Tiêu', 'active', '2025-01-01', N'Phụ trách kiểm soát pha dung dịch dinh dưỡng A+B'),
(2, 4, 'CN-DL-02', N'Công Nhân Chăm Sóc Cây & Cắt Tỉa', 'active', '2025-03-01', N'Lành nghề tỉa chồi nách và thụ phấn dưa'),
(3, 5, 'CN-DL-03', N'Công Nhân Thu Hoạch & Đóng Gói', 'seasonal', '2025-09-01', N'Hợp đồng mùa vụ thu hoạch nông sản');
SET IDENTITY_INSERT [FarmWorker] OFF;
GO

-- 7. CROP CATEGORIES (IDs: 1 to 4)
SET IDENTITY_INSERT [CropCategory] ON;
INSERT INTO [CropCategory] (crop_category_id, category_name, description, image_url)
VALUES
(1, N'Dưa Lưới', N'Dưa lưới ruột cam giống Nhật Bản công nghệ cao', 'https://images.unsplash.com/photo-1598170845058-32b9d6a5da37'),
(2, N'Ớt Chuông Đà Lạt', N'Ớt chuông đỏ, vàng, cam ngọt giòn nhà kính', 'https://images.unsplash.com/photo-1563565375-f3fdfdbefa83'),
(3, N'Cà Chua Cherry', N'Cà chua bi ngọt chùm xuất khẩu', 'https://images.unsplash.com/photo-1592924357228-91a4daadcfea'),
(4, N'Xoài Cát Chu', N'Đặc sản xoài cát chu Đồng Tháp', 'https://images.unsplash.com/photo-1553279768-865429fa0078');
SET IDENTITY_INSERT [CropCategory] OFF;
GO

-- 8. CROP SEASONS (IDs: 1 to 3)
SET IDENTITY_INSERT [CropSeason] ON;
INSERT INTO [CropSeason] (crop_season_id, farm_id, crop_category_id, season_name, start_date, end_date, season_status, notes, created_by)
VALUES
(1, 1, 1, N'Vụ Dưa Lưới Xuân 2026', '2026-01-15', '2026-04-30', 'active', N'Vụ dưa lưới trọng điểm phục vụ thị trường dịp lễ', 2),
(2, 1, 2, N'Vụ Ớt Chuông Đông 2025', '2025-09-01', '2025-12-25', 'completed', N'Vụ mùa đã thu hoạch và thanh toán hoàn tất', 2),
(3, 1, 3, N'Vụ Cà Chua Hè 2026', '2026-05-01', '2026-08-30', 'planned', N'Kế hoạch gieo trồng nhà màng số 2', 2);
SET IDENTITY_INSERT [CropSeason] OFF;
GO

-- 9. ZONES (IDs: 1 to 4)
SET IDENTITY_INSERT [Zone] ON;
INSERT INTO [Zone] (zone_id, farm_id, zone_name, description, area_value, area_unit)
VALUES
(1, 1, N'Khu Vực A - Nhà Màng', N'Khu nhà màng công nghệ cao trồng dưa lưới thủy canh', 5000.00, 'm2'),
(2, 2, N'Khu Vực B - Luống Hở', N'Khu trồng ớt chuông và rau ăn củ ngoài trời', 8000.00, 'm2'),
(3, 1, N'Khu Vực C - Vườn Ươm', N'Vườn ươm cây giống sạch bệnh đạt chuẩn', 2000.00, 'm2'),
(4, 2, N'Khu Vực 1 - Vườn Xoài', N'Vườn xoài cát chu ghép cành lâu năm', 15000.00, 'm2');
SET IDENTITY_INSERT [Zone] OFF;
GO

-- 10. BLOCKS (IDs: 1 to 5)
SET IDENTITY_INSERT [Block] ON;
INSERT INTO [Block] (block_id, zone_id, block_name, description, area_value, area_unit, soil_type)
VALUES
(1, 1, N'Lô A1', N'Lô dưa lưới giống F1 Taka Nhật Bản', 2500.00, 'm2', N'Xơ dừa xả chát thủy canh'),
(2, 1, N'Lô A2', N'Lô dưa lưới giống Huỳnh Long ruột vàng', 2500.00, 'm2', N'Xơ dừa xả chát thủy canh'),
(3, 2, N'Lô B1', N'Lô ớt chuông đỏ ngọt', 4000.00, 'm2', N'Đất đỏ bazan Lâm Đồng'),
(4, 2, N'Lô B2', N'Lô ớt chuông vàng ngọt', 4000.00, 'm2', N'Đất đỏ bazan Lâm Đồng'),
(5, 3, N'Lô C1', N'Lô ươm khay hạt mầm dưa lưới', 2000.00, 'm2', N'Mùn dừa trộn tro trấu vi sinh');
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
(1, 1, 1, 1, 1, N'Xử lý giá thể xơ dừa', N'Ngâm rửa xả chát tannin và xử lý nấm trichoderma', N'Làm đất', 'high', 'completed', '2026-01-10 07:00:00', '2026-01-14 17:00:00', 2),
(2, 1, 1, 1, 1, N'Xuống giống cây con dưa lưới', N'Chuyển cây con 12 ngày tuổi từ vườn ươm sang bầu trồng', N'Gieo trồng', 'urgent', 'completed', '2026-01-15 06:30:00', '2026-01-16 11:30:00', 2),
(3, 1, 1, 1, 1, N'Châm dinh dưỡng nhỏ giọt đợt 3', N'Pha dinh dưỡng A+B nồng độ EC 1.8 và chạy tưới nhỏ giọt', N'Bón phân & Tưới', 'normal', 'completed', '2026-02-10 08:00:00', '2026-02-10 11:00:00', 2),
(4, 1, 1, 1, 1, N'Thụ phấn nhân tạo dưa lưới', N'Thụ phấn hoa cái tầng nách lá thứ 9 đến 12', N'Chăm sóc', 'high', 'in_progress', '2026-02-25 07:00:00', '2026-03-05 10:00:00', 2),
(5, 1, 1, 1, 1, N'Phòng trừ bọ trĩ sinh học', N'Phun chế phẩm vi sinh thảo mộc Neem Chito ngừa bọ trĩ', N'Bảo vệ thực vật', 'normal', 'planned', '2026-03-08 16:00:00', '2026-03-08 18:00:00', 2),
(6, 1, 1, 1, 1, N'Thu hoạch dưa lưới chín đạt độ ngọt', N'Cắt cuống hình chữ T, cân khối lượng và dán tem QR truy xuất', N'Thu hoạch', 'urgent', 'planned', '2026-04-22 06:00:00', '2026-04-25 17:00:00', 2);
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
(1, 1, 3, 1, 1, 1, N'Châm dinh dưỡng nhỏ giọt A+B EC 1.8', N'Bón phân & Tưới', N'Đã hoàn thành châm 10 lít dung dịch A+B vào bồn tưới 1000L', '2026-02-10 08:30:00', NULL, 4),
(2, 1, 4, 1, 1, 1, N'Tỉa nhánh nách và thụ phấn dưa lưới', N'Chăm sóc', N'Thụ phấn được khoảng 1200 gốc tại hàng 1 đến 15', '2026-02-28 07:45:00', NULL, 4),
(3, 1, NULL, 1, 1, 1, N'Phun phòng ngừa nấm bệnh sinh học', N'Bảo vệ thực vật', N'Phun 2 chai chế phẩm Neem Chito lúc 16h30 trời mát', '2026-03-01 16:30:00', NULL, 4),
(4, 1, NULL, 2, 2, 3, N'Thu hoạch ớt chuông đợt cuối vụ đông 2025', N'Thu hoạch', N'Thu hoạch toàn bộ trái đạt độ chín đỏ', '2025-12-20 09:00:00', NULL, 4);
SET IDENTITY_INSERT [FarmingActivity] OFF;
GO

-- 16. ACTIVITY WORKERS (Composite PK)
INSERT INTO [ActivityWorker] (farming_activity_id, farm_worker_id, participation_role)
VALUES
(1, 1, N'Kỹ thuật viên pha dinh dưỡng'),
(2, 2, N'Công nhân thụ phấn chính'),
(4, 2, N'Hái trái'),
(4, 3, N'Phân loại và đóng két nhựa');
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
(1, 1, 3, N'Ẩm độ cao & Nấm sương mai', 'medium', N'Độ ẩm ban đêm trong nhà màng vượt 92% dễ phát sinh nấm hại', N'Bật quạt đối lưu và mở hé cửa nóc thông gió từ 05:00 sáng', '2026-01-20 10:00:00'),
(2, 2, 3, N'Xói mòn đất sườn dốc', 'low', N'Mưa to có thể cuốn trôi tầng đất mặt ở luống rìa', N'Trồng cỏ vetiver gia cố mép taluy và đào rãnh thoát nước cắt ngang', '2026-01-22 14:00:00');
SET IDENTITY_INSERT [ZoneRiskAssessment] OFF;
GO

-- 19. WATER SOURCES (IDs: 1 to 2)
SET IDENTITY_INSERT [WaterSource] ON;
INSERT INTO [WaterSource] (water_source_id, farm_id, source_name, source_type, description)
VALUES
(1, 1, N'Hồ Lắng Nước Suối Cam Ly', N'Hồ chứa bề mặt', N'Dung tích 2.500 m3 có trang bị hệ thống lọc cát và đĩa lọc vi sinh'),
(2, 1, N'Giếng Khoan Tầng Nông Số 1', N'Nước ngầm', N'Độ sâu giếng 70m, nước trong, độ cứng thấp');
SET IDENTITY_INSERT [WaterSource] OFF;
GO

-- 20. WATER ASSESSMENTS (IDs: 1 to 2)
SET IDENTITY_INSERT [WaterAssessment] ON;
INSERT INTO [WaterAssessment] (water_assessment_id, water_source_id, block_id, conducted_by, water_ph, temperature_c, quality_status, notes, assessed_at)
VALUES
(1, 1, 1, 2, 6.60, 20.50, N'Đạt chuẩn tưới VietGAP', N'Hàm lượng kim loại nặng và vi khuẩn E.coli đều dưới ngưỡng cho phép', '2026-01-12 08:30:00'),
(2, 2, 1, 2, 6.20, 21.00, N'Rất tốt', N'Nước ngầm rất ngọt và mềm, tối ưu cho việc phối trộn dinh dưỡng thủy canh', '2026-02-15 09:00:00');
SET IDENTITY_INSERT [WaterAssessment] OFF;
GO

-- 21. SOIL ASSESSMENTS (IDs: 1 to 2)
SET IDENTITY_INSERT [SoilAssessment] ON;
INSERT INTO [SoilAssessment] (soil_assessment_id, block_id, conducted_by, soil_ph, moisture_percent, nutrient_data, notes, assessed_at)
VALUES
(1, 1, 2, 6.40, 72.50, N'{"EC_mS_cm": 1.82, "N_ppm": 140, "P_ppm": 45, "K_ppm": 210, "Ca_ppm": 160, "Mg_ppm": 55}', N'Giá thể xơ dừa ổn định, độ dẫn điện EC đạt mức tối ưu cho dưa ra hoa đậu quả', '2026-02-18 10:15:00'),
(2, 3, 2, 5.80, 65.00, N'{"EC_mS_cm": 1.15, "N_ppm": 95, "P_ppm": 32, "K_ppm": 140, "Organic_matter_pct": 3.8}', N'Đất bazan màu mỡ, cần bón bổ sung vôi nông nghiệp nâng nhẹ pH lên 6.2 trước vụ mới', '2026-02-20 11:00:00');
SET IDENTITY_INSERT [SoilAssessment] OFF;
GO

-- 22. BLOCK CONDITION OBSERVATIONS (IDs: 1 to 2)
SET IDENTITY_INSERT [BlockConditionObservation] ON;
INSERT INTO [BlockConditionObservation] (observation_id, block_id, recorded_by, condition_type, description, observed_at)
VALUES
(1, 1, 2, N'Sinh trưởng cây', N'Cây dưa lưới tầng lá 10 phát triển đồng đều, nụ hoa cái mập mạp, không có dấu hiệu thiếu vi lượng', '2026-02-26 08:00:00'),
(2, 3, 2, N'Tình trạng dịch hại', N'Đất khô ráo, phát hiện rải rác một vài vết đốm phấn trắng trên lá già góc đông nam', '2026-02-27 15:30:00');
SET IDENTITY_INSERT [BlockConditionObservation] OFF;
GO

-- 23. SUPPLIERS (IDs: 1 to 2)
SET IDENTITY_INSERT [Supplier] ON;
INSERT INTO [Supplier] (supplier_id, farm_id, supplier_name, contact_name, phone, email, address, notes)
VALUES
(1, 1, N'Công Ty Nông Dược Xanh Đà Lạt', N'Nguyễn Phi Hùng', '0988123456', 'contact@nongduocxanh.vn', N'Khu Công Nghiệp Lộc Phát, TP Bảo Lộc, Lâm Đồng', N'Nhà cung cấp phân bón hòa tan và vật tư nhà màng uy tín'),
(2, 1, N'Đại Lý Dinh Dưỡng Thủy Canh AgriBio', N'Lê Thị Mai', '0977234567', 'sales@agribio.com.vn', N'54 Hùng Vương, Phường 9, TP Đà Lạt', N'Đại lý độc quyền dinh dưỡng thủy canh GreenHydro');
SET IDENTITY_INSERT [Supplier] OFF;
GO

-- 24. MATERIALS (IDs: 1 to 5)
SET IDENTITY_INSERT [Material] ON;
INSERT INTO [Material] (material_id, farm_id, material_name, material_type, unit, description, is_active)
VALUES
(1, 1, N'Phân NPK 20-20-20 Haifa', N'Phân bón hòa tan', 'kg', N'Phân bón NPK tinh khiết tan hoàn toàn nhập khẩu Israel', 1),
(2, 2, N'Dung Dịch Thủy Canh A+B GreenHydro', N'Dinh dưỡng lỏng', 'lít', N'Cặp dinh dưỡng khoáng đa trung vi lượng đậm đặc chuyên dưa lưới', 1),
(3, 1, N'Chế Phẩm Sinh Học Trừ Sâu Neem Chito', N'Thuốc BVTV sinh học', 'chai', N'Dầu neem kết hợp chitosan xua đuổi bọ trĩ và nấm bệnh', 1),
(4, 1, N'Hạt Giống Dưa Lưới F1 Taka Nhật Bản', N'Hạt giống', 'gói', N'Hạt giống tỉ lệ nảy mầm > 98%, độ ngọt brix từ 14-16%', 1),
(5, 1, N'Thùng Carton Đóng Dưa Lưới 5kg', N'Bao bì & Đóng gói', 'cái', N'Thùng giấy 5 lớp in ấn chuẩn thương hiệu MyCropDiary', 1);
SET IDENTITY_INSERT [Material] OFF;
GO

-- 25. WAREHOUSES (IDs: 1 to 2)
SET IDENTITY_INSERT [Warehouse] ON;
INSERT INTO [Warehouse] (warehouse_id, farm_id, warehouse_name, address)
VALUES
(1, 1, N'Kho Vật Tư Nông Nghiệp Trung Tâm', N'Khu kỹ thuật số 1, Trang trại Đà Lạt'),
(2, 1, N'Kho Mát Bảo Quản Nông Sản', N'Khu sơ chế đóng gói, Trang trại Đà Lạt');
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
(1, 1, 2, 3, '2026-01-05', 'PO-2026-001', 18500000.00, N'Nhập dinh dưỡng và phân bón hòa tan đầu vụ dưa lưới'),
(2, 1, 1, 3, '2026-01-20', 'PO-2026-002', 7200000.00, N'Nhập chế phẩm trừ sâu sinh học Neem Chito');
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
INSERT INTO [MaterialUsage] (material_usage_id, farm_id, material_id, farm_task_id, farming_activity_id, block_id, used_by, quantity, used_at, notes)
VALUES
(1, 1, 2, 3, 1, 1, 2, 10.000, '2026-02-10 08:30:00', N'Pha vào bồn mẹ dung tích 1000 lít tưới nhỏ giọt'),
(2, 1, 3, NULL, 3, 1, 2, 2.000, '2026-03-01 16:30:00', N'Phun sương đều 2 mặt lá lúc trời dịu mát');
SET IDENTITY_INSERT [MaterialUsage] OFF;
GO

-- 30. INVENTORY TRANSACTIONS (IDs: 1 to 3)
SET IDENTITY_INSERT [InventoryTransaction] ON;
INSERT INTO [InventoryTransaction] (inventory_transaction_id, inventory_id, transaction_type, quantity_change, reference_type, reference_id, notes, created_by)
VALUES
(1, 1, 'purchase', 300.000, 'InputPurchase', 1, N'Nhập kho PO-2026-001', 2),
(2, 2, 'purchase', 160.000, 'InputPurchase', 1, N'Nhập kho PO-2026-001', 2),
(3, 2, 'usage', -10.000, 'MaterialUsage', 1, N'Xuất kho châm tưới Lô A1', 4);
SET IDENTITY_INSERT [InventoryTransaction] OFF;
GO

-- 31. EXPENSES (IDs: 1 to 3)
SET IDENTITY_INSERT [Expense] ON;
INSERT INTO [Expense] (expense_id, farm_id, crop_season_id, farm_task_id, expense_category, description, amount, expense_date, recorded_by)
VALUES
(1, 1, 1, NULL, N'Vật tư & Phân bón', N'Chi phí mua phân bón hòa tan và dung dịch dinh dưỡng đầu vụ', 18500000.00, '2026-01-05', 3),
(2, 1, 1, 1, N'Nhân công', N'Tiền công xử lý ngâm xả chát 5000 bao xơ dừa', 4500000.00, '2026-01-14', 3),
(3, 1, 1, NULL, N'Điện nước vận hành', N'Tiền điện chạy hệ thống bơm tưới tự động tháng 01/2026', 2800000.00, '2026-02-01', 3);
SET IDENTITY_INSERT [Expense] OFF;
GO

-- 32. HARVEST RECORDS (IDs: 1 to 2)
SET IDENTITY_INSERT [HarvestRecord] ON;
INSERT INTO [HarvestRecord] (harvest_record_id, farm_id, crop_season_id, block_id, farming_activity_id, recorded_by, harvest_date, quantity, unit, quality_grade, notes)
VALUES
(1, 1, 2, 3, 4, 2, '2025-12-20', 1250.000, 'kg', N'Loại 1 (Xuất khẩu)', N'Ớt chuông đỏ đều, quả căng mọng, cuống tươi xanh'),
(2, 1, 2, 3, 4, 2, '2025-12-20', 350.000, 'kg', N'Loại 2 (Nội địa)', N'Ớt hơi cong nhẹ, vị ngọt giòn không đổi');
SET IDENTITY_INSERT [HarvestRecord] OFF;
GO

-- 33. PRODUCT SALES (IDs: 1 to 2)
SET IDENTITY_INSERT [ProductSale] ON;
INSERT INTO [ProductSale] (product_sale_id, farm_id, harvest_record_id, recorded_by, buyer_name, sale_date, quantity, unit, unit_price, notes)
VALUES
(1, 1, 1, 3, N'Hệ Thống Siêu Thị WinMart Miền Nam', '2025-12-21', 1200.000, 'kg', 42000.00, N'Hợp đồng cung ứng nông sản tươi VietGAP'),
(2, 1, 2, 3, N'Chuỗi Thực Phẩm Sạch Bác Tôm Hà Nội', '2025-12-22', 350.000, 'kg', 28000.00, N'Vận chuyển xe lạnh bay trong đêm');
SET IDENTITY_INSERT [ProductSale] OFF;
GO

-- 34. CHECKLIST RULES (IDs: 1 to 3)
SET IDENTITY_INSERT [ChecklistRule] ON;
INSERT INTO [ChecklistRule] (checklist_rule_id, farm_id, created_by, rule_name, description, category, is_active)
VALUES
(1, 1, 2, N'Kiểm soát chỉ số pH và EC hệ thống tưới', N'Kiểm tra nồng độ dinh dưỡng và độ pH của nước tưới trước ca làm việc buổi sáng', N'Tưới tiêu & Dinh dưỡng', 1),
(2, 1, 2, N'Tuân thủ thời gian cách ly thuốc BVTV (PHI)', N'Cách ly tối thiểu 14 ngày trước khi tiến hành hái quả', N'An toàn thực phẩm VietGAP', 1),
(3, 1, 2, N'Trang bị bảo hộ lao động cho công nhân', N'Trang bị đầy đủ găng tay, khẩu trang than hoạt tính và ủng cao su', N'An toàn lao động', 1);
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
(1, 1, 2, 'pass', N'pH đạt 6.35, EC đạt 1.82 mS/cm - Đạt chuẩn'),
(2, 2, 3, 'pass', N'Đã cách ly 18 ngày sau lần phun dầu neem cuối, đủ điều kiện thu hoạch');
SET IDENTITY_INSERT [ChecklistResult] OFF;
GO

-- 37. PRODUCTION REPORTS (ID: 1)
SET IDENTITY_INSERT [ProductionReport] ON;
INSERT INTO [ProductionReport] (production_report_id, farm_id, crop_season_id, generated_by, report_title, report_data)
VALUES
(1, 1, 2, 3, N'Báo cáo tổng kết sản xuất Vụ Ớt Chuông Đông 2025', N'{"season":"Vụ Đông 2025","crop":"Ớt Chuông","planted_area_m2":4000,"total_yield_kg":1600.0,"grade_1_kg":1250.0,"grade_2_kg":350.0,"total_revenue_vnd":60200000,"production_cost_vnd":25800000,"profit_vnd":34400000,"roi_pct":133.3}');
SET IDENTITY_INSERT [ProductionReport] OFF;
GO

-- 38. TRAINING RECORDS (IDs: 1 to 2)
SET IDENTITY_INSERT [TrainingRecord] ON;
INSERT INTO [TrainingRecord] (training_record_id, farm_id, organized_by, training_title, description, training_date)
VALUES
(1, 1, 3, N'Tập huấn kỹ thuật thụ phấn nhân tạo dưa lưới', N'Hướng dẫn thao tác nhổ nhị hoa đực và chấm phấn hoa cái đúng thời điểm', '2026-01-25'),
(2, 1, 3, N'Quy trình thu hoạch và bao bọc bảo vệ trái VietGAP', N'Kỹ thuật cắt cuống và đóng thùng giấy lạnh', '2025-12-15');
SET IDENTITY_INSERT [TrainingRecord] OFF;
GO

-- 39. TRAINING ATTENDEES (Composite PK)
INSERT INTO [TrainingAttendee] (training_record_id, farm_worker_id, attendance_status, notes)
VALUES
(1, 1, 'attended', N'Nắm vững quy trình kỹ thuật'),
(1, 2, 'attended', N'Thực hành đạt yêu cầu'),
(2, 2, 'attended', N'Tham gia trọn buổi'),
(2, 3, 'attended', N'Tham gia trọn buổi');
GO

-- 40. ASSESSMENT CRITERIA (IDs: 1 to 3)
SET IDENTITY_INSERT [AssessmentCriterion] ON;
INSERT INTO [AssessmentCriterion] (assessment_criterion_id, farm_id, criterion_name, description, max_score, is_active)
VALUES
(1, 1, N'Hồ sơ ghi chép nhật ký canh tác', N'Ghi chép đầy đủ các lần tưới phân, phun thuốc và người phụ trách', 100.00, 1),
(2, 1, N'Vệ sinh an toàn khu vực sản xuất', N'Khuôn viên nhà màng sạch sẽ, có lưới chắn côn trùng nguyên vẹn', 100.00, 1),
(3, 1, N'Quản lý kho hóa chất và bao bì', N'Kho thuốc BVTV có khóa, có găng tay bảo hộ và vòi rửa mắt', 100.00, 1);
SET IDENTITY_INSERT [AssessmentCriterion] OFF;
GO

-- 41. INTERNAL ASSESSMENTS (ID: 1)
SET IDENTITY_INSERT [InternalAssessment] ON;
INSERT INTO [InternalAssessment] (internal_assessment_id, farm_id, crop_season_id, zone_id, assessed_by, assessment_title, notes)
VALUES
(1, 1, 1, 1, 3, N'Đánh giá nội bộ tiêu chuẩn VietGAP Nhà màng Quý 1/2026', N'Nông trại duy trì tốt các tiêu chuẩn canh tác sạch');
SET IDENTITY_INSERT [InternalAssessment] OFF;
GO

-- 42. INTERNAL ASSESSMENT ITEMS (IDs: 1 to 3)
SET IDENTITY_INSERT [InternalAssessmentItem] ON;
INSERT INTO [InternalAssessmentItem] (internal_assessment_item_id, internal_assessment_id, assessment_criterion_id, score, result_notes)
VALUES
(1, 1, 1, 96.00, N'Hệ thống phần mềm MyCropDiary lưu vết minh bạch'),
(2, 1, 2, 92.00, N'Lưới chắn côn trùng sạch và kín khít'),
(3, 1, 3, 98.00, N'Tất cả vật tư đều có tem nhãn rõ nguồn gốc xuất xứ');
SET IDENTITY_INSERT [InternalAssessmentItem] OFF;
GO

-- 43. KNOWLEDGE ARTICLES (IDs: 1 to 2)
SET IDENTITY_INSERT [KnowledgeArticle] ON;
INSERT INTO [KnowledgeArticle] (knowledge_article_id, created_by, title, slug, summary, body, cover_image_url, category, tags, view_count, source_url, article_status, published_at)
VALUES
(1, 1, N'Kỹ thuật trồng dưa lưới nhà màng VietGAP', 'ky-thuat-trong-dua-luoi-nha-mang-vietgap', N'Hướng dẫn tổng quan về quy trình chuẩn bị giá thể, chế độ dinh dưỡng và kiểm soát ẩm độ', N'# Kỹ thuật canh tác dưa lưới công nghệ cao VietGAP

## 1. Chuẩn bị giá thể
Xơ dừa cần được ngâm xả chát tannin và lignin kỹ qua 3-4 lần nước ngọt, sau đó ngâm nước vôi 1% trong 24 giờ.

## 2. Quản lý dinh dưỡng nhỏ giọt
- Giai đoạn cây con (ngày 1-15): EC 1.2 - 1.5 mS/cm, pH 5.8 - 6.2.
- Giai đoạn ra hoa thụ phấn (ngày 20-35): EC 1.8 - 2.0 mS/cm.
- Giai đoạn nuôi trái và tạo lưới (ngày 40-60): Tăng tỷ lệ Kali, duy trì EC 2.2 - 2.5 mS/cm.', 'https://images.unsplash.com/photo-1598170845058-32b9d6a5da37', 'seed-cultivation', N'dưa lưới, vietgap, nhà màng', 128, 'https://vietgap.gov.vn', 'published', SYSUTCDATETIME()),
(2, 1, N'Phòng trừ bọ trĩ và rệp phấn trắng bằng giải pháp sinh học', 'phong-tru-bo-tri-bo-phan-trang-bang-sinh-hoc', N'Cẩm nang nhận biết và quản lý dịch hại trên cây dưa lưới không sử dụng hóa chất độc hại', N'# Giải pháp sinh học phòng trừ dịch hại chích hút

Bọ trĩ (Thrips) là đối tượng nguy hiểm nhất gây xoăn đọt non và truyền virus khảm lá.

### Biện pháp canh tác
1. Che chắn nhà màng bằng lưới chắn côn trùng mắt lưới tối thiểu 50 mesh.
2. Treo bẫy dính màu vàng cách mặt ngọn dưa 15-20cm.
3. Phun luân phiên dầu khoáng sinh học và chế phẩm nấm ký sinh Beauveria bassiana.', 'https://images.unsplash.com/photo-1563565375-f3fdfdbefa83', 'fertilizer-pesticide', N'bọ trĩ, sinh học, rệp phấn trắng', 95, 'https://bvtv.gov.vn', 'published', SYSUTCDATETIME());
SET IDENTITY_INSERT [KnowledgeArticle] OFF;
GO

-- 44. AI CONVERSATIONS (ID: 1)
SET IDENTITY_INSERT [AIConversation] ON;
INSERT INTO [AIConversation] (ai_conversation_id, user_id, farm_id, block_id, crop_season_id, conversation_title, conversation_status)
VALUES
(1, 2, 1, 1, 1, N'Tư vấn biểu hiện vàng viền lá trên dưa lưới', 'open');
SET IDENTITY_INSERT [AIConversation] OFF;
GO

-- 45. AI MESSAGES (IDs: 1 to 2)
SET IDENTITY_INSERT [AIMessage] ON;
INSERT INTO [AIMessage] (ai_message_id, ai_conversation_id, sender_type, message_text, model_name)
VALUES
(1, 1, 'user', N'Cây dưa lưới 30 ngày tuổi của tôi ở Lô A1 bị vàng ở mép lá già bên dưới, gân lá vẫn xanh. Đây là dấu hiệu gì và tôi nên xử lý thế nào?', 'gemini-1.5-pro'),
(2, 1, 'assistant', N'Chào bạn! Dựa trên mô tả triệu chứng vàng mép lá nhưng gân lá còn xanh ở tầng lá già, đây là biểu hiện điển hình của tình trạng thiếu Magie (Mg) hoặc mất cân đối tỷ lệ K/Mg trong dung dịch tưới.

Khuyến nghị khắc phục:
1. Kiểm tra lại độ pH của giá thể: pH thích hợp từ 5.8 - 6.2 (nếu pH < 5.5 cây sẽ khó hấp thu Magie).
2. Bổ sung muối Magie Sunfat (MgSO4) hòa tan nồng độ 1g/lít phun qua lá vào sáng sớm.
3. Kiểm tra nồng độ Kali trong bồn mẹ, tránh châm Kali quá cao cạnh tranh hấp thu với Magie.', 'gemini-1.5-pro');
SET IDENTITY_INSERT [AIMessage] OFF;
GO

-- 46. AI FEEDBACK (ID: 1)
SET IDENTITY_INSERT [AIFeedback] ON;
INSERT INTO [AIFeedback] (ai_feedback_id, ai_message_id, user_id, rating, feedback_text)
VALUES
(1, 2, 2, 5, N'Tư vấn rất chính xác và khoa học. Sau 3 ngày bổ sung MgSO4 cây đã hồi phục màu xanh.');
SET IDENTITY_INSERT [AIFeedback] OFF;
GO

-- 47. AI FEEDBACK REPLIES (ID: 1)
SET IDENTITY_INSERT [AIFeedbackReply] ON;
INSERT INTO [AIFeedbackReply] (ai_feedback_reply_id, ai_feedback_id, user_id, reply_text)
VALUES
(1, 1, 1, N'Đội ngũ MyCropDiary rất vui được đồng hành cùng bạn. Chúc trang trại có một vụ mùa bội thu!');
SET IDENTITY_INSERT [AIFeedbackReply] OFF;
GO

SELECT N'MyCropDiary database schema and seed data created successfully.' AS Result;
GO
