package com.hotel.web.controller;

import com.hotel.application.services.PaymentService;
import com.hotel.web.dto.PaymentConfigResponse;
import com.hotel.web.dto.PaymentIntentRequest;
import com.hotel.web.dto.PaymentIntentResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @GetMapping("/config")
    public ResponseEntity<PaymentConfigResponse> getConfig() {
        return ResponseEntity.ok(paymentService.getConfig());
    }

    @PostMapping("/create-intent")
    public ResponseEntity<PaymentIntentResponse> createPaymentIntent(@Valid @RequestBody PaymentIntentRequest request) {
        return ResponseEntity.ok(paymentService.createPaymentIntent(request));
    }
}
