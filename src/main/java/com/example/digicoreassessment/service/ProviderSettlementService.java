package com.example.digicoreassessment.service;

import com.example.digicoreassessment.dto.ProviderSettlementRequest;
import com.example.digicoreassessment.model.ProviderSettlement;
import com.example.digicoreassessment.repository.ProviderSettlementRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProviderSettlementService {

    private final ProviderSettlementRepository settlementRepository;

    @Transactional
    public List<ProviderSettlement> replaceBatch(List<ProviderSettlementRequest> requests) {
        settlementRepository.deleteAll();

        List<ProviderSettlement> settlements = requests.stream().map(req -> {
            ProviderSettlement s = new ProviderSettlement();
            s.setProviderReference(req.getProviderReference());
            s.setAmount(req.getAmount());
            s.setCurrency(req.getCurrency());
            s.setSettledStatus(req.getSettledStatus());
            s.setSettledAt(req.getSettledAt());
            return s;
        }).toList();

        return settlementRepository.saveAll(settlements);
    }

    public List<ProviderSettlement> getAll() {
        return settlementRepository.findAll();
    }
}
