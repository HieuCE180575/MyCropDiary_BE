package com.mycropdiary.api.service.cultivation;

import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.mycropdiary.api.dto.vietgap.ChecklistResultItemResp;
import com.mycropdiary.api.dto.vietgap.ChecklistRunResp;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

@Service
public class VietGapPdfExportService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
            .withZone(ZoneId.of("Asia/Ho_Chi_Minh"));

    public byte[] generateAuditReportPdf(ChecklistRunResp run) {
        Document document = new Document(PageSize.A4, 30, 30, 36, 36);
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try {
            PdfWriter.getInstance(document, out);
            document.open();

            // Font configurations (using standard Helvetica / Times Roman with UTF-8 support)
            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 15, Font.BOLD, new Color(20, 83, 45));
            Font subHeaderFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Font.BOLD, Color.DARK_GRAY);
            Font sectionFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, Font.BOLD, new Color(22, 101, 52));
            Font boldFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, Font.BOLD, Color.BLACK);
            Font regularFont = FontFactory.getFont(FontFactory.HELVETICA, 8, Font.NORMAL, Color.BLACK);
            Font smallFont = FontFactory.getFont(FontFactory.HELVETICA, 7, Font.ITALIC, Color.GRAY);

            // 1. National Motto & Header
            Paragraph nationalHeader = new Paragraph("CONG HOA XA HOI CHU NGHIA VIET NAM\nDoc lap - Tu do - Hanh phuc\n-------------------------", subHeaderFont);
            nationalHeader.setAlignment(Element.ALIGN_CENTER);
            document.add(nationalHeader);

            document.add(new Paragraph(" "));

            // 2. Report Title
            Paragraph reportTitle = new Paragraph("BAO CAO TU DANH GIA NOI BO VIETGAP TRONG TROT\n(TCVN 11892-1 - Cay ngan ngay & Rau mau)", titleFont);
            reportTitle.setAlignment(Element.ALIGN_CENTER);
            document.add(reportTitle);

            document.add(new Paragraph(" "));

            // 3. Metadata Table
            PdfPTable metaTable = new PdfPTable(2);
            metaTable.setWidthPercentage(100);
            metaTable.setWidths(new float[]{1.5f, 1.5f});

            addMetaCell(metaTable, "Ma dot danh gia: #" + run.runId(), boldFont);
            addMetaCell(metaTable, "Trang thai: " + ("completed".equalsIgnoreCase(run.runStatus()) ? "HOAN TAT (DA KHOA)" : "DANG THUC HIEN"), boldFont);
            addMetaCell(metaTable, "Mua vu: " + (run.seasonName() != null ? run.seasonName() : "Toan trang trai"), regularFont);
            addMetaCell(metaTable, "Phan khu / Lo: " + (run.blockName() != null ? run.blockName() : "Tat ca cac lo"), regularFont);
            addMetaCell(metaTable, "Nguoi kiem tra: " + (run.assignedToName() != null ? run.assignedToName() : "Can bo ky thuat VietGAP"), regularFont);
            addMetaCell(metaTable, "Thoi gian lap: " + (run.createdAt() != null ? DATE_FMT.format(run.createdAt()) : "N/A"), regularFont);

            document.add(metaTable);
            document.add(new Paragraph(" "));

            // 4. Compliance Summary Box
            PdfPTable summaryTable = new PdfPTable(4);
            summaryTable.setWidthPercentage(100);
            summaryTable.setWidths(new float[]{1f, 1f, 1f, 1.2f});

            int total = run.totalItems() != null ? run.totalItems() : (run.results() != null ? run.results().size() : 0);
            int passed = run.passedItems() != null ? run.passedItems() : 0;
            int failed = run.failedItems() != null ? run.failedItems() : 0;
            Double pct = run.compliancePercent() != null ? run.compliancePercent() : 0.0;

            addSummaryCell(summaryTable, "Tong so tieu chi", String.valueOf(total), sectionFont, boldFont);
            addSummaryCell(summaryTable, "So tieu chi DAT", String.valueOf(passed), sectionFont, boldFont);
            addSummaryCell(summaryTable, "So CHUA DAT", String.valueOf(failed), sectionFont, boldFont);
            addSummaryCell(summaryTable, "Ty le tuan thu", pct + "%", sectionFont, boldFont);

            document.add(summaryTable);
            document.add(new Paragraph(" "));

            // 5. Checklist Items Table (33 criteria)
            PdfPTable table = new PdfPTable(5);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{0.6f, 1.2f, 4.5f, 1.2f, 4.0f});

            addTableHeader(table, "STT", boldFont);
            addTableHeader(table, "Ma", boldFont);
            addTableHeader(table, "Noi dung kiem tra VietGAP", boldFont);
            addTableHeader(table, "Ket qua", boldFont);
            addTableHeader(table, "Ghi nhan thuc te & Minh chung", boldFont);

            if (run.results() != null && !run.results().isEmpty()) {
                int index = 1;
                String currentCat = "";

                for (ChecklistResultItemResp item : run.results()) {
                    String cat = item.category() != null ? item.category() : "CHUNG";
                    if (!cat.equals(currentCat)) {
                        currentCat = cat;
                        PdfPCell catCell = new PdfPCell(new Phrase("NHOM: " + currentCat, sectionFont));
                        catCell.setColspan(5);
                        catCell.setBackgroundColor(new Color(240, 253, 244));
                        catCell.setPadding(5);
                        table.addCell(catCell);
                    }

                    table.addCell(new PdfPCell(new Phrase(String.valueOf(index++), regularFont)));
                    table.addCell(new PdfPCell(new Phrase(item.ruleCode() != null ? item.ruleCode() : "-", boldFont)));
                    table.addCell(new PdfPCell(new Phrase(item.ruleName() != null ? item.ruleName() : "Tieu chi kiem tra", regularFont)));

                    String statusText = "CHUA XONG";
                    Color statusColor = Color.GRAY;
                    if ("pass".equalsIgnoreCase(item.resultStatus())) {
                        statusText = "DAT";
                        statusColor = new Color(22, 101, 52);
                    } else if ("fail".equalsIgnoreCase(item.resultStatus())) {
                        statusText = "CHUA DAT";
                        statusColor = Color.RED;
                    } else if ("not_applicable".equalsIgnoreCase(item.resultStatus())) {
                        statusText = "K.A.P";
                        statusColor = Color.DARK_GRAY;
                    }

                    Font statusFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, Font.BOLD, statusColor);
                    PdfPCell statusCell = new PdfPCell(new Phrase(statusText, statusFont));
                    statusCell.setHorizontalAlignment(Element.ALIGN_CENTER);
                    table.addCell(statusCell);

                    String notes = item.notes() != null && !item.notes().isBlank() ? item.notes() : "-";
                    if (item.evidenceImageUrl() != null && !item.evidenceImageUrl().isBlank()) {
                        notes += "\n[Kem anh minh chung]";
                    }
                    table.addCell(new PdfPCell(new Phrase(notes, regularFont)));
                }
            }

            document.add(table);
            document.add(new Paragraph(" "));

            // 6. Conclusion & Recommendation
            Paragraph conclusion = new Paragraph("KET LUAN: " +
                    (pct >= 90.0 && failed == 0
                            ? "Co so san xuat DU DIEU KIEN nop ho so de nghi to chuc chung nhan danh gia cap Giay chung nhan VietGAP."
                            : "Co so can tiep tuc khac phuc cac tieu chi Chua dat truoc khi nop ho so tham dinh chinh thuc."), boldFont);
            document.add(conclusion);

            document.add(new Paragraph(" "));
            document.add(new Paragraph(" "));

            // 7. Signatures Table
            PdfPTable signTable = new PdfPTable(2);
            signTable.setWidthPercentage(100);
            signTable.setWidths(new float[]{1f, 1f});

            PdfPCell signLeft = new PdfPCell();
            signLeft.setBorder(Rectangle.NO_BORDER);
            signLeft.setHorizontalAlignment(Element.ALIGN_CENTER);
            signLeft.addElement(new Paragraph("DAI DIEN TRANG TRAI / CHU CO SO\n(Ky ten, ghi ro ho ten va dong dau)\n\n\n\n\n.....................................................", boldFont));
            signTable.addCell(signLeft);

            PdfPCell signRight = new PdfPCell();
            signRight.setBorder(Rectangle.NO_BORDER);
            signRight.setHorizontalAlignment(Element.ALIGN_CENTER);
            signRight.addElement(new Paragraph("CAN BO KY THUAT / NGUOI KIEM TRA\n(Ky va ghi ro ho ten)\n\n\n\n\n" + (run.assignedToName() != null ? run.assignedToName() : "....................................................."), boldFont));
            signTable.addCell(signRight);

            document.add(signTable);

            document.close();
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Loi khi tao bao cao PDF VietGAP: " + e.getMessage(), e);
        }
    }

    private void addMetaCell(PdfPTable table, String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setPadding(3);
        table.addCell(cell);
    }

    private void addSummaryCell(PdfPTable table, String header, String value, Font headerFont, Font valFont) {
        PdfPCell cell = new PdfPCell();
        cell.setBackgroundColor(new Color(248, 250, 252));
        cell.setPadding(6);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        Paragraph p1 = new Paragraph(header, headerFont);
        p1.setAlignment(Element.ALIGN_CENTER);
        Paragraph p2 = new Paragraph(value, valFont);
        p2.setAlignment(Element.ALIGN_CENTER);
        cell.addElement(p1);
        cell.addElement(p2);
        table.addCell(cell);
    }

    private void addTableHeader(PdfPTable table, String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBackgroundColor(new Color(220, 252, 231));
        cell.setPadding(5);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        table.addCell(cell);
    }
}

