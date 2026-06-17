package com.example.digicoreassessment.service;

import com.example.digicoreassessment.dto.DiscrepancyDto;
import com.example.digicoreassessment.dto.ReconciliationResult;
import com.example.digicoreassessment.dto.ReconciliationSummary;
import com.example.digicoreassessment.dto.RunSummaryDto;
import com.example.digicoreassessment.enums.DiscrepancyCategory;
import com.example.digicoreassessment.enums.PaymentStatus;
import com.example.digicoreassessment.enums.SettledStatus;
import com.example.digicoreassessment.exception.AppException;
import com.example.digicoreassessment.model.InternalPayment;
import com.example.digicoreassessment.model.ProviderSettlement;
import com.example.digicoreassessment.model.Reconciliation;
import com.example.digicoreassessment.model.ReconciliationDiscrepancy;
import com.example.digicoreassessment.repository.InternalPaymentRepository;
import com.example.digicoreassessment.repository.ProviderSettlementRepository;
import com.example.digicoreassessment.repository.ReconciliationDiscrepancyRepository;
import com.example.digicoreassessment.repository.ReconciliationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReconciliationService {

    private final InternalPaymentRepository internalRepo;
    private final ProviderSettlementRepository providerRepo;
    private final ReconciliationRepository runRepo;
    private final ReconciliationDiscrepancyRepository discrepancyRepo;

    @Transactional
    public ReconciliationResult run() {
        List<InternalPayment> internals = internalRepo.findAll();
        List<ProviderSettlement> settlements = providerRepo.findAll();

        if (settlements.isEmpty()) {
            throw new AppException("No provider settlement batch has been uploaded. Upload one via POST /provider/settlements before running reconciliation.", HttpStatus.BAD_REQUEST);
        }

        Map<String, ProviderSettlement> byRef = settlements.stream()
                .collect(Collectors.toMap(ProviderSettlement::getProviderReference, s -> s));

        Set<String> claimedRefs = new HashSet<>();
        List<ReconciliationDiscrepancy> discrepancies = new ArrayList<>();

        int matched = 0, amountMismatches = 0, statusMismatches = 0,
                missingFromProvider = 0, noProviderReference = 0;

        for (InternalPayment payment : internals) {
            if (payment.getProviderReference() == null) {
                noProviderReference++;
                discrepancies.add(noProviderRef(payment));
                continue;
            }

            ProviderSettlement settlement = byRef.get(payment.getProviderReference());
            if (settlement == null) {
                missingFromProvider++;
                discrepancies.add(missingFromProvider(payment));
                continue;
            }

            claimedRefs.add(payment.getProviderReference());

            if (payment.getAmount().compareTo(settlement.getAmount()) != 0) {
                amountMismatches++;
                discrepancies.add(amountMismatch(payment, settlement));
            } else if (!isCompatible(payment.getStatus(), settlement.getSettledStatus())) {
                statusMismatches++;
                discrepancies.add(statusMismatch(payment, settlement));
            } else {
                matched++;
            }
        }

        int unmatchedProviderCount = 0;
        for (ProviderSettlement s : settlements) {
            if (!claimedRefs.contains(s.getProviderReference())) {
                unmatchedProviderCount++;
                discrepancies.add(unmatchedProvider(s));
            }
        }

        long runNumber = runRepo.count() + 1;
        Reconciliation run = new Reconciliation();
        run.setRunId("recon_" + String.format("%03d", runNumber));
        run.setRanAt(Instant.now());
        run.setTotalInternal(internals.size());
        run.setTotalProvider(settlements.size());
        run.setMatched(matched);
        run.setAmountMismatches(amountMismatches);
        run.setStatusMismatches(statusMismatches);
        run.setMissingFromProvider(missingFromProvider);
        run.setNoProviderReference(noProviderReference);
        run.setUnmatchedProviderRecords(unmatchedProviderCount);

        Reconciliation savedRun = runRepo.save(run);

        for (ReconciliationDiscrepancy d : discrepancies) {
            d.setRun(savedRun);
        }
        discrepancyRepo.saveAll(discrepancies);

        return toResult(savedRun, discrepancies);
    }

    public ReconciliationResult getLatest() {
        Reconciliation run = runRepo.findTopByOrderByRanAtDesc()
                .orElseThrow(() -> new AppException("No reconciliation runs found.", HttpStatus.NOT_FOUND));
        List<ReconciliationDiscrepancy> discrepancies = discrepancyRepo.findByRunId(run.getId());
        return toResult(run, discrepancies);
    }

    public List<RunSummaryDto> getHistory() {
        return runRepo.findAll().stream()
                .sorted(Comparator.comparing(Reconciliation::getRanAt).reversed())
                .map(this::toSummaryDto)
                .toList();
    }

    public List<DiscrepancyDto> getDiscrepanciesForRun(String runId, DiscrepancyCategory category) {
        Reconciliation run = runRepo.findByRunId(runId)
                .orElseThrow(() -> new AppException("Reconciliation run not found: " + runId, HttpStatus.NOT_FOUND));

        List<ReconciliationDiscrepancy> results = category != null
                ? discrepancyRepo.findByRunIdAndCategory(run.getId(), category)
                : discrepancyRepo.findByRunId(run.getId());

        return results.stream().map(this::toDiscrepancyDto).toList();
    }

    public boolean isCompatible(PaymentStatus internal, SettledStatus provider) {
        return switch (internal) {
            case SUCCESS -> provider == SettledStatus.SETTLED;
            case FAILED -> provider == SettledStatus.REVERSED;
            case PENDING -> provider == SettledStatus.PENDING;
        };
    }

    private ReconciliationDiscrepancy noProviderRef(InternalPayment p) {
        ReconciliationDiscrepancy d = new ReconciliationDiscrepancy();
        d.setCategory(DiscrepancyCategory.NO_PROVIDER_REFERENCE);
        d.setInternalId(p.getInternalId());
        d.setInternalAmount(p.getAmount());
        d.setCurrency(p.getCurrency());
        d.setNote("Payment has no provider reference — likely failed before reaching the provider");
        return d;
    }

    private ReconciliationDiscrepancy missingFromProvider(InternalPayment p) {
        ReconciliationDiscrepancy d = new ReconciliationDiscrepancy();
        d.setCategory(DiscrepancyCategory.MISSING_FROM_PROVIDER);
        d.setInternalId(p.getInternalId());
        d.setProviderReference(p.getProviderReference());
        d.setInternalAmount(p.getAmount());
        d.setCurrency(p.getCurrency());
        d.setNote("No matching record found in provider batch for reference " + p.getProviderReference());
        return d;
    }

    private ReconciliationDiscrepancy amountMismatch(InternalPayment p, ProviderSettlement s) {
        BigDecimal diff = p.getAmount().subtract(s.getAmount()).abs().setScale(2, RoundingMode.HALF_UP);
        ReconciliationDiscrepancy d = new ReconciliationDiscrepancy();
        d.setCategory(DiscrepancyCategory.AMOUNT_MISMATCH);
        d.setInternalId(p.getInternalId());
        d.setProviderReference(p.getProviderReference());
        d.setInternalAmount(p.getAmount());
        d.setProviderAmount(s.getAmount());
        d.setCurrency(p.getCurrency());
        d.setNote("Amount differs by " + diff.toPlainString() + " " + p.getCurrency());
        return d;
    }

    private ReconciliationDiscrepancy statusMismatch(InternalPayment p, ProviderSettlement s) {
        ReconciliationDiscrepancy d = new ReconciliationDiscrepancy();
        d.setCategory(DiscrepancyCategory.STATUS_MISMATCH);
        d.setInternalId(p.getInternalId());
        d.setProviderReference(p.getProviderReference());
        d.setInternalAmount(p.getAmount());
        d.setProviderAmount(s.getAmount());
        d.setCurrency(p.getCurrency());
        d.setNote("Status mismatch: internal " + p.getStatus() + " is not compatible with provider " + s.getSettledStatus());
        return d;
    }

    private ReconciliationDiscrepancy unmatchedProvider(ProviderSettlement s) {
        ReconciliationDiscrepancy d = new ReconciliationDiscrepancy();
        d.setCategory(DiscrepancyCategory.UNMATCHED_PROVIDER_RECORD);
        d.setProviderReference(s.getProviderReference());
        d.setProviderAmount(s.getAmount());
        d.setCurrency(s.getCurrency());
        d.setNote("No internal payment record found for provider reference " + s.getProviderReference());
        return d;
    }

    private ReconciliationResult toResult(Reconciliation run, List<ReconciliationDiscrepancy> discrepancies) {
        return ReconciliationResult.builder()
                .runId(run.getRunId())
                .ranAt(run.getRanAt())
                .summary(toSummary(run))
                .discrepancies(discrepancies.stream().map(this::toDiscrepancyDto).toList())
                .build();
    }

    private RunSummaryDto toSummaryDto(Reconciliation run) {
        return RunSummaryDto.builder()
                .runId(run.getRunId())
                .ranAt(run.getRanAt())
                .summary(toSummary(run))
                .build();
    }

    private ReconciliationSummary toSummary(Reconciliation run) {
        return ReconciliationSummary.builder()
                .totalInternal(run.getTotalInternal())
                .totalProvider(run.getTotalProvider())
                .matched(run.getMatched())
                .amountMismatches(run.getAmountMismatches())
                .statusMismatches(run.getStatusMismatches())
                .missingFromProvider(run.getMissingFromProvider())
                .noProviderReference(run.getNoProviderReference())
                .unmatchedProviderRecords(run.getUnmatchedProviderRecords())
                .build();
    }

    private DiscrepancyDto toDiscrepancyDto(ReconciliationDiscrepancy d) {
        return DiscrepancyDto.builder()
                .category(d.getCategory())
                .internalId(d.getInternalId())
                .providerReference(d.getProviderReference())
                .internalAmount(d.getInternalAmount())
                .providerAmount(d.getProviderAmount())
                .currency(d.getCurrency())
                .note(d.getNote())
                .build();
    }
}
