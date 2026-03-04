package com.xsh.trueused.inspection.service;

import java.io.ByteArrayOutputStream;

import org.springframework.stereotype.Service;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.xsh.trueused.inspection.dto.InspectionFlowDTO;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class InspectionPdfService {

    public byte[] generateInspectionReportPdf(InspectionFlowDTO inspection) throws Exception {
        // 1. 创建内存流
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4);
        
        try {
            PdfWriter.getInstance(document, baos);
            document.open();

            // 2. 🔥 关键：设置中文字体
            BaseFont bfChinese;
            try {
                // 使用 OpenPDF 自带的亚洲字体库
                bfChinese = BaseFont.createFont("STSong-Light", "UniGB-UCS2-H", BaseFont.NOT_EMBEDDED);
            } catch (Exception e) {
                log.warn("STSong-Light font not found in classpath (itext-asian jar might be missing or issues with openpdf-fonts-extra). Falling back to Helvetica (no Chinese support).", e);
                // 如果字体挂了，回退到英文，防止程序崩溃
                bfChinese = BaseFont.createFont(BaseFont.HELVETICA, BaseFont.WINANSI, BaseFont.NOT_EMBEDDED);
            }

            Font titleFont = new Font(bfChinese, 18, Font.BOLD);
            Font normalFont = new Font(bfChinese, 12, Font.NORMAL);
            Font boldFont = new Font(bfChinese, 12, Font.BOLD); // 加粗字体用于小标题

            // 3. 写入标题 (更美观的标题)
            Paragraph title = new Paragraph("TrueUsed 官方验货报告", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(20f);
            document.add(title);

            // 4. 写入基本信息 (美化表格)
            PdfPTable infoTable = new PdfPTable(2);
            infoTable.setWidthPercentage(100);
            infoTable.setSpacingAfter(20f);
            infoTable.setWidths(new float[]{1f, 3f}); // 调整列宽比例
            
            // 移除了订单编号
            addTableRow(infoTable, "商品标题:", safeStr(inspection.getProductTitle()), normalFont);
            addTableRow(infoTable, "验货状态:", formatStatus(inspection.getStatus()), normalFont);
            addTableRow(infoTable, "成色评级:", safeStr(inspection.getGrade()), normalFont);
            addTableRow(infoTable, "检测时间:", formatDate(inspection.getUpdatedAt()), normalFont);
            
            document.add(infoTable);

            // 5. 写入验货结果摘要 (带背景色的块)
            PdfPTable summaryTable = new PdfPTable(1);
            summaryTable.setWidthPercentage(100);
            summaryTable.setSpacingAfter(20f);
            
            PdfPCell summaryHeader = new PdfPCell(new Phrase("质检工程师摘要", boldFont));
            summaryHeader.setBackgroundColor(new java.awt.Color(242, 242, 242)); // 淡灰色背景
            summaryHeader.setPadding(8f);
            summaryHeader.setBorder(Rectangle.NO_BORDER);
            summaryTable.addCell(summaryHeader);
            
            PdfPCell summaryContent = new PdfPCell(new Phrase(safeStr(inspection.getResultSummary()), normalFont));
            summaryContent.setPadding(10f);
            summaryContent.setPaddingBottom(15f);
            summaryContent.setBorder(Rectangle.NO_BORDER);
            summaryContent.setBackgroundColor(new java.awt.Color(250, 250, 250)); // 下方更浅灰色背景
            summaryTable.addCell(summaryContent);
            
            document.add(summaryTable);
            
            // 6. 详细检测项
            document.add(new Paragraph("详细检测项目:", boldFont));
            document.add(new Paragraph(" ", new Font(bfChinese, 5)));

            if (inspection.getItems() != null && !inspection.getItems().isEmpty()) {
                PdfPTable itemTable = new PdfPTable(3);
                itemTable.setWidthPercentage(100);
                itemTable.setWidths(new float[] { 3f, 5f, 2f });
                itemTable.setHeaderRows(1);

                // 表头
                addItemHeader(itemTable, "检测点", normalFont);
                addItemHeader(itemTable, "说明/备注", normalFont);
                addItemHeader(itemTable, "结果", normalFont);

                for (com.xsh.trueused.inspection.dto.InspectionItemResultDTO item : inspection.getItems()) {
                    addItemCell(itemTable, safeStr(item.getItemName()), normalFont);
                    
                    String desc = safeStr(item.getItemDescription());
                    if (item.getNotes() != null && !item.getNotes().isEmpty()) {
                        desc += "\n备注: " + item.getNotes();
                    }
                    addItemCell(itemTable, desc, normalFont);
                    
                    addItemStatusCell(itemTable, item.getStatus(), normalFont);
                }
                document.add(itemTable);
            } else {
                document.add(new Paragraph("暂无详细检测数据", normalFont));
            }

            // Footer
            Paragraph footer = new Paragraph("\n* 本报告由 TrueUsed 平台生成，仅代表当时检测状态。", new Font(bfChinese, 10, Font.ITALIC, new java.awt.Color(128, 128, 128)));
            footer.setAlignment(Element.ALIGN_CENTER);
            footer.setSpacingBefore(30f);
            document.add(footer);

            document.close(); // 这一步很重要，不close会导致流不完整

        } catch (Exception e) {
            throw new RuntimeException("PDF生成内部错误: " + e.getMessage(), e);
        }

        return baos.toByteArray();
    }

    // 辅助方法：添加表格行
    private void addTableRow(PdfPTable table, String label, String value, Font font) {
        PdfPCell cellLabel = new PdfPCell(new Phrase(label, font));
        cellLabel.setBorder(Rectangle.BOTTOM); // 只保留底边框
        cellLabel.setBorderColor(new java.awt.Color(230, 230, 230));
        cellLabel.setPadding(8f);
        table.addCell(cellLabel);

        PdfPCell cellValue = new PdfPCell(new Phrase(value, font));
        cellValue.setBorder(Rectangle.BOTTOM);
        cellValue.setBorderColor(new java.awt.Color(230, 230, 230));
        cellValue.setPadding(8f);
        table.addCell(cellValue);
    }

    private void addItemHeader(PdfPTable table, String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBackgroundColor(new java.awt.Color(245, 245, 245));
        cell.setPadding(6f);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        table.addCell(cell);
    }

    private void addItemCell(PdfPTable table, String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setPadding(6f);
        table.addCell(cell);
    }

    private void addItemStatusCell(PdfPTable table, String status, Font font) {
        String displayStatus = "PASSED".equalsIgnoreCase(status) ? "正常" : "异常";
        Font statusFont = new Font(font);
        if ("PASSED".equalsIgnoreCase(status)) {
            statusFont.setColor(0, 128, 0); // Green
        } else {
            statusFont.setColor(255, 0, 0); // Red
            if (status != null && !status.equals("FAILED")) {
                 displayStatus = status; // 如果是其他状态值直接显示
            }
        }
        
        PdfPCell cell = new PdfPCell(new Phrase(displayStatus, statusFont));
        cell.setPadding(6f);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        table.addCell(cell);
    }
    
    private String formatStatus(String status) {
        if ("COMPLETED".equalsIgnoreCase(status)) return "已完成";
        if ("PENDING".equalsIgnoreCase(status)) return "检测中";
        return safeStr(status);
    }

    private String formatDate(java.time.Instant instant) {
        if (instant == null) return "-";
        return new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm").format(java.util.Date.from(instant));
    }

    // 辅助方法：防空指针 (Null Safe String)
    private String safeStr(String str) {
        return str == null ? "" : str;
    }
}
