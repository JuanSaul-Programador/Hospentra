package com.hotel.application.services;

import com.hotel.web.dto.PaymentConfigResponse;
import com.hotel.web.dto.PaymentIntentRequest;
import com.hotel.web.dto.PaymentIntentResponse;

public interface PaymentService {

    PaymentConfigResponse getConfig();

    PaymentIntentResponse createPaymentIntent(PaymentIntentRequest request);
}
