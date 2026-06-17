package com.example.digicoreassessment;

import com.example.digicoreassessment.enums.DiscrepancyCategory;
import com.example.digicoreassessment.enums.PaymentStatus;
import com.example.digicoreassessment.enums.SettledStatus;
import com.example.digicoreassessment.model.InternalPayment;
import com.example.digicoreassessment.model.ProviderSettlement;
import com.example.digicoreassessment.repository.InternalPaymentRepository;
import com.example.digicoreassessment.repository.ProviderSettlementRepository;
import com.example.digicoreassessment.repository.ReconciliationDiscrepancyRepository;
import com.example.digicoreassessment.repository.ReconciliationRepository;
import com.example.digicoreassessment.service.ReconciliationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
class ReconciliationServiceTest {

    @Autowired ReconciliationService reconciliationService;
    @Autowired InternalPaymentRepository internalRepo;
    @Autowired ProviderSettlementRepository providerRepo;
    @Autowired ReconciliationRepository runRepo;
    @Autowired ReconciliationDiscrepancyRepository discrepancyRepo;

    @BeforeEach
    void setUp() {
        discrepancyRepo.deleteAll();
        runRepo.deleteAll();
        internalRepo.deleteAll();
        providerRepo.deleteAll();
    }

    @Test
    void successMapsToSettled_isMatched() {
        saveInternal("pay_1", "ref_1", new BigDecimal("100.00"), "NGN", PaymentStatus.SUCCESS);
        saveProvider("ref_1", new BigDecimal("100.00"), "NGN", SettledStatus.SETTLED);

        var result = reconciliationService.run();

        assertThat(result.getSummary().getMatched()).isEqualTo(1);
        assertThat(result.getDiscrepancies()).isEmpty();
    }

    @Test
    void failedMapsToReversed_isMatched() {
        saveInternal("pay_2", "ref_2", new BigDecimal("200.00"), "NGN", PaymentStatus.FAILED);
        saveProvider("ref_2", new BigDecimal("200.00"), "NGN", SettledStatus.REVERSED);

        var result = reconciliationService.run();

        assertThat(result.getSummary().getMatched()).isEqualTo(1);
        assertThat(result.getDiscrepancies()).isEmpty();
    }

    @Test
    void pendingMapsToSettled_isStatusMismatch() {
        saveInternal("pay_3", "ref_3", new BigDecimal("300.00"), "NGN", PaymentStatus.PENDING);
        saveProvider("ref_3", new BigDecimal("300.00"), "NGN", SettledStatus.SETTLED);

        var result = reconciliationService.run();

        assertThat(result.getSummary().getStatusMismatches()).isEqualTo(1);
        assertThat(result.getDiscrepancies()).hasSize(1);
        assertThat(result.getDiscrepancies().get(0).getCategory()).isEqualTo(DiscrepancyCategory.STATUS_MISMATCH);
    }

    @Test
    void successMapsToReversed_isStatusMismatch() {
        saveInternal("pay_4", "ref_4", new BigDecimal("400.00"), "NGN", PaymentStatus.SUCCESS);
        saveProvider("ref_4", new BigDecimal("400.00"), "NGN", SettledStatus.REVERSED);

        var result = reconciliationService.run();

        assertThat(result.getSummary().getStatusMismatches()).isEqualTo(1);
    }

    @Test
    void amountDiffers_isAmountMismatch() {
        saveInternal("pay_5", "ref_5", new BigDecimal("500.00"), "NGN", PaymentStatus.SUCCESS);
        saveProvider("ref_5", new BigDecimal("450.00"), "NGN", SettledStatus.SETTLED);

        var result = reconciliationService.run();

        assertThat(result.getSummary().getAmountMismatches()).isEqualTo(1);
        var d = result.getDiscrepancies().get(0);
        assertThat(d.getCategory()).isEqualTo(DiscrepancyCategory.AMOUNT_MISMATCH);
        assertThat(d.getNote()).contains("50.00 NGN");
    }

    @Test
    void noProviderReference_flaggedCorrectly() {
        saveInternal("pay_6", null, new BigDecimal("600.00"), "NGN", PaymentStatus.FAILED);
        saveProvider("ref_6", new BigDecimal("600.00"), "NGN", SettledStatus.REVERSED);

        var result = reconciliationService.run();

        assertThat(result.getSummary().getNoProviderReference()).isEqualTo(1);
        assertThat(result.getSummary().getUnmatchedProviderRecords()).isEqualTo(1);
        assertThat(result.getDiscrepancies()).hasSize(2);
    }

