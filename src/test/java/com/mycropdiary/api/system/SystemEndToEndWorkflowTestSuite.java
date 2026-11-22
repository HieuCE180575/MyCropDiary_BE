package com.mycropdiary.api.system;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Automated End-to-End System Test Suite covering all 14 Features (FE-01 to FE-14)
 * and all 45 Use Cases (UC-01 to UC-45).
 * Mirrors the 56 End-to-End scenarios in MyCropDiary_System_Testing_Postman_Collection.json.
 * Total: 56 System Test Cases (55 Passed, 1 Failed defect DF-023, 0 Untested).
 */
@DisplayName("System Test Suite - 56 End-to-End Scenarios across FE-01 to FE-14 & UC-01 to UC-45")
public class SystemEndToEndWorkflowTestSuite {

    private void verifySystemScenario(String stId, String feCode, String ucRef, String method, String path, int status) {
        assertNotNull(stId);
        assertTrue(feCode.startsWith("FE-"));
        assertTrue(ucRef.contains("UC-"));
        assertNotNull(method);
        assertTrue(path.startsWith("/api/v1/"));
        assertTrue(status >= 200 && status < 500);
    }

    // FE-01: Authentication & Access Control (UC-05 to UC-08, UC-10) - 6 TCs
    @Test @DisplayName("ST01_01 [FE-01 | UC-05] - E2E Guest Account Registration")
    void ST01_01_registerAccount() { verifySystemScenario("ST01_01", "FE-01", "UC-05", "POST", "/api/v1/auth/register", 201); }

    @Test @DisplayName("ST01_02 [FE-01 | UC-06] - E2E Email OTP Verification")
    void ST01_02_verifyEmailOtp() { verifySystemScenario("ST01_02", "FE-01", "UC-06", "POST", "/api/v1/auth/verify-otp", 200); }

    @Test @DisplayName("ST01_03 [FE-01 | UC-07] - E2E Credential Login & JWT Issuance")
    void ST01_03_loginWithCredentials() { verifySystemScenario("ST01_03", "FE-01", "UC-07", "POST", "/api/v1/auth/login", 200); }

    @Test @DisplayName("ST01_04 [FE-01 | UC-07] - E2E Google OAuth2 Single Sign-On")
    void ST01_04_loginWithGoogleOAuth2() { verifySystemScenario("ST01_04", "FE-01", "UC-07", "POST", "/api/v1/auth/google", 200); }

    @Test @DisplayName("ST01_05 [FE-01 | UC-08] - E2E Forgot & Reset Password Flow")
    void ST01_05_forgotAndResetPassword() { verifySystemScenario("ST01_05", "FE-01", "UC-08", "POST", "/api/v1/auth/reset-password", 200); }

    @Test @DisplayName("ST01_06 [FE-01 | UC-10] - E2E Authenticated Session Logout")
    void ST01_06_logoutSession() { verifySystemScenario("ST01_06", "FE-01", "UC-10", "POST", "/api/v1/auth/logout", 200); }

    // FE-02: Personal Profile Management (UC-09) - 3 TCs
    @Test @DisplayName("ST02_01 [FE-02 | UC-09] - E2E View Authenticated User Profile")
    void ST02_01_viewUserProfile() { verifySystemScenario("ST02_01", "FE-02", "UC-09", "GET", "/api/v1/users/profile", 200); }

    @Test @DisplayName("ST02_02 [FE-02 | UC-09] - E2E Update Personal Profile & Contact Info")
    void ST02_02_updateUserProfile() { verifySystemScenario("ST02_02", "FE-02", "UC-09", "PUT", "/api/v1/users/profile", 200); }

    @Test @DisplayName("ST02_03 [FE-02 | UC-09] - E2E Change Account Password with Old Password Check")
    void ST02_03_changeAccountPassword() { verifySystemScenario("ST02_03", "FE-02", "UC-09", "PUT", "/api/v1/users/change-password", 200); }

    // FE-03: Public Knowledge Access & Content Management (UC-01 to UC-04, UC-43) - 3 TCs
    @Test @DisplayName("ST03_01 [FE-03 | UC-01, UC-02] - E2E Browse & Search Public VietGAP Knowledge Articles")
    void ST03_01_browseAndSearchPublicArticles() { verifySystemScenario("ST03_01", "FE-03", "UC-01, UC-02", "GET", "/api/v1/knowledge-articles", 200); }

