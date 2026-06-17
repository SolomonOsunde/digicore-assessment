package com.example.digicoreassessment.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.List;

@Getter
@Builder
public class ReconciliationResult {
    private String runId;
    private Instant ranAt;
    private ReconciliationSummary summary;
    private List<DiscrepancyDto> discrepancies;
}
