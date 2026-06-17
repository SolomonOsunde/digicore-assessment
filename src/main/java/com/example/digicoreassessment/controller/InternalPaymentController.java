package com.example.digicoreassessment.controller;

import com.example.digicoreassessment.dto.InternalPaymentRequest;
import com.example.digicoreassessment.model.InternalPayment;
import com.example.digicoreassessment.service.InternalPaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/internal/payments")
@RequiredArgsConstructor
@Tag(name = "Internal Payments", description = "Register and list your system's payment records")
public class InternalPaymentController {

    private final InternalPaymentService paymentService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Register an internal payment record")
    public InternalPayment create(@Valid @RequestBody InternalPaymentRequest request) {
        return paymentService.register(request);
    }

    @GetMapping
    @Operation(summary = "List all internal payment records")
    public List<InternalPayment> list() {
        return paymentService.getAll();
    }
}