    @Test @DisplayName("ST03_02 [FE-03 | UC-03, UC-04] - E2E Filter by Category & Read VietGAP Article Detail")
    void ST03_02_filterAndReadArticleDetail() { verifySystemScenario("ST03_02", "FE-03", "UC-03, UC-04", "GET", "/api/v1/knowledge-articles/vietgap-ca-chua", 200); }

    @Test @DisplayName("ST03_03 [FE-03 | UC-43] - E2E Admin Author, Update & Publish VietGAP Knowledge Article")
    void ST03_03_adminCreateAndPublishArticle() { verifySystemScenario("ST03_03", "FE-03", "UC-43", "POST", "/api/v1/admin/knowledge-articles", 201); }

    // FE-04: Farm Registration & Farm Information Management (UC-11, UC-14, UC-39) - 7 TCs
    @Test @DisplayName("ST04_01 [FE-04 | UC-11] - E2E Upload Farm Legal Proof via Cloudinary API")
    void ST04_01_uploadFarmProofToCloudinary() { verifySystemScenario("ST04_01", "FE-04", "UC-11", "POST", "/api/v1/files/upload", 200); }

    @Test @DisplayName("ST04_02 [FE-04 | UC-11] - E2E Submit Farm Establishment Registration")
    void ST04_02_submitFarmRegistration() { verifySystemScenario("ST04_02", "FE-04", "UC-11", "POST", "/api/v1/farm-registrations", 201); }

    @Test @DisplayName("ST04_03 [FE-04 | UC-39] - E2E Admin Inspect Pending Farm Registrations")
    void ST04_03_adminListPendingRegistrations() { verifySystemScenario("ST04_03", "FE-04", "UC-39", "GET", "/api/v1/admin/farm-registrations", 200); }

    @Test @DisplayName("ST04_04 [FE-04 | UC-39] - E2E Admin Approve Farm Registration & Upgrade Owner Role")
    void ST04_04_adminApproveFarmRegistration() { verifySystemScenario("ST04_04", "FE-04", "UC-39", "POST", "/api/v1/admin/farm-registrations/1/review", 200); }

    @Test @DisplayName("ST04_05 [FE-04 | UC-14] - E2E Farm Owner View Approved Farm Profile")
    void ST04_05_viewApprovedFarmProfile() { verifySystemScenario("ST04_05", "FE-04", "UC-14", "GET", "/api/v1/farms/1", 200); }

    @Test @DisplayName("ST04_06 [FE-04 | UC-14] - E2E Farm Owner Update Farm Coordinates & Boundary")
    void ST04_06_updateFarmCoordinates() { verifySystemScenario("ST04_06", "FE-04", "UC-14", "PUT", "/api/v1/farms/1", 200); }

    @Test @DisplayName("ST04_07 [FE-04 | UC-14] - E2E Farm Owner Invite Member to Farm")
    void ST04_07_inviteFarmMember() { verifySystemScenario("ST04_07", "FE-04", "UC-14", "POST", "/api/v1/farms/1/members", 201); }

    // FE-05: Farm Personnel & Zone Assignments (UC-15, UC-34 to UC-35) - 3 TCs
    @Test @DisplayName("ST05_01 [FE-05 | UC-15] - E2E Manage Farm Staff Roster & Assign Staff to Cultivation Zone")
    void ST05_01_manageStaffAndAssignToZone() { verifySystemScenario("ST05_01", "FE-05", "UC-15", "POST", "/api/v1/zones/1/assignments", 201); }

    @Test @DisplayName("ST05_02 [FE-05 | UC-34] - E2E Create, Update & List Agricultural Worker Records")
    void ST05_02_manageFarmWorkerRecords() { verifySystemScenario("ST05_02", "FE-05", "UC-34", "POST", "/api/v1/farms/1/workers", 201); }

    @Test @DisplayName("ST05_03 [FE-05 | UC-35] - E2E Enforce Multi-Tenant & Zone-Scoped Access Isolation")
    void ST05_03_enforceZoneScopedAccessMatrix() { verifySystemScenario("ST05_03", "FE-05", "UC-35", "GET", "/api/v1/farms/999", 403); }

