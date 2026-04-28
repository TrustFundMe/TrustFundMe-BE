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

    static String SHEET_PLAN = "DANH SÁCH ĐỢT";
    static String SHEET_ITEMS = "HẠNG MỤC CHI TIẾT";

    static String[] HEADERS_PLAN = {
        "STT", "Tên đợt giải ngân", "Ngày bắt đầu (dd/mm/yyyy)", "Ngày kết thúc (dd/mm/yyyy)", "Mô tả", "Điều kiện giải ngân"
    };

    static String[] HEADERS_ITEMS = {
        "STT", "Tên đợt giải ngân", "Hạng mục", "Tên hàng hóa / Dịch vụ", "Nhãn hàng", "Địa điểm mua", "Số lượng dự kiến", "Đơn vị", "Đơn giá dự kiến (VNĐ)",
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
            Sheet sheet = workbook.createSheet(SHEET_ITEMS);
            Row headerRow = sheet.createRow(0);

            CellStyle headerStyle = workbook.createCellStyle();
            headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            headerStyle.setFillPattern(FillPatternType.solid_foreground);
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);

            for (int col = 0; col < HEADERS_ITEMS.length; col++) {
                Cell cell = headerRow.createCell(col);
                cell.setCellValue(HEADERS_ITEMS[col]);
                cell.setCellStyle(headerStyle);
            }

            int rowIdx = 1;
            int stt = 1;
            for (ExpenditureItemResponse item : items) {
                Row row = sheet.createRow(rowIdx++);

                row.createCell(0).setCellValue(stt++);
                row.createCell(1).setCellValue(item.getCategory() != null ? item.getCategory() : "");
                row.createCell(2).setCellValue(item.getBrand() != null ? item.getBrand() : "");
                row.createCell(3).setCellValue(item.getPurchaseLocation() != null ? item.getPurchaseLocation() : "");
                row.createCell(4).setCellValue(item.getExpectedQuantity() != null ? item.getExpectedQuantity() : 0);
                row.createCell(5).setCellValue(item.getUnit() != null ? item.getUnit() : "");
                double expectedPrice = toDouble(item.getExpectedPrice());
                row.createCell(6).setCellValue(expectedPrice);
                int qty = item.getExpectedQuantity() != null ? item.getExpectedQuantity() : 0;
                row.createCell(7).setCellValue(expectedPrice * qty);
                row.createCell(8).setCellValue(item.getNote() != null ? item.getNote() : "");
            }

            for (int col = 0; col < HEADERS_ITEMS.length; col++) {
                sheet.autoSizeColumn(col);
            }

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
            // SHEET 1: DANH SÁCH ĐỢT
            Sheet planSheet = workbook.createSheet(SHEET_PLAN);
            Row planHeaderRow = planSheet.createRow(0);
            CellStyle headerStyle = workbook.createCellStyle();
            headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);

            for (int i = 0; i < HEADERS_PLAN.length; i++) {
                Cell cell = planHeaderRow.createCell(i);
                cell.setCellValue(HEADERS_PLAN[i]);
                cell.setCellStyle(headerStyle);
            }

            String[][] planSamples = {
                { "1", "Đợt 1: Hỗ trợ khẩn cấp", "01/05/2026", "15/05/2026", "Móc giải ngân đầu tiên để mua nhu yếu phẩm", "Sau khi campaign được duyệt" },
                { "2", "Đợt 2: Phục hồi sinh kế", "16/05/2026", "30/06/2026", "Hỗ trợ hạt giống và công cụ sản xuất", "Sau khi hoàn thành đợt 1" }
            };

            for (int i = 0; i < planSamples.length; i++) {
                Row row = planSheet.createRow(i + 1);
                for (int j = 0; j < planSamples[i].length; j++) {
                    row.createCell(j).setCellValue(planSamples[i][j]);
                }
            }
            for (int i = 0; i < HEADERS_PLAN.length; i++) planSheet.autoSizeColumn(i);

            // SHEET 2: HẠNG MỤC CHI TIẾT
            Sheet itemSheet = workbook.createSheet(SHEET_ITEMS);
            Row itemHeaderRow = itemSheet.createRow(0);
            for (int i = 0; i < HEADERS_ITEMS.length; i++) {
                Cell cell = itemHeaderRow.createCell(i);
                cell.setCellValue(HEADERS_ITEMS[i]);
                cell.setCellStyle(headerStyle);
            }

            String[][] itemSamples = {
                { "1", "Đợt 1: Hỗ trợ khẩn cấp", "Thực phẩm", "Thùng mì tôm", "Hảo Hảo", "Co.opmart", "100", "Thùng", "70000", "7000000", "Cứu trợ miền Trung" },
                { "2", "Đợt 1: Hỗ trợ khẩn cấp", "Thực phẩm", "Nước đóng chai", "Aquafina", "Đại lý", "50", "Chai", "18000", "900000", "" },
                { "3", "Đợt 2: Phục hồi sinh kế", "Nông nghiệp", "Hạt giống rau", "Trang nông", "Cửa hàng vật tư", "50", "Gói", "25000", "1250000", "" }
            };

            for (int i = 0; i < itemSamples.length; i++) {
                Row row = itemSheet.createRow(i + 1);
                for (int j = 0; j < itemSamples[i].length; j++) {
                    row.createCell(j).setCellValue(itemSamples[i][j]);
                }
            }
            for (int i = 0; i < HEADERS_ITEMS.length; i++) itemSheet.autoSizeColumn(i);

            workbook.write(out);
            return new ByteArrayInputStream(out.toByteArray());
        } catch (IOException e) {
            throw new RuntimeException("fail to generate Excel template: " + e.getMessage());
        }
    }

    public static List<com.trustfund.model.request.BulkMilestoneImportRequest> excelToMilestones(InputStream is) {
        try (Workbook workbook = new XSSFWorkbook(is)) {
            List<com.trustfund.model.request.BulkMilestoneImportRequest> milestones = new ArrayList<>();
            
            // 1. Phân tích Tab Kế hoạch (Sheet 0)
            Sheet planSheet = workbook.getSheet(SHEET_PLAN);
            if (planSheet == null) planSheet = workbook.getSheetAt(0);
            Iterator<Row> planRows = planSheet.iterator();
            if (planRows.hasNext()) {
                Row header = planRows.next();
                int titleIdx = -1, startIdx = -1, endIdx = -1, descIdx = -1, condIdx = -1;
                for (Cell cell : header) {
                    String h = getCellStringValue(cell).toLowerCase();
                    int col = cell.getColumnIndex();
                    if (h.contains("tên") || h.contains("mốc")) titleIdx = col;
                    else if (h.contains("bắt đầu")) startIdx = col;
                    else if (h.contains("kết thúc") || h.contains("dự kiến")) endIdx = col;
                    else if (h.contains("mô tả")) descIdx = col;
                    else if (h.contains("điều kiện")) condIdx = col;
                }
                while (planRows.hasNext()) {
                    Row row = planRows.next();
                    if (isEmptyRow(row)) continue;
                    String title = getCellStringValue(row.getCell(titleIdx)).trim();
                    if (title.isEmpty()) continue;
                    
                    com.trustfund.model.request.BulkMilestoneImportRequest milestone = com.trustfund.model.request.BulkMilestoneImportRequest.builder()
                            .milestoneTitle(title)
                            .startDate(startIdx != -1 ? getCellStringValue(row.getCell(startIdx)) : "")
                            .endDate(endIdx != -1 ? getCellStringValue(row.getCell(endIdx)) : "")
                            .description(descIdx != -1 ? getCellStringValue(row.getCell(descIdx)) : "")
                            .releaseCondition(condIdx != -1 ? getCellStringValue(row.getCell(condIdx)) : "")
                            .categories(new ArrayList<>())
                            .build();
                    milestones.add(milestone);
                }
            }

            // 2. Phân tích Tab Chi tiết (Sheet 1)
            Sheet itemSheet = workbook.getSheet(SHEET_ITEMS);
            if (itemSheet == null && workbook.getNumberOfSheets() > 1) itemSheet = workbook.getSheetAt(1);
            if (itemSheet != null) {
                Iterator<Row> itemRows = itemSheet.iterator();
                if (itemRows.hasNext()) {
                    Row header = itemRows.next();
                    int milIdx = -1, catIdx = -1, nameIdx = -1, qtyIdx = -1, priceIdx = -1, unitIdx = -1, brandIdx = -1, locIdx = -1, noteIdx = -1;
                    for (Cell cell : header) {
                        String h = getCellStringValue(cell).toLowerCase();
                        int col = cell.getColumnIndex();
                        if (h.contains("đợt")) milIdx = col;
                        else if (h.contains("hạng mục")) catIdx = col;
                        else if (h.contains("tên hàng") || h.contains("vật phẩm")) nameIdx = col;
                        else if (h.contains("số lượng")) qtyIdx = col;
                        else if (h.contains("đơn giá")) priceIdx = col;
                        else if (h.contains("đơn vị")) unitIdx = col;
                        else if (h.contains("nhãn")) brandIdx = col;
                        else if (h.contains("địa điểm")) locIdx = col;
                        else if (h.contains("ghi chú")) noteIdx = col;
                    }

                    while (itemRows.hasNext()) {
                        Row row = itemRows.next();
                        if (isEmptyRow(row)) continue;
                        String milTitle = getCellStringValue(row.getCell(milIdx)).trim();
                        String catName = getCellStringValue(row.getCell(catIdx)).trim();
                        String itemName = getCellStringValue(row.getCell(nameIdx)).trim();
                        if (milTitle.isEmpty() || catName.isEmpty() || itemName.isEmpty()) continue;

                        // Tìm milestone đã tạo từ tab plan hoặc tạo mới nếu chưa có
                        com.trustfund.model.request.BulkMilestoneImportRequest milestone = milestones.stream()
                                .filter(m -> m.getMilestoneTitle().equalsIgnoreCase(milTitle))
                                .findFirst().orElse(null);
                        if (milestone == null) {
                            milestone = com.trustfund.model.request.BulkMilestoneImportRequest.builder()
                                    .milestoneTitle(milTitle).categories(new ArrayList<>()).build();
                            milestones.add(milestone);
                        }

                        com.trustfund.model.request.CreateExpenditureCatologyRequest category = milestone.getCategories().stream()
                                .filter(c -> c.getName().equalsIgnoreCase(catName))
                                .findFirst().orElse(null);
                        if (category == null) {
                            category = com.trustfund.model.request.CreateExpenditureCatologyRequest.builder()
                                    .name(catName).items(new ArrayList<>()).build();
                            milestone.getCategories().add(category);
                        }

                        CreateExpenditureItemRequest item = new CreateExpenditureItemRequest();
                        item.setCategory(itemName);
                        item.setExpectedQuantity(getCellIntValue(row.getCell(qtyIdx)));
                        item.setExpectedPrice(getCellBigDecimalValue(row.getCell(priceIdx)));
                        if (unitIdx != -1) item.setUnit(getCellStringValue(row.getCell(unitIdx)));
                        if (brandIdx != -1) item.setBrand(getCellStringValue(row.getCell(brandIdx)));
                        if (locIdx != -1) item.setPurchaseLocation(getCellStringValue(row.getCell(locIdx)));
                        if (noteIdx != -1) item.setNote(getCellStringValue(row.getCell(noteIdx)));
                        item.setActualPrice(BigDecimal.ZERO);
                        category.getItems().add(item);
                    }
                }
            }
            return milestones;
        } catch (IOException e) {
            throw new RuntimeException("fail to parse bulk Excel file: " + e.getMessage());
        }
    }

    private static boolean isEmptyRow(Row row) {
        if (row == null) return true;
        for (int i = 0; i < row.getLastCellNum(); i++) {
            Cell cell = row.getCell(i);
            if (cell != null && cell.getCellType() != CellType.BLANK) return false;
        }
        return true;
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
            if (!rows.hasNext()) return items;

            Row headerRow = rows.next();
            int nameIdx = -1, qtyIdx = -1, priceIdx = -1, noteIdx = -1, unitIdx = -1, brandIdx = -1, locationIdx = -1;

            // Map column indices dynamically
            for (Cell cell : headerRow) {
                String header = getCellStringValue(cell).toLowerCase();
                int colIdx = cell.getColumnIndex();
                if (header.contains("tên") || header.contains("hàng") || header.contains("vật")) nameIdx = colIdx;
                else if (header.contains("số lượng") || header.contains("sl")) qtyIdx = colIdx;
                else if (header.contains("đơn giá") || header.contains("giá")) priceIdx = colIdx;
                else if (header.contains("ghi chú") || header.contains("note")) noteIdx = colIdx;
                else if (header.contains("đơn vị") || header.contains("unit")) unitIdx = colIdx;
                else if (header.contains("nhãn") || header.contains("brand")) brandIdx = colIdx;
                else if (header.contains("điểm") || header.contains("location")) locationIdx = colIdx;
            }

            // Fallback for strict old template matching if dynamic matching failed
            if (nameIdx == -1) nameIdx = 1;
            if (qtyIdx == -1) qtyIdx = 2;
            if (priceIdx == -1) priceIdx = 3;
            // if noteIdx is still -1, it means we didn't find "ghi chú", we'll just leave it and not import note

            // But wait, what if the user used the legacy template with "thành tiền" in col 4 and "ghi chú" in col 5?
            // If they didn't have a header row that matched, we default to:
            if (noteIdx == -1) noteIdx = 5;

            while (rows.hasNext()) {
                Row currentRow = rows.next();
                
                // Skip empty rows
                boolean isEmptyRow = true;
                for (Cell cell : currentRow) {
                    if (cell != null && cell.getCellType() != CellType.BLANK) {
                        isEmptyRow = false;
                        break;
                    }
                }
                if (isEmptyRow) continue;

                CreateExpenditureItemRequest item = new CreateExpenditureItemRequest();

                if (nameIdx != -1) {
                    Cell c = currentRow.getCell(nameIdx, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
                    item.setCategory(getCellStringValue(c));
                }
                if (qtyIdx != -1) {
                    Cell c = currentRow.getCell(qtyIdx, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
                    item.setExpectedQuantity(getCellIntValue(c));
                }
                if (priceIdx != -1) {
                    Cell c = currentRow.getCell(priceIdx, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
                    item.setExpectedPrice(getCellBigDecimalValue(c));
                }
                if (noteIdx != -1) {
                    Cell c = currentRow.getCell(noteIdx, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
                    item.setNote(getCellStringValue(c));
                }
                if (unitIdx != -1) {
                    Cell c = currentRow.getCell(unitIdx, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
                    item.setUnit(getCellStringValue(c));
                }
                if (brandIdx != -1) {
                    Cell c = currentRow.getCell(brandIdx, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
                    item.setBrand(getCellStringValue(c));
                }
                if (locationIdx != -1) {
                    Cell c = currentRow.getCell(locationIdx, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
                    item.setPurchaseLocation(getCellStringValue(c));
                }

                // If name is empty, skip this row (probably end of data)
                if (item.getCategory() == null || item.getCategory().trim().isEmpty()) {
                    continue;
                }

                item.setActualPrice(BigDecimal.ZERO);
                items.add(item);
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
