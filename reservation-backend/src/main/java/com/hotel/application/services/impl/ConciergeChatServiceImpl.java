package com.hotel.application.services.impl;

import com.hotel.application.dto.notification.NotificationEvent;
import com.hotel.application.services.AuditLogService;
import com.hotel.application.services.ConciergeChatService;
import com.hotel.application.services.SseNotificationService;
import com.hotel.domain.model.ConciergeMessage;
import com.hotel.domain.model.Reservation;
import com.hotel.domain.model.ReservationStatus;
import com.hotel.domain.model.Role;
import com.hotel.domain.model.User;
import com.hotel.domain.repository.ConciergeMessageRepository;
import com.hotel.domain.repository.ReservationRepository;
import com.hotel.domain.repository.UserRepository;
import com.hotel.web.dto.ConciergeConversationSummary;
import com.hotel.web.dto.ConciergeMessageRequest;
import com.hotel.web.dto.ConciergeMessageResponse;
import com.hotel.web.dto.ConciergeStatusResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Service
@RequiredArgsConstructor
public class ConciergeChatServiceImpl implements ConciergeChatService {

    private final ConciergeMessageRepository conciergeMessageRepository;
    private final UserRepository userRepository;
    private final ReservationRepository reservationRepository;
    private final SseNotificationService sseNotificationService;
    private final AuditLogService auditLogService;

    private final ConcurrentMap<Long, String> activeClaims = new ConcurrentHashMap<>();

