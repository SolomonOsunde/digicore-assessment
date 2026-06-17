package com.example.digicoreassessment.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
public class RunSummaryDto {
    private String runId;
    private Instant ranAt;
    private ReconciliationSummary summary;
}
