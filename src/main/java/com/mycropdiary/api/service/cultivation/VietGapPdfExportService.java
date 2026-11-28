package com.mycropdiary.api.service.cultivation;

import com.lowagie.text.*;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.mycropdiary.api.dto.vietgap.ChecklistResultItemRespDTO;
import com.mycropdiary.api.dto.vietgap.ChecklistResultRespDTO;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

@Service
public class VietGapPdfExportService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy")
            .withZone(ZoneId.of("Asia/Ho_Chi_Minh"));

    // Brand Colors (VietGAP Agricultural Green & Clean Official SaaS Colors)
    private static final Color COLOR_PRIMARY_GREEN = new Color(16, 128, 67);     // #108043 (Xanh lá chuẩn VietGAP)
    private static final Color COLOR_HEADER_BG = new Color(241, 245, 249);        // #f1f5f9 (Nền thead chuẩn)
    private static final Color COLOR_CAT_BG = new Color(236, 253, 245);           // #ecfdf5 (Nền phân nhóm)
    private static final Color COLOR_BORDER = new Color(148, 163, 184);           // #94a3b8 (Viền bảng rõ ràng, sắc nét)
    private static final Color COLOR_TEXT_DARK = new Color(15, 23, 42);           // #0f172a
    private static final Color COLOR_TEXT_MUTED = new Color(71, 85, 105);         // #475569

    public byte[] generateAuditReportPdf(ChecklistResultRespDTO run) {
        // Khổ A4, lề chuẩn hành chính: Trái 36pt, Phải 36pt, Trên 36pt, Dưới 36pt
        Document document = new Document(PageSize.A4, 36, 36, 36, 36);
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try {
            PdfWriter.getInstance(document, out);
            document.open();

            // Cấu hình Font tiếng Việt có dấu chuẩn Unicode
            BaseFont baseFont;
            BaseFont baseFontBold;
            BaseFont baseFontItalic;
            try {
                String arialPath = "C:/Windows/Fonts/arial.ttf";
                String arialBdPath = "C:/Windows/Fonts/arialbd.ttf";
                String arialItPath = "C:/Windows/Fonts/ariali.ttf";
                if (!new File(arialPath).exists()) {
                    arialPath = "C:/Windows/Fonts/times.ttf";
                    arialBdPath = "C:/Windows/Fonts/timesbd.ttf";
                    arialItPath = "C:/Windows/Fonts/timesi.ttf";
                }
                baseFont = BaseFont.createFont(arialPath, BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
                baseFontBold = BaseFont.createFont(arialBdPath, BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
                baseFontItalic = BaseFont.createFont(arialItPath, BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
            } catch (Exception ex) {
                baseFont = BaseFont.createFont(BaseFont.HELVETICA, BaseFont.WINANSI, BaseFont.NOT_EMBEDDED);
                baseFontBold = BaseFont.createFont(BaseFont.HELVETICA_BOLD, BaseFont.WINANSI, BaseFont.NOT_EMBEDDED);
                baseFontItalic = BaseFont.createFont(BaseFont.HELVETICA_OBLIQUE, BaseFont.WINANSI, BaseFont.NOT_EMBEDDED);
            }

            Font fontTitle = new Font(baseFontBold, 13, Font.BOLD, COLOR_PRIMARY_GREEN);
            Font fontSubTitle = new Font(baseFontBold, 10, Font.BOLD, COLOR_TEXT_DARK);
            Font fontLegal = new Font(baseFontItalic, 8.5f, Font.ITALIC, COLOR_TEXT_MUTED);
            Font fontInfo = new Font(baseFont, 9f, Font.NORMAL, COLOR_TEXT_DARK);
            Font fontInfoBold = new Font(baseFontBold, 9f, Font.BOLD, COLOR_TEXT_DARK);
            Font fontTh = new Font(baseFontBold, 8.5f, Font.BOLD, COLOR_TEXT_DARK);
            Font fontTd = new Font(baseFont, 8f, Font.NORMAL, COLOR_TEXT_DARK);
            Font fontTdBold = new Font(baseFontBold, 8f, Font.BOLD, COLOR_TEXT_DARK);
            Font fontCatHeader = new Font(baseFontBold, 8.5f, Font.BOLD, COLOR_PRIMARY_GREEN);

            // ─── 1. DECORATIVE TOP ACCENT BAR ───
            PdfPTable topBar = new PdfPTable(1);
            topBar.setWidthPercentage(100);
            PdfPCell barCell = new PdfPCell(new Phrase(" "));
            barCell.setBackgroundColor(COLOR_PRIMARY_GREEN);
            barCell.setBorder(Rectangle.NO_BORDER);
            barCell.setFixedHeight(4f);
            topBar.addCell(barCell);
            document.add(topBar);

            document.add(new Paragraph(" ", new Font(baseFont, 4)));

            // ─── 1.1 QUỐC HIỆU & TIÊU NGỮ ───
            Paragraph quocHieu = new Paragraph("CỘNG HÒA XÃ HỘI CHỦ NGHĨA VIỆT NAM", fontSubTitle);
            quocHieu.setAlignment(Element.ALIGN_CENTER);
            document.add(quocHieu);

            Paragraph tieuNgu = new Paragraph("Độc lập - Tự do - Hạnh phúc", fontSubTitle);
            tieuNgu.setAlignment(Element.ALIGN_CENTER);
            tieuNgu.setSpacingAfter(2f);
            document.add(tieuNgu);

            Paragraph decorLine = new Paragraph("-----------------***-----------------", fontLegal);
            decorLine.setAlignment(Element.ALIGN_CENTER);
            decorLine.setSpacingAfter(10f);
            document.add(decorLine);

            // ─── 2. REPORT HEADER ───
            Paragraph titlePara = new Paragraph("BẢNG KIỂM TRA ĐÁNH GIÁ NỘI BỘ VIETGAP", fontTitle);
            titlePara.setAlignment(Element.ALIGN_CENTER);
            titlePara.setSpacingAfter(2f);
            document.add(titlePara);

            Paragraph subTitlePara = new Paragraph("(Dùng để kiểm tra, đánh giá nội bộ theo mùa vụ canh tác rau màu / năm)", fontSubTitle);
            subTitlePara.setAlignment(Element.ALIGN_CENTER);
            subTitlePara.setSpacingAfter(4f);
            document.add(subTitlePara);

            Paragraph legalPara = new Paragraph(
                    "(Căn cứ Tiêu chuẩn Quốc gia TCVN 11892-1:2017 & Lộ trình cập nhật TCVN 11892-1:2026 - VietGAP Trồng trọt)",
                    fontLegal
            );
            legalPara.setAlignment(Element.ALIGN_CENTER);
            legalPara.setSpacingAfter(10f);
            document.add(legalPara);

            // ─── 3. METADATA SECTION (Theo đúng chuẩn mẫu biểu hành chính) ───
            PdfPTable metaTable = new PdfPTable(2);
            metaTable.setWidthPercentage(100);
            metaTable.setWidths(new float[]{3.2f, 2.8f});
            metaTable.setSpacingAfter(10f);

            String farmInfo = run.seasonName() != null ? run.seasonName() : "Trang trại áp dụng quy chuẩn VietGAP";
            String blockInfo = run.blockName() != null && !run.blockName().isBlank() ? run.blockName() : "Toàn bộ khu vực sản xuất";
            String inspectorInfo = run.assignedToName() != null && !run.assignedToName().isBlank()
                    ? run.assignedToName()
                    : "Ban kiểm soát nội bộ";
            String auditDate = run.completedAt() != null ? DATE_FMT.format(run.completedAt())
                    : (run.scheduledAt() != null ? DATE_FMT.format(run.scheduledAt())
                    : (run.createdAt() != null ? DATE_FMT.format(run.createdAt()) : "........................."));

            String checklistVersion = run.checklistVersion() != null && !run.checklistVersion().isBlank()
                    ? run.checklistVersion()
                    : "V1";

            addMetaLine(metaTable, "Thông tin về cơ sở / người sản xuất:", farmInfo, fontInfoBold, fontInfo);
            addMetaLine(metaTable, "Địa điểm / Lô sản xuất:", blockInfo, fontInfoBold, fontInfo);
            addMetaLine(metaTable, "Thông tin về Tổ kiểm tra:", inspectorInfo, fontInfoBold, fontInfo);
            addMetaLine(metaTable, "Ngày đánh giá:", auditDate, fontInfoBold, fontInfo);
            addMetaLine(metaTable, "Phiên bản bộ tiêu chí:", checklistVersion, fontInfoBold, fontInfo);
            addMetaLine(metaTable, "Mã đợt đánh giá:", run.runId() != null ? ("#" + run.runId()) : "-", fontInfoBold, fontInfo);

            document.add(metaTable);

            // ─── 4. CHECKLIST CRITERIA TABLE (6 CỘT CHUẨN FORM BỘ NN&PTNT) ───
            PdfPTable table = new PdfPTable(6);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{0.5f, 4.9f, 0.9f, 0.8f, 0.9f, 2.8f});
            table.setHeaderRows(2); // Giữ thead 2 tầng khi sang trang mới

            // Thead Tầng 1:
            table.addCell(createHeaderCell("TT", fontTh, 1, 2));
            table.addCell(createHeaderCell("Chỉ tiêu", fontTh, 1, 2));
            table.addCell(createHeaderCell("Yêu cầu", fontTh, 1, 2));
            table.addCell(createHeaderCell("Kết quả đánh giá", fontTh, 2, 1));
            table.addCell(createHeaderCell("Diễn giải / Minh chứng", fontTh, 1, 2));

            // Thead Tầng 2:
            table.addCell(createHeaderCell("Đạt", fontTh, 1, 1));
            table.addCell(createHeaderCell("Không đạt", fontTh, 1, 1));

            // Duyệt từng tiêu chí và kết xuất dữ liệu
            if (run.results() != null && !run.results().isEmpty()) {
                int index = 1;
                String currentCat = "";

                for (ChecklistResultItemRespDTO item : run.results()) {
                    String cat = item.category() != null ? item.category() : "TIÊU CHUẨN CHUNG";
                    if (!cat.equals(currentCat)) {
                        currentCat = cat;
                        PdfPCell catCell = new PdfPCell(new Phrase(formatCategoryTitle(currentCat), fontCatHeader));
                        catCell.setColspan(6);
                        catCell.setBackgroundColor(COLOR_CAT_BG);
                        catCell.setBorderColor(COLOR_BORDER);
                        catCell.setBorderWidth(1f);
                        catCell.setPadding(6f);
                        catCell.setHorizontalAlignment(Element.ALIGN_LEFT);
                        table.addCell(catCell);
                    }

                    // 1. TT
                    PdfPCell cellIndex = new PdfPCell(new Phrase(String.valueOf(index++), fontTd));
                    cellIndex.setHorizontalAlignment(Element.ALIGN_CENTER);
                    cellIndex.setVerticalAlignment(Element.ALIGN_MIDDLE);
                    cellIndex.setBorderColor(COLOR_BORDER);
                    cellIndex.setPadding(5f);
                    table.addCell(cellIndex);

                    // 2. Chỉ tiêu kiểm tra (In thuần túy nội dung tiêu chí theo đúng biểu mẫu cơ quan quản lý)
                    String criteriaText = item.ruleName() != null ? item.ruleName() : "Tiêu chí kiểm định VietGAP";
                    PdfPCell cellCriteria = new PdfPCell(new Phrase(criteriaText, fontTd));
                    cellCriteria.setBorderColor(COLOR_BORDER);
                    cellCriteria.setPadding(5f);
                    cellCriteria.setVerticalAlignment(Element.ALIGN_MIDDLE);
                    table.addCell(cellCriteria);

                    // 3. Yêu cầu (Mức độ A: Bắt buộc tuân thủ 100%, Mức độ B: Khuyến khích)
                    String level = "A";
                    if (item.ruleCode() != null && (item.ruleCode().startsWith("VG-CS-03") || item.ruleCode().startsWith("VG-AT-04"))) {
                        level = "B";
                    }
                    PdfPCell cellLevel = new PdfPCell(new Phrase(level, fontTd));
                    cellLevel.setHorizontalAlignment(Element.ALIGN_CENTER);
                    cellLevel.setVerticalAlignment(Element.ALIGN_MIDDLE);
                    cellLevel.setBorderColor(COLOR_BORDER);
                    cellLevel.setPadding(5f);
                    table.addCell(cellLevel);

                    // 4 & 5. Kết quả (Đạt / Không đạt)
                    boolean isPass = "pass".equalsIgnoreCase(item.resultStatus());
                    boolean isFail = "fail".equalsIgnoreCase(item.resultStatus());

                    PdfPCell cellPass = new PdfPCell(new Phrase(isPass ? "X" : "", fontTdBold));
                    cellPass.setHorizontalAlignment(Element.ALIGN_CENTER);
                    cellPass.setVerticalAlignment(Element.ALIGN_MIDDLE);
                    cellPass.setBorderColor(COLOR_BORDER);
                    cellPass.setPadding(5f);
                    if (isPass) {
                        cellPass.setBackgroundColor(new Color(240, 253, 244));
                    }
                    table.addCell(cellPass);

                    PdfPCell cellFail = new PdfPCell(new Phrase(isFail ? "X" : "", fontTdBold));
                    cellFail.setHorizontalAlignment(Element.ALIGN_CENTER);
                    cellFail.setVerticalAlignment(Element.ALIGN_MIDDLE);
                    cellFail.setBorderColor(COLOR_BORDER);
                    cellFail.setPadding(5f);
                    if (isFail) {
                        cellFail.setBackgroundColor(new Color(254, 242, 242));
                    }
                    table.addCell(cellFail);

                    // 6. Diễn giải / Minh chứng
                    String notes = item.notes() != null && !item.notes().isBlank() ? item.notes() : "";
                    if (item.evidenceImageUrl() != null && !item.evidenceImageUrl().isBlank()) {
                        notes = notes.isBlank() ? "Có ảnh minh chứng đính kèm" : notes + " (Có ảnh minh chứng)";
                    }
                    if ("not_applicable".equalsIgnoreCase(item.resultStatus())) {
                        notes = notes.isBlank() ? "Không áp dụng" : "K.Áp dụng: " + notes;
                    }
                    PdfPCell cellNotes = new PdfPCell(new Phrase(notes, fontTd));
                    cellNotes.setBorderColor(COLOR_BORDER);
                    cellNotes.setPadding(5f);
                    cellNotes.setVerticalAlignment(Element.ALIGN_MIDDLE);
                    table.addCell(cellNotes);
                }
            }

            document.add(table);
            document.add(new Paragraph(" ", new Font(baseFont, 8)));

            // ─── 5. SUMMARY & CONCLUSION BOX ───
            int total = run.totalItems() != null ? run.totalItems() : (run.results() != null ? run.results().size() : 0);
            int passed = run.passedItems() != null ? run.passedItems() : 0;
            int failed = run.failedItems() != null ? run.failedItems() : 0;
            Double pct = run.compliancePercent() != null ? run.compliancePercent() : 0.0;

            PdfPTable summaryBox = new PdfPTable(1);
            summaryBox.setWidthPercentage(100);
            PdfPCell sumCell = new PdfPCell();
            sumCell.setBackgroundColor(new Color(248, 250, 252));
            sumCell.setBorderColor(COLOR_BORDER);
            sumCell.setPadding(8f);

            Paragraph sumPara = new Paragraph();
            sumPara.add(new Chunk("TỔNG HỢP KẾT QUẢ ĐÁNH GIÁ:\n", fontInfoBold));
            sumPara.add(new Chunk("• Tổng số tiêu chí đánh giá: " + total + " tiêu chí | ", fontInfo));
            sumPara.add(new Chunk("Đạt: " + passed + " tiêu chí | ", fontInfoBold));
            sumPara.add(new Chunk("Không đạt: " + failed + " tiêu chí | ", fontInfo));
            sumPara.add(new Chunk("Tỷ lệ tuân thủ đạt: " + Math.round(pct) + "%\n", fontInfoBold));

            String conclusionText = pct >= 97.0 && failed <= 1
                    ? "• KẾT LUẬN: Cơ sở sản xuất ĐỦ ĐIỀU KIỆN ĐẠT CHUẨN VIETGAP theo quy định tại TCVN 11892-1:2017 & lộ trình TCVN 11892-1:2026 (Đạt tối thiểu 100% chỉ tiêu Mức A và >= 80% chỉ tiêu Mức B)."
                    : "• KẾT LUẬN: Cơ sở sản xuất cần tiếp tục khắc phục các tiêu chí Chưa đạt (" + failed + " tiêu chí) trước khi nộp hồ sơ đánh giá chứng nhận chính thức.";
            sumPara.add(new Chunk(conclusionText, fontInfoBold));

            sumCell.addElement(sumPara);
            summaryBox.addCell(sumCell);
            document.add(summaryBox);

            document.add(new Paragraph(" ", new Font(baseFont, 12)));

            // ─── 6. SIGNATURE SECTION ───
            PdfPTable signTable = new PdfPTable(2);
            signTable.setWidthPercentage(100);
            signTable.setWidths(new float[]{1f, 1f});

            PdfPCell signLeft = new PdfPCell();
            signLeft.setBorder(Rectangle.NO_BORDER);
            signLeft.setHorizontalAlignment(Element.ALIGN_CENTER);
            Paragraph pLeft = new Paragraph("ĐẠI DIỆN CƠ SỞ / NGƯỜI SẢN XUẤT\n(Ký và ghi rõ họ tên)\n\n\n\n\n\n...........................................................", fontInfoBold);
            pLeft.setAlignment(Element.ALIGN_CENTER);
            signLeft.addElement(pLeft);
            signTable.addCell(signLeft);

            PdfPCell signRight = new PdfPCell();
            signRight.setBorder(Rectangle.NO_BORDER);
            signRight.setHorizontalAlignment(Element.ALIGN_CENTER);
            String auditorName = run.assignedToName() != null ? run.assignedToName() : "...........................................................";
            Paragraph pRight = new Paragraph("ĐẠI DIỆN TỔ KIỂM TRA ĐÁNH GIÁ\n(Ký và ghi rõ họ tên)\n\n\n\n\n\n" + auditorName, fontInfoBold);
            pRight.setAlignment(Element.ALIGN_CENTER);
            signRight.addElement(pRight);
            signTable.addCell(signRight);

            document.add(signTable);

            document.close();
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Lỗi khi tạo báo cáo PDF VietGAP: " + e.getMessage(), e);
        }
    }

    private PdfPCell createHeaderCell(String text, Font font, int colspan, int rowspan) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBackgroundColor(COLOR_HEADER_BG);
        cell.setBorderColor(COLOR_BORDER);
        cell.setColspan(colspan);
        cell.setRowspan(rowspan);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setPadding(6f);
        return cell;
    }

    private void addMetaLine(PdfPTable table, String label, String value, Font labelFont, Font valFont) {
        PdfPCell cell = new PdfPCell();
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setPadding(2f);
        Paragraph p = new Paragraph();
        p.add(new Chunk(label + " ", labelFont));
        p.add(new Chunk(value, valFont));
        cell.addElement(p);
        table.addCell(cell);
    }

    private String formatCategoryTitle(String cat) {
        if (cat == null) return "MỤC: TIÊU CHUẨN CHUNG";
        return switch (cat.toUpperCase()) {
            case "RECORDS_AND_TRAINING", "HỒ SƠ & ĐÀO TẠO" -> "MỤC I: HỒ SƠ & ĐÀO TẠO TẬP HUẤN";
            case "INFRASTRUCTURE_EQUIPMENT", "FACILITIES_AND_EQUIPMENT", "CƠ SỞ VẬT CHẤT & THIẾT BỊ" -> "MỤC II: CƠ SỞ VẬT CHẤT & TRANG THIẾT BỊ";
            case "INPUTS_AND_TESTING", "INPUT_MATERIALS", "VẬT TƯ ĐẦU VÀO (ĐẤT, NƯỚC)" -> "MỤC III: ĐẤT TRỒNG, GIÁ THỂ & NGUỒN NƯỚC TƯỚI";
            case "FERTILIZER_PESTICIDE_MANAGEMENT", "FERTILIZERS_AND_PESTICIDES", "PHÂN BÓN & THUỐC BVTV" -> "MỤC IV: QUẢN LÝ PHÂN BÓN & THUỐC BVTV";
            case "HARVEST_STORAGE_TRANSPORT", "HARVESTING_AND_STORAGE", "THU HOẠCH & BẢO QUẢN" -> "MỤC V: THU HOẠCH & BẢO QUẢN NÔNG SẢN";
            case "TRACEABILITY_PRODUCT", "TRACEABILITY_AND_RECALL", "TRUY XUẤT NGUỒN GỐC" -> "MỤC VI: TRUY XUẤT NGUỒN GỐC & THU HỒI SẢN PHẨM";
            case "LABOR_SAFETY_ENVIRONMENT", "WORKER_HEALTH_AND_ENVIRONMENT", "AN TOÀN LAO ĐỘNG & MÔI TRƯỜNG" -> "MỤC VII: AN TOÀN LAO ĐỘNG & BẢO VỆ MÔI TRƯỜNG";
            default -> "MỤC: " + cat;
        };
    }
}