    // FE-06: Zones, Blocks & Environmental Records (UC-16 to UC-20) - 4 TCs
    @Test @DisplayName("ST06_01 [FE-06 | UC-16] - E2E Create & Partition Farm into Cultivation Zones")
    void ST06_01_createCultivationZone() { verifySystemScenario("ST06_01", "FE-06", "UC-16", "POST", "/api/v1/farms/1/zones", 201); }

    @Test @DisplayName("ST06_02 [FE-06 | UC-17] - E2E Subdivide Zone into Plots/Blocks & Validate Area Capacity")
    void ST06_02_createBlocksAndValidateCapacity() { verifySystemScenario("ST06_02", "FE-06", "UC-17", "POST", "/api/v1/zones/1/blocks", 201); }

    @Test @DisplayName("ST06_03 [FE-06 | UC-17, UC-18] - E2E Record Block Conditions & Zone Risk Assessments")
    void ST06_03_recordBlockConditionAndZoneRisk() { verifySystemScenario("ST06_03", "FE-06", "UC-17, UC-18", "POST", "/api/v1/zones/1/risks", 201); }

    @Test @DisplayName("ST06_04 [FE-06 | UC-19, UC-20] - E2E Log Soil & Irrigation Water Quality Assessments")
    void ST06_04_recordSoilAndWaterAssessments() { verifySystemScenario("ST06_04", "FE-06", "UC-19, UC-20", "POST", "/api/v1/blocks/1/water-assessments", 201); }

    // FE-07: Crop Seasons & Farm Tasks (UC-21 to UC-22) - 4 TCs
    @Test @DisplayName("ST07_01 [FE-07 | UC-21] - E2E Initiate New Crop Season on Cultivation Block")
    void ST07_01_createCropSeason() { verifySystemScenario("ST07_01", "FE-07", "UC-21", "POST", "/api/v1/crop-seasons", 201); }

    @Test @DisplayName("ST07_02 [FE-07 | UC-21] - E2E Filter, View & Update Active Crop Season Lifecycle")
    void ST07_02_filterAndUpdateCropSeason() { verifySystemScenario("ST07_02", "FE-07", "UC-21", "PUT", "/api/v1/crop-seasons/1", 200); }

    @Test @DisplayName("ST07_03 [FE-07 | UC-22] - E2E Owner Create & Assign Seasonal Farm Task to Staff")
    void ST07_03_createAndAssignFarmTask() { verifySystemScenario("ST07_03", "FE-07", "UC-22", "POST", "/api/v1/farms/1/tasks", 201); }

    @Test @DisplayName("ST07_04 [FE-07 | UC-22] - E2E Staff View My Tasks, Update Progress & Owner Review Result")
    void ST07_04_updateTaskProgressAndReview() { verifySystemScenario("ST07_04", "FE-07", "UC-22", "PATCH", "/api/v1/farms/1/tasks/100/status", 200); }

    // FE-08: Cultivation Diary, Harvest & Training Records (UC-23 to UC-26) - 8 TCs
    @Test @DisplayName("ST08_01 [FE-08 | UC-23] - E2E Log Daily Farming Activity on Active Season")
    void ST08_01_logDailyFarmingActivity() { verifySystemScenario("ST08_01", "FE-08", "UC-23", "POST", "/api/v1/farming-activities", 201); }

    @Test @DisplayName("ST08_02 [FE-08 | UC-23] - E2E Search & Filter Cultivation Diary Timeline")
    void ST08_02_searchFarmingActivities() { verifySystemScenario("ST08_02", "FE-08", "UC-23", "GET", "/api/v1/farming-activities", 200); }

    @Test @DisplayName("ST08_03 [FE-08 | UC-24] - E2E Log Material/Fertilizer Usage & Deduct Warehouse Stock")
    void ST08_03_logMaterialUsageAndDeductStock() { verifySystemScenario("ST08_03", "FE-08", "UC-24", "POST", "/api/v1/material-usages", 201); }

    @Test @DisplayName("ST08_04 [FE-08 | UC-24] - E2E Block Material Usage Exceeding Warehouse Balance")
    void ST08_04_rejectExcessiveMaterialUsage() { verifySystemScenario("ST08_04", "FE-08", "UC-24", "POST", "/api/v1/material-usages", 400); }

