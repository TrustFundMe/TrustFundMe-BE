package com.trustfund.utils;

import com.trustfund.model.request.CreateExpenditureItemRequest;
import com.trustfund.model.response.ExpenditureItemResponse;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class ExpenditureExcelHelper {
    public static String TYPE = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
    static String SHEET = "KhoanChi";
    static SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd/MM/yyyy");

    static String[] HEADERS = {
        "STT", "Tên hàng hóa / Dịch vụ", "Số lượng dự kiến", "Đơn giá dự kiến (VNĐ)",
        "Thành tiền dự kiến (VNĐ)", "Ghi chú"
    };

    public static boolean hasExcelFormat(MultipartFile file) {
        if (TYPE.equals(file.getContentType())) return true;
        String filename = file.getOriginalFilename();
        if (filename == null) return false;
        String lower = filename.toLowerCase();
        return lower.endsWith(".xlsx") || lower.endsWith(".xls");
    }

    /**
     * Xuất danh sách hạng mục chi tiêu ra file Excel.
     * Tên file: KhoanChi_ngày_xuất.xlsx
     */
    public static ByteArrayInputStream itemsToExcel(List<ExpenditureItemResponse> items) {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet(SHEET);

            Row headerRow = sheet.createRow(0);
            for (int col = 0; col < HEADERS.length; col++) {
                Cell cell = headerRow.createCell(col);
                cell.setCellValue(HEADERS[col]);

                CellStyle style = workbook.createCellStyle();
                Font font = workbook.createFont();
                font.setBold(true);
                style.setFont(font);
                style.setFillForegroundColor(IndexedColors.LIGHT_ORANGE.getIndex());
                style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
                style.setBorderTop(BorderStyle.THIN);
                style.setBorderBottom(BorderStyle.THIN);
                style.setBorderLeft(BorderStyle.THIN);
                style.setBorderRight(BorderStyle.THIN);
                cell.setCellStyle(style);
            }

            int rowIdx = 1;
            int stt = 1;
            for (ExpenditureItemResponse item : items) {
                Row row = sheet.createRow(rowIdx++);

                row.createCell(0).setCellValue(stt++);
                row.createCell(1).setCellValue(item.getCategory() != null ? item.getCategory() : "");
                row.createCell(2).setCellValue(item.getQuantity() != null ? item.getQuantity() : 0);
                double expectedPrice = toDouble(item.getExpectedPrice());
                row.createCell(3).setCellValue(expectedPrice);
                int qty = item.getQuantity() != null ? item.getQuantity() : 0;
                row.createCell(4).setCellValue(expectedPrice * qty);
                row.createCell(5).setCellValue(item.getNote() != null ? item.getNote() : "");
            }

            for (int col = 0; col < HEADERS.length; col++) {
                sheet.setColumnWidth(col, 22 * 256);
            }
            sheet.setColumnWidth(0, 6 * 256);
            sheet.setColumnWidth(1, 30 * 256);

            workbook.write(out);
            return new ByteArrayInputStream(out.toByteArray());
        } catch (IOException e) {
            throw new RuntimeException("fail to export data to Excel file: " + e.getMessage());
        }
    }

    /**
     * Tạo file mẫu Excel với dữ liệu minh hoạ để người dùng tải về.
     */
    public static ByteArrayInputStream itemsToExcelTemplate() {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet(SHEET);

            Row headerRow = sheet.createRow(0);
            for (int col = 0; col < HEADERS.length; col++) {
                Cell cell = headerRow.createCell(col);
                cell.setCellValue(HEADERS[col]);

                CellStyle style = workbook.createCellStyle();
                Font font = workbook.createFont();
                font.setBold(true);
                style.setFont(font);
                style.setFillForegroundColor(IndexedColors.LIGHT_ORANGE.getIndex());
                style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
                style.setBorderTop(BorderStyle.THIN);
                style.setBorderBottom(BorderStyle.THIN);
                style.setBorderLeft(BorderStyle.THIN);
                style.setBorderRight(BorderStyle.THIN);
                cell.setCellStyle(style);
            }

            String[][] samples = {
                { "1", "Thùng mì tôm", "100", "7000", "700000", "Mua tại siêu thị Co.opmart" },
                { "2", "Nước đóng chai (lốc 6 chai)", "50", "18000", "900000", "Nước suối bidrico 1500ml" },
                { "3", "Gạo (kg)", "200", "25000", "5000000", "Gạo ST25 Việt Nam" },
            };

            CellStyle dataStyle = workbook.createCellStyle();
            dataStyle.setBorderTop(BorderStyle.THIN);
            dataStyle.setBorderBottom(BorderStyle.THIN);
            dataStyle.setBorderLeft(BorderStyle.THIN);
            dataStyle.setBorderRight(BorderStyle.THIN);

            for (int i = 0; i < samples.length; i++) {
                Row row = sheet.createRow(i + 1);
                for (int j = 0; j < samples[i].length; j++) {
                    Cell cell = row.createCell(j);
                    cell.setCellValue(samples[i][j]);
                    cell.setCellStyle(dataStyle);
                }
            }

            for (int col = 0; col < HEADERS.length; col++) {
                sheet.setColumnWidth(col, 22 * 256);
            }
            sheet.setColumnWidth(0, 6 * 256);
            sheet.setColumnWidth(1, 30 * 256);

            workbook.write(out);
            return new ByteArrayInputStream(out.toByteArray());
        } catch (IOException e) {
            throw new RuntimeException("fail to generate Excel template: " + e.getMessage());
        }
    }

    /**
     * Đọc file Excel nhập vào, trả về danh sách CreateExpenditureItemRequest.
     * Cột: STT (bỏ qua), Tên hàng hóa, Số lượng, Đơn giá, Thành tiền (bỏ qua), Ghi chú
     */
    public static List<CreateExpenditureItemRequest> excelToItems(InputStream is) {
        try (Workbook workbook = new XSSFWorkbook(is)) {
            Sheet sheet = workbook.getSheet(SHEET);
            if (sheet == null) {
                sheet = workbook.getSheetAt(0);
            }
            Iterator<Row> rows = sheet.iterator();

            List<CreateExpenditureItemRequest> items = new ArrayList<>();

            int rowNumber = 0;
            while (rows.hasNext()) {
                Row currentRow = rows.next();

                if (rowNumber == 0) {
                    rowNumber++;
                    continue;
                }

                Iterator<Cell> cellsInRow = currentRow.iterator();
                CreateExpenditureItemRequest item = new CreateExpenditureItemRequest();

                int cellIdx = 0;
                while (cellsInRow.hasNext()) {
                    Cell currentCell = cellsInRow.next();

                    switch (cellIdx) {
                        case 0: // STT – bỏ qua
                            break;
                        case 1: // Tên hàng hóa / Dịch vụ
                            item.setCategory(getCellStringValue(currentCell));
                            break;
                        case 2: // Số lượng dự kiến
                            item.setQuantity(getCellIntValue(currentCell));
                            break;
                        case 3: // Đơn giá dự kiến
                            item.setExpectedPrice(getCellBigDecimalValue(currentCell));
                            break;
                        case 4: // Thành tiền – bỏ qua
                            break;
                        case 5: // Ghi chú
                            item.setNote(getCellStringValue(currentCell));
                            break;
                    }
                    cellIdx++;
                }

                item.setPrice(BigDecimal.ZERO);
                items.add(item);
                rowNumber++;
            }

            return items;
        } catch (IOException e) {
            throw new RuntimeException("fail to parse Excel file: " + e.getMessage());
        }
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private static double toDouble(BigDecimal value) {
        return value != null ? value.doubleValue() : 0.0;
    }

    private static String getCellStringValue(Cell cell) {
        if (cell == null) return "";
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue().trim();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return DATE_FORMAT.format(cell.getDateCellValue());
                }
                return String.valueOf((long) cell.getNumericCellValue());
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                try {
                    return String.valueOf((long) cell.getNumericCellValue());
                } catch (Exception e) {
                    return cell.getStringCellValue();
                }
            default:
                return "";
        }
    }

    private static Integer getCellIntValue(Cell cell) {
        if (cell == null) return 1;
        switch (cell.getCellType()) {
            case NUMERIC:
                return (int) cell.getNumericCellValue();
            case STRING:
                try {
                    return Integer.parseInt(cell.getStringCellValue().trim());
                } catch (NumberFormatException e) {
                    return 1;
                }
            default:
                return 1;
        }
    }

    private static BigDecimal getCellBigDecimalValue(Cell cell) {
        if (cell == null) return BigDecimal.ZERO;
        switch (cell.getCellType()) {
            case NUMERIC:
                return BigDecimal.valueOf(cell.getNumericCellValue());
            case STRING:
                try {
                    String val = cell.getStringCellValue().trim()
                            .replace(".", "")
                            .replace(",", ".");
                    return new BigDecimal(val);
                } catch (NumberFormatException e) {
                    return BigDecimal.ZERO;
                }
            default:
                return BigDecimal.ZERO;
        }
    }
}