    @Override
    @Transactional
    public ConciergeMessageResponse sendMessage(ConciergeMessageRequest request, String currentUsername) {
        User sender = userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado: " + currentUsername));

        Role senderRole = sender.getRole();
        String senderName = resolveSenderName(sender);

        Reservation reservation;
        String suiteNumber;
        User recipient = null;

        if (senderRole == Role.ROLE_CUSTOMER) {
            Optional<Reservation> activeOpt = findActiveCheckedInReservation(sender);
            if (activeOpt.isEmpty()) {
                throw new AccessDeniedException("El Concierge de Suite es exclusivo para huéspedes con estancia activa (Check-In realizado).");
            }
            reservation = activeOpt.get();
            suiteNumber = (reservation.getRoom() != null) ? reservation.getRoom().getRoomNumber() : request.suiteNumber();
        } else if (senderRole == Role.ROLE_RECEPTIONIST || senderRole == Role.ROLE_ADMIN) {
            if (request.recipientId() == null) {
                throw new IllegalArgumentException("El personal debe especificar el ID del huésped destinatario");
            }
            recipient = userRepository.findById(request.recipientId())
                    .orElseThrow(() -> new IllegalArgumentException("Huésped destinatario no encontrado: " + request.recipientId()));

            Optional<Reservation> activeOpt = findActiveCheckedInReservation(recipient);
            if (activeOpt.isPresent()) {
                reservation = activeOpt.get();
                suiteNumber = (reservation.getRoom() != null) ? reservation.getRoom().getRoomNumber() : request.suiteNumber();
            } else if (request.reservationId() != null) {
                reservation = reservationRepository.findById(request.reservationId()).orElse(null);
                suiteNumber = (reservation != null && reservation.getRoom() != null) ? reservation.getRoom().getRoomNumber() : request.suiteNumber();
            } else {
                reservation = null;
                suiteNumber = request.suiteNumber();
            }

            activeClaims.putIfAbsent(recipient.getId(), senderName);
        } else {
            throw new AccessDeniedException("Su rol no tiene autorización para enviar mensajes de Concierge");
        }

        ConciergeMessage entity = ConciergeMessage.builder()
                .sender(sender)
                .recipient(recipient)
                .reservation(reservation)
                .senderRole(senderRole)
                .senderName(senderName)
                .suiteNumber(suiteNumber)
                .message(request.message().trim())
                .readStatus(false)
                .build();

        ConciergeMessage saved = conciergeMessageRepository.save(entity);
        ConciergeMessageResponse response = mapToResponse(saved);

        if (senderRole == Role.ROLE_CUSTOMER) {
            NotificationEvent event = NotificationEvent.of(
                    "CONCIERGE_MESSAGE",
                    "FRONT_DESK",
                    saved.getId(),
                    "Mensaje de Huésped (" + (suiteNumber != null ? "Suite " + suiteNumber : senderName) + ")",
                    request.message(),
                    response
            );
            sseNotificationService.sendToRole("ROLE_RECEPTIONIST", event);
            sseNotificationService.sendToRole("ROLE_ADMIN", event);
            auditLogService.logAction(
                    "CONCIERGE_MESSAGE",
                    "ConciergeMessage",
                    String.valueOf(saved.getId()),
                    "Huésped " + senderName + " envió mensaje a recepción",
                    "SUCCESS"
            );
        } else {
            NotificationEvent event = NotificationEvent.of(
                    "CONCIERGE_MESSAGE",
                    "GUEST",
                    saved.getId(),
                    "Respuesta de Concierge / Front Desk",
                    request.message(),
                    response
            );
            sseNotificationService.sendToUser(recipient.getUsername(), event);
            auditLogService.logAction(
                    "CONCIERGE_REPLY",
                    "ConciergeMessage",
                    String.valueOf(saved.getId()),
                    "Recepción respondió a huésped " + recipient.getUsername(),
                    "SUCCESS"
            );
        }

        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ConciergeMessageResponse> getMyConversation(String currentUsername) {
        User user = userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado: " + currentUsername));

        if (user.getRole() == Role.ROLE_CUSTOMER) {
            Optional<Reservation> activeOpt = findActiveCheckedInReservation(user);
            if (activeOpt.isEmpty()) {
                return List.of();
            }
            return conciergeMessageRepository.findConversationByReservationId(activeOpt.get().getId())
                    .stream()
                    .map(this::mapToResponse)
                    .toList();
        }

        return conciergeMessageRepository.findConversationByUserId(user.getId())
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ConciergeMessageResponse> getConversationWithGuest(Long guestUserId, String staffUsername) {
        verifyStaffAccess(staffUsername);

        User guest = userRepository.findById(guestUserId)
                .orElseThrow(() -> new IllegalArgumentException("Huésped no encontrado: " + guestUserId));

        Optional<Reservation> activeOpt = findActiveCheckedInReservation(guest);
        if (activeOpt.isPresent()) {
            List<ConciergeMessage> messages = conciergeMessageRepository.findConversationByReservationId(activeOpt.get().getId());
            if (!messages.isEmpty()) {
                return messages.stream().map(this::mapToResponse).toList();
            }
        }

        return conciergeMessageRepository.findConversationBetweenStaffAndGuest(guestUserId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ConciergeConversationSummary> getActiveConversations(String staffUsername) {
        verifyStaffAccess(staffUsername);

        List<Reservation> inHouseReservations = reservationRepository.findByStatus(ReservationStatus.CHECKED_IN);
        List<ConciergeConversationSummary> summaries = new ArrayList<>();

        for (Reservation res : inHouseReservations) {
            if (res.getGuest() == null) continue;

            Optional<User> guestUserOpt = userRepository.findByGuestId(res.getGuest().getId());
            if (guestUserOpt.isEmpty()) continue;

            User guestUser = guestUserOpt.get();
            List<ConciergeMessage> messages = conciergeMessageRepository.findConversationByReservationId(res.getId());

            String lastMessage;
            LocalDateTime lastMessageAt;
            long unread = 0L;

            if (messages.isEmpty()) {
                lastMessage = "Estancia iniciada. Sin mensajes aún.";
                lastMessageAt = res.getCreatedAt() != null ? res.getCreatedAt() : LocalDateTime.now();
            } else {
                ConciergeMessage latest = messages.get(messages.size() - 1);
                lastMessage = latest.getMessage();
                lastMessageAt = latest.getCreatedAt();
                unread = messages.stream()
                        .filter(m -> m.getSenderRole() == Role.ROLE_CUSTOMER && !m.isReadStatus())
                        .count();
            }

            String suite = (res.getRoom() != null) ? res.getRoom().getRoomNumber() : null;
            String guestName = resolveSenderName(guestUser);
            String claimedBy = activeClaims.get(guestUser.getId());

            summaries.add(new ConciergeConversationSummary(
                    guestUser.getId(),
                    guestName,
                    guestUser.getEmail(),
                    suite,
                    lastMessage,
                    lastMessageAt,
                    unread,
                    claimedBy,
                    res.getId()
            ));
        }

        summaries.sort((a, b) -> {
            if (a.unreadCount() > 0 && b.unreadCount() == 0) return -1;
            if (a.unreadCount() == 0 && b.unreadCount() > 0) return 1;
            return b.lastMessageAt().compareTo(a.lastMessageAt());
        });

        return summaries;
    }

    @Override
    @Transactional
    public void markConversationAsRead(Long guestUserId, String currentUsername) {
        User user = userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado: " + currentUsername));

        if (user.getRole() == Role.ROLE_CUSTOMER) {
            conciergeMessageRepository.markAllAsReadForRecipient(user.getId());
        } else if (user.getRole() == Role.ROLE_RECEPTIONIST || user.getRole() == Role.ROLE_ADMIN) {
            if (guestUserId != null) {
                conciergeMessageRepository.markAllAsReadFromGuest(guestUserId);
            }
        }
    }

    @Override
    @Transactional(readOnly = true)
    public long getUnreadCount(String currentUsername) {
        User user = userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado: " + currentUsername));

        if (user.getRole() == Role.ROLE_CUSTOMER) {
            return conciergeMessageRepository.countByRecipientIdAndReadStatusFalse(user.getId());
        } else if (user.getRole() == Role.ROLE_RECEPTIONIST || user.getRole() == Role.ROLE_ADMIN) {
            return conciergeMessageRepository.countUnreadForFrontDesk();
        }
        return 0L;
    }

    @Override
    @Transactional(readOnly = true)
    public ConciergeStatusResponse getConciergeStatus(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado: " + username));

        if (user.getRole() != Role.ROLE_CUSTOMER) {
            return new ConciergeStatusResponse(false, null, null, resolveSenderName(user));
        }

        Optional<Reservation> activeOpt = findActiveCheckedInReservation(user);
        if (activeOpt.isEmpty()) {
            return new ConciergeStatusResponse(false, null, null, resolveSenderName(user));
        }

        Reservation active = activeOpt.get();
        String suiteNumber = (active.getRoom() != null) ? active.getRoom().getRoomNumber() : null;
        return new ConciergeStatusResponse(true, suiteNumber, active.getId(), resolveSenderName(user));
    }

    @Override
    public ConciergeConversationSummary claimConversation(Long guestUserId, String staffUsername) {
        verifyStaffAccess(staffUsername);

        User staff = userRepository.findByUsername(staffUsername)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado: " + staffUsername));
        User guest = userRepository.findById(guestUserId)
                .orElseThrow(() -> new IllegalArgumentException("Huésped no encontrado: " + guestUserId));

        String staffDisplayName = resolveSenderName(staff);
        activeClaims.put(guestUserId, staffDisplayName);

        NotificationEvent claimEvent = NotificationEvent.of(
                "CONCIERGE_CLAIM",
                "FRONT_DESK",
                guestUserId,
                "Conversación asignada",
                staffDisplayName + " está atendiendo la conversación.",
                Map.of("guestUserId", guestUserId, "claimedBy", staffDisplayName)
        );
        sseNotificationService.sendToRole("ROLE_RECEPTIONIST", claimEvent);
        sseNotificationService.sendToRole("ROLE_ADMIN", claimEvent);

        auditLogService.logAction(
                "CONCIERGE_CLAIM",
                "User",
                String.valueOf(guestUserId),
                "Personal " + staffDisplayName + " asumió la atención de la suite",
                "SUCCESS"
        );

        Optional<Reservation> activeOpt = findActiveCheckedInReservation(guest);
        Long resId = activeOpt.map(Reservation::getId).orElse(null);
        String suiteNumber = activeOpt.filter(r -> r.getRoom() != null).map(r -> r.getRoom().getRoomNumber()).orElse(null);

        return new ConciergeConversationSummary(
                guestUserId,
                resolveSenderName(guest),
                guest.getEmail(),
                suiteNumber,
                "Atención asignada a " + staffDisplayName,
                LocalDateTime.now(),
                0L,
                staffDisplayName,
                resId
        );
    }

    private Optional<Reservation> findActiveCheckedInReservation(User user) {
        if (user.getGuest() == null) {
            return Optional.empty();
        }
        return reservationRepository.findByGuestId(user.getGuest().getId())
                .stream()
                .filter(r -> r.getStatus() == ReservationStatus.CHECKED_IN)
                .findFirst();
    }

    private void verifyStaffAccess(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado: " + username));

        if (user.getRole() != Role.ROLE_RECEPTIONIST && user.getRole() != Role.ROLE_ADMIN) {
            throw new AccessDeniedException("Acceso restringido a personal de recepción o administración");
        }
    }

    private String resolveSenderName(User user) {
        if (user.getGuest() != null && user.getGuest().getFirstName() != null) {
            String name = user.getGuest().getFirstName();
            if (user.getGuest().getLastName() != null) {
                name += " " + user.getGuest().getLastName();
            }
            return name.trim();
        }
        return user.getUsername();
    }

    private ConciergeMessageResponse mapToResponse(ConciergeMessage m) {
        return new ConciergeMessageResponse(
                m.getId(),
                m.getSender().getId(),
                m.getSenderName(),
                m.getSenderRole().name(),
                m.getRecipient() != null ? m.getRecipient().getId() : null,
                m.getReservation() != null ? m.getReservation().getId() : null,
                m.getSuiteNumber(),
                m.getMessage(),
                m.isReadStatus(),
                m.getCreatedAt()
        );
    }
}
