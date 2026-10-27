package com.hotel.application.services.impl;

import com.hotel.application.dto.notification.NotificationEvent;
import com.hotel.application.services.SseNotificationService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class SseNotificationServiceImpl implements SseNotificationService {

    private record ClientEmitter(String id, String username, String role, SseEmitter emitter) {}

    private final CopyOnWriteArrayList<ClientEmitter> clients = new CopyOnWriteArrayList<>();

    private static final Long EMITTER_TIMEOUT = 30L * 60L * 1000L;

    @Override
    public SseEmitter createEmitter(String username, String role) {
        SseEmitter emitter = new SseEmitter(EMITTER_TIMEOUT);
        String clientId = UUID.randomUUID().toString();
        ClientEmitter client = new ClientEmitter(clientId, username, role, emitter);

        emitter.onCompletion(() -> clients.remove(client));
        emitter.onTimeout(() -> {
            emitter.complete();
            clients.remove(client);
        });
        emitter.onError(e -> {
            emitter.complete();
            clients.remove(client);
        });

        clients.add(client);

        try {
            emitter.send(SseEmitter.event()
                    .name("HANDSHAKE")
                    .data(NotificationEvent.of("HANDSHAKE", "SYSTEM", null, "Conexión Establecida", "Canal de eventos en tiempo real activo", null)));
        } catch (IOException e) {
            emitter.complete();
            clients.remove(client);
        }

        return emitter;
    }

    @Override
    public void broadcast(NotificationEvent event) {
        for (ClientEmitter client : clients) {
            sendEventToClient(client, event);
        }
    }

    @Override
    public void sendToRole(String role, NotificationEvent event) {
        for (ClientEmitter client : clients) {
            if ("ROLE_ADMIN".equalsIgnoreCase(client.role()) || role.equalsIgnoreCase(client.role())) {
                sendEventToClient(client, event);
            }
        }
    }

    @Override
    public void sendToUser(String username, NotificationEvent event) {
        for (ClientEmitter client : clients) {
            if (client.username().equalsIgnoreCase(username)) {
                sendEventToClient(client, event);
            }
        }
    }

    private void sendEventToClient(ClientEmitter client, NotificationEvent event) {
        try {
            client.emitter().send(SseEmitter.event()
                    .name(event.eventType())
                    .data(event));
        } catch (Exception e) {
            client.emitter().complete();
            clients.remove(client);
        }
    }

    @Scheduled(fixedRate = 20000)
    public void sendHeartbeat() {
        if (clients.isEmpty()) return;

        NotificationEvent pingEvent = NotificationEvent.of("PING", "SYSTEM", null, "ping", "keep-alive", null);
        for (ClientEmitter client : clients) {
            try {
                client.emitter().send(SseEmitter.event().name("PING").data(pingEvent));
            } catch (Exception e) {
                client.emitter().complete();
                clients.remove(client);
            }
        }
    }
}
