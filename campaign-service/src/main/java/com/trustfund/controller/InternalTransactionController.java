package com.trustfund.controller;

import com.trustfund.model.InternalTransaction;
import com.trustfund.model.request.InternalTransactionRequest;
import com.trustfund.service.InternalTransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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
    @Operation(summary = "Tạo giao dịch nội bộ", description = "Chuyên dùng cho Quỹ chung gửi/thu hồi tiền (Staff/Admin)")
    @PreAuthorize("hasAnyRole('STAFF', 'ADMIN')")
    public ResponseEntity<InternalTransaction> create(@Valid @RequestBody InternalTransactionRequest request) {
        InternalTransaction created = transactionService.createTransaction(
                request.getFromCampaignId(),
                request.getToCampaignId(),
                request.getAmount(),
                request.getType(),
                request.getReason());
        return ResponseEntity.ok(created);
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
}