    @Test
    void internalRefNotInProvider_isMissingFromProvider() {
        saveInternal("pay_7", "ref_ghost", new BigDecimal("700.00"), "NGN", PaymentStatus.SUCCESS);
        saveProvider("ref_other", new BigDecimal("700.00"), "NGN", SettledStatus.SETTLED);

        var result = reconciliationService.run();

        assertThat(result.getSummary().getMissingFromProvider()).isEqualTo(1);
        assertThat(result.getSummary().getUnmatchedProviderRecords()).isEqualTo(1);
    }

    @Test
    void providerRecordWithNoInternalMatch_isUnmatched() {
        saveInternal("pay_8", "ref_8", new BigDecimal("800.00"), "NGN", PaymentStatus.SUCCESS);
        saveProvider("ref_8", new BigDecimal("800.00"), "NGN", SettledStatus.SETTLED);
        saveProvider("ref_extra", new BigDecimal("999.00"), "NGN", SettledStatus.SETTLED);

        var result = reconciliationService.run();

        assertThat(result.getSummary().getMatched()).isEqualTo(1);
        assertThat(result.getSummary().getUnmatchedProviderRecords()).isEqualTo(1);
    }

    @Test
    void summaryCountsAddUp() {
        saveInternal("pay_a", "ref_a", new BigDecimal("100.00"), "NGN", PaymentStatus.SUCCESS);
        saveInternal("pay_b", "ref_b", new BigDecimal("200.00"), "NGN", PaymentStatus.FAILED);
        saveInternal("pay_c", null, new BigDecimal("300.00"), "NGN", PaymentStatus.PENDING);

        saveProvider("ref_a", new BigDecimal("999.00"), "NGN", SettledStatus.SETTLED); // amount mismatch
        saveProvider("ref_b", new BigDecimal("200.00"), "NGN", SettledStatus.SETTLED);  // status mismatch (FAILED vs SETTLED)
        saveProvider("ref_extra", new BigDecimal("50.00"), "NGN", SettledStatus.PENDING); // unmatched

        var result = reconciliationService.run();
        var s = result.getSummary();

        int internalSideTotal = s.getMatched() + s.getAmountMismatches() + s.getStatusMismatches()
                + s.getMissingFromProvider() + s.getNoProviderReference();
        assertThat(internalSideTotal).isEqualTo(s.getTotalInternal());
    }

    @Test
    void isCompatible_allCombinations() {
        assertThat(reconciliationService.isCompatible(PaymentStatus.SUCCESS, SettledStatus.SETTLED)).isTrue();
        assertThat(reconciliationService.isCompatible(PaymentStatus.FAILED, SettledStatus.REVERSED)).isTrue();
        assertThat(reconciliationService.isCompatible(PaymentStatus.PENDING, SettledStatus.PENDING)).isTrue();

        assertThat(reconciliationService.isCompatible(PaymentStatus.SUCCESS, SettledStatus.REVERSED)).isFalse();
        assertThat(reconciliationService.isCompatible(PaymentStatus.SUCCESS, SettledStatus.PENDING)).isFalse();
        assertThat(reconciliationService.isCompatible(PaymentStatus.FAILED, SettledStatus.SETTLED)).isFalse();
        assertThat(reconciliationService.isCompatible(PaymentStatus.FAILED, SettledStatus.PENDING)).isFalse();
        assertThat(reconciliationService.isCompatible(PaymentStatus.PENDING, SettledStatus.SETTLED)).isFalse();
        assertThat(reconciliationService.isCompatible(PaymentStatus.PENDING, SettledStatus.REVERSED)).isFalse();
    }

    @Test
    void runWithNoProviderBatch_throws400() {
        saveInternal("pay_x", "ref_x", new BigDecimal("100.00"), "NGN", PaymentStatus.SUCCESS);

        var ex = assertThrows(com.example.digicoreassessment.exception.AppException.class,
                () -> reconciliationService.run());

        assertThat(ex.getStatus()).isEqualTo(org.springframework.http.HttpStatus.BAD_REQUEST);
    }

    private void saveInternal(String internalId, String providerRef, BigDecimal amount, String currency, PaymentStatus status) {
        InternalPayment p = new InternalPayment();
        p.setInternalId(internalId);
        p.setProviderReference(providerRef);
        p.setAmount(amount);
        p.setCurrency(currency);
        p.setStatus(status);
        p.setInitiatedAt(Instant.now());
        internalRepo.save(p);
    }

    private void saveProvider(String providerRef, BigDecimal amount, String currency, SettledStatus status) {
        ProviderSettlement s = new ProviderSettlement();
        s.setProviderReference(providerRef);
        s.setAmount(amount);
        s.setCurrency(currency);
        s.setSettledStatus(status);
        s.setSettledAt(Instant.now());
        providerRepo.save(s);
    }
}
