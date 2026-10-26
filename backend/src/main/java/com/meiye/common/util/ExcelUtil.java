package com.meiye.common.util;

import jakarta.servlet.http.HttpServletResponse;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.web.multipart.MultipartFile;

import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ExcelUtil {

    private ExcelUtil() {
    }

    public static void write(HttpServletResponse response, String filename, String sheetName,
                             List<String> headers, List<List<Object>> rows) {
        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet(sheetName);
            CellStyle headStyle = wb.createCellStyle();
            Font font = wb.createFont();
            font.setBold(true);
            headStyle.setFont(font);
            headStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            headStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            Row head = sheet.createRow(0);
            for (int i = 0; i < headers.size(); i++) {
                Cell c = head.createCell(i);
                c.setCellValue(headers.get(i));
                c.setCellStyle(headStyle);
                sheet.setColumnWidth(i, 20 * 256);
            }
            for (int r = 0; r < rows.size(); r++) {
                Row row = sheet.createRow(r + 1);
                List<Object> data = rows.get(r);
                for (int c = 0; c < data.size(); c++) {
                    Cell cell = row.createCell(c);
                    Object v = data.get(c);
                    if (v == null) {
                        cell.setCellValue("");
                    } else if (v instanceof Number n) {
                        cell.setCellValue(n.doubleValue());
                    } else {
                        cell.setCellValue(String.valueOf(v));
                    }
                }
            }
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setHeader("Content-Disposition", "attachment;filename=" +
                    URLEncoder.encode(filename, StandardCharsets.UTF_8));
            try (OutputStream out = response.getOutputStream()) {
                wb.write(out);
                out.flush();
            }
        } catch (Exception e) {
            throw new IllegalStateException("Excel 导出失败", e);
        }
    }

    public static List<Map<String, String>> read(MultipartFile file) {
        List<Map<String, String>> result = new ArrayList<>();
        try (Workbook wb = WorkbookFactory.create(file.getInputStream())) {
            Sheet sheet = wb.getSheetAt(0);
            if (sheet.getPhysicalNumberOfRows() < 2) {
                return result;
            }
            Row head = sheet.getRow(0);
            List<String> headers = new ArrayList<>();
            DataFormatter formatter = new DataFormatter();
            for (int i = 0; i < head.getLastCellNum(); i++) {
                headers.add(formatter.formatCellValue(head.getCell(i)).trim());
            }
            for (int r = 1; r <= sheet.getLastRowNum(); r++) {
                Row row = sheet.getRow(r);
                if (row == null) {
                    continue;
                }
                Map<String, String> m = new LinkedHashMap<>();
                boolean empty = true;
                for (int c = 0; c < headers.size(); c++) {
                    String v = formatter.formatCellValue(row.getCell(c)).trim();
                    if (!v.isEmpty()) {
                        empty = false;
                    }
                    m.put(headers.get(c), v);
                }
                if (!empty) {
                    result.add(m);
                }
            }
            return result;
        } catch (Exception e) {
            throw new IllegalStateException("Excel 解析失败", e);
        }
    }
}
