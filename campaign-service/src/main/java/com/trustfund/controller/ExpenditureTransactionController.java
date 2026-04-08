package com.trustfund.controller;

import com.trustfund.model.ExpenditureTransaction;
import com.trustfund.model.response.ExpenditureTransactionResponse;
import com.trustfund.repository.ExpenditureTransactionRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/expenditures/transactions")
@RequiredArgsConstructor
@Tag(name = "Expenditure Transactions", description = "API quản lý giao dịch chi tiêu/hoàn tiền")
public class ExpenditureTransactionController {

    private final ExpenditureTransactionRepository repository;

    @GetMapping("/type/{type}/status/{status}")
    @Operation(summary = "Lấy danh sách giao dịch theo loại và trạng thái", description = "Lấy danh sách PAYOUT hoặc REFUND có status nhất định")
    public ResponseEntity<List<ExpenditureTransactionResponse>> getByTypeAndStatus(
            @PathVariable("type") String type,
            @PathVariable("status") String status) {

        List<ExpenditureTransaction> transactions = repository.findByTypeAndStatus(type, status);

        List<ExpenditureTransactionResponse> responses = transactions.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(responses);
    }

    @GetMapping("/type/{type}/status/{status}/paginated")
    @Operation(summary = "Lấy danh sách giao dịch phân trang", description = "Lấy danh sách PAYOUT hoặc REFUND có status nhất định với phân trang")
    public ResponseEntity<Page<ExpenditureTransactionResponse>> getByTypeAndStatusPaginated(
            @PathVariable("type") String type,
            @PathVariable("status") String status,
            Pageable pageable) {

        Page<ExpenditureTransaction> transactions = repository.findByTypeAndStatus(type, status, pageable);
        Page<ExpenditureTransactionResponse> responses = transactions.map(this::mapToResponse);

        return ResponseEntity.ok(responses);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Lấy chi tiết giao dịch theo ID", description = "Lấy chi tiết một giao dịch chi tiêu/hoàn tiền theo ID")
    public ResponseEntity<ExpenditureTransactionResponse> getById(@PathVariable("id") Long id) {
        return repository.findById(id)
                .map(this::mapToResponse)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    private ExpenditureTransactionResponse mapToResponse(ExpenditureTransaction tx) {
        return ExpenditureTransactionResponse.builder()
                .id(tx.getId())
                .expenditureId(tx.getExpenditure() != null ? tx.getExpenditure().getId() : null)
                .fromUserId(tx.getFromUserId())
                .toUserId(tx.getToUserId())
                .amount(tx.getAmount())
                .fromBankCode(tx.getFromBankCode())
                .fromAccountNumber(tx.getFromAccountNumber())
                .fromAccountHolderName(tx.getFromAccountHolderName())
                .toBankCode(tx.getToBankCode())
                .toAccountNumber(tx.getToAccountNumber())
                .toAccountHolderName(tx.getToAccountHolderName())
                .type(tx.getType())
                .status(tx.getStatus())
                .proofUrl(tx.getProofUrl())
                .createdAt(tx.getCreatedAt())
                .build();
    }
}
