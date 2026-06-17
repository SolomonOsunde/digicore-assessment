package com.example.digicoreassessment.controller;

import com.example.digicoreassessment.dto.ProviderSettlementRequest;
import com.example.digicoreassessment.model.ProviderSettlement;
import com.example.digicoreassessment.service.ProviderSettlementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/provider/settlements")
@RequiredArgsConstructor
@Tag(name = "Provider Settlements", description = "Upload and list the external provider's settlement records")
public class ProviderSettlementController {

    private final ProviderSettlementService settlementService;

    @PostMapping
    @Operation(
            summary = "Upload a batch of provider settlement records",
            description = "Replaces the entire existing provider batch. Providers often resend corrected files, so each upload is treated as the authoritative snapshot."
    )
    public List<ProviderSettlement> upload(@Valid @RequestBody List<@Valid ProviderSettlementRequest> requests) {
        return settlementService.replaceBatch(requests);
    }

    @GetMapping
    @Operation(summary = "List all currently loaded provider settlement records")
    public List<ProviderSettlement> list() {
        return settlementService.getAll();
    }
}
