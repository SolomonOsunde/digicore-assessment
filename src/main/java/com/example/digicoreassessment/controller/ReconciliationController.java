package com.example.digicoreassessment.controller;

import com.example.digicoreassessment.dto.DiscrepancyDto;
import com.example.digicoreassessment.dto.ReconciliationResult;
import com.example.digicoreassessment.dto.RunSummaryDto;
import com.example.digicoreassessment.enums.DiscrepancyCategory;
import com.example.digicoreassessment.service.ReconciliationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/reconciliation")
@RequiredArgsConstructor
@Tag(name = "Reconciliation", description = "Run reconciliation and view results")
public class ReconciliationController {

    private final ReconciliationService reconciliationService;

    @PostMapping("/run")
    @Operation(
            summary = "Run reconciliation",
            description = """
                    Compares all internal payment records against the current provider settlement batch.

                    Each internal record is categorised as one of:
                    - **MATCHED** — same reference, amount, currency, and compatible status (excluded from discrepancies list)
                    - **AMOUNT_MISMATCH** — same reference but amounts differ
                    - **STATUS_MISMATCH** — same reference and amount but statuses are incompatible
                    - **MISSING_FROM_PROVIDER** — has a providerReference but no matching provider record exists
                    - **NO_PROVIDER_REFERENCE** — payment never reached the provider

                    Provider records with no corresponding internal record are flagged as **UNMATCHED_PROVIDER_RECORD**.

                    Status compatibility: SUCCESS↔SETTLED, FAILED↔REVERSED, PENDING↔PENDING.
                    """
    )
    public ReconciliationResult run() {
        return reconciliationService.run();
    }

    @GetMapping("/latest")
    @Operation(summary = "Get the most recent reconciliation result")
    public ReconciliationResult latest() {
        return reconciliationService.getLatest();
    }

    @GetMapping("/history")
    @Operation(summary = "List all past reconciliation runs (summary only, no discrepancies)")
    public List<RunSummaryDto> history() {
        return reconciliationService.getHistory();
    }

    @GetMapping("/{runId}/discrepancies")
    @Operation(summary = "Get discrepancies for a specific run, optionally filtered by category")
    public List<DiscrepancyDto> discrepancies(
            @PathVariable String runId,
            @Parameter(description = "Filter by discrepancy category") @RequestParam(required = false) DiscrepancyCategory category) {
        return reconciliationService.getDiscrepanciesForRun(runId, category);
    }
}
