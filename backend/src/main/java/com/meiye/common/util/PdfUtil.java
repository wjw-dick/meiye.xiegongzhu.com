package com.meiye.common.util;

import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.awt.Color;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * PDF 生成：中文字体嵌入，结算单/开卡单/服务报告/合同/报表通用。
 */
@Slf4j
@Component
public class PdfUtil {

    @Value("${app.pdf.font:/usr/share/fonts/chinese/stsong.ttf}")
    private String fontPath;

    private BaseFont baseFont() {
        try {
            return BaseFont.createFont(fontPath, BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
        } catch (Exception e) {
            log.warn("PDF 中文字体加载失败，降级 Helvetica: {}", e.getMessage());
            try {
                return BaseFont.createFont(BaseFont.HELVETICA, BaseFont.WINANSI, BaseFont.NOT_EMBEDDED);
            } catch (Exception ex) {
                throw new IllegalStateException("PDF 字体初始化失败", ex);
            }
        }
    }

    /**
     * 生成通用业务单据 PDF。
     *
     * @param title    标题
     * @param subtitle 副标题（单号/时间等）
     * @param meta     抬头信息键值对
     * @param headers  表格表头
     * @param rows     表格数据
     * @param footer   页脚说明
     * @param signature 电子签名位名称（可为空）
     */
    public void write(HttpServletResponse response, String filename, String title, String subtitle,
                      Map<String, String> meta, List<String> headers, List<List<Object>> rows,
                      String footer, String signature) {
        Document doc = new Document(PageSize.A4, 40, 40, 46, 46);
        try (OutputStream out = response.getOutputStream()) {
            PdfWriter.getInstance(doc, out);
            doc.open();
            BaseFont bf = baseFont();
            Font titleFont = new Font(bf, 20, Font.BOLD);
            Font subFont = new Font(bf, 10, Font.NORMAL, new Color(110, 110, 110));
            Font headFont = new Font(bf, 10.5f, Font.BOLD, Color.WHITE);
            Font bodyFont = new Font(bf, 10);
            Font smallFont = new Font(bf, 9, Font.NORMAL, new Color(90, 90, 90));

            Paragraph t = new Paragraph(title, titleFont);
            t.setAlignment(Element.ALIGN_CENTER);
            doc.add(t);
            if (subtitle != null) {
                Paragraph s = new Paragraph(subtitle, subFont);
                s.setAlignment(Element.ALIGN_CENTER);
                s.setSpacingAfter(14);
                doc.add(s);
            }
            if (meta != null && !meta.isEmpty()) {
                PdfPTable mt = new PdfPTable(2);
                mt.setWidthPercentage(100);
                mt.setWidths(new float[]{1, 3});
                for (Map.Entry<String, String> e : meta.entrySet()) {
                    mt.addCell(cell(e.getKey(), headFont, new Color(64, 90, 140)));
                    mt.addCell(cell(e.getValue() == null ? "" : e.getValue(), bodyFont, Color.WHITE));
                }
                mt.setSpacingAfter(12);
                doc.add(mt);
            }
            if (headers != null && !headers.isEmpty()) {
                PdfPTable table = new PdfPTable(headers.size());
                table.setWidthPercentage(100);
                for (String h : headers) {
                    table.addCell(cell(h, headFont, new Color(64, 90, 140)));
                }
                if (rows != null) {
                    for (List<Object> row : rows) {
                        for (Object v : row) {
                            table.addCell(cell(v == null ? "" : String.valueOf(v), bodyFont, Color.WHITE));
                        }
                    }
                }
                doc.add(table);
            }
            if (footer != null && !footer.isBlank()) {
                Paragraph f = new Paragraph(footer, smallFont);
                f.setSpacingBefore(14);
                doc.add(f);
            }
            if (signature != null && !signature.isBlank()) {
                Paragraph sig = new Paragraph("\n\n" + signature + "：____________________", bodyFont);
                sig.setSpacingBefore(24);
                doc.add(sig);
            }
            Paragraph stamp = new Paragraph("打印时间："
                    + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")), smallFont);
            stamp.setSpacingBefore(20);
            doc.add(stamp);
            doc.close();
            response.setContentType("application/pdf");
            response.setHeader("Content-Disposition", "attachment;filename=" +
                    URLEncoder.encode(filename, StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new IllegalStateException("PDF 生成失败", e);
        }
    }

    private PdfPCell cell(String text, Font font, Color bg) {
        PdfPCell c = new PdfPCell(new Phrase(text, font));
        c.setPadding(5);
        c.setBackgroundColor(bg);
        c.setBorderColor(new Color(210, 215, 225));
        return c;
    }
}
