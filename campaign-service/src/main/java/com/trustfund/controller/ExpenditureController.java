package com.trustfund.controller;

import com.trustfund.model.Expenditure;
import com.trustfund.model.ExpenditureItem;
import com.trustfund.model.request.CreateExpenditureRequest;
import com.trustfund.service.ExpenditureService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/expenditures")
@RequiredArgsConstructor
@Tag(name = "Expenditures", description = "API quản lý chi tiêu chiến dịch")
public class ExpenditureController {

    private final ExpenditureService expenditureService;

    @PostMapping
    @Operation(summary = "Tạo mới khoản chi tiêu", description = "Tạo mới khoản chi tiêu và các hạng mục đi kèm. Tự động duyệt nếu là quỹ mục tiêu.")
    public ResponseEntity<Expenditure> create(@Valid @RequestBody CreateExpenditureRequest request) {
        Expenditure created = expenditureService.createExpenditure(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/campaign/{campaignId}")
    @Operation(summary = "Lấy danh sách chi tiêu của chiến dịch", description = "Lấy toàn bộ các khoản chi tiêu thuộc về một chiến dịch.")
    public List<Expenditure> getByCampaignId(@PathVariable Long campaignId) {
        return expenditureService.getExpendituresByCampaign(campaignId);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Lấy chi tiết khoản chi tiêu", description = "Lấy thông tin chi tiết của một khoản chi tiêu theo ID.")
    public ResponseEntity<Expenditure> getById(@PathVariable Long id) {
        return ResponseEntity.ok(expenditureService.getExpenditureById(id));
    }

    @PutMapping("/{id}/status")
    @Operation(summary = "Cập nhật trạng thái chi tiêu", description = "Cập nhật trạng thái duyệt của khoản chi tiêu (Yêu cầu quyền Staff hoặc Admin).")
    @PreAuthorize("hasAnyRole('STAFF', 'ADMIN')")
    public ResponseEntity<Expenditure> updateStatus(@PathVariable Long id, @RequestParam String status) {
        return ResponseEntity.ok(expenditureService.updateExpenditureStatus(id, status));
    }

    @PutMapping("/{id}/actuals")
    @Operation(summary = "Cập nhật thực tế chi tiêu", description = "Cập nhật số lượng và đơn giá thực tế sau khi mua sắm.")
    public ResponseEntity<Expenditure> updateActuals(@PathVariable Long id, @Valid @RequestBody com.trustfund.model.request.UpdateExpenditureActualsRequest request) {
        return ResponseEntity.ok(expenditureService.updateExpenditureActuals(id, request));
    }

    @PostMapping("/{id}/request-withdrawal")
    @Operation(summary = "Yêu cầu rút tiền", description = "Đánh dấu yêu cầu rút tiền cho khoản chi. Nếu là quỹ mục tiêu sẽ đóng luôn đợt chi này.")
    public ResponseEntity<Expenditure> requestWithdrawal(
            @PathVariable Long id, 
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME) java.time.LocalDateTime evidenceDueAt) {
        return ResponseEntity.ok(expenditureService.requestWithdrawal(id, evidenceDueAt));
    }

    @GetMapping("/{id}/items")
    @Operation(summary = "Lấy các hạng mục của chi tiêu", description = "Lấy danh sách các hạng mục (ExpenditureItems) thuộc về một khoản chi tiêu.")
    public List<ExpenditureItem> getItems(@PathVariable Long id) {
        return expenditureService.getExpenditureItems(id);
    }
}