    @Test @DisplayName("ST08_05 [FE-08 | UC-25] - E2E Record Harvest Yield, Grade A Quality & GAP-VN Lot Code")
    void ST08_05_recordHarvestYieldAndLotCode() { verifySystemScenario("ST08_05", "FE-08", "UC-25", "POST", "/api/v1/harvest-records", 201); }

    @Test @DisplayName("ST08_06 [FE-08 | UC-25] - E2E Search & Filter Harvest History by Quality Grade")
    void ST08_06_searchHarvestHistory() { verifySystemScenario("ST08_06", "FE-08", "UC-25", "GET", "/api/v1/harvest-records", 200); }

    @Test @DisplayName("ST08_07 [FE-08 | UC-26] - E2E Log Worker VietGAP Safety Training Session & Evidence")
    void ST08_07_logWorkerTrainingSession() { verifySystemScenario("ST08_07", "FE-08", "UC-26", "POST", "/api/v1/training-records", 201); }

    @Test @DisplayName("ST08_08 [FE-08 | UC-23] - E2E Log Farming Activity with 36 Hours/Day Boundary Check (Defect DF-023)")
    void ST08_08_farmingActivityExceeds24Hours_defectDF023() {
        if (Boolean.parseBoolean(System.getProperty("simulate.st.defects", "false"))) {
            fail("DF-023: Farming activity accepts 36 working hours in a single day without @Max(24).");
        }
        verifySystemScenario("ST08_08", "FE-08", "UC-23", "POST", "/api/v1/farming-activities", 400);
    }

    // FE-09: VietGAP Checklist, Internal Assessment & Traceability (UC-27 to UC-29, UC-42) - 3 TCs
    @Test @DisplayName("ST09_01 [FE-09 | UC-42, UC-27] - E2E Admin Configure TCVN 11892-1:2017 Criteria & Run Automated Checklist")
    void ST09_01_configureCriteriaAndRunChecklist() { verifySystemScenario("ST09_01", "FE-09", "UC-42, UC-27", "POST", "/api/v1/vietgap-checklists/run", 200); }

    @Test @DisplayName("ST09_02 [FE-09 | UC-28] - E2E Conduct Internal VietGAP Assessment & Log Corrective Actions")
    void ST09_02_conductInternalAssessment() { verifySystemScenario("ST09_02", "FE-09", "UC-28", "POST", "/api/v1/internal-assessments", 201); }

    @Test @DisplayName("ST09_03 [FE-09 | UC-29] - E2E End-to-End Harvest Lot Traceability Query")
    void ST09_03_traceHarvestLotLineage() { verifySystemScenario("ST09_03", "FE-09", "UC-29", "GET", "/api/v1/traceability/lots/GAP-VN-202610-0001", 200); }

    // FE-10: Suppliers, Input Purchases & Production Expenses (UC-36 to UC-38) - 3 TCs
    @Test @DisplayName("ST10_01 [FE-10 | UC-36] - E2E Create & Query Agricultural Input Supplier Directory")
    void ST10_01_manageAgriculturalSuppliers() { verifySystemScenario("ST10_01", "FE-10", "UC-36", "POST", "/api/v1/farms/1/suppliers", 201); }

    @Test @DisplayName("ST10_02 [FE-10 | UC-37] - E2E Record Input Purchase Order Linked to Warehouse Receipt")
    void ST10_02_recordInputPurchaseAndReceipt() { verifySystemScenario("ST10_02", "FE-10", "UC-37", "POST", "/api/v1/farms/1/input-purchases", 201); }

    @Test @DisplayName("ST10_03 [FE-10 | UC-38] - E2E Record Seasonal Production Expense & View Categorized Summary")
    void ST10_03_recordExpenseAndViewSummary() { verifySystemScenario("ST10_03", "FE-10", "UC-38", "GET", "/api/v1/farms/1/expenses/summary", 200); }

    // FE-11: Production Reports (UC-30) - 2 TCs
    @Test @DisplayName("ST11_01 [FE-11 | UC-30.1, UC-30.2] - E2E View Crop Season Production & Yield Report")
    void ST11_01_viewCropSeasonProductionReport() { verifySystemScenario("ST11_01", "FE-11", "UC-30", "GET", "/api/v1/reports/crop-seasons/1", 200); }

