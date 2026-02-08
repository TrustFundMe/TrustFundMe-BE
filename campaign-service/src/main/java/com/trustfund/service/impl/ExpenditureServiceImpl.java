package com.trustfund.service.impl;

import com.trustfund.model.Campaign;
import com.trustfund.model.Expenditure;
import com.trustfund.model.ExpenditureItem;
import com.trustfund.model.request.CreateExpenditureRequest;
import com.trustfund.repository.ExpenditureItemRepository;
import com.trustfund.repository.ExpenditureRepository;
import com.trustfund.service.CampaignService;
import com.trustfund.service.ExpenditureService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ExpenditureServiceImpl implements ExpenditureService {

    private final ExpenditureRepository expenditureRepository;
    private final ExpenditureItemRepository expenditureItemRepository;
    private final CampaignService campaignService;

    @Override
    @Transactional
    public Expenditure createExpenditure(CreateExpenditureRequest request) {
        Campaign campaign = campaignService.getById(request.getCampaignId());

        // Logic: AUTHORIZED -> PENDING_REVIEW, ITEMIZED -> APPROVED
        String initialStatus = "APPROVED";
        if ("AUTHORIZED".equalsIgnoreCase(campaign.getType())) {
            initialStatus = "PENDING_REVIEW";
        }

        BigDecimal totalAmount = BigDecimal.ZERO;
        BigDecimal totalExpectedAmount = BigDecimal.ZERO;

        if (request.getItems() != null) {
            totalAmount = request.getItems().stream()
                    .map(item -> item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            totalExpectedAmount = request.getItems().stream()
                    .map(item -> item.getExpectedPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        }

        BigDecimal variance = totalExpectedAmount.subtract(totalAmount);

        Expenditure expenditure = Expenditure.builder()
                .campaignId(request.getCampaignId())
                .evidenceDueAt(request.getEvidenceDueAt())
                .evidenceStatus(request.getEvidenceStatus() != null ? request.getEvidenceStatus() : "PENDING")
                .totalAmount(totalAmount)
                .totalExpectedAmount(totalExpectedAmount)
                .variance(variance)
                .plan(request.getPlan())
                .status(initialStatus)
                .build();

        final Expenditure savedExpenditure = expenditureRepository.save(expenditure);

        if (request.getItems() != null && !request.getItems().isEmpty()) {
            List<ExpenditureItem> items = request.getItems().stream()
                    .map(itemReq -> ExpenditureItem.builder()
                            .expenditure(savedExpenditure)
                            .category(itemReq.getCategory())
                            .quantity(itemReq.getQuantity())
                            .actualQuantity(0) // Default to 0 initially
                            .price(BigDecimal.ZERO) // Default Actual Price to 0 initially
                            .expectedPrice(itemReq.getExpectedPrice())
                            .note(itemReq.getNote())
                            .build())
                    .collect(Collectors.toList());
            expenditureItemRepository.saveAll(items);
        }

        return savedExpenditure;
    }

    @Override
    public List<Expenditure> getExpendituresByCampaign(Long campaignId) {
        return expenditureRepository.findByCampaignId(campaignId);
    }

    @Override
    public Expenditure getExpenditureById(Long id) {
        return expenditureRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Expenditure not found: " + id));
    }

    @Override
    @Transactional
    public Expenditure updateExpenditureStatus(Long id, String status) {
        Expenditure expenditure = getExpenditureById(id);
        expenditure.setStatus(status);
        return expenditureRepository.save(expenditure);
    }

    @Override
    public List<ExpenditureItem> getExpenditureItems(Long expenditureId) {
        return expenditureItemRepository.findByExpenditureId(expenditureId);
    }

    @Override
    @Transactional
    public Expenditure updateExpenditureActuals(Long id, com.trustfund.model.request.UpdateExpenditureActualsRequest request) {
        Expenditure expenditure = getExpenditureById(id);

        for (com.trustfund.model.request.UpdateExpenditureActualsRequest.UpdateItem updateItem : request.getItems()) {
            ExpenditureItem item = expenditureItemRepository.findById(updateItem.getId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Item not found: " + updateItem.getId()));
            
            if (!item.getExpenditure().getId().equals(id)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Item does not belong to this expenditure");
            }

            if (updateItem.getActualQuantity() != null) {
                item.setActualQuantity(updateItem.getActualQuantity());
            }
            if (updateItem.getPrice() != null) {
                item.setPrice(updateItem.getPrice());
            }
            expenditureItemRepository.save(item);
        }

        // Recalculate totals
        List<ExpenditureItem> allItems = expenditureItemRepository.findByExpenditureId(id);
        
        BigDecimal totalAmount = allItems.stream()
                .map(item -> item.getPrice().multiply(BigDecimal.valueOf(item.getActualQuantity() != null ? item.getActualQuantity() : 0)))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalExpectedAmount = expenditure.getTotalExpectedAmount(); // Keep original expected amount
        BigDecimal variance = totalExpectedAmount.subtract(totalAmount);

        expenditure.setTotalAmount(totalAmount);
        expenditure.setVariance(variance);

        return expenditureRepository.save(expenditure);
    }
}
