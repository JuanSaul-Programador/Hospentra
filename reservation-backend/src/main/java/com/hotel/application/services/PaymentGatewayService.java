package com.hotel.application.services;

import com.hotel.web.dto.PaymentIntentRequest;
import com.hotel.web.dto.PaymentIntentResponse;

import java.math.BigDecimal;

public interface PaymentGatewayService {

    PaymentIntentResponse createPaymentIntent(PaymentIntentRequest request, String ownerUsername);

    boolean verifyAndConsumePayment(String paymentIntentId, BigDecimal expectedAmount, String callerUsername, boolean callerIsStaff);
}
