package com.trustfund.controller;

import com.trustfund.model.response.ExpenditureResponse;
import com.trustfund.model.response.ExpenditureItemResponse;
import com.trustfund.model.request.CreateExpenditureItemRequest;
import com.trustfund.model.request.CreateExpenditureRequest;
import com.trustfund.model.request.UpdateExpenditureActualsRequest;
import com.trustfund.model.request.UpdateDisbursementProofRequest;
import com.trustfund.model.request.CreateRefundRequest;
import com.trustfund.model.response.ExpenditureTransactionResponse;
import com.trustfund.service.ExpenditureService;
import com.trustfund.utils.ExpenditureExcelHelper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/expenditures")
@RequiredArgsConstructor
@Tag(name = "Expenditures", description = "API quản lý chi tiêu chiến dịch")
public class ExpenditureController {

    private final ExpenditureService expenditureService;

    @PostMapping
    @Operation(summary = "Tạo mới khoản chi tiêu", description = "Tạo mới khoản chi tiêu và các hạng mục đi kèm. Tự động duyệt nếu là quỹ mục tiêu.")
    public ResponseEntity<ExpenditureResponse> create(@Valid @RequestBody CreateExpenditureRequest request) {
        ExpenditureResponse created = expenditureService.createExpenditure(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/campaign/{campaignId}")
    @Operation(summary = "Lấy danh sách chi tiêu của chiến dịch", description = "Lấy toàn bộ các khoản chi tiêu thuộc về một chiến dịch.")
    public ResponseEntity<List<ExpenditureResponse>> getByCampaignId(@PathVariable("campaignId") Long campaignId) {
        return ResponseEntity.ok(expenditureService.getExpendituresByCampaign(campaignId));
    }

    @GetMapping("/campaign/{campaignId}/items")
    @Operation(summary = "Lấy toàn bộ hạng mục chi tiêu của chiến dịch", description = "Lấy tất cả các ExpenditureItem thuộc về các Expenditure của một chiến dịch.")
    public ResponseEntity<List<ExpenditureItemResponse>> getItemsByCampaignId(
            @PathVariable("campaignId") Long campaignId) {
        return ResponseEntity.ok(expenditureService.getExpenditureItemsByCampaign(campaignId));
    }

    @GetMapping("/campaign/{campaignId}/items/approved")
    @Operation(summary = "Lấy items từ expenditure APPROVED mới nhất của campaign", description = "Chỉ trả về items của expenditure có status APPROVED mới nhất. Nếu không có thì trả về 403.")
    public ResponseEntity<?> getApprovedItemsByCampaignId(@PathVariable("campaignId") Long campaignId) {
        List<ExpenditureItemResponse> items = expenditureService.getApprovedItemsByCampaign(campaignId);
        if (items == null) {
            return ResponseEntity.status(403).body(java.util.Map.of("message",
                    "Chiến dịch chưa có đợt chi tiêu nào được duyệt hoặc đang trong quá trình giải ngân."));
        }
        return ResponseEntity.ok(items);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Lấy chi tiết khoản chi tiêu", description = "Lấy thông tin chi tiết của một khoản chi tiêu theo ID.")
    public ResponseEntity<ExpenditureResponse> getById(@PathVariable("id") Long id) {
        return ResponseEntity.ok(expenditureService.getExpenditureById(id));
    }

    @PutMapping("/{id}/status")
    @Operation(summary = "Cập nhật trạng thái chi tiêu", description = "Cập nhật trạng thái duyệt của khoản chi tiêu (Yêu cầu quyền Staff hoặc Admin).")
    @PreAuthorize("hasAnyRole('STAFF', 'ADMIN')")
    public ResponseEntity<ExpenditureResponse> updateStatus(@PathVariable("id") Long id,
            @Valid @RequestBody com.trustfund.model.request.ReviewExpenditureRequest request) {
        return ResponseEntity.ok(expenditureService.updateExpenditureStatus(id, request));
    }

    @PutMapping("/{id}/actuals")
    @Operation(summary = "Cập nhật thực tế chi tiêu", description = "Cập nhật số lượng và đơn giá thực tế sau khi mua sắm.")
    public ResponseEntity<ExpenditureResponse> updateActuals(@PathVariable("id") Long id,
            @Valid @RequestBody UpdateExpenditureActualsRequest request) {
        return ResponseEntity.ok(expenditureService.updateExpenditureActuals(id, request));
    }

    @PostMapping("/{id}/request-withdrawal")
    @Operation(summary = "Yêu cầu rút tiền", description = "Đánh dấu yêu cầu rút tiền cho khoản chi. Nếu là quỹ mục tiêu sẽ đóng luôn đợt chi này.")
    public ResponseEntity<ExpenditureResponse> requestWithdrawal(
            @PathVariable("id") Long id,
            @RequestParam(name = "evidenceDueAt", required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME) java.time.LocalDateTime evidenceDueAt,
            @RequestParam(name = "withdrawAmount", required = false) java.math.BigDecimal withdrawAmount) {
        return ResponseEntity.ok(expenditureService.requestWithdrawal(id, evidenceDueAt, withdrawAmount));
    }

    @GetMapping("/{id}/items")
    @Operation(summary = "Lấy các hạng mục của chi tiêu", description = "Lấy danh sách các hạng mục (ExpenditureItems) thuộc về một khoản chi tiêu.")
    public ResponseEntity<List<ExpenditureItemResponse>> getItems(@PathVariable("id") Long id) {
        return ResponseEntity.ok(expenditureService.getExpenditureItems(id));
    }

    @PutMapping("/{id}/disbursement-proof")
    @Operation(summary = "Cập nhật minh chứng giải ngân", description = "Cập nhật URL ảnh minh chứng chuyển khoản (Screenshot) và chuyển trạng thái minh chứng sang COMPLETED.")
    public ResponseEntity<ExpenditureResponse> updateDisbursementProof(@PathVariable("id") Long id,
            @Valid @RequestBody UpdateDisbursementProofRequest request) {
        return ResponseEntity.ok(expenditureService.updateDisbursementProof(id, request));
    }

    @PostMapping("/{id}/items")
    @Operation(summary = "Thêm hạng mục vào khoản chi tiêu", description = "Thêm một danh sách các hạng mục mới vào một khoản chi tiêu đã tồn tại.")
    public ResponseEntity<ExpenditureResponse> addItems(@PathVariable("id") Long id,
            @Valid @RequestBody List<CreateExpenditureItemRequest> items) {
        return ResponseEntity.ok(expenditureService.addItemsToExpenditure(id, items));
    }

    @GetMapping("/items/{itemId}")
    @Operation(summary = "Lấy chi tiết hạng mục chi tiêu", description = "Lấy thông tin chi tiết của một hạng mục chi tiêu theo ID.")
    public ResponseEntity<ExpenditureItemResponse> getItemById(@PathVariable("itemId") Long itemId) {
        return ResponseEntity.ok(expenditureService.getExpenditureItemById(itemId));
    }

    @PutMapping("/items/{itemId}/update-quantity")
    @Operation(summary = "Cập nhật số lượng vật phẩm (từ hệ thống thanh toán)", description = "Cập nhật số lượng Tổng quyên góp và còn lại của một hạng mục.")
    public ResponseEntity<Void> updateQuantity(@PathVariable("itemId") Long itemId,
            @RequestParam("amount") Integer amount) {
        expenditureService.updateExpenditureItemQuantity(itemId, amount);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/items/{itemId}")
    @Operation(summary = "Xóa hạng mục chi tiêu", description = "Xóa một hạng mục chi tiêu cụ thể theo ID.")
    public ResponseEntity<Void> deleteItem(@PathVariable("itemId") Long itemId) {
        expenditureService.deleteExpenditureItem(itemId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/evidence-status")
    @Operation(summary = "Cập nhật trạng thái minh chứng", description = "Cập nhật trạng thái của bằng chứng chi tiêu (Ví dụ: SUBMITTED).")
    public ResponseEntity<ExpenditureResponse> updateEvidenceStatus(@PathVariable("id") Long id,
            @RequestParam("status") String status) {
        return ResponseEntity.ok(expenditureService.updateEvidenceStatus(id, status));
    }

    @PostMapping("/{id}/refund")
    @Operation(summary = "Tạo yêu cầu hoàn tiền dư", description = "Fund owner gửi tiền dư về cho admin. Bao gồm số tài khoản người gửi, người nhận và minh chứng chuyển khoản.")
    public ResponseEntity<ExpenditureTransactionResponse> createRefund(
            @PathVariable("id") Long expenditureId,
            @Valid @RequestBody CreateRefundRequest request,
            @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        ExpenditureTransactionResponse result = expenditureService.createRefund(
                expenditureId, request.getAmount(), userId, request.getProofUrl(),
                request.getFromBankCode(), request.getFromAccountNumber(), request.getFromAccountHolderName(),
                request.getToBankCode(), request.getToAccountNumber(), request.getToAccountHolderName());
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    @GetMapping("/campaign/{campaignId}/export")
    @Operation(summary = "Xuất Excel hạng mục chi tiêu", description = "Xuất toàn bộ hạng mục chi tiêu của chiến dịch ra file Excel. Tên file: KhoanChi_ddMMyyyy.xlsx")
    public ResponseEntity<byte[]> exportItemsToExcel(@PathVariable("campaignId") Long campaignId) {
        java.io.ByteArrayInputStream data = expenditureService.exportItemsToExcel(campaignId);
        String filename = "KhoanChi_"
                + java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("ddMMyyyy")) + ".xlsx";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(
                        MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(data.readAllBytes());
    }

    @GetMapping("/transactions")
    @Operation(summary = "Lấy tất cả giao dịch PAYOUT/REFUND", description = "Trả về toàn bộ ExpenditureTransaction để admin quản lý giải ngân/hoàn tiền.")
    @PreAuthorize("hasAnyRole('STAFF', 'ADMIN')")
    public ResponseEntity<List<ExpenditureTransactionResponse>> getAllTransactions() {
        return ResponseEntity.ok(expenditureService.getAllTransactions());
    }

    @GetMapping("/import/template")
    @Operation(summary = "Tải file mẫu Excel nhập khoản chi", description = "Tải file mẫu Excel với dữ liệu minh hoạ để người dùng nhập theo.")
    public ResponseEntity<byte[]> downloadTemplate() {
        java.io.ByteArrayInputStream data = expenditureService.exportItemsToExcelTemplate();
        String filename = "KhoanChi_Mau_"
                + java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("ddMMyyyy")) + ".xlsx";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(
                        MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(data.readAllBytes());
    }

    @PostMapping("/import")
    @Operation(summary = "Nhập hạng mục chi tiêu từ Excel", description = "Đọc file Excel và trả về danh sách hạng mục chi tiêu để xem trước trước khi tạo khoản chi.")
    public ResponseEntity<?> importItemsFromExcel(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(java.util.Map.of("error", "File không được để trống"));
        }
        if (!ExpenditureExcelHelper.hasExcelFormat(file)) {
            return ResponseEntity.badRequest()
                    .body(java.util.Map.of("error", "Vui lòng upload file Excel (.xlsx hoặc .xls)"));
        }
        try {
            java.util.List<CreateExpenditureItemRequest> items = ExpenditureExcelHelper
                    .excelToItems(file.getInputStream());
            return ResponseEntity.ok(java.util.Map.of(
                    "success", true,
                    "message", "Đọc " + items.size() + " hạng mục từ file Excel thành công",
                    "data", items));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(java.util.Map.of("error", "Không thể đọc file Excel: " + e.getMessage()));
        }
    }

    @GetMapping("/status/{status}")
    @Operation(summary = "Lấy danh sách chi tiêu theo trạng thái", description = "Lấy danh sách các khoản chi tiêu dựa trên trạng thái (Ví dụ: DISBURSED).")
    public ResponseEntity<List<ExpenditureResponse>> getByStatus(@PathVariable("status") String status) {
        return ResponseEntity.ok(expenditureService.getExpendituresByStatus(status));
    }
}
