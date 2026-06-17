package com.example.digicoreassessment.service;

import com.example.digicoreassessment.dto.InternalPaymentRequest;
import com.example.digicoreassessment.model.InternalPayment;
import com.example.digicoreassessment.repository.InternalPaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class InternalPaymentService {

    private final InternalPaymentRepository paymentRepository;

    public InternalPayment register(InternalPaymentRequest req) {
        InternalPayment payment = new InternalPayment();
        payment.setInternalId(req.getInternalId());
        payment.setProviderReference(req.getProviderReference());
        payment.setAmount(req.getAmount());
        payment.setCurrency(req.getCurrency());
        payment.setStatus(req.getStatus());
        payment.setInitiatedAt(req.getInitiatedAt());
        return paymentRepository.save(payment);
    }

    public List<InternalPayment> getAll() {
        return paymentRepository.findAll();
    }
}
