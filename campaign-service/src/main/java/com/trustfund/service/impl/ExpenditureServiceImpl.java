package com.trustfund.service.impl;

import com.trustfund.model.Expenditure;
import com.trustfund.model.request.CreateExpenditureRequest;
import com.trustfund.model.request.UpdateExpenditureRequest;
import com.trustfund.repository.ExpenditureRepository;
import com.trustfund.service.ExpenditureService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ExpenditureServiceImpl implements ExpenditureService {

    private final ExpenditureRepository expenditureRepository;

    @Override
    public List<Expenditure> getAll() {
        return expenditureRepository.findAll();
    }

    @Override
    public Expenditure getById(Long id) {
        return expenditureRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Expenditure not found with id: " + id));
    }

    @Override
    public List<Expenditure> getByCampaignId(Long campaignId) {
        return expenditureRepository.findByCampaignId(campaignId);
    }

    @Override
    @Transactional
    public Expenditure create(CreateExpenditureRequest request) {
        Expenditure expenditure = Expenditure.builder()
                .campaignId(request.getCampaignId())
                .voteCreatedBy(request.getVoteCreatedBy())
                .evidenceDueAt(request.getEvidenceDueAt())
                .totalAmount(request.getTotalAmount())
                .plan(request.getPlan())
                .voteStartAt(request.getVoteStartAt())
                .voteEndAt(request.getVoteEndAt())
                .voteStatus(request.getVoteStatus())
                .status(request.getStatus())
                .evidenceStatus(request.getEvidenceStatus())
                .voteResult(request.getVoteResult())
                .build();
        return expenditureRepository.save(expenditure);
    }

    @Override
    @Transactional
    public Expenditure update(Long id, UpdateExpenditureRequest request) {
        Expenditure expenditure = getById(id);
        
        if (request.getEvidenceDueAt() != null) expenditure.setEvidenceDueAt(request.getEvidenceDueAt());
        if (request.getTotalAmount() != null) expenditure.setTotalAmount(request.getTotalAmount());
        if (request.getPlan() != null) expenditure.setPlan(request.getPlan());
        if (request.getVoteStartAt() != null) expenditure.setVoteStartAt(request.getVoteStartAt());
        if (request.getVoteEndAt() != null) expenditure.setVoteEndAt(request.getVoteEndAt());
        if (request.getVoteStatus() != null) expenditure.setVoteStatus(request.getVoteStatus());
        if (request.getStatus() != null) expenditure.setStatus(request.getStatus());
        if (request.getEvidenceStatus() != null) expenditure.setEvidenceStatus(request.getEvidenceStatus());
        if (request.getVoteResult() != null) expenditure.setVoteResult(request.getVoteResult());
        
        return expenditureRepository.save(expenditure);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        if (!expenditureRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Expenditure not found with id: " + id);
        }
        expenditureRepository.deleteById(id);
    }
}
