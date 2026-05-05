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
            "STT", "Tên đợt giải ngân", "Ngày bắt đầu (dd/mm/yyyy)", "Ngày kết thúc (dd/mm/yyyy)",
            "Hạn nộp minh chứng (dd/mm/yyyy)", "Mô tả"
    };

    static String[] HEADERS_ITEMS = {
            "STT", "Đợt / Mốc giải ngân", "Hạng mục", "Tên hàng hóa / Dịch vụ", "Nhãn hàng", "Địa điểm mua",
            "Số lượng dự kiến", "Đơn vị", "Đơn giá dự kiến (VNĐ)",
            "Thành tiền dự kiến (VNĐ)", "Ghi chú"
    };

    static String[] HEADERS_ACTUALS = {
            "STT", "Hạng mục", "Tên hàng hóa / Dịch vụ", "Nhãn hàng", "Địa điểm mua",
            "Số lượng thực tế", "Đơn vị", "Đơn giá thực tế (VNĐ)",
            "Thành tiền thực tế (VNĐ)", "Ghi chú"
    };

    public static boolean hasExcelFormat(MultipartFile file) {
        if (TYPE.equals(file.getContentType()))
            return true;
        String filename = file.getOriginalFilename();
        if (filename == null)
            return false;
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
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
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
                row.createCell(1).setCellValue(""); // Milestone name (not available in response currently)
                row.createCell(2).setCellValue(item.getCatologyName() != null ? item.getCatologyName() : "");
                row.createCell(3).setCellValue(item.getName() != null ? item.getName() : "");
                row.createCell(4).setCellValue(item.getExpectedBrand() != null ? item.getExpectedBrand() : "");
                row.createCell(5).setCellValue(
                        item.getExpectedPurchaseLocation() != null ? item.getExpectedPurchaseLocation() : "");
                row.createCell(6).setCellValue(item.getExpectedQuantity() != null ? item.getExpectedQuantity() : 0);
                row.createCell(7).setCellValue(item.getExpectedUnit() != null ? item.getExpectedUnit() : "");
                double expectedPrice = toDouble(item.getExpectedPrice());
                row.createCell(8).setCellValue(expectedPrice);
                int qty = item.getExpectedQuantity() != null ? item.getExpectedQuantity() : 0;
                row.createCell(9).setCellValue(expectedPrice * qty);
                row.createCell(10).setCellValue(item.getExpectedNote() != null ? item.getExpectedNote() : "");
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

            // Text format for date columns to prevent Excel from auto-converting dates
            CellStyle textStyle = workbook.createCellStyle();
            DataFormat textFormat = workbook.createDataFormat();
            textStyle.setDataFormat(textFormat.getFormat("@"));

            for (int i = 0; i < HEADERS_PLAN.length; i++) {
                Cell cell = planHeaderRow.createCell(i);
                cell.setCellValue(HEADERS_PLAN[i]);
                cell.setCellStyle(headerStyle);
            }

            // Set date columns (2=start, 3=end, 4=evidence due) to Text format
            planSheet.setDefaultColumnStyle(2, textStyle);
            planSheet.setDefaultColumnStyle(3, textStyle);
            planSheet.setDefaultColumnStyle(4, textStyle);

            String[][] planSamples = {
                    { "1", "Đợt 1: Hỗ trợ khẩn cấp", "01/05/2026", "15/05/2026", "20/05/2026",
                            "Giải ngân đầu tiên để mua nhu yếu phẩm" },
                    { "2", "Đợt 2: Phục hồi sinh kế", "21/05/2026", "30/06/2026", "05/07/2026",
                            "Hỗ trợ hạt giống và công cụ sản xuất" }
            };

            for (int i = 0; i < planSamples.length; i++) {
                Row row = planSheet.createRow(i + 1);
                for (int j = 0; j < planSamples[i].length; j++) {
                    Cell cell = row.createCell(j);
                    cell.setCellValue(planSamples[i][j]);
                    // Apply text format explicitly to date columns
                    if (j >= 2 && j <= 4) {
                        cell.setCellStyle(textStyle);
                    }
                }
            }
            for (int i = 0; i < HEADERS_PLAN.length; i++)
                planSheet.autoSizeColumn(i);

            // SHEET 2: HẠNG MỤC CHI TIẾT
            Sheet itemSheet = workbook.createSheet(SHEET_ITEMS);
            Row itemHeaderRow = itemSheet.createRow(0);
            for (int i = 0; i < HEADERS_ITEMS.length; i++) {
                Cell cell = itemHeaderRow.createCell(i);
                cell.setCellValue(HEADERS_ITEMS[i]);
                cell.setCellStyle(headerStyle);
            }

            String[][] itemSamples = {
                    { "1", "Đợt 1: Hỗ trợ khẩn cấp", "Thực phẩm", "Thùng mì tôm", "Hảo Hảo", "Co.opmart",
                            "100", "Thùng", "70000", "7000000", "Cứu trợ miền Trung" },
                    { "2", "Đợt 1: Hỗ trợ khẩn cấp", "Thực phẩm", "Nước đóng chai", "Aquafina", "Đại lý",
                            "50", "Chai", "18000", "900000", "" },
                    { "3", "Đợt 2: Phục hồi sinh kế", "Nông nghiệp", "Hạt giống rau", "Trang nông",
                            "Cửa hàng vật tư", "50", "Gói", "25000", "1250000", "" }
            };

            for (int i = 0; i < itemSamples.length; i++) {
                Row row = itemSheet.createRow(i + 1);
                for (int j = 0; j < itemSamples[i].length; j++) {
                    row.createCell(j).setCellValue(itemSamples[i][j]);
                }
            }
            for (int i = 0; i < HEADERS_ITEMS.length; i++)
                itemSheet.autoSizeColumn(i);

            workbook.write(out);
            return new ByteArrayInputStream(out.toByteArray());
        } catch (IOException e) {
            throw new RuntimeException("fail to generate Excel template: " + e.getMessage());
        }
    }

    /**
     * Tạo file mẫu Excel thực chi (không có cột đợt/giải ngân).
     */
    public static ByteArrayInputStream itemsToActualsExcelTemplate() {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet itemSheet = workbook.createSheet(SHEET_ITEMS);
            Row itemHeaderRow = itemSheet.createRow(0);

            CellStyle headerStyle = workbook.createCellStyle();
            headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);

            for (int i = 0; i < HEADERS_ACTUALS.length; i++) {
                Cell cell = itemHeaderRow.createCell(i);
                cell.setCellValue(HEADERS_ACTUALS[i]);
                cell.setCellStyle(headerStyle);
            }

            String[][] itemSamples = {
                    { "1", "Thực phẩm", "Thùng mì tôm", "Hảo Hảo", "Co.opmart", "100", "Thùng", "70000", "7000000",
                            "Cứu trợ miền Trung" },
                    { "2", "Thực phẩm", "Nước đóng chai", "Aquafina", "Đại lý", "50", "Chai", "18000", "900000", "" },
                    { "3", "Nông nghiệp", "Hạt giống rau", "Trang nông", "Cửa hàng vật tư", "50", "Gói", "25000",
                            "1250000", "" }
            };

            for (int i = 0; i < itemSamples.length; i++) {
                Row row = itemSheet.createRow(i + 1);
                for (int j = 0; j < itemSamples[i].length; j++) {
                    row.createCell(j).setCellValue(itemSamples[i][j]);
                }
            }
            for (int i = 0; i < HEADERS_ACTUALS.length; i++)
                itemSheet.autoSizeColumn(i);

            workbook.write(out);
            return new ByteArrayInputStream(out.toByteArray());
        } catch (IOException e) {
            throw new RuntimeException("fail to generate actuals template: " + e.getMessage());
        }
    }

    public static List<com.trustfund.model.request.BulkMilestoneImportRequest> excelToMilestones(InputStream is) {
        try (Workbook workbook = new XSSFWorkbook(is)) {
            List<com.trustfund.model.request.BulkMilestoneImportRequest> milestones = new ArrayList<>();

            // 1. Phân tích Tab Kế hoạch (Sheet 0)
            Sheet planSheet = workbook.getSheet(SHEET_PLAN);
            if (planSheet == null)
                planSheet = workbook.getSheetAt(0);
            Iterator<Row> planRows = planSheet.iterator();
            if (planRows.hasNext()) {
                Row header = planRows.next();
                int titleIdx = -1, startIdx = -1, endIdx = -1, dueIdx = -1, descIdx = -1;
                for (Cell cell : header) {
                    String h = getCellStringValue(cell).toLowerCase();
                    int col = cell.getColumnIndex();
                    if (h.contains("tên") || h.contains("mốc") || h.contains("giải ngân"))
                        titleIdx = col;
                    else if (h.contains("bắt đầu"))
                        startIdx = col;
                    else if (h.contains("kết thúc"))
                        endIdx = col;
                    else if (h.contains("minh chứng") || h.contains("hạn nộp"))
                        dueIdx = col;
                    else if (h.contains("mô tả"))
                        descIdx = col;
                }

                // Fallbacks if headers not found strictly
                if (titleIdx == -1)
                    titleIdx = 1;
                if (startIdx == -1)
                    startIdx = 2;
                if (endIdx == -1)
                    endIdx = 3;
                if (dueIdx == -1)
                    dueIdx = 4;
                if (descIdx == -1)
                    descIdx = 5;
                while (planRows.hasNext()) {
                    Row row = planRows.next();
                    if (isEmptyRow(row))
                        continue;

                    Cell titleCell = titleIdx != -1 ? row.getCell(titleIdx) : null;
                    String title = getCellStringValue(titleCell).trim();
                    if (title.isEmpty())
                        continue;

                    com.trustfund.model.request.BulkMilestoneImportRequest milestone = com.trustfund.model.request.BulkMilestoneImportRequest
                            .builder()
                            .milestoneTitle(title)
                            .startDate(startIdx != -1 ? getCellStringValue(row.getCell(startIdx)) : "")
                            .endDate(endIdx != -1 ? getCellStringValue(row.getCell(endIdx)) : "")
                            .evidenceDueAt(dueIdx != -1 ? getCellStringValue(row.getCell(dueIdx)) : "")
                            .description(descIdx != -1 ? getCellStringValue(row.getCell(descIdx)) : "")
                            .categories(new ArrayList<>())
                            .build();
                    milestones.add(milestone);
                }
            }

            // 2. Phân tích Tab Chi tiết (Sheet 1)
            Sheet itemSheet = workbook.getSheet(SHEET_ITEMS);
            if (itemSheet == null && workbook.getNumberOfSheets() > 1)
                itemSheet = workbook.getSheetAt(1);
            if (itemSheet != null) {
                Iterator<Row> itemRows = itemSheet.iterator();
                if (itemRows.hasNext()) {
                    Row header = itemRows.next();
                    int milIdx = -1, catIdx = -1, nameIdx = -1, qtyIdx = -1, priceIdx = -1, unitIdx = -1,
                            brandIdx = -1, locIdx = -1, noteIdx = -1;
                    for (Cell cell : header) {
                        String h = getCellStringValue(cell).toLowerCase();
                        int col = cell.getColumnIndex();
                        if (h.contains("đợt") || h.contains("mốc"))
                            milIdx = col;
                        else if (h.contains("hạng mục"))
                            catIdx = col;
                        else if (h.contains("tên hàng") || h.contains("vật phẩm") || h.contains("dịch vụ"))
                            nameIdx = col;
                        else if (h.contains("số lượng"))
                            qtyIdx = col;
                        else if (h.contains("đơn giá"))
                            priceIdx = col;
                        else if (h.contains("đơn vị"))
                            unitIdx = col;
                        else if (h.contains("nhãn"))
                            brandIdx = col;
                        else if (h.contains("địa điểm") || h.contains("nơi mua"))
                            locIdx = col;
                        else if (h.contains("ghi chú"))
                            noteIdx = col;
                    }

                    // Fallbacks for Sheet 1 (Campaign Creation Template - 11 columns)
                    if (milIdx == -1)
                        milIdx = 1;
                    if (catIdx == -1)
                        catIdx = 2;
                    if (nameIdx == -1)
                        nameIdx = 3;
                    if (brandIdx == -1)
                        brandIdx = 4;
                    if (locIdx == -1)
                        locIdx = 5;
                    if (qtyIdx == -1)
                        qtyIdx = 6;
                    if (unitIdx == -1)
                        unitIdx = 7;
                    if (priceIdx == -1)
                        priceIdx = 8;
                    if (noteIdx == -1)
                        noteIdx = 10;

                    while (itemRows.hasNext()) {
                        Row row = itemRows.next();
                        if (isEmptyRow(row))
                            continue;

                        String milTitle = milIdx != -1 ? getCellStringValue(row.getCell(milIdx)).trim() : "";
                        String catName = catIdx != -1 ? getCellStringValue(row.getCell(catIdx)).trim() : "";
                        String itemName = nameIdx != -1 ? getCellStringValue(row.getCell(nameIdx)).trim() : "";

                        if (milTitle.isEmpty() || catName.isEmpty() || itemName.isEmpty())
                            continue;

                        // Tìm milestone đã tạo từ tab plan hoặc tạo mới nếu chưa có
                        com.trustfund.model.request.BulkMilestoneImportRequest milestone = milestones.stream()
                                .filter(m -> m.getMilestoneTitle().equalsIgnoreCase(milTitle))
                                .findFirst().orElse(null);
                        if (milestone == null) {
                            milestone = com.trustfund.model.request.BulkMilestoneImportRequest.builder()
                                    .milestoneTitle(milTitle).categories(new ArrayList<>()).build();
                            milestones.add(milestone);
                        }

                        com.trustfund.model.request.CreateExpenditureCatologyRequest category = milestone
                                .getCategories().stream()
                                .filter(c -> c.getName().equalsIgnoreCase(catName))
                                .findFirst().orElse(null);
                        if (category == null) {
                            category = com.trustfund.model.request.CreateExpenditureCatologyRequest.builder()
                                    .name(catName).items(new ArrayList<>()).build();
                            milestone.getCategories().add(category);
                        }

                        CreateExpenditureItemRequest item = new CreateExpenditureItemRequest();
                        item.setName(itemName);

                        item.setExpectedQuantity(getCellIntValue(row.getCell(qtyIdx)));
                        item.setExpectedPrice(getCellBigDecimalValue(row.getCell(priceIdx)));
                        if (unitIdx != -1)
                            item.setExpectedUnit(getCellStringValue(row.getCell(unitIdx)));
                        if (brandIdx != -1)
                            item.setExpectedBrand(getCellStringValue(row.getCell(brandIdx)));
                        if (locIdx != -1)
                            item.setExpectedPurchaseLocation(getCellStringValue(row.getCell(locIdx)));
                        if (noteIdx != -1)
                            item.setExpectedNote(getCellStringValue(row.getCell(noteIdx)));
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
        if (row == null)
            return true;
        for (int i = 0; i < row.getLastCellNum(); i++) {
            Cell cell = row.getCell(i);
            if (cell != null && cell.getCellType() != CellType.BLANK)
                return false;
        }
        return true;
    }

    /**
     * Đọc file Excel nhập vào, trả về danh sách CreateExpenditureItemRequest.
     * Cột: STT (bỏ qua), Tên hàng hóa, Số lượng, Đơn giá, Thành tiền (bỏ qua), Ghi
     * chú
     */
    public static List<CreateExpenditureItemRequest> excelToItems(InputStream is) {
        try (Workbook workbook = WorkbookFactory.create(is)) {
            Sheet sheet = workbook.getSheet(SHEET_ITEMS);
            if (sheet == null) {
                sheet = workbook.getSheet(SHEET);
            }
            if (sheet == null) {
                sheet = workbook.getSheetAt(0);
            }
            Iterator<Row> rows = sheet.iterator();

            List<CreateExpenditureItemRequest> items = new ArrayList<>();
            if (!rows.hasNext())
                return items;

            Row headerRow = rows.next();
            int nameIdx = -1, qtyIdx = -1, priceIdx = -1, noteIdx = -1, unitIdx = -1, brandIdx = -1,
                    locationIdx = -1, catIdx = -1;
            int actualQtyIdx = -1, actualPriceIdx = -1, actualBrandIdx = -1, actualUnitIdx = -1,
                    actualLocationIdx = -1;

            // Map column indices dynamically
            for (Cell cell : headerRow) {
                String header = getCellStringValue(cell).toLowerCase();
                int colIdx = cell.getColumnIndex();
                if (header.contains("tên") || header.contains("hàng") || header.contains("vật"))
                    nameIdx = colIdx;

                // Actual fields (check for "thực" or "actual")
                boolean isActual = header.contains("thực") || header.contains("actual");

                if (header.contains("số lượng") || header.contains("sl")) {
                    if (isActual)
                        actualQtyIdx = colIdx;
                    else
                        qtyIdx = colIdx;
                } else if (header.contains("đơn giá") || header.contains("giá")) {
                    if (isActual)
                        actualPriceIdx = colIdx;
                    else
                        priceIdx = colIdx;
                } else if (header.contains("nhãn") || header.contains("brand")) {
                    if (isActual)
                        actualBrandIdx = colIdx;
                    else
                        brandIdx = colIdx;
                } else if (header.contains("đơn vị") || header.contains("unit")) {
                    if (isActual)
                        actualUnitIdx = colIdx;
                    else
                        unitIdx = colIdx;
                } else if (header.contains("điểm") || header.contains("location") || header.contains("nơi mua")) {
                    if (isActual)
                        actualLocationIdx = colIdx;
                    else
                        locationIdx = colIdx;
                } else if (header.contains("ghi chú") || header.contains("note"))
                    noteIdx = colIdx;
            }

            // Fallback for actuals template (10 columns: STT, Category, Name, Brand,
            // Location, Qty, Unit, Price, Total, Note)
            if (nameIdx == -1)
                nameIdx = 2;
            if (catIdx == -1)
                catIdx = 1;
            if (brandIdx == -1)
                brandIdx = 3;
            if (locationIdx == -1)
                locationIdx = 4;
            if (actualQtyIdx == -1)
                actualQtyIdx = 5;
            if (actualUnitIdx == -1)
                actualUnitIdx = 6;
            if (actualPriceIdx == -1)
                actualPriceIdx = 7;
            if (noteIdx == -1)
                noteIdx = 9;

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
                if (isEmptyRow)
                    continue;

                CreateExpenditureItemRequest item = new CreateExpenditureItemRequest();

                if (nameIdx != -1) {
                    Cell c = currentRow.getCell(nameIdx, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
                    item.setName(getCellStringValue(c));
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
                    item.setExpectedNote(getCellStringValue(c));
                }
                if (unitIdx != -1) {
                    Cell c = currentRow.getCell(unitIdx, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
                    item.setExpectedUnit(getCellStringValue(c));
                }
                if (brandIdx != -1) {
                    Cell c = currentRow.getCell(brandIdx, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
                    item.setExpectedBrand(getCellStringValue(c));
                }
                if (locationIdx != -1) {
                    Cell c = currentRow.getCell(locationIdx, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
                    item.setExpectedPurchaseLocation(getCellStringValue(c));
                }

                // Actuals mapping
                if (actualQtyIdx != -1) {
                    Cell c = currentRow.getCell(actualQtyIdx, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
                    item.setActualQuantity(getCellIntValue(c));
                }
                if (actualPriceIdx != -1) {
                    Cell c = currentRow.getCell(actualPriceIdx, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
                    item.setActualPrice(getCellBigDecimalValue(c));
                }
                if (actualBrandIdx != -1) {
                    Cell c = currentRow.getCell(actualBrandIdx, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
                    item.setActualBrand(getCellStringValue(c));
                }
                if (actualUnitIdx != -1) {
                    Cell c = currentRow.getCell(actualUnitIdx, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
                    item.setActualUnit(getCellStringValue(c));
                }
                if (actualLocationIdx != -1) {
                    Cell c = currentRow.getCell(actualLocationIdx, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
                    item.setActualPurchaseLocation(getCellStringValue(c));
                }

                // If name is empty, skip this row (probably end of data)
                if (item.getName() == null || item.getName().trim().isEmpty()) {
                    continue;
                }

                if (item.getActualPrice() == null) {
                    item.setActualPrice(BigDecimal.ZERO);
                }
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
        if (cell == null)
            return "";
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue().trim();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return DATE_FORMAT.format(cell.getDateCellValue());
                }
                double numericVal = cell.getNumericCellValue();
                // Excel date serial numbers: 1/1/2020 = 43831, 31/12/2030 = 47847
                // If value is in date range and column likely holds a date, format it
                if (numericVal >= 40000 && numericVal <= 60000) {
                    try {
                        java.util.Date dateVal = DateUtil.getJavaDate(numericVal);
                        return DATE_FORMAT.format(dateVal);
                    } catch (Exception ignored) {
                        // Not a valid date serial, fall through to number formatting
                    }
                }
                if (numericVal == (long) numericVal) {
                    return String.valueOf((long) numericVal);
                }
                return String.valueOf(numericVal);
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
        if (cell == null)
            return 1;
        switch (cell.getCellType()) {
            case NUMERIC:
                return (int) Math.round(cell.getNumericCellValue());
            case STRING:
                try {
                    String str = cell.getStringCellValue().trim().replaceAll("[^0-9]", "");
                    return str.isEmpty() ? 0 : Integer.parseInt(str);
                } catch (Exception e) {
                    return 0;
                }
            case FORMULA:
                try {
                    return (int) Math.round(cell.getNumericCellValue());
                } catch (Exception e) {
                    return 0;
                }
            default:
                return 0;
        }
    }

    private static BigDecimal getCellBigDecimalValue(Cell cell) {
        if (cell == null)
            return BigDecimal.ZERO;
        switch (cell.getCellType()) {
            case NUMERIC:
                return BigDecimal.valueOf(cell.getNumericCellValue());
            case STRING:
                try {
                    String val = cell.getStringCellValue().trim()
                            .replaceAll("[^0-9.,]", "");
                    if (val.isEmpty())
                        return BigDecimal.ZERO;
                    // Handle formats like "1.000,00" or "1000.00"
                    if (val.contains(",") && val.contains(".")) {
                        if (val.indexOf(".") < val.indexOf(",")) {
                            val = val.replace(".", "").replace(",", "."); // 1.000,00 -> 1000.00
                        } else {
                            val = val.replace(",", ""); // 1,000.00 -> 1000.00
                        }
                    } else if (val.contains(",")) {
                        // If only comma, usually it's the decimal separator in VN
                        val = val.replace(",", ".");
                    }
                    return new BigDecimal(val);
                } catch (Exception e) {
                    return BigDecimal.ZERO;
                }
            case FORMULA:
                try {
                    return BigDecimal.valueOf(cell.getNumericCellValue());
                } catch (Exception e) {
                    return BigDecimal.ZERO;
                }
            default:
                return BigDecimal.ZERO;
        }
    }
}
