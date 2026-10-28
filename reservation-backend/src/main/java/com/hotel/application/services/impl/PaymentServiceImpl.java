package com.hotel.application.services.impl;

import com.hotel.application.services.PaymentService;
import com.hotel.web.dto.PaymentConfigResponse;
import com.hotel.web.dto.PaymentIntentRequest;
import com.hotel.web.dto.PaymentIntentResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class PaymentServiceImpl implements PaymentService {

    @Value("${stripe.publishable-key:pk_test_TYooMQauvdEDq54NiTFae7bg}")
    private String publishableKey;

    @Override
    public PaymentConfigResponse getConfig() {
        return new PaymentConfigResponse(publishableKey);
    }

    @Override
    public PaymentIntentResponse createPaymentIntent(PaymentIntentRequest request) {
        String intentId = "pi_" + UUID.randomUUID().toString().replace("-", "").substring(0, 24);
        String clientSecret = intentId + "_secret_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        String currency = request.currency() != null ? request.currency().toLowerCase() : "usd";

        return new PaymentIntentResponse(
                clientSecret,
                intentId,
                request.amount(),
                currency,
                "requires_payment_method"
        );
    }
}