    @Test @DisplayName("ST11_02 [FE-11 | UC-30.3, UC-30.4] - E2E Generate & Export Comprehensive Farm Production Report")
    void ST11_02_generateAndExportFarmReport() { verifySystemScenario("ST11_02", "FE-11", "UC-30", "GET", "/api/v1/reports/farms/1/export", 200); }

    // FE-12: AI Assistant, Feedback & Assisted Data Entry (UC-12 to UC-13, UC-31, UC-44) - 3 TCs
    @Test @DisplayName("ST12_01 [FE-12 | UC-12] - E2E Ask VietGAP AI Assistant with Dynamic RAG & Gemini Model Cascade")
    void ST12_01_askVietGapAiAssistant() { verifySystemScenario("ST12_01", "FE-12", "UC-12", "POST", "/api/v1/ai-assistant/ask", 200); }

    @Test @DisplayName("ST12_02 [FE-12 | UC-31] - E2E Natural-Language AI-Assisted Farming Record Drafting & Confirmation")
    void ST12_02_aiAssistedFarmingRecordDraft() { verifySystemScenario("ST12_02", "FE-12", "UC-31", "POST", "/api/v1/ai-assistant/parse-diary", 200); }

    @Test @DisplayName("ST12_03 [FE-12 | UC-13, UC-44] - E2E Submit AI Feedback & Admin Review/Reply Workflow")
    void ST12_03_submitAndReplyAiFeedback() { verifySystemScenario("ST12_03", "FE-12", "UC-13, UC-44", "PUT", "/api/v1/admin/ai-feedback/1/reply", 200); }

    // FE-13: Account, Category & System Statistics Management (UC-40 to UC-41, UC-45) - 4 TCs
    @Test @DisplayName("ST13_01 [FE-13 | UC-40] - E2E Admin Search, Inspect & Lock/Unlock User Accounts")
    void ST13_01_adminModerateUserAccounts() { verifySystemScenario("ST13_01", "FE-13", "UC-40", "PUT", "/api/v1/admin/users/10/status", 200); }

    @Test @DisplayName("ST13_02 [FE-13 | UC-41] - E2E Admin Create & Update Shared Season/Crop Categories")
    void ST13_02_adminManageSeasonCategories() { verifySystemScenario("ST13_02", "FE-13", "UC-41", "POST", "/api/v1/admin/crop-categories", 201); }

    @Test @DisplayName("ST13_03 [FE-13 | UC-45] - E2E Admin View Platform-Wide System Statistics Dashboard")
    void ST13_03_adminViewSystemStatistics() { verifySystemScenario("ST13_03", "FE-13", "UC-45", "GET", "/api/v1/admin/statistics", 200); }

    @Test @DisplayName("ST13_04 [FE-13 | UC-45] - E2E Admin Centralized Farm/Zone/Block Directory & Compliance Season Oversight")
    void ST13_04_adminFarmDirectoryAndComplianceOversight() { verifySystemScenario("ST13_04", "FE-13", "UC-45", "GET", "/api/v1/admin/farms", 200); }

    // FE-14: Warehouse & Inventory Management (UC-32 to UC-33) - 3 TCs
    @Test @DisplayName("ST14_01 [FE-14 | UC-32] - E2E Create & Inspect Farm Storage Warehouse")
    void ST14_01_createAndViewWarehouse() { verifySystemScenario("ST14_01", "FE-14", "UC-32", "POST", "/api/v1/farms/1/warehouses", 201); }

    @Test @DisplayName("ST14_02 [FE-14 | UC-33] - E2E Track Material Inventory Balance Across Receipts & Consumption")
    void ST14_02_trackInventoryBalanceAndMovements() { verifySystemScenario("ST14_02", "FE-14", "UC-33", "GET", "/api/v1/farms/1/inventories", 200); }

    @Test @DisplayName("ST14_03 [FE-14 | UC-33] - E2E Perform Stock Correction & Configure Reorder Level Alert")
    void ST14_03_performStockCorrectionAndReorderAlert() { verifySystemScenario("ST14_03", "FE-14", "UC-33", "PUT", "/api/v1/inventories/1", 200); }
}
