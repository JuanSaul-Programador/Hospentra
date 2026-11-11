package com.hotel.application.services.impl;

import com.hotel.application.services.PaymentGatewayService;
import com.hotel.application.services.PaymentService;
import com.hotel.web.dto.PaymentConfigResponse;
import com.hotel.web.dto.PaymentIntentRequest;
import com.hotel.web.dto.PaymentIntentResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final PaymentGatewayService paymentGatewayService;

    @Value("${stripe.publishable-key:pk_test_TYooMQauvdEDq54NiTFae7bg}")
    private String publishableKey;

    @Override
    public PaymentConfigResponse getConfig() {
        return new PaymentConfigResponse(publishableKey);
    }

    @Override
    public PaymentIntentResponse createPaymentIntent(PaymentIntentRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication != null && authentication.isAuthenticated()
                && !"anonymousUser".equals(authentication.getPrincipal())
                ? authentication.getName()
                : null;
        return paymentGatewayService.createPaymentIntent(request, username);
    }
}
