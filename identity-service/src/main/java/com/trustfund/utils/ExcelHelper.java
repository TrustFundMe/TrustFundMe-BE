package com.trustfund.utils;

import com.trustfund.model.User;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class ExcelHelper {
    public static String TYPE = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
    static String[] HEADERS = { "ID", "Email", "Full Name", "Phone Number", "Role", "Is Active", "Verified", "Ban Reason" };
    static String SHEET = "Users";

    public static boolean hasExcelFormat(MultipartFile file) {
        if (TYPE.equals(file.getContentType())) return true;
        String filename = file.getOriginalFilename();
        if (filename == null) return false;
        String lower = filename.toLowerCase();
        return lower.endsWith(".xlsx") || lower.endsWith(".xls");
    }

    public static ByteArrayInputStream usersToExcel(List<User> users) {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet(SHEET);

            // Header
            Row headerRow = sheet.createRow(0);
            for (int col = 0; col < HEADERS.length; col++) {
                Cell cell = headerRow.createCell(col);
                cell.setCellValue(HEADERS[col]);
            }

            int rowIdx = 1;
            for (User user : users) {
                Row row = sheet.createRow(rowIdx++);

                row.createCell(0).setCellValue(user.getId() != null ? user.getId() : 0);
                row.createCell(1).setCellValue(user.getEmail() != null ? user.getEmail() : "");
                row.createCell(2).setCellValue(user.getFullName() != null ? user.getFullName() : "");
                row.createCell(3).setCellValue(user.getPhoneNumber() != null ? user.getPhoneNumber() : "");
                row.createCell(4).setCellValue(user.getRole() != null ? user.getRole().toString() : "");
                row.createCell(5).setCellValue(user.getIsActive() != null && user.getIsActive() ? "Yes" : "No");
                row.createCell(6).setCellValue(user.getVerified() != null && user.getVerified() ? "Yes" : "No");
                row.createCell(7).setCellValue(user.getBanReason() != null ? user.getBanReason() : "");
            }

            workbook.write(out);
            return new ByteArrayInputStream(out.toByteArray());
        } catch (IOException e) {
            throw new RuntimeException("fail to import data to Excel file: " + e.getMessage());
        }
    }

    public static ByteArrayInputStream usersToExcelTemplate() {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet(SHEET);

            // Header
            Row headerRow = sheet.createRow(0);
            for (int col = 0; col < HEADERS.length; col++) {
                Cell cell = headerRow.createCell(col);
                cell.setCellValue(HEADERS[col]);
                CellStyle headerStyle = workbook.createCellStyle();
                Font headerFont = workbook.createFont();
                headerFont.setBold(true);
                headerStyle.setFont(headerFont);
                headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
                headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
                cell.setCellStyle(headerStyle);
            }

            // Sample row (with placeholder values to guide users)
            Row sampleRow = sheet.createRow(1);
            sampleRow.createCell(0).setCellValue(""); // ID - auto-generated
            sampleRow.createCell(1).setCellValue("nguoidung_moi@example.com"); // Email
            sampleRow.createCell(2).setCellValue("Nguyễn Văn Mẫu"); // Full Name
            sampleRow.createCell(3).setCellValue("0912345678"); // Phone Number
            sampleRow.createCell(4).setCellValue("USER"); // Role
            sampleRow.createCell(5).setCellValue("Yes"); // Is Active
            sampleRow.createCell(6).setCellValue("No"); // Verified
            sampleRow.createCell(7).setCellValue(""); // Ban Reason

            // Auto-size columns
            for (int col = 0; col < HEADERS.length; col++) {
                sheet.autoSizeColumn(col);
            }

            workbook.write(out);
            return new ByteArrayInputStream(out.toByteArray());
        } catch (IOException e) {
            throw new RuntimeException("fail to generate Excel template: " + e.getMessage());
        }
    }

    public static List<User> excelToUsers(InputStream is) {
        try (Workbook workbook = new XSSFWorkbook(is)) {
            Sheet sheet = workbook.getSheet(SHEET);
            if (sheet == null) {
                sheet = workbook.getSheetAt(0);
            }
            Iterator<Row> rows = sheet.iterator();

            List<User> users = new ArrayList<>();

            int rowNumber = 0;
            while (rows.hasNext()) {
                Row currentRow = rows.next();

                // skip header
                if (rowNumber == 0) {
                    rowNumber++;
                    continue;
                }

                Iterator<Cell> cellsInRow = currentRow.iterator();

                User user = new User();

                int cellIdx = 0;
                while (cellsInRow.hasNext()) {
                    Cell currentCell = cellsInRow.next();

                    switch (cellIdx) {
                        case 1: // Email
                            user.setEmail(currentCell.getStringCellValue());
                            break;
                        case 2: // Full Name
                            user.setFullName(currentCell.getStringCellValue());
                            break;
                        case 3: // Phone Number
                            if (currentCell.getCellType() == CellType.STRING) {
                                user.setPhoneNumber(currentCell.getStringCellValue());
                            } else if (currentCell.getCellType() == CellType.NUMERIC) {
                                user.setPhoneNumber(String.valueOf((long) currentCell.getNumericCellValue()));
                            }
                            break;
                        case 4: // Role
                            try {
                                user.setRole(User.Role.valueOf(currentCell.getStringCellValue().toUpperCase()));
                            } catch (Exception e) {
                                user.setRole(User.Role.USER);
                            }
                            break;
                        default:
                            break;
                    }

                    cellIdx++;
                }
                
                // Set default password if importing new users
                user.setPassword("TrustFund123@"); // Default password, should be changed
                user.setIsActive(true);
                user.setVerified(false);
                
                users.add(user);
            }

            return users;
        } catch (IOException e) {
            throw new RuntimeException("fail to parse Excel file: " + e.getMessage());
        }
    }
}
