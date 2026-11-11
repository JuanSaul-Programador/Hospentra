package com.hotel.application.services;

import com.hotel.application.dto.notification.NotificationEvent;
import com.hotel.application.services.impl.SseNotificationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyEmitter;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class SseNotificationServiceImplTest {

    private SseNotificationServiceImpl sseNotificationService;

    @BeforeEach
    void setUp() {
        sseNotificationService = new SseNotificationServiceImpl();
    }

    @Test
    void sendToRole_DeliversOnlyToMatchingRoleWithoutLeakingToCustomersOrDuplicatingAdmin() {
        SseEmitter adminEmitter = sseNotificationService.createEmitter("admin", "ROLE_ADMIN");
        SseEmitter recepEmitter = sseNotificationService.createEmitter("recep", "ROLE_RECEPTIONIST");
        SseEmitter kitchenEmitter = sseNotificationService.createEmitter("chef", "ROLE_KITCHEN");
        SseEmitter customerEmitter = sseNotificationService.createEmitter("ana", "ROLE_CUSTOMER");

        NotificationEvent event = NotificationEvent.of(
                "PRE_CHECK_IN_COMPLETED",
                "FRONT_DESK",
                10L,
                "Pre-Check-In",
                "Documento PASSPORT P123456",
                null
        );

        sseNotificationService.sendToRole("ROLE_ADMIN", event);
        sseNotificationService.sendToRole("ROLE_RECEPTIONIST", event);

        assertThat(extractEventTypes(adminEmitter)).containsExactly("HANDSHAKE", "PRE_CHECK_IN_COMPLETED");
        assertThat(extractEventTypes(recepEmitter)).containsExactly("HANDSHAKE", "PRE_CHECK_IN_COMPLETED");
        assertThat(extractEventTypes(kitchenEmitter)).containsExactly("HANDSHAKE");
        assertThat(extractEventTypes(customerEmitter)).containsExactly("HANDSHAKE");
    }

    @Test
    void sendToUser_DeliversOnlyToTargetUsername() {
        SseEmitter anaEmitter = sseNotificationService.createEmitter("ana", "ROLE_CUSTOMER");
        SseEmitter bobEmitter = sseNotificationService.createEmitter("bob", "ROLE_CUSTOMER");
        SseEmitter adminEmitter = sseNotificationService.createEmitter("admin", "ROLE_ADMIN");

        NotificationEvent event = NotificationEvent.of(
                "RESERVATION_STATUS_CHANGED",
                "FRONT_DESK",
                55L,
                "Check-In",
                "Su reserva fue actualizada",
                null
        );

        sseNotificationService.sendToUser("ana", event);

        assertThat(extractEventTypes(anaEmitter)).containsExactly("HANDSHAKE", "RESERVATION_STATUS_CHANGED");
        assertThat(extractEventTypes(bobEmitter)).containsExactly("HANDSHAKE");
        assertThat(extractEventTypes(adminEmitter)).containsExactly("HANDSHAKE");
    }

    @SuppressWarnings("unchecked")
    private List<String> extractEventTypes(SseEmitter emitter) {
        Set<ResponseBodyEmitter.DataWithMediaType> attempts =
                (Set<ResponseBodyEmitter.DataWithMediaType>) ReflectionTestUtils.getField(emitter, "earlySendAttempts");
        if (attempts == null) {
            return List.of();
        }
        return attempts.stream()
                .map(ResponseBodyEmitter.DataWithMediaType::getData)
                .filter(NotificationEvent.class::isInstance)
                .map(NotificationEvent.class::cast)
                .map(NotificationEvent::eventType)
                .toList();
    }
}
