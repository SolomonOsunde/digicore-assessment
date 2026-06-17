package com.example.digicoreassessment.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ReconciliationSummary {
    private int totalInternal;
    private int totalProvider;
    private int matched;
    private int amountMismatches;
    private int statusMismatches;
    private int missingFromProvider;
    private int noProviderReference;
    private int unmatchedProviderRecords;
}
