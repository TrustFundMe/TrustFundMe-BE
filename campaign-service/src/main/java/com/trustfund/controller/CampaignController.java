package com.trustfund.controller;

import com.trustfund.model.Campaign;
import com.trustfund.model.request.CreateCampaignRequest;
import com.trustfund.model.request.UpdateCampaignRequest;
import com.trustfund.service.CampaignService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/campaigns")
@RequiredArgsConstructor
@Tag(name = "Campaigns", description = "API quản lý chiến dịch")
public class CampaignController {

    private final CampaignService campaignService;

    @GetMapping
    @Operation(summary = "Lấy danh sách campaign")
    public List<Campaign> getAll() {
        return campaignService.getAll();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Lấy campaign theo ID")
    public ResponseEntity<Campaign> getById(@PathVariable Long id) {
        return ResponseEntity.ok(campaignService.getById(id));
    }

    @GetMapping("/fund-owner/{fundOwnerId}")
    @Operation(summary = "Lấy campaign theo fund owner")
    public List<Campaign> getByFundOwnerId(@PathVariable Long fundOwnerId) {
        return campaignService.getByFundOwnerId(fundOwnerId);
    }

    @PostMapping
    @Operation(summary = "Tạo campaign mới")
    public ResponseEntity<Campaign> create(@Valid @RequestBody CreateCampaignRequest request) {
        Campaign created = campaignService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Cập nhật campaign")
    public ResponseEntity<Campaign> update(@PathVariable Long id, @Valid @RequestBody UpdateCampaignRequest request) {
        return ResponseEntity.ok(campaignService.update(id, request));
    }

    @PutMapping("/{id}/mark-deleted")
    @Operation(summary = "Đánh dấu xóa (cập nhật status = DELETED)")
    public ResponseEntity<Campaign> markAsDeleted(@PathVariable Long id) {
        return ResponseEntity.ok(campaignService.markAsDeleted(id));
    }
}
