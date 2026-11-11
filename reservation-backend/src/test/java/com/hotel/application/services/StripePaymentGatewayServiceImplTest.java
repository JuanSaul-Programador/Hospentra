package com.hotel.application.services;

import com.hotel.application.services.impl.StripePaymentGatewayServiceImpl;
import com.hotel.domain.repository.ReservationRepository;
import com.hotel.web.dto.PaymentIntentRequest;
import com.hotel.web.dto.PaymentIntentResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StripePaymentGatewayServiceImplTest {

    @Mock
    private ReservationRepository reservationRepository;

    private StripePaymentGatewayServiceImpl gatewayService;

    @BeforeEach
    void setUp() {
        gatewayService = new StripePaymentGatewayServiceImpl(reservationRepository);
        ReflectionTestUtils.setField(gatewayService, "stripeSecretKey", "sk_test_placeholder");
        ReflectionTestUtils.setField(gatewayService, "simulationEnabled", true);
    }

    @Test
    void verifyAndConsumePayment_WithValidIssuedIntentAndMatchingAmount_ReturnsTrueOnce() {
        PaymentIntentRequest req = new PaymentIntentRequest(new BigDecimal("600.00"), "usd", "Suite 101", "ana@hotel.com");
        PaymentIntentResponse intent = gatewayService.createPaymentIntent(req, "ana");

        when(reservationRepository.existsByPaymentIntentId(intent.paymentIntentId())).thenReturn(false);

        boolean firstVerification = gatewayService.verifyAndConsumePayment(
                intent.paymentIntentId(), new BigDecimal("600.00"), "ana", false
        );
        boolean replayVerification = gatewayService.verifyAndConsumePayment(
                intent.paymentIntentId(), new BigDecimal("600.00"), "ana", false
        );

        assertThat(firstVerification).isTrue();
        assertThat(replayVerification).isFalse();
    }

    @Test
    void verifyAndConsumePayment_WithNullOrFakeIntent_ReturnsFalse() {
        assertThat(gatewayService.verifyAndConsumePayment(null, new BigDecimal("600.00"), "ana", false)).isFalse();
        assertThat(gatewayService.verifyAndConsumePayment("", new BigDecimal("600.00"), "ana", false)).isFalse();
        assertThat(gatewayService.verifyAndConsumePayment("pi_fake_12345", new BigDecimal("600.00"), "ana", false)).isFalse();
        assertThat(gatewayService.verifyAndConsumePayment("pm_vault_99999", new BigDecimal("600.00"), "ana", false)).isFalse();
    }

    @Test
    void verifyAndConsumePayment_WithMismatchedAmount_ReturnsFalse() {
        PaymentIntentRequest req = new PaymentIntentRequest(new BigDecimal("10.00"), "usd", "Suite 101", "ana@hotel.com");
        PaymentIntentResponse intent = gatewayService.createPaymentIntent(req, "ana");

        when(reservationRepository.existsByPaymentIntentId(intent.paymentIntentId())).thenReturn(false);

        boolean result = gatewayService.verifyAndConsumePayment(
                intent.paymentIntentId(), new BigDecimal("600.00"), "ana", false
        );

        assertThat(result).isFalse();
    }

    @Test
    void verifyAndConsumePayment_WithDifferentNonStaffOwner_ReturnsFalse() {
        PaymentIntentRequest req = new PaymentIntentRequest(new BigDecimal("300.00"), "usd", "Suite 101", "ana@hotel.com");
        PaymentIntentResponse intent = gatewayService.createPaymentIntent(req, "ana");

        when(reservationRepository.existsByPaymentIntentId(intent.paymentIntentId())).thenReturn(false);

        boolean attackerResult = gatewayService.verifyAndConsumePayment(
                intent.paymentIntentId(), new BigDecimal("300.00"), "intruso", false
        );
        boolean staffResult = gatewayService.verifyAndConsumePayment(
                intent.paymentIntentId(), new BigDecimal("300.00"), "recepcion", true
        );

        assertThat(attackerResult).isFalse();
        assertThat(staffResult).isTrue();
    }

    @Test
    void verifyAndConsumePayment_WhenAlreadyStoredOnReservation_ReturnsFalse() {
        PaymentIntentRequest req = new PaymentIntentRequest(new BigDecimal("300.00"), "usd", "Suite 101", "ana@hotel.com");
        PaymentIntentResponse intent = gatewayService.createPaymentIntent(req, "ana");

        when(reservationRepository.existsByPaymentIntentId(intent.paymentIntentId())).thenReturn(true);

        boolean result = gatewayService.verifyAndConsumePayment(
                intent.paymentIntentId(), new BigDecimal("300.00"), "ana", false
        );

        assertThat(result).isFalse();
    }

    @Test
    void createAndVerify_WhenSimulationDisabledAndNoLiveKey_FailsClosed() {
        ReflectionTestUtils.setField(gatewayService, "simulationEnabled", false);

        assertThatThrownBy(() -> gatewayService.createPaymentIntent(
                new PaymentIntentRequest(new BigDecimal("100.00"), "usd", "Test", null), "ana"))
                .isInstanceOf(IllegalStateException.class);

        assertThat(gatewayService.verifyAndConsumePayment("pi_any", new BigDecimal("100.00"), "ana", false)).isFalse();
    }
}
