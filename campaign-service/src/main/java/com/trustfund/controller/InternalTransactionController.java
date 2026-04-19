package com.trustfund.controller;

import com.trustfund.model.InternalTransaction;
import com.trustfund.model.request.InternalTransactionRequest;
import com.trustfund.service.InternalTransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/internal-transactions")
@RequiredArgsConstructor
@Tag(name = "Internal Transactions", description = "Quản lý giao dịch nội bộ / Quỹ chung")
public class InternalTransactionController {

    private final InternalTransactionService transactionService;

    @PostMapping
    @Operation(summary = "Tạo giao dịch nội bộ (Propose/Execute)", description = "Chuyên dùng cho Quỹ chung gửi/thu hồi tiền (Staff/Admin)")
    @PreAuthorize("hasAnyRole('STAFF', 'ADMIN')")
    public ResponseEntity<InternalTransaction> create(@Valid @RequestBody InternalTransactionRequest request) {
        InternalTransaction created = transactionService.createTransaction(
                request.getFromCampaignId(),
                request.getToCampaignId(),
                request.getAmount(),
                request.getType(),
                request.getReason(),
                request.getCreatedByStaffId(),
                request.getEvidenceImageId(),
                request.getStatus());
        return ResponseEntity.ok(created);
    }

    @GetMapping
    @Operation(summary = "Lấy tất cả giao dịch nội bộ")
    @PreAuthorize("hasAnyRole('STAFF', 'ADMIN')")
    public ResponseEntity<List<InternalTransaction>> getAll() {
        return ResponseEntity.ok(transactionService.getAll());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Lấy giao dịch nội bộ theo ID")
    @PreAuthorize("hasAnyRole('STAFF', 'ADMIN')")
    public ResponseEntity<InternalTransaction> getById(@PathVariable Long id) {
        return ResponseEntity.ok(transactionService.getById(id));
    }

    @PutMapping("/{id}/status")
    @Operation(summary = "Cập nhật trạng thái giao dịch (Duyệt/Hoàn tất)", description = "Chỉ dùng cho Admin duyệt hoặc hoàn tất giao dịch")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<InternalTransaction> updateStatus(
            @PathVariable Long id,
            @RequestParam com.trustfund.model.enums.InternalTransactionStatus status) {
        return ResponseEntity.ok(transactionService.updateTransactionStatus(id, status));
    }

    @PutMapping("/{id}/evidence")
    @Operation(summary = "Cập nhật ảnh minh chứng giao dịch", description = "Dành cho Admin/Staff tải lên ảnh minh chứng")
    @PreAuthorize("hasAnyRole('STAFF', 'ADMIN')")
    public ResponseEntity<InternalTransaction> updateEvidence(
            @PathVariable Long id,
            @RequestParam Long evidenceImageId) {
        return ResponseEntity.ok(transactionService.updateEvidence(id, evidenceImageId));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Xóa giao dịch nội bộ (chỉ khi chưa COMPLETED)")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        transactionService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/stats")
    @Operation(summary = "Lấy thống kê Quỹ chung", description = "Trả về Số dư, Tổng Cứu trợ, Tổng Thu hồi")
    @PreAuthorize("hasAnyRole('STAFF', 'ADMIN')")
    public ResponseEntity<Map<String, BigDecimal>> getStats() {
        return ResponseEntity.ok(transactionService.getGeneralFundStats());
    }

    @GetMapping("/history")
    @Operation(summary = "Lấy lịch sử giao dịch Quỹ chung", description = "Trả về danh sách giao dịch liên quan đến Quỹ chung")
    @PreAuthorize("hasAnyRole('STAFF', 'ADMIN')")
    public ResponseEntity<List<InternalTransaction>> getHistory() {
        return ResponseEntity.ok(transactionService.getGeneralFundHistory());
    }

    @GetMapping("/history/paginated")
    @Operation(summary = "Lấy lịch sử giao dịch Quỹ chung (Phân trang)", description = "Trả về danh sách giao dịch liên quan đến Quỹ chung có phân trang")
    @PreAuthorize("hasAnyRole('STAFF', 'ADMIN')")
    public ResponseEntity<Page<InternalTransaction>> getHistoryPaginated(Pageable pageable) {
        return ResponseEntity.ok(transactionService.getGeneralFundHistoryPaginated(pageable));
    }

    @GetMapping("/campaign/{campaignId}/received")
    @Operation(summary = "Lấy các giao dịch nhận từ Quỹ chung của một Campaign (Cho Analytics)")
    public ResponseEntity<List<InternalTransaction>> getReceivedTransactions(@PathVariable Long campaignId) {
        return ResponseEntity.ok(transactionService.getCompletedTransactionsToCampaign(campaignId));
    }
}
