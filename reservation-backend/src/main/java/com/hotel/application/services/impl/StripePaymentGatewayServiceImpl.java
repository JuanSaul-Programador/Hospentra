package com.hotel.application.services.impl;

import com.hotel.application.services.PaymentGatewayService;
import com.hotel.domain.repository.ReservationRepository;
import com.hotel.web.dto.PaymentIntentRequest;
import com.hotel.web.dto.PaymentIntentResponse;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.net.RequestOptions;
import com.stripe.param.PaymentIntentCreateParams;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
@RequiredArgsConstructor
@Slf4j
public class StripePaymentGatewayServiceImpl implements PaymentGatewayService {

    private record SimulatedIntent(
            String id,
            BigDecimal amount,
            String currency,
            String ownerUsername,
            Instant createdAt,
            AtomicBoolean consumed
    ) {}

    private final ReservationRepository reservationRepository;

    private final ConcurrentHashMap<String, SimulatedIntent> simulatedIntents = new ConcurrentHashMap<>();
    private final Set<String> consumedLiveIntents = ConcurrentHashMap.newKeySet();

    @Value("${stripe.secret-key:sk_test_placeholder}")
    private String stripeSecretKey;

    @Value("${app.payment.simulation-enabled:false}")
    private boolean simulationEnabled;

    @Override
    public PaymentIntentResponse createPaymentIntent(PaymentIntentRequest request, String ownerUsername) {
        if (request == null || request.amount() == null || request.amount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("El monto del pago debe ser mayor a cero.");
        }

        BigDecimal normalizedAmount = request.amount().setScale(2, RoundingMode.HALF_UP);
        String currency = request.currency() != null && !request.currency().isBlank()
                ? request.currency().trim().toLowerCase()
                : "usd";

        if (isLiveStripeConfigured()) {
            try {
                long amountCents = normalizedAmount.movePointRight(2).longValueExact();
                PaymentIntentCreateParams.Builder paramsBuilder = PaymentIntentCreateParams.builder()
                        .setAmount(amountCents)
                        .setCurrency(currency);
                if (request.description() != null && !request.description().isBlank()) {
                    paramsBuilder.setDescription(request.description());
                }
                if (request.guestEmail() != null && !request.guestEmail().isBlank()) {
                    paramsBuilder.setReceiptEmail(request.guestEmail());
                }
                RequestOptions options = RequestOptions.builder()
                        .setApiKey(stripeSecretKey.trim())
                        .build();
                PaymentIntent intent = PaymentIntent.create(paramsBuilder.build(), options);
                return new PaymentIntentResponse(
                        intent.getClientSecret(),
                        intent.getId(),
                        normalizedAmount,
                        intent.getCurrency(),
                        intent.getStatus()
                );
            } catch (StripeException ex) {
                log.error("Stripe PaymentIntent creation failed", ex);
                throw new IllegalStateException("No se pudo iniciar la transacción en la pasarela de pago.");
            }
        }

        if (!simulationEnabled) {
            throw new IllegalStateException("La pasarela de pago no se encuentra configurada.");
        }

        String intentId = "pi_sim_" + UUID.randomUUID().toString().replace("-", "").substring(0, 24);
        String clientSecret = intentId + "_secret_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);

        simulatedIntents.put(intentId, new SimulatedIntent(
                intentId,
                normalizedAmount,
                currency,
                ownerUsername,
                Instant.now(),
                new AtomicBoolean(false)
        ));

        return new PaymentIntentResponse(
                clientSecret,
                intentId,
                normalizedAmount,
                currency,
                "requires_payment_method"
        );
    }

    @Override
    public boolean verifyAndConsumePayment(String paymentIntentId, BigDecimal expectedAmount, String callerUsername, boolean callerIsStaff) {
        if (paymentIntentId == null || paymentIntentId.isBlank()
                || expectedAmount == null || expectedAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return false;
        }

        String cleanIntentId = paymentIntentId.trim();
        BigDecimal normalizedExpected = expectedAmount.setScale(2, RoundingMode.HALF_UP);

        if (reservationRepository.existsByPaymentIntentId(cleanIntentId)) {
            return false;
        }

        if (isLiveStripeConfigured()) {
            if (consumedLiveIntents.contains(cleanIntentId)) {
                return false;
            }
            try {
                RequestOptions options = RequestOptions.builder()
                        .setApiKey(stripeSecretKey.trim())
                        .build();
                PaymentIntent intent = PaymentIntent.retrieve(cleanIntentId, options);
                if (intent == null || !"succeeded".equalsIgnoreCase(intent.getStatus())) {
                    return false;
                }
                long expectedCents = normalizedExpected.movePointRight(2).longValueExact();
                Long receivedCents = intent.getAmountReceived() != null && intent.getAmountReceived() > 0
                        ? intent.getAmountReceived()
                        : intent.getAmount();
                if (receivedCents == null || receivedCents < expectedCents) {
                    return false;
                }
                return consumedLiveIntents.add(cleanIntentId);
            } catch (StripeException ex) {
                log.warn("Stripe PaymentIntent verification failed for id {}", cleanIntentId, ex);
                return false;
            }
        }

        if (!simulationEnabled) {
            return false;
        }

        SimulatedIntent simIntent = simulatedIntents.get(cleanIntentId);
        if (simIntent == null) {
            return false;
        }
        if (simIntent.amount().compareTo(normalizedExpected) != 0) {
            return false;
        }
        if (simIntent.ownerUsername() != null && !callerIsStaff) {
            if (callerUsername == null || !simIntent.ownerUsername().equals(callerUsername)) {
                return false;
            }
        }
        return simIntent.consumed().compareAndSet(false, true);
    }

    private boolean isLiveStripeConfigured() {
        return stripeSecretKey != null
                && !stripeSecretKey.isBlank()
                && !stripeSecretKey.contains("placeholder")
                && stripeSecretKey.trim().startsWith("sk_");
    }
}
